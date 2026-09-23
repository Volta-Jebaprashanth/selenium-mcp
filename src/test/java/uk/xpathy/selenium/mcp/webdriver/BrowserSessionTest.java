package uk.xpathy.selenium.mcp.webdriver;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.service.DriverService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrowserSessionTest {

    private final BrowserFactory factory = mock(BrowserFactory.class);
    private final WebDriver driver = mock(WebDriver.class);
    private final DriverService service = mock(DriverService.class);

    BrowserSessionTest() {
        when(factory.resolveName(any())).thenReturn("chrome");
    }

    @Test
    void closingAnAttachedBrowserStopsTheServiceWithoutQuitting() {
        when(factory.attach("chrome", "127.0.0.1:9222"))
                .thenReturn(new AttachedDriver(driver, service, "127.0.0.1:9222"));
        BrowserSession session = new BrowserSession(factory);

        assertSame(driver, session.attach("chrome", "127.0.0.1:9222"));
        assertTrue(session.isAttached());
        assertEquals("127.0.0.1:9222", session.getDebuggerAddress());

        session.close();

        verify(service).stop();
        verify(driver, never()).quit();
        assertFalse(session.isOpen());
        assertFalse(session.isAttached());
        assertNull(session.getDebuggerAddress());
    }

    @Test
    void closingADebugLaunchedBrowserLeavesItRunning() {
        when(factory.launchDebuggable("chrome", 9333, null))
                .thenReturn(new AttachedDriver(driver, service, "127.0.0.1:9333"));
        BrowserSession session = new BrowserSession(factory);

        session.launchDebuggable("chrome", 9333, null);
        session.close();

        verify(service).stop();
        verify(driver, never()).quit();
    }

    @Test
    void closingALaunchedBrowserQuitsIt() {
        when(factory.create(any(), any())).thenReturn(driver);
        BrowserSession session = new BrowserSession(factory);

        session.open("chrome");
        assertFalse(session.isAttached());
        session.close();

        verify(driver).quit();
    }

    @Test
    void attachReusesAnAlreadyOpenBrowser() {
        when(factory.create(any(), any())).thenReturn(driver);
        BrowserSession session = new BrowserSession(factory);
        session.open("chrome");

        assertSame(driver, session.attach("chrome", null));
        assertSame(driver, session.launchDebuggable("chrome", 9222, null));

        verify(factory, never()).attach(any(), any());
        verify(factory, never()).launchDebuggable(any(), anyInt(), any());
    }
}
