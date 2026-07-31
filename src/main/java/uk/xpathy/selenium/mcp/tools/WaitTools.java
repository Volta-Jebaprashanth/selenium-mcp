package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for explicit waits. Every wait method blocks
 * up to the given timeout and returns a failure string (rather than throwing) if the
 * condition is never met.
 */
@Component
public class WaitTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName";
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private final Tools tools;

    public WaitTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Wait until an element is visible on the page, up to the given timeout.")
    public String waitForVisible(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForVisible(locatorType, locatorValue, resolveTimeout(timeoutSeconds));
                return "Element is now visible.";
            } catch (Exception e) {
                return "Timed out waiting for element to be visible: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until an element is visible and enabled, ready to be clicked, up to the given timeout.")
    public String waitForClickable(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForClickable(locatorType, locatorValue, resolveTimeout(timeoutSeconds));
                return "Element is now clickable.";
            } catch (Exception e) {
                return "Timed out waiting for element to be clickable: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until an element is present in the DOM (not necessarily visible), up to the given timeout.")
    public String waitForPresent(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForPresent(locatorType, locatorValue, resolveTimeout(timeoutSeconds));
                return "Element is now present in the DOM.";
            } catch (Exception e) {
                return "Timed out waiting for element to be present: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until an element is invisible or removed from the DOM, up to the given timeout.")
    public String waitForInvisible(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForInvisible(locatorType, locatorValue, resolveTimeout(timeoutSeconds));
                return "Element is now invisible.";
            } catch (Exception e) {
                return "Timed out waiting for element to be invisible: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until an element contains the given text, up to the given timeout.")
    public String waitForTextPresent(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "The text expected to appear in the element", required = true) String text,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForTextPresent(locatorType, locatorValue, text, resolveTimeout(timeoutSeconds));
                return "Text is now present in element.";
            } catch (Exception e) {
                return "Timed out waiting for text to appear: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until the page title contains the given text, up to the given timeout.")
    public String waitForTitleContains(
            @McpToolParam(description = "The text expected to appear in the page title", required = true) String text,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForTitleContains(text, resolveTimeout(timeoutSeconds));
                return "Title now contains \"" + text + "\"";
            } catch (Exception e) {
                return "Timed out waiting for title: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until the current URL contains the given text, up to the given timeout.")
    public String waitForUrlContains(
            @McpToolParam(description = "The text expected to appear in the URL", required = true) String text,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForUrlContains(text, resolveTimeout(timeoutSeconds));
                return "URL now contains \"" + text + "\"";
            } catch (Exception e) {
                return "Timed out waiting for URL: " + e.getMessage();
            }
        }
    }

    private int resolveTimeout(Integer timeoutSeconds) {
        return timeoutSeconds == null ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }
}
