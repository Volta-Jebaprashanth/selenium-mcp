package uk.xpathy.selenium.mcp.webdriver;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.Filter;
import org.openqa.selenium.remote.http.HttpHandler;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Drives {@link NetworkMonitor}'s request filter directly, standing in for the {@code NetworkInterceptor}
 * that would install it on a real browser. The live-browser path is covered by {@code ChromeIntegrationTest}.
 */
class NetworkMonitorTest {

    private final WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasDevTools.class));
    private final AtomicInteger interceptorsInstalled = new AtomicInteger();
    private final AtomicInteger interceptorsClosed = new AtomicInteger();
    private Filter filter;
    private final NetworkMonitor monitor = new NetworkMonitor(driver, installed -> {
        filter = installed;
        interceptorsInstalled.incrementAndGet();
        return interceptorsClosed::incrementAndGet;
    });

    @Test
    void everyCdpFeatureIsRefusedForABrowserWithoutDevTools() {
        NetworkMonitor firefox = new NetworkMonitor(mock(WebDriver.class), installed -> {
            throw new AssertionError("no interceptor should be installed");
        });

        assertFalse(firefox.isSupported());
        assertThrows(UnsupportedOperationException.class, () -> firefox.startCapture(null));
        assertThrows(UnsupportedOperationException.class, () -> firefox.mockResponse(".*", null, 200, null, "", null));
        assertThrows(UnsupportedOperationException.class, () -> firefox.blockRequests(".*"));
        assertThrows(UnsupportedOperationException.class, () -> firefox.waitForNetworkIdle(100, 1));
        assertThrows(UnsupportedOperationException.class, () -> firefox.setNetworkConditions(true, 0, 0, 0));
        assertThrows(UnsupportedOperationException.class, () -> firefox.setBasicAuthCredentials("u", "p"));
    }

    @Test
    void oneInterceptorServesCaptureMocksAndBlocksAndIsClosedWithTheMonitor() {
        monitor.startCapture(null);
        monitor.mockResponse("/api", null, 200, null, "{}", null);
        monitor.blockRequests("ads");

        monitor.close();

        assertEquals(1, interceptorsInstalled.get());
        assertEquals(1, interceptorsClosed.get());
    }

    @Test
    void aBlockedRequestNeverReachesTheNetworkAndWinsOverAMock() {
        monitor.mockResponse("ads", null, 200, null, "mocked", null);
        monitor.blockRequests("ads\\.example");

        HttpResponse response = send(HttpMethod.GET, "https://ads.example/pixel", req -> {
            throw new AssertionError("blocked request was sent");
        });

        assertEquals(403, response.getStatus());
        assertTrue(response.contentAsString().contains("ads\\.example"), response.contentAsString());
        NetworkMonitor.NetworkEntry entry = monitor.getEntries(null, null, 0).getFirst();
        assertTrue(entry.mocked());
        assertEquals(403, entry.status());
    }

    @Test
    void aMockAnswersOnlyTheMethodItWasRegisteredFor() {
        monitor.mockResponse("/api/user", "GET", 418, "application/json", "{\"name\":\"x\"}", Map.of("X-Mock", "1"));

        HttpResponse mocked = send(HttpMethod.GET, "https://app.example/api/user", req -> {
            throw new AssertionError("mocked request was sent");
        });
        HttpResponse passedThrough = send(HttpMethod.POST, "https://app.example/api/user", req -> ok("real"));

        assertEquals(418, mocked.getStatus());
        assertEquals("{\"name\":\"x\"}", mocked.contentAsString());
        assertEquals("application/json", mocked.getHeader("Content-Type"));
        assertEquals("1", mocked.getHeader("X-Mock"));
        assertEquals("real", passedThrough.contentAsString());
    }

    @Test
    void captureRecordsMatchingTrafficOnlyWhileOn() {
        monitor.startCapture("/api/");
        send(HttpMethod.POST, "https://app.example/api/login", req -> ok("token"), "{\"u\":\"a\"}");
        send(HttpMethod.GET, "https://app.example/logo.png", req -> ok("png"));
        send(HttpMethod.GET, "https://app.example/api/me", req -> ok("me"));
        monitor.stopCapture();
        send(HttpMethod.GET, "https://app.example/api/later", req -> ok("later"));

        List<NetworkMonitor.NetworkEntry> all = monitor.getEntries(null, null, 0);
        assertEquals(List.of("https://app.example/api/login", "https://app.example/api/me"),
                all.stream().map(NetworkMonitor.NetworkEntry::url).toList());
        NetworkMonitor.NetworkEntry login = all.getFirst();
        assertEquals("{\"u\":\"a\"}", login.requestBody());
        assertEquals("token", login.responseBody());
        assertFalse(login.mocked());

        assertEquals(1, monitor.getEntries("login", null, 0).size());
        assertEquals(1, monitor.getEntries(null, "post", 0).size());
        assertEquals(1, monitor.getEntries(null, null, 1).size());

        monitor.clearLog();
        assertEquals(List.of(), monitor.getEntries(null, null, 0));
    }

    @Test
    void longBodiesAreTruncatedAndTheLogKeepsOnlyTheNewestEntries() {
        monitor.startCapture(null);
        send(HttpMethod.GET, "https://app.example/big", req -> ok("x".repeat(20_001)));
        for (int i = 0; i < 500; i++) {
            send(HttpMethod.GET, "https://app.example/" + i, req -> ok(""));
        }

        List<NetworkMonitor.NetworkEntry> entries = monitor.getEntries(null, null, 0);
        assertEquals(500, entries.size());
        assertEquals("https://app.example/0", entries.getFirst().url());

        monitor.clearLog();
        send(HttpMethod.GET, "https://app.example/big", req -> ok("x".repeat(20_001)));
        String body = monitor.getEntries(null, null, 0).getFirst().responseBody();
        assertEquals("x".repeat(20_000) + "...[truncated]", body);
    }

    @Test
    void inFlightRequestsAreCountedEvenWhenTheyFail() {
        monitor.armTracking();
        AtomicInteger pendingDuringRequest = new AtomicInteger(-1);

        assertThrows(IllegalStateException.class, () -> send(HttpMethod.GET, "https://app.example/", req -> {
            pendingDuringRequest.set(monitor.getPendingRequestCount());
            throw new IllegalStateException("connection reset");
        }));

        assertEquals(1, pendingDuringRequest.get());
        assertEquals(0, monitor.getPendingRequestCount());
    }

    @Test
    void waitForNetworkIdleReturnsOnceQuietAndTimesOutWhileARequestHangs() throws Exception {
        monitor.waitForNetworkIdle(50, 5);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread request = new Thread(() -> send(HttpMethod.GET, "https://app.example/slow", req -> {
            started.countDown();
            await(release);
            return ok("done");
        }));
        request.start();
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));

            RuntimeException e = assertThrows(RuntimeException.class, () -> monitor.waitForNetworkIdle(50, 1));

            assertTrue(e.getMessage().contains("1 request(s) still in flight"), e.getMessage());
        } finally {
            release.countDown();
            request.join(5_000);
        }
        monitor.waitForNetworkIdle(50, 5);
    }

    private HttpResponse send(HttpMethod method, String url, HttpHandler network) {
        return send(method, url, network, null);
    }

    private HttpResponse send(HttpMethod method, String url, HttpHandler network, String body) {
        HttpRequest request = new HttpRequest(method, url);
        if (body != null) {
            request.setContent(Contents.utf8String(body));
        }
        return filter.apply(network).execute(request);
    }

    private static HttpResponse ok(String body) {
        return new HttpResponse().setStatus(200).setContent(Contents.utf8String(body));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
