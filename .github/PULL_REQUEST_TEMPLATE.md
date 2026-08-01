## Summary

<!-- What does this PR change and why? -->

## Related issue

<!-- Closes #... -->

## Type of change

- [ ] Bug fix
- [ ] New tool / capability
- [ ] Documentation
- [ ] Refactor (no behavior change)
- [ ] Other (describe below)

## Checklist

- [ ] I read [AGENTS.md](../AGENTS.md) and kept the `tools/` / `webdriver/` / `http/` layering intact
- [ ] `mvn clean package` succeeds
- [ ] `mvn test` succeeds
- [ ] New/changed `@McpTool` methods return strings and don't let exceptions propagate
- [ ] README.md updated if a tool was added, removed, or changed behavior
- [ ] No writes to `System.out`/`System.err` and no console logging re-enabled (would corrupt the stdio MCP transport)

## How was this tested?

<!-- e.g. "Added unit test against webdriver.Tools", "Wired the jar into Claude Desktop and called X manually" -->
