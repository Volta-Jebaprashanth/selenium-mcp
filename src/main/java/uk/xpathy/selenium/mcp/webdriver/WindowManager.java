package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import java.util.Set;

/**
 * Manages windows/tabs and the current window's size and position on a single
 * {@link WebDriver} instance.
 */
public class WindowManager {

    private final WebDriver driver;

    public WindowManager(WebDriver driver) {
        this.driver = driver;
    }

    public void maximize() {
        driver.manage().window().maximize();
    }

    public void minimize() {
        driver.manage().window().minimize();
    }

    public void fullscreen() {
        driver.manage().window().fullscreen();
    }

    public void setSize(int width, int height) {
        driver.manage().window().setSize(new Dimension(width, height));
    }

    public Dimension getSize() {
        return driver.manage().window().getSize();
    }

    public void setPosition(int x, int y) {
        driver.manage().window().setPosition(new Point(x, y));
    }

    public Point getPosition() {
        return driver.manage().window().getPosition();
    }

    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    public void switchToWindow(String handle) {
        driver.switchTo().window(handle);
    }

    /**
     * Opens a new tab and switches the driver's focus to it.
     *
     * @return the handle of the newly opened tab
     */
    public String openNewTab() {
        driver.switchTo().newWindow(WindowType.TAB);
        return driver.getWindowHandle();
    }

    public void closeCurrentWindow() {
        driver.close();
    }
}
