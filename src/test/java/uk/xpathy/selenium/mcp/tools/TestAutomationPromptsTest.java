package uk.xpathy.selenium.mcp.tools;

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.provider.prompt.SyncMcpPromptProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestAutomationPromptsTest {

    private final Map<String, SyncPromptSpecification> specs =
            new SyncMcpPromptProvider(List.of(new TestAutomationPrompts())).getPromptSpecifications().stream()
                    .collect(Collectors.toMap(s -> s.prompt().name(), Function.identity()));

    @Test
    void registersEveryPromptWithItsRequiredArgument() {
        assertEquals(Set.of("generate-page-object", "write-test", "fix-broken-locator", "debug-failing-test",
                "fix-flaky-test", "review-page-object", "api-ui-test"), specs.keySet());

        Map<String, Boolean> args = specs.get("generate-page-object").prompt().arguments().stream()
                .collect(Collectors.toMap(PromptArgument::name, PromptArgument::required));
        assertEquals(Map.of("pageName", true, "url", false, "scope", false), args);
    }

    @Test
    void promptIsASingleUserMessageWithTheArgumentsFilledIn() {
        GetPromptResult result = get("debug-failing-test",
                Map.of("test", "CheckoutTest#placeOrder", "error", "NoSuchElementException: #pay"));

        assertEquals(1, result.messages().size());
        assertEquals(Role.USER, result.messages().get(0).role());
        String text = text(result);
        assertTrue(text.contains("`CheckoutTest#placeOrder` is failing"), text);
        assertTrue(text.contains("NoSuchElementException: #pay"), text);
        assertTrue(text.contains("launchDebugBrowser"), text);
        assertTrue(text.contains("Work inside the existing framework"), text);
    }

    @Test
    void optionalArgumentsCanBeLeftOut() {
        String text = text(get("generate-page-object", Map.of("pageName", "LoginPage")));

        assertTrue(text.contains("Create a Page Object named `LoginPage` for the page already open"), text);
        assertFalse(text.contains("null"), text);
        assertTrue(text.contains("Choosing stable Selenium locators"), text);
    }

    @Test
    void everyPromptRendersWithOnlyItsRequiredArguments() {
        specs.forEach((name, spec) -> {
            Map<String, Object> args = spec.prompt().arguments().stream()
                    .filter(PromptArgument::required)
                    .collect(Collectors.toMap(PromptArgument::name, a -> "value-of-" + a.name()));
            String text = text(spec.promptHandler().apply(null, GetPromptRequest.builder(name).arguments(args).build()));
            assertFalse(text.contains("null"), name);
            args.values().forEach(value -> assertTrue(text.contains((String) value), name));
        });
    }

    private GetPromptResult get(String name, Map<String, Object> args) {
        return specs.get(name).promptHandler().apply(null, GetPromptRequest.builder(name).arguments(args).build());
    }

    private static String text(GetPromptResult result) {
        return ((TextContent) result.messages().get(0).content()).text();
    }
}
