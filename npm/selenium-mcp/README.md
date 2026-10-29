# @voltajeba/selenium-mcp

A [Model Context Protocol](https://modelcontextprotocol.io) server that lets AI coding assistants (Claude Code, Cursor, VS Code, Claude Desktop) drive a real Chrome, Edge, or Firefox through Selenium WebDriver — built for Java/Selenium/REST Assured test automation engineers.

This package bundles its own Java runtime, so **you don't need Java installed**. All you need is Node.js (for `npx`) and a browser.

## Setup

**Claude Code / Cursor / Claude Desktop** (`.mcp.json`, `mcp.json`, `claude_desktop_config.json`):

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

**VS Code** (`.vscode/mcp.json`):

```json
{
  "servers": {
    "selenium-mcp": {
      "command": "npx",
      "args": ["-y", "@voltajeba/selenium-mcp"]
    }
  }
}
```

On **native Windows**, Claude Code needs `npx` wrapped in `cmd /c`: `"command": "cmd", "args": ["/c", "npx", "-y", "@voltajeba/selenium-mcp"]`.

Or, in Claude Code, from a terminal: `claude mcp add selenium-mcp -- npx -y @voltajeba/selenium-mcp`

The first start downloads about 90 MB (the runtime for your OS, plus the server). If your client times out on that first start, restart the server once — or pre-download it with `npx -y @voltajeba/selenium-mcp --version` in a terminal.

Supported platforms: Windows x64, Linux x64 (glibc), macOS Intel, macOS Apple Silicon.

## Options

Any extra arguments are passed to the server, e.g. to load fewer tools:

```json
"args": ["-y", "@voltajeba/selenium-mcp", "--selenium.mcp.exclude-tool-groups=api,network,cookies"]
```

Logs go to `~/.selenium-mcp/mcp-server.log`.

Full tool reference and documentation: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/) · Source: [GitHub](https://github.com/Volta-Jebaprashanth/selenium-mcp)
