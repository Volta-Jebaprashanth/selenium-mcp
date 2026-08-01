package uk.xpathy.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * MCP-facing wrapper around {@link Tools} for focused, token-efficient views of the current
 * page, as alternatives to the full HTML returned by {@code BrowserTools#getPageSource()}.
 */
@Component
public class PageSourceTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName. "
                    + "Omit (together with locatorValue) to use the whole page.";
    private static final int DEFAULT_MAX_DEPTH = 20;

    private final Tools tools;

    public PageSourceTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Get every <script> on the current page as a JSON array, without the rest of the HTML. "
            + "Each entry has type (external or inline), plus src for external scripts or content for inline "
            + "scripts. Inline content over 10,000 characters is truncated with truncated: true.")
    public String getPageScripts() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getPageScripts();
            } catch (Exception e) {
                return "Failed to get page scripts: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get every stylesheet on the current page as a JSON array, without the rest of the HTML. "
            + "Each entry has type (external or inline), plus href for external stylesheets or content for inline "
            + "<style> tags. Inline content over 10,000 characters is truncated with truncated: true.")
    public String getPageStyles() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getPageStyles();
            } catch (Exception e) {
                return "Failed to get page styles: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the page's elements as a compact JSON tree, without scripts, styles, or comments - "
            + "far cheaper to read than the full HTML source. Each node has tag, any notable attributes present "
            + "(id, class, name, type, href, src, value, placeholder, role, alt, title, for, target, data-testid, "
            + "aria-label, aria-hidden, disabled, checked, selected, readonly), text (its own direct text, trimmed "
            + "and truncated to 120 characters), and children. Scope to a subtree with locatorType/locatorValue to "
            + "cut the size further on large pages.")
    public String getPageElements(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = false)
            String locatorType,
            @McpToolParam(description = "The locator value scoping the tree to a subtree root. Omit for the whole page.", required = false)
            String locatorValue,
            @McpToolParam(description = "Max tree depth to descend from the root. Deeper subtrees are reported as "
                    + "childrenTruncated: <count> instead of being expanded. Defaults to 20.", required = false)
            Integer maxDepth) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                int depth = maxDepth == null ? DEFAULT_MAX_DEPTH : maxDepth;
                if (locatorType == null || locatorType.isBlank()) {
                    return tools.getPageElements(depth);
                }
                return tools.getPageElements(locatorType, locatorValue, depth);
            } catch (Exception e) {
                return "Failed to get page elements: " + e.getMessage();
            }
        }
    }
}
