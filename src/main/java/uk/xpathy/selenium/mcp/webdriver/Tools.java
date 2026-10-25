package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.time.Duration;
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
 * ({@link Navigator}, {@link ElementInteractor}, {@link ElementInspector}, {@link ClickabilityChecker}, {@link WaitHelper},
 * {@link AlertHandler}, {@link FrameHandler}, {@link WindowManager}, {@link CookieManager},
 * {@link ScreenshotTaker}, {@link ScriptRunner}, {@link ActionsHelper}, {@link SelectHelper},
 * {@link FileHelper}, {@link NetworkMonitor}, {@link ConsoleLogMonitor}, {@link PageSourceInspector},
 * {@link InitScriptManager}, {@link AutoWaiter})
 * act on the driver for the current session. Any of these can be swapped independently without
 * touching the others.
 * <p>
 * Every action (navigation, clicks, typing, mouse and keyboard actions, selects, uploads, alert
 * answers) runs through {@link AutoWaiter}, which applies the session's {@link AutoWaitSettings} and
 * records a {@link WaitRecord} of what the action set off. New action methods should do the same.
 * <p>
 * {@link NetworkMonitor} and {@link ConsoleLogMonitor} are CDP-backed and only function against
 * Chrome/Edge; their methods throw {@link UnsupportedOperationException} on other browsers.
 */
public class Tools {

    private final Locators locators;
    private final BrowserSession session;
    private final WaitCodeGenerator waitCodeGenerator;

    private Navigator navigator;
    private ElementInteractor elementInteractor;
    private ElementInspector elementInspector;
    private ClickabilityChecker clickabilityChecker;
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
    private InitScriptManager initScriptManager;
    private AutoWaiter autoWaiter;

    public Tools() {
        this(new BrowserFactory());
    }

    public Tools(BrowserFactory browserFactory) {
        this.locators = new Locators();
        this.session = new BrowserSession(browserFactory);
        this.waitCodeGenerator = new WaitCodeGenerator();
    }

    /**
     * Wraps an already-running driver, e.g. one created directly by a test.
     */
    public Tools(WebDriver driver) {
        this.locators = new Locators();
        this.session = new BrowserSession(new BrowserFactory(), driver);
        this.waitCodeGenerator = new WaitCodeGenerator();
        attachCollaborators(driver);
    }

    public WebDriver getDriver() {
        return session.getDriver();
    }

    public boolean isBrowserOpen() {
        return session.isOpen();
    }

