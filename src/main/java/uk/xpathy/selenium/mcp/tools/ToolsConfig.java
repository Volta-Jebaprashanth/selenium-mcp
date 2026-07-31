package uk.xpathy.selenium.mcp.tools;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.xpathy.selenium.mcp.webdriver.Tools;

/**
 * Produces the single {@link Tools} instance shared by every {@code @McpTool} class.
 * There is one browser session per server process, so all tool classes must operate
 * on the same {@link Tools}/driver rather than each holding its own.
 */
@Configuration
public class ToolsConfig {

    @Bean
    public Tools tools() {
        return new Tools();
    }
}
