package uk.xpathy.selenium.mcp.tools;

import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import uk.xpathy.selenium.mcp.webdriver.AutoWaitSettings;
import uk.xpathy.selenium.mcp.webdriver.Tools;
import uk.xpathy.selenium.mcp.webdriver.WaitRecord;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The text that keeps an agent's generated tests in step with the session's wait strategy: what the
 * strategy is, a note after each action that set something off (and which {@code Waits} call the
 * generated test needs there), a hint when an action fails the way a missing wait makes it fail, and
 * the session's wait report.
 * <p>
 * This server is for generating Selenium tests, so it never waits behind the agent's back - but every
 * wait it does make, or that the page needed, is spelled out, because the generated test has to make it too.
 */
final class AutoWaitMessages {

    static final String SNIPPETS_POINTER = "Before writing test code, call getWaitReport (which steps need a wait) "
            + "and getWaitCodeSnippets (the Java for those waits).";

    private AutoWaitMessages() {
    }

    static String describe(AutoWaitSettings settings) {
        if (settings.isNone()) {
            return "none - plain Selenium: no implicit wait, nothing waited for after actions";
        }
        List<String> parts = new ArrayList<>();
        parts.add(settings.implicitWait().isZero() ? "no implicit wait" : "implicit wait " + format(settings.implicitWait()));
        parts.add(settings.waitsForNetwork()
                ? "network idle " + format(settings.networkIdle()) + " after actions (timeout " + format(settings.networkTimeout()) + ")"
                : "no network wait");
        parts.add(settings.waitsForAnimations()
                ? "animations idle " + format(settings.animationIdle()) + " after actions (timeout " + format(settings.animationTimeout()) + ")"
                : "no animation wait");
        if (!settings.ignoredUrls().isEmpty()) {
            parts.add("ignoring requests to URLs containing " + String.join(", ", settings.ignoredUrls()));
        }
        return String.join("; ", parts);
    }

    /**
     * @return the lines an open/connect tool adds to its result, stating the session's wait strategy
     */
    static String sessionOpened(Tools tools) {
        AutoWaitSettings settings = settingsOf(tools);
        if (settings == null) {
            return "";
        }
        String text = "\nWait strategy: " + describe(settings) + ".";
        if (settings.isNone()) {
            text += " If the framework you are generating tests for waits globally (an implicit wait, or a wait "
                    + "after each action), call setAutoWait to match it, so what passes here also passes in the generated test.";
        }
        return text + " " + SNIPPETS_POINTER;
    }

