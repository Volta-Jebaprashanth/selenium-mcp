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
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, or tagName.";
    private static final String NOTABLE_ATTRS_DESC =
            "id, class, name, type, href, src, value, placeholder, role, alt, title, for, target, "
                    + "data-testid, aria-label, aria-hidden, disabled, checked, selected, readonly";
    private static final int DEFAULT_MAX_DEPTH = 20;
    private static final int DEFAULT_LIMIT = 50;

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

    @McpTool(description = "Get the ENTIRE page as a compact JSON element tree rooted at <body>, without scripts, "
            + "styles, or comments - far cheaper to read than the full HTML source. Each node has tag, any notable "
            + "attributes present (" + NOTABLE_ATTRS_DESC + "), text (its own direct text, trimmed and truncated to "
            + "120 characters), and children. There is no locator filtering here - it always returns the whole page. "
            + "If you already know roughly what you're looking for (e.g. all inputs, or one specific element among "
            + "several), use getPageElementsFiltered instead - it targets matches directly and is much cheaper.")
    public String getPageElements(
            @McpToolParam(description = "Max tree depth to descend from <body>. Deeper subtrees are reported as "
                    + "childrenTruncated: <count> instead of being expanded. Defaults to 20.", required = false)
            Integer maxDepth) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getPageElements(maxDepth == null ? DEFAULT_MAX_DEPTH : maxDepth);
            } catch (Exception e) {
                return "Failed to get page elements: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Find every element matching a locator (not just the first) and describe each one as "
            + "JSON, optionally with surrounding context - the tool to reach for when you want a specific kind of "
            + "element (e.g. every <input>, every element with class 'card') rather than the whole page tree. "
            + "\n\nExample: locatorType=xpath, locatorValue=//input returns one entry per <input> on the page, each "
            + "with tag/id/class/name/type/etc. and its own direct text. "
            + "\n\nBy default only the matched elements themselves are returned. Turn on the extra context you need: "
            + "\n- includeAncestors=true adds each match's parent chain, immediate parent first up through <html> "
            + "last. Every node in the chain (and the match itself) gets siblingIndex/siblingCount, e.g. "
            + "siblingIndex:2, siblingCount:5 means it's the 2nd <div> among 5 sibling <div>s under that same "
            + "parent. Use this to build a locator that's actually unique, e.g. "
            + "\"(//form[@id='login']//input)[2]\" or \"div.container > form#login > input:nth-of-type(2)\", instead "
            + "of guessing and hitting 'multiple elements found'. "
            + "\n- includeSiblings=true adds the other children of the match's immediate parent, so you can see what "
            + "else is nearby (e.g. a <label> right before an <input>) without walking the whole ancestor chain. "
            + "\n- includeDescendants=true expands each match's own children (respecting maxDepth) - only useful "
            + "when the locator targets a container (e.g. a <form> or <div>) rather than a leaf element like "
            + "<input>. "
            + "\n\nResults are capped by limit (default 50); the response always reports totalMatches, "
            + "returnedMatches, and truncated so you know if more matches exist than were returned.")
    public String getPageElementsFiltered(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true)
            String locatorType,
            @McpToolParam(description = "The locator value to match elements against, e.g. //input for xpath or "
                    + "input for css.", required = true)
            String locatorValue,
            @McpToolParam(description = "Include each match's ancestor chain (immediate parent first, up to <html> "
                    + "last), with siblingIndex/siblingCount on every node to help build a unique locator. Defaults "
                    + "to false.", required = false)
            Boolean includeAncestors,
            @McpToolParam(description = "Include each match's own descendant elements (respecting maxDepth). Useful "
                    + "when the locator targets a container rather than a leaf element. Defaults to false.", required = false)
            Boolean includeDescendants,
            @McpToolParam(description = "Include the other children of each match's immediate parent, for nearby "
                    + "context (e.g. a neighboring <label>). Defaults to false.", required = false)
            Boolean includeSiblings,
            @McpToolParam(description = "Max depth to descend when includeDescendants is true. Ignored otherwise. "
                    + "Defaults to 20.", required = false)
            Integer maxDepth,
            @McpToolParam(description = "Max number of matches to return. Defaults to 50.", required = false)
            Integer limit) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getPageElementsFiltered(locatorType, locatorValue,
                        Boolean.TRUE.equals(includeAncestors), Boolean.TRUE.equals(includeDescendants),
                        Boolean.TRUE.equals(includeSiblings), maxDepth == null ? DEFAULT_MAX_DEPTH : maxDepth,
                        limit == null ? DEFAULT_LIMIT : limit);
            } catch (Exception e) {
                return "Failed to get filtered page elements: " + e.getMessage();
            }
        }
    }
}
