# Your Selenium Test Isn't Flaky. Your Backend Is Just Slower Than Your `Thread.sleep()`.

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server that gives an AI coding assistant — Claude Code, Cursor, Claude Desktop — a real Selenium WebDriver session to drive Chrome, Firefox, or Edge directly, built specifically for test automation engineers working in **Java, Selenium, and REST Assured**. Beyond structured element discovery for reliable locators, it exposes Chrome DevTools Protocol tooling for network capture, mocking, and console log debugging, plus a standalone REST client for API testing. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

Every Selenium suite has that one test. It passes locally, fails in CI, passes again if you re-run it, fails again next Tuesday for no obvious reason. Nine times out of ten, when you actually dig in, the cause isn't your locator or your assertion — it's timing. Some XHR call the click triggered hadn't finished when your test tried to read the result.

The usual fixes are all some flavor of unsatisfying: a `Thread.sleep(2000)` that either wastes time or isn't long enough depending on the day, or an explicit wait on a UI element that only *implies* the network call behind it has settled. Neither one tells you what's actually happening on the wire.

Selenium 4 quietly shipped access to the Chrome DevTools Protocol — the same low-level API Chrome's own DevTools panel uses — which means you can actually see the network traffic, not just guess at it from what rendered. Most Selenium suites never touch it, because wiring up `HasDevTools` and `NetworkInterceptor` by hand is enough ceremony that people reach for `Thread.sleep()` instead. I wired it into **selenium-mcp** so an AI coding assistant can use it on your behalf, as part of writing or debugging a Java Selenium test.

## What `NetworkTools` actually gives you

This group is built on Selenium 4's CDP support and works against Chrome and Edge (Firefox doesn't expose CDP the same way — every tool in this group returns a clear explanatory message instead of throwing if you call it on an unsupported browser, so it fails loud and readable, not silently).

The tool that solves the flaky-wait problem directly is **`waitForNetworkIdle`** — it waits until there have been no in-flight HTTP requests for a continuous idle period. That's the actual condition you meant when you wrote a fixed sleep after a click that triggers an XHR call. Pair it with **`getPendingRequestCount`** when you want to assert *why* a wait is still blocking, instead of just watching it time out.

Beyond that:

- **`startNetworkCapture`** / **`stopNetworkCapture`** — passively record method, URL, headers, bodies, status, and timing for every request, optionally filtered by a URL regex, so you're not drowning in analytics beacons and font requests.
- **`getNetworkLog`** / **`clearNetworkLog`** — pull captured entries back as JSON, filterable by URL pattern or method, most recent last.
- **`mockResponse`** / **`clearMockResponses`** — stub a response for requests matching a URL regex (status, headers, body) without touching the real network. Genuinely useful for testing a UI's error-handling path when you can't easily make the real backend return a 500 on demand.
- **`blockRequests`** / **`clearBlockedRequests`** — block requests matching a URL regex outright (immediate 403). Handy for stripping third-party ad/tracker calls that add flakiness to a test with nothing to do with what you're actually testing.
- **`setNetworkConditions`** / **`getNetworkConditions`** / **`clearNetworkConditions`** — simulate offline mode, added latency, or throttled throughput, for testing how your app behaves on a bad connection without needing an actual bad connection.
- **`setBasicAuthCredentials`** — register HTTP Basic auth so a login popup never blocks navigation in the first place.
- **`startConsoleCapture`**, **`getConsoleLogs`** / **`clearConsoleLogs`** — capture browser console output (`console.log`/`warn`/`error`), filterable by type. When a test fails and you can't tell if it's a UI bug or a JS exception, this is usually the fastest way to find out.

## Where this actually changes how you debug

Picture the standard flaky-test investigation: a test fails intermittently in CI, passes locally, and you can't reproduce it on demand. Normally that's an afternoon of adding sleeps, staring at video recordings, and guessing.

With an AI agent driving this toolset, you can instead ask it to reproduce the flow, capture the network log around the failing assertion, and check console output for the same window — in one pass, from inside your chat. If the real cause is a request that's still pending when the assertion runs, `getNetworkLog` and `getPendingRequestCount` show that directly, instead of you inferring it from a screenshot. If it's an uncaught JS error the UI silently swallowed, `getConsoleLogs` surfaces it. Either way, you're fixing the actual cause instead of adding another `Thread.sleep()` and hoping.

## Why this matters specifically for a Java/Selenium suite

This isn't a capability unique to browser automation in general — Playwright has had first-class network interception for a while, which is part of why Playwright suites are often less flaky than comparable Selenium ones. The gap has always been on the Java/Selenium side: the capability exists in Selenium 4, but almost nobody uses it because there was no lightweight way to reach for it mid-debugging-session. `selenium-mcp` doesn't add a new capability to Selenium — it makes the one that was already there, and mostly ignored, actually usable from an AI coding assistant, in the same Java toolchain your suite already runs on.

Full parameter details for every tool in this group are on the [tool reference](https://selenium-mcp.xpathy.uk/docs/tools). If you're chasing a flaky test right now, `waitForNetworkIdle` is the one worth trying first — it's usually a straight swap for whatever `Thread.sleep()` you were about to write.
