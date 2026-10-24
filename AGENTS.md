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
            string translation. No Selenium logic here. BrowserSessions holds
            the open browser sessions (one webdriver/Tools each).

webdriver/  Plain-Java Selenium layer. No Spring, no MCP annotations, no
            knowledge that it's being called from an MCP tool. Tools.java is
            the facade/entry point; BrowserFactory, BrowserSession, Locators,
            and ~20 single-purpose collaborators (Navigator, ElementInteractor,
            WaitHelper, NetworkMonitor, PageSourceInspector, ...) sit behind it.

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

**Browser sessions live in `tools/BrowserSessions`.** It holds one `webdriver/Tools` per named session and tracks the active one; every `@McpTool` acts on `sessions.active()`, so tool signatures carry no session parameter. Only `BrowserTools`' open/connect/launch tools create sessions (via `sessions.open(name, opener)`, which registers nothing if opening throws), up to `--selenium.mcp.max-sessions` (default 5); `listSessions`/`switchSession`/`closeBrowser` manage them. Sessions whose browser was closed by hand or crashed are dropped by `pruneDead()`, and every session is closed on shutdown (`@PreDestroy closeAll()`). `webdriver/` stays single-browser: one `Tools` is one browser.

**Attaching over a remote-debugging port is Chrome/Edge only too.** `BrowserFactory.attach`/`launchDebuggable` return an `AttachedDriver` (driver + its `DriverService`). `BrowserSession.close()` disconnects such a session by stopping the service, never by quitting the driver, so a browser the user or a test is also using stays up. Keep that distinction if you touch session teardown.

## Conventions

