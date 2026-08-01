package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.HasAuthentication;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;
import org.openqa.selenium.chromium.HasNetworkConditions;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.Filter;
import org.openqa.selenium.remote.http.HttpHandler;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Captures, mocks, and blocks HTTP traffic for the current browser session using Selenium 4's
 * {@link NetworkInterceptor}, and tracks in-flight request counts so callers can wait for the
 * network to go idle. A single {@link Filter} is armed lazily on first use and stays in place
 * for the lifetime of the session; it handles blocking, mocking, and passive capture in one pass
 * so all three features can be combined.
 * <p>
 * Only available for drivers that implement {@link HasDevTools} (Chrome and Edge, via CDP).
 * Firefox is not supported and every entry point throws {@link UnsupportedOperationException}.
 */
public class NetworkMonitor {

    private static final int MAX_ENTRIES = 500;
    private static final int MAX_BODY_CHARS = 20_000;

    private final WebDriver driver;
    private final Deque<NetworkEntry> entries = new ArrayDeque<>();
    private final List<MockRule> mockRules = new CopyOnWriteArrayList<>();
    private final List<Pattern> blockedPatterns = new CopyOnWriteArrayList<>();
    private final AtomicInteger pendingRequests = new AtomicInteger();
    private final AtomicLong lastActivityMillis = new AtomicLong(System.currentTimeMillis());

    private NetworkInterceptor interceptor;
    private volatile boolean capturing;
    private volatile Pattern captureFilter;

    public NetworkMonitor(WebDriver driver) {
        this.driver = driver;
    }

    public record NetworkEntry(
            Instant timestamp, String method, String url,
            Map<String, String> requestHeaders, String requestBody,
            int status, Map<String, String> responseHeaders, String responseBody,
            long durationMillis, boolean mocked) {
    }

    private record MockRule(
            Pattern urlPattern, String method, int status,
            String contentType, String body, Map<String, String> headers) {
    }

    public boolean isSupported() {
        return driver instanceof HasDevTools;
    }

    // -- capture --

    public synchronized void startCapture(String urlPattern) {
        requireSupported();
        this.captureFilter = (urlPattern == null || urlPattern.isBlank()) ? null : Pattern.compile(urlPattern);
        this.capturing = true;
        ensureInterceptor();
    }

    public void stopCapture() {
        this.capturing = false;
    }

    public synchronized void clearLog() {
        entries.clear();
    }

    public synchronized List<NetworkEntry> getEntries(String urlPattern, String method, int limit) {
        Pattern p = (urlPattern == null || urlPattern.isBlank()) ? null : Pattern.compile(urlPattern);
        return entries.stream()
                .filter(e -> p == null || p.matcher(e.url()).find())
                .filter(e -> method == null || method.isBlank() || e.method().equalsIgnoreCase(method))
                .limit(limit <= 0 ? MAX_ENTRIES : limit)
                .collect(Collectors.toList());
    }

    // -- mocking / blocking --

    public void mockResponse(String urlPattern, String method, int status,
                              String contentType, String body, Map<String, String> headers) {
        requireSupported();
        mockRules.add(new MockRule(Pattern.compile(urlPattern), method, status, contentType, body,
                headers == null ? Map.of() : headers));
        ensureInterceptor();
    }

    public void clearMocks() {
        mockRules.clear();
    }

    public void blockRequests(String urlPattern) {
        requireSupported();
        blockedPatterns.add(Pattern.compile(urlPattern));
        ensureInterceptor();
    }

    public void clearBlocked() {
        blockedPatterns.clear();
    }

    // -- network idle wait --

    public int getPendingRequestCount() {
        return pendingRequests.get();
    }

    /**
     * Arms the interceptor (if not already armed) so pending-request tracking is active,
     * without enabling capture logging or any mock/block rules.
     */
    public void armTracking() {
        requireSupported();
        ensureInterceptor();
    }

