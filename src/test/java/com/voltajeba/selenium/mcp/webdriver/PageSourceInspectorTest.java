package com.voltajeba.selenium.mcp.webdriver;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Covers the Java side of {@link PageSourceInspector} - which elements and options reach the script.
 * What the scripts produce from a real DOM is covered by {@code ChromeIntegrationTest}.
 */
class PageSourceInspectorTest {

    private final WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
    private final JavascriptExecutor js = (JavascriptExecutor) driver;
    private final PageSourceInspector inspector = new PageSourceInspector(driver, new Locators());
    private final WebElement a = mock(WebElement.class);
    private final WebElement b = mock(WebElement.class);
    private final WebElement c = mock(WebElement.class);

    @Test
    void matchesBeyondTheLimitAreDroppedAndReportedAsTruncated() {
        when(driver.findElements(By.cssSelector("li"))).thenReturn(List.of(a, b, c));
        when(js.executeScript(anyString(), eq(List.of(a, b)), eq(true), eq(false), eq(true), eq(5), eq(3), eq(true)))
                .thenReturn("{\"totalMatches\":3}");

        assertEquals("{\"totalMatches\":3}", inspector.getFilteredElements("css", "li", true, false, true, 5, 2));
    }

    @Test
    void matchesWithinTheLimitAreAllPassedAndNotTruncated() {
        when(driver.findElements(By.id("x"))).thenReturn(List.of(a, b));

        inspector.getFilteredElements(By.id("x"), false, true, false, 20, 2);

        verify(js).executeScript(anyString(), eq(List.of(a, b)), eq(false), eq(true), eq(false), eq(20), eq(2), eq(false));
    }

    @Test
    void elementTreeDepthIsPassedToTheScript() {
        when(js.executeScript(anyString(), eq(3))).thenReturn("{}");

        assertEquals("{}", inspector.getElements(3));
    }
}
