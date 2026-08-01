package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Facade over the Selenium collaborators in this package, and the entry point meant
 * for direct use when generating tests. It mirrors every browser tool exposed over
 * MCP, plus {@link By}-based overloads for callers that already have a locator.
 * <p>
 * Each concern lives in its own collaborator, wired together here rather than reached
 * through static helpers: {@link BrowserFactory} creates drivers, {@link BrowserSession}
 * owns the driver lifecycle, {@link Locators} resolves locator strategies, and the rest
 * ({@link Navigator}, {@link ElementInteractor}, {@link ElementInspector}, {@link WaitHelper},
 * {@link AlertHandler}, {@link FrameHandler}, {@link WindowManager}, {@link CookieManager},
 * {@link ScreenshotTaker}, {@link ScriptRunner}, {@link ActionsHelper}, {@link SelectHelper},
 * {@link FileHelper}, {@link NetworkMonitor}, {@link ConsoleLogMonitor}, {@link PageSourceInspector})
 * act on the driver for the current session. Any of these can be swapped independently without
 * touching the others.
 * <p>
 * {@link NetworkMonitor} and {@link ConsoleLogMonitor} are CDP-backed and only function against
 * Chrome/Edge; their methods throw {@link UnsupportedOperationException} on other browsers.
 */
public class Tools {

    private final Locators locators;
    private final BrowserSession session;

