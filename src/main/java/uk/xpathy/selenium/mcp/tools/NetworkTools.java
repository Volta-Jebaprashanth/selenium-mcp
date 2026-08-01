package uk.xpathy.selenium.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.ConsoleLogMonitor;
import uk.xpathy.selenium.mcp.webdriver.NetworkMonitor;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP-facing wrapper around {@link Tools} for Selenium 4's Chrome DevTools Protocol
 * capabilities: passive network capture, request mocking/blocking, simulated network
 * conditions, HTTP Basic auth injection, browser console log capture, and the
 * network-idle wait that the plain {@code WaitTools} explicit waits can't express.
 * <p>
 * Everything here requires Chrome or Edge; on Firefox every method returns a message
 * explaining that CDP is unavailable rather than throwing.
 */
@Component
public class NetworkTools {

    private static final int DEFAULT_LOG_LIMIT = 100;
    private static final long DEFAULT_IDLE_MILLIS = 500;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final Tools tools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NetworkTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Start capturing network traffic (method, URL, headers, bodies, status, timing) for the "
            + "current page. Requires Chrome or Edge. Call getNetworkLog to read captured entries.")
    public String startNetworkCapture(
            @McpToolParam(description = "Only capture requests whose URL matches this regex. Omit to capture everything.", required = false)
            String urlPattern) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.startNetworkCapture(urlPattern);
                return "Network capture started" + (urlPattern != null && !urlPattern.isBlank() ? " for URLs matching: " + urlPattern : ".");
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to start network capture: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Stop capturing new network traffic. Already-captured entries remain available via getNetworkLog.")
    public String stopNetworkCapture() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.stopNetworkCapture();
                return "Network capture stopped.";
            } catch (Exception e) {
                return "Failed to stop network capture: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get captured network requests/responses as a JSON array, most recent last. "
            + "Each entry has method, url, requestHeaders, requestBody, status, responseHeaders, responseBody, "
            + "durationMillis, and mocked. Bodies over ~20,000 characters are truncated.")
    public String getNetworkLog(
            @McpToolParam(description = "Only return entries whose URL matches this regex. Omit for all.", required = false)
            String urlPattern,
            @McpToolParam(description = "Only return entries with this HTTP method (e.g. GET, POST). Omit for all.", required = false)
            String method,
            @McpToolParam(description = "Max number of entries to return, most recent. Defaults to 100.", required = false)
            Integer limit) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                List<NetworkMonitor.NetworkEntry> entries =
                        tools.getNetworkLog(urlPattern, method, limit == null ? DEFAULT_LOG_LIMIT : limit);
                return toJson(entries.stream().map(this::toMap).toList());
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to read network log: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Clear the captured network log without stopping capture.")
    public String clearNetworkLog() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.clearNetworkLog();
                return "Network log cleared.";
            } catch (Exception e) {
                return "Failed to clear network log: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until there have been no in-flight HTTP requests for a continuous idle period, "
            + "e.g. after clicking a button that triggers one or more XHR/fetch calls. Requires Chrome or Edge. "
            + "Useful in place of a fixed sleep before asserting on the resulting DOM state.")
    public String waitForNetworkIdle(
            @McpToolParam(description = "How many milliseconds of continuous silence count as idle. Defaults to 500.", required = false)
            Long idleMillis,
            @McpToolParam(description = "Max seconds to wait overall. Defaults to 30.", required = false)
            Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForNetworkIdle(idleMillis == null ? DEFAULT_IDLE_MILLIS : idleMillis,
                        timeoutSeconds == null ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds);
                return "Network is idle.";
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Timed out waiting for network idle: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the number of HTTP requests currently in flight. Requires Chrome or Edge; "
            + "arms passive network tracking as a side effect if it wasn't already active.")
    public String getPendingRequestCount() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.armNetworkTracking();
                return String.valueOf(tools.getPendingRequestCount());
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to read pending request count: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Stub out responses for requests whose URL matches a regex, e.g. to fake a backend "
            + "response in a UI test without a real server. Requires Chrome or Edge. Matching requests never "
            + "hit the network. Rules are checked in the order added; the first match wins.")
    public String mockResponse(
            @McpToolParam(description = "Regex matched against the full request URL.", required = true)
            String urlPattern,
            @McpToolParam(description = "Only mock this HTTP method (e.g. GET, POST). Omit to match any method.", required = false)
            String method,
            @McpToolParam(description = "HTTP status code to return, e.g. 200 or 404.", required = true)
            Integer status,
            @McpToolParam(description = "Content-Type header of the mocked response, e.g. application/json.", required = false)
            String contentType,
            @McpToolParam(description = "Response body to return.", required = false)
            String body,
            @McpToolParam(description = "Extra response headers as a JSON object, e.g. {\"X-Test\": \"1\"}. Omit for none.", required = false)
            String headersJson) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.mockResponse(urlPattern, method, status, contentType, body, parseHeaders(headersJson));
                return "Mock registered for requests matching: " + urlPattern;
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to register mock: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Remove all registered response mocks.")
    public String clearMockResponses() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.clearMockResponses();
                return "All mocks cleared.";
            } catch (Exception e) {
                return "Failed to clear mocks: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Block requests whose URL matches a regex; matching requests get an immediate 403 "
            + "response instead of reaching the network. Requires Chrome or Edge. Useful for blocking ads, "
            + "trackers, or third-party calls that would otherwise slow down or flake a test.")
    public String blockRequests(
            @McpToolParam(description = "Regex matched against the full request URL.", required = true)
            String urlPattern) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.blockRequests(urlPattern);
                return "Blocking requests matching: " + urlPattern;
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to block requests: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Remove all URL blocking rules.")
    public String clearBlockedRequests() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.clearBlockedRequests();
                return "All blocking rules cleared.";
            } catch (Exception e) {
                return "Failed to clear blocking rules: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Simulate network conditions (offline mode, latency, throughput) via Chrome DevTools "
            + "Protocol. Requires Chrome or Edge.")
    public String setNetworkConditions(
            @McpToolParam(description = "Simulate the browser being offline. Defaults to false.", required = false)
            Boolean offline,
            @McpToolParam(description = "Simulated round-trip latency in milliseconds. Defaults to 0.", required = false)
            Long latencyMillis,
            @McpToolParam(description = "Simulated download throughput in kb/s. Use -1 for no limit. Defaults to -1.", required = false)
            Integer downloadThroughputKbps,
            @McpToolParam(description = "Simulated upload throughput in kb/s. Use -1 for no limit. Defaults to -1.", required = false)
            Integer uploadThroughputKbps) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.setNetworkConditions(Boolean.TRUE.equals(offline), latencyMillis == null ? 0 : latencyMillis,
                        downloadThroughputKbps == null ? -1 : downloadThroughputKbps,
                        uploadThroughputKbps == null ? -1 : uploadThroughputKbps);
                return "Network conditions applied.";
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to set network conditions: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the currently simulated network conditions.")
    public String getNetworkConditions() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getNetworkConditions();
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to read network conditions: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Reset simulated network conditions back to normal (no throttling, online).")
    public String clearNetworkConditions() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.clearNetworkConditions();
                return "Network conditions reset to normal.";
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to reset network conditions: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Register HTTP Basic auth credentials so any auth challenge (a browser login popup) "
            + "is answered automatically instead of blocking navigation. Requires Chrome or Edge.")
    public String setBasicAuthCredentials(
            @McpToolParam(description = "Username to supply for HTTP Basic auth challenges.", required = true)
            String username,
            @McpToolParam(description = "Password to supply for HTTP Basic auth challenges.", required = true)
            String password) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.setBasicAuthCredentials(username, password);
                return "Basic auth credentials registered.";
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to register basic auth credentials: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Start capturing browser console output (console.log/warn/error and similar). Requires "
            + "Chrome or Edge. Call getConsoleLogs to read captured entries.")
    public String startConsoleCapture() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.startConsoleCapture();
                return "Console log capture started.";
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to start console log capture: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get captured browser console log entries as a JSON array, most recent last. "
            + "Each entry has timestamp, type (e.g. log, warning, error), and message.")
    public String getConsoleLogs(
            @McpToolParam(description = "Only return entries of this console message type (e.g. error, warning, log). Omit for all.", required = false)
            String type,
            @McpToolParam(description = "Max number of entries to return, most recent. Defaults to 100.", required = false)
            Integer limit) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                List<ConsoleLogMonitor.ConsoleLogEntry> entries =
                        tools.getConsoleLogs(type, limit == null ? DEFAULT_LOG_LIMIT : limit);
                return toJson(entries.stream().map(this::toMap).toList());
            } catch (UnsupportedOperationException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "Failed to read console logs: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Clear captured console log entries without stopping capture.")
    public String clearConsoleLogs() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.clearConsoleLogs();
                return "Console log cleared.";
            } catch (Exception e) {
                return "Failed to clear console log: " + e.getMessage();
            }
        }
    }

    private Map<String, Object> toMap(NetworkMonitor.NetworkEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", entry.timestamp().toString());
        map.put("method", entry.method());
        map.put("url", entry.url());
        map.put("requestHeaders", entry.requestHeaders());
        map.put("requestBody", entry.requestBody());
        map.put("status", entry.status());
        map.put("responseHeaders", entry.responseHeaders());
        map.put("responseBody", entry.responseBody());
        map.put("durationMillis", entry.durationMillis());
        map.put("mocked", entry.mocked());
        return map;
    }

    private Map<String, Object> toMap(ConsoleLogMonitor.ConsoleLogEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", entry.timestamp().toString());
        map.put("type", entry.type());
        map.put("message", entry.message());
        return map;
    }

    private Map<String, String> parseHeaders(String headersJson) throws Exception {
        if (headersJson == null || headersJson.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(headersJson, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, String.class));
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
