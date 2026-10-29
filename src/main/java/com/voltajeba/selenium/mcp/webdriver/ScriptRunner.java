package com.voltajeba.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Executes arbitrary JavaScript and performs scroll operations on a single
 * {@link WebDriver} instance.
 */
public class ScriptRunner {

    private final WebDriver driver;
    private final Locators locators;

    public ScriptRunner(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    /**
     * Runs {@code script} with the session's script timeout temporarily set to {@code timeout},
     * restoring the previous value afterwards. A returned Promise is awaited by the driver, so
     * this is what bounds long-running {@code await} chains.
     */
    public Object executeScript(String script, Duration timeout, Object... args) {
        if (timeout == null) {
            return executeScript(script, args);
        }
        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        Duration previous = timeouts.getScriptTimeout();
        timeouts.scriptTimeout(timeout);
        try {
            return executeScript(script, args);
        } finally {
            timeouts.scriptTimeout(previous);
        }
    }

    public void scrollIntoView(By locator) {
        WebElement element = driver.findElement(locator);
        executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    public void scrollIntoView(String locatorType, String locatorValue) {
        scrollIntoView(locators.toBy(locatorType, locatorValue));
    }

    public void scrollBy(int x, int y) {
        executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);
    }

    public void scrollToTop() {
        executeScript("window.scrollTo(0, 0);");
    }

    public void scrollToBottom() {
        executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }
}
