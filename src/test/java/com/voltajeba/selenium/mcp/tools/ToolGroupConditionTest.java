package com.voltajeba.selenium.mcp.tools;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolGroupConditionTest {

    @Test
    void allGroupsAreEnabledByDefault() {
        assertTrue(ToolGroupCondition.isEnabled("network", Set.of("all"), Set.of()));
    }

    @Test
    void includeListRestrictsGroups() {
        Set<String> include = ToolGroupCondition.parse("elements, wait");
        assertTrue(ToolGroupCondition.isEnabled("wait", include, Set.of()));
        assertFalse(ToolGroupCondition.isEnabled("network", include, Set.of()));
    }

    @Test
    void excludeWinsOverInclude() {
        assertFalse(ToolGroupCondition.isEnabled("api", Set.of("all"), ToolGroupCondition.parse("api,network")));
    }

    @Test
    void parsingIsCaseAndSeparatorInsensitive() {
        assertEquals(Set.of("page-source", "api"), ToolGroupCondition.parse(" Page_Source ,API,, "));
    }
}
