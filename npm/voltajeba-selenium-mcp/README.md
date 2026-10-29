# @voltajeba/selenium-mcp

The same [Model Context Protocol](https://modelcontextprotocol.io) server as [@xpathy/selenium-mcp](https://www.npmjs.com/package/@xpathy/selenium-mcp), published under the `@voltajeba` scope. It lets AI coding assistants (Claude Code, Cursor, VS Code, Claude Desktop) drive a real Chrome, Edge, or Firefox through Selenium WebDriver, and is built for Java/Selenium/REST Assured test automation engineers.

It installs `@xpathy/selenium-mcp` of the same version, which bundles its own Java runtime, so **you don't need Java installed**. All you need is Node.js (for `npx`) and a browser.

## Setup

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

In VS Code (`.vscode/mcp.json`) the top-level key is `servers` instead of `mcpServers`. On **native Windows**, Claude Code needs `npx` wrapped in `cmd /c`: `"command": "cmd", "args": ["/c", "npx", "-y", "@voltajeba/selenium-mcp"]`.

The first start downloads about 90 MB. If your client times out on it, restart the server once, or pre-download it with `npx -y @voltajeba/selenium-mcp --version` in a terminal.

Options, platforms and the full tool reference: [@xpathy/selenium-mcp](https://www.npmjs.com/package/@xpathy/selenium-mcp) · [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/) · Source: [GitHub](https://github.com/Volta-Jebaprashanth/selenium-mcp)
