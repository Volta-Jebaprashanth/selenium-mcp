package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.Pdf;
import org.openqa.selenium.PrintsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.print.PrintOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * File-oriented capabilities on a single {@link WebDriver} instance: uploading a local
 * file through a file input, and printing the current page to PDF.
 */
public class FileHelper {

    private final WebDriver driver;
    private final Locators locators;

    public FileHelper(WebDriver driver, Locators locators) {
        this.driver = driver;
        this.locators = locators;
    }

    /**
     * Uploads a local file by sending its absolute path to a {@code <input type="file">}
     * element. The file must exist on the machine running the browser.
     */
    public void uploadFile(By locator, String absoluteFilePath) {
        driver.findElement(locator).sendKeys(absoluteFilePath);
    }

    public void uploadFile(String locatorType, String locatorValue, String absoluteFilePath) {
        uploadFile(locators.toBy(locatorType, locatorValue), absoluteFilePath);
    }

    public String printToPdfBase64() {
        Pdf pdf = ((PrintsPage) driver).print(new PrintOptions());
        return pdf.getContent();
    }

    public void printToPdfFile(String filePath) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(printToPdfBase64());
        Files.write(Path.of(filePath), bytes);
    }
}
