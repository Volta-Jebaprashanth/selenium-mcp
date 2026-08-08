# REST Assured Doesn't Have an Official AI Tool Yet. Here's Why That Matters More Than It Sounds.

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server that gives an AI coding assistant a real Selenium WebDriver session — plus a standalone REST client — built specifically for test automation engineers working in **Java, Selenium, and REST Assured**. It drives Chrome, Firefox, or Edge directly, discovers page elements with enough structure to write locators that are unique on the first try, and exposes Chrome DevTools Protocol tooling alongside plain HTTP request tools for API testing. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

Go look at [issue #1832 on the REST Assured GitHub repo](https://github.com/rest-assured/rest-assured/issues/1832). Someone's asking, plainly: is there a plan for an official MCP server, so AI agents can drive API test automation the way they've started driving everything else? As of writing, there's no official answer.

That's a small GitHub issue, but it says something real about where the current wave of "AI-assisted testing" tooling has and hasn't reached. If you write UI tests in Playwright and TypeScript, or you're doing general web scraping, there's no shortage of AI tooling aimed at you. If you write **Java, Selenium, and REST Assured** — which describes a huge share of enterprise QA teams — the tooling gap is real, and it's not just about the browser. It's about the API layer too.

I built **selenium-mcp** to close both halves of that gap at once, for exactly that audience.

## Why a browser tool also needs an API client

Think about how a real end-to-end test actually gets written. You rarely test the UI in total isolation — you seed data through an API first (create a user, create an order), drive the UI flow, and then very often verify the *result* by calling the API again rather than scraping it back out of the DOM. That's the whole reason REST Assured exists next to Selenium in so many Java test suites in the first place: UI and API testing are two tools for the same job, used together constantly.

An AI coding assistant that can only see the browser is missing half of that picture. It can help you write the UI half of a test and then just... stop, and hand the API half back to you. That's the same "you do the real work, I'll type it up" problem I've written about with locators — just showing up in a different part of the test.

## What `ApiTools` gives you

`ApiTools` is a standalone HTTP client, independent of the browser session entirely — it works whether or not a browser is even open. It gives an agent:

- **`httpGet`** / **`httpDelete`** — GET/DELETE with optional headers.
- **`httpPost`** / **`httpPut`** / **`httpPatch`** — send a body plus optional headers.
- **`httpRequest`** — arbitrary HTTP method, for the odd `HEAD` or `OPTIONS` check.

Every response comes back as a consistent JSON shape — `{status, headers, body, durationMillis}` — which matters more than it sounds like: it means the agent (and you, reading the tool call output) get response *and* timing back from the same call, useful when you're checking not just correctness but whether an endpoint is meeting a latency expectation as part of the test. Headers go in as a plain JSON object string, e.g. `{"Authorization": "Bearer xyz", "Content-Type": "application/json"}` — nothing to memorize beyond what you'd already write in REST Assured itself.

## What this looks like in a real test

Say you're writing an end-to-end checkout test. The flow is: create a test user via API, log that user in through the UI, add an item to the cart, complete checkout, then verify the order landed correctly by calling the orders API directly rather than trusting a confirmation `<div>` on screen.

With `ApiTools` and `BrowserTools`/`PageSourceTools` available in the same agent session, you can ask for exactly that end-to-end flow in one request, and the agent writes it as one coherent Java test — `httpPost` to seed the user, Selenium calls (with correctly discovered locators, via `getPageElementsFiltered`) to drive the UI, `httpGet` to verify the order — instead of you assembling the API call yourself and asking the agent to "add this in."

## Why this is different from a general-purpose REST client tool

There are plenty of standalone MCP servers that just do HTTP requests — Postman-style tools, generic API testers. What makes this different isn't the HTTP client itself; it's that it lives in the *same* session as the Selenium tooling, so an agent building a test doesn't have to context-switch between two separate MCP servers to write one test that spans both layers. One conversation, one Java test file, both halves of your suite's actual working pattern reflected in the output.

It's also, notably, not something the Node/Python Selenium MCP servers offer either — most of them are scoped to browser automation alone, aimed at agentic browsing rather than test-suite authoring. Between that and the open question on REST Assured's own repo about official AI tooling, this is a gap that's been sitting unaddressed on the Java/Selenium/REST Assured side specifically, not something everyone else already solved and I just packaged differently.

Full details on request/response shapes are on the [tool reference](https://selenium-mcp.xpathy.uk/docs/tools). If you're maintaining a suite that already mixes REST Assured and Selenium, this is the part of `selenium-mcp` worth trying first — it's the piece that mirrors how you were already structuring tests, not a new pattern to learn.
