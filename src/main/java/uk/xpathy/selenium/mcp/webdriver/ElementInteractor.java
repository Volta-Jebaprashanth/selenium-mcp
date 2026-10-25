package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Performs element-level interactions against a single {@link WebDriver} instance.
 * {@link By} overloads are canonical; locator-string overloads resolve through
 * {@link Locators}.
 */
public class ElementInteractor {

    private static final long RETRY_INTERVAL_MILLIS = 250;

    private final WebDriver driver;
    private final Locators locators;
    private final ClickabilityChecker clickabilityChecker;

    public ElementInteractor(WebDriver driver, Locators locators, ClickabilityChecker clickabilityChecker) {
        this.driver = driver;
        this.locators = locators;
        this.clickabilityChecker = clickabilityChecker;
    }

    public void click(By locator) {
        driver.findElement(locator).click();
    }

    public void click(String locatorType, String locatorValue) {
        click(locators.toBy(locatorType, locatorValue));
    }

    /**
     * Clicks, retrying while the click is intercepted by another element on top - typically a loading
     * overlay that hasn't gone yet - for up to {@code waitWhileCovered}. Any other failure is thrown at once.
     * <p>
     * Whether the element was covered is checked before clicking, not inferred from the retries: some
     * driver versions hold a click themselves while an overlay is on its way out, which a plain Selenium
     * click can't count on elsewhere.
     *
     * @return what covered the element and for how long, or {@link Covered#NONE} if nothing did
     * @throws ElementClickInterceptedException if the element is still covered when the time is up
     */
    public Covered click(By locator, Duration waitWhileCovered) {
        long start = System.nanoTime();
        long deadline = start + waitWhileCovered.toNanos();
        String coveredBy = waitWhileCovered.isZero() ? null : coveredBy(locator);
        while (true) {
            try {
                driver.findElement(locator).click();
                return coveredBy == null ? Covered.NONE : new Covered(coveredBy, Duration.ofNanos(System.nanoTime() - start));
            } catch (ElementClickInterceptedException e) {
                if (coveredBy == null) {
                    coveredBy = "another element";
                }
                if (System.nanoTime() >= deadline) {
                    throw e;
                }
                try {
                    Thread.sleep(RETRY_INTERVAL_MILLIS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    /**
     * @param by       a short description of the element on top, e.g. {@code div#overlay.loading}
     * @param duration how long the click took to get through
     */
    public record Covered(String by, Duration duration) {
        public static final Covered NONE = new Covered(null, Duration.ZERO);
    }

    private String coveredBy(By locator) {
        try {
            ClickabilityChecker.Clickability state = clickabilityChecker.check(locator);
            return state.visible() && state.enabled() && !state.clickable() ? state.coveredBy() : null;
        } catch (RuntimeException e) {
            // the click itself will report a missing or unusable element
            return null;
        }
    }

    public void sendKeys(By locator, String text) {
        sendKeys(locator, text, false);
    }

    public void sendKeys(By locator, String text, boolean clearFirst) {
        WebElement element = driver.findElement(locator);
        if (clearFirst) {
            element.clear();
        }
        element.sendKeys(text);
    }

    public void sendKeys(String locatorType, String locatorValue, String text) {
        sendKeys(locatorType, locatorValue, text, false);
    }

    public void sendKeys(String locatorType, String locatorValue, String text, boolean clearFirst) {
        sendKeys(locators.toBy(locatorType, locatorValue), text, clearFirst);
    }
}
