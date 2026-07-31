package uk.xpathy.selenium.mcp.tools;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import java.util.List;

/**
 * MCP-facing wrapper around {@link Tools} for read-only element queries (text, attributes,
 * visibility/state, geometry, existence). No method here changes page state.
 */
@Component
public class ElementQueryTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName";

    private final Tools tools;

    public ElementQueryTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Get the visible text of an element.")
    public String getText(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getText(locatorType, locatorValue);
            } catch (Exception e) {
                return "Failed to get text: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the value of an HTML attribute (or DOM property) of an element.")
    public String getAttribute(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "The attribute name to read, e.g. href, value, class", required = true) String attributeName) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                String value = tools.getAttribute(locatorType, locatorValue, attributeName);
                return value == null ? "(null)" : value;
            } catch (Exception e) {
                return "Failed to get attribute: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the computed CSS value of a property of an element.")
    public String getCssValue(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "The CSS property name, e.g. color, font-size", required = true) String propertyName) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getCssValue(locatorType, locatorValue, propertyName);
            } catch (Exception e) {
                return "Failed to get CSS value: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the HTML tag name of an element.")
    public String getTagName(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getTagName(locatorType, locatorValue);
            } catch (Exception e) {
                return "Failed to get tag name: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Check whether an element is displayed (visible) on the page.")
    public String isDisplayed(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.isDisplayed(locatorType, locatorValue));
            } catch (Exception e) {
                return "Failed to check displayed state: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Check whether an element is enabled (not disabled).")
    public String isEnabled(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.isEnabled(locatorType, locatorValue));
            } catch (Exception e) {
                return "Failed to check enabled state: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Check whether a checkbox, radio button, or option element is selected.")
    public String isSelected(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.isSelected(locatorType, locatorValue));
            } catch (Exception e) {
                return "Failed to check selected state: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the rendered width and height of an element in pixels.")
    public String getElementSize(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Dimension size = tools.getElementSize(locatorType, locatorValue);
                return size.getWidth() + "x" + size.getHeight();
            } catch (Exception e) {
                return "Failed to get element size: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the top-left (x, y) position of an element relative to the page.")
    public String getElementLocation(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Point location = tools.getElementLocation(locatorType, locatorValue);
                return "(" + location.getX() + ", " + location.getY() + ")";
            } catch (Exception e) {
                return "Failed to get element location: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Check whether at least one element matching the locator exists on the page, without throwing if it doesn't.")
    public String elementExists(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.elementExists(locatorType, locatorValue));
            } catch (Exception e) {
                return "Failed to check element existence: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Count how many elements on the page match the locator.")
    public String countElements(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the elements", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.countElements(locatorType, locatorValue));
            } catch (Exception e) {
                return "Failed to count elements: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the visible text of every element matching the locator, newline-separated.")
    public String getElementsText(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the elements", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                List<String> texts = tools.getElementsText(locatorType, locatorValue);
                return String.join("\n", texts);
            } catch (Exception e) {
                return "Failed to get elements text: " + e.getMessage();
            }
        }
    }
}
