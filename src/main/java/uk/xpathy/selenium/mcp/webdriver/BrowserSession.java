package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.WebDriver;

/**
 * Owns the lifecycle of a single {@link WebDriver} instance for a browsing session.
 */
public class BrowserSession {

    private final BrowserFactory browserFactory;
    private WebDriver driver;
    private String browserName;

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

    public void close() {
        if (driver != null) {
            driver.quit();
            driver = null;
            browserName = null;
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

    public WebDriver requireDriver() {
        if (driver == null) {
            throw new IllegalStateException("No browser is open. Call openBrowser first.");
        }
        return driver;
    }
}
