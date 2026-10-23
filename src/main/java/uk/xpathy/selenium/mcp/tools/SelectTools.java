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
@ToolGroup("select")
public class SelectTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)";

    private final BrowserSessions sessions;

    public SelectTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Select an option from a dropdown by its visible text.")
    public String selectByVisibleText(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = "The visible text of the option to select", required = true) String text,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.selectByVisibleText(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), text);
                return "Selected option with text \"" + text + "\"";
            } catch (Exception e) {
                return sessions.label() + "Failed to select option: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Select an option from a dropdown by its value attribute.")
    public String selectByValue(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = "The value attribute of the option to select", required = true) String optionValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.selectByValue(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), optionValue);
                return "Selected option with value \"" + optionValue + "\"";
            } catch (Exception e) {
                return sessions.label() + "Failed to select option: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Select an option from a dropdown by its zero-based index.")
    public String selectByIndex(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = "Zero-based index of the option to select", required = true) Integer index,
            @McpToolParam(description = "Zero-based index of the <select> element to use when the locator matches several. Defaults to the first match.", required = false) Integer elementIndex,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.selectByIndex(tools.locator(locatorType, locatorValue, elementIndex, Boolean.TRUE.equals(visibleOnly)), index);
                return "Selected option at index " + index;
            } catch (Exception e) {
                return sessions.label() + "Failed to select option: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Deselect all selected options in a multi-select dropdown.")
    public String deselectAllOptions(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.deselectAllOptions(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return "Deselected all options.";
            } catch (Exception e) {
                return sessions.label() + "Failed to deselect options: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the visible text of the currently selected option(s) in a dropdown.")
    public String getSelectedOptionsText(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                List<String> texts = tools.getSelectedOptionsText(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return String.join(", ", texts);
            } catch (Exception e) {
                return sessions.label() + "Failed to get selected options: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Check whether a dropdown allows selecting multiple options.")
    public String isMultipleSelect(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the <select> element", required = true) String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return String.valueOf(tools.isMultipleSelect(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly))));
            } catch (Exception e) {
                return sessions.label() + "Failed to check multiple-select state: " + ErrorMessages.of(e);
            }
        }
    }
}
