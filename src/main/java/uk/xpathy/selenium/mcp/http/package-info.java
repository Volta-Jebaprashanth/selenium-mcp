/**
 * A standalone HTTP client for REST API automation, entirely independent of the
 * {@code webdriver} package. It exists so an MCP client can exercise backend APIs
 * directly — to seed/verify test data, or to test a service with no UI at all —
 * alongside or instead of browser-driven automation. No Spring or MCP types here;
 * see {@code tools.ApiTools} for the MCP-facing wrapper.
 */
package uk.xpathy.selenium.mcp.http;
