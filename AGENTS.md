# AGENTS.md

Instructions for AI coding agents working in this repository.

## What this project is

A Spring Boot MCP (Model Context Protocol) server that exposes Selenium browser automation to MCP clients over **stdio**. It is not a web app — there is no HTTP server, no REST API, no UI.

## Build & verify

```bash
mvn clean package        # compile + package the jar
mvn test                 # run tests (none exist yet — add them under src/test/java)
```

There is no Maven wrapper (`mvnw`) committed; use a locally installed Maven 3.9+ with `JAVA_HOME` set to a JDK 21.

The server can't be smoke-tested by just running the jar and typing at it — it speaks the MCP stdio protocol, not a human-friendly REPL. To verify a change end-to-end, either write a unit/integration test against `uk.xpathy.selenium.mcp.webdriver.Tools` directly (it has no Spring/MCP dependency and can be instantiated plainly), or wire the jar into an actual MCP client.

## Architecture — three layers, keep them separate

```
tools/      @McpTool-annotated classes. MCP-facing only: parameter validation,
            "is a browser open" precondition checks, exception → user-readable
            string translation. No Selenium logic here.

webdriver/  Plain-Java Selenium layer. No Spring, no MCP annotations, no
            knowledge that it's being called from an MCP tool. Tools.java is
            the facade/entry point; BrowserFactory, BrowserSession, Locators,
            Navigator, and ElementInteractor are its collaborators, each with
            one responsibility.

http/       Plain-Java REST client (ApiClient), with no Selenium/Spring/MCP
            dependency and no relationship to the browser session at all.
            Backs tools/ApiTools.java for API automation that's independent
            of — and usable without — an open browser.
```

When adding a new browser capability:
1. Add the Selenium logic to `webdriver/` (usually as a method on `Tools`, delegating to the right collaborator — or a new collaborator if it's a distinct concern).
2. Add a thin `@McpTool` wrapper method in `tools/BrowserTools.java` (or a new `tools/*Tools.java` class if it's a different domain) that checks preconditions and translates exceptions to strings.

When adding a new capability that has nothing to do with the browser (e.g. another kind of outbound client), give it its own top-level package mirroring `http/` rather than bolting it onto `webdriver/`.

Don't put Selenium calls directly in the `tools/` layer, and don't put MCP/Spring types in `webdriver/` or `http/`. `webdriver/Tools` is meant to be usable standalone for hand-written or generated Selenium tests, independent of the MCP server; `http/ApiClient` is meant to be usable standalone too, independent of both Selenium and the MCP server.

**CDP-backed features are Chrome/Edge only.** `webdriver/NetworkMonitor` (network capture/mocking/blocking/conditions, HTTP Basic auth) and `webdriver/ConsoleLogMonitor` (console log capture) are built on Selenium 4's Chrome DevTools Protocol support (`HasDevTools`/`NetworkInterceptor`/`HasLogEvents`), which Firefox doesn't implement. Their methods throw `UnsupportedOperationException` when the driver doesn't support it; the `tools/NetworkTools` wrappers catch that specifically and return an explanatory string rather than letting it read as a generic failure.

## Conventions

- **Locator strategy strings** (`id`, `name`, `css`/`cssSelector`, `xpath`, `className`/`class`, `linkText`, `partialLinkText`, `tagName`/`tag`) are resolved in one place: `Locators.toBy()`. If you add a new strategy, add it there, not inline elsewhere.
- **`@McpTool` methods return strings**, always — success and failure both. Catch exceptions in the tool wrapper and return a descriptive message; don't let exceptions propagate out of an `@McpTool` method.
- **Precondition checks** ("is a browser open?") belong in the `tools/` wrapper, mirroring the existing pattern in `BrowserTools` (`if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";`).
- **`BrowserTools` methods are `synchronized`** — there's one browser session per server process; keep new stateful tool methods synchronized too rather than introducing concurrent access to the driver.
- Favor collaborator objects wired through constructors (see `Tools`'s constructor) over static helpers or singletons.
- Javadoc is used sparingly on package/class-level docs to explain *why* a class exists and how it relates to its neighbors — follow that style rather than commenting individual methods.

## Hard constraints — do not break these

- **Never write to `System.out`/`System.err` or enable console logging.** The stdio transport uses stdout for the MCP protocol; any stray print statement or re-enabled console appender will corrupt it and break every client. Logging goes to the file appender configured in `application.properties` (`logging.file.name=mcp-server.log`) only.
- **Don't add a web server.** `spring.main.web-application-type=none` is required — this is a stdio-only process.
- **Don't change `spring.ai.mcp.server.stdio` or the transport config** without understanding this is what makes the process function as an MCP server at all.

## Adding dependencies

Selenium version and WebDriverManager version are pinned explicitly in `pom.xml` (not managed via a BOM) — bump them there deliberately, not implicitly via a parent/BOM update. Selenium in particular needs to stay reasonably current: its bundled CDP (`selenium-devtools-vNN`) modules only cover a handful of Chrome major versions at a time, and `NetworkMonitor`/`ConsoleLogMonitor` fall back to a no-op CDP implementation (raising `DevToolsException`) against a released Chrome/Edge that's newer than what the pinned Selenium version supports. `jackson-databind` is declared explicitly (version resolved via the Spring Boot parent's dependency management) for JSON request/response handling in `NetworkTools`/`ApiTools` — it was previously only available as an incidental transitive dependency of `webdrivermanager`, which isn't something to build on.
