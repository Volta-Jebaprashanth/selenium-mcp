# How to Get an AI Coding Assistant to Write a Correct Selenium Locator on the First Try

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server that hands an AI coding agent a real Selenium WebDriver session — it opens Chrome, Firefox, or Edge, inspects the actual live page, and writes the Java locator itself instead of guessing from pasted HTML. Built specifically for test automation engineers working in **Java, Selenium, and REST Assured**, it also covers CDP network/console debugging and standalone REST API testing. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

Every Selenium engineer has written this line of debugging output at least once:

```
org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element
```

or the meaner cousin — the locator *does* match something, just three of it, and your test clicked the wrong one. You fix it the same way every time: open DevTools, inspect the actual element, walk up the DOM until you find something unique about it, write the real selector, run the test again.

Now hand that same task to an AI coding assistant without giving it eyes on the page, and it does the only thing it can — it guesses from whatever HTML you pasted in, or from a locator pattern that looked right in a similar test elsewhere in your suite. Sometimes it's right. Often it isn't, and you're back to doing the DevTools inspection yourself and feeding the answer back into the chat.

This is the specific problem **selenium-mcp** exists to solve.

## The tool that changes the workflow

It's called `getPageElementsFiltered`, part of the `PageSourceTools` group. Instead of finding just the first element that matches a locator, it finds **every** matching element on the live page and returns each as structured JSON, with three optional context flags:

- **`includeAncestors`** — the full parent chain up to `<html>`, with every node annotated with `siblingIndex` and `siblingCount`. This is the piece that matters most: it's what lets the agent build a selector that's provably unique instead of one that merely looks specific.
- **`includeSiblings`** — the other children of the same parent, useful when a locator needs to correlate a `<label>` with the `<input>` sitting next to it.
- **`includeDescendants`** — expand a match's own children, for when you're locating a container rather than a leaf, like a table row you need to then query further.

The response reports `totalMatches`, `returnedMatches`, and `truncated` every time (results capped via `limit`, default 50), so the agent — and you, reading the tool call log — always know whether you're seeing the whole picture.

## What this looks like in a real Page Object

Say you're writing a `LoginPage` class and the markup gives you nothing helpful — two `<input>` elements with no `id`, no `name`, generic classes shared across the form. This is the exact situation that used to mean tabbing over to the browser yourself.

Instead, point the agent at `getPageElementsFiltered` with `locatorType=xpath`, `locatorValue=//input`, `includeAncestors=true`. It gets back one JSON entry per `<input>` on the page, each with its ancestor path and sibling position. For the password field — second input inside the login form — the ancestor chain comes back annotated as `siblingIndex: 1, siblingCount: 2` under `form#login`. From that, the agent writes:

```java
private final By passwordField = By.cssSelector("div.container > form#login > input:nth-of-type(2)");
```

That selector is unique *because the tool told the agent exactly how many sibling inputs exist and where this one sits* — not because it looked plausible. It compiles into a working `LoginPage` method without a single round trip through your browser's DevTools.

## The rest of the family

`getPageElementsFiltered` is what you reach for once you know what you're locating. Three other tools in the same group cover the steps before and around it:

- **`getPageScripts`** — every `<script>` on the page as JSON (`src` for external, `content` for inline, truncated past 10,000 characters). Handy when you're trying to understand what a page's JS is actually doing before writing a wait condition against it.
- **`getPageStyles`** — the same idea for stylesheets.
- **`getPageElements`** — the whole page as a compact JSON tree rooted at `<body>`, scripts/styles/comments stripped, depth-capped via `maxDepth` (default 20). This is the "give me an overview" tool, for when you're starting a new Page Object and don't yet know the structure at all.

## Why this matters for a test suite specifically, not just "an agent"

A generic browser-automation agent just needs *a* selector that works once. A test automation engineer needs one that keeps working — through refactors, through a designer swapping a `<div>` for a `<section>`, through a QA lead reviewing the PR and asking "why does this selector match three things." Getting the ancestor and sibling context up front, before a selector ever gets written into a `By.cssSelector(...)` call, is the difference between a Page Object you trust and one you'll be back in six months later, debugging the same `NoSuchElementException` all over again.

It's also just cheaper. A page with a few hundred DOM nodes routinely produces 30,000+ characters of raw HTML. Reading that into a prompt every time you need one locator is expensive in tokens and slow to iterate on. `getPageElementsFiltered`, scoped to exactly the elements you're locating, is a fraction of that size — and it comes with the disambiguating context a raw dump never gave you in the first place.

Full parameter list is on the [tool reference](https://selenium-mcp.xpathy.uk/docs/tools); the [repo](https://github.com/Volta-Jebaprashanth/selenium-mcp) has the source if you want to see how the ancestor walk is built (plain Selenium/JS underneath, nothing exotic).
