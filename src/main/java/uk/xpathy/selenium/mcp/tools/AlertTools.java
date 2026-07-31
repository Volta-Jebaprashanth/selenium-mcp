package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for JavaScript alert/confirm/prompt dialogs.
 */
@Component
public class AlertTools {

    private final Tools tools;

    public AlertTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Accept (click OK on) the currently open alert/confirm/prompt dialog.")
    public String acceptAlert() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.acceptAlert();
                return "Alert accepted.";
            } catch (Exception e) {
                return "Failed to accept alert: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Dismiss (click Cancel on) the currently open alert/confirm/prompt dialog.")
    public String dismissAlert() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.dismissAlert();
                return "Alert dismissed.";
            } catch (Exception e) {
                return "Failed to dismiss alert: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the text of the currently open alert/confirm/prompt dialog.")
    public String getAlertText() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getAlertText();
            } catch (Exception e) {
                return "Failed to get alert text: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Type text into a JavaScript prompt dialog.")
    public String sendKeysToAlert(
            @McpToolParam(description = "The text to type into the prompt", required = true) String text) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.sendKeysToAlert(text);
                return "Sent keys to alert.";
            } catch (Exception e) {
                return "Failed to send keys to alert: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Check whether a JavaScript alert/confirm/prompt dialog is currently open.")
    public String isAlertPresent() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            return String.valueOf(tools.isAlertPresent());
        }
    }
}
