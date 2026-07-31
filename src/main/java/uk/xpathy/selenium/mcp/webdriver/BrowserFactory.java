package uk.xpathy.selenium.mcp.webdriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

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
     * Creates a new driver for the given browser with default options.
     *
     * @throws IllegalArgumentException if the browser is not supported
     */
    public WebDriver create(String browser) {
        return create(browser, BrowserOptions.DEFAULT);
    }

    /**
     * Creates a new driver for the given browser, applying the given launch options.
     *
     * @throws IllegalArgumentException if the browser is not supported
     */
    public WebDriver create(String browser, BrowserOptions options) {
        String name = resolveName(browser);
        BrowserOptions opts = options == null ? BrowserOptions.DEFAULT : options;
        WebDriver driver = switch (name) {
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions co = new ChromeOptions();
                if (opts.headless()) co.addArguments("--headless=new");
                if (opts.incognito()) co.addArguments("--incognito");
                if (opts.userAgent() != null && !opts.userAgent().isBlank()) {
                    co.addArguments("--user-agent=" + opts.userAgent());
                }
                yield new ChromeDriver(co);
            }
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions fo = new FirefoxOptions();
                if (opts.headless()) fo.addArguments("-headless");
                if (opts.incognito()) fo.addArguments("-private");
                if (opts.userAgent() != null && !opts.userAgent().isBlank()) {
                    fo.addPreference("general.useragent.override", opts.userAgent());
                }
                yield new FirefoxDriver(fo);
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions eo = new EdgeOptions();
                if (opts.headless()) eo.addArguments("--headless=new");
                if (opts.incognito()) eo.addArguments("--inprivate");
                if (opts.userAgent() != null && !opts.userAgent().isBlank()) {
                    eo.addArguments("--user-agent=" + opts.userAgent());
                }
                yield new EdgeDriver(eo);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported browser: " + browser + ". Use chrome, firefox, or edge.");
        };
        applyWindowSize(driver, opts.windowSize());
        return driver;
    }

    private void applyWindowSize(WebDriver driver, String windowSize) {
        if (windowSize == null || windowSize.isBlank()) {
            return;
        }
        String[] parts = windowSize.toLowerCase().split("x");
        if (parts.length != 2) {
            throw new IllegalArgumentException("windowSize must be in the form WIDTHxHEIGHT, e.g. 1920x1080");
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            driver.manage().window().setSize(new Dimension(width, height));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("windowSize must be in the form WIDTHxHEIGHT, e.g. 1920x1080");
        }
    }
}
