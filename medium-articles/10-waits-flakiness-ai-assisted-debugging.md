# The Wait Condition You Actually Need Almost Never Matches the One You Wrote

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server built for test automation engineers working in **Java, Selenium, and REST Assured**. It gives an AI coding agent a real Selenium WebDriver session to drive Chrome, Firefox, or Edge directly — structured element discovery for reliable locators, Chrome DevTools Protocol network/console tooling, and a standalone REST client, all in one server. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

Ask any Selenium engineer to name the single biggest source of flaky tests in their suite, and "waits" wins almost every time — not because waiting is conceptually hard, but because picking the *right* wait condition requires knowing exactly what the page is doing at that moment, and most of the time you're guessing.

You wait for an element to be visible, but the click handler attached to it hasn't finished initializing. You wait for a URL to change, but the page it changed to is still fetching the data it needs to render. You wait for text to appear, but it appears immediately in a loading state and then changes again half a second later. Every one of these produces a test that passes locally nine times out of ten and fails in CI often enough to erode trust in the whole suite.

This piece is about how **selenium-mcp**'s wait tooling — and the CDP-backed network tools sitting next to it — help an AI coding assistant actually diagnose which condition you need, instead of you iterating through `Thread.sleep()` values by hand.

## The full set of wait conditions available

`WaitTools` covers the standard explicit-wait surface, all with a configurable timeout (default 10s):

- **`waitForVisible`** / **`waitForClickable`** / **`waitForPresent`** / **`waitForInvisible`** — the core element-state waits.
- **`waitForTextPresent`** — waits until an element contains specific text, useful for loading-state-to-final-state transitions.
- **`waitForTitleContains`** / **`waitForUrlContains`** — page-level navigation waits.
- **`waitForPageLoad`** — waits until `document.readyState` is `"complete"`.
- **`waitForJsCondition`** — waits until an arbitrary JavaScript expression evaluates truthy, for the cases where none of the built-in conditions quite fit.
- **`waitForElementCount`** — waits until the number of elements matching a locator equals an expected count, handy for list/table rendering that happens incrementally.
- **`waitForAttributeToBe`** — waits until an element's attribute or DOM property equals an expected value.
- **`waitForNumberOfWindowsToBe`** — waits until the open window/tab count matches, for flows that open a new tab.

And critically, sitting in `NetworkTools` rather than `WaitTools` because it's CDP-backed: **`waitForNetworkIdle`**, which waits until there have been no in-flight HTTP requests for a continuous idle period. This is the condition that actually matches what most engineers mean when they add a sleep "to let things settle" — not an element state, but the absence of pending network activity.

## Why having an agent pick the condition is the actual value-add

Any experienced Selenium engineer knows this list already. The hard part was never knowing the API — it's diagnosing, for a *specific* flaky test, which condition actually corresponds to the real race. That diagnosis normally means reproducing the failure, watching it happen (ideally slowed down or recorded), and reasoning backward from what state the page was actually in when the assertion fired too early.

With an AI agent that has both `WaitTools` and `NetworkTools` available in the same session, that diagnosis becomes something you can actually delegate. A reasonable prompt: "This test fails intermittently at the `assertOrderConfirmed` step. Run it, capture the network log and console output around that assertion, and tell me what's actually still pending when it fails." The agent can drive the flow, pull `getNetworkLog` and `getConsoleLogs` scoped to that window, and tell you concretely — "there's a `POST /api/orders` still in flight when the assertion runs" — instead of you inferring it from a screenshot or a CI video recording.

Once the actual cause is known, the fix is usually obvious and small: swap a blind `Thread.sleep(2000)` for `waitForNetworkIdle`, or add `waitForElementCount` where the UI renders a list in multiple network round trips instead of one. The value isn't that the agent knows an API you didn't — it's that it can *run the reproduction and read the evidence* faster than you can context-switch into doing it yourself, especially for a test that only fails intermittently and needs a few reproduction attempts to catch in the act.

## A pattern worth adopting even without an AI agent in the loop

Independent of whether you're using an agent at all: if you're debugging a flaky Selenium test today, reach for `waitForNetworkIdle` and a network log before you reach for a longer sleep. A longer sleep treats the symptom and makes your suite slower for everyone, forever, whether or not the race condition is even still there next month. Actually looking at what's in flight when the assertion fires tells you the real cause, and usually the real fix is a more precise wait, not a bigger one.

Full parameter details for every wait condition and the network tools they pair with are on the [tool reference](https://selenium-mcp.xpathy.uk/docs/tools). If your suite has a flaky test sitting in a "known issue, don't have time" state right now, it's a reasonable one to try this against first.
