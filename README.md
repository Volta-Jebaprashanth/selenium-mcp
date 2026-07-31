# selenium-mcp

An [MCP](https://modelcontextprotocol.io) server that exposes Selenium browser automation as tools for AI agents. Built with Spring Boot and Spring AI's MCP server starter, it talks to clients over stdio.

## What it does

An MCP client (Claude Desktop, Claude Code, Cursor, etc.) can drive a real Chrome, Firefox, or Edge browser through this server: open a browser, navigate to a URL, read the page source, click elements, and type into fields — all via standard Selenium locators.

## Prerequisites

- Java 21+ (`JAVA_HOME` pointing at a JDK 21 install)
- Maven 3.9+
- Chrome, Firefox, or Edge installed locally (drivers are fetched automatically by [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) on first use)

## Build

```bash
mvn clean package
```

## Run

The server communicates over stdio, so it's meant to be launched by an MCP client rather than run standalone in a terminal.

```bash
java -jar target/selenium-mcp-1.0-SNAPSHOT.jar
```

### Registering with an MCP client

Example client config (adjust the jar path to your build output):

```json
{
  "mcpServers": {
    "selenium-mcp": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/target/selenium-mcp-1.0-SNAPSHOT.jar"]
    }
  }
}
```

## Available tools

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
│   └── SystemTools.java
└── webdriver/                    # Plain-Java Selenium layer, no MCP/Spring dependency
    ├── Tools.java                 # Facade / entry point for this package
    ├── BrowserFactory.java        # Creates WebDriver instances per browser
    ├── BrowserOptions.java        # Launch options: headless, incognito, window size, user agent
    ├── BrowserSession.java        # Owns a single driver's lifecycle
    ├── Locators.java               # Resolves locator strategy strings to By
    ├── Navigator.java              # Navigation (get/back/forward/refresh/url/title)
    ├── ElementInteractor.java      # click / sendKeys
    ├── ElementInspector.java       # Read-only element queries
    ├── WaitHelper.java             # Explicit waits
    ├── AlertHandler.java           # Alert/confirm/prompt dialogs
    ├── FrameHandler.java           # Frame/iframe switching
    ├── WindowManager.java          # Window/tab management
    ├── CookieManager.java          # Cookie management
    ├── ScreenshotTaker.java        # Page/element screenshots
    ├── ScriptRunner.java           # JS execution & scrolling
    ├── ActionsHelper.java          # Mouse gestures & key presses
    ├── SelectHelper.java           # <select> dropdowns
    └── FileHelper.java             # File upload & PDF printing
```

The `webdriver` package has no MCP or Spring dependency — it's a standalone Selenium utility layer that can also be used directly to hand-write or generate Selenium tests. `tools` is a thin translation layer on top of it for MCP clients.

## Configuration

See [application.properties](src/main/resources/application.properties). Notably, `spring.main.web-application-type=none` and stdio transport are required for the server to work as an MCP server — don't add a web server or enable console logging, as that would corrupt the stdio protocol stream.

## Logging

Logs go to `mcp-server.log` (rotated), not the console — console output is reserved for the MCP stdio protocol.

## For AI coding agents

See [AGENTS.md](AGENTS.md) for conventions and guidance when generating or modifying code in this repo.
