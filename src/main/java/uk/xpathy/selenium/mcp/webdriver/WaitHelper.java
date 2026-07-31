package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Explicit waits against a single {@link WebDriver} instance, built fresh per call
 * so each wait can use its own timeout.
 */
public class WaitHelper {

    private final WebDriver driver;
    private final Locators locators;

    public WaitHelper(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public void waitForVisible(By locator, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void waitForVisible(String locatorType, String locatorValue, int timeoutSeconds) {
        waitForVisible(locators.toBy(locatorType, locatorValue), timeoutSeconds);
    }

    public void waitForClickable(By locator, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void waitForClickable(String locatorType, String locatorValue, int timeoutSeconds) {
        waitForClickable(locators.toBy(locatorType, locatorValue), timeoutSeconds);
    }

    public void waitForPresent(By locator, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public void waitForPresent(String locatorType, String locatorValue, int timeoutSeconds) {
        waitForPresent(locators.toBy(locatorType, locatorValue), timeoutSeconds);
    }

    public void waitForInvisible(By locator, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public void waitForInvisible(String locatorType, String locatorValue, int timeoutSeconds) {
        waitForInvisible(locators.toBy(locatorType, locatorValue), timeoutSeconds);
    }

    public void waitForTextPresent(By locator, String text, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public void waitForTextPresent(String locatorType, String locatorValue, String text, int timeoutSeconds) {
        waitForTextPresent(locators.toBy(locatorType, locatorValue), text, timeoutSeconds);
    }

    public void waitForTitleContains(String text, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.titleContains(text));
    }

    public void waitForUrlContains(String text, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.urlContains(text));
    }

    private WebDriverWait wait(int timeoutSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }
}
