package uk.xpathy.selenium.mcp.webdriver;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class AutoWaiterTest {

    /** Queued in place of a status: the page has no tracker, so the status script returns null. */
    private static final Object NO_TRACKER = new Object();

    private final List<String> scripts = new ArrayList<>();
    private final Deque<Object> statuses = new ArrayDeque<>();
    private final WebDriver driver = mock(WebDriver.class, withSettings()
            .extraInterfaces(JavascriptExecutor.class)
            .defaultAnswer(invocation -> {
                String method = invocation.getMethod().getName();
                if (method.equals("manage")) {
                    return mock(WebDriver.Options.class, Answers.RETURNS_DEEP_STUBS);
                }
                if (!method.equals("executeScript")) {
                    return null;
                }
                String script = invocation.getArgument(0);
                scripts.add(script);
                if (script.contains("t.status(")) {
                    Object next = statuses.isEmpty() ? status(true, true, 0, 0, 0) : statuses.poll();
                    return next == NO_TRACKER ? null : next;
                }
                return script.contains("t.mark()") ? Boolean.TRUE : null;
            }));
    private final AutoWaiter waiter = new AutoWaiter(driver);

    @Test
    void withNoStrategyAnActionRunsWithoutTouchingThePage() {
        waiter.run("click", () -> { });

        assertTrue(scripts.isEmpty());
        assertNull(waiter.lastRecord());
        assertTrue(waiter.records().isEmpty());
    }

    @Test
    void aCoveredClickIsRecordedEvenWithNoStrategy() {
        waiter.runCoverable("click By.id: save", () -> new ElementInteractor.Covered("div.loader", Duration.ofMillis(1200)));

        WaitRecord record = waiter.lastRecord();
        assertEquals("div.loader", record.coveredBy());
        assertEquals(Duration.ofMillis(1200), record.coveredFor());
        assertFalse(record.tracked());
        assertTrue(record.needsClickableWait());
        assertTrue(scripts.isEmpty());
    }

    @Test
    void anActionIsMarkedThenWaitedOnUntilTheNetworkIsIdle() {
        waiter.apply(settings(200, 0));
        scripts.clear();
        statuses.add(status(false, true, 1, 1, 0));
        statuses.add(status(true, true, 0, 1, 0));

        waiter.run("click By.id: load", () -> { });

        assertTrue(scripts.getFirst().contains("t.mark()"), scripts.toString());
        assertTrue(scripts.get(1).contains("restartIdle"), scripts.toString());
        WaitRecord record = waiter.lastRecord();
        assertEquals(1, record.requests());
        assertTrue(record.needsNetworkWait());
        assertFalse(record.networkTimedOut());
        assertEquals(1, record.step());
    }

    @Test
    void aPageWithoutTheTrackerGetsItInstalledAndIsReportedLate() {
        waiter.apply(settings(200, 0));
        statuses.add(NO_TRACKER);
        statuses.add(status(true, true, 0, 0, 0));

        waiter.run("navigate", () -> { });

        assertTrue(waiter.lastRecord().trackerLate());
        assertTrue(scripts.stream().filter(s -> s.contains("__seleniumMcpWait = t")).count() >= 2,
                "tracker should be installed on apply and again on the new page");
    }

    @Test
    void aTimeoutIsRecordedRatherThanThrown() {
        waiter.apply(new AutoWaitSettings(Duration.ZERO, Duration.ofMillis(200), Duration.ofMillis(150),
                Duration.ZERO, AutoWaitSettings.DEFAULT_TIMEOUT, List.of()));
        for (int i = 0; i < 100; i++) {
            statuses.add(status(false, true, 2, 2, 0));
        }

        waiter.run("click", () -> { });

        WaitRecord record = waiter.lastRecord();
        assertTrue(record.networkTimedOut());
        assertEquals(2, record.pending());
    }

    @Test
    void clearingRestartsTheStepCount() {
        waiter.apply(settings(0, 100));
        waiter.run("a", () -> { });
        waiter.run("b", () -> { });
        assertEquals(2, waiter.records().getLast().step());

        waiter.clearRecords();
        waiter.run("c", () -> { });

        assertEquals(1, waiter.records().size());
        assertEquals(1, waiter.lastRecord().step());
    }

    @Test
    void theIgnoreListIsPassedToThePageAsJavaScriptStrings() {
        String source = AutoWaiter.trackerSource(List.of("/poll", "it's", "</script>"));

        assertTrue(source.startsWith("window.__seleniumMcpWaitIgnore = ['/poll', 'it\\'s', '\\x3c/script>'];\n"), source);
    }

    @Test
    void settingsRejectNegativeDurations() {
        assertThrows(IllegalArgumentException.class, () -> new AutoWaitSettings(Duration.ofSeconds(-1),
                Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, List.of()));
        AutoWaitSettings blanks = new AutoWaitSettings(Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO,
                Duration.ZERO, List.of(" ", " /poll "));
        assertEquals(List.of("/poll"), blanks.ignoredUrls());
        assertTrue(AutoWaitSettings.NONE.isNone());
    }

    private static AutoWaitSettings settings(long networkIdleMillis, long animationIdleMillis) {
        return new AutoWaitSettings(Duration.ZERO, Duration.ofMillis(networkIdleMillis), AutoWaitSettings.DEFAULT_TIMEOUT,
                Duration.ofMillis(animationIdleMillis), AutoWaitSettings.DEFAULT_TIMEOUT, List.of());
    }

    private static Map<String, Object> status(boolean networkIdle, boolean animationsIdle, long pending, long requests,
                                              long animations) {
        return Map.of("networkIdle", networkIdle, "animationsIdle", animationsIdle,
                "pending", pending, "requests", requests, "animations", animations);
    }
}