    /**
     * Like {@link #isBrowserOpen()}, but also checks that the browser still responds - it is {@code false}
     * once the browser's last window has been closed by hand or its process has died.
     */
    public boolean isBrowserAlive() {
        return session.isAlive();
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

    /**
     * Attaches to a Chrome/Edge already running with {@code --remote-debugging-port}, if no browser
     * is open. {@link #closeBrowser()} will disconnect and leave that browser running.
     *
     * @param debuggerAddress {@code host:port}; defaults to {@code 127.0.0.1:9222} when blank
     */
    public WebDriver attachBrowser(String browser, String debuggerAddress) {
        WebDriver driver = session.attach(browser, debuggerAddress);
        attachCollaborators(driver);
        return driver;
    }

    /**
     * Launches a Chrome/Edge with remote debugging on {@code port} and attaches to it, if no browser
     * is open. The browser keeps running after {@link #closeBrowser()}, so other clients (e.g. a test)
     * can attach to {@code 127.0.0.1:<port>}.
     *
     * @param userDataDir profile directory, or {@code null} for a default one under the system temp dir
     */
    public WebDriver launchDebugBrowser(String browser, int port, String userDataDir) {
        WebDriver driver = session.launchDebuggable(browser, port, userDataDir);
        attachCollaborators(driver);
        return driver;
    }

    public boolean isAttached() {
        return session.isAttached();
    }

    public String getDebuggerAddress() {
        return session.getDebuggerAddress();
    }

    public boolean isDebuggerListening(String debuggerAddress) {
        return session.isDebuggerListening(debuggerAddress);
    }

    public void closeBrowser() {
        if (networkMonitor != null) {
            networkMonitor.close();
        }
        if (consoleLogMonitor != null) {
            consoleLogMonitor.close();
        }
        if (autoWaiter != null && session.isAttached()) {
            autoWaiter.close();
        }
        session.close();
        navigator = null;
        elementInteractor = null;
        elementInspector = null;
        clickabilityChecker = null;
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
        initScriptManager = null;
        autoWaiter = null;
    }

    // -- Locators --

    /**
     * Resolves a locator strategy and value into a {@link By}, optionally narrowed to visible matches
     * and/or the match at a zero-based {@code index}. Pass the result to any {@code By} overload.
     */
    public By locator(String locatorType, String locatorValue, Integer index, boolean visibleOnly) {
        return locators.toBy(locatorType, locatorValue, index, visibleOnly);
    }

    // -- Navigation --

    public void navigateTo(String url) {
        requireAutoWaiter().run("navigate " + url, () -> requireNavigator().navigateTo(url));
    }

    public void back() {
        requireAutoWaiter().run("back", () -> requireNavigator().back());
    }

    public void forward() {
        requireAutoWaiter().run("forward", () -> requireNavigator().forward());
    }

    public void refresh() {
        requireAutoWaiter().run("refresh", () -> requireNavigator().refresh());
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

    public String getPageElementsFiltered(By locator, boolean includeAncestors, boolean includeDescendants,
                                           boolean includeSiblings, int maxDepth, int limit) {
        return requirePageSourceInspector().getFilteredElements(
                locator, includeAncestors, includeDescendants, includeSiblings, maxDepth, limit);
    }

    public String getPageElementsFiltered(String locatorType, String locatorValue, boolean includeAncestors,
                                           boolean includeDescendants, boolean includeSiblings, int maxDepth, int limit) {
        return requirePageSourceInspector().getFilteredElements(
                locatorType, locatorValue, includeAncestors, includeDescendants, includeSiblings, maxDepth, limit);
    }

    // -- Element interaction --

    public void click(By locator) {
        requireAutoWaiter().run("click " + locator, () -> requireElementInteractor().click(locator));
    }

    public void click(String locatorType, String locatorValue) {
        click(locators.toBy(locatorType, locatorValue));
    }

    /**
     * Clicks, retrying for up to {@code waitWhileCovered} while another element (e.g. a loading overlay)
     * intercepts the click.
     */
    public void click(By locator, Duration waitWhileCovered) {
        requireAutoWaiter().runCoverable("click " + locator,
                () -> requireElementInteractor().click(locator, waitWhileCovered));
    }

    public void sendKeys(By locator, String text) {
        sendKeys(locator, text, false);
    }

    public void sendKeys(By locator, String text, boolean clearFirst) {
        requireAutoWaiter().run("sendKeys " + locator,
                () -> requireElementInteractor().sendKeys(locator, text, clearFirst));
    }

    public void sendKeys(String locatorType, String locatorValue, String text) {
        sendKeys(locators.toBy(locatorType, locatorValue), text, false);
    }

    public void sendKeys(String locatorType, String locatorValue, String text, boolean clearFirst) {
        sendKeys(locators.toBy(locatorType, locatorValue), text, clearFirst);
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

    /**
     * Whether the element is displayed, enabled and not covered by another element - scrolling it into
     * view first if needed, as a click would.
     */
    public ClickabilityChecker.Clickability checkClickable(By locator) {
        return requireClickabilityChecker().check(locator);
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

    // -- Session-wide auto-waits --

    /**
     * Sets this session's wait strategy: Selenium's implicit wait, and the waits every action makes
     * afterwards for the network and animations to settle. {@link AutoWaitSettings#NONE} restores plain
     * Selenium behaviour.
     */
    public void setAutoWait(AutoWaitSettings settings) {
        requireAutoWaiter().apply(settings);
    }

    public AutoWaitSettings getAutoWait() {
        return requireAutoWaiter().getSettings();
    }

    /**
     * @return whether the auto-wait tracker sees requests from the start of each page load (Chrome/Edge),
     * rather than only once the page has loaded
     */
    public boolean isAutoWaitTrackingFromPageStart() {
        return requireAutoWaiter().tracksFromPageStart();
    }

    /**
     * @return what the last action set off and waited for, or {@code null} if nothing was tracked for it
     */
    public WaitRecord getLastWaitRecord() {
        return requireAutoWaiter().lastRecord();
    }

    public List<WaitRecord> getWaitRecords() {
        return requireAutoWaiter().records();
    }

    public void clearWaitRecords() {
        requireAutoWaiter().clearRecords();
    }

    /**
     * @return a Java {@code Waits} helper class that makes the same waits as {@code settings}, for
     * generated tests
     */
    public String generateWaitsHelper(AutoWaitSettings settings) {
        return waitCodeGenerator.helperClass(settings);
    }

    /**
     * @return the Java a test needs right after creating its driver to wait the way {@code settings} does
     */
    public String generateWaitSetup(AutoWaitSettings settings) {
        return waitCodeGenerator.setup(settings);
    }

    // -- Alerts --

    public void acceptAlert() {
        requireAutoWaiter().run("acceptAlert", () -> requireAlertHandler().accept());
    }

    public void dismissAlert() {
        requireAutoWaiter().run("dismissAlert", () -> requireAlertHandler().dismiss());
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

    public String takeFullPageScreenshotBase64() {
        return requireScreenshotTaker().captureFullPageBase64();
    }

    public void takeFullPageScreenshotToFile(String filePath) throws IOException {
        requireScreenshotTaker().captureFullPageToFile(filePath);
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

    /**
     * Executes {@code script} with a one-off script timeout (the W3C default is 30 seconds).
     * A {@code null} timeout keeps the session's current setting.
     */
    public Object executeScript(String script, Duration timeout, Object... args) {
        return requireScriptRunner().executeScript(script, timeout, args);
    }

    // -- Init scripts (Chrome/Edge only) --

    public boolean isInitScriptSupported() {
        return requireInitScriptManager().isSupported();
    }

    public String addInitScript(String source, boolean runNow) {
        return requireInitScriptManager().add(source, runNow);
    }

    public void removeInitScript(String id) {
        requireInitScriptManager().remove(id);
    }

    public void clearInitScripts() {
        requireInitScriptManager().clear();
    }

    public java.util.Map<String, String> getInitScripts() {
        return requireInitScriptManager().list();
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
        requireAutoWaiter().run("hover " + locator, () -> requireActionsHelper().hover(locator));
    }

    public void hover(String locatorType, String locatorValue) {
        hover(locators.toBy(locatorType, locatorValue));
    }

    public void doubleClick(By locator) {
        requireAutoWaiter().run("doubleClick " + locator, () -> requireActionsHelper().doubleClick(locator));
    }

    public void doubleClick(String locatorType, String locatorValue) {
        doubleClick(locators.toBy(locatorType, locatorValue));
    }

    public void rightClick(By locator) {
        requireAutoWaiter().run("rightClick " + locator, () -> requireActionsHelper().rightClick(locator));
    }

    public void rightClick(String locatorType, String locatorValue) {
        rightClick(locators.toBy(locatorType, locatorValue));
    }

    public void clickAndHold(By locator) {
        requireAutoWaiter().run("clickAndHold " + locator, () -> requireActionsHelper().clickAndHold(locator));
    }

    public void clickAndHold(String locatorType, String locatorValue) {
        clickAndHold(locators.toBy(locatorType, locatorValue));
    }

    public void release() {
        requireAutoWaiter().run("releaseClick", () -> requireActionsHelper().release());
    }

    public void dragAndDrop(By source, By target) {
        requireAutoWaiter().run("dragAndDrop " + source + " -> " + target,
                () -> requireActionsHelper().dragAndDrop(source, target));
    }

    public void dragAndDrop(String sourceType, String sourceValue, String targetType, String targetValue) {
        dragAndDrop(locators.toBy(sourceType, sourceValue), locators.toBy(targetType, targetValue));
    }

    public void dragAndDropByOffset(By locator, int xOffset, int yOffset) {
        requireAutoWaiter().run("dragAndDropByOffset " + locator,
                () -> requireActionsHelper().dragAndDropByOffset(locator, xOffset, yOffset));
    }

    public void dragAndDropByOffset(String locatorType, String locatorValue, int xOffset, int yOffset) {
        dragAndDropByOffset(locators.toBy(locatorType, locatorValue), xOffset, yOffset);
    }

    public void pressKey(String keyName) {
        requireAutoWaiter().run("pressKey " + keyName, () -> requireActionsHelper().pressKey(keyName));
    }

    // -- Select dropdowns --

    public void selectByVisibleText(By locator, String text) {
        requireAutoWaiter().run("selectByVisibleText " + locator, () -> requireSelectHelper().selectByVisibleText(locator, text));
    }

    public void selectByVisibleText(String locatorType, String locatorValue, String text) {
        selectByVisibleText(locators.toBy(locatorType, locatorValue), text);
    }

    public void selectByValue(By locator, String value) {
        requireAutoWaiter().run("selectByValue " + locator, () -> requireSelectHelper().selectByValue(locator, value));
    }

    public void selectByValue(String locatorType, String locatorValue, String optionValue) {
        selectByValue(locators.toBy(locatorType, locatorValue), optionValue);
    }

    public void selectByIndex(By locator, int index) {
        requireAutoWaiter().run("selectByIndex " + locator, () -> requireSelectHelper().selectByIndex(locator, index));
    }

    public void selectByIndex(String locatorType, String locatorValue, int index) {
        selectByIndex(locators.toBy(locatorType, locatorValue), index);
    }

    public void deselectAllOptions(By locator) {
        requireAutoWaiter().run("deselectAllOptions " + locator, () -> requireSelectHelper().deselectAll(locator));
    }

    public void deselectAllOptions(String locatorType, String locatorValue) {
        deselectAllOptions(locators.toBy(locatorType, locatorValue));
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
        requireAutoWaiter().run("uploadFile " + locator, () -> requireFileHelper().uploadFile(locator, absoluteFilePath));
    }

    public void uploadFile(String locatorType, String locatorValue, String absoluteFilePath) {
        uploadFile(locators.toBy(locatorType, locatorValue), absoluteFilePath);
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
        this.clickabilityChecker = new ClickabilityChecker(driver);
        this.elementInteractor = new ElementInteractor(driver, locators, clickabilityChecker);
        this.elementInspector = new ElementInspector(driver, locators);
        this.waitHelper = new WaitHelper(driver, locators, clickabilityChecker);
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
        this.initScriptManager = new InitScriptManager(driver);
        this.autoWaiter = new AutoWaiter(driver);
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

    private ClickabilityChecker requireClickabilityChecker() {
        session.requireDriver();
        return clickabilityChecker;
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

    private InitScriptManager requireInitScriptManager() {
        session.requireDriver();
        return initScriptManager;
    }

    private AutoWaiter requireAutoWaiter() {
        session.requireDriver();
        return autoWaiter;
    }
}
