package uk.xpathy.selenium.mcp.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A plain-Java HTTP client for REST API automation, built on the JDK's {@link HttpClient}.
 * Stateless and thread-safe; a single instance can be reused for every call.
 */
public class ApiClient {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public record ApiResponse(int status, Map<String, String> headers, String body, long durationMillis) {
    }

    /**
     * Sends an HTTP request and returns the response. {@code body} is only attached when
     * non-null; {@code method} values like GET/DELETE are sent with no body if {@code body}
     * is null, but can still carry one if explicitly provided.
     */
    public ApiResponse send(String method, String url, Map<String, String> headers, String body, Integer timeoutSeconds) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds == null ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds));

            if (headers != null) {
                headers.forEach(builder::header);
            }

            HttpRequest.BodyPublisher publisher = (body == null)
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);
            builder.method(method.toUpperCase(), publisher);

            long start = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;

            Map<String, String> responseHeaders = new LinkedHashMap<>();
            response.headers().map().forEach((name, values) -> responseHeaders.put(name, String.join(", ", values)));

            return new ApiResponse(response.statusCode(), responseHeaders, response.body(), duration);
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP request interrupted", e);
        }
    }
}