- **Locator strategy strings** (`id`, `name`, `css`/`cssSelector`, `xpath`, `className`/`class`, `linkText`, `partialLinkText`, `tagName`/`tag`, `text`, `partialText`) are resolved in one place: `Locators.toBy()`. If you add a new strategy, add it there, not inline elsewhere.
- **Every `tools/*Tools` class except `BrowserTools` carries `@ToolGroup("<name>")`** so users can switch it off with `--selenium.mcp.tool-groups` / `--selenium.mcp.exclude-tool-groups`. Give a new tool class a group (an existing one, or a new name documented in the README's "Loading fewer tools" list).
- **Format exception text with `ErrorMessages.of(e)`**, not `e.getMessage()` — `WebDriverException.getMessage()` appends build/system/capabilities/session diagnostics that bury the actual cause.
- **Element tools that act on one element** should accept the optional `index`/`visibleOnly` parameters (descriptions in `ElementMatch`) and resolve them with `tools.locator(...)`, which builds a `FilteredBy` — don't hand-roll `findElements().get(n)`.
- **Prefer raw CDP via `HasCdp.executeCdpCommand`** for one-shot Chrome/Edge commands (see `ScreenshotTaker.captureFullPage`, `InitScriptManager`): it sends plain JSON and keeps working when Chrome outpaces Selenium's bundled `selenium-devtools-vNN` modules, unlike `HasDevTools`/`NetworkInterceptor`/`HasLogEvents`.
- **`@McpTool` methods return strings** — success and failure both. Catch exceptions in the tool wrapper and return a descriptive message; don't let exceptions propagate out of an `@McpTool` method. The one exception is `ScreenshotTools`, which returns a `CallToolResult` so the screenshot reaches the client as image content; its messages are still text, with failures flagged `isError`. Only return a `CallToolResult` when the result can't be text.
- **Clickable means not covered.** `ClickabilityChecker` (displayed + enabled + nothing on top at the element's centre) is the definition used by `waitForClickable`, `isClickable` and the `getPageElements*` flags. Use it rather than Selenium's `elementToBeClickable`, which passes for a button under a loading overlay.
- **Precondition checks** ("is a browser open?") belong in the `tools/` wrapper, mirroring the existing pattern (`Tools tools = sessions.active();` then `if (!tools.isBrowserOpen()) return "No browser is open. Call openBrowser first.";`). With no session open, `active()` returns a `Tools` with no browser, so the check works unchanged.
- **Tool methods run inside `synchronized (sessions)`** and read `sessions.active()` inside that block. This serializes every tool call across all sessions and keeps the active session from changing mid-call; keep new stateful tool methods to the same pattern rather than introducing concurrent access to a driver.
- **Prefix failure messages with `sessions.label()`** (`return sessions.label() + "Failed to add cookie: " + ErrorMessages.of(e);`). It's `[name] ` while more than one session is open and empty otherwise, so errors say which browser they came from.
- Favor collaborator objects wired through constructors (see `Tools`'s constructor) over static helpers or singletons.
- Javadoc is used sparingly on package/class-level docs to explain *why* a class exists and how it relates to its neighbors — follow that style rather than commenting individual methods.

## Hard constraints — do not break these

- **Never write to `System.out`/`System.err` or enable console logging.** The stdio transport uses stdout for the MCP protocol; any stray print statement or re-enabled console appender will corrupt it and break every client. Logging goes to the file appender configured in `application.properties` (`logging.file.name=mcp-server.log`) only.
- **Don't add a web server.** `spring.main.web-application-type=none` is required — this is a stdio-only process.
- **Don't change `spring.ai.mcp.server.stdio` or the transport config** without understanding this is what makes the process function as an MCP server at all.

## npm distribution

`npm/` packages the server for `npx -y @xpathy/selenium-mcp`, with no Java install needed: CI (`.github/workflows/ci.yml`, `bundle` and `publish-npm` jobs) jlinks a trimmed Java runtime per OS/CPU, packs it with the jar into `@xpathy/selenium-mcp-<platform>-<arch>`, and `npm/selenium-mcp/bin/selenium-mcp.js` launches the jar on whichever one npm installed. The same stdout rule applies to the launcher: it must never write to stdout (except for the terminal-only `--version`). On Windows the launcher spawns the JVM `detached` and pipes stdin to it rather than inheriting it: Node otherwise kills the JVM outright together with the launcher, skipping the shutdown that closes browsers. Keep that if you touch the spawn. The jlink module list is `JLINK_MODULES` in `ci.yml`; if a dependency starts needing another JDK module, the bundle job's smoke tests (`npm/scripts/smoke-test.js`) fail and the module goes there.

`publish-npm` authenticates with npm trusted publishing (GitHub OIDC), so every `@xpathy` package must list `ci.yml` as its trusted publisher on npmjs.com — a new platform package needs a token-based first publish before that can be configured. It waits until the registry serves each platform version before publishing the launcher; keep that ordering. If a user's npx cache still ends up without the platform package, the launcher installs it into that cache itself (`repairNpxInstall`).

## Versioning

The version has one source: `<revision>` in `pom.xml` (e.g. `1.0-SNAPSHOT`). CI's `build` job turns it into the release version `<revision without -SNAPSHOT>.<run number>` and builds with `-Drevision=<that>`, so the jar, the version the server reports to MCP clients (`spring.ai.mcp.server.version=@project.version@`, filtered at build time), the GitHub release tag and the npm packages all carry the same number. Don't hard-code a version anywhere else; `npm/selenium-mcp/package.json` stays at `0.0.0` and is stamped by `npm/scripts/set-version.js` at publish time.

## Adding dependencies

Selenium version and WebDriverManager version are pinned explicitly in `pom.xml` (not managed via a BOM) — bump them there deliberately, not implicitly via a parent/BOM update. Selenium in particular needs to stay reasonably current: its bundled CDP (`selenium-devtools-vNN`) modules only cover a handful of Chrome major versions at a time, and `NetworkMonitor`/`ConsoleLogMonitor` fall back to a no-op CDP implementation (raising `DevToolsException`) against a released Chrome/Edge that's newer than what the pinned Selenium version supports. `jackson-databind` is declared explicitly (version resolved via the Spring Boot parent's dependency management) for JSON request/response handling in `NetworkTools`/`ApiTools` — it was previously only available as an incidental transitive dependency of `webdrivermanager`, which isn't something to build on.
