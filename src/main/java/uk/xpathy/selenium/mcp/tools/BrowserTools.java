package uk.xpathy.selenium.mcp.tools;

import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebDriverException;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.AutoWaitSettings;
import uk.xpathy.selenium.mcp.webdriver.BrowserOptions;
import uk.xpathy.selenium.mcp.webdriver.Tools;
import uk.xpathy.selenium.mcp.webdriver.WaitRecord;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * MCP-facing wrapper around {@link Tools} for browser sessions, navigation, and the original
 * click/sendKeys element interactions, translating results/exceptions into the human-readable
 * strings MCP clients expect.
 * <p>
 * Opening a browser creates a named session in {@link BrowserSessions} and makes it active; every
 * other tool, here and in the other {@code @McpTool} classes, acts on the active session. All methods
 * synchronize on {@code sessions} so that concurrent calls serialize against the browsers.
 */
@Component
public class BrowserTools {

    private static final int DEFAULT_DEBUG_PORT = 9222;
    private static final int DEFAULT_WAIT_IF_COVERED_SECONDS = 5;
    private static final String NO_BROWSER = "No browser is open. Call openBrowser first.";
    private static final String SESSION_DESC = "Name for this browser session (1-32 letters, digits, '-' or '_'). "
            + "Only needed to run several browsers at once: the first browser is named 'default', and opening "
            + "another alongside it requires a name. The new session becomes the active one that all tools act on.";

    private static final String IMPLICIT_WAIT_DESC = "Wait strategy, to match the framework you are generating tests "
            + "for: Selenium implicit wait in seconds. Omitted: none, as in plain Selenium. See setAutoWait for all options.";
    private static final String NETWORK_IDLE_DESC = "Wait strategy: after each action, wait until no fetch/XHR request "
            + "has been in flight for this many milliseconds, and report which steps need a network wait in the generated "
            + "test. Recommended: 500. Omitted: no network wait. See setAutoWait.";
    private static final String ANIMATION_IDLE_DESC = "Wait strategy: after each action, wait until no animation has run "
            + "for this many milliseconds, and report which steps need an animation wait in the generated test. "
            + "Recommended: 300. Omitted: no animation wait. See setAutoWait.";

    private final BrowserSessions sessions;

