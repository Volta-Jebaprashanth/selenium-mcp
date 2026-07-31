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

| Tool | Description |
|---|---|
| `openBrowser` | Opens a browser window (`chrome`, `firefox`, or `edge`; defaults to `chrome`). Reuses an already-open browser. |
| `closeBrowser` | Closes the open browser and releases the driver. |
| `navigate` | Navigates the open browser to a URL. |
| `getPageSource` | Returns the full HTML of the current page. |
| `click` | Clicks an element, located by strategy (`id`, `name`, `css`, `xpath`, `className`, `linkText`, `partialLinkText`, `tagName`) and value. |
| `sendKeys` | Types text into an element, with an option to clear existing content first. |
| `getStatus` | Basic server health check. |

## Project structure

```
src/main/java/uk/xpathy/selenium/mcp/
├── McpServerApplication.java     # Spring Boot entry point
├── tools/                        # @McpTool-annotated classes — the MCP-facing surface
│   ├── BrowserTools.java
│   └── SystemTools.java
└── webdriver/                    # Plain-Java Selenium layer, no MCP/Spring dependency
    ├── Tools.java                 # Facade / entry point for this package
    ├── BrowserFactory.java        # Creates WebDriver instances per browser
    ├── BrowserSession.java        # Owns a single driver's lifecycle
    ├── Locators.java               # Resolves locator strategy strings to By
    └── ElementInteractor.java + Navigator.java  # Act on the driver
```

The `webdriver` package has no MCP or Spring dependency — it's a standalone Selenium utility layer that can also be used directly to hand-write or generate Selenium tests. `tools` is a thin translation layer on top of it for MCP clients.

## Configuration

See [application.properties](src/main/resources/application.properties). Notably, `spring.main.web-application-type=none` and stdio transport are required for the server to work as an MCP server — don't add a web server or enable console logging, as that would corrupt the stdio protocol stream.

## Logging

Logs go to `mcp-server.log` (rotated), not the console — console output is reserved for the MCP stdio protocol.

## For AI coding agents

See [AGENTS.md](AGENTS.md) for conventions and guidance when generating or modifying code in this repo.
