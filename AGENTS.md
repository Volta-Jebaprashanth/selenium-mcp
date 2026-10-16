# AGENTS.md

Instructions for AI coding agents working in this repository.

## What this project is

A Spring Boot MCP (Model Context Protocol) server that exposes Selenium browser automation to MCP clients over **stdio**. It is not a web app — there is no HTTP server, no REST API, no UI.

## Build & verify

```bash
mvn clean package        # compile + package the jar
mvn test                 # unit tests
mvn test -Dselenium.it=true   # also run ChromeIntegrationTest against a real headless Chrome
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

**Attaching over a remote-debugging port is Chrome/Edge only too.** `BrowserFactory.attach`/`launchDebuggable` return an `AttachedDriver` (driver + its `DriverService`). `BrowserSession.close()` disconnects such a session by stopping the service, never by quitting the driver, so a browser the user or a test is also using stays up. Keep that distinction if you touch session teardown.

## Conventions

- **Locator strategy strings** (`id`, `name`, `css`/`cssSelector`, `xpath`, `className`/`class`, `linkText`, `partialLinkText`, `tagName`/`tag`) are resolved in one place: `Locators.toBy()`. If you add a new strategy, add it there, not inline elsewhere.
- **Every `tools/*Tools` class except `BrowserTools` carries `@ToolGroup("<name>")`** so users can switch it off with `--selenium.mcp.tool-groups` / `--selenium.mcp.exclude-tool-groups`. Give a new tool class a group (an existing one, or a new name documented in the README's "Loading fewer tools" list).
- **Format exception text with `ErrorMessages.of(e)`**, not `e.getMessage()` — `WebDriverException.getMessage()` appends build/system/capabilities/session diagnostics that bury the actual cause.
- **Element tools that act on one element** should accept the optional `index`/`visibleOnly` parameters (descriptions in `ElementMatch`) and resolve them with `tools.locator(...)`, which builds a `FilteredBy` — don't hand-roll `findElements().get(n)`.
- **Prefer raw CDP via `HasCdp.executeCdpCommand`** for one-shot Chrome/Edge commands (see `ScreenshotTaker.captureFullPage`, `InitScriptManager`): it sends plain JSON and keeps working when Chrome outpaces Selenium's bundled `selenium-devtools-vNN` modules, unlike `HasDevTools`/`NetworkInterceptor`/`HasLogEvents`.
- **`@McpTool` methods return strings**, always — success and failure both. Catch exceptions in the tool wrapper and return a descriptive message; don't let exceptions propagate out of an `@McpTool` method.
- **Precondition checks** ("is a browser open?") belong in the `tools/` wrapper, mirroring the existing pattern in `BrowserTools` (`if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";`).
- **`BrowserTools` methods are `synchronized`** — there's one browser session per server process; keep new stateful tool methods synchronized too rather than introducing concurrent access to the driver.
- Favor collaborator objects wired through constructors (see `Tools`'s constructor) over static helpers or singletons.
- Javadoc is used sparingly on package/class-level docs to explain *why* a class exists and how it relates to its neighbors — follow that style rather than commenting individual methods.

## Hard constraints — do not break these

- **Never write to `System.out`/`System.err` or enable console logging.** The stdio transport uses stdout for the MCP protocol; any stray print statement or re-enabled console appender will corrupt it and break every client. Logging goes to the file appender configured in `application.properties` (`logging.file.name=mcp-server.log`) only.
- **Don't add a web server.** `spring.main.web-application-type=none` is required — this is a stdio-only process.
- **Don't change `spring.ai.mcp.server.stdio` or the transport config** without understanding this is what makes the process function as an MCP server at all.

## npm distribution

`npm/` packages the server for `npx -y @xpathy/selenium-mcp`, with no Java install needed: CI (`.github/workflows/ci.yml`, `bundle` and `publish-npm` jobs) jlinks a trimmed Java runtime per OS/CPU, packs it with the jar into `@xpathy/selenium-mcp-<platform>-<arch>`, and `npm/selenium-mcp/bin/selenium-mcp.js` launches the jar on whichever one npm installed. The same stdout rule applies to the launcher: it must never write to stdout (except for the terminal-only `--version`). The jlink module list is `JLINK_MODULES` in `ci.yml`; if a dependency starts needing another JDK module, the bundle job's smoke tests (`npm/scripts/smoke-test.js`) fail and the module goes there.

## Adding dependencies

Selenium version and WebDriverManager version are pinned explicitly in `pom.xml` (not managed via a BOM) — bump them there deliberately, not implicitly via a parent/BOM update. Selenium in particular needs to stay reasonably current: its bundled CDP (`selenium-devtools-vNN`) modules only cover a handful of Chrome major versions at a time, and `NetworkMonitor`/`ConsoleLogMonitor` fall back to a no-op CDP implementation (raising `DevToolsException`) against a released Chrome/Edge that's newer than what the pinned Selenium version supports. `jackson-databind` is declared explicitly (version resolved via the Spring Boot parent's dependency management) for JSON request/response handling in `NetworkTools`/`ApiTools` — it was previously only available as an incidental transitive dependency of `webdrivermanager`, which isn't something to build on.
