package uk.xpathy.selenium.mcp.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiClientTest {

    private final ApiClient client = new ApiClient();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            exchange.getResponseHeaders().add("X-Trace", "abc");
            respond(exchange, 201, exchange.getRequestMethod() + "|" + auth + "|" + body);
        });
        server.createContext("/missing", exchange -> respond(exchange, 404, "not here"));
        server.createContext("/old", exchange -> {
            exchange.getResponseHeaders().add("Location", "/echo");
            respond(exchange, 302, "");
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsMethodHeadersAndBodyAndReturnsTheResponse() {
        ApiClient.ApiResponse response = client.send("post", baseUrl + "/echo",
                Map.of("Authorization", "Bearer t"), "{\"a\":1}", null);

        assertEquals(201, response.status());
        assertEquals("POST|Bearer t|{\"a\":1}", response.body());
        assertEquals("abc", response.headers().get("x-trace"));
        assertTrue(response.durationMillis() >= 0);
    }

    @Test
    void aRequestWithoutBodyOrHeadersSendsNothingExtra() {
        ApiClient.ApiResponse response = client.send("GET", baseUrl + "/echo", null, null, 5);

        assertEquals("GET|null|", response.body());
    }

    @Test
    void errorStatusesAreReturnedNotThrown() {
        ApiClient.ApiResponse response = client.send("GET", baseUrl + "/missing", null, null, null);

        assertEquals(404, response.status());
        assertEquals("not here", response.body());
    }

    @Test
    void redirectsAreFollowed() {
        ApiClient.ApiResponse response = client.send("GET", baseUrl + "/old", null, null, null);

        assertEquals(201, response.status());
        assertEquals("GET|null|", response.body());
    }

    @Test
    void aConnectionFailureIsWrappedWithItsCause() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> client.send("GET", "http://127.0.0.1:" + closedPort + "/", null, null, 5));

        assertTrue(e.getMessage().startsWith("HTTP request failed"), e.getMessage());
        assertTrue(e.getCause() instanceof IOException, String.valueOf(e.getCause()));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
