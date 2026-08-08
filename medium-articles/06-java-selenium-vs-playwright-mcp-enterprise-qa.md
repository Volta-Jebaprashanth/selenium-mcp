# No, Your Team Doesn't Need to Rewrite Its Selenium Suite in Playwright to Get AI-Assisted Testing

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server that gives an AI coding assistant a real Selenium WebDriver session to drive Chrome, Firefox, or Edge directly, built specifically for test automation engineers working in **Java, Selenium, and REST Assured**. It covers structured element discovery for reliable locators, Chrome DevTools Protocol network/console debugging, and a standalone REST client for API testing — all inside the Selenium/Maven toolchain teams already run. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

Every few months, someone on a Java QA team asks the same question in a different form: "should we just move to Playwright? It's got better AI tooling, better tracing, the MCP server is official." It's a fair question. [Playwright MCP](https://github.com/microsoft/playwright-mcp) is genuinely good — structured accessibility-tree snapshots, official Microsoft backing, tight integration with the framework it's built for.

It's also, for a huge share of enterprise QA teams, not a realistic answer. You don't migrate a decade of Selenium suites, a CI pipeline built around a Selenium Grid, a team of engineers who know Java and TestNG, and every internal utility library built on top of `WebDriver`, just to get an AI coding assistant that can see the page. That's not a tooling decision, that's a multi-quarter migration project with its own risk, and most teams correctly won't sign up for it just to unlock a developer-experience improvement.

This is the actual reason I built **selenium-mcp**: so Java/Selenium teams get the same category of capability without the rewrite.

## What Playwright MCP actually solved

Worth being precise about this, since it's the thing everyone's actually asking for. Playwright MCP's core contribution is replacing raw DOM dumps with a structured accessibility-tree snapshot — an agent reasons about "the Sign In button" as a distinct, addressable thing instead of parsing markup by eye. That's the capability that made AI-driven browser testing actually reliable instead of a novelty.

`selenium-mcp` brings the same underlying idea to Selenium — not the identical mechanism (Selenium doesn't expose Playwright's accessibility-snapshot API), but the same outcome: `getPageElementsFiltered` finds every element matching a locator and returns it with full ancestor-chain and sibling-position context, so an agent builds a locator that's unique by construction instead of by guesswork. Same problem, same category of fix, native to Selenium instead of bolted onto it.

## Where the two genuinely differ, past that

Being straight about trade-offs matters more than pretending there aren't any:

- **Language and existing investment.** Playwright MCP is Node.js. If your suite, your CI, and your team's expertise are already Java/Selenium, `selenium-mcp` runs as a single jar inside that same world — no new language, no new CI toolchain, no new runtime to operationalize.
- **Browser engine.** Playwright drives Chromium, Firefox, and WebKit through its own automation protocol. Selenium drives real Chrome, Firefox, and Edge through WebDriver — the actual browsers your users run, via the actual protocol your existing suite already trusts.
- **API testing in the same tool.** `selenium-mcp` ships a standalone REST client (`ApiTools`) alongside the browser tools, so an agent can help write a REST Assured-style verification step in the same session as a UI step. Playwright MCP is scoped to the browser; API testing is a separate concern in that ecosystem.
- **CDP-backed network and console tooling.** Both expose network capture; `selenium-mcp` additionally offers request mocking/blocking and simulated network conditions via Chrome DevTools Protocol on Chrome/Edge — the same low-level access, made reachable from Selenium instead of requiring you to already be on Playwright to get it.

## What this isn't

This isn't "Selenium is better than Playwright" — that's a different, much older debate, and it depends heavily on what you're testing and how your team already works. This is specifically about the AI-assisted testing gap: if your team's stack is already Playwright, you have this solved. If it's Java and Selenium, you didn't, until now, and the honest fix for that gap was never "switch frameworks" — it was "bring the same capability natively to the framework you're already using and already trust."

## The actual decision in front of most teams

If you're starting a browser-automation project from zero today, with no existing suite and no team constraints, the Playwright-vs-Selenium conversation is genuinely open and worth having on its own merits. But that's not the situation most QA organizations reading this are in. You have a working Selenium suite, a CI pipeline that runs it, and engineers who are productive in Java. The question was never "is Playwright's tooling nicer" — often, yes. The question is whether that's worth a rewrite. For most teams, it isn't, and now it doesn't have to be a trade-off you make at all.

Full tool reference is on the [docs site](https://selenium-mcp.xpathy.uk/docs/tools) if you want to see exactly what's available before deciding whether this closes the gap for your team.
