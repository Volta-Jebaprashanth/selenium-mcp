package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only queries against elements on the page. Complements {@link ElementInteractor},
 * which performs state-changing actions (click, sendKeys).
 */
public class ElementInspector {

    private final WebDriver driver;
    private final Locators locators;

    public ElementInspector(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public String getText(By locator) {
        return driver.findElement(locator).getText();
    }

    public String getText(String locatorType, String locatorValue) {
        return getText(locators.toBy(locatorType, locatorValue));
    }

    public String getAttribute(By locator, String attributeName) {
        return driver.findElement(locator).getAttribute(attributeName);
    }

    public String getAttribute(String locatorType, String locatorValue, String attributeName) {
        return getAttribute(locators.toBy(locatorType, locatorValue), attributeName);
    }

    public String getCssValue(By locator, String propertyName) {
        return driver.findElement(locator).getCssValue(propertyName);
    }

    public String getCssValue(String locatorType, String locatorValue, String propertyName) {
        return getCssValue(locators.toBy(locatorType, locatorValue), propertyName);
    }

    public String getTagName(By locator) {
        return driver.findElement(locator).getTagName();
    }

    public String getTagName(String locatorType, String locatorValue) {
        return getTagName(locators.toBy(locatorType, locatorValue));
    }

    public boolean isDisplayed(By locator) {
        return driver.findElement(locator).isDisplayed();
    }

    public boolean isDisplayed(String locatorType, String locatorValue) {
        return isDisplayed(locators.toBy(locatorType, locatorValue));
    }

    public boolean isEnabled(By locator) {
        return driver.findElement(locator).isEnabled();
    }

    public boolean isEnabled(String locatorType, String locatorValue) {
        return isEnabled(locators.toBy(locatorType, locatorValue));
    }

    public boolean isSelected(By locator) {
        return driver.findElement(locator).isSelected();
    }

    public boolean isSelected(String locatorType, String locatorValue) {
        return isSelected(locators.toBy(locatorType, locatorValue));
    }

    public org.openqa.selenium.Dimension getSize(By locator) {
        return driver.findElement(locator).getSize();
    }

    public org.openqa.selenium.Dimension getSize(String locatorType, String locatorValue) {
        return getSize(locators.toBy(locatorType, locatorValue));
    }

    public org.openqa.selenium.Point getLocation(By locator) {
        return driver.findElement(locator).getLocation();
    }

    public org.openqa.selenium.Point getLocation(String locatorType, String locatorValue) {
        return getLocation(locators.toBy(locatorType, locatorValue));
    }

    public boolean exists(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    public boolean exists(String locatorType, String locatorValue) {
        return exists(locators.toBy(locatorType, locatorValue));
    }

    public int count(By locator) {
        return driver.findElements(locator).size();
    }

    public int count(String locatorType, String locatorValue) {
        return count(locators.toBy(locatorType, locatorValue));
    }

    public List<String> getTexts(By locator) {
        return driver.findElements(locator).stream().map(WebElement::getText).collect(Collectors.toList());
    }

    public List<String> getTexts(String locatorType, String locatorValue) {
        return getTexts(locators.toBy(locatorType, locatorValue));
    }
}
