package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
