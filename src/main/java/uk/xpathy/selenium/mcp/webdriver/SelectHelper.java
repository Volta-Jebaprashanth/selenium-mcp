package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Operates {@code <select>} dropdowns on a single {@link WebDriver} instance via
 * Selenium's {@link Select} support class.
 */
public class SelectHelper {

    private final WebDriver driver;
    private final Locators locators;

    public SelectHelper(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public void selectByVisibleText(By locator, String text) {
        select(locator).selectByVisibleText(text);
    }

    public void selectByVisibleText(String locatorType, String locatorValue, String text) {
        selectByVisibleText(locators.toBy(locatorType, locatorValue), text);
    }

    public void selectByValue(By locator, String value) {
        select(locator).selectByValue(value);
    }

    public void selectByValue(String locatorType, String locatorValue, String value) {
        selectByValue(locators.toBy(locatorType, locatorValue), value);
    }

    public void selectByIndex(By locator, int index) {
        select(locator).selectByIndex(index);
    }

    public void selectByIndex(String locatorType, String locatorValue, int index) {
        selectByIndex(locators.toBy(locatorType, locatorValue), index);
    }

    public void deselectAll(By locator) {
        select(locator).deselectAll();
    }

    public void deselectAll(String locatorType, String locatorValue) {
        deselectAll(locators.toBy(locatorType, locatorValue));
    }

    public List<String> getSelectedOptionsText(By locator) {
        return select(locator).getAllSelectedOptions().stream()
                .map(org.openqa.selenium.WebElement::getText)
                .collect(Collectors.toList());
    }

    public List<String> getSelectedOptionsText(String locatorType, String locatorValue) {
        return getSelectedOptionsText(locators.toBy(locatorType, locatorValue));
    }

    public boolean isMultiple(By locator) {
        return select(locator).isMultiple();
    }

    public boolean isMultiple(String locatorType, String locatorValue) {
        return isMultiple(locators.toBy(locatorType, locatorValue));
    }

    private Select select(By locator) {
        return new Select(driver.findElement(locator));
    }
}
