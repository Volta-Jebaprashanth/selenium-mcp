package com.voltajeba.selenium.mcp.tools;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides whether a {@link ToolGroup}-annotated tool class is registered, based on the
 * {@code selenium.mcp.tool-groups} / {@code selenium.mcp.exclude-tool-groups} properties.
 */
class ToolGroupCondition implements Condition {

    static final String INCLUDE_PROPERTY = "selenium.mcp.tool-groups";
    static final String EXCLUDE_PROPERTY = "selenium.mcp.exclude-tool-groups";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ToolGroup.class.getName());
        if (attributes == null) {
            return true;
        }
        String group = normalize((String) attributes.get("value"));
        Set<String> include = parse(context.getEnvironment().getProperty(INCLUDE_PROPERTY, "all"));
        Set<String> exclude = parse(context.getEnvironment().getProperty(EXCLUDE_PROPERTY, ""));
        return isEnabled(group, include, exclude);
    }

    static boolean isEnabled(String group, Set<String> include, Set<String> exclude) {
        if (exclude.contains(group)) {
            return false;
        }
        return include.isEmpty() || include.contains("all") || include.contains(group);
    }

    static Set<String> parse(String value) {
        return Arrays.stream(value.split(","))
                .map(ToolGroupCondition::normalize)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase().replace("_", "-");
    }
}