    public BrowserTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Open a browser window. Must be called before navigate. If a browser is already open, it is "
            + "reused - unless a new session name is given, which opens another browser alongside it.")
    public String openBrowser(
            @McpToolParam(description = "Browser to launch: chrome, firefox, or edge. Defaults to chrome.", required = false)
            String browser,
            @McpToolParam(description = "Launch the browser in headless mode. Defaults to false.", required = false)
            Boolean headless,
            @McpToolParam(description = "Launch the browser in incognito/private mode. Defaults to false.", required = false)
            Boolean incognito,
            @McpToolParam(description = "Initial window size as WIDTHxHEIGHT, e.g. 1920x1080.", required = false)
            String windowSize,
            @McpToolParam(description = "Custom User-Agent string to launch the browser with.", required = false)
            String userAgent,
            @McpToolParam(description = IMPLICIT_WAIT_DESC, required = false)
            Integer implicitWaitSeconds,
            @McpToolParam(description = NETWORK_IDLE_DESC, required = false)
            Long networkIdleMillis,
            @McpToolParam(description = ANIMATION_IDLE_DESC, required = false)
            Long animationIdleMillis,
            @McpToolParam(description = SESSION_DESC, required = false)
            String session) {
        synchronized (sessions) {
            String note = pruneDeadSessions();
            String name = newSessionName(session);
            String refusal = refuseNewSession(name, "open another browser");
            if (refusal != null) {
                return note + refusal;
            }
            try {
                BrowserOptions options = new BrowserOptions(
                        Boolean.TRUE.equals(headless), Boolean.TRUE.equals(incognito), windowSize, userAgent);
                AutoWaitSettings waits = waitSettings(implicitWaitSeconds, networkIdleMillis, animationIdleMillis);
                Tools tools = sessions.open(name, t -> {
                    t.openBrowser(browser, options);
                    t.setAutoWait(waits);
                });
                return note + tools.getBrowserName() + " browser opened" + sessionSuffix(name) + "."
                        + AutoWaitMessages.sessionOpened(tools);
            } catch (IllegalArgumentException e) {
                return note + ErrorMessages.of(e);
            } catch (Exception e) {
                return note + "Failed to open the browser: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Connect to a Chrome or Edge browser that is already running with remote debugging enabled "
            + "(started with --remote-debugging-port=<port> and a non-default --user-data-dir), instead of launching a new one. "
            + "Controls whichever tab is currently active; use the window tools to switch tabs. "
            + "closeBrowser will disconnect and leave the browser running. "
            + "Pass a session name to connect to several debug browsers at once.")
    public String connectToBrowser(
            @McpToolParam(description = "Browser that is running: chrome or edge. Defaults to chrome.", required = false)
            String browser,
            @McpToolParam(description = "Remote-debugging address as host:port. Defaults to 127.0.0.1:9222.", required = false)
            String debuggerAddress,
            @McpToolParam(description = IMPLICIT_WAIT_DESC, required = false)
            Integer implicitWaitSeconds,
            @McpToolParam(description = NETWORK_IDLE_DESC, required = false)
            Long networkIdleMillis,
            @McpToolParam(description = ANIMATION_IDLE_DESC, required = false)
            Long animationIdleMillis,
            @McpToolParam(description = SESSION_DESC, required = false)
            String session) {
        synchronized (sessions) {
            String note = pruneDeadSessions();
            String name = newSessionName(session);
            String refusal = refuseNewSession(name, "connect to another browser");
            if (refusal != null) {
                return note + refusal;
            }
            try {
                String owner = sessions.sessionAttachedTo(debuggerAddress);
                if (owner != null) {
                    return note + "Session '" + owner + "' is already connected to that browser. Call switchSession to use it.";
                }
                AutoWaitSettings waits = waitSettings(implicitWaitSeconds, networkIdleMillis, animationIdleMillis);
                Tools tools = sessions.open(name, t -> {
                    t.attachBrowser(browser, debuggerAddress);
                    t.setAutoWait(waits);
                });
                return note + "Connected to " + tools.getBrowserName() + " at " + tools.getDebuggerAddress() + sessionSuffix(name)
                        + " (current page: " + tools.getCurrentUrl() + "). closeBrowser will disconnect and leave it running."
                        + AutoWaitMessages.sessionOpened(tools);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return note + ErrorMessages.of(e);
            } catch (WebDriverException e) {
                return note + "Failed to connect to the browser: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Launch Chrome or Edge with remote debugging enabled and connect to it. Unlike openBrowser, "
            + "the browser keeps running after closeBrowser, so test code can attach to the same browser via "
            + "ChromeOptions.setExperimentalOption(\"debuggerAddress\", \"127.0.0.1:<port>\") - e.g. run a failing test "
            + "up to the failure, then inspect the live page here. The test and this server should take turns rather than "
            + "drive the browser at the same time. If a browser is already listening on the port, connects to it instead. "
            + "Pass a session name to run several debug browsers at once; each gets its own port.")
    public String launchDebugBrowser(
            @McpToolParam(description = "Browser to launch: chrome or edge. Defaults to chrome.", required = false)
            String browser,
            @McpToolParam(description = "Remote-debugging port. Defaults to 9222, or the next port up that no other "
                    + "session is using.", required = false)
            Integer port,
            @McpToolParam(description = "Profile directory for the browser. Defaults to a dedicated folder under the "
                    + "system temp dir; Chrome refuses remote debugging on your everyday profile.", required = false)
            String userDataDir,
            @McpToolParam(description = IMPLICIT_WAIT_DESC, required = false)
            Integer implicitWaitSeconds,
            @McpToolParam(description = NETWORK_IDLE_DESC, required = false)
            Long networkIdleMillis,
            @McpToolParam(description = ANIMATION_IDLE_DESC, required = false)
            Long animationIdleMillis,
            @McpToolParam(description = SESSION_DESC, required = false)
            String session) {
        synchronized (sessions) {
            String note = pruneDeadSessions();
            String name = newSessionName(session);
            String refusal = refuseNewSession(name, "launch another debug browser");
            if (refusal != null) {
                return note + refusal;
            }
            try {
                int debugPort = port != null ? port : firstPortWithoutSession();
                String address = "127.0.0.1:" + debugPort;
                String owner = sessions.sessionAttachedTo(address);
                if (owner != null) {
                    return note + "Session '" + owner + "' is already connected to port " + debugPort
                            + ". Call switchSession to use it, or pass a different port.";
                }
                AutoWaitSettings waits = waitSettings(implicitWaitSeconds, networkIdleMillis, animationIdleMillis);
                if (sessions.active().isDebuggerListening(address)) {
                    Tools tools = sessions.open(name, t -> {
                        t.attachBrowser(browser, address);
                        t.setAutoWait(waits);
                    });
                    return note + "A browser was already listening on " + address + "; connected to "
                            + tools.getBrowserName() + " there" + sessionSuffix(name)
                            + " (current page: " + tools.getCurrentUrl() + ")." + AutoWaitMessages.sessionOpened(tools);
                }
                Tools tools = sessions.open(name, t -> {
                    t.launchDebugBrowser(browser, debugPort, userDataDir);
                    t.setAutoWait(waits);
                });
                return note + "Launched " + tools.getBrowserName() + " with remote debugging on " + address
                        + sessionSuffix(name) + ". Test code can attach with debuggerAddress " + address
                        + ". closeBrowser will disconnect and leave it running." + AutoWaitMessages.sessionOpened(tools);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return note + ErrorMessages.of(e);
            } catch (WebDriverException e) {
                return note + "Failed to launch the debug browser: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "List the open browser sessions - name, browser, whether it was launched or attached over "
            + "a debugging port, and current URL - and which one is active.")
    public String listSessions() {
        synchronized (sessions) {
            String note = pruneDeadSessions();
            Map<String, Tools> all = sessions.all();
            if (all.isEmpty()) {
                return note + NO_BROWSER;
            }
            StringBuilder sb = new StringBuilder();
            all.forEach((name, tools) -> {
                sb.append(name.equals(sessions.activeName()) ? "* " : "  ").append(name).append(": ")
                        .append(tools.getBrowserName());
                if (tools.isAttached()) {
                    sb.append(", attached at ").append(tools.getDebuggerAddress());
                }
                sb.append(", ").append(currentUrlOf(tools)).append(", waits: ").append(waitsOf(tools)).append('\n');
            });
            return note + sb.append("(* = active session)").toString();
        }
    }

    @McpTool(description = "Make another open browser session the active one, so that all tools act on its browser.")
    public String switchSession(
            @McpToolParam(description = "Name of the session to switch to, as shown by listSessions", required = true)
            String session) {
        synchronized (sessions) {
            String note = pruneDeadSessions();
            if (session == null || !sessions.switchTo(session.strip())) {
                return note + "No session named '" + session + "'. " + openSessionNames();
            }
            Tools tools = sessions.active();
            return note + "Switched to session '" + sessions.activeName() + "' (" + tools.getBrowserName() + ", "
                    + currentUrlOf(tools) + ").";
        }
    }

    @McpTool(description = "Close a browser and release its driver: the active session, the one named, or all of them. "
            + "A browser attached via connectToBrowser or launchDebugBrowser is only disconnected and keeps running. "
            + "When the active session closes, the most recently opened remaining one becomes active.")
    public String closeBrowser(
            @McpToolParam(description = "Name of the session to close. Defaults to the active session.", required = false)
            String session,
            @McpToolParam(description = "Close every open session. Defaults to false.", required = false)
            Boolean all) {
        synchronized (sessions) {
            if (sessions.isEmpty()) {
                return "No browser is open.";
            }
            if (Boolean.TRUE.equals(all)) {
                boolean anyAttached = sessions.all().values().stream().anyMatch(Tools::isAttached);
                List<String> closed = sessions.closeAll();
                return "Closed " + closed.size() + " session(s): " + String.join(", ", closed) + "."
                        + (anyAttached ? " Attached browsers were only disconnected and are still running." : "");
            }
            String name = session == null || session.isBlank() ? sessions.activeName() : session.strip();
            if (!sessions.has(name)) {
                return "No session named '" + name + "'. " + openSessionNames();
            }
            Tools tools = sessions.all().get(name);
            String report = pendingReport(tools);
            String message = tools.isAttached()
                    ? "Disconnected from " + tools.getBrowserName() + " at " + tools.getDebuggerAddress()
                            + "; the browser is still running."
                    : "Browser closed.";
            try {
                sessions.close(name);
            } catch (Exception e) {
                message = "Session '" + name + "' was closed, but the browser reported an error: " + ErrorMessages.of(e);
            }
            message += report;
            return sessions.isEmpty() ? message : message + " Active session is now '" + sessions.activeName() + "'.";
        }
    }


    @McpTool(description = "Navigate the currently open browser to the given URL.")
    public String navigate(
            @McpToolParam(description = "The URL to navigate to", required = true)
            String url) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            try {
                tools.navigateTo(url);
                return sessions.label() + "Navigated to " + url + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to navigate: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Navigate back to the previous page in browser history.")
    public String back() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            try {
                tools.back();
                return sessions.label() + "Navigated back." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to navigate back: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Navigate forward to the next page in browser history.")
    public String forward() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            try {
                tools.forward();
                return sessions.label() + "Navigated forward." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to navigate forward: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Reload the current page in the open browser, equivalent to pressing the browser refresh button. Page state such as form input and scroll position may be lost.")
    public String refresh() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            try {
                tools.refresh();
                return sessions.label() + "Page refreshed." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to refresh: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the URL of the current page.")
    public String getCurrentUrl() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            try {
                return sessions.label() + tools.getCurrentUrl();
            } catch (Exception e) {
                return sessions.label() + "Failed to get the current URL: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the title of the current page.")
    public String getTitle() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            try {
                return sessions.label() + tools.getTitle();
            } catch (Exception e) {
                return sessions.label() + "Failed to get the title: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the full HTML page source of the currently open browser.")
    public String getPageSource() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            try {
                return tools.getPageSource();
            } catch (Exception e) {
                return sessions.label() + "Failed to get the page source: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Click an element on the current page, located by a strategy and value. If another element "
            + "covers it (e.g. a loading overlay), the click is retried until it clears, up to waitIfCoveredSeconds; the result "
            + "then says so, since the generated test has to wait for that too.")
    public String click(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly,
            @McpToolParam(description = "Max seconds to keep retrying while another element (e.g. a loading overlay or "
                    + "spinner) intercepts the click. 0 tries once. Defaults to " + DEFAULT_WAIT_IF_COVERED_SECONDS + ".",
                    required = false) Integer waitIfCoveredSeconds) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            int wait = waitIfCoveredSeconds == null ? DEFAULT_WAIT_IF_COVERED_SECONDS : Math.max(0, waitIfCoveredSeconds);
            try {
                tools.click(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), Duration.ofSeconds(wait));
                return sessions.label() + "Clicked element located by " + locatorType + "=" + locatorValue
                        + AutoWaitMessages.afterAction(tools);
            } catch (ElementClickInterceptedException e) {
                return sessions.label() + "Failed to click element: another element still covered it after " + wait
                        + "s. Wait for that element to go away (e.g. waitForInvisible on it) or raise waitIfCoveredSeconds. "
                        + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            } catch (Exception e) {
                return sessions.label() + "Failed to click element: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    @McpTool(description = "Send keys (type text) into an element on the current page, located by a strategy and value.")
    public String sendKeys(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue,
            @McpToolParam(description = "The text to type into the element", required = true)
            String text,
            @McpToolParam(description = "If true, clears the element's existing content before typing. Defaults to false.", required = false)
            Boolean clearFirst,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) {
                return NO_BROWSER;
            }
            try {
                tools.sendKeys(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), text, Boolean.TRUE.equals(clearFirst));
                return sessions.label() + "Sent keys to element located by " + locatorType + "=" + locatorValue
                        + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to send keys: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    /**
     * Drops sessions whose browser has gone away, so a browser closed by hand doesn't keep blocking or
     * answering for its session.
     *
     * @return a note telling the client which sessions were removed, or an empty string
     */
    private String pruneDeadSessions() {
        List<String> dead = sessions.pruneDead();
        if (dead.isEmpty()) {
            return "";
        }
        return (dead.size() == 1 ? "Session '" + dead.getFirst() + "' was" : "Sessions " + String.join(", ", dead) + " were")
                + " removed: the browser is no longer reachable (closed or crashed).\n";
    }

    /**
     * The name to open a new session under: the one requested, else {@code default} when nothing is open yet,
     * else {@code null} - an unnamed open alongside existing sessions is refused rather than guessed at.
     */
    private String newSessionName(String session) {
        if (session != null && !session.isBlank()) {
            return session.strip();
        }
        return sessions.isEmpty() ? BrowserSessions.DEFAULT_SESSION : null;
    }

    /**
     * @return the message to answer with instead of opening session {@code name}, or {@code null} to go ahead.
     * Asking for a session that is already open switches to it.
     */
    private String refuseNewSession(String name, String action) {
        if (name == null) {
            return "A browser is already open (session '" + sessions.activeName() + "'). To " + action
                    + " alongside it, pass a session name; listSessions shows what is open.";
        }
        if (sessions.has(name)) {
            sessions.switchTo(name);
            return "Session '" + name + "' is already open; switched to it.";
        }
        return sessions.checkCanOpen(name);
    }

    private int firstPortWithoutSession() {
        int port = DEFAULT_DEBUG_PORT;
        while (sessions.sessionAttachedTo("127.0.0.1:" + port) != null) {
            port++;
        }
        return port;
    }

    private String sessionSuffix(String name) {
        return sessions.all().size() > 1 || !name.equals(BrowserSessions.DEFAULT_SESSION) ? " as session '" + name + "'" : "";
    }

    private String openSessionNames() {
        return sessions.isEmpty() ? NO_BROWSER : "Open sessions: " + String.join(", ", sessions.all().keySet()) + ".";
    }

    private static AutoWaitSettings waitSettings(Integer implicitWaitSeconds, Long networkIdleMillis, Long animationIdleMillis) {
        return WaitTools.toSettings(implicitWaitSeconds, networkIdleMillis, null, animationIdleMillis, null, null);
    }

    private static String waitsOf(Tools tools) {
        try {
            AutoWaitSettings settings = tools.getAutoWait();
            return settings.isNone() ? "none" : AutoWaitMessages.describe(settings);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * @return the session's wait report, if it has steps that need a wait, since closing loses it
     */
    private static String pendingReport(Tools tools) {
        try {
            if (tools.getWaitRecords().stream().anyMatch(WaitRecord::needsWait)) {
                return "\nThe session's wait report, which closing discards:\n" + AutoWaitMessages.report(tools);
            }
        } catch (Exception ignored) {
            // the browser may already be gone; there is nothing left to report
        }
        return "";
    }

    private static String currentUrlOf(Tools tools) {
        try {
            return tools.getCurrentUrl();
        } catch (Exception e) {
            return "unreachable: " + ErrorMessages.of(e);
        }
    }
}
