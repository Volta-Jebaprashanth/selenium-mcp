package com.voltajeba.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import com.voltajeba.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for mouse gestures (hover, double-click,
 * right-click, drag and drop) and single key presses, via Selenium's Actions API.
 */
@Component
@ToolGroup("actions")
public class ActionsTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)";

    private final BrowserSessions sessions;

    public ActionsTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Move the mouse over an element (hover), e.g. to reveal a dropdown menu.")
    public String hover(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.hover(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return "Hovered over element." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to hover: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    @McpTool(description = "Double-click the element identified by the given locator in the open browser. Use for controls that react to dblclick, such as editable cells or list items that open on double-click.")
    public String doubleClick(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.doubleClick(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return "Double-clicked element." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to double-click: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    @McpTool(description = "Right-click (context click) an element.")
    public String rightClick(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.rightClick(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return "Right-clicked element." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to right-click: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    @McpTool(description = "Press and hold the left mouse button down on an element. Pair with releaseClick.")
    public String clickAndHold(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.clickAndHold(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return "Clicked and held element." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to click and hold: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    @McpTool(description = "Release a previously held left mouse button (see clickAndHold).")
    public String releaseClick() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.release();
                return "Released mouse button." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to release: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    @McpTool(description = "Drag an element from source and drop it onto target.")
    public String dragAndDrop(
            @McpToolParam(description = "Locator strategy for the source element", required = true) String sourceLocatorType,
            @McpToolParam(description = "Locator value for the source element", required = true) String sourceLocatorValue,
            @McpToolParam(description = "Locator strategy for the target element", required = true) String targetLocatorType,
            @McpToolParam(description = "Locator value for the target element", required = true) String targetLocatorValue) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.dragAndDrop(sourceLocatorType, sourceLocatorValue, targetLocatorType, targetLocatorValue);
                return "Dragged element to target." + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to drag and drop: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    @McpTool(description = "Drag an element by a pixel offset instead of onto another element.")
    public String dragAndDropByOffset(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Horizontal pixels to drag by", required = true) Integer xOffset,
            @McpToolParam(description = "Vertical pixels to drag by", required = true) Integer yOffset,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.dragAndDropByOffset(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), xOffset, yOffset);
                return "Dragged element by offset (" + xOffset + ", " + yOffset + ")" + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to drag by offset: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }

    @McpTool(description = "Send a single named key (e.g. ENTER, TAB, ESCAPE, ARROW_DOWN) to whichever element currently has focus.")
    public String pressKey(
            @McpToolParam(description = "Key name matching a Selenium Keys constant, e.g. ENTER, TAB, ESCAPE", required = true) String keyName) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.pressKey(keyName);
                return "Pressed key " + keyName + AutoWaitMessages.afterAction(tools);
            } catch (Exception e) {
                return sessions.label() + "Failed to press key: " + ErrorMessages.of(e) + AutoWaitMessages.failureHint(tools, e);
            }
        }
    }
}
