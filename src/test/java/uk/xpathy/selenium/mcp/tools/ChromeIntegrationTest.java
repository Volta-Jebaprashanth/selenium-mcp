package uk.xpathy.selenium.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ScriptTimeoutException;
import uk.xpathy.selenium.mcp.webdriver.BrowserOptions;
import uk.xpathy.selenium.mcp.webdriver.ConsoleLogMonitor;
import uk.xpathy.selenium.mcp.webdriver.NetworkMonitor;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives a real headless Chrome against a local HTTP server. Opt-in, since it needs Chrome
 * installed: {@code mvn test -Dselenium.it=true}.
 */
@EnabledIfSystemProperty(named = "selenium.it", matches = "true")
class ChromeIntegrationTest {

    // Every page carries an inline icon so Chrome never requests /favicon.ico. Once a network test arms
    // the interceptor it stays armed for the shared browser, and on Linux Chrome never answers Selenium's
    // Fetch.getResponseBody for that browser-initiated request: each page load then stalls on 30s CDP timeouts.
    private static final String PAGE = """
            <!doctype html><html><head><link rel="icon" href="data:,"><title>it</title></head>
            <body style="margin:0">
              <button id="hidden" class="b" style="display:none" onclick="window.clicked='hidden'">h</button>
              <button id="first" class="b" onclick="window.clicked='first'">1</button>
              <button id="second" class="b" onclick="window.clicked='second'">2</button>
              <div style="height:3000px;background:linear-gradient(red,blue)"></div>
            </body></html>
            """;

    private static final String CONTROLS_PAGE = """
            <!doctype html><html><head><link rel="icon" href="data:,"><title>Create Put</title></head><body>
              <button id="save" disabled><span>Create Put</span></button>
              <button id="go"><span class="icon"></span> Go   now </button>
              <input id="nosave" type="submit" value="Don't save">
              <div id="plain">Just text</div>
            </body></html>
            """;

    private static final String OVERLAY_PAGE = """
            <!doctype html><html><head><link rel="icon" href="data:,"><title>overlay</title></head><body>
              <button id="under" onclick="window.clicked='under'">Under</button>
              <div id="overlay" class="loading backdrop" style="position:fixed;inset:0;background:rgba(0,0,0,.3)"></div>
            </body></html>
            """;

    private static HttpServer server;
    private static BrowserSessions sessions;
    private static Tools tools;
    private static String baseUrl;

