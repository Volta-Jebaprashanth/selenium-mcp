package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for explicit waits. Every wait method blocks
 * up to the given timeout and returns a failure string (rather than throwing) if the
 * condition is never met.
 * <p>
 * For waiting on in-flight network requests to settle (e.g. after a click that triggers
 * XHR/fetch calls), see {@code waitForNetworkIdle} in {@link NetworkTools}.
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

    @McpTool(description = "Wait until the page has finished loading (document.readyState is \"complete\"), up to the given timeout.")
    public String waitForPageLoad(
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForPageLoad(resolveTimeout(timeoutSeconds));
                return "Page has finished loading.";
            } catch (Exception e) {
                return "Timed out waiting for page to load: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until a custom JavaScript expression evaluates truthy, up to the given timeout. "
            + "Use \"return\" for a full function body (e.g. \"return document.title === 'Done'\"), or a bare "
            + "boolean expression (e.g. \"!!document.querySelector('.ready')\").")
    public String waitForJsCondition(
            @McpToolParam(description = "JavaScript expression or \"return ...\" statement that should evaluate truthy", required = true) String script,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForJsCondition(script, resolveTimeout(timeoutSeconds));
                return "Condition is now true.";
            } catch (Exception e) {
                return "Timed out waiting for JavaScript condition: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until the number of elements matching a locator equals an expected count, up to the given timeout.")
    public String waitForElementCount(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the elements", required = true) String locatorValue,
            @McpToolParam(description = "The expected number of matching elements", required = true) Integer expectedCount,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForElementCount(locatorType, locatorValue, expectedCount, resolveTimeout(timeoutSeconds));
                return "Element count is now " + expectedCount + ".";
            } catch (Exception e) {
                return "Timed out waiting for element count: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until an element's attribute or DOM property equals an expected value, up to the given timeout.")
    public String waitForAttributeToBe(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "The attribute or DOM property name to check", required = true) String attribute,
            @McpToolParam(description = "The expected value of the attribute", required = true) String expectedValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForAttributeToBe(locatorType, locatorValue, attribute, expectedValue, resolveTimeout(timeoutSeconds));
                return "Attribute \"" + attribute + "\" is now \"" + expectedValue + "\".";
            } catch (Exception e) {
                return "Timed out waiting for attribute value: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Wait until the number of open windows/tabs equals an expected count, up to the given timeout. "
            + "Useful after an action expected to open or close a tab.")
    public String waitForNumberOfWindowsToBe(
            @McpToolParam(description = "The expected number of open windows/tabs", required = true) Integer expectedCount,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForNumberOfWindowsToBe(expectedCount, resolveTimeout(timeoutSeconds));
                return "Number of windows is now " + expectedCount + ".";
            } catch (Exception e) {
                return "Timed out waiting for window count: " + e.getMessage();
            }
        }
    }

    private int resolveTimeout(Integer timeoutSeconds) {
        return timeoutSeconds == null ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }
}
