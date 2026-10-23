package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for switching focus between frames/iframes.
 */
@Component
@ToolGroup("frames")
public class FrameTools {

    private final BrowserSessions sessions;

    public FrameTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Switch the driver's focus to a frame/iframe by its zero-based index on the page.")
    public String switchToFrameByIndex(
            @McpToolParam(description = "Zero-based index of the frame", required = true) Integer index) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.switchToFrameByIndex(index);
                return "Switched to frame at index " + index;
            } catch (Exception e) {
                return sessions.label() + "Failed to switch frame: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Switch the driver's focus to a frame/iframe by its name or id attribute.")
    public String switchToFrameByNameOrId(
            @McpToolParam(description = "The name or id attribute of the frame", required = true) String nameOrId) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.switchToFrameByNameOrId(nameOrId);
                return "Switched to frame " + nameOrId;
            } catch (Exception e) {
                return sessions.label() + "Failed to switch frame: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Switch the driver's focus to a frame/iframe located by a strategy and value.")
    public String switchToFrame(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)", required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the frame element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.switchToFrame(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return "Switched to frame located by " + locatorType + "=" + locatorValue;
            } catch (Exception e) {
                return sessions.label() + "Failed to switch frame: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Switch the driver's focus back to the main document, out of any frames.")
    public String switchToDefaultContent() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.switchToDefaultContent();
                return "Switched to default content.";
            } catch (Exception e) {
                return sessions.label() + "Failed to switch to default content: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Switch the driver's focus to the parent frame of the current frame.")
    public String switchToParentFrame() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.switchToParentFrame();
                return "Switched to parent frame.";
            } catch (Exception e) {
                return sessions.label() + "Failed to switch to parent frame: " + ErrorMessages.of(e);
            }
        }
    }
}
