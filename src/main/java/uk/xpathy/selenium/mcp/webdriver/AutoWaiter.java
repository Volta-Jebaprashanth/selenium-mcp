package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chromium.HasCdp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Applies a session's {@link AutoWaitSettings}: sets Selenium's implicit wait, and wraps every action
 * {@link Tools} performs so that, when enabled, it waits afterwards for the network and then for
 * animations to settle. Each action is logged as a {@link WaitRecord} of what it set off.
 * <p>
 * Settling is judged by {@code wait-tracker.js}, a page script that counts fetch/XHR requests and
 * animations - deliberately not by CDP network events, because a generated Selenium test can run the
 * same script ({@link WaitCodeGenerator} embeds it) and so wait for exactly the same thing. On Chrome/Edge
 * the script is registered with {@code Page.addScriptToEvaluateOnNewDocument} so it sees requests from
 * the start of each page load; elsewhere it is installed once a page has loaded, and a
 * {@link WaitRecord#trackerLate()} record says so. Timing out is not an error: the action already
 * happened, and the record says what was still busy.
 * <p>
 * With the default {@link AutoWaitSettings#NONE} nothing is tracked or waited for, as in plain Selenium.
 * Clicks that had to wait for a covering element are still recorded, since that wait is one the
 * generated test has to make too.
 */
public class AutoWaiter {

    static final String TRACKER_JS = loadTracker();

    private static final String MARK_JS =
            "var t = window.__seleniumMcpWait; if (!t) return false; t.mark(); return true;";
    private static final String STATUS_JS =
            "var t = window.__seleniumMcpWait; return t ? t.status(arguments[0], arguments[1]) : null;";
    private static final String RESTART_IDLE_JS =
            "var t = window.__seleniumMcpWait; if (t) t.restartIdle(arguments[0]);";
    private static final long POLL_MILLIS = 50;
    private static final int MAX_RECORDS = 500;

    private final WebDriver driver;
    private final List<WaitRecord> records = new ArrayList<>();
    private AutoWaitSettings settings = AutoWaitSettings.NONE;
    private String initScriptId;
    private int steps;
    private WaitRecord last;

    public AutoWaiter(WebDriver driver) {
        this.driver = driver;
    }

    public AutoWaitSettings getSettings() {
        return settings;
    }

    public void apply(AutoWaitSettings next) {
        if (!next.implicitWait().equals(settings.implicitWait())) {
            driver.manage().timeouts().implicitlyWait(next.implicitWait());
        }
        removeInitScript();
        settings = next;
        if (next.waitsAfterActions()) {
            String source = trackerSource();
            if (driver instanceof HasCdp cdp) {
                try {
                    Map<String, Object> result = cdp.executeCdpCommand(
                            "Page.addScriptToEvaluateOnNewDocument", Map.of("source", source));
                    initScriptId = String.valueOf(result.get("identifier"));
                } catch (WebDriverException e) {
                    rethrowIfBrowserGone(e);
                    // falls back to installing the tracker on each page once it has loaded
                }
            }
            install();
        }
    }

    /**
     * @return whether the tracker runs from the start of every page load (Chrome/Edge), rather than
     * being installed once a page has loaded
     */
    public boolean tracksFromPageStart() {
        return initScriptId != null;
    }

    /**
     * Runs {@code body}, then waits as the settings say and records what happened.
     */
    public void run(String action, Runnable body) {
        runCoverable(action, () -> {
            body.run();
            return ElementInteractor.Covered.NONE;
        });
    }

    /**
     * Like {@link #run}, for an action that may first wait for a covering element to go.
     *
     * @param body performs the action and returns what covered its target, if anything
     */
    public void runCoverable(String action, Supplier<ElementInteractor.Covered> body) {
        last = null;
        boolean tracking = settings.waitsAfterActions();
        if (tracking) {
            mark();
        }
        ElementInteractor.Covered covered = body.get();
        steps++;
        if (tracking) {
            record(settle(action, covered));
        } else if (covered.by() != null) {
            record(new WaitRecord(steps, action, covered.by(), covered.duration(), -1, -1,
                    Duration.ZERO, false, 0, Duration.ZERO, false, false));
        }
    }

    /**
     * @return what the last action set off and waited for, or {@code null} if it was neither tracked nor
     * had to wait for a covering element
     */
    public WaitRecord lastRecord() {
        return last;
    }

    /**
     * @return the recorded actions in order, up to the most recent {@value #MAX_RECORDS}
     */
    public List<WaitRecord> records() {
        return List.copyOf(records);
    }

    public void clearRecords() {
        records.clear();
        steps = 0;
        last = null;
    }

    /**
     * Unregisters the tracker from the browser, for an attached browser that outlives the session.
     */
    public void close() {
        try {
            removeInitScript();
        } catch (WebDriverException ignored) {
            // the browser or its debugging connection may already be gone
        }
    }

    private WaitRecord settle(String action, ElementInteractor.Covered covered) {
        long networkIdle = settings.networkIdle().toMillis();
        long animationIdle = settings.animationIdle().toMillis();
        Settling settling = new Settling();

        Duration networkWaited = Duration.ZERO;
        boolean networkTimedOut = false;
        if (settings.waitsForNetwork()) {
            long start = System.nanoTime();
            networkTimedOut = !settling.await("network", start + settings.networkTimeout().toNanos(),
                    networkIdle, animationIdle, Status::networkIdle);
            networkWaited = Duration.ofNanos(System.nanoTime() - start);
        }
        Duration animationWaited = Duration.ZERO;
        boolean animationTimedOut = false;
        if (settings.waitsForAnimations() && !settling.blocked) {
            long start = System.nanoTime();
            animationTimedOut = !settling.await("animations", start + settings.animationTimeout().toNanos(),
                    networkIdle, animationIdle, Status::animationsIdle);
            animationWaited = Duration.ofNanos(System.nanoTime() - start);
        }
        Status status = settling.latest;
        return new WaitRecord(steps, action, covered.by(), covered.duration(),
                status == null ? 0 : status.requests(), status == null ? 0 : status.animations(),
                networkWaited, networkTimedOut, networkTimedOut && status != null ? status.pending() : 0,
                animationWaited, animationTimedOut, settling.trackerLate);
    }

    /**
     * Polls the tracker for one settle phase, carrying what it learned into the next.
     */
    private final class Settling {
        Status latest;
        boolean trackerLate;
        boolean blocked;

        /**
         * Waits for {@code done}, with the idle time for {@code phase} counted from now - as the generated
         * {@code Waits} helper counts it from when it is called.
         *
         * @return {@code true} once {@code done} holds, {@code false} on timeout - or right away if an alert
         * or a closed window means the page can't be checked, in which case there is nothing to wait on
         */
        boolean await(String phase, long deadline, long networkIdle, long animationIdle, Predicate<Status> done) {
            try {
                ((JavascriptExecutor) driver).executeScript(RESTART_IDLE_JS, phase);
            } catch (UnhandledAlertException | NoSuchWindowException e) {
                blocked = true;
                return true;
            } catch (WebDriverException e) {
                rethrowIfBrowserGone(e);
            }
            while (true) {
                Status status;
                try {
                    status = poll(networkIdle, animationIdle);
                } catch (UnhandledAlertException | NoSuchWindowException e) {
                    blocked = true;
                    return true;
                }
                if (status == Status.MISSING) {
                    trackerLate = true;
                    install();
                } else if (status != null) {
                    latest = status;
                    if (done.test(status)) {
                        return true;
                    }
                }
                if (System.nanoTime() >= deadline) {
                    return false;
                }
                try {
                    Thread.sleep(POLL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
    }

    /**
     * @return the tracker's status, {@link Status#MISSING} on a page without it, or {@code null} if the
     * page couldn't answer (e.g. mid-navigation)
     */
    private Status poll(long networkIdle, long animationIdle) {
        Object result;
        try {
            result = ((JavascriptExecutor) driver).executeScript(STATUS_JS, networkIdle, animationIdle);
        } catch (UnhandledAlertException | NoSuchWindowException e) {
            throw e;
        } catch (WebDriverException e) {
            rethrowIfBrowserGone(e);
            return null;
        }
        if (!(result instanceof Map<?, ?> map)) {
            return Status.MISSING;
        }
        return new Status(
                Boolean.TRUE.equals(map.get("networkIdle")),
                Boolean.TRUE.equals(map.get("animationsIdle")),
                intOf(map.get("pending")), intOf(map.get("requests")), intOf(map.get("animations")));
    }

    private void mark() {
        try {
            Object marked = ((JavascriptExecutor) driver).executeScript(MARK_JS);
            if (!Boolean.TRUE.equals(marked)) {
                install();
                ((JavascriptExecutor) driver).executeScript(MARK_JS);
            }
        } catch (WebDriverException e) {
            // e.g. an alert is open; the action itself will report anything that matters
            rethrowIfBrowserGone(e);
        }
    }

    private void install() {
        try {
            ((JavascriptExecutor) driver).executeScript(trackerSource());
        } catch (WebDriverException e) {
            rethrowIfBrowserGone(e);
        }
    }

    private void removeInitScript() {
        if (initScriptId != null && driver instanceof HasCdp cdp) {
            String id = initScriptId;
            initScriptId = null;
            cdp.executeCdpCommand("Page.removeScriptToEvaluateOnNewDocument", Map.of("identifier", id));
        }
    }

    private void record(WaitRecord record) {
        last = record;
        records.add(record);
        if (records.size() > MAX_RECORDS) {
            records.removeFirst();
        }
    }

    private String trackerSource() {
        return trackerSource(settings.ignoredUrls());
    }

    /**
     * @return the tracker script preceded by the ignore list it reads
     */
    static String trackerSource(List<String> ignoredUrls) {
        StringBuilder sb = new StringBuilder("window.__seleniumMcpWaitIgnore = [");
        for (int i = 0; i < ignoredUrls.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(jsString(ignoredUrls.get(i)));
        }
        return sb.append("];\n").append(TRACKER_JS).toString();
    }

    static String jsString(String value) {
        StringBuilder sb = new StringBuilder("'");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '<' -> sb.append("\\x3c");
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.append('\'').toString();
    }

    private static int intOf(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static void rethrowIfBrowserGone(WebDriverException e) {
        if (BrowserSession.isBrowserGone(e)) {
            throw e;
        }
    }

    private static String loadTracker() {
        try (InputStream in = AutoWaiter.class.getResourceAsStream("wait-tracker.js")) {
            if (in == null) {
                throw new IllegalStateException("wait-tracker.js is missing from the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Status(boolean networkIdle, boolean animationsIdle, int pending, int requests, int animations) {
        static final Status MISSING = new Status(false, false, 0, 0, 0);
    }
}
