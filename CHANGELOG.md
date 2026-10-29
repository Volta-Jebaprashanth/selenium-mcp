# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
once it reaches a 1.0.0 release.

## [Unreleased]

### Added

- Session wait strategy for generated tests: `setAutoWait` / `getAutoWait`, and `implicitWaitSeconds` / `networkIdleMillis` / `animationIdleMillis` on `openBrowser`, `connectToBrowser` and `launchDebugBrowser`. Sessions still default to plain Selenium, and the server recommends `networkIdleMillis=500` / `animationIdleMillis=300` to the agent. When set, every action waits afterwards for fetch/XHR requests and then animations to settle, using an in-page tracker. Each action reports what it set off and which wait the generated test needs there, and failures a missing wait causes get a hint.
- `getWaitReport`: the steps whose generated code needs a wait. `closeBrowser` prints it before discarding it.
- `getWaitCodeSnippets`: usage rules, setup lines and a `Waits` helper class (Java 11+, Selenium 4) that runs the same page checks as the session.
- Server instructions sent to MCP clients on connect, describing the wait workflow.
- `text` and `partialText` locator strategies: find an element by the words on screen, e.g. `text=Create Put`, without writing XPath.
- `isClickable` tool, and `visible`/`enabled`/`clickable` flags (plus `coveredBy`) on interactive elements in `getPageElements` and on every match in `getPageElementsFiltered`. Clickable means displayed, enabled and not covered by another element such as a loading overlay.
- `click` retries while another element intercepts the click, up to `waitIfCoveredSeconds` (default 5).
- `index`/`visibleOnly` on every remaining single-element tool (element state and geometry queries, waits, select, `uploadFile`, `switchToFrame`, `clickAndHold`, `dragAndDropByOffset`), and `visibleOnly` on the tools that act on every match.
- Multiple browser sessions: `openBrowser`, `connectToBrowser` and `launchDebugBrowser` take an optional `session` name to run several browsers at once (up to `--selenium.mcp.max-sessions`, default 5); new `listSessions` and `switchSession` tools; `closeBrowser` takes `session` and `all`. Every tool acts on the active session, and while several are open, status messages and every tool's failures start with `[session]` so they say which browser they came from.
- npm distribution with a bundled Java runtime: `npx -y @xpathy/selenium-mcp`, no JDK install needed (Windows x64, Linux x64, macOS Intel/Apple Silicon). See the README's Quick start. The launcher repairs an npx cache that's missing its platform package, and releases are published via npm trusted publishing.
- The npm package is also published as `@voltajeba/selenium-mcp`, an alias that installs and runs `@xpathy/selenium-mcp` of the same version.
- `takeScreenshot` `fullPage` option for capturing the whole scrollable page (Chrome/Edge via raw CDP, Firefox natively).
- `executeScript` `timeoutMs` option to override the 30-second script timeout per call.
- `addInitScript` / `removeInitScript` tools: JavaScript that runs on every page load in the current tab (Chrome/Edge).
- Optional `index` and `visibleOnly` parameters on `click`, `sendKeys`, `getText`, `getAttribute`, `hover`, `doubleClick`, `rightClick`, `scrollIntoView`, and `takeElementScreenshot` to target the nth (visible) match.
- Tool groups: `--selenium.mcp.tool-groups` / `--selenium.mcp.exclude-tool-groups` launch arguments to load fewer tools.
- Opt-in headless Chrome integration test (`mvn test -Dselenium.it=true`).

### Changed

- `takeScreenshot` and `takeElementScreenshot` return the image as MCP image content instead of a base64 string, so clients can show it to the model directly. Images too large to send inline ask for `filePath` instead.
- `waitForClickable` also waits until no other element covers the target, rather than stopping at visible + enabled.
- `click` reports when its target was covered at the moment it was attempted, even when the driver itself held the click until the cover went (newer chromedriver versions do), since the generated test has to wait for it too.
- Browsers are closed (attached ones disconnected) when the server shuts down, instead of being left running. On Windows the npm launcher now runs the server detached and feeds it stdin, so a client killing the launcher no longer kills the server mid-flight and orphans chromedriver and browser processes.
- A browser that was closed by hand or crashed no longer stays "open": tool errors say the browser is gone and what to do, and `openBrowser`/`connectToBrowser`/`launchDebugBrowser`/`listSessions`/`switchSession` drop the dead session.
- CI runs the headless Chrome integration tests on every build.
- `navigate`, `back`, `forward`, `refresh`, `getCurrentUrl`, `getTitle` and `getPageSource` return an error message instead of throwing when the browser fails.
- Tool error messages are now the driver's own message only, without Selenium's build/system/capabilities/session diagnostics.
- `executeScript` returns objects and arrays as JSON instead of Java `toString()` output.
- `takeScreenshot` description now states it captures the viewport by default (it never captured the full page).
- Selenium bumped to 4.51.0 (CDP for Chrome 153–155). Console and network capture failed on Chrome 155 with 4.46.0 (`no-op implementation of the CDP`).

- MCP server exposing Selenium browser automation (Chrome/Firefox/Edge) over stdio, built with Spring Boot and Spring AI's MCP server starter.
- Browser lifecycle & navigation, element queries, explicit waits, alerts, frames, windows/tabs, cookies, screenshots, JS execution & scrolling, mouse/keyboard actions, dropdown handling, and file upload/PDF printing tools.
- Chrome DevTools Protocol (CDP) integration for network capture, request mocking/blocking, simulated network conditions, and console log capture (Chrome/Edge only).
- `PageSourceTools`: token-efficient JSON alternatives to the raw HTML page source — `getPageScripts`, `getPageStyles`, `getPageElements` (full page as a compact element tree), and `getPageElementsFiltered` (find every element matching a locator at once, with optional ancestor/sibling/descendant context and `siblingIndex`/`siblingCount` for building a locator that's guaranteed unique).
- Standalone REST client (`ApiTools`) for API automation independent of the browser session.
- Project documentation: [README.md](README.md), [AGENTS.md](AGENTS.md).
- Open-source project scaffolding: [LICENSE.md](LICENSE.md), [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), [SECURITY.md](SECURITY.md).

[Unreleased]: https://github.com/Volta-Jebaprashanth/selenium-mcp/commits/main
