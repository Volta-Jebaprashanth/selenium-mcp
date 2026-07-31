package uk.xpathy.selenium.mcp.tools;


import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class SystemTools {

    @McpTool(description = "Get current server status")
    public String getStatus() {
        return "Server is healthy. Uptime: 99.9%";
    }

}
