package com.voltajeba.selenium.mcp.tools;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import com.voltajeba.selenium.mcp.webdriver.AutoWaitSettings;
import com.voltajeba.selenium.mcp.webdriver.BrowserOptions;
import com.voltajeba.selenium.mcp.webdriver.Tools;
import com.voltajeba.selenium.mcp.webdriver.WaitRecord;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The session-wide auto-waits against a real headless Chrome, and the generated {@code Waits} helper
 * compiled and run against the same page. Opt-in like {@link ChromeIntegrationTest}:
 * {@code mvn test -Dselenium.it=true}.
 */
@EnabledIfSystemProperty(named = "selenium.it", matches = "true")
class ChromeAutoWaitIntegrationTest {

    private static final String PAGE = """
            <!doctype html><html><head><link rel="icon" href="data:,"><title>waits</title>
            <style>
              #box { opacity: 0; transition: opacity 400ms; }
              #box.on { opacity: 1; }
              #spinner { width: 10px; height: 10px; animation: spin 1s linear infinite; }
              @keyframes spin { to { transform: rotate(360deg); } }
            </style></head><body>
              <button id="load" onclick="setTimeout(() => fetch('/api/slow').then(r => r.text())
                  .then(t => document.getElementById('out').textContent = t), 50)">Load</button>
              <button id="hang" onclick="fetch('/api/hang')">Hang</button>
              <button id="fade" onclick="document.getElementById('box').classList.add('on')">Fade</button>
              <button id="nothing">Nothing</button>
              <div id="box">box</div>
              <div id="spinner"></div>
              <div id="out"></div>
            </body></html>
            """;

    private static HttpServer server;
    private static BrowserSessions sessions;
    private static Tools tools;
    private static String baseUrl;

