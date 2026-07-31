package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import java.util.List;

/**
 * MCP-facing wrapper around {@link Tools} for {@code <select>} dropdown elements.
 */
@Component
public class SelectTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName";

    private final Tools tools;

    public SelectTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Select an option from a dropdown by its visible text.")
    public String selectByVisibleText(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = "The visible text of the option to select", required = true) String text) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.selectByVisibleText(locatorType, locatorValue, text);
                return "Selected option with text \"" + text + "\"";
            } catch (Exception e) {
                return "Failed to select option: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Select an option from a dropdown by its value attribute.")
    public String selectByValue(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = "The value attribute of the option to select", required = true) String optionValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.selectByValue(locatorType, locatorValue, optionValue);
                return "Selected option with value \"" + optionValue + "\"";
            } catch (Exception e) {
                return "Failed to select option: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Select an option from a dropdown by its zero-based index.")
    public String selectByIndex(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = "Zero-based index of the option to select", required = true) Integer index) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.selectByIndex(locatorType, locatorValue, index);
                return "Selected option at index " + index;
            } catch (Exception e) {
                return "Failed to select option: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Deselect all selected options in a multi-select dropdown.")
    public String deselectAllOptions(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.deselectAllOptions(locatorType, locatorValue);
                return "Deselected all options.";
            } catch (Exception e) {
                return "Failed to deselect options: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the visible text of the currently selected option(s) in a dropdown.")
    public String getSelectedOptionsText(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                List<String> texts = tools.getSelectedOptionsText(locatorType, locatorValue);
                return String.join(", ", texts);
            } catch (Exception e) {
                return "Failed to get selected options: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Check whether a dropdown allows selecting multiple options.")
    public String isMultipleSelect(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.isMultipleSelect(locatorType, locatorValue));
            } catch (Exception e) {
                return "Failed to check multiple-select state: " + e.getMessage();
            }
        }
    }
}
