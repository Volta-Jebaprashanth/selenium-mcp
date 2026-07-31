package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Captures screenshots of the full page or a single element on a single
 * {@link WebDriver} instance.
 */
public class ScreenshotTaker {

    private final WebDriver driver;
    private final Locators locators;

    public ScreenshotTaker(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    public String captureBase64() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
    }

    public void captureToFile(String filePath) throws IOException {
        byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Files.write(Path.of(filePath), bytes);
    }

    public String captureElementBase64(By locator) {
        WebElement element = driver.findElement(locator);
        return ((TakesScreenshot) element).getScreenshotAs(OutputType.BASE64);
    }

    public String captureElementBase64(String locatorType, String locatorValue) {
        return captureElementBase64(locators.toBy(locatorType, locatorValue));
    }

    public void captureElementToFile(By locator, String filePath) throws IOException {
        WebElement element = driver.findElement(locator);
        byte[] bytes = ((TakesScreenshot) element).getScreenshotAs(OutputType.BYTES);
        Files.write(Path.of(filePath), bytes);
    }

    public void captureElementToFile(String locatorType, String locatorValue, String filePath) throws IOException {
        captureElementToFile(locators.toBy(locatorType, locatorValue), filePath);
    }
}
