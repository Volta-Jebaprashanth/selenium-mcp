# Security Policy

## Supported Versions

selenium-mcp is pre-1.0 and does not yet have a stable release line. Security
fixes are applied to the `main` branch only.

| Version | Supported |
| --- | --- |
| `main` (latest) | ✅ |
| Older snapshots | ❌ |

## Reporting a Vulnerability

Please **do not open a public GitHub issue** for security vulnerabilities.

Instead, report it privately using [GitHub's private vulnerability reporting](https://github.com/Volta-Jebaprashanth/selenium-mcp/security/advisories/new) for this repository (Security tab → "Report a vulnerability").

Include as much detail as you can:

- A description of the vulnerability and its potential impact
- Steps to reproduce, or a proof-of-concept
- The affected version/commit
- Any suggested mitigation, if you have one

You should expect an initial response within a few days. If the report is
confirmed, a fix will be prioritized and a security advisory published once a
patch is available; you'll be credited unless you prefer otherwise.

## Scope notes specific to this project

selenium-mcp is an MCP server that launches and drives real browsers (Chrome,
Firefox, Edge) and can execute arbitrary JavaScript in a page context
(`executeScript`), issue arbitrary outbound HTTP requests (`ApiTools`), and
read/write local files (`uploadFile`, `printToPdf`, screenshots). It is
designed to be run locally by a trusted MCP client under the invoking user's
own privileges — it is **not** designed to be exposed to untrusted network
input or run as a multi-tenant service. Issues in that category (e.g.
"a malicious MCP client can make it do X") are generally expected behavior
for a tool with this level of access, but please report anything that lets a
remote/untrusted **web page** the browser navigates to escape the browser
sandbox, exfiltrate host data it shouldn't have access to, or execute
arbitrary code on the host outside of the documented tool surface.
