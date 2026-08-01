package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Extracts focused, token-efficient views of the current page (scripts, styles, and
 * element trees) by running JavaScript against the live DOM, rather than parsing the raw
 * HTML string returned by {@link Navigator#getPageSource()}. Each method returns a JSON
 * string built in-browser via {@code JSON.stringify}.
 */
public class PageSourceInspector {

    private static final String SCRIPTS_JS = """
            var scripts = document.getElementsByTagName('script');
            var maxLen = 10000;
            var result = [];
            for (var i = 0; i < scripts.length; i++) {
                var s = scripts[i];
                var entry = {};
                if (s.src) {
                    entry.type = 'external';
                    entry.src = s.src;
                } else {
                    var content = s.textContent || '';
                    entry.type = 'inline';
                    if (content.length > maxLen) {
                        entry.content = content.slice(0, maxLen);
                        entry.truncated = true;
                    } else {
                        entry.content = content;
                    }
                }
                if (s.type) entry.scriptType = s.type;
                if (s.id) entry.id = s.id;
                if (s.async) entry.async = true;
                if (s.defer) entry.defer = true;
                result.push(entry);
            }
            return JSON.stringify(result);
            """;

    private static final String STYLES_JS = """
            var maxLen = 10000;
            var result = [];
            var links = document.querySelectorAll('link[rel="stylesheet"]');
            for (var i = 0; i < links.length; i++) {
                var l = links[i];
                var entry = {type: 'external', href: l.href};
                if (l.media) entry.media = l.media;
                result.push(entry);
            }
            var styleTags = document.getElementsByTagName('style');
            for (var i = 0; i < styleTags.length; i++) {
                var st = styleTags[i];
                var content = st.textContent || '';
                var entry = {type: 'inline'};
                if (st.id) entry.id = st.id;
                if (st.media) entry.media = st.media;
                if (content.length > maxLen) {
                    entry.content = content.slice(0, maxLen);
                    entry.truncated = true;
                } else {
                    entry.content = content;
                }
                result.push(entry);
            }
            return JSON.stringify(result);
            """;

    private static final String ELEMENTS_JS = """
            var maxDepth = arguments[0] || 20;
            var SKIP_TAGS = {SCRIPT: 1, STYLE: 1, NOSCRIPT: 1, TEMPLATE: 1};
            var NOTABLE_ATTRS = ['id', 'class', 'name', 'type', 'href', 'src', 'value', 'placeholder',
                'role', 'alt', 'title', 'for', 'target', 'data-testid', 'aria-label', 'aria-hidden',
                'disabled', 'checked', 'selected', 'readonly'];

            function truncate(s, n) {
                if (!s) return '';
                s = s.replace(/\\s+/g, ' ').trim();
                return s.length > n ? s.slice(0, n) + '...' : s;
            }

            function directText(el) {
                var text = '';
                for (var i = 0; i < el.childNodes.length; i++) {
                    var child = el.childNodes[i];
                    if (child.nodeType === 3) text += child.textContent;
                }
                return truncate(text, 120);
            }

            function serialize(el, depth) {
                if (!el || !el.tagName || SKIP_TAGS[el.tagName]) return null;
                var node = {tag: el.tagName.toLowerCase()};
                for (var i = 0; i < NOTABLE_ATTRS.length; i++) {
                    var attr = NOTABLE_ATTRS[i];
                    var v = el.getAttribute(attr);
                    if (v !== null && v !== '') node[attr] = v;
                }
                var text = directText(el);
                if (text) node.text = text;
                var childEls = el.children;
                if (depth >= maxDepth) {
                    if (childEls.length) node.childrenTruncated = childEls.length;
                    return node;
                }
                var children = [];
                for (var i = 0; i < childEls.length; i++) {
                    var s = serialize(childEls[i], depth + 1);
                    if (s) children.push(s);
                }
                if (children.length) node.children = children;
                return node;
            }

            return JSON.stringify(serialize(document.body, 0));
            """;

    private static final String FILTERED_ELEMENTS_JS = """
            var matches = arguments[0];
            var includeAncestors = arguments[1];
            var includeDescendants = arguments[2];
            var includeSiblings = arguments[3];
            var maxDepth = arguments[4] || 20;
            var totalMatches = arguments[5];
            var truncated = arguments[6];

            var SKIP_TAGS = {SCRIPT: 1, STYLE: 1, NOSCRIPT: 1, TEMPLATE: 1};
            var NOTABLE_ATTRS = ['id', 'class', 'name', 'type', 'href', 'src', 'value', 'placeholder',
                'role', 'alt', 'title', 'for', 'target', 'data-testid', 'aria-label', 'aria-hidden',
                'disabled', 'checked', 'selected', 'readonly'];

            function truncate(s, n) {
                if (!s) return '';
                s = s.replace(/\\s+/g, ' ').trim();
                return s.length > n ? s.slice(0, n) + '...' : s;
            }

            function directText(el) {
                var text = '';
                for (var i = 0; i < el.childNodes.length; i++) {
                    var child = el.childNodes[i];
                    if (child.nodeType === 3) text += child.textContent;
                }
                return truncate(text, 120);
            }

            function describe(el) {
                var node = {tag: el.tagName.toLowerCase()};
                for (var i = 0; i < NOTABLE_ATTRS.length; i++) {
                    var attr = NOTABLE_ATTRS[i];
                    var v = el.getAttribute(attr);
                    if (v !== null && v !== '') node[attr] = v;
                }
                var text = directText(el);
                if (text) node.text = text;
                var parent = el.parentElement;
                if (parent) {
                    var sameTag = [];
                    for (var j = 0; j < parent.children.length; j++) {
                        if (parent.children[j].tagName === el.tagName) sameTag.push(parent.children[j]);
                    }
                    var idx = sameTag.indexOf(el);
                    if (idx >= 0) {
                        node.siblingIndex = idx + 1;
                        node.siblingCount = sameTag.length;
                    }
                }
                return node;
            }

            function serializeDescendants(el, depth) {
                if (!el || !el.tagName || SKIP_TAGS[el.tagName]) return null;
                var node = describe(el);
                delete node.siblingIndex;
                delete node.siblingCount;
                var childEls = el.children;
                if (depth >= maxDepth) {
                    if (childEls.length) node.childrenTruncated = childEls.length;
                    return node;
                }
                var children = [];
                for (var i = 0; i < childEls.length; i++) {
                    var s = serializeDescendants(childEls[i], depth + 1);
                    if (s) children.push(s);
                }
                if (children.length) node.children = children;
                return node;
            }

            function buildAncestors(el) {
                var chain = [];
                var current = el.parentElement;
                while (current && current.tagName && !SKIP_TAGS[current.tagName]) {
                    chain.push(describe(current));
                    current = current.parentElement;
                }
                return chain;
            }

            function buildSiblings(el) {
                var parent = el.parentElement;
                if (!parent) return [];
                var result = [];
                for (var i = 0; i < parent.children.length; i++) {
                    var sib = parent.children[i];
                    if (!sib.tagName || SKIP_TAGS[sib.tagName]) continue;
                    var d = describe(sib);
                    d.self = (sib === el);
                    result.push(d);
                }
                return result;
            }

            var results = [];
            for (var i = 0; i < matches.length; i++) {
                var el = matches[i];
                var entry = describe(el);
                if (includeAncestors) entry.ancestors = buildAncestors(el);
                if (includeSiblings) entry.siblings = buildSiblings(el);
                if (includeDescendants) {
                    var children = [];
                    for (var c = 0; c < el.children.length; c++) {
                        var s = serializeDescendants(el.children[c], 1);
                        if (s) children.push(s);
                    }
                    if (children.length) entry.descendants = children;
                }
                results.push(entry);
            }

            return JSON.stringify({
                totalMatches: totalMatches,
                returnedMatches: results.length,
                truncated: truncated,
                matches: results
            });
            """;

    private final WebDriver driver;
    private final Locators locators;

    public PageSourceInspector(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public String getScripts() {
        return (String) js().executeScript(SCRIPTS_JS);
    }

    public String getStyles() {
        return (String) js().executeScript(STYLES_JS);
    }

    public String getElements(int maxDepth) {
        return (String) js().executeScript(ELEMENTS_JS, maxDepth);
    }

    public String getFilteredElements(By locator, boolean includeAncestors, boolean includeDescendants,
                                       boolean includeSiblings, int maxDepth, int limit) {
        List<WebElement> matches = driver.findElements(locator);
        int total = matches.size();
        boolean truncated = total > limit;
        List<WebElement> limited = truncated ? matches.subList(0, limit) : matches;
        return (String) js().executeScript(FILTERED_ELEMENTS_JS,
                limited, includeAncestors, includeDescendants, includeSiblings, maxDepth, total, truncated);
    }

    public String getFilteredElements(String locatorType, String locatorValue, boolean includeAncestors,
                                       boolean includeDescendants, boolean includeSiblings, int maxDepth, int limit) {
        return getFilteredElements(locators.toBy(locatorType, locatorValue),
                includeAncestors, includeDescendants, includeSiblings, maxDepth, limit);
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }
}
