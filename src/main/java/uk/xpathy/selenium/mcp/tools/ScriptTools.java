package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for JavaScript execution and scrolling.
 */
@Component
public class ScriptTools {

    private final Tools tools;

    public ScriptTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Execute arbitrary JavaScript in the context of the current page and return its result as a string.")
    public String executeScript(
            @McpToolParam(description = "JavaScript source to execute. Use 'return' to produce a result.", required = true)
            String script) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Object result = tools.executeScript(script);
                return result == null ? "(null)" : result.toString();
            } catch (Exception e) {
                return "Failed to execute script: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Scroll the page so that an element is in view, located by a strategy and value.")
    public String scrollIntoView(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.scrollIntoView(locatorType, locatorValue);
                return "Scrolled element into view.";
            } catch (Exception e) {
                return "Failed to scroll into view: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Scroll the page by the given horizontal and vertical pixel offsets.")
    public String scrollBy(
            @McpToolParam(description = "Horizontal pixels to scroll", required = true) Integer x,
            @McpToolParam(description = "Vertical pixels to scroll", required = true) Integer y) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.scrollBy(x, y);
                return "Scrolled by (" + x + ", " + y + ")";
            } catch (Exception e) {
                return "Failed to scroll: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Scroll to the top of the page.")
    public String scrollToTop() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.scrollToTop();
                return "Scrolled to top.";
            } catch (Exception e) {
                return "Failed to scroll to top: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Scroll to the bottom of the page.")
    public String scrollToBottom() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.scrollToBottom();
                return "Scrolled to bottom.";
            } catch (Exception e) {
                return "Failed to scroll to bottom: " + e.getMessage();
            }
        }
    }
}
