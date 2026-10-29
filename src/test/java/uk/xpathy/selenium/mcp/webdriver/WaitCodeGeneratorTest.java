package uk.xpathy.selenium.mcp.webdriver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitCodeGeneratorTest {

    private final WaitCodeGenerator generator = new WaitCodeGenerator();

    private final AutoWaitSettings full = new AutoWaitSettings(Duration.ofSeconds(5), Duration.ofMillis(500),
            Duration.ofSeconds(15), Duration.ofMillis(250), Duration.ofMillis(7500), List.of("/poll", "say \"hi\""));

    @Test
    void theHelperCompilesForOldJava(@TempDir Path dir) throws Exception {
        Path source = dir.resolve("Waits.java");
        Files.writeString(source, generator.helperClass(full));
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        int status = compiler.run(null, null, errors, "--release", "11", "-Xlint:-options",
                "-classpath", System.getProperty("java.class.path"), "-d", dir.toString(), source.toString());

        assertEquals(0, status, errors.toString());
    }

    @Test
    void theHelperCarriesTheSessionsIdleTimesAndTimeouts() {
        String helper = generator.helperClass(full);

        assertTrue(helper.contains("NETWORK_IDLE_MILLIS = 500;"), helper);
        assertTrue(helper.contains("NETWORK_TIMEOUT = Duration.ofSeconds(15);"), helper);
        assertTrue(helper.contains("ANIMATION_IDLE_MILLIS = 250;"), helper);
        assertTrue(helper.contains("ANIMATION_TIMEOUT = Duration.ofMillis(7500);"), helper);
        assertTrue(helper.contains("['/poll', 'say \\\"hi\\\"']"), helper);
    }

    @Test
    void theHelperEmbedsTheTrackerTheSessionRuns() {
        String helper = generator.helperClass(AutoWaitSettings.NONE);

        assertTrue(helper.contains("t.restartIdle = function (what) {"), helper);
        assertTrue(helper.contains("return interactState(arguments[0], true);"), helper);
        assertFalse(helper.contains("// Tracks in-flight"), "comment-only lines are left out");
        assertTrue(helper.contains("NETWORK_IDLE_MILLIS = " + WaitCodeGenerator.DEFAULT_NETWORK_IDLE_MILLIS + ";"),
                "a wait the session has off still gets a usable default");
    }

    @Test
    void setupMatchesTheStrategy() {
        assertEquals("driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));\nWaits.install(driver);\n",
                generator.setup(full));
        assertTrue(generator.setup(AutoWaitSettings.NONE).startsWith("// No wait strategy"));
        AutoWaitSettings implicitOnly = new AutoWaitSettings(Duration.ofMillis(1500), Duration.ZERO,
                AutoWaitSettings.DEFAULT_TIMEOUT, Duration.ZERO, AutoWaitSettings.DEFAULT_TIMEOUT, List.of());
        assertEquals("driver.manage().timeouts().implicitlyWait(Duration.ofMillis(1500));\n", generator.setup(implicitOnly));
    }

    @Test
    void javaStringsEscapeWhatJavaNeeds() {
        assertEquals("\"a\\\\b \\\"c\\\" \\t\"", WaitCodeGenerator.javaString("a\\b \"c\" \t"));
    }
}
