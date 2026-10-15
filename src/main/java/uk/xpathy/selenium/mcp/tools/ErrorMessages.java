package uk.xpathy.selenium.mcp.tools;

import org.openqa.selenium.WebDriverException;

/**
 * Turns exceptions into the short, single-purpose text an MCP client should see.
 * <p>
 * {@link WebDriverException#getMessage()} appends build info, system info, driver info,
 * the full capabilities block and the session id to the underlying error, which buries a
 * one-line cause (e.g. a JavaScript {@code TypeError}) under 15+ lines of noise and wastes
 * the client's tokens. This keeps only the driver's own message and drops the trailing
 * {@code (Session info: ...)} / {@code Build info:} style diagnostics.
 */
final class ErrorMessages {

    private static final String[] NOISE_MARKERS = {
            "(Session info:", "Build info:", "System info:", "Driver info:",
            "Capabilities {", "Session ID:", "For documentation on this error",
            "Command:", "Element:", "Stacktrace:"
    };

    private ErrorMessages() {
    }

    static String of(Throwable e) {
        if (e == null) {
            return "unknown error";
        }
        String message = e instanceof WebDriverException wde ? wde.getRawMessage() : e.getMessage();
        if (message == null || message.isBlank()) {
            Throwable cause = e.getCause();
            return cause != null && cause != e ? of(cause) : e.getClass().getSimpleName();
        }
        return trim(message);
    }

    static String trim(String message) {
        StringBuilder kept = new StringBuilder();
        for (String line : message.split("\\R")) {
            String stripped = line.strip();
            if (isNoise(stripped)) {
                break;
            }
            if (stripped.isEmpty()) {
                continue;
            }
            if (!kept.isEmpty()) {
                kept.append('\n');
            }
            kept.append(stripped);
        }
        return kept.isEmpty() ? message.strip() : kept.toString();
    }

    private static boolean isNoise(String line) {
        for (String marker : NOISE_MARKERS) {
            if (line.startsWith(marker)) {
                return true;
            }
        }
        return false;
    }
}
