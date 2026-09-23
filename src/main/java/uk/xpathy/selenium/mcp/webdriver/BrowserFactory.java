package uk.xpathy.selenium.mcp.webdriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.service.DriverService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * Creates {@link WebDriver} instances for the browsers this project supports:
 * Chrome, Firefox, and Edge. Chrome is the default when no browser is specified.
 * <p>
 * Chrome and Edge can also be driven over a remote-debugging port, either by attaching to one
 * already running or by launching one that stays up after this process disconnects — see
 * {@link AttachedDriver}. Firefox has no equivalent that Selenium supports reliably.
 */
public class BrowserFactory {

    private static final String DEFAULT_BROWSER = "chrome";
    private static final String DEFAULT_DEBUGGER_ADDRESS = "127.0.0.1:9222";

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

    /**
     * Connects to a Chrome/Edge browser that is already running with remote debugging enabled
     * (started with {@code --remote-debugging-port}), instead of launching a new one.
     *
     * @param debuggerAddress {@code host:port}; defaults to {@code 127.0.0.1:9222} when blank
     * @throws IllegalArgumentException if the browser isn't chrome/edge or the address is malformed
     * @throws IllegalStateException    if nothing is listening on the address
     */
    public AttachedDriver attach(String browser, String debuggerAddress) {
        String name = requireChromium(browser);
        String address = normalizeDebuggerAddress(debuggerAddress);
        if (!isDebuggerListening(address)) {
            throw new IllegalStateException("Nothing is listening on " + address + ". Start " + name
                    + " with --remote-debugging-port=<port> and a separate --user-data-dir, or use launchDebugBrowser.");
        }
        return switch (name) {
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions co = new ChromeOptions();
                co.setExperimentalOption("debuggerAddress", address);
                ChromeDriverService service = ChromeDriverService.createDefaultService();
                yield connect(service, () -> new ChromeDriver(service, co), address);
            }
            default -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions eo = new EdgeOptions();
                eo.setExperimentalOption("debuggerAddress", address);
                EdgeDriverService service = EdgeDriverService.createDefaultService();
                yield connect(service, () -> new EdgeDriver(service, eo), address);
            }
        };
    }

    /**
     * Launches a new Chrome/Edge with remote debugging enabled on {@code port}, in detached mode
     * so the browser outlives the driver. Other clients (e.g. a test under repair) can attach to
     * the same browser via {@code debuggerAddress = 127.0.0.1:<port>}.
     *
     * @param userDataDir profile directory; defaults to a per-browser, per-port folder under the
     *                    system temp dir. Chrome 136+ refuses remote debugging on the default profile,
     *                    so a dedicated directory is always used.
     * @throws IllegalArgumentException if the browser isn't chrome/edge or the port is out of range
     * @throws IllegalStateException    if something is already listening on the port
     */
    public AttachedDriver launchDebuggable(String browser, int port, String userDataDir) {
        String name = requireChromium(browser);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        String address = "127.0.0.1:" + port;
        if (isDebuggerListening(address)) {
            throw new IllegalStateException("Port " + port + " is already in use. Attach to it with debuggerAddress "
                    + address + " instead.");
        }
        String profile = (userDataDir == null || userDataDir.isBlank())
                ? Path.of(System.getProperty("java.io.tmpdir"), "selenium-mcp-debug-" + name + "-" + port).toString()
                : userDataDir;
        List<String> args = List.of("--remote-debugging-port=" + port, "--user-data-dir=" + profile);
        return switch (name) {
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions co = new ChromeOptions();
                co.addArguments(args);
                co.setExperimentalOption("detach", true);
                ChromeDriverService service = ChromeDriverService.createDefaultService();
                yield connect(service, () -> new ChromeDriver(service, co), address);
            }
            default -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions eo = new EdgeOptions();
                eo.addArguments(args);
                eo.setExperimentalOption("detach", true);
                EdgeDriverService service = EdgeDriverService.createDefaultService();
                yield connect(service, () -> new EdgeDriver(service, eo), address);
            }
        };
    }

    /**
     * @return whether something accepts TCP connections on {@code debuggerAddress}
     * ({@code host:port}, defaulting to {@code 127.0.0.1:9222} when blank)
     */
    public boolean isDebuggerListening(String debuggerAddress) {
        String address = normalizeDebuggerAddress(debuggerAddress);
        int colon = address.lastIndexOf(':');
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address.substring(0, colon),
                    Integer.parseInt(address.substring(colon + 1))), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Normalizes a debugger address to {@code host:port}, accepting an optional {@code http://}
     * prefix and trailing slash, and defaulting to {@code 127.0.0.1:9222} when blank.
     *
     * @throws IllegalArgumentException if the address isn't {@code host:port}
     */
    public String normalizeDebuggerAddress(String debuggerAddress) {
        if (debuggerAddress == null || debuggerAddress.isBlank()) {
            return DEFAULT_DEBUGGER_ADDRESS;
        }
        String address = debuggerAddress.trim().replaceFirst("^https?://", "").replaceFirst("/+$", "");
        int colon = address.lastIndexOf(':');
        if (colon <= 0 || colon == address.length() - 1) {
            throw new IllegalArgumentException("debuggerAddress must be in the form host:port, e.g. 127.0.0.1:9222");
        }
        try {
            int port = Integer.parseInt(address.substring(colon + 1));
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("debuggerAddress must be in the form host:port, e.g. 127.0.0.1:9222");
        }
        return address;
    }

    private String requireChromium(String browser) {
        String name = resolveName(browser);
        if (!name.equals("chrome") && !name.equals("edge")) {
            throw new IllegalArgumentException(
                    "Connecting over a remote-debugging port is only supported for chrome and edge, not " + browser + ".");
        }
        return name;
    }

    private AttachedDriver connect(DriverService service, Supplier<WebDriver> driver, String address) {
        try {
            return new AttachedDriver(driver.get(), service, address);
        } catch (RuntimeException e) {
            service.stop();
            throw e;
        }
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
