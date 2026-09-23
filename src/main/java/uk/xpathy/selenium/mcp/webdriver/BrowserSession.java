package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.service.DriverService;

/**
 * Owns the lifecycle of a single {@link WebDriver} instance for a browsing session.
 * <p>
 * The driver is either <em>launched</em> (owned by this session, and quit on {@link #close()}) or
 * <em>attached</em> over a remote-debugging port (see {@link AttachedDriver}), in which case
 * {@link #close()} only disconnects and the browser keeps running.
 */
public class BrowserSession {

    private final BrowserFactory browserFactory;
    private WebDriver driver;
    private String browserName;
    private DriverService attachedService;
    private String debuggerAddress;

    public BrowserSession(BrowserFactory browserFactory) {
        this.browserFactory = browserFactory;
    }

    /**
     * Wraps an already-running driver. The browser name is unknown in this case,
     * since the driver was not created by this session.
     */
    public BrowserSession(BrowserFactory browserFactory, WebDriver driver) {
        this.browserFactory = browserFactory;
        this.driver = driver;
    }

    public boolean isOpen() {
        return driver != null;
    }

    /**
     * Opens a browser if one is not already open. If a browser is already open, it is reused.
     */
    public WebDriver open(String browser) {
        return open(browser, BrowserOptions.DEFAULT);
    }

    /**
     * Opens a browser with the given launch options if one is not already open.
     * If a browser is already open, it is reused and the options are ignored.
     */
    public WebDriver open(String browser, BrowserOptions options) {
        if (driver == null) {
            browserName = browserFactory.resolveName(browser);
            driver = browserFactory.create(browser, options);
        }
        return driver;
    }

    /**
     * Attaches to a Chrome/Edge already running with remote debugging, if no browser is open.
     * If a browser is already open, it is reused.
     */
    public WebDriver attach(String browser, String debuggerAddress) {
        if (driver == null) {
            adopt(browser, browserFactory.attach(browser, debuggerAddress));
        }
        return driver;
    }

    /**
     * Launches a Chrome/Edge with remote debugging on {@code port} and attaches to it, if no
     * browser is open. The browser outlives {@link #close()}. If a browser is already open, it is reused.
     */
    public WebDriver launchDebuggable(String browser, int port, String userDataDir) {
        if (driver == null) {
            adopt(browser, browserFactory.launchDebuggable(browser, port, userDataDir));
        }
        return driver;
    }

    private void adopt(String browser, AttachedDriver attached) {
        browserName = browserFactory.resolveName(browser);
        driver = attached.driver();
        attachedService = attached.service();
        debuggerAddress = attached.debuggerAddress();
    }

    /**
     * Quits a launched browser, or disconnects from an attached one without closing it.
     */
    public void close() {
        if (driver != null) {
            try {
                if (attachedService != null) {
                    attachedService.stop();
                } else {
                    driver.quit();
                }
            } finally {
                driver = null;
                browserName = null;
                attachedService = null;
                debuggerAddress = null;
            }
        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    /**
     * @return the resolved browser name (e.g. {@code "chrome"}), or {@code null} if
     * no browser is open, or if the driver was supplied externally
     */
    public String getBrowserName() {
        return browserName;
    }

    /**
     * @return whether the open browser is attached over a remote-debugging port, and so
     * will be left running by {@link #close()}
     */
    public boolean isAttached() {
        return attachedService != null;
    }

    /**
     * @return the {@code host:port} of the attached browser's debugger, or {@code null} if not attached
     */
    public String getDebuggerAddress() {
        return debuggerAddress;
    }

    public boolean isDebuggerListening(String debuggerAddress) {
        return browserFactory.isDebuggerListening(debuggerAddress);
    }

    public WebDriver requireDriver() {
        if (driver == null) {
            throw new IllegalStateException("No browser is open. Call openBrowser first.");
        }
        return driver;
    }
}
