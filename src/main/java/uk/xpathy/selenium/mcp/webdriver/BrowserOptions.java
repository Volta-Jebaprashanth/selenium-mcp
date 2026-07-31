package uk.xpathy.selenium.mcp.webdriver;

/**
 * Launch-time options for {@link BrowserFactory#create(String, BrowserOptions)}.
 * {@code windowSize} is a {@code "WIDTHxHEIGHT"} string, e.g. {@code "1920x1080"}.
 */
public record BrowserOptions(boolean headless, boolean incognito, String windowSize, String userAgent) {

    public static final BrowserOptions DEFAULT = new BrowserOptions(false, false, null, null);
}
