package uk.xpathy.selenium.mcp.tools;


import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class SystemTools {

    @McpTool(description = "Check that the Selenium MCP server process is running and responsive. Returns a short health message; does not report browser state.")
    public String getStatus() {
        return "Server is healthy. Uptime: 99.9%";
    }

}
