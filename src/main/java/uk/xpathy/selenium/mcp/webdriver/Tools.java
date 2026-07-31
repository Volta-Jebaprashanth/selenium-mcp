package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Facade over the Selenium collaborators in this package, and the entry point meant
 * for direct use when generating tests. It mirrors every browser tool exposed over
 * MCP, plus {@link By}-based overloads for callers that already have a locator.
 * <p>
 * Each concern lives in its own collaborator, wired together here rather than reached
 * through static helpers: {@link BrowserFactory} creates drivers, {@link BrowserSession}
 * owns the driver lifecycle, {@link Locators} resolves locator strategies, and
 * {@link Navigator} / {@link ElementInteractor} act on the driver for the current
 * session. Any of these can be swapped independently without touching the others.
 */
public class Tools {

    private final Locators locators;
    private final BrowserSession session;

    private Navigator navigator;
    private ElementInteractor elementInteractor;

    public Tools() {
        this(new BrowserFactory());
    }

    public Tools(BrowserFactory browserFactory) {
        this.locators = new Locators();
        this.session = new BrowserSession(browserFactory);
    }

    /**
     * Wraps an already-running driver, e.g. one created directly by a test.
     */
    public Tools(WebDriver driver) {
        this.locators = new Locators();
        this.session = new BrowserSession(new BrowserFactory(), driver);
        attachCollaborators(driver);
    }

    public WebDriver getDriver() {
        return session.getDriver();
    }

    public boolean isBrowserOpen() {
        return session.isOpen();
    }

    public String getBrowserName() {
        return session.getBrowserName();
    }

    /**
     * Opens a browser if one is not already open. If a browser is already open, it is reused.
     */
    public WebDriver openBrowser(String browser) {
        WebDriver driver = session.open(browser);
        attachCollaborators(driver);
        return driver;
    }

    public void closeBrowser() {
        session.close();
        navigator = null;
        elementInteractor = null;
    }

    public void navigateTo(String url) {
        requireNavigator().navigateTo(url);
    }

    public String getPageSource() {
        return requireNavigator().getPageSource();
    }

    public void click(By locator) {
        requireElementInteractor().click(locator);
    }

    public void click(String locatorType, String locatorValue) {
        requireElementInteractor().click(locatorType, locatorValue);
    }

    public void sendKeys(By locator, String text) {
        requireElementInteractor().sendKeys(locator, text);
    }

    public void sendKeys(By locator, String text, boolean clearFirst) {
        requireElementInteractor().sendKeys(locator, text, clearFirst);
    }

    public void sendKeys(String locatorType, String locatorValue, String text) {
        requireElementInteractor().sendKeys(locatorType, locatorValue, text);
    }

    public void sendKeys(String locatorType, String locatorValue, String text, boolean clearFirst) {
        requireElementInteractor().sendKeys(locatorType, locatorValue, text, clearFirst);
    }

    private void attachCollaborators(WebDriver driver) {
        this.navigator = new Navigator(driver);
        this.elementInteractor = new ElementInteractor(driver, locators);
    }

    private Navigator requireNavigator() {
        session.requireDriver();
        return navigator;
    }

    private ElementInteractor requireElementInteractor() {
        session.requireDriver();
        return elementInteractor;
    }
}
