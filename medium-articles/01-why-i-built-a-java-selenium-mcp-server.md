# Java and Selenium Test Engineers Have Been Left Out of the AI Coding Wave. Not Anymore.

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server that gives AI coding assistants — Claude Code, Cursor, Claude Desktop — a real Selenium WebDriver session to drive Chrome, Firefox, or Edge directly, instead of guessing at locators from pasted HTML. It's built specifically for test automation engineers working in **Java, Selenium, and REST Assured**: structured element discovery for locators that are unique on the first try, Chrome DevTools Protocol tooling for network/console debugging, and a standalone REST client, all in one MCP server. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

If you're a test automation engineer working in Java and Selenium, you've probably already tried pointing an AI coding assistant at your test suite. Claude Code, Cursor, Copilot — pick one. They're genuinely good at scaffolding a TestNG class, writing assertions, cleaning up a Page Object Model's structure.

Then you ask for the one thing you actually needed help with — "add a method to click the checkout button on this page" — and it stalls. It can't see the page. It doesn't know if that button is `#checkout-btn`, `.checkout-button`, or the third `<button>` inside a `<div>` with no id at all. So you do what you always did before AI showed up: open DevTools yourself, inspect the element, copy the selector, paste it back into the chat so the assistant can finish writing the method.

That's not AI-assisted test automation. That's you doing the automation part and the AI doing the typing.

I hit this wall enough times that I built a fix for it — open a browser, inspect the actual live page, find the element itself, then write the Java that clicks it. No more being the copy-paste bridge between DevTools and your chat window.

## The gap this fills

The Node/Playwright ecosystem has had this figured out for a while. [Playwright MCP](https://github.com/microsoft/playwright-mcp) lets an agent open a page and reason about it structurally instead of guessing from raw markup. If your team writes Playwright and TypeScript, you already have this.

If your team writes **Java, Selenium, TestNG or JUnit, and REST Assured** — which is still how most enterprise QA organizations are built, and isn't going anywhere soon — there's been nothing equivalent. I went looking before I built this, because duplicating an existing tool is a waste of everyone's time, and what I found were a handful of Selenium MCP servers, mostly in Node.js and Python, aimed at general browser-automation agents: booking a flight, scraping a page, filling out a form on someone's behalf. None of them are built around the actual day-to-day job of a test automation engineer — writing and maintaining a Java test suite, needing correct locators for a Page Object Model, needing API calls to seed and verify test data alongside the UI flow.

There's an even more direct signal that this gap is real: [REST Assured's own GitHub repo has an open issue](https://github.com/rest-assured/rest-assured/issues/1832) asking, essentially, "when do we get an MCP server for AI-driven API test automation?" As of writing, there isn't an official answer. The Java/Selenium/REST Assured ecosystem — the one a huge share of enterprise QA runs on — has been sitting outside the current wave of AI-assisted testing tooling almost entirely.

## What selenium-mcp actually gives you

At its core, it's Selenium WebDriver exposed as MCP tools: open Chrome, Firefox, or Edge, navigate, click, type, read state — all through the same locator strategies (`id`, `css`, `xpath`, `name`, `linkText`, and so on) you already write by hand. Nothing exotic there. The part that actually changes your workflow is `PageSourceTools`, and specifically `getPageElementsFiltered` — a tool that finds every element matching a locator on the live page and hands back each one's ancestor chain and sibling position, so the agent can construct a selector that's unique on the first try instead of guessing and hitting `NoSuchElementException` against your test target. I'll go deep on exactly how that works, with a real Page Object example, in the next piece.

Past locator discovery, it covers the rest of what a real Selenium suite needs: explicit waits, alert and frame handling, window/tab management, cookies, screenshots, drag-and-drop, dropdown handling, file upload, PDF printing. It also exposes Selenium 4's Chrome DevTools Protocol support — network capture, request mocking/blocking, simulated network conditions, console log capture — useful for the kind of flaky-test diagnosis every automation engineer has lost an afternoon to. And because so much real test-suite work is API plus UI together, there's a standalone REST client built in, so an agent can help you write a REST Assured-style verification step and a Selenium UI step as part of the same test, without leaving your Java toolchain.

That's **112 tools across 16 categories** at last count — full reference on the [docs site](https://selenium-mcp.xpathy.uk/docs/tools).

## Why it had to be Java

This is the question I get asked most, so let me be direct: because the people who need this don't want to leave Java. Rewriting a decade of Selenium suites in Playwright and TypeScript just to get an AI coding assistant that can see the page isn't a realistic option for most enterprise QA teams, and it shouldn't have to be. `selenium-mcp` ships as a single jar. Point Claude Code or Cursor at it alongside your existing Maven project, and the agent is driving the same WebDriver your suite already runs on — not a different automation stack bolted on next to it.

## What's next

This is the first in a set of notes on the specific problems this solves for test automation engineers — locator discovery in depth, using the CDP network tools to debug flaky tests, combining REST Assured-style API checks with Selenium UI flows in one agent session, and the architecture decisions that keep the underlying Selenium layer usable on its own, independent of MCP, so it fits into a codebase you'd actually want to maintain.

If you want to try it now: [quickstart on the docs site](https://selenium-mcp.xpathy.uk/), grab the jar, point your MCP client at it, and ask it to write a Page Object method against a real page. It's MIT-licensed, and if something's rough, [open an issue](https://github.com/Volta-Jebaprashanth/selenium-mcp/issues) — that's genuinely the fastest way to make it better.
