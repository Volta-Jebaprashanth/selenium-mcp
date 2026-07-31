package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;

/**
 * Handles JavaScript alert/confirm/prompt dialogs on a single {@link WebDriver} instance.
 */
public class AlertHandler {

    private final WebDriver driver;

    public AlertHandler(WebDriver driver) {
        this.driver = driver;
    }

    public void accept() {
        driver.switchTo().alert().accept();
    }

    public void dismiss() {
        driver.switchTo().alert().dismiss();
    }

    public String getText() {
        return driver.switchTo().alert().getText();
    }

    public void sendKeys(String text) {
        driver.switchTo().alert().sendKeys(text);
    }

    public boolean isPresent() {
        try {
            Alert alert = driver.switchTo().alert();
            return alert != null;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }
}
