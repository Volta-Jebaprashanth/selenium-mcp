package com.voltajeba.selenium.mcp.tools;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import com.voltajeba.selenium.mcp.webdriver.AutoWaitSettings;
import com.voltajeba.selenium.mcp.webdriver.Tools;
import com.voltajeba.selenium.mcp.webdriver.WaitRecord;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoWaitMessagesTest {

    private static final AutoWaitSettings NETWORK = WaitTools.toSettings(null, 500L, null, 300L, null, "/poll, ,hotjar");

    private final Tools tools = mock(Tools.class);

    AutoWaitMessagesTest() {
        when(tools.isBrowserOpen()).thenReturn(true);
        when(tools.getAutoWait()).thenReturn(NETWORK);
    }

    @Test
    void toolParametersBecomeSettings() {
        assertEquals(Duration.ZERO, NETWORK.implicitWait());
        assertEquals(Duration.ofMillis(500), NETWORK.networkIdle());
        assertEquals(AutoWaitSettings.DEFAULT_TIMEOUT, NETWORK.networkTimeout());
        assertEquals(List.of("/poll", "hotjar"), NETWORK.ignoredUrls());
        assertTrue(WaitTools.toSettings(null, null, null, null, null, null).isNone());
        assertThrows(IllegalArgumentException.class, () -> WaitTools.toSettings(-1, null, null, null, null, null));
    }

    @Test
    void theStrategyIsDescribedInFull() {
        assertEquals("no implicit wait; network idle 500ms after actions (timeout 10s); animations idle 300ms after "
                + "actions (timeout 10s); ignoring requests to URLs containing /poll, hotjar", AutoWaitMessages.describe(NETWORK));
        assertTrue(AutoWaitMessages.describe(AutoWaitSettings.NONE).startsWith("none - plain Selenium"));
    }

    @Test
    void anActionThatSetOffRequestsSaysWhichWaitTheTestNeeds() {
        when(tools.getLastWaitRecord()).thenReturn(record(Duration.ZERO, 2, 0, false));

        String note = AutoWaitMessages.afterAction(tools);

        assertTrue(note.startsWith("\n[auto-wait] This step set off 2 request(s). Waited 820ms for network idle."), note);
        assertTrue(note.endsWith("follow this step with: Waits.networkIdle(driver);"), note);
    }

    @Test
    void requestsAndAnimationsTogetherNeedAfterAction() {
        when(tools.getLastWaitRecord()).thenReturn(record(Duration.ZERO, 1, 1, false));

        assertTrue(AutoWaitMessages.afterAction(tools).contains("Waits.afterAction(driver)"));
    }

    @Test
    void aQuietActionGetsNoNote() {
        when(tools.getLastWaitRecord()).thenReturn(record(Duration.ZERO, 0, 0, false));

        assertEquals("", AutoWaitMessages.afterAction(tools));
    }

    @Test
    void aCoveredClickNeedsAClickableWait() {
        when(tools.getLastWaitRecord()).thenReturn(record(Duration.ofMillis(1500), 0, 0, false));

        String note = AutoWaitMessages.afterAction(tools);

        assertTrue(note.contains("covered by div.spinner when the click was attempted; it got through after 1.5s"), note);
        assertTrue(note.contains("Waits.clickable(driver, <locator>)"), note);
    }

    @Test
    void aTimeoutSuggestsIgnoringBackgroundRequests() {
        when(tools.getLastWaitRecord()).thenReturn(record(Duration.ZERO, 1, 0, true));

        assertTrue(AutoWaitMessages.afterAction(tools).contains("add their URL to setAutoWait's ignoreUrls"));
    }

    @Test
    void failuresAMissingWaitCausesGetAHint() {
        when(tools.getAutoWait()).thenReturn(AutoWaitSettings.NONE);

        assertTrue(AutoWaitMessages.failureHint(tools, new NoSuchElementException("x"))
                .contains("this session has no wait strategy"));
        assertTrue(AutoWaitMessages.failureHint(tools, new ElementClickInterceptedException("x"))
                .contains("Waits.clickable"));
        assertEquals("", AutoWaitMessages.failureHint(tools, new TimeoutException("x")));
    }

    @Test
    void theReportListsOnlyTheStepsThatNeedAWait() {
        when(tools.getWaitRecords()).thenReturn(List.of(
                record(1, "click By.id: nothing", Duration.ZERO, 0, 0, false),
                record(2, "click By.id: save", Duration.ZERO, 3, 0, false),
                record(3, "click By.id: under", Duration.ofMillis(400), 0, 0, false)));

        String report = AutoWaitMessages.report(tools);

        assertTrue(report.contains("2 of 3 recorded action(s) need a wait"), report);
        assertTrue(report.contains(" 2. click By.id: save - set off 3 request(s) -> Waits.networkIdle(driver) after it"), report);
        assertTrue(report.contains(" 3. click By.id: under - covered by div.spinner -> Waits.clickable(driver, <locator>) before it"), report);
        assertTrue(!report.contains("By.id: nothing"), report);
    }

    @Test
    void withoutTrackingTheReportSaysHowToTurnItOn() {
        when(tools.getAutoWait()).thenReturn(AutoWaitSettings.NONE);
        when(tools.getWaitRecords()).thenReturn(List.of());

        String report = AutoWaitMessages.report(tools);

        assertTrue(report.contains("No actions recorded yet."), report);
        assertTrue(report.contains("tracking is off"), report);
    }

    @Test
    void openingStatesTheStrategyAndWhereTheCodeIs() {
        when(tools.getAutoWait()).thenReturn(AutoWaitSettings.NONE);

        String note = AutoWaitMessages.sessionOpened(tools);

        assertTrue(note.startsWith("\nWait strategy: none - plain Selenium"), note);
        assertTrue(note.contains(AutoWaitMessages.RECOMMENDATION), note);
        assertTrue(note.endsWith(AutoWaitMessages.SNIPPETS_POINTER), note);
    }

    @Test
    void openingWithOnlyAnImplicitWaitStillRecommendsTracking() {
        when(tools.getAutoWait()).thenReturn(new AutoWaitSettings(Duration.ofSeconds(5), Duration.ZERO,
                AutoWaitSettings.DEFAULT_TIMEOUT, Duration.ZERO, AutoWaitSettings.DEFAULT_TIMEOUT, List.of()));

        assertTrue(AutoWaitMessages.sessionOpened(tools).contains(AutoWaitMessages.RECOMMENDATION));
    }

    @Test
    void openingWithTrackingDoesNotRecommendIt() {
        when(tools.getAutoWait()).thenReturn(new AutoWaitSettings(Duration.ZERO, Duration.ofMillis(500),
                AutoWaitSettings.DEFAULT_TIMEOUT, Duration.ofMillis(300), AutoWaitSettings.DEFAULT_TIMEOUT, List.of()));

        assertFalse(AutoWaitMessages.sessionOpened(tools).contains("Recommended"));
    }

    private static WaitRecord record(Duration covered, int requests, int animations, boolean networkTimedOut) {
        return record(1, "click", covered, requests, animations, networkTimedOut);
    }

    private static WaitRecord record(int step, String action, Duration covered, int requests, int animations,
                                     boolean networkTimedOut) {
        return new WaitRecord(step, action, covered.isZero() ? null : "div.spinner", covered, requests, animations,
                requests > 0 ? Duration.ofMillis(820) : Duration.ZERO, networkTimedOut, networkTimedOut ? 1 : 0,
                Duration.ZERO, false, false);
    }
}