    @BeforeAll
    static void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            boolean api = path.startsWith("/api");
            String page = switch (path) {
                case "/controls" -> CONTROLS_PAGE;
                case "/overlay" -> OVERLAY_PAGE;
                default -> PAGE;
            };
            byte[] body = (api ? "{\"ok\":true}" : page).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", api ? "application/json" : "text/html");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        sessions = new BrowserSessions(5);
        tools = sessions.open(BrowserSessions.DEFAULT_SESSION,
                t -> t.openBrowser("chrome", new BrowserOptions(true, false, "800x600", null)));
    }

    @AfterAll
    static void stop() {
        if (sessions != null) sessions.closeAll();
        if (server != null) server.stop(0);
    }

    @BeforeEach
    void load() {
        tools.navigateTo(baseUrl + "/");
    }

    @Test
    void scriptErrorsAreOneLine() {
        String result = new ScriptTools(sessions).executeScript("return undefined.click()", null);
        assertTrue(result.startsWith("Script error: javascript error:"), result);
        assertTrue(result.contains("reading 'click'"), result);
        assertFalse(result.contains("Build info") || result.contains("Capabilities") || result.contains("\n"), result);
    }

    @Test
    void scriptResultsComeBackAsJson() {
        assertEquals("{\"a\":1,\"b\":[\"x\"]}", new ScriptTools(sessions).executeScript("return {a: 1, b: ['x']}", null));
    }

    @Test
    void scriptTimeoutIsConfigurableAndRestored() {
        String slow = "return new Promise(r => setTimeout(() => r('done'), 1500))";
        assertThrows(ScriptTimeoutException.class, () -> tools.executeScript(slow, Duration.ofMillis(300)));
        assertEquals("done", tools.executeScript(slow, Duration.ofSeconds(10)));
        assertEquals(Duration.ofSeconds(30), tools.getDriver().manage().timeouts().getScriptTimeout());
    }

    @Test
    void fullPageScreenshotCoversTheWholeDocument() throws Exception {
        BufferedImage viewport = decode(tools.takeScreenshotBase64());
        BufferedImage full = decode(tools.takeFullPageScreenshotBase64());
        assertTrue(viewport.getHeight() < 1000, "viewport height " + viewport.getHeight());
        assertTrue(full.getHeight() >= 3000, "full-page height " + full.getHeight());
    }

    @Test
    void indexAndVisibleOnlyPickTheRightMatch() {
        tools.click(tools.locator("css", ".b", 1, true));
        assertEquals("second", tools.executeScript("return window.clicked"));
        assertEquals("1", tools.getText(tools.locator("css", ".b", 0, true)));
        assertThrows(org.openqa.selenium.NoSuchElementException.class,
                () -> tools.click(tools.locator("css", ".b", 5, false)));
    }

    @Test
    void initScriptsSurviveNavigation() {
        String id = tools.addInitScript("window.__helper = 42;", true);
        try {
            assertEquals(42L, tools.executeScript("return window.__helper"));
            tools.refresh();
            assertEquals(42L, tools.executeScript("return window.__helper"));
        } finally {
            tools.removeInitScript(id);
        }
        tools.refresh();
        assertEquals(null, tools.executeScript("return window.__helper"));
    }

    @Test
    void consoleCaptureWorksOnTheInstalledChrome() throws Exception {
        tools.startConsoleCapture();
        tools.executeScript("console.error('boom-from-test')");
        List<ConsoleLogMonitor.ConsoleLogEntry> entries = List.of();
        for (int i = 0; i < 50 && entries.isEmpty(); i++) {
            Thread.sleep(100);
            entries = tools.getConsoleLogs(null, 100).stream()
                    .filter(e -> e.message().contains("boom-from-test")).toList();
        }
        assertFalse(entries.isEmpty(), "console.error was not captured");
    }

    @Test
    void networkCaptureWorksOnTheInstalledChrome() throws Exception {
        tools.startNetworkCapture("/api");
        tools.executeScript("return fetch('/api/ping').then(r => r.text())");
        List<NetworkMonitor.NetworkEntry> entries = List.of();
        for (int i = 0; i < 50 && entries.isEmpty(); i++) {
            Thread.sleep(100);
            entries = tools.getNetworkLog("/api/ping", null, 10);
        }
        assertFalse(entries.isEmpty(), "fetch was not captured");
        assertEquals(200, entries.get(0).status());
        tools.stopNetworkCapture();
    }

    @Test
    void networkMocksAndBlocksApplyToPageRequests() {
        tools.mockResponse("/api/mocked", "GET", 418, "application/json", "{\"mocked\":true}", null);
        tools.blockRequests("/api/blocked");
        try {
            assertEquals("418 {\"mocked\":true}", tools.executeScript(
                    "return fetch('/api/mocked').then(r => r.text().then(t => r.status + ' ' + t))"));
            String blocked = (String) tools.executeScript(
                    "return fetch('/api/blocked').then(r => r.text().then(t => r.status + ' ' + t))");
            assertTrue(blocked.startsWith("403 Blocked by selenium-mcp"), blocked);
            assertEquals("{\"ok\":true}", tools.executeScript("return fetch('/api/ping').then(r => r.text())"));
        } finally {
            tools.clearMockResponses();
            tools.clearBlockedRequests();
        }
    }

    @Test
    void pageElementsDescribeTheLivePage() throws Exception {
        ObjectMapper json = new ObjectMapper();
        assertTrue(tools.getPageElements(20).contains("\"first\""));

        JsonNode filtered = json.readTree(tools.getPageElementsFiltered("css", ".b", true, false, false, 20, 2));
        assertEquals(3, filtered.get("totalMatches").asInt());
        assertEquals(2, filtered.get("returnedMatches").asInt());
        assertTrue(filtered.get("truncated").asBoolean());
    }

    @Test
    void textLocatorsFindElementsByWhatTheySay() {
        tools.navigateTo(baseUrl + "/controls");

        assertEquals("save", tools.getAttribute(tools.locator("text", "Create Put", null, false), "id"));
        assertEquals("false", new ElementQueryTools(sessions).isEnabled("text", "Create Put", null, null));
        assertEquals("go", tools.getAttribute(tools.locator("text", "Go now", null, false), "id"));
        assertEquals("go", tools.getAttribute(tools.locator("partialText", "Go", null, false), "id"));
        assertEquals("nosave", tools.getAttribute(tools.locator("text", "Don't save", null, false), "id"));
        assertEquals("plain", tools.getAttribute(tools.locator("text", "Just text", null, false), "id"));
        assertEquals(1, tools.countElements(tools.locator("text", "Create Put", null, false)));

        String missing = new ElementQueryTools(sessions).getText("text", "Nope", null, null);
        assertTrue(missing.contains("By.text: Nope"), missing);
    }

    @Test
    void anOverlayMakesAButtonUnclickableUntilItGoes() throws Exception {
        tools.navigateTo(baseUrl + "/overlay");
        By under = tools.locator("id", "under", null, false);

        assertEquals("false: covered by div#overlay.loading.backdrop", tools.checkClickable(under).describe());
        String waited = new WaitTools(sessions).waitForClickable("id", "under", 1, null, null);
        assertTrue(waited.contains("covered by div#overlay"), waited);
        JsonNode match = new ObjectMapper().readTree(tools.getPageElementsFiltered(under, false, false, false, 20, 5))
                .get("matches").get(0);
        assertTrue(match.get("visible").asBoolean());
        assertTrue(match.get("enabled").asBoolean());
        assertFalse(match.get("clickable").asBoolean());
        assertEquals("div#overlay.loading.backdrop", match.get("coveredBy").asText());
        assertThrows(ElementClickInterceptedException.class, () -> tools.click(under, Duration.ZERO));

        tools.executeScript("setTimeout(() => document.getElementById('overlay').remove(), 700)");
        assertEquals("Clicked element located by id=under",
                new BrowserTools(sessions).click("id", "under", null, null, 5));
        assertEquals("under", tools.executeScript("return window.clicked"));
        assertEquals("true", tools.checkClickable(under).describe());
    }

    @Test
    void screenshotsComeBackAsImages() {
        CallToolResult result = new ScreenshotTools(sessions).takeScreenshot(null, false);

        assertNotEquals(Boolean.TRUE, result.isError());
        assertEquals("image/png", assertInstanceOf(ImageContent.class, result.content().getFirst()).mimeType());
    }

    @Test
    void browserToolsReportFailuresInsteadOfThrowing() {
        String result = new BrowserTools(sessions).navigate("not a url");
        assertTrue(result.startsWith("Failed to navigate: "), result);
        assertFalse(result.contains("Build info"), result);
    }

    private static BufferedImage decode(String base64) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    }
}
