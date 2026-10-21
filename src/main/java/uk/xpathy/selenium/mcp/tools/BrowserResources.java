package uk.xpathy.selenium.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Read-only MCP resources over the live browser session, so an engineer can attach the current
 * page state to a conversation (e.g. {@code @selenium-mcp:selenium://page/elements} in Claude Code)
 * without asking the agent to make a tool call. They mirror existing tools but pick token-cheap
 * defaults: the element tree rather than raw HTML first, failed requests rather than the whole log.
 * <p>
 * Like the tools, every resource answers with explanatory text rather than an error when there is
 * no browser, or when a CDP-backed view is read on a browser that doesn't support it.
 */
@Component
@ToolGroup("resources")
public class BrowserResources {

    static final String SESSION_URI = "selenium://session";
    static final String PAGE_ELEMENTS_URI = "selenium://page/elements";
    static final String PAGE_SOURCE_URI = "selenium://page/source";
    static final String PAGE_SCREENSHOT_URI = "selenium://page/screenshot";
    static final String CONSOLE_LOGS_URI = "selenium://console/logs";
    static final String NETWORK_LOG_URI = "selenium://network/log";
    static final String NETWORK_FAILURES_URI = "selenium://network/failures";
    static final String LOCATOR_GUIDE_URI = "selenium://guide/locators";

    private static final String JSON = "application/json";
    private static final String TEXT = "text/plain";
    private static final String MARKDOWN = "text/markdown";
    private static final String NO_BROWSER =
            "No browser is open. Call openBrowser, or launchDebugBrowser/connectToBrowser to work in the "
                    + "browser a test is running in.";
    private static final int ELEMENT_TREE_DEPTH = 20;

    private final Tools tools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BrowserResources(Tools tools) {
        this.tools = tools;
    }

    @McpResource(uri = SESSION_URI, name = "session", title = "Browser session",
            description = "Snapshot of the browser session: browser, whether it was launched or attached over a "
                    + "debugger address (a test's browser), current URL and title, open windows, any open alert, "
                    + "and whether network/console capture is running.",
            mimeType = JSON)
    public ResourceContents session() {
        synchronized (tools) {
            return read(SESSION_URI, JSON, () -> {
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("browserOpen", tools.isBrowserOpen());
                if (!tools.isBrowserOpen()) {
                    state.put("hint", NO_BROWSER);
                    return toJson(state);
                }
                state.put("browser", tools.getBrowserName());
                state.put("mode", tools.isAttached() ? "attached" : "launched");
                if (tools.isAttached()) {
                    state.put("debuggerAddress", tools.getDebuggerAddress());
                }
                // URL and title can't be read while a dialog is open, so report the dialog instead.
                if (tools.isAlertPresent()) {
                    state.put("alertText", tools.getAlertText());
                } else {
                    state.put("currentUrl", tools.getCurrentUrl());
                    state.put("title", tools.getTitle());
                }
                state.put("windowHandle", tools.getWindowHandle());
                state.put("windowCount", tools.getWindowHandles().size());
                state.put("networkCapture", captureState(tools.isNetworkCaptureSupported(), tools::isNetworkCapturing));
                state.put("consoleCapture", captureState(tools.isConsoleCaptureSupported(), tools::isConsoleCapturing));
                return toJson(state);
            });
        }
    }

    @McpResource(uri = PAGE_ELEMENTS_URI, name = "page-elements", title = "Page element tree",
            description = "The current page as a compact JSON element tree (tag, notable attributes such as id, name, "
                    + "data-testid and aria-label, own text, children) without scripts or styles. Much cheaper than "
                    + "the raw HTML; the starting point for writing locators or a Page Object.",
            mimeType = JSON)
    public ResourceContents pageElements() {
        synchronized (tools) {
            return readPage(PAGE_ELEMENTS_URI, JSON, () -> tools.getPageElements(ELEMENT_TREE_DEPTH));
        }
    }

    @McpResource(uri = PAGE_SOURCE_URI, name = "page-source", title = "Page HTML source",
            description = "The full HTML of the current page. Large; prefer selenium://page/elements unless you need "
                    + "the exact markup.",
            mimeType = "text/html")
    public ResourceContents pageSource() {
        synchronized (tools) {
            return readPage(PAGE_SOURCE_URI, "text/html", tools::getPageSource);
        }
    }

    @McpResource(uri = PAGE_SCREENSHOT_URI, name = "page-screenshot", title = "Page screenshot",
            description = "PNG screenshot of the current viewport, e.g. to spot an overlay, spinner or cookie banner "
                    + "covering the element a test failed on.",
            mimeType = "image/png")
    public ResourceContents pageScreenshot() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return text(PAGE_SCREENSHOT_URI, TEXT, NO_BROWSER);
            }
            try {
                return BlobResourceContents.builder(PAGE_SCREENSHOT_URI, tools.takeScreenshotBase64())
                        .mimeType("image/png")
                        .build();
            } catch (Exception e) {
                return text(PAGE_SCREENSHOT_URI, TEXT, "Failed to take screenshot: " + ErrorMessages.of(e));
            }
        }
    }

    @McpResource(uri = CONSOLE_LOGS_URI, name = "console-logs", title = "Browser console logs",
            description = "Browser console output captured so far (most recent last), with whether capture is "
                    + "running. Capture only records from startConsoleCapture onwards. Chrome/Edge only.",
            mimeType = JSON)
    public ResourceContents consoleLogs() {
        synchronized (tools) {
            return readPage(CONSOLE_LOGS_URI, JSON, () -> {
                if (!tools.isConsoleCaptureSupported()) {
                    return unsupported("Console capture");
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("capturing", tools.isConsoleCapturing());
                if (!tools.isConsoleCapturing()) {
                    result.put("hint", "Console capture is off. Call startConsoleCapture, then repeat the steps.");
                }
                result.put("entries", tools.getConsoleLogs(null, 0).stream().map(NetworkTools::toMap).toList());
                return toJson(result);
            });
        }
    }

    @McpResource(uri = NETWORK_LOG_URI, name = "network-log", title = "Network log summary",
            description = "Captured network traffic as a one-line-per-request summary (method, URL, status, duration, "
                    + "mocked), most recent last, plus whether capture is running. No headers or bodies; use the "
                    + "getNetworkLog tool for full details of specific requests. Chrome/Edge only.",
            mimeType = JSON)
    public ResourceContents networkLog() {
        synchronized (tools) {
            return readPage(NETWORK_LOG_URI, JSON, () -> {
                if (!tools.isNetworkCaptureSupported()) {
                    return unsupported("Network capture");
                }
                List<Map<String, Object>> entries = tools.getNetworkLog(null, null, 0).stream().map(e -> {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("timestamp", e.timestamp().toString());
                    summary.put("method", e.method());
                    summary.put("url", e.url());
                    summary.put("status", e.status());
                    summary.put("durationMillis", e.durationMillis());
                    summary.put("mocked", e.mocked());
                    return summary;
                }).toList();
                return toJson(networkResult(entries));
            });
        }
    }

    @McpResource(uri = NETWORK_FAILURES_URI, name = "network-failures", title = "Failed network requests",
            description = "Captured requests that got an HTTP 4xx/5xx response from the real backend, with full "
                    + "headers and bodies - usually the fastest way to tell an application bug from a test bug. "
                    + "Mocked and blocked requests are left out. Chrome/Edge only.",
            mimeType = JSON)
    public ResourceContents networkFailures() {
        synchronized (tools) {
            return readPage(NETWORK_FAILURES_URI, JSON, () -> {
                if (!tools.isNetworkCaptureSupported()) {
                    return unsupported("Network capture");
                }
                List<Map<String, Object>> failures = tools.getNetworkLog(null, null, 0).stream()
                        .filter(e -> e.status() >= 400 && !e.mocked())
                        .map(NetworkTools::toMap)
                        .toList();
                return toJson(networkResult(failures));
            });
        }
    }

    @McpResource(uri = LOCATOR_GUIDE_URI, name = "locator-guide", title = "Locator guide",
            description = "How to choose stable Selenium locators and verify them against the live page with this "
                    + "server's tools. Static reference; no browser needed.",
            mimeType = MARKDOWN)
    public ResourceContents locatorGuide() {
        return text(LOCATOR_GUIDE_URI, MARKDOWN, TestAutomationPrompts.LOCATOR_GUIDE);
    }

    private Map<String, Object> networkResult(List<Map<String, Object>> entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("capturing", tools.isNetworkCapturing());
        if (!tools.isNetworkCapturing()) {
            result.put("hint", "Network capture is off. Call startNetworkCapture, then repeat the steps.");
        }
        result.put("entries", entries);
        return result;
    }

    private static String captureState(boolean supported, Supplier<Boolean> capturing) {
        if (!supported) {
            return "unsupported";
        }
        return capturing.get() ? "on" : "off";
    }

    private String unsupported(String feature) throws Exception {
        return toJson(Map.of("supported", false, "hint",
                feature + " requires Chrome or Edge (Chrome DevTools Protocol); the open browser is "
                        + tools.getBrowserName() + "."));
    }

    private ResourceContents readPage(String uri, String mimeType, ThrowingSupplier body) {
        if (!tools.isBrowserOpen()) {
            return text(uri, TEXT, NO_BROWSER);
        }
        return read(uri, mimeType, body);
    }

    private ResourceContents read(String uri, String mimeType, ThrowingSupplier body) {
        try {
            return text(uri, mimeType, body.get());
        } catch (Exception e) {
            return text(uri, TEXT, "Failed to read " + uri + ": " + ErrorMessages.of(e));
        }
    }

    private static ResourceContents text(String uri, String mimeType, String content) {
        return TextResourceContents.builder(uri, content).mimeType(mimeType).build();
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        String get() throws Exception;
    }
}
