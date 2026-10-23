package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for capturing screenshots. When a file path is
 * given, the image is saved to disk; otherwise it is returned inline as base64 PNG.
 */
@Component
@ToolGroup("screenshots")
public class ScreenshotTools {

    private final BrowserSessions sessions;

    public ScreenshotTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Take a screenshot of the current page. By default captures only the visible viewport; "
            + "set fullPage=true to capture the whole scrollable page (Chrome, Edge, Firefox). Content that scrolls "
            + "inside its own container (not the page) is only captured as far as it is visible. "
            + "Saves to filePath if given, otherwise returns the image as base64 PNG.")
    public String takeScreenshot(
            @McpToolParam(description = "Absolute file path to save the PNG to. If omitted, the image is returned as base64.", required = false)
            String filePath,
            @McpToolParam(description = "Capture the entire scrollable page instead of just the viewport. Defaults to false.", required = false)
            Boolean fullPage) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            boolean full = Boolean.TRUE.equals(fullPage);
            try {
                if (filePath == null || filePath.isBlank()) {
                    return full ? tools.takeFullPageScreenshotBase64() : tools.takeScreenshotBase64();
                }
                if (full) {
                    tools.takeFullPageScreenshotToFile(filePath);
                    return "Full-page screenshot saved to " + filePath;
                }
                tools.takeScreenshotToFile(filePath);
                return "Viewport screenshot saved to " + filePath;
            } catch (UnsupportedOperationException e) {
                return ErrorMessages.of(e);
            } catch (Exception e) {
                return "Failed to take screenshot: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Take a screenshot of a single element. Saves to filePath if given, otherwise returns the image as base64 PNG.")
    public String takeElementScreenshot(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue,
            @McpToolParam(description = "Absolute file path to save the PNG to. If omitted, the image is returned as base64.", required = false)
            String filePath,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                if (filePath == null || filePath.isBlank()) {
                    return tools.takeElementScreenshotBase64(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                }
                tools.takeElementScreenshotToFile(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), filePath);
                return "Element screenshot saved to " + filePath;
            } catch (Exception e) {
                return "Failed to take element screenshot: " + ErrorMessages.of(e);
            }
        }
    }
}
