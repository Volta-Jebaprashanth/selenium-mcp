package uk.xpathy.selenium.mcp.tools;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.service.DriverService;
import uk.xpathy.selenium.mcp.webdriver.AttachedDriver;
import uk.xpathy.selenium.mcp.webdriver.BrowserFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrowserSessionsTest {

    private final BrowserFactory factory = mock(BrowserFactory.class);
    private final WebDriver first = mock(WebDriver.class);
    private final WebDriver second = mock(WebDriver.class);
    private final WebDriver third = mock(WebDriver.class);
    private final BrowserSessions sessions = new BrowserSessions(factory, 3);
    private final BrowserTools browserTools = new BrowserTools(sessions);

    BrowserSessionsTest() {
        when(factory.resolveName(any())).thenAnswer(call -> call.getArgument(0) == null ? "chrome" : call.getArgument(0));
        when(factory.create(any(), any())).thenReturn(first, second, third);
        when(factory.normalizeDebuggerAddress(anyString())).thenAnswer(call -> call.getArgument(0));
        when(first.getCurrentUrl()).thenReturn("https://one.example/");
        when(second.getCurrentUrl()).thenReturn("https://two.example/");
    }

    @Test
    void firstBrowserIsTheDefaultSessionAndAnUnnamedSecondOpenIsRefused() {
        assertEquals("chrome browser opened.", open(null, null));
        assertEquals(BrowserSessions.DEFAULT_SESSION, sessions.activeName());

        String refusal = open(null, null);

        assertTrue(refusal.startsWith("A browser is already open (session 'default')"), refusal);
        assertEquals(1, sessions.all().size());
    }

    @Test
    void toolsActOnTheActiveSession() {
        open(null, null);
        assertEquals("firefox browser opened as session 'admin'.", open("firefox", "admin"));

        assertEquals("[admin] https://two.example/", browserTools.getCurrentUrl());
        assertEquals("Switched to session 'default' (chrome, https://one.example/).", browserTools.switchSession("default"));
        assertEquals("[default] https://one.example/", browserTools.getCurrentUrl());
    }

    @Test
    void openingAnExistingSessionNameSwitchesToIt() {
        open(null, null);
        open(null, "admin");

        assertEquals("Session 'default' is already open; switched to it.", open(null, "default"));
        assertEquals("default", sessions.activeName());
        verify(factory, never()).create("chrome", null);
    }

    @Test
    void closingTheActiveSessionActivatesTheMostRecentRemainingOne() {
        open(null, null);
        open(null, "admin");
        browserTools.switchSession("default");

        assertEquals("Browser closed. Active session is now 'admin'.", browserTools.closeBrowser(null, null));
        verify(first).quit();
        assertEquals("admin", sessions.activeName());

        assertEquals("Browser closed.", browserTools.closeBrowser(null, null));
        assertNull(sessions.activeName());
        assertFalse(sessions.active().isBrowserOpen());
        assertEquals("No browser is open. Call openBrowser first.", browserTools.getTitle());
    }

    @Test
    void sessionLimitAndNamesAreEnforced() {
        open(null, null);
        open(null, "b");
        open(null, "c");

        assertTrue(open(null, "d").startsWith("Already at the limit of 3 open sessions"));
        assertTrue(open(null, "bad name").startsWith("Invalid session name"));
    }

    @Test
    void aBrowserThatFailsToOpenIsNotRegistered() {
        when(factory.create(any(), any())).thenThrow(new IllegalArgumentException("Unsupported browser: opera."));

        assertEquals("Unsupported browser: opera.", open("opera", null));
        assertTrue(sessions.isEmpty());
    }

    @Test
    void closeAllCarriesOnPastFailuresAndOnlyDisconnectsAttachedBrowsers() {
        DriverService service = mock(DriverService.class);
        WebDriver attached = mock(WebDriver.class);
        when(factory.isDebuggerListening("127.0.0.1:9222")).thenReturn(true);
        when(factory.attach("chrome", "127.0.0.1:9222")).thenReturn(new AttachedDriver(attached, service, "127.0.0.1:9222"));
        doThrow(new RuntimeException("driver already gone")).when(first).quit();

        open(null, null);
        browserTools.connectToBrowser("chrome", "127.0.0.1:9222", null, null, null, "debug");

        assertEquals(List.of("default", "debug"), sessions.closeAll());
        verify(service).stop();
        verify(attached, never()).quit();
        assertTrue(sessions.isEmpty());
    }

    @Test
    void aDebuggerAddressCanOnlyBeAttachedByOneSession() {
        when(factory.isDebuggerListening("127.0.0.1:9222")).thenReturn(true);
        when(factory.attach("chrome", "127.0.0.1:9222"))
                .thenReturn(new AttachedDriver(first, mock(DriverService.class), "127.0.0.1:9222"));
        browserTools.connectToBrowser("chrome", "127.0.0.1:9222", null, null, null, null);

        assertEquals("Session 'default' is already connected to that browser. Call switchSession to use it.",
                browserTools.connectToBrowser("chrome", "127.0.0.1:9222", null, null, null, "again"));
    }

    @Test
    void launchDebugBrowserPicksTheNextPortWithoutASession() {
        when(factory.isDebuggerListening("127.0.0.1:9222")).thenReturn(true);
        when(factory.attach("chrome", "127.0.0.1:9222"))
                .thenReturn(new AttachedDriver(first, mock(DriverService.class), "127.0.0.1:9222"));
        when(factory.launchDebuggable("chrome", 9223, null))
                .thenReturn(new AttachedDriver(second, mock(DriverService.class), "127.0.0.1:9223"));

        browserTools.launchDebugBrowser("chrome", null, null, null, null, null, null);
        String launched = browserTools.launchDebugBrowser("chrome", null, null, null, null, null, "second");

        assertTrue(launched.startsWith("Launched chrome with remote debugging on 127.0.0.1:9223 as session 'second'"), launched);
    }

    @Test
    void aBrowserClosedByHandIsReportedThenDroppedSoItCanBeReopened() {
        open(null, null);
        open(null, "other");
        browserTools.switchSession("default");
        NoSuchSessionException gone = new NoSuchSessionException("invalid session id");
        when(first.getWindowHandles()).thenThrow(gone);
        when(first.getTitle()).thenThrow(gone);

        assertEquals("[default] Failed to get the title: invalid session id" + ErrorMessages.BROWSER_GONE_HINT,
                browserTools.getTitle());

        assertEquals("Session 'default' was removed: the browser is no longer reachable (closed or crashed).\n"
                + "chrome browser opened as session 'default'.", open(null, "default"));
        verify(first).quit();
        assertEquals(List.of("other", "default"), List.copyOf(sessions.all().keySet()));
        assertEquals("default", sessions.activeName());
    }

    @Test
    void whenTheActiveBrowserDiesTheMostRecentRemainingSessionBecomesActive() {
        open(null, null);
        open(null, "other");
        when(second.getWindowHandles()).thenThrow(new NoSuchSessionException("invalid session id"));

        String listing = browserTools.listSessions();

        assertTrue(listing.startsWith("Session 'other' was removed"), listing);
        assertTrue(listing.contains("* default: chrome"), listing);
        assertEquals("default", sessions.activeName());
    }

    @Test
    void aSlowOrFailingCheckDoesNotCountAsDead() {
        open(null, null);
        when(first.getWindowHandles()).thenThrow(new TimeoutException("timed out"));

        assertEquals(List.of(), sessions.pruneDead());
        assertTrue(sessions.has("default"));
    }

    @Test
    void failuresSayWhichSessionTheyCameFromOnceSeveralAreOpen() {
        AlertTools alertTools = new AlertTools(sessions);
        when(first.switchTo()).thenThrow(new NoAlertPresentException("no such alert"));
        when(second.switchTo()).thenThrow(new NoAlertPresentException("no such alert"));
        open(null, null);

        assertEquals("Failed to get alert text: no such alert", alertTools.getAlertText());

        open(null, "admin");

        assertEquals("[admin] Failed to get alert text: no such alert", alertTools.getAlertText());
    }

    @Test
    void openingABrowserStatesItsWaitStrategy() {
        String opened = browserTools.openBrowser(null, null, null, null, null, null, null, null, null);

        assertTrue(opened.startsWith("chrome browser opened.\nWait strategy: none - plain Selenium"), opened);
        assertTrue(opened.endsWith(AutoWaitMessages.SNIPPETS_POINTER), opened);
    }

    /**
     * @return the open result without the wait strategy lines, which {@link #openingABrowserStatesItsWaitStrategy} covers
     */
    private String open(String browser, String session) {
        return withoutWaitStrategy(browserTools.openBrowser(browser, null, null, null, null, null, null, null, session));
    }

    static String withoutWaitStrategy(String result) {
        int at = result.indexOf("\nWait strategy:");
        return at < 0 ? result : result.substring(0, at);
    }
}