    private Navigator navigator;
    private ElementInteractor elementInteractor;
    private ElementInspector elementInspector;
    private WaitHelper waitHelper;
    private AlertHandler alertHandler;
    private FrameHandler frameHandler;
    private WindowManager windowManager;
    private CookieManager cookieManager;
    private ScreenshotTaker screenshotTaker;
    private ScriptRunner scriptRunner;
    private ActionsHelper actionsHelper;
    private SelectHelper selectHelper;
    private FileHelper fileHelper;
    private NetworkMonitor networkMonitor;
    private ConsoleLogMonitor consoleLogMonitor;
    private PageSourceInspector pageSourceInspector;

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
        return openBrowser(browser, BrowserOptions.DEFAULT);
    }

    /**
     * Opens a browser with the given launch options if one is not already open.
     * If a browser is already open, it is reused and the options are ignored.
     */
    public WebDriver openBrowser(String browser, BrowserOptions options) {
        WebDriver driver = session.open(browser, options);
        attachCollaborators(driver);
        return driver;
    }

    public void closeBrowser() {
        if (networkMonitor != null) {
            networkMonitor.close();
        }
        if (consoleLogMonitor != null) {
            consoleLogMonitor.close();
        }
        session.close();
        navigator = null;
        elementInteractor = null;
        elementInspector = null;
        waitHelper = null;
        alertHandler = null;
        frameHandler = null;
        windowManager = null;
        cookieManager = null;
        screenshotTaker = null;
        scriptRunner = null;
        actionsHelper = null;
        selectHelper = null;
        fileHelper = null;
        networkMonitor = null;
        consoleLogMonitor = null;
        pageSourceInspector = null;
    }

    // -- Navigation --

    public void navigateTo(String url) {
        requireNavigator().navigateTo(url);
    }

    public void back() {
        requireNavigator().back();
    }

    public void forward() {
        requireNavigator().forward();
    }

    public void refresh() {
        requireNavigator().refresh();
    }

    public String getCurrentUrl() {
        return requireNavigator().getCurrentUrl();
    }

    public String getTitle() {
        return requireNavigator().getTitle();
    }

    public String getPageSource() {
        return requireNavigator().getPageSource();
    }

    public String getPageScripts() {
        return requirePageSourceInspector().getScripts();
    }

    public String getPageStyles() {
        return requirePageSourceInspector().getStyles();
    }

    public String getPageElements(int maxDepth) {
        return requirePageSourceInspector().getElements(maxDepth);
    }

    public String getPageElements(By locator, int maxDepth) {
        return requirePageSourceInspector().getElements(locator, maxDepth);
    }

    public String getPageElements(String locatorType, String locatorValue, int maxDepth) {
        return requirePageSourceInspector().getElements(locatorType, locatorValue, maxDepth);
    }

    // -- Element interaction --

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

    // -- Element queries --

    public String getText(By locator) {
        return requireElementInspector().getText(locator);
    }

    public String getText(String locatorType, String locatorValue) {
        return requireElementInspector().getText(locatorType, locatorValue);
    }

    public String getAttribute(By locator, String attributeName) {
        return requireElementInspector().getAttribute(locator, attributeName);
    }

    public String getAttribute(String locatorType, String locatorValue, String attributeName) {
        return requireElementInspector().getAttribute(locatorType, locatorValue, attributeName);
    }

    public String getCssValue(By locator, String propertyName) {
        return requireElementInspector().getCssValue(locator, propertyName);
    }

    public String getCssValue(String locatorType, String locatorValue, String propertyName) {
        return requireElementInspector().getCssValue(locatorType, locatorValue, propertyName);
    }

    public String getTagName(By locator) {
        return requireElementInspector().getTagName(locator);
    }

    public String getTagName(String locatorType, String locatorValue) {
        return requireElementInspector().getTagName(locatorType, locatorValue);
    }

    public boolean isDisplayed(By locator) {
        return requireElementInspector().isDisplayed(locator);
    }

    public boolean isDisplayed(String locatorType, String locatorValue) {
        return requireElementInspector().isDisplayed(locatorType, locatorValue);
    }

    public boolean isEnabled(By locator) {
        return requireElementInspector().isEnabled(locator);
    }

    public boolean isEnabled(String locatorType, String locatorValue) {
        return requireElementInspector().isEnabled(locatorType, locatorValue);
    }

    public boolean isSelected(By locator) {
        return requireElementInspector().isSelected(locator);
    }

    public boolean isSelected(String locatorType, String locatorValue) {
        return requireElementInspector().isSelected(locatorType, locatorValue);
    }

    public Dimension getElementSize(By locator) {
        return requireElementInspector().getSize(locator);
    }

    public Dimension getElementSize(String locatorType, String locatorValue) {
        return requireElementInspector().getSize(locatorType, locatorValue);
    }

    public Point getElementLocation(By locator) {
        return requireElementInspector().getLocation(locator);
    }

    public Point getElementLocation(String locatorType, String locatorValue) {
        return requireElementInspector().getLocation(locatorType, locatorValue);
    }

    public boolean elementExists(By locator) {
        return requireElementInspector().exists(locator);
    }

    public boolean elementExists(String locatorType, String locatorValue) {
        return requireElementInspector().exists(locatorType, locatorValue);
    }

    public int countElements(By locator) {
        return requireElementInspector().count(locator);
    }

    public int countElements(String locatorType, String locatorValue) {
        return requireElementInspector().count(locatorType, locatorValue);
    }

    public List<String> getElementsText(By locator) {
        return requireElementInspector().getTexts(locator);
    }

    public List<String> getElementsText(String locatorType, String locatorValue) {
        return requireElementInspector().getTexts(locatorType, locatorValue);
    }

    // -- Waits --

    public void waitForVisible(By locator, int timeoutSeconds) {
        requireWaitHelper().waitForVisible(locator, timeoutSeconds);
    }

    public void waitForVisible(String locatorType, String locatorValue, int timeoutSeconds) {
        requireWaitHelper().waitForVisible(locatorType, locatorValue, timeoutSeconds);
    }

    public void waitForClickable(By locator, int timeoutSeconds) {
        requireWaitHelper().waitForClickable(locator, timeoutSeconds);
    }

    public void waitForClickable(String locatorType, String locatorValue, int timeoutSeconds) {
        requireWaitHelper().waitForClickable(locatorType, locatorValue, timeoutSeconds);
    }

    public void waitForPresent(By locator, int timeoutSeconds) {
        requireWaitHelper().waitForPresent(locator, timeoutSeconds);
    }

    public void waitForPresent(String locatorType, String locatorValue, int timeoutSeconds) {
        requireWaitHelper().waitForPresent(locatorType, locatorValue, timeoutSeconds);
    }

    public void waitForInvisible(By locator, int timeoutSeconds) {
        requireWaitHelper().waitForInvisible(locator, timeoutSeconds);
    }

    public void waitForInvisible(String locatorType, String locatorValue, int timeoutSeconds) {
        requireWaitHelper().waitForInvisible(locatorType, locatorValue, timeoutSeconds);
    }

    public void waitForTextPresent(By locator, String text, int timeoutSeconds) {
        requireWaitHelper().waitForTextPresent(locator, text, timeoutSeconds);
    }

    public void waitForTextPresent(String locatorType, String locatorValue, String text, int timeoutSeconds) {
        requireWaitHelper().waitForTextPresent(locatorType, locatorValue, text, timeoutSeconds);
    }

    public void waitForTitleContains(String text, int timeoutSeconds) {
        requireWaitHelper().waitForTitleContains(text, timeoutSeconds);
    }

    public void waitForUrlContains(String text, int timeoutSeconds) {
        requireWaitHelper().waitForUrlContains(text, timeoutSeconds);
    }

    public void waitForPageLoad(int timeoutSeconds) {
        requireWaitHelper().waitForPageLoad(timeoutSeconds);
    }

    public void waitForJsCondition(String script, int timeoutSeconds) {
        requireWaitHelper().waitForJsCondition(script, timeoutSeconds);
    }

    public void waitForElementCount(By locator, int expectedCount, int timeoutSeconds) {
        requireWaitHelper().waitForElementCount(locator, expectedCount, timeoutSeconds);
    }

    public void waitForElementCount(String locatorType, String locatorValue, int expectedCount, int timeoutSeconds) {
        requireWaitHelper().waitForElementCount(locatorType, locatorValue, expectedCount, timeoutSeconds);
    }

    public void waitForAttributeToBe(By locator, String attribute, String expectedValue, int timeoutSeconds) {
        requireWaitHelper().waitForAttributeToBe(locator, attribute, expectedValue, timeoutSeconds);
    }

    public void waitForAttributeToBe(String locatorType, String locatorValue, String attribute,
                                      String expectedValue, int timeoutSeconds) {
        requireWaitHelper().waitForAttributeToBe(locatorType, locatorValue, attribute, expectedValue, timeoutSeconds);
    }

    public void waitForNumberOfWindowsToBe(int expectedCount, int timeoutSeconds) {
        requireWaitHelper().waitForNumberOfWindowsToBe(expectedCount, timeoutSeconds);
    }

    // -- Alerts --

    public void acceptAlert() {
        requireAlertHandler().accept();
    }

    public void dismissAlert() {
        requireAlertHandler().dismiss();
    }

    public String getAlertText() {
        return requireAlertHandler().getText();
    }

    public void sendKeysToAlert(String text) {
        requireAlertHandler().sendKeys(text);
    }

    public boolean isAlertPresent() {
        return requireAlertHandler().isPresent();
    }

    // -- Frames --

    public void switchToFrameByIndex(int index) {
        requireFrameHandler().switchToFrame(index);
    }

    public void switchToFrameByNameOrId(String nameOrId) {
        requireFrameHandler().switchToFrame(nameOrId);
    }

    public void switchToFrame(By locator) {
        requireFrameHandler().switchToFrame(locator);
    }

    public void switchToFrame(String locatorType, String locatorValue) {
        requireFrameHandler().switchToFrame(locatorType, locatorValue);
    }

    public void switchToDefaultContent() {
        requireFrameHandler().switchToDefaultContent();
    }

    public void switchToParentFrame() {
        requireFrameHandler().switchToParentFrame();
    }

    // -- Windows --

    public void maximizeWindow() {
        requireWindowManager().maximize();
    }

    public void minimizeWindow() {
        requireWindowManager().minimize();
    }

    public void fullscreenWindow() {
        requireWindowManager().fullscreen();
    }

    public void setWindowSize(int width, int height) {
        requireWindowManager().setSize(width, height);
    }

    public Dimension getWindowSize() {
        return requireWindowManager().getSize();
    }

    public void setWindowPosition(int x, int y) {
        requireWindowManager().setPosition(x, y);
    }

    public Point getWindowPosition() {
        return requireWindowManager().getPosition();
    }

    public String getWindowHandle() {
        return requireWindowManager().getWindowHandle();
    }

    public Set<String> getWindowHandles() {
        return requireWindowManager().getWindowHandles();
    }

    public void switchToWindow(String handle) {
        requireWindowManager().switchToWindow(handle);
    }

    public String openNewTab() {
        return requireWindowManager().openNewTab();
    }

    public void closeCurrentWindow() {
        requireWindowManager().closeCurrentWindow();
    }

    // -- Cookies --

    public void addCookie(String name, String value) {
        requireCookieManager().addCookie(name, value);
    }

    public void addCookie(String name, String value, String domain, String path) {
        requireCookieManager().addCookie(name, value, domain, path);
    }

    public Cookie getCookie(String name) {
        return requireCookieManager().getCookie(name);
    }

    public Set<Cookie> getAllCookies() {
        return requireCookieManager().getAllCookies();
    }

    public void deleteCookie(String name) {
        requireCookieManager().deleteCookie(name);
    }

    public void deleteAllCookies() {
        requireCookieManager().deleteAllCookies();
    }

    // -- Screenshots --

    public String takeScreenshotBase64() {
        return requireScreenshotTaker().captureBase64();
    }

    public void takeScreenshotToFile(String filePath) throws IOException {
        requireScreenshotTaker().captureToFile(filePath);
    }

    public String takeElementScreenshotBase64(By locator) {
        return requireScreenshotTaker().captureElementBase64(locator);
    }

    public String takeElementScreenshotBase64(String locatorType, String locatorValue) {
        return requireScreenshotTaker().captureElementBase64(locatorType, locatorValue);
    }

    public void takeElementScreenshotToFile(By locator, String filePath) throws IOException {
        requireScreenshotTaker().captureElementToFile(locator, filePath);
    }

    public void takeElementScreenshotToFile(String locatorType, String locatorValue, String filePath) throws IOException {
        requireScreenshotTaker().captureElementToFile(locatorType, locatorValue, filePath);
    }

    // -- Script execution / scrolling --

    public Object executeScript(String script, Object... args) {
        return requireScriptRunner().executeScript(script, args);
    }

    public void scrollIntoView(By locator) {
        requireScriptRunner().scrollIntoView(locator);
    }

    public void scrollIntoView(String locatorType, String locatorValue) {
        requireScriptRunner().scrollIntoView(locatorType, locatorValue);
    }

    public void scrollBy(int x, int y) {
        requireScriptRunner().scrollBy(x, y);
    }

    public void scrollToTop() {
        requireScriptRunner().scrollToTop();
    }

    public void scrollToBottom() {
        requireScriptRunner().scrollToBottom();
    }

    // -- Mouse / keyboard actions --

    public void hover(By locator) {
        requireActionsHelper().hover(locator);
    }

    public void hover(String locatorType, String locatorValue) {
        requireActionsHelper().hover(locatorType, locatorValue);
    }

    public void doubleClick(By locator) {
        requireActionsHelper().doubleClick(locator);
    }

    public void doubleClick(String locatorType, String locatorValue) {
        requireActionsHelper().doubleClick(locatorType, locatorValue);
    }

    public void rightClick(By locator) {
        requireActionsHelper().rightClick(locator);
    }

    public void rightClick(String locatorType, String locatorValue) {
        requireActionsHelper().rightClick(locatorType, locatorValue);
    }

    public void clickAndHold(By locator) {
        requireActionsHelper().clickAndHold(locator);
    }

    public void clickAndHold(String locatorType, String locatorValue) {
        requireActionsHelper().clickAndHold(locatorType, locatorValue);
    }

    public void release() {
        requireActionsHelper().release();
    }

    public void dragAndDrop(By source, By target) {
        requireActionsHelper().dragAndDrop(source, target);
    }

    public void dragAndDrop(String sourceType, String sourceValue, String targetType, String targetValue) {
        requireActionsHelper().dragAndDrop(sourceType, sourceValue, targetType, targetValue);
    }

    public void dragAndDropByOffset(By locator, int xOffset, int yOffset) {
        requireActionsHelper().dragAndDropByOffset(locator, xOffset, yOffset);
    }

    public void dragAndDropByOffset(String locatorType, String locatorValue, int xOffset, int yOffset) {
        requireActionsHelper().dragAndDropByOffset(locatorType, locatorValue, xOffset, yOffset);
    }

    public void pressKey(String keyName) {
        requireActionsHelper().pressKey(keyName);
    }

    // -- Select dropdowns --

    public void selectByVisibleText(By locator, String text) {
        requireSelectHelper().selectByVisibleText(locator, text);
    }

    public void selectByVisibleText(String locatorType, String locatorValue, String text) {
        requireSelectHelper().selectByVisibleText(locatorType, locatorValue, text);
    }

    public void selectByValue(By locator, String value) {
        requireSelectHelper().selectByValue(locator, value);
    }

    public void selectByValue(String locatorType, String locatorValue, String optionValue) {
        requireSelectHelper().selectByValue(locatorType, locatorValue, optionValue);
    }

    public void selectByIndex(By locator, int index) {
        requireSelectHelper().selectByIndex(locator, index);
    }

    public void selectByIndex(String locatorType, String locatorValue, int index) {
        requireSelectHelper().selectByIndex(locatorType, locatorValue, index);
    }

    public void deselectAllOptions(By locator) {
        requireSelectHelper().deselectAll(locator);
    }

    public void deselectAllOptions(String locatorType, String locatorValue) {
        requireSelectHelper().deselectAll(locatorType, locatorValue);
    }

    public List<String> getSelectedOptionsText(By locator) {
        return requireSelectHelper().getSelectedOptionsText(locator);
    }

    public List<String> getSelectedOptionsText(String locatorType, String locatorValue) {
        return requireSelectHelper().getSelectedOptionsText(locatorType, locatorValue);
    }

    public boolean isMultipleSelect(By locator) {
        return requireSelectHelper().isMultiple(locator);
    }

    public boolean isMultipleSelect(String locatorType, String locatorValue) {
        return requireSelectHelper().isMultiple(locatorType, locatorValue);
    }

    // -- File upload / PDF printing --

    public void uploadFile(By locator, String absoluteFilePath) {
        requireFileHelper().uploadFile(locator, absoluteFilePath);
    }

    public void uploadFile(String locatorType, String locatorValue, String absoluteFilePath) {
        requireFileHelper().uploadFile(locatorType, locatorValue, absoluteFilePath);
    }

    public String printToPdfBase64() {
        return requireFileHelper().printToPdfBase64();
    }

    public void printToPdfFile(String filePath) throws IOException {
        requireFileHelper().printToPdfFile(filePath);
    }

    // -- Network capture / mocking / blocking / conditions --

    public boolean isNetworkCaptureSupported() {
        return requireNetworkMonitor().isSupported();
    }

    public void startNetworkCapture(String urlPattern) {
        requireNetworkMonitor().startCapture(urlPattern);
    }

    public void stopNetworkCapture() {
        requireNetworkMonitor().stopCapture();
    }

    public void clearNetworkLog() {
        requireNetworkMonitor().clearLog();
    }

    public java.util.List<NetworkMonitor.NetworkEntry> getNetworkLog(String urlPattern, String method, int limit) {
        return requireNetworkMonitor().getEntries(urlPattern, method, limit);
    }

    public void mockResponse(String urlPattern, String method, int status, String contentType,
                              String body, java.util.Map<String, String> headers) {
        requireNetworkMonitor().mockResponse(urlPattern, method, status, contentType, body, headers);
    }

    public void clearMockResponses() {
        requireNetworkMonitor().clearMocks();
    }

    public void blockRequests(String urlPattern) {
        requireNetworkMonitor().blockRequests(urlPattern);
    }

    public void clearBlockedRequests() {
        requireNetworkMonitor().clearBlocked();
    }

    public int getPendingRequestCount() {
        return requireNetworkMonitor().getPendingRequestCount();
    }

    public void armNetworkTracking() {
        requireNetworkMonitor().armTracking();
    }

    public void waitForNetworkIdle(long idleMillis, int timeoutSeconds) {
        requireNetworkMonitor().waitForNetworkIdle(idleMillis, timeoutSeconds);
    }

    public void setNetworkConditions(boolean offline, long latencyMillis, int downloadKbps, int uploadKbps) {
        requireNetworkMonitor().setNetworkConditions(offline, latencyMillis, downloadKbps, uploadKbps);
    }

    public String getNetworkConditions() {
        return requireNetworkMonitor().getNetworkConditions();
    }

    public void clearNetworkConditions() {
        requireNetworkMonitor().clearNetworkConditions();
    }

    public void setBasicAuthCredentials(String username, String password) {
        requireNetworkMonitor().setBasicAuthCredentials(username, password);
    }

    // -- Console log capture --

    public boolean isConsoleCaptureSupported() {
        return requireConsoleLogMonitor().isSupported();
    }

    public void startConsoleCapture() {
        requireConsoleLogMonitor().start();
    }

    public java.util.List<ConsoleLogMonitor.ConsoleLogEntry> getConsoleLogs(String type, int limit) {
        return requireConsoleLogMonitor().getEntries(type, limit);
    }

    public void clearConsoleLogs() {
        requireConsoleLogMonitor().clear();
    }

    private void attachCollaborators(WebDriver driver) {
        this.navigator = new Navigator(driver);
        this.elementInteractor = new ElementInteractor(driver, locators);
        this.elementInspector = new ElementInspector(driver, locators);
        this.waitHelper = new WaitHelper(driver, locators);
        this.alertHandler = new AlertHandler(driver);
        this.frameHandler = new FrameHandler(driver, locators);
        this.windowManager = new WindowManager(driver);
        this.cookieManager = new CookieManager(driver);
        this.screenshotTaker = new ScreenshotTaker(driver, locators);
        this.scriptRunner = new ScriptRunner(driver, locators);
        this.actionsHelper = new ActionsHelper(driver, locators);
        this.selectHelper = new SelectHelper(driver, locators);
        this.fileHelper = new FileHelper(driver, locators);
        this.networkMonitor = new NetworkMonitor(driver);
        this.consoleLogMonitor = new ConsoleLogMonitor(driver);
        this.pageSourceInspector = new PageSourceInspector(driver, locators);
    }

    private Navigator requireNavigator() {
        session.requireDriver();
        return navigator;
    }

    private ElementInteractor requireElementInteractor() {
        session.requireDriver();
        return elementInteractor;
    }

    private ElementInspector requireElementInspector() {
        session.requireDriver();
        return elementInspector;
    }

    private WaitHelper requireWaitHelper() {
        session.requireDriver();
        return waitHelper;
    }

    private AlertHandler requireAlertHandler() {
        session.requireDriver();
        return alertHandler;
    }

    private FrameHandler requireFrameHandler() {
        session.requireDriver();
        return frameHandler;
    }

    private WindowManager requireWindowManager() {
        session.requireDriver();
        return windowManager;
    }

    private CookieManager requireCookieManager() {
        session.requireDriver();
        return cookieManager;
    }

    private ScreenshotTaker requireScreenshotTaker() {
        session.requireDriver();
        return screenshotTaker;
    }

    private ScriptRunner requireScriptRunner() {
        session.requireDriver();
        return scriptRunner;
    }

    private ActionsHelper requireActionsHelper() {
        session.requireDriver();
        return actionsHelper;
    }

    private SelectHelper requireSelectHelper() {
        session.requireDriver();
        return selectHelper;
    }

    private FileHelper requireFileHelper() {
        session.requireDriver();
        return fileHelper;
    }

    private NetworkMonitor requireNetworkMonitor() {
        session.requireDriver();
        return networkMonitor;
    }

    private ConsoleLogMonitor requireConsoleLogMonitor() {
        session.requireDriver();
        return consoleLogMonitor;
    }

    private PageSourceInspector requirePageSourceInspector() {
        session.requireDriver();
        return pageSourceInspector;
    }
}
