package com.voltajeba.selenium.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import com.voltajeba.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for capturing screenshots. When a file path is
 * given, the image is saved to disk; otherwise it is returned as MCP image content, which
 * clients show to the model as a picture rather than a wall of base64 text.
 * <p>
 * These are the only tools that return a {@link CallToolResult} instead of a {@code String},
 * since a string can't carry an image. Messages - success with a file path, or failure - are
 * still plain text, and failures are flagged {@code isError}.
 */
@Component
@ToolGroup("screenshots")
public class ScreenshotTools {

    /**
     * Base64 length above which an image isn't returned inline: around 3.75 MB of PNG, under the
     * ~5 MB per-image limit common to MCP clients and model APIs.
     */
    static final int MAX_INLINE_BASE64_LENGTH = 5_000_000;

    private final BrowserSessions sessions;

    public ScreenshotTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Take a screenshot of the current page. By default captures only the visible viewport; "
            + "set fullPage=true to capture the whole scrollable page (Chrome, Edge, Firefox). Content that scrolls "
            + "inside its own container (not the page) is only captured as far as it is visible. "
            + "Saves to filePath if given, otherwise returns the PNG as an image you can look at directly.")
    public CallToolResult takeScreenshot(
            @McpToolParam(description = "Absolute file path to save the PNG to. If omitted, the image is returned directly.", required = false)
            String filePath,
            @McpToolParam(description = "Capture the entire scrollable page instead of just the viewport. Defaults to false.", required = false)
            Boolean fullPage) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return error("No browser is open. Call openBrowser first.");
            boolean full = Boolean.TRUE.equals(fullPage);
            try {
                if (filePath == null || filePath.isBlank()) {
                    return image(full ? tools.takeFullPageScreenshotBase64() : tools.takeScreenshotBase64(),
                            full ? "Full-page screenshot" : "Viewport screenshot");
                }
                if (full) {
                    tools.takeFullPageScreenshotToFile(filePath);
                    return text("Full-page screenshot saved to " + filePath);
                }
                tools.takeScreenshotToFile(filePath);
                return text("Viewport screenshot saved to " + filePath);
            } catch (UnsupportedOperationException e) {
                return error(sessions.label() + ErrorMessages.of(e));
            } catch (Exception e) {
                return error(sessions.label() + "Failed to take screenshot: " + ErrorMessages.of(e));
            }
        }
    }

    @McpTool(description = "Take a screenshot of a single element. Saves to filePath if given, otherwise returns the PNG "
            + "as an image you can look at directly.")
    public CallToolResult takeElementScreenshot(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue,
            @McpToolParam(description = "Absolute file path to save the PNG to. If omitted, the image is returned directly.", required = false)
            String filePath,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return error("No browser is open. Call openBrowser first.");
            try {
                if (filePath == null || filePath.isBlank()) {
                    return image(tools.takeElementScreenshotBase64(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly))),
                            "Element screenshot");
                }
                tools.takeElementScreenshotToFile(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), filePath);
                return text("Element screenshot saved to " + filePath);
            } catch (Exception e) {
                return error(sessions.label() + "Failed to take element screenshot: " + ErrorMessages.of(e));
            }
        }
    }

    /**
     * The PNG as image content with a one-line caption, or - if it is too big to send inline - an error
     * asking for a file path instead.
     */
    CallToolResult image(String base64Png, String caption) {
        if (base64Png.length() > MAX_INLINE_BASE64_LENGTH) {
            return error(sessions.label() + caption + " is too large to return inline (~"
                    + base64Png.length() * 3 / 4 / 1_000_000 + " MB). Pass filePath to save it to disk instead.");
        }
        return CallToolResult.builder()
                .addContent(new ImageContent(null, base64Png, "image/png", null))
                .addTextContent(sessions.label() + caption + ".")
                .build();
    }

    private static CallToolResult text(String message) {
        return CallToolResult.builder().addTextContent(message).build();
    }

    private static CallToolResult error(String message) {
        return CallToolResult.builder().addTextContent(message).isError(true).build();
    }
}
