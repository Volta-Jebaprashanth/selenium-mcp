package uk.xpathy.selenium.mcp.webdriver;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers JavaScript that the browser runs at the start of every new document, before the
 * page's own scripts, so helpers and hooks survive navigations and reloads instead of being lost
 * with each page's {@code window}. Backed by CDP's {@code Page.addScriptToEvaluateOnNewDocument},
 * sent as raw JSON through {@link HasCdp} so it works regardless of Selenium's devtools-module
 * version coverage.
 * <p>
 * Chrome/Edge only; other browsers throw {@link UnsupportedOperationException}. Registrations
 * apply to the tab that was current when they were added.
 */
public class InitScriptManager {

    private final WebDriver driver;
    private final Map<String, String> scripts = new LinkedHashMap<>();

    public InitScriptManager(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isSupported() {
        return driver instanceof HasCdp;
    }

    /**
     * Registers {@code source} to run on every new document, optionally also running it on the
     * current page right away.
     *
     * @return the identifier to pass to {@link #remove(String)}
     */
    public synchronized String add(String source, boolean runNow) {
        Map<String, Object> result = cdp().executeCdpCommand(
                "Page.addScriptToEvaluateOnNewDocument", Map.of("source", source));
        String id = String.valueOf(result.get("identifier"));
        scripts.put(id, source);
        if (runNow) {
            ((JavascriptExecutor) driver).executeScript(source);
        }
        return id;
    }

    public synchronized void remove(String id) {
        if (!scripts.containsKey(id)) {
            throw new IllegalArgumentException("No init script registered with id " + id);
        }
        cdp().executeCdpCommand("Page.removeScriptToEvaluateOnNewDocument", Map.of("identifier", id));
        scripts.remove(id);
    }

    public synchronized void clear() {
        for (String id : scripts.keySet()) {
            cdp().executeCdpCommand("Page.removeScriptToEvaluateOnNewDocument", Map.of("identifier", id));
        }
        scripts.clear();
    }

    public synchronized Map<String, String> list() {
        return new LinkedHashMap<>(scripts);
    }

    private HasCdp cdp() {
        if (!(driver instanceof HasCdp cdp)) {
            throw new UnsupportedOperationException(
                    "Init scripts require Chrome or Edge (Chrome DevTools Protocol); not supported for this browser.");
        }
        return cdp;
    }
}