    /**
     * @return a note on what the last action set off and what the generated test needs for it, or an
     * empty string when there is nothing to say
     */
    static String afterAction(Tools tools) {
        WaitRecord record;
        try {
            record = tools.getLastWaitRecord();
        } catch (RuntimeException e) {
            return "";
        }
        if (record == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (record.needsClickableWait()) {
            parts.add("The target was covered by " + record.coveredBy() + " when the click was attempted; it got "
                    + "through after " + format(record.coveredFor()) + ". Depending on the browser and driver version, a "
                    + "plain Selenium click there fails with ElementClickInterceptedException, so in the generated test "
                    + "wait for it first: Waits.clickable(driver, <locator>).");
        }
        if (record.tracked() && (record.needsNetworkWait() || record.needsAnimationWait())) {
            parts.add(settled(record) + " In the generated test, follow this step with: " + followUp(record) + ";");
        }
        if (record.networkTimedOut()) {
            parts.add("The network was still busy when the wait timed out (" + record.pending() + " request(s) in flight); "
                    + "carried on anyway. If those are background requests (analytics, long polling), "
                    + "add their URL to setAutoWait's ignoreUrls.");
        }
        if (record.animationTimedOut()) {
            parts.add("Animations were still running when the wait timed out; carried on anyway.");
        }
        if (record.trackerLate() && record.tracked()) {
            parts.add("This browser can't track requests a page makes while it loads, so any made during this load "
                    + "were missed; in the generated test, also wait for the element the next step needs.");
        }
        return parts.isEmpty() ? "" : "\n[auto-wait] " + String.join(" ", parts);
    }

    /**
     * @return a hint for failures a missing wait typically causes, or an empty string
     */
    static String failureHint(Tools tools, Exception e) {
        if (!(e instanceof NoSuchElementException || e instanceof ElementClickInterceptedException
                || e instanceof ElementNotInteractableException || e instanceof StaleElementReferenceException)) {
            return "";
        }
        AutoWaitSettings settings = settingsOf(tools);
        if (settings == null) {
            return "";
        }
        if (settings.isNone()) {
            return "\nHint: this session has no wait strategy, so this ran the moment the previous step returned - as a "
                    + "plain Selenium test would. If the page was still loading or animating, the generated test needs an "
                    + "explicit wait before this step (waitForClickable / waitForVisible here, Waits.clickable in the test), "
                    + "or call setAutoWait if the target framework waits globally.";
        }
        return "\nHint: the session's waits (" + describe(settings) + ") weren't enough for this step. The generated "
                + "test needs an explicit wait for this element before it (waitForClickable / waitForVisible here, "
                + "Waits.clickable in the test).";
    }

    /**
     * @return the session's wait report: the steps whose generated code needs a wait, and which one
     */
    static String report(Tools tools) {
        List<WaitRecord> records = tools.getWaitRecords();
        List<WaitRecord> needing = records.stream().filter(WaitRecord::needsWait).toList();
        StringBuilder sb = new StringBuilder("Wait strategy: ").append(describe(tools.getAutoWait())).append(".\n");
        boolean tracking = tools.getAutoWait().waitsAfterActions() || records.stream().anyMatch(WaitRecord::tracked);
        if (needing.isEmpty()) {
            sb.append(records.isEmpty() ? "No actions recorded yet." : "None of the " + records.size()
                    + " recorded action(s) needs a wait in the generated test.");
        } else {
            sb.append(needing.size()).append(" of ").append(records.size())
                    .append(" recorded action(s) need a wait in the generated test:\n");
            for (WaitRecord record : needing) {
                sb.append(reportLine(record)).append('\n');
            }
            sb.append("Steps not listed set off nothing the session could see; they need no wait.");
        }
        if (!tracking) {
            sb.append("\nRequest and animation tracking is off, so only clicks that had to wait for a covering element "
                    + "are recorded. To see which steps set off requests or animations, call setAutoWait with "
                    + "networkIdleMillis and/or animationIdleMillis, then repeat the flow.");
        }
        return sb.toString();
    }

    private static String reportLine(WaitRecord record) {
        List<String> needs = new ArrayList<>();
        if (record.needsClickableWait()) {
            needs.add("covered by " + record.coveredBy() + " -> Waits.clickable(driver, <locator>) before it");
        }
        if (record.tracked() && (record.needsNetworkWait() || record.needsAnimationWait())) {
            needs.add(triggered(record) + " -> " + followUp(record) + " after it");
        }
        if (record.networkTimedOut()) {
            needs.add("network never went idle here (" + record.pending() + " request(s) in flight) - consider ignoreUrls");
        }
        return " " + record.step() + ". " + record.action() + " - " + String.join("; ", needs);
    }

    private static String settled(WaitRecord record) {
        List<String> waited = new ArrayList<>();
        if (!record.networkWaited().isZero()) {
            waited.add(format(record.networkWaited()) + " for network idle");
        }
        if (!record.animationWaited().isZero()) {
            waited.add(format(record.animationWaited()) + " for animations");
        }
        String set = "This step " + triggered(record) + ".";
        return waited.isEmpty() ? set : set + " Waited " + String.join(", ", waited) + ".";
    }

    private static String triggered(WaitRecord record) {
        List<String> what = new ArrayList<>();
        if (record.requests() > 0) {
            what.add(record.requests() + " request(s)");
        }
        if (record.animations() > 0) {
            what.add(record.animations() + " animation(s)");
        }
        return what.isEmpty() ? "kept the page busy" : "set off " + String.join(" and ", what);
    }

    private static String followUp(WaitRecord record) {
        boolean network = record.needsNetworkWait();
        boolean animations = record.needsAnimationWait();
        if (network && animations) {
            return "Waits.afterAction(driver)";
        }
        return network ? "Waits.networkIdle(driver)" : "Waits.animationsIdle(driver)";
    }

    private static AutoWaitSettings settingsOf(Tools tools) {
        try {
            return tools.isBrowserOpen() ? tools.getAutoWait() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    static String format(Duration duration) {
        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + "ms";
        }
        return millis % 1000 == 0 ? millis / 1000 + "s" : String.format("%.1fs", millis / 1000.0);
    }
}
