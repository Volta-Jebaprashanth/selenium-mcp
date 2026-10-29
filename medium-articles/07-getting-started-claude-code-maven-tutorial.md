# From Zero to a Working Selenium Test, Written by an AI Agent That Can Actually See the Page

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server built for test automation engineers working in **Java, Selenium, and REST Assured**. It gives an AI coding agent — Claude Code, Cursor, Claude Desktop, anything that speaks MCP — a real Selenium WebDriver session it can drive directly, with structured element discovery, CDP network/console tooling, and a standalone REST client, instead of guessing at locators from pasted HTML. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.voltajeba.com](https://selenium-mcp.voltajeba.com/)

This one's a walkthrough, not an argument. If you're a test automation engineer curious about **selenium-mcp** but haven't tried it yet, here's exactly how to go from nothing to an AI coding assistant writing a real, passing Selenium test against a page it inspected itself.

## Prerequisites

Nothing exotic:

- [Node.js](https://nodejs.org) 18+, for `npx`
- Chrome, Firefox, or Edge installed locally — WebDriver binaries are fetched automatically on first use, no manual driver management
- An MCP-capable client: Claude Code, Claude Desktop, Cursor, VS Code, etc.

That's the whole list. Notice what's *not* on it: Java. The server is written in Java, but it's published to npm as `@voltajeba/selenium-mcp` with its own trimmed Java 21 runtime bundled, so you don't need a JDK installed or `JAVA_HOME` set just to run it. And there's no separate browser runtime either — the server drives the actual browser you already have.

## Step 1 — Add it to your MCP client

The server talks over stdio, so it's launched by your MCP client rather than run standalone in a terminal. For Claude Code (`.mcp.json`), Cursor, or Claude Desktop, add this to your MCP server config:

```json
{
  "mcpServers": {
    "selenium-mcp": {
      "command": "npx",
      "args": ["-y", "@voltajeba/selenium-mcp"]
    }
  }
}
```

A few variations, depending on where you're running it:

- **Claude Code from a terminal**, if you'd rather not edit JSON: `claude mcp add selenium-mcp -- npx -y @voltajeba/selenium-mcp`
- **Claude Code on native Windows** needs `npx` wrapped in `cmd /c`: `"command": "cmd", "args": ["/c", "npx", "-y", "@voltajeba/selenium-mcp"]`
- **VS Code** (`.vscode/mcp.json`) uses `servers` as the top-level key instead of `mcpServers`; the inner `command`/`args` are the same.

Restart your client, and you should see `selenium-mcp` show up as an available tool provider, with all 116 tools across 16 categories ready to call.

**The first start is the slow one.** npm installs only the platform package matching your machine (Windows x64, Linux x64, macOS Intel, or macOS Apple Silicon), but that's still around 90 MB — the Java runtime plus the server. If your client gives up waiting on that first launch, just restart the server once, or pre-download it from a terminal beforehand with `npx -y @voltajeba/selenium-mcp --version`. Every start after that is local. Logs go to `~/.selenium-mcp/mcp-server.log` if you need to see what happened.

**Optional: load fewer tools.** Every tool's schema costs your agent context tokens before it's done anything. Anything you append to `args` is passed through to the server, so if you're only interested in the browser and element discovery for now, you can switch off whole tool groups:

```json
"args": ["-y", "@voltajeba/selenium-mcp", "--selenium.mcp.exclude-tool-groups=api,network,cookies,alerts,select,files"]
```

That takes it from 116 tools down to 75. Core browser tools (open, navigate, click, type) are always on.

### Prefer running the jar on your own JDK?

If your team would rather run it on a Java 21+ runtime you already manage — locked-down machines, no Node allowed, or just preference — the plain jar is still published with every GitHub Release:

**[⬇ Download selenium-mcp.jar (latest release)](https://github.com/Volta-Jebaprashanth/selenium-mcp/releases/latest/download/selenium-mcp.jar)**

and is wired up with `"command": "java", "args": ["-jar", "/absolute/path/to/selenium-mcp.jar"]` instead. Or build it from source with `git clone https://github.com/Volta-Jebaprashanth/selenium-mcp.git && cd selenium-mcp && mvn clean package`. Same server, same tools — npm is just the shorter path.

## Step 2 — Point it at your actual Maven project

This is the part that matters if you're evaluating this for real work rather than just kicking the tires: open your MCP client in the same workspace as your existing Java/Selenium/Maven test project. The agent isn't just driving a throwaway browser session — it's writing Java files directly into your `src/test/java` tree, using whatever test framework (TestNG, JUnit 5) and structure your project already has.

## Step 3 — Ask for a real test

Here's a prompt shape that exercises the actual point of the tool, rather than a toy example:

> "Open Chrome and navigate to our staging login page at `https://staging.example.com/login`. Inspect the email and password fields using the page element tools, then write a `LoginPageTest` class in our existing `LoginPage` Page Object style, using TestNG, that logs in with a valid test user and asserts we land on the dashboard."

Watch the tool calls as they happen (most MCP clients show these): `openBrowser`, `navigate`, then `getPageElementsFiltered` against the login form to get real ancestor/sibling-disambiguated locators — not guesses. The agent writes the `By` selectors from what it actually found on the page, adds a `waitForVisible` or `waitForPageLoad` where the flow needs it, and hands you a test file that follows your project's existing conventions because it can see them, same as it can see the page.

## Step 4 — Run it, review it like any other PR

The output is a plain Java file using standard Selenium and your existing test framework — nothing proprietary, no runtime dependency on the MCP server itself (see the [architecture piece](05-architecture-tour-clean-selenium-layer.md) if you want the detail on why that's true by design). Run it with your normal `mvn test`. Review it the way you'd review any teammate's PR — check the locators are sensible, check the waits are appropriate, ask for changes the same way you would with a human's first draft.

## A few things worth trying beyond the first test

Once the basic loop works, a few follow-ups are worth exploring in the same session:

- Ask it to add a REST Assured-style verification step using `ApiTools`, seeding or checking data through the API instead of only through the UI — see the [API+UI piece](04-rest-assured-ai-gap-api-ui-testing.md) for what that looks like in practice.
- Point it at a flaky existing test and ask it to diagnose the timing issue using `NetworkTools` — `waitForNetworkIdle` and `getNetworkLog` in particular.
- Next time a test fails, have it attach to the browser the test was driving (`launchDebugBrowser` / `connectToBrowser`) and inspect the live failure state instead of reproducing it — see the [debugger-mode piece](11-attach-ai-agent-to-failing-selenium-test-browser.md).
- Ask it to refactor an existing Page Object's brittle locators using `getPageElementsFiltered`, rather than only writing new ones.

## If something doesn't work

The project is MIT-licensed and actively maintained — if a tool call behaves unexpectedly or a locator strategy you need isn't supported, [open an issue](https://github.com/Volta-Jebaprashanth/selenium-mcp/issues). The full, searchable tool reference — every parameter, every category — lives on the [docs site](https://selenium-mcp.voltajeba.com/docs/tools), worth bookmarking once you're past the first test and exploring what else is available.
