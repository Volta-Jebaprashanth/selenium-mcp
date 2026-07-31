package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Performs element-level interactions against a single {@link WebDriver} instance.
 * {@link By} overloads are canonical; locator-string overloads resolve through
 * {@link Locators}.
 */
public class ElementInteractor {

    private final WebDriver driver;
    private final Locators locators;

    public ElementInteractor(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public void click(By locator) {
        driver.findElement(locator).click();
    }

    public void click(String locatorType, String locatorValue) {
        click(locators.toBy(locatorType, locatorValue));
    }

    public void sendKeys(By locator, String text) {
        sendKeys(locator, text, false);
    }

    public void sendKeys(By locator, String text, boolean clearFirst) {
        WebElement element = driver.findElement(locator);
        if (clearFirst) {
            element.clear();
        }
        element.sendKeys(text);
    }

    public void sendKeys(String locatorType, String locatorValue, String text) {
        sendKeys(locatorType, locatorValue, text, false);
    }

    public void sendKeys(String locatorType, String locatorValue, String text, boolean clearFirst) {
        sendKeys(locators.toBy(locatorType, locatorValue), text, clearFirst);
    }
}
