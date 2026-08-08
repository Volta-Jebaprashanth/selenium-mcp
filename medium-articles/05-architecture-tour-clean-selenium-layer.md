# I Didn't Want an AI Tool That Pollutes My Test Codebase. Here's How the Architecture Prevents That.

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server built for test automation engineers working in **Java, Selenium, and REST Assured**. It gives an AI coding assistant a real Selenium WebDriver session to drive Chrome, Firefox, or Edge directly — structured element discovery for reliable locators, Chrome DevTools Protocol network/console tooling, and a standalone REST client, all in one server. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

A fair worry, if you're a test automation engineer looking at any AI-adjacent tool: does adopting this mean my codebase now has some framework's fingerprints all over it? Half the "AI-powered testing" tools out there generate code that's obviously generated — proprietary annotations, a runtime dependency on the vendor's SDK, patterns your team wouldn't have chosen on their own. You end up maintaining the AI tool's opinions as much as your actual tests.

That was a hard constraint for me going into **selenium-mcp**: whatever code an agent writes using this should look exactly like Selenium code a senior engineer on your team would have written by hand. No trace of "an MCP server helped write this." Here's how the architecture makes that true, not just a claim in a README.

## Three layers, deliberately kept apart

```
tools/      @McpTool-annotated classes. MCP-facing only.
webdriver/  Plain-Java Selenium layer. No Spring, no MCP annotations.
http/       Plain-Java REST client. No Selenium/Spring/MCP dependency at all.
```

The `tools/` package is the only place that knows this is an MCP server at all — parameter validation, "is a browser open" precondition checks, translating exceptions into readable strings. Genuinely thin.

`webdriver/` is where the real work happens, and it has zero knowledge of MCP, Spring, or an AI agent existing. `Tools.java` is the facade; `BrowserFactory`, `BrowserSession`, `Locators`, `Navigator`, `ElementInteractor`, and the rest are single-responsibility collaborators wired through constructors. This is exactly the shape a hand-written Selenium utility layer would take if you built it from scratch with clean separation of concerns.

`http/` mirrors that for the REST client (`ApiClient`) — standalone, no relationship to the browser session or the MCP layer at all.

## Why this matters beyond "clean code"

It's not an aesthetic preference. It has a concrete consequence: **`webdriver/Tools` is usable directly, with no MCP server running at all**, as a standalone Selenium utility layer for hand-writing or generating tests. Same for `http/ApiClient` on the API side. The AI-assisted layer sits *on top of* a plain Java Selenium library — it isn't a separate thing that happens to also produce Selenium-shaped output. When an agent uses `selenium-mcp` to explore a page and then writes a `LoginPage` class for your suite, the Java it writes calls the same kind of APIs — `driver.findElement(By.cssSelector(...))`, explicit `WebDriverWait`s — that you'd write yourself, because under the hood, that's genuinely what's running.

That's the difference between "AI tool that happens to output Java" and "Selenium library an AI agent happens to be using." The second one is what you actually want in a codebase your whole team maintains, agent-assisted or not.

## The rule that keeps it that way

There's a hard line documented in the project's [AGENTS.md](https://github.com/Volta-Jebaprashanth/selenium-mcp/blob/main/AGENTS.md), the file that governs how AI coding agents are allowed to modify the project itself: no Selenium calls in `tools/`, no MCP/Spring types in `webdriver/` or `http/`. It's enforced by convention and code review, not a compiler — but it's the single rule that keeps the Selenium layer honest as a standalone library instead of slowly growing MCP-specific assumptions into itself over time.

There's a second convention worth calling out for anyone auditing this before adopting it: **`@McpTool` methods always return strings, success or failure** — exceptions are caught in the tool wrapper and turned into a descriptive message rather than propagating out. That's a deliberate MCP-protocol-compatibility choice at the `tools/` boundary, and it stays exactly at that boundary — it doesn't leak into how `webdriver/Tools` itself is written, which throws normal Java exceptions like any other library.

## CDP support gets the same treatment

One more example, because it's a real edge case: `NetworkMonitor` and `ConsoleLogMonitor` are built on Selenium 4's Chrome DevTools Protocol support, which Firefox doesn't implement. Their methods throw `UnsupportedOperationException` on an unsupported driver — that's plain Java behavior, no MCP awareness. It's only the `tools/NetworkTools` wrapper that catches that specific exception and turns it into an explanatory string for the agent. The distinction matters: the capability boundary is enforced in the Selenium layer, where it belongs; the "make this readable for an AI client" translation happens only at the very top.

## What this means if you're evaluating this for your team

If your team is cautious about adopting AI tooling into a shared test codebase — a completely reasonable position — the thing to check isn't "does it generate working tests," it's "what does the generated code actually depend on." Here, the answer is: your test files depend on plain Selenium and a plain REST client, same as if you'd written them by hand. The MCP server is a development-time tool, not a runtime dependency your suite inherits. You can stop using `selenium-mcp` entirely tomorrow and every test it helped write keeps running exactly as it does today.

Full project structure is documented in the [README](https://github.com/Volta-Jebaprashanth/selenium-mcp#project-structure) and [AGENTS.md](https://github.com/Volta-Jebaprashanth/selenium-mcp/blob/main/AGENTS.md) if you want to see the layering rules in full before pointing an agent at your repo.
