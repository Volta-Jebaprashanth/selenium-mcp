package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Performs element-level interactions against a single {@link WebDriver} instance.
 * {@link By} overloads are canonical; locator-string overloads resolve through
 * {@link Locators}.
 */
public class ElementInteractor {

    private static final long RETRY_INTERVAL_MILLIS = 250;

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

    /**
     * Clicks, retrying while the click is intercepted by another element on top - typically a loading
     * overlay that hasn't gone yet - for up to {@code waitWhileCovered}. Any other failure is thrown at once.
     *
     * @throws ElementClickInterceptedException if the element is still covered when the time is up
     */
    public void click(By locator, Duration waitWhileCovered) {
        long deadline = System.nanoTime() + waitWhileCovered.toNanos();
        while (true) {
            try {
                driver.findElement(locator).click();
                return;
            } catch (ElementClickInterceptedException e) {
                if (System.nanoTime() >= deadline) {
                    throw e;
                }
                try {
                    Thread.sleep(RETRY_INTERVAL_MILLIS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
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
