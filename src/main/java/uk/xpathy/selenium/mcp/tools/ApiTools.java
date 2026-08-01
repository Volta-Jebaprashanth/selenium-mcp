package uk.xpathy.selenium.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.http.ApiClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP-facing wrapper around {@link ApiClient} for REST API automation. Unlike every other
 * {@code @McpTool} class in this package, these methods don't touch the browser session at
 * all — no {@code openBrowser} precondition, nothing to synchronize on — so an MCP client can
 * exercise a backend API directly, whether or not a browser is open. Handy for seeding/verifying
 * test data around a UI flow, or for testing a service that has no UI.
 */
@Component
public class ApiTools {

    private static final String HEADERS_DESC =
            "Request headers as a JSON object, e.g. {\"Authorization\": \"Bearer xyz\", \"Content-Type\": \"application/json\"}. Omit for none.";

    private final ApiClient apiClient = new ApiClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @McpTool(description = "Send an HTTP GET request and return the response as JSON (status, headers, body, durationMillis).")
    public String httpGet(
            @McpToolParam(description = "The URL to request", required = true) String url,
            @McpToolParam(description = HEADERS_DESC, required = false) String headersJson,
            @McpToolParam(description = "Request timeout in seconds. Defaults to 30.", required = false) Integer timeoutSeconds) {
        return request("GET", url, headersJson, null, timeoutSeconds);
    }

    @McpTool(description = "Send an HTTP DELETE request and return the response as JSON (status, headers, body, durationMillis).")
    public String httpDelete(
            @McpToolParam(description = "The URL to request", required = true) String url,
            @McpToolParam(description = HEADERS_DESC, required = false) String headersJson,
            @McpToolParam(description = "Request timeout in seconds. Defaults to 30.", required = false) Integer timeoutSeconds) {
        return request("DELETE", url, headersJson, null, timeoutSeconds);
    }

    @McpTool(description = "Send an HTTP POST request with a body and return the response as JSON (status, headers, body, durationMillis).")
    public String httpPost(
            @McpToolParam(description = "The URL to request", required = true) String url,
            @McpToolParam(description = "The request body to send, e.g. a JSON string", required = false) String body,
            @McpToolParam(description = HEADERS_DESC, required = false) String headersJson,
            @McpToolParam(description = "Request timeout in seconds. Defaults to 30.", required = false) Integer timeoutSeconds) {
        return request("POST", url, headersJson, body, timeoutSeconds);
    }

    @McpTool(description = "Send an HTTP PUT request with a body and return the response as JSON (status, headers, body, durationMillis).")
    public String httpPut(
            @McpToolParam(description = "The URL to request", required = true) String url,
            @McpToolParam(description = "The request body to send, e.g. a JSON string", required = false) String body,
            @McpToolParam(description = HEADERS_DESC, required = false) String headersJson,
            @McpToolParam(description = "Request timeout in seconds. Defaults to 30.", required = false) Integer timeoutSeconds) {
        return request("PUT", url, headersJson, body, timeoutSeconds);
    }

    @McpTool(description = "Send an HTTP PATCH request with a body and return the response as JSON (status, headers, body, durationMillis).")
    public String httpPatch(
            @McpToolParam(description = "The URL to request", required = true) String url,
            @McpToolParam(description = "The request body to send, e.g. a JSON string", required = false) String body,
            @McpToolParam(description = HEADERS_DESC, required = false) String headersJson,
            @McpToolParam(description = "Request timeout in seconds. Defaults to 30.", required = false) Integer timeoutSeconds) {
        return request("PATCH", url, headersJson, body, timeoutSeconds);
    }

    @McpTool(description = "Send an HTTP request with an arbitrary method (e.g. HEAD, OPTIONS) and return the "
            + "response as JSON (status, headers, body, durationMillis). Prefer httpGet/httpPost/httpPut/httpPatch/httpDelete "
            + "for the common verbs.")
    public String httpRequest(
            @McpToolParam(description = "The HTTP method, e.g. GET, POST, HEAD, OPTIONS", required = true) String method,
            @McpToolParam(description = "The URL to request", required = true) String url,
            @McpToolParam(description = "The request body to send. Omit for none.", required = false) String body,
            @McpToolParam(description = HEADERS_DESC, required = false) String headersJson,
            @McpToolParam(description = "Request timeout in seconds. Defaults to 30.", required = false) Integer timeoutSeconds) {
        return request(method, url, headersJson, body, timeoutSeconds);
    }

    private String request(String method, String url, String headersJson, String body, Integer timeoutSeconds) {
        try {
            Map<String, String> headers = parseHeaders(headersJson);
            ApiClient.ApiResponse response = apiClient.send(method, url, headers, body, timeoutSeconds);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", response.status());
            result.put("headers", response.headers());
            result.put("body", response.body());
            result.put("durationMillis", response.durationMillis());
            return objectMapper.writeValueAsString(result);
        } catch (IllegalArgumentException e) {
            return "Invalid URL: " + e.getMessage();
        } catch (Exception e) {
            return "Request failed: " + e.getMessage();
        }
    }

    private Map<String, String> parseHeaders(String headersJson) throws Exception {
        if (headersJson == null || headersJson.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(headersJson, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, String.class));
    }
}
