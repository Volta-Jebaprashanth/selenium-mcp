# selenium-mcp

### The Java-based Selenium MCP server built for test automation engineers, with Playwright-MCP-style smart element discovery

[![CI](https://github.com/Volta-Jebaprashanth/selenium-mcp/actions/workflows/ci.yml/badge.svg)](https://github.com/Volta-Jebaprashanth/selenium-mcp/actions/workflows/ci.yml)
[![Latest release](https://img.shields.io/github/v/release/Volta-Jebaprashanth/selenium-mcp)](https://github.com/Volta-Jebaprashanth/selenium-mcp/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.md)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://adoptium.net/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Website](https://img.shields.io/badge/docs-selenium--mcp.xpathy.uk-blue.svg)](https://selenium-mcp.xpathy.uk/)

**[📖 Full docs, interactive tool reference & quickstart →](https://selenium-mcp.xpathy.uk/)**

A [Model Context Protocol](https://modelcontextprotocol.io) server, built in Java with Spring Boot and Spring AI's MCP server starter, for **test automation engineers working in Java, Selenium, and REST Assured**. It lets AI coding assistants (Claude Code, Cursor, Claude Desktop, etc.) drive a real Chrome, Firefox, or Edge browser through Selenium WebDriver while you write and maintain your test suite — and, unlike every other Selenium MCP server, gives them a *structured* way to find elements instead of forcing them to parse raw HTML.

## The problem this solves

Every test automation engineer who's tried pairing an AI coding assistant with a Selenium suite hits the same wall: the assistant can't see the page, so it either dumps the entire raw HTML page source into its context just to find one element and guesses at a locator — hitting `element not unique, multiple matches found` — or you end up opening DevTools yourself and pasting the selector back into the chat. It's slow, it burns context, and it's flaky, and it turns "AI-assisted test automation" into you doing the automation part and the AI doing the typing.

[Playwright MCP](https://github.com/microsoft/playwright-mcp) solved this on the Node/Playwright side with structured accessibility-tree snapshots instead of raw DOM dumps. **`selenium-mcp` brings that same capability to the Java/Selenium/REST Assured test automation ecosystem — as far as we're aware, it's the only Selenium-based MCP server that does, and the only one built specifically for test automation engineers rather than general browser-automation agents.**

`PageSourceTools` gives an agent focused, JSON-shaped views of the page instead of raw markup — scripts only, styles only, a compact element tree — and, most powerfully, `getPageElementsFiltered`: it finds *every* element matching a locator in one call and hands back the exact ancestor chain and sibling position (`div.container > form#login > input:nth-of-type(2)`) needed to write a locator that's unique on the first try, no more trial-and-error against the live page. See [Page inspection](#page-inspection-pagesourcetools--the-flagship-feature) below.

| | selenium-mcp | Other Selenium MCP servers | Playwright MCP |
|---|---|---|---|
| Language / stack | Java (Spring Boot) | mostly Node.js / Python | Node.js |
| Browser engine | Selenium WebDriver (Chrome, Firefox, Edge) | Selenium WebDriver | Playwright (Chromium, Firefox, WebKit) |
| Structured element discovery (no raw HTML dump) | ✅ filtered queries with ancestor/sibling context | ❌ raw `getPageSource` only | ✅ accessibility-tree snapshot |
| CDP network capture, mocking, blocking, console logs | ✅ | rare | ✅ |
| Standalone REST/API testing tools | ✅ | ❌ | ❌ |

## What it does

Beyond element discovery, it's a full Selenium automation surface for writing and maintaining a real test suite: open a browser, navigate, read the page source, click elements, type into fields — all via standard Selenium locators (`id`, `name`, `css`, `xpath`, `className`, `linkText`, `partialLinkText`, `tagName`).

It also exposes Selenium 4's Chrome DevTools Protocol (CDP) capabilities — passive network capture, request mocking/blocking, simulated network conditions, and browser console log capture, useful for diagnosing flaky waits — plus a standalone REST client for API automation, so a test automation engineer can get AI help combining REST Assured-style API checks with Selenium UI flows in one test, in one workflow. See [Network capture, mocking & console logs](#network-capture-mocking--console-logs-networktools--chromeedge-only) and [REST API automation](#rest-api-automation-apitools) below.

## Prerequisites

- Java 21+ (`JAVA_HOME` pointing at a JDK 21 install)
- Maven 3.9+
- Chrome, Firefox, or Edge installed locally (drivers are fetched automatically by [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) on first use). Network capture/mocking/blocking and console log capture require Chrome or Edge — they're backed by the Chrome DevTools Protocol, which Firefox doesn't expose.

## Get the jar

Every push to `main` builds and publishes a new [GitHub Release](https://github.com/Volta-Jebaprashanth/selenium-mcp/releases) with the jar attached — no need to build from source. Grab the latest one directly:

**[⬇ Download selenium-mcp.jar (latest release)](https://github.com/Volta-Jebaprashanth/selenium-mcp/releases/latest/download/selenium-mcp.jar)**

Or build it yourself:

```bash
mvn clean package
```

## Run

The server communicates over stdio, so it's meant to be launched by an MCP client rather than run standalone in a terminal.

```bash
java -jar selenium-mcp.jar
```

### Registering with an MCP client

Download the jar (see [Get the jar](#get-the-jar) above), then point your MCP client at it — adjust the path to wherever you saved it:

```json
{
  "mcpServers": {
    "selenium-mcp": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/selenium-mcp.jar"]
    }
  }
}
```

## Available tools

For the full searchable tool surface (112 tools across 16 categories) with parameters and examples, see the [MCP Tools Reference](https://selenium-mcp.xpathy.uk/docs/tools) on the docs site. Summary by category below.

Locator strategies accepted throughout: `id`, `name`, `css`/`cssSelector`, `xpath`, `className`/`class`, `linkText`, `partialLinkText`, `tagName`/`tag`.

### Browser lifecycle & navigation (`BrowserTools`)

| Tool | Description |
|---|---|
| `openBrowser` | Opens a browser window (`chrome`, `firefox`, or `edge`; defaults to `chrome`), with optional `headless`, `incognito`, `windowSize` (e.g. `1920x1080`), and `userAgent`. Reuses an already-open browser. |
| `closeBrowser` | Closes the open browser and releases the driver. |
| `navigate` | Navigates the open browser to a URL. |
| `back` / `forward` / `refresh` | Browser history navigation. |
| `getCurrentUrl` | Returns the URL of the current page. |
| `getTitle` | Returns the title of the current page. |
| `getPageSource` | Returns the full HTML of the current page. |
| `click` | Clicks an element, located by strategy and value. |
| `sendKeys` | Types text into an element, with an option to clear existing content first. |

### Page inspection (`PageSourceTools`) — the flagship feature

Token-efficient, JSON-shaped alternatives to the raw HTML from `getPageSource` — read only what you actually need instead of parsing the whole page, and never guess at a locator again. This is the capability described in [The problem this solves](#the-problem-this-solves) above, and what sets `selenium-mcp` apart from other Selenium MCP servers.

| Tool | Description |
|---|---|
| `getPageScripts` | Every `<script>` on the page as a JSON array — `src` for external scripts, `content` for inline ones (truncated over 10,000 characters). No markup, no styles, no noise. |
| `getPageStyles` | Every stylesheet as a JSON array — `href` for external `<link rel="stylesheet">`s, `content` for inline `<style>` tags (same truncation). |
| `getPageElements` | The entire page as a compact JSON tree rooted at `<body>` — tag, notable attributes, each element's own text, and children — with `<script>`/`<style>`/comments stripped out entirely. Far cheaper for an agent to read than raw HTML. Depth-capped via `maxDepth` (default 20). |
| `getPageElementsFiltered` | **The one to reach for once you know what you're looking for.** Finds *every* element matching a locator (not just the first) and returns each as JSON, with three optional context toggles: `includeAncestors` — the parent chain up to `<html>`, with `siblingIndex`/`siblingCount` on every node so you can build a locator that's actually unique (e.g. `div.container > form#login > input:nth-of-type(2)`) instead of hitting "multiple elements found"; `includeSiblings` — the other children of the same parent, e.g. a `<label>` right next to an `<input>`; `includeDescendants` — expand a match's own children, for when the locator targets a container rather than a leaf. Results are capped (`limit`, default 50) and the response always reports `totalMatches`/`returnedMatches`/`truncated`. |

Example: `locatorType=xpath`, `locatorValue=//input`, `includeAncestors=true` returns one entry per `<input>` on the page, each annotated with exactly the ancestor path and sibling position needed to disambiguate it from every other `<input>`.

### Element queries (`ElementQueryTools`) — read-only

| Tool | Description |
|---|---|
| `getText` | Visible text of an element. |
| `getAttribute` | Value of an HTML attribute/DOM property. |
| `getCssValue` | Computed CSS property value. |
| `getTagName` | HTML tag name. |
| `isDisplayed` / `isEnabled` / `isSelected` | Element state checks. |
| `getElementSize` / `getElementLocation` | Rendered dimensions / position. |
| `elementExists` | Whether a locator matches anything, without throwing. |
| `countElements` | Count of elements matching a locator. |
| `getElementsText` | Text of every element matching a locator. |

### Waits (`WaitTools`)

| Tool | Description |
|---|---|
| `waitForVisible` / `waitForClickable` / `waitForPresent` / `waitForInvisible` | Explicit waits on an element, with configurable timeout (default 10s). |
| `waitForTextPresent` | Waits until an element contains given text. |
| `waitForTitleContains` / `waitForUrlContains` | Waits on page title / URL. |
| `waitForPageLoad` | Waits until `document.readyState` is `"complete"`. |
| `waitForJsCondition` | Waits until a custom JavaScript expression evaluates truthy. |
| `waitForElementCount` | Waits until the number of elements matching a locator equals an expected count. |
| `waitForAttributeToBe` | Waits until an element's attribute/DOM property equals an expected value. |
| `waitForNumberOfWindowsToBe` | Waits until the number of open windows/tabs equals an expected count. |

For waiting on in-flight network requests to settle (e.g. after a click that triggers XHR/fetch calls), see `waitForNetworkIdle` under [Network Tools](#network-capture-mocking--console-logs-networktools--chromeedge-only) below.

### Alerts (`AlertTools`)

| Tool | Description |
|---|---|
| `acceptAlert` / `dismissAlert` | Accept or dismiss a JS alert/confirm/prompt. |
| `getAlertText` | Read the dialog's text. |
| `sendKeysToAlert` | Type into a prompt dialog. |
| `isAlertPresent` | Check whether a dialog is open. |

### Frames (`FrameTools`)

| Tool | Description |
|---|---|
| `switchToFrameByIndex` / `switchToFrameByNameOrId` / `switchToFrame` | Switch focus into a frame/iframe. |
| `switchToDefaultContent` | Return focus to the main document. |
| `switchToParentFrame` | Move focus up one frame level. |

### Windows & tabs (`WindowTools`)

| Tool | Description |
|---|---|
| `maximizeWindow` / `minimizeWindow` / `fullscreenWindow` | Window state. |
| `setWindowSize` / `getWindowSize` | Resize / read window size. |
| `setWindowPosition` | Move the window on screen. |
| `getWindowHandle` / `getWindowHandles` | Current / all window handles. |
| `switchToWindow` | Switch focus to a window/tab by handle. |
| `openNewTab` | Open and switch to a new tab. |
| `closeCurrentWindow` | Close the focused window/tab (without ending the driver session). |

### Cookies (`CookieTools`)

| Tool | Description |
|---|---|
| `addCookie` | Add a cookie, optionally with domain/path. |
| `getCookie` / `getAllCookies` | Read cookies. |
| `deleteCookie` / `deleteAllCookies` | Remove cookies. |

### Screenshots (`ScreenshotTools`)

| Tool | Description |
|---|---|
| `takeScreenshot` | Full-page screenshot — returns base64 PNG, or saves to a file path if given. |
| `takeElementScreenshot` | Screenshot of a single element. |

### JavaScript & scrolling (`ScriptTools`)

| Tool | Description |
|---|---|
| `executeScript` | Runs arbitrary JavaScript in the page context and returns the result. |
| `scrollIntoView` | Scrolls an element into view. |
| `scrollBy` | Scrolls by a pixel offset. |
| `scrollToTop` / `scrollToBottom` | Scrolls to the page extremes. |

### Mouse & keyboard actions (`ActionsTools`)

| Tool | Description |
|---|---|
| `hover` | Moves the mouse over an element. |
| `doubleClick` / `rightClick` | Double-click / context-click an element. |
| `clickAndHold` / `releaseClick` | Press-and-hold / release the mouse button. |
| `dragAndDrop` | Drags one element onto another. |
| `dragAndDropByOffset` | Drags an element by a pixel offset. |
| `pressKey` | Sends a single named key (e.g. `ENTER`, `TAB`, `ESCAPE`) to the focused element. |

### Dropdowns (`SelectTools`)

| Tool | Description |
|---|---|
| `selectByVisibleText` / `selectByValue` / `selectByIndex` | Select an option from a `<select>`. |
| `deselectAllOptions` | Clear selections in a multi-select. |
| `getSelectedOptionsText` | Read the currently selected option(s). |
| `isMultipleSelect` | Whether the dropdown allows multiple selections. |

### Files (`FileTools`)

| Tool | Description |
|---|---|
| `uploadFile` | Uploads a local file via an `<input type="file">` element. |
| `printToPdf` | Prints the current page to PDF — returns base64, or saves to a file path if given. |

### Network capture, mocking & console logs (`NetworkTools`) — Chrome/Edge only

Built on Selenium 4's Chrome DevTools Protocol (CDP) support (`HasDevTools`/`NetworkInterceptor`). Not available on Firefox — every tool below returns an explanatory message instead of throwing if called on an unsupported browser.

| Tool | Description |
|---|---|
| `startNetworkCapture` / `stopNetworkCapture` | Start/stop passively recording network traffic (method, URL, headers, bodies, status, timing), optionally filtered by a URL regex. |
| `getNetworkLog` | Read captured entries as JSON, optionally filtered by URL regex / method, most recent last. |
| `clearNetworkLog` | Clear captured entries without stopping capture. |
| `waitForNetworkIdle` | Wait until there have been no in-flight HTTP requests for a continuous idle period — the "wait until all requests complete" custom wait, handy in place of a fixed sleep after an action that triggers XHR/fetch calls. |
| `getPendingRequestCount` | Number of HTTP requests currently in flight. |
| `mockResponse` / `clearMockResponses` | Stub out responses for requests matching a URL regex (status, headers, body) without hitting the real network — e.g. to fake a backend response in a UI test. |
| `blockRequests` / `clearBlockedRequests` | Block requests matching a URL regex (immediate 403), e.g. to strip out ads/trackers/third-party calls that would otherwise slow down or flake a test. |
| `setNetworkConditions` / `getNetworkConditions` / `clearNetworkConditions` | Simulate offline mode, latency, and throughput limits. |
| `setBasicAuthCredentials` | Register HTTP Basic auth credentials so a login popup never blocks navigation. |
| `startConsoleCapture` | Start capturing browser console output (`console.log`/`warn`/`error` and similar). |
| `getConsoleLogs` / `clearConsoleLogs` | Read/clear captured console log entries as JSON, optionally filtered by type. |

### REST API automation (`ApiTools`)

A standalone HTTP client, independent of the browser session — usable whether or not a browser is open. Lets an agent exercise a backend API directly (seed/verify test data, or test a service with no UI at all) alongside browser-driven automation. Every response comes back as JSON: `{status, headers, body, durationMillis}`.

| Tool | Description |
|---|---|
| `httpGet` / `httpDelete` | Send a GET/DELETE request with optional headers. |
| `httpPost` / `httpPut` / `httpPatch` | Send a POST/PUT/PATCH request with a body and optional headers. |
| `httpRequest` | Send a request with an arbitrary HTTP method (e.g. HEAD, OPTIONS). |

Headers are passed as a JSON object string, e.g. `{"Authorization": "Bearer xyz", "Content-Type": "application/json"}`.

### System (`SystemTools`)

| Tool | Description |
|---|---|
| `getStatus` | Basic server health check. |

## Project structure

```
src/main/java/uk/xpathy/selenium/mcp/
├── McpServerApplication.java     # Spring Boot entry point
├── tools/                        # @McpTool-annotated classes — the MCP-facing surface
│   ├── ToolsConfig.java           # Produces the single shared Tools bean
│   ├── BrowserTools.java          # Lifecycle, navigation, click/sendKeys
│   ├── PageSourceTools.java       # Token-efficient JSON views of the page: scripts, styles, element tree, filtered element queries
│   ├── ElementQueryTools.java     # Read-only element state queries
│   ├── WaitTools.java             # Explicit waits
│   ├── AlertTools.java            # JS alert/confirm/prompt dialogs
│   ├── FrameTools.java            # Frame/iframe switching
│   ├── WindowTools.java           # Window/tab management
│   ├── CookieTools.java           # Cookie management
│   ├── ScreenshotTools.java       # Page/element screenshots
│   ├── ScriptTools.java           # JS execution & scrolling
│   ├── ActionsTools.java          # Mouse gestures & key presses
│   ├── SelectTools.java           # <select> dropdowns
│   ├── FileTools.java             # File upload & PDF printing
│   ├── NetworkTools.java          # CDP network capture/mocking/blocking, console logs (Chrome/Edge)
│   ├── ApiTools.java              # REST API automation, independent of the browser session
│   └── SystemTools.java
├── webdriver/                    # Plain-Java Selenium layer, no MCP/Spring dependency
│   ├── Tools.java                 # Facade / entry point for this package
│   ├── BrowserFactory.java        # Creates WebDriver instances per browser
│   ├── BrowserOptions.java        # Launch options: headless, incognito, window size, user agent
│   ├── BrowserSession.java        # Owns a single driver's lifecycle
│   ├── Locators.java               # Resolves locator strategy strings to By
│   ├── Navigator.java              # Navigation (get/back/forward/refresh/url/title)
│   ├── ElementInteractor.java      # click / sendKeys
│   ├── PageSourceInspector.java    # JS-driven scripts/styles/element-tree extraction, backing PageSourceTools
│   ├── ElementInspector.java       # Read-only element queries
│   ├── WaitHelper.java             # Explicit waits, incl. page load / JS condition / element count
│   ├── AlertHandler.java           # Alert/confirm/prompt dialogs
│   ├── FrameHandler.java           # Frame/iframe switching
│   ├── WindowManager.java          # Window/tab management
│   ├── CookieManager.java          # Cookie management
│   ├── ScreenshotTaker.java        # Page/element screenshots
│   ├── ScriptRunner.java           # JS execution & scrolling
│   ├── ActionsHelper.java          # Mouse gestures & key presses
│   ├── SelectHelper.java           # <select> dropdowns
│   ├── FileHelper.java             # File upload & PDF printing
│   ├── NetworkMonitor.java         # CDP network capture/mocking/blocking/conditions/basic auth
│   └── ConsoleLogMonitor.java      # CDP browser console log capture
└── http/                         # Standalone HTTP client, no Selenium/MCP/Spring dependency
    ├── package-info.java
    └── ApiClient.java              # REST client used by tools.ApiTools
```

The `webdriver` package has no MCP or Spring dependency — it's a standalone Selenium utility layer that can also be used directly to hand-write or generate Selenium tests. `http` is likewise standalone and has no Selenium dependency — it's a plain REST client usable on its own. `tools` is a thin translation layer on top of both for MCP clients.

`NetworkMonitor` and `ConsoleLogMonitor` are built on Selenium 4's Chrome DevTools Protocol (CDP) support and only work against Chrome/Edge; on Firefox their methods throw `UnsupportedOperationException`, which `NetworkTools` turns into an explanatory message rather than a failure.

## Configuration

See [application.properties](src/main/resources/application.properties). Notably, `spring.main.web-application-type=none` and stdio transport are required for the server to work as an MCP server — don't add a web server or enable console logging, as that would corrupt the stdio protocol stream.

## Logging

Logs go to `mcp-server.log` (rotated), not the console — console output is reserved for the MCP stdio protocol.

## For AI coding agents

See [AGENTS.md](AGENTS.md) for conventions and guidance when generating or modifying code in this repo.

## Contributing

Contributions are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md) for the development setup, architecture layering rules, and PR process. Please also read [AGENTS.md](AGENTS.md), which is the source of truth for how this codebase is organized. This project follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## Security

To report a security vulnerability, please follow the process in [SECURITY.md](SECURITY.md) rather than opening a public issue.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## License

[MIT](LICENSE.md)
