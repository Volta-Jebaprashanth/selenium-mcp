package uk.xpathy.selenium.mcp.webdriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Creates {@link WebDriver} instances for the browsers this project supports:
 * Chrome, Firefox, and Edge. Chrome is the default when no browser is specified.
 */
public class BrowserFactory {

    private static final String DEFAULT_BROWSER = "chrome";

    /**
     * Resolves a user-supplied browser name to its canonical lowercase form,
     * falling back to {@code chrome} when blank.
     */
    public String resolveName(String browser) {
        return (browser == null || browser.isBlank()) ? DEFAULT_BROWSER : browser.trim().toLowerCase();
    }

    /**
     * Creates a new driver for the given browser.
     *
     * @throws IllegalArgumentException if the browser is not supported
     */
    public WebDriver create(String browser) {
        String name = resolveName(browser);
        return switch (name) {
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver();
            }
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                yield new FirefoxDriver();
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                yield new EdgeDriver();
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported browser: " + browser + ". Use chrome, firefox, or edge.");
        };
    }
}
