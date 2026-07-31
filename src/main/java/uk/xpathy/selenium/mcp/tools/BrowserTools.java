package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools}. Holds the session's {@link Tools} instance
 * and translates its results/exceptions into the human-readable strings MCP clients expect.
 */
@Component
public class BrowserTools {

    private final Tools tools = new Tools();

    @McpTool(description = "Open a browser window. Must be called before navigate. If a browser is already open, it is reused.")
    public synchronized String openBrowser(
            @McpToolParam(description = "Browser to launch: chrome, firefox, or edge. Defaults to chrome.", required = false)
            String browser) {
        if (tools.isBrowserOpen()) {
            return "Browser is already open.";
        }
        try {
            tools.openBrowser(browser);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        return tools.getBrowserName() + " browser opened.";
    }

    @McpTool(description = "Close the currently open browser and release the driver.")
    public synchronized String closeBrowser() {
        if (!tools.isBrowserOpen()) {
            return "No browser is open.";
        }
        tools.closeBrowser();
        return "Browser closed.";
    }

    @McpTool(description = "Navigate the currently open browser to the given URL.")
    public synchronized String navigate(
            @McpToolParam(description = "The URL to navigate to", required = true)
            String url) {
        if (!tools.isBrowserOpen()) {
            return "No browser is open. Call openBrowser first.";
        }
        tools.navigateTo(url);
        return "Navigated to " + url;
    }

    @McpTool(description = "Get the full HTML page source of the currently open browser.")
    public synchronized String getPageSource() {
        if (!tools.isBrowserOpen()) {
            return "No browser is open. Call openBrowser first.";
        }
        return tools.getPageSource();
    }

    @McpTool(description = "Click an element on the current page, located by a strategy and value.")
    public synchronized String click(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue) {
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

    @McpTool(description = "Send keys (type text) into an element on the current page, located by a strategy and value.")
    public synchronized String sendKeys(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue,
            @McpToolParam(description = "The text to type into the element", required = true)
            String text,
            @McpToolParam(description = "If true, clears the element's existing content before typing. Defaults to false.", required = false)
            Boolean clearFirst) {
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
