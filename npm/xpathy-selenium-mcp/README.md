# @xpathy/selenium-mcp

**This package is now [@voltajeba/selenium-mcp](https://www.npmjs.com/package/@voltajeba/selenium-mcp).** `@xpathy/selenium-mcp` keeps working and is still released with every version: it installs and runs `@voltajeba/selenium-mcp` of the same version, so existing configs need no change.

For new setups, use the new name:

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

selenium-mcp is a [Model Context Protocol](https://modelcontextprotocol.io) server that lets AI coding assistants (Claude Code, Cursor, VS Code, Claude Desktop) drive a real Chrome, Edge, or Firefox through Selenium WebDriver, built for Java/Selenium/REST Assured test automation engineers. It bundles its own Java runtime, so you don't need Java installed.

Setup, options and the full tool reference: [@voltajeba/selenium-mcp](https://www.npmjs.com/package/@voltajeba/selenium-mcp) · [selenium-mcp.voltajeba.com](https://selenium-mcp.voltajeba.com/) · Source: [GitHub](https://github.com/Volta-Jebaprashanth/selenium-mcp)
