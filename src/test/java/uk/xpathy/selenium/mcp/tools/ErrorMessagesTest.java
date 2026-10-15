package uk.xpathy.selenium.mcp.tools;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorMessagesTest {

    @Test
    void keepsOnlyTheDriverMessageOfAJavascriptError() {
        JavascriptException e = new JavascriptException(
                "javascript error: Cannot read properties of undefined (reading 'click')\n"
                        + "  (Session info: chrome=155.0.8059.41)");
        e.addInfo("Capabilities", "{browserName: chrome, ...}");

        assertTrue(e.getMessage().contains("Build info"), "precondition: Selenium adds diagnostics");
        assertEquals("javascript error: Cannot read properties of undefined (reading 'click')",
                ErrorMessages.of(e));
    }

    @Test
    void dropsDocumentationLinkAndSessionInfo() {
        NoSuchElementException e = new NoSuchElementException(
                "no such element: Unable to locate element: {\"method\":\"css selector\",\"selector\":\"#nope\"}\n"
                        + "  (Session info: chrome=155.0.8059.41)\n"
                        + "For documentation on this error, please visit: https://www.selenium.dev/...");

        assertEquals("no such element: Unable to locate element: {\"method\":\"css selector\",\"selector\":\"#nope\"}",
                ErrorMessages.of(e));
    }

    @Test
    void passesPlainExceptionsThrough() {
        assertEquals("Unsupported locator type: foo",
                ErrorMessages.of(new IllegalArgumentException("Unsupported locator type: foo")));
    }

    @Test
    void fallsBackToCauseOrTypeWhenThereIsNoMessage() {
        assertEquals("boom", ErrorMessages.of(new RuntimeException(null, new IllegalStateException("boom"))));
        assertEquals("IllegalStateException", ErrorMessages.of(new IllegalStateException()));
    }
}
