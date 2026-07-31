package uk.xpathy.selenium.mcp.tools;

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

    @McpTool(description = "Close the currently open browser and release the driver.")
    public String closeBrowser() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) {
                return "No browser is open.";
            }
            tools.closeBrowser();
            return "Browser closed.";
        }
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

    @McpTool(description = "Refresh the current page.")
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
