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
public class FileTools {

    private final Tools tools;

    public FileTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Upload a local file by sending its absolute path to an <input type=\"file\"> element. The file must exist on the machine running the browser.")
    public String uploadFile(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the file input element", required = true)
            String locatorValue,
            @McpToolParam(description = "Absolute path of the local file to upload", required = true)
            String absoluteFilePath) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.uploadFile(locatorType, locatorValue, absoluteFilePath);
                return "Uploaded file " + absoluteFilePath;
            } catch (Exception e) {
                return "Failed to upload file: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Print the current page to PDF. Saves to filePath if given, otherwise returns the PDF as base64.")
    public String printToPdf(
            @McpToolParam(description = "Absolute file path to save the PDF to. If omitted, the PDF is returned as base64.", required = false)
            String filePath) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                if (filePath == null || filePath.isBlank()) {
                    return tools.printToPdfBase64();
                }
                tools.printToPdfFile(filePath);
                return "PDF saved to " + filePath;
            } catch (Exception e) {
                return "Failed to print to PDF: " + e.getMessage();
            }
        }
    }
}
