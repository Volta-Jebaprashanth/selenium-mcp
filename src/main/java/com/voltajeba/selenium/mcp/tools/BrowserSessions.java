package com.voltajeba.selenium.mcp.tools;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.voltajeba.selenium.mcp.webdriver.BrowserFactory;
import com.voltajeba.selenium.mcp.webdriver.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * The named browser sessions this server holds, each its own {@link Tools} (and so its own browser,
 * network mocks and console log), and which of them is <em>active</em>. Every {@code @McpTool} class
 * acts on {@link #active()}, so tool signatures don't grow a session parameter: an agent driving
 * several browsers opens each under a name and calls {@code switchSession} between them.
 * <p>
 * Tool methods synchronize on this object, which serializes every tool call against every session -
 * the same one-call-at-a-time behavior as when there was a single browser, and it keeps the active
 * session from changing under a call in progress.
 * <p>
 * Sessions are closed when the server shuts down, so launched browsers aren't left running; browsers
 * attached over a debugging port are only disconnected, as with {@code closeBrowser}.
 */
@Component
public class BrowserSessions {

    public static final String DEFAULT_SESSION = "default";

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final BrowserFactory browserFactory;
    private final int maxSessions;
    private final Map<String, Tools> sessions = new LinkedHashMap<>();
    private final Tools noSession;
    private String activeName;

    @Autowired
    public BrowserSessions(@Value("${selenium.mcp.max-sessions:5}") int maxSessions) {
        this(new BrowserFactory(), maxSessions);
    }

    BrowserSessions(BrowserFactory browserFactory, int maxSessions) {
        if (maxSessions < 1) {
            throw new IllegalArgumentException("selenium.mcp.max-sessions must be 1 or more, got " + maxSessions);
        }
        this.browserFactory = browserFactory;
        this.maxSessions = maxSessions;
        this.noSession = new Tools(browserFactory);
    }

    /**
     * @return the active session's {@link Tools}, or, when no session is open, a {@link Tools} with no
     * browser, so the usual {@code isBrowserOpen()} precondition check reports that
     */
    public synchronized Tools active() {
        return activeName == null ? noSession : sessions.get(activeName);
    }

    public synchronized String activeName() {
        return activeName;
    }

    public synchronized boolean isEmpty() {
        return sessions.isEmpty();
    }

    public synchronized boolean has(String name) {
        return sessions.containsKey(name);
    }

    public synchronized Map<String, Tools> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(sessions));
    }

    /**
     * @return why a new session called {@code name} can't be opened, or {@code null} if it can
     */
    public synchronized String checkCanOpen(String name) {
        if (!NAME.matcher(name).matches()) {
            return "Invalid session name '" + name + "'. Use 1-32 letters, digits, '-' or '_'.";
        }
        if (sessions.containsKey(name)) {
            return "Session '" + name + "' is already open.";
        }
        if (sessions.size() >= maxSessions) {
            return "Already at the limit of " + maxSessions + " open sessions (" + String.join(", ", sessions.keySet())
                    + "). Close one with closeBrowser first, or raise --selenium.mcp.max-sessions.";
        }
        return null;
    }

    /**
     * Creates a session called {@code name}, lets {@code opener} open its browser, and makes it the active
     * session. If {@code opener} throws, nothing is registered and any half-opened browser is closed.
     *
     * @throws IllegalArgumentException if {@link #checkCanOpen(String)} would refuse {@code name}
     */
    public synchronized Tools open(String name, Consumer<Tools> opener) {
        String refusal = checkCanOpen(name);
        if (refusal != null) {
            throw new IllegalArgumentException(refusal);
        }
        Tools tools = new Tools(browserFactory);
        try {
            opener.accept(tools);
        } catch (RuntimeException e) {
            closeQuietly(tools);
            throw e;
        }
        sessions.put(name, tools);
        activeName = name;
        return tools;
    }

    public synchronized boolean switchTo(String name) {
        if (!sessions.containsKey(name)) {
            return false;
        }
        activeName = name;
        return true;
    }

    /**
     * Closes session {@code name} (quitting a launched browser, disconnecting from an attached one) and
     * forgets it, even if closing fails. If it was active, the most recently opened remaining session
     * becomes active.
     */
    public synchronized void close(String name) {
        Tools tools = remove(name);
        if (tools != null) {
            tools.closeBrowser();
        }
    }

    /**
     * Forgets the sessions whose browser has gone away - its last window closed by hand, or its process
     * crashed - releasing their drivers. If the active session was one of them, the most recently opened
     * remaining session becomes active, as with {@link #close(String)}.
     *
     * @return the names of the sessions removed
     */
    public synchronized List<String> pruneDead() {
        List<String> dead = sessions.entrySet().stream()
                .filter(entry -> !entry.getValue().isBrowserAlive())
                .map(Map.Entry::getKey)
                .toList();
        for (String name : dead) {
            closeQuietly(remove(name));
        }
        return dead;
    }

    /**
     * Closes every session, carrying on past any that fail to close.
     *
     * @return the names of the sessions that were open
     */
    @PreDestroy
    public synchronized List<String> closeAll() {
        List<String> names = new ArrayList<>(sessions.keySet());
        for (String name : names) {
            closeQuietly(sessions.remove(name));
        }
        activeName = null;
        return names;
    }

    /**
     * @return the session attached to the browser debugging at {@code debuggerAddress}, or {@code null}
     * @throws IllegalArgumentException if the address isn't {@code host:port}
     */
    public synchronized String sessionAttachedTo(String debuggerAddress) {
        String address = browserFactory.normalizeDebuggerAddress(debuggerAddress);
        for (Map.Entry<String, Tools> entry : sessions.entrySet()) {
            if (address.equals(entry.getValue().getDebuggerAddress())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * @return {@code "[name] "} for the active session while more than one is open, so results say which
     * browser they came from; otherwise an empty string
     */
    public synchronized String label() {
        return sessions.size() > 1 && activeName != null ? "[" + activeName + "] " : "";
    }

    private Tools remove(String name) {
        Tools tools = sessions.remove(name);
        if (tools != null && name.equals(activeName)) {
            activeName = sessions.isEmpty() ? null : new ArrayList<>(sessions.keySet()).getLast();
        }
        return tools;
    }

    private static void closeQuietly(Tools tools) {
        try {
            tools.closeBrowser();
        } catch (RuntimeException ignored) {
            // best-effort cleanup; the browser or driver may already be gone
        }
    }
}
