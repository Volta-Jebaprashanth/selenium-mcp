package uk.xpathy.selenium.mcp.tools;

import org.openqa.selenium.WebDriverException;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.BrowserOptions;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for browser lifecycle, navigation, and the
 * original click/sendKeys element interactions. Holds the shared {@link Tools} instance
 * and translates its results/exceptions into the human-readable strings MCP clients expect.
 * All state-changing methods synchronize on {@code tools} so that concurrent calls across
 * every {@code @McpTool} class in this package serialize against the single browser session.
 */
@Component
public class BrowserTools {

    private static final int DEFAULT_DEBUG_PORT = 9222;

    private final Tools tools;

    public BrowserTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Open a browser window. Must be called before navigate. If a browser is already open, it is reused.")
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
            String userAgent) {
        synchronized (tools) {
            if (tools.isBrowserOpen()) {
                return "Browser is already open.";
            }
            try {
                BrowserOptions options = new BrowserOptions(
                        Boolean.TRUE.equals(headless), Boolean.TRUE.equals(incognito), windowSize, userAgent);
                tools.openBrowser(browser, options);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
            return tools.getBrowserName() + " browser opened.";
        }
    }

    @McpTool(description = "Connect to a Chrome or Edge browser that is already running with remote debugging enabled "
            + "(started with --remote-debugging-port=<port> and a non-default --user-data-dir), instead of launching a new one. "
            + "Controls whichever tab is currently active; use the window tools to switch tabs. "
            + "closeBrowser will disconnect and leave the browser running.")
    public String connectToBrowser(
            @McpToolParam(description = "Browser that is running: chrome or edge. Defaults to chrome.", required = false)
            String browser,
            @McpToolParam(description = "Remote-debugging address as host:port. Defaults to 127.0.0.1:9222.", required = false)
            String debuggerAddress) {
        synchronized (tools) {
            if (tools.isBrowserOpen()) {
                return "Browser is already open. Call closeBrowser first to connect to a different one.";
            }
            try {
                tools.attachBrowser(browser, debuggerAddress);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return e.getMessage();
            } catch (WebDriverException e) {
                return "Failed to connect to the browser: " + firstLine(e.getMessage());
            }
            return "Connected to " + tools.getBrowserName() + " at " + tools.getDebuggerAddress()
                    + " (current page: " + tools.getCurrentUrl() + "). closeBrowser will disconnect and leave it running.";
        }
    }

    @McpTool(description = "Launch Chrome or Edge with remote debugging enabled and connect to it. Unlike openBrowser, "
            + "the browser keeps running after closeBrowser, so test code can attach to the same browser via "
            + "ChromeOptions.setExperimentalOption(\"debuggerAddress\", \"127.0.0.1:<port>\") - e.g. run a failing test "
            + "up to the failure, then inspect the live page here. The test and this server should take turns rather than "
            + "drive the browser at the same time. If a browser is already listening on the port, connects to it instead.")
    public String launchDebugBrowser(
            @McpToolParam(description = "Browser to launch: chrome or edge. Defaults to chrome.", required = false)
            String browser,
            @McpToolParam(description = "Remote-debugging port. Defaults to 9222.", required = false)
            Integer port,
            @McpToolParam(description = "Profile directory for the browser. Defaults to a dedicated folder under the "
                    + "system temp dir; Chrome refuses remote debugging on your everyday profile.", required = false)
            String userDataDir) {
        synchronized (tools) {
            if (tools.isBrowserOpen()) {
                return "Browser is already open. Call closeBrowser first to launch a debug browser.";
            }
            int debugPort = port == null ? DEFAULT_DEBUG_PORT : port;
            String address = "127.0.0.1:" + debugPort;
            try {
                if (tools.isDebuggerListening(address)) {
                    tools.attachBrowser(browser, address);
                    return "A browser was already listening on " + address + "; connected to "
                            + tools.getBrowserName() + " there (current page: " + tools.getCurrentUrl() + ").";
                }
                tools.launchDebugBrowser(browser, debugPort, userDataDir);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return e.getMessage();
            } catch (WebDriverException e) {
                return "Failed to launch the debug browser: " + firstLine(e.getMessage());
            }
            return "Launched " + tools.getBrowserName() + " with remote debugging on " + address
                    + ". Test code can attach with debuggerAddress " + address
                    + ". closeBrowser will disconnect and leave it running.";
        }
    }

    @McpTool(description = "Close the currently open browser and release the driver. A browser attached via "
            + "connectToBrowser or launchDebugBrowser is only disconnected and keeps running.")
    public String closeBrowser() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open.";
            }
            if (tools.isAttached()) {
                String message = "Disconnected from " + tools.getBrowserName() + " at " + tools.getDebuggerAddress()
                        + "; the browser is still running.";
                tools.closeBrowser();
                return message;
            }
            tools.closeBrowser();
            return "Browser closed.";
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "unknown error";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    @McpTool(description = "Navigate the currently open browser to the given URL.")
    public String navigate(
            @McpToolParam(description = "The URL to navigate to", required = true)
            String url) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            tools.navigateTo(url);
            return "Navigated to " + url;
        }
    }

    @McpTool(description = "Navigate back to the previous page in browser history.")
    public String back() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            tools.back();
            return "Navigated back.";
        }
    }

    @McpTool(description = "Navigate forward to the next page in browser history.")
    public String forward() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            tools.forward();
            return "Navigated forward.";
        }
    }

    @McpTool(description = "Reload the current page in the open browser, equivalent to pressing the browser refresh button. Page state such as form input and scroll position may be lost.")
    public String refresh() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            tools.refresh();
            return "Page refreshed.";
        }
    }

    @McpTool(description = "Get the URL of the current page.")
    public String getCurrentUrl() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            return tools.getCurrentUrl();
        }
    }

    @McpTool(description = "Get the title of the current page.")
    public String getTitle() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            return tools.getTitle();
        }
    }

    @McpTool(description = "Get the full HTML page source of the currently open browser.")
    public String getPageSource() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            return tools.getPageSource();
        }
    }

    @McpTool(description = "Click an element on the current page, located by a strategy and value.")
    public String click(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            try {
                tools.click(locatorType, locatorValue);
                return "Clicked element located by " + locatorType + "=" + locatorValue;
            } catch (Exception e) {
                return "Failed to click element: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Send keys (type text) into an element on the current page, located by a strategy and value.")
    public String sendKeys(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue,
            @McpToolParam(description = "The text to type into the element", required = true)
            String text,
            @McpToolParam(description = "If true, clears the element's existing content before typing. Defaults to false.", required = false)
            Boolean clearFirst) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open. Call openBrowser first.";
            }
            try {
                tools.sendKeys(locatorType, locatorValue, text, Boolean.TRUE.equals(clearFirst));
                return "Sent keys to element located by " + locatorType + "=" + locatorValue;
            } catch (Exception e) {
                return "Failed to send keys: " + e.getMessage();
            }
        }
    }
}
