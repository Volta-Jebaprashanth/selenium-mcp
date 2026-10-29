package com.voltajeba.selenium.mcp.tools;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Puts an {@code @McpTool} class in a named group that can be switched off, so clients that don't
 * need every capability can load a smaller tool list (each tool's schema costs the client tokens).
 * Selection is driven by two properties, typically passed as launch arguments:
 * <ul>
 *   <li>{@code selenium.mcp.tool-groups} - comma-separated groups to enable; defaults to {@code all}</li>
 *   <li>{@code selenium.mcp.exclude-tool-groups} - comma-separated groups to disable</li>
 * </ul>
 * e.g. {@code java -jar selenium-mcp.jar --selenium.mcp.exclude-tool-groups=api,network}.
 * {@link BrowserTools} carries no group and is always registered. See {@link ToolGroupCondition}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Conditional(ToolGroupCondition.class)
public @interface ToolGroup {

    String value();
}
