package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.util.Set;

/**
 * Manages browser cookies for the current domain on a single {@link WebDriver} instance.
 */
public class CookieManager {

    private final WebDriver driver;

    public CookieManager(WebDriver driver) {
        this.driver = driver;
    }

    public void addCookie(String name, String value) {
        driver.manage().addCookie(new Cookie(name, value));
    }

    public void addCookie(String name, String value, String domain, String path) {
        Cookie.Builder builder = new Cookie.Builder(name, value);
        if (domain != null && !domain.isBlank()) builder.domain(domain);
        if (path != null && !path.isBlank()) builder.path(path);
        driver.manage().addCookie(builder.build());
    }

    public Cookie getCookie(String name) {
        return driver.manage().getCookieNamed(name);
    }

    public Set<Cookie> getAllCookies() {
        return driver.manage().getCookies();
    }

    public void deleteCookie(String name) {
        driver.manage().deleteCookieNamed(name);
    }

    public void deleteAllCookies() {
        driver.manage().deleteAllCookies();
    }
}
