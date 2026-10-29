package com.voltajeba.selenium.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import com.voltajeba.selenium.mcp.webdriver.AutoWaitSettings;
import com.voltajeba.selenium.mcp.webdriver.Tools;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * MCP-facing wrapper around {@link Tools} for explicit waits. Every wait method blocks
 * up to the given timeout and returns a failure string (rather than throwing) if the
 * condition is never met.
 * <p>
 * It also holds the session-wide wait strategy ({@code setAutoWait}) and what generated tests need
 * to match it: {@code getWaitReport} says which steps need a wait, {@code getWaitCodeSnippets} gives
 * the Java for them. See {@link AutoWaitMessages}.
 * <p>
 * For waiting on in-flight network requests to settle (e.g. after a click that triggers
 * XHR/fetch calls), see {@code waitForNetworkIdle} in {@link NetworkTools}.
 */
@Component
@ToolGroup("wait")
public class WaitTools {

    private static final String LOCATOR_TYPE_DESC =
            "Locator strategy: id, name, css, xpath, className, linkText, partialLinkText, tagName, text (exact visible text, e.g. Create Put), or partialText (visible text contains)";
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private final BrowserSessions sessions;

    public WaitTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Wait until an element is visible on the page, up to the given timeout.")
    public String waitForVisible(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForVisible(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), resolveTimeout(timeoutSeconds));
                return "Element is now visible.";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for element to be visible: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until an element is visible, enabled and not covered by another element (e.g. a "
            + "loading overlay), so a click will reach it, up to the given timeout.")
    public String waitForClickable(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForClickable(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), resolveTimeout(timeoutSeconds));
                return "Element is now clickable.";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for element to be clickable: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until an element is present in the DOM (not necessarily visible), up to the given timeout.")
    public String waitForPresent(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForPresent(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), resolveTimeout(timeoutSeconds));
                return "Element is now present in the DOM.";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for element to be present: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until an element is invisible or removed from the DOM, up to the given timeout.")
    public String waitForInvisible(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForInvisible(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), resolveTimeout(timeoutSeconds));
                return "Element is now invisible.";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for element to be invisible: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until an element contains the given text, up to the given timeout.")
    public String waitForTextPresent(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "The text expected to appear in the element", required = true) String text,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForTextPresent(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), text, resolveTimeout(timeoutSeconds));
                return "Text is now present in element.";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for text to appear: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until the page title contains the given text, up to the given timeout.")
    public String waitForTitleContains(
            @McpToolParam(description = "The text expected to appear in the page title", required = true) String text,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForTitleContains(text, resolveTimeout(timeoutSeconds));
                return "Title now contains \"" + text + "\"";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for title: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until the current URL contains the given text, up to the given timeout.")
    public String waitForUrlContains(
            @McpToolParam(description = "The text expected to appear in the URL", required = true) String text,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForUrlContains(text, resolveTimeout(timeoutSeconds));
                return "URL now contains \"" + text + "\"";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for URL: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until the page has finished loading (document.readyState is \"complete\"), up to the given timeout.")
    public String waitForPageLoad(
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForPageLoad(resolveTimeout(timeoutSeconds));
                return "Page has finished loading.";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for page to load: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until a custom JavaScript expression evaluates truthy, up to the given timeout. "
            + "Use \"return\" for a full function body (e.g. \"return document.title === 'Done'\"), or a bare "
            + "boolean expression (e.g. \"!!document.querySelector('.ready')\").")
    public String waitForJsCondition(
            @McpToolParam(description = "JavaScript expression or \"return ...\" statement that should evaluate truthy", required = true) String script,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForJsCondition(script, resolveTimeout(timeoutSeconds));
                return "Condition is now true.";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for JavaScript condition: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until the number of elements matching a locator equals an expected count, up to the given timeout.")
    public String waitForElementCount(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the elements", required = true) String locatorValue,
            @McpToolParam(description = "The expected number of matching elements", required = true) Integer expectedCount,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_ALL_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForElementCount(tools.locator(locatorType, locatorValue, null, Boolean.TRUE.equals(visibleOnly)), expectedCount, resolveTimeout(timeoutSeconds));
                return "Element count is now " + expectedCount + ".";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for element count: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until an element's attribute or DOM property equals an expected value, up to the given timeout.")
    public String waitForAttributeToBe(
            @McpToolParam(description = LOCATOR_TYPE_DESC, required = true) String locatorType,
            @McpToolParam(description = "The locator value to find the element", required = true) String locatorValue,
            @McpToolParam(description = "The attribute or DOM property name to check", required = true) String attribute,
            @McpToolParam(description = "The expected value of the attribute", required = true) String expectedValue,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds,
            @McpToolParam(description = ElementMatch.INDEX_DESC, required = false) Integer index,
            @McpToolParam(description = ElementMatch.VISIBLE_ONLY_DESC, required = false) Boolean visibleOnly) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForAttributeToBe(tools.locator(locatorType, locatorValue, index, Boolean.TRUE.equals(visibleOnly)), attribute, expectedValue, resolveTimeout(timeoutSeconds));
                return "Attribute \"" + attribute + "\" is now \"" + expectedValue + "\".";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for attribute value: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Wait until the number of open windows/tabs equals an expected count, up to the given timeout. "
            + "Useful after an action expected to open or close a tab.")
    public String waitForNumberOfWindowsToBe(
            @McpToolParam(description = "The expected number of open windows/tabs", required = true) Integer expectedCount,
            @McpToolParam(description = "Max seconds to wait. Defaults to 10.", required = false) Integer timeoutSeconds) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.waitForNumberOfWindowsToBe(expectedCount, resolveTimeout(timeoutSeconds));
                return "Number of windows is now " + expectedCount + ".";
            } catch (Exception e) {
                return sessions.label() + "Timed out waiting for window count: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Set this browser session's wait strategy to match the framework you are generating "
            + "Selenium tests for, so a step that passes here also passes in the generated test. By default the session "
            + "is plain Selenium: no implicit wait, and nothing waited for after actions. implicitWaitSeconds is Selenium's "
            + "own implicit wait. networkIdleMillis / animationIdleMillis make every action (click, typing, navigation, "
            + "select, mouse/key actions, upload, alert answers) wait afterwards until no fetch/XHR request has been in "
            + "flight, then no animation has run, for that long. Each action then reports what it set off and which wait "
            + "the generated test needs there. Recommended for almost every app: networkIdleMillis=500 and "
            + "animationIdleMillis=300, so steps that set off requests or animations aren't missed. Every call replaces "
            + "the whole strategy: omitted values are off, and a call with no arguments restores plain Selenium.")
    public String setAutoWait(
            @McpToolParam(description = "Selenium implicit wait in seconds (driver.manage().timeouts().implicitlyWait). "
                    + "Every lookup that finds nothing - elementExists, countElements and waitForInvisible included - then "
                    + "waits this long first, as in Selenium. 0 or omitted: none.", required = false)
            Integer implicitWaitSeconds,
            @McpToolParam(description = "After each action, wait until no fetch/XHR request has been in flight for this "
                    + "many milliseconds, e.g. 500. 0 or omitted: no network wait.", required = false)
            Long networkIdleMillis,
            @McpToolParam(description = "Max seconds the network wait may take before carrying on. Defaults to 10.", required = false)
            Integer networkTimeoutSeconds,
            @McpToolParam(description = "After each action (and the network wait), wait until no CSS transition/animation "
                    + "or Web Animation has run for this many milliseconds, e.g. 300. Animations that repeat forever "
                    + "(spinners) are ignored. 0 or omitted: no animation wait.", required = false)
            Long animationIdleMillis,
            @McpToolParam(description = "Max seconds the animation wait may take before carrying on. Defaults to 10.", required = false)
            Integer animationTimeoutSeconds,
            @McpToolParam(description = "Comma-separated URL substrings whose requests don't count as network activity, "
                    + "e.g. google-analytics,/poll,hotjar.", required = false)
            String ignoreUrls) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                AutoWaitSettings settings = toSettings(implicitWaitSeconds, networkIdleMillis, networkTimeoutSeconds,
                        animationIdleMillis, animationTimeoutSeconds, ignoreUrls);
                tools.setAutoWait(settings);
                return sessions.label() + "Wait strategy set: " + AutoWaitMessages.describe(settings) + "."
                        + trackingCaveat(tools, settings)
                        + "\nIn the generated test, right after creating the driver:\n" + tools.generateWaitSetup(settings)
                        + (settings.waitsAfterActions() ? "getWaitCodeSnippets has the Waits helper class these lines use." : "");
            } catch (IllegalArgumentException e) {
                return sessions.label() + "Invalid wait strategy: " + ErrorMessages.of(e);
            } catch (Exception e) {
                return sessions.label() + "Failed to set the wait strategy: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get this browser session's wait strategy (see setAutoWait) and the Java a generated test "
            + "needs right after creating its driver to match it.")
    public String getAutoWait() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                AutoWaitSettings settings = tools.getAutoWait();
                return sessions.label() + "Wait strategy: " + AutoWaitMessages.describe(settings) + "."
                        + trackingCaveat(tools, settings)
                        + "\nIn the generated test, right after creating the driver:\n" + tools.generateWaitSetup(settings);
            } catch (Exception e) {
                return sessions.label() + "Failed to get the wait strategy: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "List the steps of this browser session whose generated test code needs a wait, and which "
            + "one: a click whose target was covered (needs Waits.clickable before it), or an action that set off "
            + "requests or animations (needs Waits.networkIdle / animationsIdle after it) - however fast they finished "
            + "here, since they can take seconds in CI. Call before writing the test; see getWaitCodeSnippets for the code.")
    public String getWaitReport(
            @McpToolParam(description = "Clear the recorded steps after reporting, e.g. before recording another flow. "
                    + "Defaults to false.", required = false)
            Boolean clear) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                String report = AutoWaitMessages.report(tools);
                if (Boolean.TRUE.equals(clear)) {
                    tools.clearWaitRecords();
                    report += "\nRecorded steps cleared.";
                }
                return sessions.label() + report;
            } catch (Exception e) {
                return sessions.label() + "Failed to build the wait report: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the Java for waiting in generated Selenium tests the way this session waits: how to "
            + "use the waits without making tests flaky, the setup lines for right after creating the driver, and a "
            + "Waits helper class (clickable, networkIdle, animationsIdle, afterAction) that runs the same page "
            + "checks this server runs. Java 8+, Selenium 4.")
    public String getWaitCodeSnippets() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            AutoWaitSettings settings = tools.isBrowserOpen() ? tools.getAutoWait() : AutoWaitSettings.NONE;
            String source = tools.isBrowserOpen()
                    ? "This session's wait strategy: " + AutoWaitMessages.describe(settings) + "."
                    : "No browser is open, so this uses the default strategy (plain Selenium) and default idle times.";
            return source + "\n\n" + USAGE_RULES
                    + "\n\nSetup, right after creating the driver:\n```java\n" + tools.generateWaitSetup(settings) + "```"
                    + "\n\nHelper class (add your package line):\n```java\n" + tools.generateWaitsHelper(settings) + "```";
        }
    }

    private static final String USAGE_RULES = """
            Using these waits without making tests flaky:
            1. Wait on conditions, never on time: no Thread.sleep. Durations in the wait report describe this run; they \
            are not values to sleep for.
            2. Put waits where getWaitReport says, rather than around every action: Waits.clickable before a step whose \
            target was covered; Waits.networkIdle / animationsIdle / afterAction after a step that set off requests or \
            animations. A step that set one off needs the wait even if it finished instantly here - in CI it can take seconds.
            3. After a wait, assert on the outcome (text, URL, element state): a settled page isn't proof the step worked.
            4. Selenium's docs warn against mixing implicit and explicit waits. Prefer explicit waits with no implicit wait; \
            if the framework already sets one, keep it (match it with setAutoWait), and know that each poll of an explicit \
            wait for a missing element then blocks for the implicit timeout.
            5. If the project already has wait utilities (a BasePage.waitFor..., its own Waits class), use them and take \
            this helper as the reference for what to check; add only what is missing.
            6. On Firefox and Safari the helper can't see requests a page makes while it loads. After a navigation there, \
            also wait for the element the next step needs (Waits.clickable, ExpectedConditions.visibilityOfElementLocated).""";

    /**
     * Builds a wait strategy from tool parameters, where {@code null} means off for the idle times and the
     * default for the timeouts.
     */
    static AutoWaitSettings toSettings(Integer implicitWaitSeconds, Long networkIdleMillis, Integer networkTimeoutSeconds,
                                       Long animationIdleMillis, Integer animationTimeoutSeconds, String ignoreUrls) {
        return new AutoWaitSettings(
                Duration.ofSeconds(implicitWaitSeconds == null ? 0 : implicitWaitSeconds),
                Duration.ofMillis(networkIdleMillis == null ? 0 : networkIdleMillis),
                networkTimeoutSeconds == null ? AutoWaitSettings.DEFAULT_TIMEOUT : Duration.ofSeconds(networkTimeoutSeconds),
                Duration.ofMillis(animationIdleMillis == null ? 0 : animationIdleMillis),
                animationTimeoutSeconds == null ? AutoWaitSettings.DEFAULT_TIMEOUT : Duration.ofSeconds(animationTimeoutSeconds),
                ignoreUrls == null ? List.of() : Arrays.asList(ignoreUrls.split(",")));
    }

    private static String trackingCaveat(Tools tools, AutoWaitSettings settings) {
        if (!settings.waitsAfterActions() || tools.isAutoWaitTrackingFromPageStart()) {
            return "";
        }
        return " Note: on this browser, requests a page makes while it loads can't be seen - only those made after it "
                + "has loaded.";
    }

    private int resolveTimeout(Integer timeoutSeconds) {
        return timeoutSeconds == null ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }
}
