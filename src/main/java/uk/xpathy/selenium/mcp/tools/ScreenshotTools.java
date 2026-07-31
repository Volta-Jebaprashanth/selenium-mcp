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
public class ScreenshotTools {

    private final Tools tools;

    public ScreenshotTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Take a screenshot of the full current page. Saves to filePath if given, otherwise returns the image as base64 PNG.")
    public String takeScreenshot(
            @McpToolParam(description = "Absolute file path to save the PNG to. If omitted, the image is returned as base64.", required = false)
            String filePath) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                if (filePath == null || filePath.isBlank()) {
                    return tools.takeScreenshotBase64();
                }
                tools.takeScreenshotToFile(filePath);
                return "Screenshot saved to " + filePath;
            } catch (Exception e) {
                return "Failed to take screenshot: " + e.getMessage();
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
            String filePath) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                if (filePath == null || filePath.isBlank()) {
                    return tools.takeElementScreenshotBase64(locatorType, locatorValue);
                }
                tools.takeElementScreenshotToFile(locatorType, locatorValue, filePath);
                return "Element screenshot saved to " + filePath;
            } catch (Exception e) {
                return "Failed to take element screenshot: " + e.getMessage();
            }
        }
    }
}
