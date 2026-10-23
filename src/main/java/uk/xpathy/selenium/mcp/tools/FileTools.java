package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for uploading local files through file inputs
 * and printing the current page to PDF.
 */
@Component
@ToolGroup("files")
public class FileTools {

    private final BrowserSessions sessions;

    public FileTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Upload a local file by sending its absolute path to an <input type=\"file\"> element. The file must exist on the machine running the browser.")
    public String uploadFile(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the file input element", required = true)
            String locatorValue,
            @McpToolParam(description = "Absolute path of the local file to upload", required = true)
            String absoluteFilePath,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.uploadFile(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), absoluteFilePath);
                return "Uploaded file " + absoluteFilePath;
            } catch (Exception e) {
                return sessions.label() + "Failed to upload file: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Print the current page to PDF. Saves to filePath if given, otherwise returns the PDF as base64.")
    public String printToPdf(
            @McpToolParam(description = "Absolute file path to save the PDF to. If omitted, the PDF is returned as base64.", required = false)
            String filePath) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                if (filePath == null || filePath.isBlank()) {
                    return tools.printToPdfBase64();
                }
                tools.printToPdfFile(filePath);
                return "PDF saved to " + filePath;
            } catch (Exception e) {
                return sessions.label() + "Failed to print to PDF: " + ErrorMessages.of(e);
            }
        }
    }
}
