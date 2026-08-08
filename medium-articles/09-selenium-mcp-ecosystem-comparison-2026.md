# I Looked at Every Selenium MCP Server I Could Find Before Building My Own. Here's the Actual Landscape.

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server that gives an AI coding assistant a real Selenium WebDriver session to inspect and drive Chrome, Firefox, or Edge directly, built specifically for test automation engineers working in **Java, Selenium, and REST Assured**. It combines structured element discovery for reliable locators, Chrome DevTools Protocol network/console tooling, and a standalone REST client in one server. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

Before writing a single line of **selenium-mcp**, I did what you're supposed to do before building anything: I checked whether it already existed. It's a reasonable thing to want to verify as a reader too — "another Selenium MCP server" is a fair amount of skepticism to bring to a new one. So here's the honest landscape, from the perspective of the audience this project is actually for: **test automation engineers working in Java, Selenium, and REST Assured**, evaluating whether any of these tools help with that specific job, not general browser automation.

## What's actually out there

There are more Selenium MCP servers than you'd expect — a healthy sign the underlying idea (let an AI agent drive Selenium) is obviously useful. Most cluster around the same shape:

**Node.js/TypeScript implementations**, several of which expose 15–80+ tools covering browser lifecycle, element interaction, and window/frame/cookie management — the standard Selenium surface. One of the more complete ones even includes an accessibility-tree resource for structured page inspection, which is a genuinely good idea done well. What none of them offer, as far as I found: a REST client for API testing alongside the browser tools, or Chrome DevTools Protocol access for network mocking/blocking. And structurally, they're aimed at general agentic browser automation — the target user is "an agent doing a task in a browser," not specifically a Java engineer maintaining a Selenium test suite.

**Python implementations**, similar shape — solid coverage of core Selenium actions, locator-based find/click/type tooling, screenshots. Same gap: no structured ancestor/sibling context for disambiguating locators, no CDP-backed network tooling, no adjacent API-testing capability.

**Remote/browserless-backed implementations**, built for connecting to a Selenium Grid or a browserless service rather than driving a local browser — useful for a specific infrastructure pattern, but orthogonal to the locator-discovery and API-testing gaps this piece is about.

I want to be fair here: several of these are well-built, actively maintained projects solving a real problem — browser automation for AI agents, full stop. That's a legitimate and different goal from what `selenium-mcp` is for.

## What's specifically missing, and it's the same gap every time

Across essentially all of them, three things are consistently absent — and they're the three things that matter most if your actual job is maintaining a Java test suite, not just getting an agent to click through a demo:

- **Structured, disambiguating element discovery.** Most expose a `find_element`-equivalent plus raw page source. None I found return a match's ancestor chain with sibling position/count the way `getPageElementsFiltered` does — the specific thing that lets an agent construct a locator that's unique *before* trying it, instead of hitting `NoSuchElementException` and iterating.
- **CDP-backed network and console tooling.** Selenium 4 has had `HasDevTools`/`NetworkInterceptor` access for a while. Almost nobody wires it up. Network capture shows up occasionally; mocking, blocking, simulated network conditions, and console log capture — the tools that actually help diagnose a flaky test — are rare to absent.
- **A standalone REST client for API testing.** Not one of the general-purpose Selenium MCP servers I found ships an HTTP client alongside the browser tools. Given that [REST Assured's own repo has an open issue](https://github.com/rest-assured/rest-assured/issues/1832) asking for exactly this kind of AI-driven API testing support, this isn't a niche gap — it's a documented, unaddressed one across the whole Java testing ecosystem, not just among Selenium MCP servers specifically.

## Where Playwright MCP fits into this

[Playwright MCP](https://github.com/microsoft/playwright-mcp) deserves its own mention because it's the project that proved the underlying idea — structured page inspection beats raw HTML dumps for agent reliability — at scale, with official backing. It's excellent. It's also Node/Playwright, which means it does nothing for a team whose suite, CI, and team expertise are Java and Selenium, short of a full framework migration. I've written [more on that specific trade-off separately](06-java-selenium-vs-playwright-mcp-enterprise-qa.md) — the short version is that `selenium-mcp` brings the same category of capability natively into Selenium, rather than asking Java teams to leave it.

## Where selenium-mcp actually sits

Putting it plainly, without the hedging: `selenium-mcp` is, as far as I've been able to verify, the only Selenium-based MCP server that combines all three of structured ancestor/sibling-aware element discovery, CDP-backed network/console tooling, and a standalone REST client for API testing — built in Java specifically so it drops into an existing Selenium/Maven toolchain rather than asking a team to adopt a new language or runtime. If you know of another implementation that covers this same combination, I'd genuinely like to hear about it — [open an issue](https://github.com/Volta-Jebaprashanth/selenium-mcp/issues) or a discussion, comparisons like this are only useful if they stay accurate as the landscape moves.

If you want to check the claims yourself rather than take my word for it, the [tool reference](https://selenium-mcp.xpathy.uk/docs/tools) lists every one of the 112 tools with parameters, and the [repo](https://github.com/Volta-Jebaprashanth/selenium-mcp) is MIT-licensed and open for inspection.
