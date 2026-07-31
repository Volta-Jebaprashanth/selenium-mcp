package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for mouse gestures (hover, double-click,
 * right-click, drag and drop) and single key presses, via Selenium's Actions API.
 */
@Component
public class ActionsTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName";

    private final Tools tools;

    public ActionsTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Move the mouse over an element (hover), e.g. to reveal a dropdown menu.")
    public String hover(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.hover(locatorType, locatorValue);
                return "Hovered over element.";
            } catch (Exception e) {
                return "Failed to hover: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Double-click an element.")
    public String doubleClick(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.doubleClick(locatorType, locatorValue);
                return "Double-clicked element.";
            } catch (Exception e) {
                return "Failed to double-click: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Right-click (context click) an element.")
    public String rightClick(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.rightClick(locatorType, locatorValue);
                return "Right-clicked element.";
            } catch (Exception e) {
                return "Failed to right-click: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Press and hold the left mouse button down on an element. Pair with releaseClick.")
    public String clickAndHold(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.clickAndHold(locatorType, locatorValue);
                return "Clicked and held element.";
            } catch (Exception e) {
                return "Failed to click and hold: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Release a previously held left mouse button (see clickAndHold).")
    public String releaseClick() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.release();
                return "Released mouse button.";
            } catch (Exception e) {
                return "Failed to release: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Drag an element from source and drop it onto target.")
    public String dragAndDrop(
            @McpToolParam(description = "Locator strategy for the source element", required = true) String sourceLocatorType,
            @McpToolParam(description = "Locator value for the source element", required = true) String sourceLocatorValue,
            @McpToolParam(description = "Locator strategy for the target element", required = true) String targetLocatorType,
            @McpToolParam(description = "Locator value for the target element", required = true) String targetLocatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.dragAndDrop(sourceLocatorType, sourceLocatorValue, targetLocatorType, targetLocatorValue);
                return "Dragged element to target.";
            } catch (Exception e) {
                return "Failed to drag and drop: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Drag an element by a pixel offset instead of onto another element.")
    public String dragAndDropByOffset(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Horizontal pixels to drag by", required = true) Integer xOffset,
            @McpToolParam(description = "Vertical pixels to drag by", required = true) Integer yOffset) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.dragAndDropByOffset(locatorType, locatorValue, xOffset, yOffset);
                return "Dragged element by offset (" + xOffset + ", " + yOffset + ")";
            } catch (Exception e) {
                return "Failed to drag by offset: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Send a single named key (e.g. ENTER, TAB, ESCAPE, ARROW_DOWN) to whichever element currently has focus.")
    public String pressKey(
            @McpToolParam(description = "Key name matching a Selenium Keys constant, e.g. ENTER, TAB, ESCAPE", required = true) String keyName) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.pressKey(keyName);
                return "Pressed key " + keyName;
            } catch (Exception e) {
                return "Failed to press key: " + e.getMessage();
            }
        }
    }
}
