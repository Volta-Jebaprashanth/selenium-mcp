package uk.xpathy.selenium.mcp.webdriver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the Java a generated Selenium test needs to wait the way a session's {@link AutoWaitSettings}
 * do: a {@code Waits} helper class, and the lines to call right after creating the driver.
 * <p>
 * The helper embeds the same page scripts the session runs - {@code wait-tracker.js} (see
 * {@link AutoWaiter}) and {@link ClickabilityChecker#STATE_FN} - so a step that settled here settles in
 * the test by the same rule, rather than by an approximation of it. The output targets Java 8+ and
 * Selenium 4, since generated tests land in existing projects that may be on either.
 */
public class WaitCodeGenerator {

    static final long DEFAULT_NETWORK_IDLE_MILLIS = 500;
    static final long DEFAULT_ANIMATION_IDLE_MILLIS = 300;

    /**
     * @return the statements to run right after creating the driver, as Java source
     */
    public String setup(AutoWaitSettings settings) {
        StringBuilder sb = new StringBuilder();
        if (!settings.implicitWait().isZero()) {
            sb.append("driver.manage().timeouts().implicitlyWait(").append(durationCode(settings.implicitWait()))
                    .append(");\n");
        }
        if (settings.waitsAfterActions()) {
            sb.append("Waits.install(driver);\n");
        }
        if (sb.isEmpty()) {
            sb.append("// No wait strategy: plain Selenium, nothing to set up. Add an explicit wait at each step that needs one.\n");
        }
        return sb.toString();
    }

    /**
     * @return a complete {@code Waits} class (no package line) whose defaults are {@code settings}' idle
     * times and timeouts, falling back to sensible values for the waits the session has switched off
     */
    public String helperClass(AutoWaitSettings settings) {
        long networkIdle = settings.waitsForNetwork() ? settings.networkIdle().toMillis() : DEFAULT_NETWORK_IDLE_MILLIS;
        long animationIdle = settings.waitsForAnimations() ? settings.animationIdle().toMillis() : DEFAULT_ANIMATION_IDLE_MILLIS;
        return """
                import org.openqa.selenium.By;
                import org.openqa.selenium.JavascriptException;
                import org.openqa.selenium.JavascriptExecutor;
                import org.openqa.selenium.StaleElementReferenceException;
                import org.openqa.selenium.WebDriver;
                import org.openqa.selenium.WebElement;
                import org.openqa.selenium.chromium.HasCdp;
                import org.openqa.selenium.support.ui.ExpectedConditions;
                import org.openqa.selenium.support.ui.WebDriverWait;

                import java.time.Duration;
                import java.util.Collections;
                import java.util.Map;

                /**
                 * Explicit waits matching the selenium-mcp session this test was generated from. The page
                 * scripts below are the ones the session ran, so a step that settled there settles here.
                 * Call install(driver) once after creating the driver; then, at each step that needs it:
                 * clickable(driver, by) before acting on an element that can be covered (overlays, spinners),
                 * networkIdle(driver) after an action that sends requests, animationsIdle(driver) after one
                 * that animates, or afterAction(driver) for both, in the order the session waited.
                 * Every wait throws TimeoutException when its condition isn't met in time.
                 */
                public final class Waits {

                    public static final long NETWORK_IDLE_MILLIS = %d;
                    public static final Duration NETWORK_TIMEOUT = %s;
                    public static final long ANIMATION_IDLE_MILLIS = %d;
                    public static final Duration ANIMATION_TIMEOUT = %s;
                    public static final Duration CLICKABLE_TIMEOUT = %s;

                    /** URL substrings whose requests don't count as network activity. */
                    private static final String IGNORED_URLS = %s;

                    /** Counts in-flight fetch/XHR requests and running animations on the page. */
                    private static final String TRACKER = String.join("\\n",
                %s);

                    /** Reports whether an element is visible, enabled and not covered at its centre. */
                    private static final String CLICKABLE = String.join("\\n",
                %s);

                    private Waits() {
                    }

                    /**
                     * Starts tracking. On Chrome/Edge the tracker then runs from the start of every page load;
                     * on other browsers it is added to each page when a wait first runs there, so requests a
                     * page makes while it loads are not seen.
                     */
                    public static void install(WebDriver driver) {
                        if (driver instanceof HasCdp) {
                            ((HasCdp) driver).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                                    Collections.<String, Object>singletonMap("source", IGNORED_URLS + TRACKER));
                        }
                        ensureTracker(driver);
                    }

                    /** Waits as the session did after each action: network idle, then animations idle. */
                    public static void afterAction(WebDriver driver) {
                        networkIdle(driver);
                        animationsIdle(driver);
                    }

                    public static void networkIdle(WebDriver driver) {
                        networkIdle(driver, NETWORK_IDLE_MILLIS, NETWORK_TIMEOUT);
                    }

                    /** Waits until no fetch/XHR request has been in flight for idleMillis, counted from now. */
                    public static void networkIdle(WebDriver driver, long idleMillis, Duration timeout) {
                        settle(driver, "network", "networkIdle", idleMillis, timeout);
                    }

                    public static void animationsIdle(WebDriver driver) {
                        animationsIdle(driver, ANIMATION_IDLE_MILLIS, ANIMATION_TIMEOUT);
                    }

                    /**
                     * Waits until no CSS transition/animation or Web Animation has run for idleMillis, counted
                     * from now. Animations that repeat forever (spinners) are left out.
                     */
                    public static void animationsIdle(WebDriver driver, long idleMillis, Duration timeout) {
                        settle(driver, "animations", "animationsIdle", idleMillis, timeout);
                    }

                    public static WebElement clickable(WebDriver driver, By locator) {
                        return clickable(driver, locator, CLICKABLE_TIMEOUT);
                    }

                    /**
                     * Waits until the element is visible, enabled and not covered by another element, so a
                     * click reaches it - stricter than ExpectedConditions.elementToBeClickable, which a button
                     * under a loading overlay already passes.
                     */
                    public static WebElement clickable(WebDriver driver, final By locator, Duration timeout) {
                        return new WebDriverWait(driver, timeout).ignoring(StaleElementReferenceException.class)
                                .until(d -> {
                                    WebElement element = ExpectedConditions.elementToBeClickable(locator).apply(d);
                                    if (element == null) {
                                        return null;
                                    }
                                    Object state = js(d).executeScript(CLICKABLE, element);
                                    return state instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) state).get("clickable"))
                                            ? element : null;
                                });
                    }

                    private static void settle(WebDriver driver, String phase, final String check, final long idleMillis,
                                               Duration timeout) {
                        ensureTracker(driver);
                        js(driver).executeScript("window.__seleniumMcpWait.restartIdle(arguments[0]);", phase);
                        new WebDriverWait(driver, timeout).ignoring(JavascriptException.class).until(d -> {
                            Object idle = js(d).executeScript(
                                    "var t = window.__seleniumMcpWait; return t ? t." + check + "(arguments[0]) : null;",
                                    idleMillis);
                            if (idle == null) {
                                ensureTracker(d);
                                return false;
                            }
                            return Boolean.TRUE.equals(idle);
                        });
                    }

                    private static void ensureTracker(WebDriver driver) {
                        js(driver).executeScript("if (!window.__seleniumMcpWait) {\\n" + IGNORED_URLS + TRACKER + "\\n}");
                    }

                    private static JavascriptExecutor js(WebDriver driver) {
                        return (JavascriptExecutor) driver;
                    }
                }
                """.formatted(
                networkIdle, durationCode(settings.networkTimeout()),
                animationIdle, durationCode(settings.animationTimeout()),
                durationCode(AutoWaitSettings.DEFAULT_TIMEOUT),
                javaString(ignoreListScript(settings.ignoredUrls())),
                javaLines(codeLines(AutoWaiter.TRACKER_JS)),
                javaLines(codeLines(ClickabilityChecker.STATE_FN + "return interactState(arguments[0], true);")));
    }

    private static String ignoreListScript(List<String> ignoredUrls) {
        String source = AutoWaiter.trackerSource(ignoredUrls);
        return source.substring(0, source.indexOf('\n') + 1);
    }

    /**
     * @return {@code source} without comment-only and blank lines, which only cost the reader tokens
     */
    private static List<String> codeLines(String source) {
        List<String> lines = new ArrayList<>();
        for (String line : source.split("\\R")) {
            String stripped = line.strip();
            if (!stripped.isEmpty() && !stripped.startsWith("//")) {
                lines.add(line.stripTrailing());
            }
        }
        return lines;
    }

    private static String javaLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append("            ").append(javaString(lines.get(i))).append(i < lines.size() - 1 ? ",\n" : "");
        }
        return sb.toString();
    }

    static String javaString(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.append('"').toString();
    }

    static String durationCode(Duration duration) {
        long millis = duration.toMillis();
        return millis % 1000 == 0 ? "Duration.ofSeconds(" + millis / 1000 + ")" : "Duration.ofMillis(" + millis + ")";
    }
}
