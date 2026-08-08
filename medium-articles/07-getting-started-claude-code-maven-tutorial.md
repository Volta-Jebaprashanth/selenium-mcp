# From Zero to a Working Selenium Test, Written by an AI Agent That Can Actually See the Page

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server built for test automation engineers working in **Java, Selenium, and REST Assured**. It gives an AI coding agent — Claude Code, Cursor, Claude Desktop, anything that speaks MCP — a real Selenium WebDriver session it can drive directly, with structured element discovery, CDP network/console tooling, and a standalone REST client, instead of guessing at locators from pasted HTML. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

This one's a walkthrough, not an argument. If you're a test automation engineer curious about **selenium-mcp** but haven't tried it yet, here's exactly how to go from nothing to an AI coding assistant writing a real, passing Selenium test against a page it inspected itself.

## Prerequisites

Nothing exotic:

- Java 21+, with `JAVA_HOME` pointing at it
- Chrome, Firefox, or Edge installed locally — WebDriver binaries are fetched automatically on first use, no manual driver management
- An MCP-capable client: Claude Code, Claude Desktop, Cursor, etc.

That's the whole list. No Node toolchain, no separate browser runtime to install — the server drives the actual browser you already have.

## Step 1 — Grab the jar

Every push to `main` publishes a fresh GitHub Release with the jar attached, so there's no build step required:

**[⬇ Download selenium-mcp.jar (latest release)](https://github.com/Volta-Jebaprashanth/selenium-mcp/releases/latest/download/selenium-mcp.jar)**

If you'd rather build from source:

```bash
git clone https://github.com/Volta-Jebaprashanth/selenium-mcp.git
cd selenium-mcp
mvn clean package
```

## Step 2 — Point your MCP client at it

The server talks over stdio, so it's launched by your MCP client rather than run standalone in a terminal. For Claude Code or Claude Desktop, add this to your MCP server config, adjusting the path to wherever you saved the jar:

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

Restart your client, and you should see `selenium-mcp` show up as an available tool provider, with all 112 tools across 16 categories ready to call.

## Step 3 — Point it at your actual Maven project

This is the part that matters if you're evaluating this for real work rather than just kicking the tires: open your MCP client in the same workspace as your existing Java/Selenium/Maven test project. The agent isn't just driving a throwaway browser session — it's writing Java files directly into your `src/test/java` tree, using whatever test framework (TestNG, JUnit 5) and structure your project already has.

## Step 4 — Ask for a real test

Here's a prompt shape that exercises the actual point of the tool, rather than a toy example:

> "Open Chrome and navigate to our staging login page at `https://staging.example.com/login`. Inspect the email and password fields using the page element tools, then write a `LoginPageTest` class in our existing `LoginPage` Page Object style, using TestNG, that logs in with a valid test user and asserts we land on the dashboard."

Watch the tool calls as they happen (most MCP clients show these): `openBrowser`, `navigate`, then `getPageElementsFiltered` against the login form to get real ancestor/sibling-disambiguated locators — not guesses. The agent writes the `By` selectors from what it actually found on the page, adds a `waitForVisible` or `waitForPageLoad` where the flow needs it, and hands you a test file that follows your project's existing conventions because it can see them, same as it can see the page.

## Step 5 — Run it, review it like any other PR

The output is a plain Java file using standard Selenium and your existing test framework — nothing proprietary, no runtime dependency on the MCP server itself (see the [architecture piece](05-architecture-tour-clean-selenium-layer.md) if you want the detail on why that's true by design). Run it with your normal `mvn test`. Review it the way you'd review any teammate's PR — check the locators are sensible, check the waits are appropriate, ask for changes the same way you would with a human's first draft.

## A few things worth trying beyond the first test

Once the basic loop works, a few follow-ups are worth exploring in the same session:

- Ask it to add a REST Assured-style verification step using `ApiTools`, seeding or checking data through the API instead of only through the UI — see the [API+UI piece](04-rest-assured-ai-gap-api-ui-testing.md) for what that looks like in practice.
- Point it at a flaky existing test and ask it to diagnose the timing issue using `NetworkTools` — `waitForNetworkIdle` and `getNetworkLog` in particular.
- Ask it to refactor an existing Page Object's brittle locators using `getPageElementsFiltered`, rather than only writing new ones.

## If something doesn't work

The project is MIT-licensed and actively maintained — if a tool call behaves unexpectedly or a locator strategy you need isn't supported, [open an issue](https://github.com/Volta-Jebaprashanth/selenium-mcp/issues). The full, searchable tool reference — every parameter, every category — lives on the [docs site](https://selenium-mcp.xpathy.uk/docs/tools), worth bookmarking once you're past the first test and exploring what else is available.
