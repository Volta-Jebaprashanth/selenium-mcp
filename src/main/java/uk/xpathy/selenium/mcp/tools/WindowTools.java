package uk.xpathy.selenium.mcp.tools;

import org.openqa.selenium.Dimension;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP-facing wrapper around {@link Tools} for window/tab management and sizing.
 */
@Component
@ToolGroup("window")
public class WindowTools {

    private final BrowserSessions sessions;

    public WindowTools(BrowserSessions sessions) {
        this.sessions = sessions;
    }

    @McpTool(description = "Maximize the current browser window.")
    public String maximizeWindow() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.maximizeWindow();
                return "Window maximized.";
            } catch (Exception e) {
                return "Failed to maximize window: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Minimize the current browser window.")
    public String minimizeWindow() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.minimizeWindow();
                return "Window minimized.";
            } catch (Exception e) {
                return "Failed to minimize window: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Make the current browser window fullscreen.")
    public String fullscreenWindow() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.fullscreenWindow();
                return "Window set to fullscreen.";
            } catch (Exception e) {
                return "Failed to fullscreen window: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Resize the current browser window.")
    public String setWindowSize(
            @McpToolParam(description = "Window width in pixels", required = true) Integer width,
            @McpToolParam(description = "Window height in pixels", required = true) Integer height) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.setWindowSize(width, height);
                return "Window resized to " + width + "x" + height;
            } catch (Exception e) {
                return "Failed to resize window: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the current browser window's size.")
    public String getWindowSize() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Dimension size = tools.getWindowSize();
                return size.getWidth() + "x" + size.getHeight();
            } catch (Exception e) {
                return "Failed to get window size: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Move the current browser window to the given screen position.")
    public String setWindowPosition(
            @McpToolParam(description = "X coordinate in pixels", required = true) Integer x,
            @McpToolParam(description = "Y coordinate in pixels", required = true) Integer y) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.setWindowPosition(x, y);
                return "Window moved to (" + x + ", " + y + ")";
            } catch (Exception e) {
                return "Failed to move window: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the handle of the currently focused window/tab.")
    public String getWindowHandle() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getWindowHandle();
            } catch (Exception e) {
                return "Failed to get window handle: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Get the handles of all open windows/tabs.")
    public String getWindowHandles() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Set<String> handles = tools.getWindowHandles();
                return handles.stream().collect(Collectors.joining(", "));
            } catch (Exception e) {
                return "Failed to get window handles: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Switch the driver's focus to the window/tab with the given handle. Use getWindowHandles to list handles.")
    public String switchToWindow(
            @McpToolParam(description = "The window handle to switch to", required = true) String handle) {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.switchToWindow(handle);
                return "Switched to window " + handle;
            } catch (Exception e) {
                return "Failed to switch window: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Open a new browser tab and switch focus to it.")
    public String openNewTab() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                String handle = tools.openNewTab();
                return "Opened new tab with handle " + handle;
            } catch (Exception e) {
                return "Failed to open new tab: " + ErrorMessages.of(e);
            }
        }
    }

    @McpTool(description = "Close the currently focused window/tab. Does not release the driver — use closeBrowser for that.")
    public String closeCurrentWindow() {
        synchronized (sessions) {
            Tools tools = sessions.active();
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.closeCurrentWindow();
                return "Closed current window.";
            } catch (Exception e) {
                return "Failed to close window: " + ErrorMessages.of(e);
            }
        }
    }
}
