package uk.xpathy.selenium.mcp.tools;

import org.openqa.selenium.Cookie;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import uk.xpathy.selenium.mcp.webdriver.Tools;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP-facing wrapper around {@link Tools} for reading and writing cookies on the current domain.
 */
@Component
public class CookieTools {

    private final Tools tools;

    public CookieTools(Tools tools) {
        this.tools = tools;
    }

    @McpTool(description = "Add a cookie on the current domain.")
    public String addCookie(
            @McpToolParam(description = "Cookie name", required = true) String name,
            @McpToolParam(description = "Cookie value", required = true) String value,
            @McpToolParam(description = "Cookie domain. Defaults to the current domain.", required = false) String domain,
            @McpToolParam(description = "Cookie path. Defaults to /.", required = false) String path) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                if (domain == null && path == null) {
                    tools.addCookie(name, value);
                } else {
                    tools.addCookie(name, value, domain, path);
                }
                return "Cookie " + name + " added.";
            } catch (Exception e) {
                return "Failed to add cookie: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get a single cookie by name from the current domain.")
    public String getCookie(
            @McpToolParam(description = "Cookie name", required = true) String name) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Cookie cookie = tools.getCookie(name);
                return cookie == null ? "No cookie named " + name + " found." : cookie.toString();
            } catch (Exception e) {
                return "Failed to get cookie: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Get all cookies visible to the current page.")
    public String getAllCookies() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                Set<Cookie> cookies = tools.getAllCookies();
                if (cookies.isEmpty()) return "No cookies set.";
                return cookies.stream().map(cookie -> cookie.toString()).collect(Collectors.joining("\n"));
            } catch (Exception e) {
                return "Failed to get cookies: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Delete a single cookie by name.")
    public String deleteCookie(
            @McpToolParam(description = "Cookie name", required = true) String name) {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.deleteCookie(name);
                return "Cookie " + name + " deleted.";
            } catch (Exception e) {
                return "Failed to delete cookie: " + e.getMessage();
            }
        }
    }

    @McpTool(description = "Delete all cookies visible to the current page.")
    public String deleteAllCookies() {
        synchronized (tools) {
            if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";
            try {
                tools.deleteAllCookies();
                return "All cookies deleted.";
            } catch (Exception e) {
                return "Failed to delete cookies: " + e.getMessage();
            }
        }
    }
}
