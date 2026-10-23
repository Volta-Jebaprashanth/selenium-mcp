package uk.xpathy.selenium.mcp.webdriver;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElementInteractorTest {

    private final WebDriver driver = mock(WebDriver.class);
    private final WebElement button = mock(WebElement.class);
    private final ElementInteractor interactor = new ElementInteractor(driver, new Locators());
    private final By locator = By.id("save");

    ElementInteractorTest() {
        when(driver.findElement(locator)).thenReturn(button);
    }

    @Test
    void anInterceptedClickIsRetriedUntilTheCoverGoes() {
        doThrow(new ElementClickInterceptedException("covered"))
                .doThrow(new ElementClickInterceptedException("covered"))
                .doNothing()
                .when(button).click();

        interactor.click(locator, Duration.ofSeconds(5));

        verify(button, times(3)).click();
    }

    @Test
    void withNoWaitAnInterceptedClickFailsAtOnce() {
        doThrow(new ElementClickInterceptedException("covered")).when(button).click();

        assertThrows(ElementClickInterceptedException.class, () -> interactor.click(locator, Duration.ZERO));
        verify(button, times(1)).click();
    }

    @Test
    void otherClickFailuresAreNotRetried() {
        doThrow(new ElementNotInteractableException("hidden")).when(button).click();

        assertThrows(ElementNotInteractableException.class, () -> interactor.click(locator, Duration.ofSeconds(5)));
        verify(button, times(1)).click();
    }

    @Test
    void aClickThatWorksIsMadeOnce() {
        doNothing().when(button).click();

        interactor.click(locator, Duration.ofSeconds(5));

        verify(button, times(1)).click();
    }
}
