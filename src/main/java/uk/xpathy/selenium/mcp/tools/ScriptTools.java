package uk.xpathy.selenium.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.WebElement;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP-facing wrapper around {@link Tools} for JavaScript execution, init scripts that persist
 * across page loads, and scrolling.
 */
@Component
@ToolGroup("script")
public class ScriptTools {

    private final BrowserSessions sessions;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScriptTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Execute JavaScript in the current page as the body of a function and return its result. "
            + "Use 'return' to produce a result; a returned Promise is awaited, so 'return (async () => {...})()' works. "
            + "Objects and arrays come back as JSON. Each call is a fresh function scope: for helpers that should "
            + "survive reloads, use addInitScript.")
    public String executeScript(
            @McpToolParam(description = "JavaScript source to execute. Use 'return' to produce a result.", required = true)
            String script,
            @McpToolParam(description = "Script timeout in milliseconds, for long-running or awaited scripts. "
                    + "Defaults to the session's setting (30000 unless changed).", required = false)
            Integer timeoutMs) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            if (timeoutMs != null && timeoutMs <= 0) return "timeoutMs must be greater than 0.";
            try {
                Object result = tools.executeScript(script, timeoutMs == null ? null : Duration.ofMillis(timeoutMs));
                return format(result);
            } catch (Exception e) {
                return sessions.label() + "Script error: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Register JavaScript that runs at the start of every page load in the current tab, before "
            + "the page's own scripts (e.g. to define helpers on window or hook console.error). Survives navigations "
            + "and reloads. Returns an id for removeInitScript. Requires Chrome or Edge.")
    public String addInitScript(
            @McpToolParam(description = "JavaScript source to run on each new document.", required = true)
            String script,
            @McpToolParam(description = "Also run it on the current page immediately. Defaults to true.", required = false)
            Boolean runNow) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                String id = tools.addInitScript(script, !Boolean.FALSE.equals(runNow));
                return "Init script registered with id " + id + ".";
            } catch (UnsupportedOperationException e) {
                return sessions.label() + ErrorMessages.of(e);
            } catch (Exception e) {
                return sessions.label() + "Failed to register init script: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Remove init scripts registered with addInitScript: one by id, or all when id is omitted. "
            + "Already-loaded pages are unaffected until they reload.")
    public String removeInitScript(
            @McpToolParam(description = "Id returned by addInitScript. Omit to remove all.", required = false)
            String id) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                if (id == null || id.isBlank()) {
                    int count = tools.getInitScripts().size();
                    tools.clearInitScripts();
                    return "Removed " + count + " init script(s).";
                }
                tools.removeInitScript(id.strip());
                return "Removed init script " + id.strip() + ".";
            } catch (UnsupportedOperationException | IllegalArgumentException e) {
                return sessions.label() + ErrorMessages.of(e);
            } catch (Exception e) {
                return sessions.label() + "Failed to remove init script: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Scroll the page so that an element is in view, located by a strategy and value.")
    public String scrollIntoView(
            @McpToolParam(description = "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)", required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true)
            String locatorValue,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false)
            Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false)
            Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.scrollIntoView(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)));
                return "Scrolled element into view.";
            } catch (Exception e) {
                return sessions.label() + "Failed to scroll into view: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Scroll the page by the given horizontal and vertical pixel offsets.")
    public String scrollBy(
            @McpToolParam(description = "Horizontal pixels to scroll", required = true) Integer x,
            @McpToolParam(description = "Vertical pixels to scroll", required = true) Integer y) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.scrollBy(x, y);
                return "Scrolled by (" + x + ", " + y + ")";
            } catch (Exception e) {
                return sessions.label() + "Failed to scroll: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Scroll to the top of the page.")
    public String scrollToTop() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.scrollToTop();
                return "Scrolled to top.";
            } catch (Exception e) {
                return sessions.label() + "Failed to scroll to top: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Scroll to the bottom of the page.")
    public String scrollToBottom() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.scrollToBottom();
                return "Scrolled to bottom.";
            } catch (Exception e) {
                return sessions.label() + "Failed to scroll to bottom: " + ErrorMessages.of(e);
            }
        }
    }

    private String format(Object result) {
        if (result == null) {
            return "(null)";
        }
        if (result instanceof Map<?, ?> || result instanceof Collection<?>) {
            try {
                return objectMapper.writeValueAsString(jsonSafe(result));
            } catch (Exception e) {
                return result.toString();
            }
        }
        return result.toString();
    }

    // Returned DOM elements become WebElements; replace them with their description so Jackson
    // doesn't walk their getters (each of which is a round trip to the browser).
    private Object jsonSafe(Object value) {
        if (value instanceof WebElement element) {
            return element.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), jsonSafe(v)));
            return copy;
        }
        if (value instanceof Collection<?> items) {
            return items.stream().map(this::jsonSafe).toList();
        }
        return value;
    }
}
