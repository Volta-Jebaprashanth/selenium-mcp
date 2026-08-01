package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Extracts focused, token-efficient views of the current page (scripts, styles, and a
 * compact element tree) by running JavaScript against the live DOM, rather than parsing
 * the raw HTML string returned by {@link Navigator#getPageSource()}. Each method returns
 * a JSON string built in-browser via {@code JSON.stringify}.
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
            var root = arguments[0] || document.body;
            var maxDepth = arguments[1] || 20;
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

            return JSON.stringify(serialize(root, 0));
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
        return (String) js().executeScript(ELEMENTS_JS, null, maxDepth);
    }

    public String getElements(By locator, int maxDepth) {
        WebElement root = driver.findElement(locator);
        return (String) js().executeScript(ELEMENTS_JS, root, maxDepth);
    }

    public String getElements(String locatorType, String locatorValue, int maxDepth) {
        return getElements(locators.toBy(locatorType, locatorValue), maxDepth);
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }
}
