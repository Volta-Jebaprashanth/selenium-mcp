package com.voltajeba.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.firefox.HasFullPageScreenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

/**
 * Captures screenshots of the viewport, the full scrollable page, or a single element on a
 * single {@link WebDriver} instance.
 * <p>
 * WebDriver's standard screenshot covers only the viewport. Full-page capture uses Firefox's
 * native {@link HasFullPageScreenshot}, or on Chrome/Edge a raw {@code Page.captureScreenshot}
 * CDP command with {@code captureBeyondViewport}. That command goes through {@link HasCdp}, which
 * passes plain JSON and so doesn't depend on Selenium's version-pinned devtools modules.
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

    public byte[] captureFullPage() {
        if (driver instanceof HasFullPageScreenshot firefox) {
            return firefox.getFullPageScreenshotAs(OutputType.BYTES);
        }
        if (driver instanceof HasCdp cdp) {
            Map<String, Object> metrics = cdp.executeCdpCommand("Page.getLayoutMetrics", Map.of());
            Object size = metrics.containsKey("cssContentSize") ? metrics.get("cssContentSize") : metrics.get("contentSize");
            Map<?, ?> content = (Map<?, ?>) size;
            double width = Math.ceil(((Number) content.get("width")).doubleValue());
            double height = Math.ceil(((Number) content.get("height")).doubleValue());
            Map<String, Object> result = cdp.executeCdpCommand("Page.captureScreenshot", Map.of(
                    "format", "png",
                    "captureBeyondViewport", true,
                    "clip", Map.of("x", 0, "y", 0, "width", width, "height", height, "scale", 1)));
            return Base64.getDecoder().decode((String) result.get("data"));
        }
        throw new UnsupportedOperationException(
                "Full-page screenshots require Chrome, Edge, or Firefox; not supported for this browser.");
    }

    public String captureFullPageBase64() {
        return Base64.getEncoder().encodeToString(captureFullPage());
    }

    public void captureFullPageToFile(String filePath) throws IOException {
        Files.write(Path.of(filePath), captureFullPage());
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
