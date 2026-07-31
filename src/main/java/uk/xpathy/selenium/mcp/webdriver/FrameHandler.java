package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Switches the driver's focus between frames/iframes on a single {@link WebDriver} instance.
 */
public class FrameHandler {

    private final WebDriver driver;
    private final Locators locators;

    public FrameHandler(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public void switchToFrame(int index) {
        driver.switchTo().frame(index);
    }

    public void switchToFrame(String nameOrId) {
        driver.switchTo().frame(nameOrId);
    }

    public void switchToFrame(By locator) {
        driver.switchTo().frame(driver.findElement(locator));
    }

    public void switchToFrame(String locatorType, String locatorValue) {
        switchToFrame(locators.toBy(locatorType, locatorValue));
    }

    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    public void switchToParentFrame() {
        driver.switchTo().parentFrame();
    }
}