    @BeforeAll
    static void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            try {
                if (path.equals("/api/slow")) {
                    Thread.sleep(600);
                } else if (path.equals("/api/hang")) {
                    Thread.sleep(2500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = (path.startsWith("/api") ? "ok" : PAGE).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", path.startsWith("/api") ? "text/plain" : "text/html");
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
        tools.setAutoWait(AutoWaitSettings.NONE);
        tools.navigateTo(baseUrl + "/");
        tools.clearWaitRecords();
    }

    @Test
    void withoutAWaitStrategyNothingIsWaitedFor() {
        tools.click(By.id("load"));

        assertEquals("", tools.getText(By.id("out")));
        assertEquals(null, tools.getLastWaitRecord());
    }

    @Test
    void theNetworkWaitCoversARequestTheActionSetsOffAMomentLater() {
        tools.setAutoWait(settings(0, 300, 0, List.of()));

        tools.click(By.id("load"));

        assertEquals("ok", tools.getText(By.id("out")));
        WaitRecord record = tools.getLastWaitRecord();
        assertEquals(1, record.requests());
        assertTrue(record.networkWaited().toMillis() >= 600, record.toString());
        assertFalse(record.networkTimedOut());
        assertFalse(record.trackerLate());
    }

    @Test
    void theAnimationWaitLetsATransitionFinishButIgnoresSpinners() {
        tools.setAutoWait(settings(0, 0, 200, List.of()));

        tools.click(By.id("fade"));

        assertEquals("1", tools.getCssValue(By.id("box"), "opacity"));
        WaitRecord record = tools.getLastWaitRecord();
        assertTrue(record.animations() >= 1, record.toString());
        assertFalse(record.animationTimedOut(), "the infinite spinner should not keep the page busy");
    }

    @Test
    void anActionThatSetsOffNothingNeedsNoWait() {
        tools.setAutoWait(settings(0, 200, 200, List.of()));

        String result = new BrowserTools(sessions).click("id", "nothing", null, null, null);

        assertEquals("Clicked element located by id=nothing", result);
        assertFalse(tools.getLastWaitRecord().needsWait());
    }

    @Test
    void ignoredUrlsDoNotCountAsNetworkActivity() {
        tools.setAutoWait(settings(0, 200, 0, List.of("/api/slow")));

        tools.click(By.id("load"));

        WaitRecord record = tools.getLastWaitRecord();
        assertEquals(0, record.requests());
        assertTrue(record.networkWaited().toMillis() < 600, record.toString());
    }

    @Test
    void aNetworkTimeoutCarriesOnAndSaysWhatWasBusy() {
        tools.setAutoWait(new AutoWaitSettings(Duration.ZERO, Duration.ofMillis(200), Duration.ofSeconds(1),
                Duration.ZERO, AutoWaitSettings.DEFAULT_TIMEOUT, List.of()));

        String result = new BrowserTools(sessions).click("id", "hang", null, null, null);

        WaitRecord record = tools.getLastWaitRecord();
        assertTrue(record.networkTimedOut(), record.toString());
        assertEquals(1, record.pending());
        assertTrue(result.contains("still busy when the wait timed out (1 request(s) in flight)"), result);
        assertTrue(result.contains("Waits.networkIdle(driver)"), result);
    }

    @Test
    void theImplicitWaitIsSeleniumsOwn() {
        tools.setAutoWait(settings(2, 0, 0, List.of()));
        tools.executeScript("setTimeout(() => { var b = document.createElement('button'); b.id = 'late';"
                + " document.body.appendChild(b); }, 500)");

        tools.click(By.id("late"));

        assertEquals(Duration.ofSeconds(2), tools.getDriver().manage().timeouts().getImplicitWaitTimeout());
    }

    @Test
    void theReportListsTheStepsThatNeedAWait() {
        tools.setAutoWait(settings(0, 300, 200, List.of()));
        tools.click(By.id("nothing"));
        tools.click(By.id("load"));
        tools.click(By.id("fade"));

        String report = new WaitTools(sessions).getWaitReport(true);

        assertTrue(report.contains("2 of 3 recorded action(s) need a wait"), report);
        assertTrue(report.contains("2. click By.id: load - set off 1 request(s)"), report);
        assertTrue(report.contains("3. click By.id: fade - set off"), report);
        assertTrue(tools.getWaitRecords().isEmpty());
    }

    @Test
    void theGeneratedWaitsHelperWaitsLikeTheSession(@TempDir Path dir) throws Exception {
        AutoWaitSettings settings = settings(0, 300, 200, List.of());
        Files.writeString(dir.resolve("Waits.java"), tools.generateWaitsHelper(settings));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int status = compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"),
                "-d", dir.toString(), dir.resolve("Waits.java").toString());
        assertEquals(0, status, "generated Waits helper did not compile");

        // A browser of its own, so the helper's tracker registration doesn't outlive this test in the shared one.
        Tools own = sessions.open("helper", t -> t.openBrowser("chrome", new BrowserOptions(true, false, "800x600", null)));
        WebDriver driver = own.getDriver();
        driver.get(baseUrl + "/");
        try (URLClassLoader loader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> waits = loader.loadClass("Waits");
            Method install = waits.getMethod("install", WebDriver.class);
            Method afterAction = waits.getMethod("afterAction", WebDriver.class);
            install.invoke(null, driver);

            driver.findElement(By.id("load")).click();
            afterAction.invoke(null, driver);
            assertEquals("ok", driver.findElement(By.id("out")).getText());

            driver.findElement(By.id("fade")).click();
            afterAction.invoke(null, driver);
            assertEquals("1", driver.findElement(By.id("box")).getCssValue("opacity"));

            driver.navigate().refresh();
            driver.findElement(By.id("load")).click();
            waits.getMethod("networkIdle", WebDriver.class).invoke(null, driver);
            assertEquals("ok", driver.findElement(By.id("out")).getText(), "the tracker should survive a reload");
        } finally {
            sessions.close("helper");
        }
    }

    private static AutoWaitSettings settings(int implicitSeconds, long networkIdleMillis, long animationIdleMillis,
                                             List<String> ignoredUrls) {
        return new AutoWaitSettings(Duration.ofSeconds(implicitSeconds), Duration.ofMillis(networkIdleMillis),
                AutoWaitSettings.DEFAULT_TIMEOUT, Duration.ofMillis(animationIdleMillis), AutoWaitSettings.DEFAULT_TIMEOUT,
                ignoredUrls);
    }
}