    /**
     * Blocks until there have been no in-flight requests for {@code idleMillis} continuously,
     * or throws once {@code timeoutSeconds} elapses. Arms the interceptor if it isn't already,
     * so this can be called standalone without {@link #startCapture} first.
     */
    public void waitForNetworkIdle(long idleMillis, int timeoutSeconds) {
        requireSupported();
        ensureInterceptor();
        // Treat "now" as a baseline activity timestamp: a request triggered just before this
        // call (e.g. by a click) may not have reached the pending counter yet, since delivery of
        // the underlying CDP event is asynchronous. Without this, a call made in that gap could
        // see zero pending requests and a stale lastActivityMillis and declare idle immediately.
        lastActivityMillis.set(System.currentTimeMillis());
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            long sinceActivity = System.currentTimeMillis() - lastActivityMillis.get();
            if (pendingRequests.get() == 0 && sinceActivity >= idleMillis) {
                return;
            }
            try {
                Thread.sleep(Math.max(20, Math.min(100, idleMillis)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for network idle", e);
            }
        }
        throw new RuntimeException("Timed out after " + timeoutSeconds + "s waiting for network idle ("
                + pendingRequests.get() + " request(s) still in flight)");
    }

    // -- network conditions (Chrome/Edge only) --

    public void setNetworkConditions(boolean offline, long latencyMillis, int downloadKbps, int uploadKbps) {
        ChromiumNetworkConditions conditions = new ChromiumNetworkConditions();
        conditions.setOffline(offline);
        conditions.setLatency(Duration.ofMillis(latencyMillis));
        conditions.setDownloadThroughput(downloadKbps);
        conditions.setUploadThroughput(uploadKbps);
        requireNetworkConditions().setNetworkConditions(conditions);
    }

    public String getNetworkConditions() {
        ChromiumNetworkConditions c = requireNetworkConditions().getNetworkConditions();
        return "offline=" + c.getOffline()
                + ", latency=" + c.getLatency().toMillis() + "ms"
                + ", downloadThroughput=" + c.getDownloadThroughput() + "kb/s"
                + ", uploadThroughput=" + c.getUploadThroughput() + "kb/s";
    }

    public void clearNetworkConditions() {
        requireNetworkConditions().deleteNetworkConditions();
    }

    // -- HTTP Basic auth (Chrome/Edge only) --

    /**
     * Registers credentials to be supplied automatically for any HTTP Basic/Digest auth
     * challenge, so a login prompt never blocks navigation.
     */
    public void setBasicAuthCredentials(String username, String password) {
        requireSupported();
        if (!(driver instanceof HasAuthentication)) {
            throw new UnsupportedOperationException("Basic auth credential injection requires Chrome or Edge.");
        }
        ((HasAuthentication) driver).register(() -> new UsernameAndPassword(username, password));
    }

    // -- teardown --

    public synchronized void close() {
        if (interceptor != null) {
            try {
                interceptor.close();
            } catch (Exception ignored) {
                // best-effort cleanup; the driver may already be gone
            } finally {
                interceptor = null;
            }
        }
        entries.clear();
        mockRules.clear();
        blockedPatterns.clear();
        capturing = false;
    }

    // -- internals --

    private void requireSupported() {
        if (!isSupported()) {
            throw new UnsupportedOperationException(
                    "Network capture/mocking/blocking requires Chrome or Edge (Chrome DevTools Protocol); "
                            + "not supported for this browser.");
        }
    }

    private HasNetworkConditions requireNetworkConditions() {
        requireSupported();
        if (!(driver instanceof HasNetworkConditions)) {
            throw new UnsupportedOperationException("Network condition simulation requires Chrome or Edge.");
        }
        return (HasNetworkConditions) driver;
    }

    private synchronized void ensureInterceptor() {
        if (interceptor != null) {
            return;
        }
        Filter filter = next -> req -> handle(req, next);
        interceptor = new NetworkInterceptor(driver, filter);
    }

    private HttpResponse handle(HttpRequest req, HttpHandler next) {
        String url = req.getUri();

        Pattern blocked = blockedPatterns.stream().filter(p -> p.matcher(url).find()).findFirst().orElse(null);
        if (blocked != null) {
            HttpResponse blockedResponse = new HttpResponse()
                    .setStatus(403)
                    .setContent(Contents.utf8String("Blocked by selenium-mcp (pattern: " + blocked.pattern() + ")"));
            lastActivityMillis.set(System.currentTimeMillis());
            record(req, blockedResponse, 0, true);
            return blockedResponse;
        }

        MockRule mock = mockRules.stream()
                .filter(r -> r.urlPattern().matcher(url).find())
                .filter(r -> r.method() == null || r.method().isBlank()
                        || r.method().equalsIgnoreCase(req.getMethod().name()))
                .findFirst().orElse(null);
        if (mock != null) {
            HttpResponse mockResponse = new HttpResponse().setStatus(mock.status());
            mock.headers().forEach(mockResponse::addHeader);
            if (mock.contentType() != null && !mock.contentType().isBlank()) {
                mockResponse.setHeader("Content-Type", mock.contentType());
            }
            mockResponse.setContent(Contents.utf8String(mock.body() == null ? "" : mock.body()));
            lastActivityMillis.set(System.currentTimeMillis());
            record(req, mockResponse, 0, true);
            return mockResponse;
        }

        pendingRequests.incrementAndGet();
        lastActivityMillis.set(System.currentTimeMillis());
        long start = System.currentTimeMillis();
        try {
            HttpResponse res = next.execute(req);
            long duration = System.currentTimeMillis() - start;
            if (capturing && (captureFilter == null || captureFilter.matcher(url).find())) {
                record(req, res, duration, false);
            }
            return res;
        } finally {
            pendingRequests.decrementAndGet();
            lastActivityMillis.set(System.currentTimeMillis());
        }
    }

    private synchronized void record(HttpRequest req, HttpResponse res, long durationMillis, boolean mocked) {
        Map<String, String> reqHeaders = headerMap(req.getHeaderNames(), req::getHeader);
        Map<String, String> resHeaders = headerMap(res.getHeaderNames(), res::getHeader);
        String reqBody = truncate(safeBody(() -> Contents.string(req)));
        String resBody = truncate(safeBody(() -> Contents.string(res)));

        entries.addLast(new NetworkEntry(Instant.now(), req.getMethod().name(), req.getUri(),
                reqHeaders, reqBody, res.getStatus(), resHeaders, resBody, durationMillis, mocked));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    private Map<String, String> headerMap(Iterable<String> names, java.util.function.Function<String, String> getter) {
        Map<String, String> map = new LinkedHashMap<>();
        names.forEach(name -> map.put(name, getter.apply(name)));
        return map;
    }

    private String safeBody(java.util.function.Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return "";
        }
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > MAX_BODY_CHARS ? body.substring(0, MAX_BODY_CHARS) + "...[truncated]" : body;
    }
}
