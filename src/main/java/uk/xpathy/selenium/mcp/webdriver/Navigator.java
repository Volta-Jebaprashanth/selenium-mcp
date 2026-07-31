package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.WebDriver;

/**
 * Performs page-level navigation against a single {@link WebDriver} instance.
 */
public class Navigator {

    private final WebDriver driver;

    public Navigator(WebDriver driver) {
        this.driver = driver;
    }

    public void navigateTo(String url) {
        driver.get(url);
    }

    public void back() {
        driver.navigate().back();
    }

    public void forward() {
        driver.navigate().forward();
    }

    public void refresh() {
        driver.navigate().refresh();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getPageSource() {
        return driver.getPageSource();
    }
}
