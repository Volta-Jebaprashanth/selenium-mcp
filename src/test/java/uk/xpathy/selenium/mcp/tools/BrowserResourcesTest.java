package uk.xpathy.selenium.mcp.tools;

import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.provider.resource.SyncMcpResourceProvider;
import uk.xpathy.selenium.mcp.webdriver.ConsoleLogMonitor;
import uk.xpathy.selenium.mcp.webdriver.NetworkMonitor;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrowserResourcesTest {

    private final Tools tools = mock(Tools.class);
    private final Map<String, SyncResourceSpecification> specs =
            new SyncMcpResourceProvider(List.of(new BrowserResources(tools))).getResourceSpecifications().stream()
                    .collect(Collectors.toMap(s -> s.resource().uri(), Function.identity()));

    @Test
    void registersEveryResourceWithItsMimeType() {
        assertEquals(Set.of(BrowserResources.SESSION_URI, BrowserResources.PAGE_ELEMENTS_URI,
                BrowserResources.PAGE_SOURCE_URI, BrowserResources.PAGE_SCREENSHOT_URI,
                BrowserResources.CONSOLE_LOGS_URI, BrowserResources.NETWORK_LOG_URI,
                BrowserResources.NETWORK_FAILURES_URI, BrowserResources.LOCATOR_GUIDE_URI), specs.keySet());
        assertEquals("image/png", specs.get(BrowserResources.PAGE_SCREENSHOT_URI).resource().mimeType());
        assertEquals("application/json", specs.get(BrowserResources.SESSION_URI).resource().mimeType());
    }

    @Test
    void explainsWhenNoBrowserIsOpen() {
        TextResourceContents contents = assertInstanceOf(TextResourceContents.class,
                read(BrowserResources.PAGE_SCREENSHOT_URI));

        assertEquals("text/plain", contents.mimeType());
        assertTrue(contents.text().startsWith("No browser is open."));
        verify(tools, never()).takeScreenshotBase64();
    }

    @Test
    void sessionReportsAttachedBrowserState() {
        when(tools.isBrowserOpen()).thenReturn(true);
        when(tools.getBrowserName()).thenReturn("chrome");
        when(tools.isAttached()).thenReturn(true);
        when(tools.getDebuggerAddress()).thenReturn("127.0.0.1:9222");
        when(tools.getCurrentUrl()).thenReturn("https://shop.test/checkout");
        when(tools.getTitle()).thenReturn("Checkout");
        when(tools.getWindowHandle()).thenReturn("w1");
        when(tools.getWindowHandles()).thenReturn(Set.of("w1", "w2"));
        when(tools.isNetworkCaptureSupported()).thenReturn(true);
        when(tools.isNetworkCapturing()).thenReturn(true);

        String json = text(BrowserResources.SESSION_URI);

        assertTrue(json.contains("\"mode\":\"attached\""), json);
        assertTrue(json.contains("\"debuggerAddress\":\"127.0.0.1:9222\""), json);
        assertTrue(json.contains("\"currentUrl\":\"https://shop.test/checkout\""), json);
        assertTrue(json.contains("\"windowCount\":2"), json);
        assertTrue(json.contains("\"networkCapture\":\"on\""), json);
        assertTrue(json.contains("\"consoleCapture\":\"unsupported\""), json);
    }

    @Test
    void sessionReportsAnOpenAlertInsteadOfReadingThePage() {
        when(tools.isBrowserOpen()).thenReturn(true);
        when(tools.isAlertPresent()).thenReturn(true);
        when(tools.getAlertText()).thenReturn("Leave site?");

        String json = text(BrowserResources.SESSION_URI);

        assertTrue(json.contains("\"alertText\":\"Leave site?\""), json);
        verify(tools, never()).getTitle();
    }

    @Test
    void screenshotIsAPngBlob() {
        when(tools.isBrowserOpen()).thenReturn(true);
        when(tools.takeScreenshotBase64()).thenReturn("iVBORw0KGgo=");

        BlobResourceContents contents = assertInstanceOf(BlobResourceContents.class,
                read(BrowserResources.PAGE_SCREENSHOT_URI));

        assertEquals("image/png", contents.mimeType());
        assertEquals("iVBORw0KGgo=", contents.blob());
    }

    @Test
    void networkFailuresKeepRealErrorsOnly() {
        when(tools.isBrowserOpen()).thenReturn(true);
        when(tools.isNetworkCaptureSupported()).thenReturn(true);
        when(tools.isNetworkCapturing()).thenReturn(true);
        when(tools.getNetworkLog(null, null, 0)).thenReturn(List.of(
                entry("https://shop.test/api/cart", 200, false),
                entry("https://shop.test/api/orders", 500, false),
                entry("https://ads.test/pixel", 403, true)));

        String json = text(BrowserResources.NETWORK_FAILURES_URI);

        assertTrue(json.contains("/api/orders"), json);
        assertFalse(json.contains("/api/cart"), json);
        assertFalse(json.contains("ads.test"), json);
    }

    @Test
    void networkLogSummaryLeavesOutBodies() {
        when(tools.isBrowserOpen()).thenReturn(true);
        when(tools.isNetworkCaptureSupported()).thenReturn(true);
        when(tools.getNetworkLog(null, null, 0)).thenReturn(List.of(entry("https://shop.test/api/cart", 200, false)));

        String json = text(BrowserResources.NETWORK_LOG_URI);

        assertTrue(json.contains("\"status\":200"), json);
        assertFalse(json.contains("responseBody"), json);
        assertTrue(json.contains("startNetworkCapture"), json);
    }

    @Test
    void consoleLogsExplainUnsupportedBrowsers() {
        when(tools.isBrowserOpen()).thenReturn(true);
        when(tools.getBrowserName()).thenReturn("firefox");

        String json = text(BrowserResources.CONSOLE_LOGS_URI);

        assertTrue(json.contains("\"supported\":false"), json);
        assertTrue(json.contains("firefox"), json);
    }

    @Test
    void consoleLogsIncludeCapturedEntries() {
        when(tools.isBrowserOpen()).thenReturn(true);
        when(tools.isConsoleCaptureSupported()).thenReturn(true);
        when(tools.isConsoleCapturing()).thenReturn(true);
        when(tools.getConsoleLogs(null, 0)).thenReturn(List.of(
                new ConsoleLogMonitor.ConsoleLogEntry(Instant.EPOCH, "error", "TypeError: x is undefined")));

        String json = text(BrowserResources.CONSOLE_LOGS_URI);

        assertTrue(json.contains("TypeError: x is undefined"), json);
        assertTrue(json.contains("\"capturing\":true"), json);
    }

    @Test
    void locatorGuideNeedsNoBrowser() {
        assertTrue(text(BrowserResources.LOCATOR_GUIDE_URI).contains("data-testid"));
    }

    private ResourceContents read(String uri) {
        return specs.get(uri).readHandler().apply(null, new ReadResourceRequest(uri)).contents().get(0);
    }

    private String text(String uri) {
        return assertInstanceOf(TextResourceContents.class, read(uri)).text();
    }

    private static NetworkMonitor.NetworkEntry entry(String url, int status, boolean mocked) {
        return new NetworkMonitor.NetworkEntry(Instant.EPOCH, "GET", url, Map.of(), "", status, Map.of(),
                "{\"error\":\"boom\"}", 12, mocked);
    }
}
