package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;

/**
 * Resolves a locator strategy name and value into a Selenium {@link By}.
 * <p>
 * Supported strategies: {@code id}, {@code name}, {@code css}/{@code cssSelector},
 * {@code xpath}, {@code className}/{@code class}, {@code linkText},
 * {@code partialLinkText}, {@code tagName}/{@code tag}. Strategy names are
 * case-insensitive.
 */
public class Locators {

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
            default -> throw new IllegalArgumentException("Unsupported locator type: " + locatorType);
        };
    }
}
