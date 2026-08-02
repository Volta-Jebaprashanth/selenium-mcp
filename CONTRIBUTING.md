# Contributing to selenium-mcp

Thanks for your interest in improving selenium-mcp. This is an [MCP](https://modelcontextprotocol.io) server that exposes Selenium browser automation to AI agents, built with Spring Boot and Spring AI's MCP server starter. Before opening a PR, please read this guide — it'll save you a review round-trip.

Full docs, the interactive tool reference, and architecture deep-dive live at [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/).

## Code of Conduct

This project follows a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you're expected to uphold it.

## Prerequisites

- Java 21+ (`JAVA_HOME` pointing at a JDK 21 install)
- Maven 3.9+ (no wrapper is committed)
- Chrome, Firefox, or Edge installed locally for manual verification

## Getting started

```bash
git clone https://github.com/Volta-Jebaprashanth/selenium-mcp.git
cd selenium-mcp
mvn clean package
```

The server speaks the MCP stdio protocol — it isn't a REPL you can run and type at. To verify a change end-to-end, either:

- write a unit/integration test against `uk.xpathy.selenium.mcp.webdriver.Tools` directly (it has no Spring/MCP dependency and can be instantiated plainly), or
- wire the built jar into an actual MCP client (Claude Desktop, Claude Code, Cursor, etc.) and exercise the tool by hand.

## Before you start coding

**Read [AGENTS.md](AGENTS.md).** It documents the architecture (the `tools/` / `webdriver/` / `http/` layering), naming conventions, and a set of hard constraints (e.g. never write to `System.out`/`System.err`, since stdout carries the MCP protocol). Both human and AI contributors are expected to follow it — it's the source of truth for how this codebase is organized, not a duplicate of this file.

For anything beyond a small fix, please open an issue first to discuss the approach before investing time in an implementation.

## Making changes

- **Keep the three layers separate**: `tools/` (MCP-facing, `@McpTool` methods, precondition checks, exception-to-string translation), `webdriver/` (plain-Java Selenium logic, no Spring/MCP types), `http/` (standalone REST client). See AGENTS.md for the full breakdown and where new capabilities should live.
- **`@McpTool` methods always return strings** — catch exceptions in the tool wrapper and return a descriptive message rather than letting them propagate.
- **Locator strategy strings** are resolved in one place, `Locators.toBy()`. Add new strategies there, not inline elsewhere.
- Favor constructor-injected collaborator objects over static helpers or singletons, consistent with the existing `Tools` class.
- Keep Javadoc sparse and purposeful — explain *why* a class exists and how it relates to its neighbors, not what each method obviously does.

## Testing

```bash
mvn test
```

Add tests under `src/test/java`, mirroring the package structure of the code under test. Prefer testing `webdriver/` and `http/` classes directly, since they have no Spring/MCP dependency and are the easiest layer to unit test in isolation.

## Commit messages & PRs

- Write commit messages that explain *why*, not just what changed.
- Keep PRs focused — one logical change per PR is easier to review than a bundle of unrelated fixes.
- Reference any related issue in the PR description.
- Make sure `mvn clean package` and `mvn test` succeed before requesting review.
- Update [README.md](README.md) if you add, remove, or change the behavior of a tool.

## Reporting bugs

Open a [GitHub issue](https://github.com/Volta-Jebaprashanth/selenium-mcp/issues) with:

- The browser and version you were driving (Chrome/Firefox/Edge)
- Your OS and Java version (`java -version`)
- Steps to reproduce, including the MCP tool calls involved
- What you expected vs. what happened, including any relevant excerpt from `mcp-server.log`

For security-relevant issues, see [SECURITY.md](SECURITY.md) instead of opening a public issue.

## Suggesting features

Open an issue describing the use case first. Since the tool surface maps closely to Selenium's API, new tools should generally correspond to real Selenium/WebDriver or CDP capabilities rather than higher-level abstractions — see AGENTS.md's architecture notes for why.

## License

By contributing, you agree that your contributions will be licensed under the project's [MIT License](LICENSE.md).
