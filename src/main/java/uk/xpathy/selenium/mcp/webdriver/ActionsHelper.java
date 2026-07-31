package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

/**
 * Mouse and keyboard gestures (hover, double-click, drag and drop, key presses) that
 * go beyond the simple click/sendKeys in {@link ElementInteractor}, built on Selenium's
 * {@link Actions} API against a single {@link WebDriver} instance.
 */
public class ActionsHelper {

    private final WebDriver driver;
    private final Locators locators;

    public ActionsHelper(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public void hover(By locator) {
        new Actions(driver).moveToElement(driver.findElement(locator)).perform();
    }

    public void hover(String locatorType, String locatorValue) {
        hover(locators.toBy(locatorType, locatorValue));
    }

    public void doubleClick(By locator) {
        new Actions(driver).doubleClick(driver.findElement(locator)).perform();
    }

    public void doubleClick(String locatorType, String locatorValue) {
        doubleClick(locators.toBy(locatorType, locatorValue));
    }

    public void rightClick(By locator) {
        new Actions(driver).contextClick(driver.findElement(locator)).perform();
    }

    public void rightClick(String locatorType, String locatorValue) {
        rightClick(locators.toBy(locatorType, locatorValue));
    }

    public void clickAndHold(By locator) {
        new Actions(driver).clickAndHold(driver.findElement(locator)).perform();
    }

    public void clickAndHold(String locatorType, String locatorValue) {
        clickAndHold(locators.toBy(locatorType, locatorValue));
    }

    public void release() {
        new Actions(driver).release().perform();
    }

    public void dragAndDrop(By source, By target) {
        new Actions(driver).dragAndDrop(driver.findElement(source), driver.findElement(target)).perform();
    }

    public void dragAndDrop(String sourceType, String sourceValue, String targetType, String targetValue) {
        dragAndDrop(locators.toBy(sourceType, sourceValue), locators.toBy(targetType, targetValue));
    }

    public void dragAndDropByOffset(By locator, int xOffset, int yOffset) {
        new Actions(driver).dragAndDropBy(driver.findElement(locator), xOffset, yOffset).perform();
    }

    public void dragAndDropByOffset(String locatorType, String locatorValue, int xOffset, int yOffset) {
        dragAndDropByOffset(locators.toBy(locatorType, locatorValue), xOffset, yOffset);
    }

    /**
     * Sends a single named key (e.g. {@code ENTER}, {@code TAB}, {@code ESCAPE}) to
     * whichever element currently has focus. Names match {@link Keys} constants.
     */
    public void pressKey(String keyName) {
        Keys key = Keys.valueOf(keyName.trim().toUpperCase());
        new Actions(driver).sendKeys(key).perform();
    }
}
