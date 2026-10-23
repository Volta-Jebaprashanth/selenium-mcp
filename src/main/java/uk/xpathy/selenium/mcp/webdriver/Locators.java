package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Resolves a locator strategy name and value into a Selenium {@link By}.
 * <p>
 * Supported strategies: {@code id}, {@code name}, {@code css}/{@code cssSelector},
 * {@code xpath}, {@code className}/{@code class}, {@code linkText},
 * {@code partialLinkText}, {@code tagName}/{@code tag}, {@code text} and
 * {@code partialText}. Strategy names are case-insensitive.
 * <p>
 * {@code text}/{@code partialText} match any element by its visible text, so an agent can say
 * "the element reading Create Put" instead of writing XPath. See {@link #byText(String, boolean)}.
 */
public class Locators {

    /**
     * Elements a text match is promoted to when the text sits in a child of one, so that
     * {@code <button><span>Save</span></button>} resolves to the button: that's what gets clicked,
     * and what {@code isEnabled} must check.
     */
    private static final String INTERACTIVE = "self::button or self::a or self::option or self::summary or self::label"
            + " or @role='button' or @role='link' or @role='menuitem' or @role='tab' or @role='option'"
            + " or @role='checkbox' or @role='radio' or @role='switch'";

    public By toBy(String locatorType, String locatorValue) {
        String type = locatorType.trim().toLowerCase();
        return switch (type) {
            case "id" -> By.id(locatorValue);
            case "name" -> By.name(locatorValue);
            case "css", "cssselector" -> By.cssSelector(locatorValue);
            case "xpath" -> By.xpath(locatorValue);
            case "classname", "class" -> By.className(locatorValue);
            case "linktext" -> By.linkText(locatorValue);
            case "partiallinktext" -> By.partialLinkText(locatorValue);
            case "tagname", "tag" -> By.tagName(locatorValue);
            case "text" -> byText(locatorValue, false);
            case "partialtext" -> byText(locatorValue, true);
            default -> throw new IllegalArgumentException("Unsupported locator type: " + locatorType);
        };
    }

    /**
     * Like {@link #toBy(String, String)}, but optionally narrowed to visible matches and/or the
     * match at a zero-based {@code index}. Returns the plain locator when neither is requested.
     */
    public By toBy(String locatorType, String locatorValue, Integer index, boolean visibleOnly) {
        By base = toBy(locatorType, locatorValue);
        return index == null && !visibleOnly ? base : new FilteredBy(base, index, visibleOnly);
    }

    /**
     * Matches elements by their visible text, whitespace-normalized (and {@code &nbsp;} treated as a
     * space), either exactly or as a substring. Only the innermost element holding the text matches,
     * not every ancestor that contains it - promoted to its nearest button/link-like ancestor if it
     * has one. Button-type {@code <input>}s match on their {@code value}.
     */
    public By byText(String text, boolean partial) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to match must not be blank.");
        }
        String literal = xpathLiteral(text.strip().replaceAll("\\s+", " "));
        String norm = "normalize-space(translate(%s, ' ', ' '))";
        String test = partial ? "contains(" + norm + ", " + literal + ")" : norm + " = " + literal;
        String self = test.formatted(".");
        String inner = "//body//*[not(self::script or self::style or self::noscript or self::template)]"
                + "[" + self + "][not(./*[" + self + "])]";
        String xpath = inner + "/ancestor-or-self::*[" + INTERACTIVE + "][1]"
                + " | " + inner + "[not(ancestor-or-self::*[" + INTERACTIVE + "])]"
                + " | //input[@type='submit' or @type='button' or @type='reset'][" + test.formatted("@value") + "]";
        return new TextBy(By.xpath(xpath), (partial ? "partialText: " : "text: ") + text);
    }

    /**
     * Quotes {@code value} as an XPath 1.0 string literal, which has no escape syntax: values holding
     * both quote kinds are built with {@code concat()}.
     */
    static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return "concat('" + value.replace("'", "', \"'\", '") + "')";
    }

    /**
     * An XPath-backed {@link By} that reports itself as the text it looks for, so "no such element"
     * errors read {@code By.text: Create Put} rather than the generated XPath.
     */
    private static final class TextBy extends By {

        private final By xpath;
        private final String description;

        TextBy(By xpath, String description) {
            this.xpath = xpath;
            this.description = description;
        }

        @Override
        public List<WebElement> findElements(SearchContext context) {
            return xpath.findElements(context);
        }

        @Override
        public WebElement findElement(SearchContext context) {
            List<WebElement> matches = findElements(context);
            if (matches.isEmpty()) {
                throw new NoSuchElementException("Cannot locate an element using " + this);
            }
            return matches.getFirst();
        }

        @Override
        public String toString() {
            return "By." + description;
        }
    }
}
