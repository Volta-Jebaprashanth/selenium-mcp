package com.voltajeba.selenium.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import com.voltajeba.selenium.mcp.webdriver.BrowserFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotToolsTest {

    private final ScreenshotTools screenshots = new ScreenshotTools(new BrowserSessions(new BrowserFactory(), 5));

    @Test
    void aScreenshotComesBackAsAnImage() {
        CallToolResult result = screenshots.image("iVBORw0KGgo=", "Viewport screenshot");

        ImageContent image = assertInstanceOf(ImageContent.class, result.content().getFirst());
        assertEquals("image/png", image.mimeType());
        assertEquals("iVBORw0KGgo=", image.data());
        assertNotEquals(Boolean.TRUE, result.isError());
    }

    @Test
    void anImageTooBigToSendInlineAsksForAFilePath() {
        CallToolResult result = screenshots.image("A".repeat(ScreenshotTools.MAX_INLINE_BASE64_LENGTH + 1), "Full-page screenshot");

        assertEquals(Boolean.TRUE, result.isError());
        String text = assertInstanceOf(TextContent.class, result.content().getFirst()).text();
        assertTrue(text.contains("Pass filePath"), text);
    }

    @Test
    void withNoBrowserTheResultIsAnError() {
        CallToolResult result = screenshots.takeScreenshot(null, false);

        assertEquals(Boolean.TRUE, result.isError());
        assertEquals("No browser is open. Call openBrowser first.",
                assertInstanceOf(TextContent.class, result.content().getFirst()).text());
    }
}
