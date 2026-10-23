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
@ToolGroup("elements")
public class ElementQueryTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)";

    private final BrowserSessions sessions;

    public ElementQueryTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Get the visible text of an element.")
    public String getText(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getText(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
            } catch (Exception e) {
                return sessions.label() + "Failed to get text: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the value of an HTML attribute (or DOM property) of an element.")
    public String getAttribute(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "The attribute name to read, e.g. href, value, class", required = true) String attributeName,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                String value = tools.getAttribute(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), attributeName);
                return value == null ? "(null)" : value;
            } catch (Exception e) {
                return sessions.label() + "Failed to get attribute: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the computed CSS value of a property of an element.")
    public String getCssValue(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "The CSS property name, e.g. color, font-size", required = true) String propertyName,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getCssValue(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), propertyName);
            } catch (Exception e) {
                return sessions.label() + "Failed to get CSS value: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the HTML tag name of an element.")
    public String getTagName(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getTagName(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
            } catch (Exception e) {
                return sessions.label() + "Failed to get tag name: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Check whether an element is displayed (visible) on the page.")
    public String isDisplayed(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.isDisplayed(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly))));
            } catch (Exception e) {
                return sessions.label() + "Failed to check displayed state: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Check whether an element is enabled (not disabled).")
    public String isEnabled(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.isEnabled(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly))));
            } catch (Exception e) {
                return sessions.label() + "Failed to check enabled state: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Check whether an element can actually be clicked right now: displayed, enabled, and not "
            + "covered by another element such as a loading overlay. Returns \"true\", or \"false: <reason>\" - e.g. "
            + "\"false: covered by div.spinner-backdrop\". Scrolls the element into view first if needed, as a click would.")
    public String isClickable(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.checkClickable(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly))).describe();
            } catch (Exception e) {
                return sessions.label() + "Failed to check clickable state: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Check whether a checkbox, radio button, or option element is selected.")
    public String isSelected(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.isSelected(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly))));
            } catch (Exception e) {
                return sessions.label() + "Failed to check selected state: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the rendered width and height of an element in pixels.")
    public String getElementSize(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Dimension size = tools.getElementSize(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return size.getWidth() + "x" + size.getHeight();
            } catch (Exception e) {
                return sessions.label() + "Failed to get element size: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the top-left (x, y) position of an element relative to the page.")
    public String getElementLocation(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Point location = tools.getElementLocation(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return "(" + location.getX() + ", " + location.getY() + ")";
            } catch (Exception e) {
                return sessions.label() + "Failed to get element location: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Check whether at least one element matching the locator exists on the page, without throwing if it doesn't.")
    public String elementExists(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_ALL_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.elementExists(tools.locator(locatorType, locatorValue, null, Boolean.TRUE.equals(visibleOnly))));
            } catch (Exception e) {
                return sessions.label() + "Failed to check element existence: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Count how many elements on the page match the locator.")
    public String countElements(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the elements", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_ALL_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.countElements(tools.locator(locatorType, locatorValue, null, Boolean.TRUE.equals(visibleOnly))));
            } catch (Exception e) {
                return sessions.label() + "Failed to count elements: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the visible text of every element matching the locator, newline-separated.")
    public String getElementsText(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the elements", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_ALL_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                List<String> texts = tools.getElementsText(tools.locator(locatorType, locatorValue, null, Boolean.TRUE.equals(visibleOnly)));
                return String.join("\n", texts);
            } catch (Exception e) {
                return sessions.label() + "Failed to get elements text: " + ErrorMessages.of(e);
            }
        }
    }
}
