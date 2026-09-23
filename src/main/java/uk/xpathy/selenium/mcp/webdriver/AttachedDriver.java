package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.service.DriverService;

/**
 * A driver connected to a Chrome/Edge browser over its remote-debugging port, together with
 * the driver service process backing it. Disconnecting means stopping {@code service} rather
 * than quitting {@code driver}: quitting would close a browser this session doesn't own, while
 * stopping the service leaves the browser running for other clients (e.g. a test) to attach to.
 */
public record AttachedDriver(WebDriver driver, DriverService service, String debuggerAddress) {
}
