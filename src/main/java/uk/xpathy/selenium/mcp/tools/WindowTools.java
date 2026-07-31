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
public class WindowTools {

    private final Tools tools;

    public WindowTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Maximize the current browser window.")
    public String maximizeWindow() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.maximizeWindow();
                return "Window maximized.";
            } catch (Exception e) {
                return "Failed to maximize window: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Minimize the current browser window.")
    public String minimizeWindow() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.minimizeWindow();
                return "Window minimized.";
            } catch (Exception e) {
                return "Failed to minimize window: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Make the current browser window fullscreen.")
    public String fullscreenWindow() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.fullscreenWindow();
                return "Window set to fullscreen.";
            } catch (Exception e) {
                return "Failed to fullscreen window: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Resize the current browser window.")
    public String setWindowSize(
            @McpToolParam(description = "Window width in pixels", required = true) Integer width,
            @McpToolParam(description = "Window height in pixels", required = true) Integer height) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.setWindowSize(width, height);
                return "Window resized to " + width + "x" + height;
            } catch (Exception e) {
                return "Failed to resize window: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the current browser window's size.")
    public String getWindowSize() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Dimension size = tools.getWindowSize();
                return size.getWidth() + "x" + size.getHeight();
            } catch (Exception e) {
                return "Failed to get window size: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Move the current browser window to the given screen position.")
    public String setWindowPosition(
            @McpToolParam(description = "X coordinate in pixels", required = true) Integer x,
            @McpToolParam(description = "Y coordinate in pixels", required = true) Integer y) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.setWindowPosition(x, y);
                return "Window moved to (" + x + ", " + y + ")";
            } catch (Exception e) {
                return "Failed to move window: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the handle of the currently focused window/tab.")
    public String getWindowHandle() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                return tools.getWindowHandle();
            } catch (Exception e) {
                return "Failed to get window handle: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get the handles of all open windows/tabs.")
    public String getWindowHandles() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Set<String> handles = tools.getWindowHandles();
                return handles.stream().collect(Collectors.joining(", "));
            } catch (Exception e) {
                return "Failed to get window handles: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Switch the driver's focus to the window/tab with the given handle. Use getWindowHandles to list handles.")
    public String switchToWindow(
            @McpToolParam(description = "The window handle to switch to", required = true) String handle) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.switchToWindow(handle);
                return "Switched to window " + handle;
            } catch (Exception e) {
                return "Failed to switch window: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Open a new browser tab and switch focus to it.")
    public String openNewTab() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                String handle = tools.openNewTab();
                return "Opened new tab with handle " + handle;
            } catch (Exception e) {
                return "Failed to open new tab: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Close the currently focused window/tab. Does not release the driver — use closeBrowser for that.")
    public String closeCurrentWindow() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.closeCurrentWindow();
                return "Closed current window.";
            } catch (Exception e) {
                return "Failed to close window: " + e.getMessage();
            }
        }
    }
}
