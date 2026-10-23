package uk.xpathy.selenium.mcp.webdriver;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocatorsTest {

    private final Locators locators = new Locators();

    @Test
    void everyStrategyAndAliasResolves() {
        assertEquals(By.id("q"), locators.toBy("id", "q"));
        assertEquals(By.name("q"), locators.toBy("name", "q"));
        assertEquals(By.cssSelector("a.b"), locators.toBy("css", "a.b"));
        assertEquals(By.cssSelector("a.b"), locators.toBy("cssSelector", "a.b"));
        assertEquals(By.xpath("//a"), locators.toBy("xpath", "//a"));
        assertEquals(By.className("btn"), locators.toBy("className", "btn"));
        assertEquals(By.className("btn"), locators.toBy("class", "btn"));
        assertEquals(By.linkText("Home"), locators.toBy("linkText", "Home"));
        assertEquals(By.partialLinkText("Ho"), locators.toBy("partialLinkText", "Ho"));
        assertEquals(By.tagName("a"), locators.toBy("tagName", "a"));
        assertEquals(By.tagName("a"), locators.toBy("tag", "a"));
    }

    @Test
    void strategyNamesIgnoreCaseAndSurroundingSpace() {
        assertEquals(By.cssSelector("a"), locators.toBy("  CSSSelector ", "a"));
        assertEquals(By.xpath("//a"), locators.toBy("XPath", "//a"));
    }

    @Test
    void anUnknownStrategyIsRejectedByName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> locators.toBy("label", "x"));

        assertEquals("Unsupported locator type: label", e.getMessage());
    }

    @Test
    void textStrategiesDescribeThemselvesByTheirText() {
        assertEquals("By.text: Create Put", locators.toBy("text", "Create Put").toString());
        assertEquals("By.partialText: Create", locators.toBy("partialText", "Create").toString());
    }

    @Test
    void blankTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> locators.toBy("text", "  "));
    }

    @Test
    void xpathLiteralsQuoteWhicheverWayTheValueAllows() {
        assertEquals("'Save'", Locators.xpathLiteral("Save"));
        assertEquals("\"Don't save\"", Locators.xpathLiteral("Don't save"));
        assertEquals("'Say \"hi\"'", Locators.xpathLiteral("Say \"hi\""));
        assertEquals("concat('It', \"'\", 's \"x\"')", Locators.xpathLiteral("It's \"x\""));
    }

    @Test
    void indexOrVisibleOnlyNarrowTheLocatorAndNeitherLeavesItPlain() {
        assertEquals(By.id("q"), locators.toBy("id", "q", null, false));
        assertInstanceOf(FilteredBy.class, locators.toBy("id", "q", 1, false));
        assertInstanceOf(FilteredBy.class, locators.toBy("id", "q", null, true));
    }
}
