package uk.xpathy.selenium.mcp.tools;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives several real headless Chromes through the MCP tool wrappers, as a client would. Opt-in like
 * {@link ChromeIntegrationTest}: {@code mvn test -Dselenium.it=true}. Kept separate from it because these
 * tests open, kill and reopen browsers rather than sharing one.
 */
@EnabledIfSystemProperty(named = "selenium.it", matches = "true")
class ChromeSessionsIntegrationTest {

    private final BrowserSessions sessions = new BrowserSessions(5);
    private final BrowserTools browser = new BrowserTools(sessions);
    private final WindowTools windows = new WindowTools(sessions);
    private final CookieTools cookies = new CookieTools(sessions);
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "<!doctype html><title>page</title>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterEach
    void stop() {
        sessions.closeAll();
        server.stop(0);
    }

    @Test
    void eachSessionIsItsOwnBrowser() {
        openHeadless(null);
        browser.navigate(baseUrl);
        cookies.addCookie("who", "first", null, null);

        assertEquals("chrome browser opened as session 'second'.", openHeadless("second"));
        browser.navigate(baseUrl);
        assertEquals("No cookie named who found.", cookies.getCookie("who"));

        browser.switchSession("default");
        assertTrue(cookies.getCookie("who").startsWith("who=first"), cookies.getCookie("who"));
        assertEquals("[default] page", browser.getTitle());
    }

    @Test
    void aBrowserWhoseLastWindowWasClosedIsDroppedAndCanBeReopened() {
        openHeadless(null);
        browser.navigate(baseUrl);

        windows.closeCurrentWindow();

        assertFalse(sessions.active().isBrowserAlive());
        String title = browser.getTitle();
        assertTrue(title.endsWith(ErrorMessages.BROWSER_GONE_HINT), title);

        String reopened = openHeadless(null);
        assertTrue(reopened.startsWith("Session 'default' was removed: the browser is no longer reachable"), reopened);
        assertTrue(reopened.endsWith("chrome browser opened."), reopened);
        assertTrue(sessions.active().isBrowserAlive());
        assertEquals("Navigated to " + baseUrl, browser.navigate(baseUrl));
    }

    private String openHeadless(String session) {
        return BrowserSessionsTest.withoutWaitStrategy(
                browser.openBrowser("chrome", true, null, null, null, null, null, null, session));
    }
}
