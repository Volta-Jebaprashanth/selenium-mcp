package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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

    /**
     * Waits until {@code document.readyState} is {@code "complete"}.
     */
    public void waitForPageLoad(int timeoutSeconds) {
        wait(timeoutSeconds).until(d -> "complete".equals(
                ((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    /**
     * Waits until the given JavaScript expression evaluates truthy. The script is evaluated as
     * the body of a function, so use {@code return}, e.g. {@code "return document.title === 'x'"}
     * or a bare expression such as {@code "!!document.querySelector('.ready')"}.
     */
    public void waitForJsCondition(String script, int timeoutSeconds) {
        String body = script.trim().startsWith("return") ? script : "return (" + script + ");";
        wait(timeoutSeconds).until(d -> {
            Object result = ((JavascriptExecutor) d).executeScript(body);
            return result instanceof Boolean b && b;
        });
    }

    /**
     * Waits until the number of elements matching the locator equals {@code expectedCount}.
     */
    public void waitForElementCount(By locator, int expectedCount, int timeoutSeconds) {
        wait(timeoutSeconds).until(d -> d.findElements(locator).size() == expectedCount);
    }

    public void waitForElementCount(String locatorType, String locatorValue, int expectedCount, int timeoutSeconds) {
        waitForElementCount(locators.toBy(locatorType, locatorValue), expectedCount, timeoutSeconds);
    }

    /**
     * Waits until the given attribute/DOM property of an element equals {@code expectedValue}.
     */
    public void waitForAttributeToBe(By locator, String attribute, String expectedValue, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.attributeToBe(locator, attribute, expectedValue));
    }

    public void waitForAttributeToBe(String locatorType, String locatorValue, String attribute,
                                      String expectedValue, int timeoutSeconds) {
        waitForAttributeToBe(locators.toBy(locatorType, locatorValue), attribute, expectedValue, timeoutSeconds);
    }

    /**
     * Waits until the number of open windows/tabs equals {@code expectedCount}, e.g. after an
     * action that is expected to open or close a tab.
     */
    public void waitForNumberOfWindowsToBe(int expectedCount, int timeoutSeconds) {
        wait(timeoutSeconds).until(ExpectedConditions.numberOfWindowsToBe(expectedCount));
    }

    private WebDriverWait wait(int timeoutSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }
}
