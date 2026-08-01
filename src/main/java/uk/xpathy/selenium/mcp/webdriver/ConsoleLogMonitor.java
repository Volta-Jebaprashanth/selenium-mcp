package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.events.CdpEventTypes;
import org.openqa.selenium.devtools.events.ConsoleEvent;
import org.openqa.selenium.logging.HasLogEvents;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Captures browser console output ({@code console.log}/{@code warn}/{@code error}, and similar)
 * via Selenium's CDP-backed {@link HasLogEvents}. The listener is armed once per driver and keeps
 * appending to an in-memory ring buffer until {@link #close()}.
 * <p>
 * Only available for drivers that implement {@link HasDevTools} (Chrome and Edge, via CDP).
 * Firefox is not supported and {@link #start()} throws {@link UnsupportedOperationException}.
 */
public class ConsoleLogMonitor {

    private static final int MAX_ENTRIES = 500;

    private final WebDriver driver;
    private final Deque<ConsoleLogEntry> entries = new ArrayDeque<>();
    private boolean listenerAttached;

    public ConsoleLogMonitor(WebDriver driver) {
        this.driver = driver;
    }

    public record ConsoleLogEntry(Instant timestamp, String type, String message) {
    }

    public boolean isSupported() {
        return driver instanceof HasDevTools && driver instanceof HasLogEvents;
    }

    public synchronized void start() {
        if (!isSupported()) {
            throw new UnsupportedOperationException(
                    "Console log capture requires Chrome or Edge (Chrome DevTools Protocol); "
                            + "not supported for this browser.");
        }
        if (listenerAttached) {
            return;
        }
        ((HasLogEvents) driver).onLogEvent(CdpEventTypes.consoleEvent(this::record));
        listenerAttached = true;
    }

    private synchronized void record(ConsoleEvent event) {
        String message = String.join(" ", event.getMessages());
        entries.addLast(new ConsoleLogEntry(event.getTimestamp(), event.getType(), message));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    public synchronized List<ConsoleLogEntry> getEntries(String type, int limit) {
        return entries.stream()
                .filter(e -> type == null || type.isBlank() || e.type().equalsIgnoreCase(type))
                .limit(limit <= 0 ? MAX_ENTRIES : limit)
                .collect(Collectors.toList());
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized void close() {
        entries.clear();
        listenerAttached = false;
    }
}
