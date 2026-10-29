# I Stopped Reproducing Failed Selenium Tests. Now the AI Debugs Them in the Browser They Failed In

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server built for test automation engineers working in **Java, Selenium, and REST Assured**. It gives an AI coding agent a real Selenium WebDriver session to drive Chrome, Firefox, or Edge directly — structured element discovery for reliable locators, Chrome DevTools Protocol network/console tooling, and a standalone REST client, all in one server. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.voltajeba.com](https://selenium-mcp.voltajeba.com/)

I write and maintain Selenium tests in Java for a living, and I can tell you where most of my debugging time goes. It isn't fixing the test. It's getting back to the place where the test broke.

A checkout test fails on step twelve with a `NoSuchElementException`. The driver quits in `@AfterMethod`, the browser closes, and everything useful about the failure goes with it. So I start over: run the test again with a breakpoint, or click through login, search, add to cart, shipping and payment by hand, until I'm finally looking at the page that broke. *Then* I open DevTools.

AI coding assistants didn't change that much at first. I'd paste the stack trace, the agent would guess at a new locator, I'd run it, and we'd go round again. The agent never saw the page that actually failed.

With selenium-mcp's debugger mode, it does. Here's the setup I use now. It's one small change to my test base, and after that, debugging is a single prompt.

## The one change: let the test base attach to an existing browser

Chrome and Edge can run with a remote-debugging port open (`127.0.0.1:9222` by default). Any WebDriver can attach to a browser like that instead of launching its own, using ChromeDriver's `debuggerAddress` option. That's what lets my test and the AI agent share one browser.

So I taught my `BaseTest` two modes: the regular mode it always had, and a debug mode that attaches to an already-running browser and **leaves it open** at the end:

```java
public abstract class BaseTest {

    // e.g. mvn test -Dtest=CheckoutTest -Ddebugger.address=127.0.0.1:9222
    private static final String DEBUGGER_ADDRESS = System.getProperty("debugger.address");

    protected WebDriver driver;

    @BeforeMethod
    public void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (DEBUGGER_ADDRESS != null) {
            // Debug mode: drive the browser that's already running, don't launch a new one
            options.setExperimentalOption("debuggerAddress", DEBUGGER_ADDRESS);
        }
        driver = new ChromeDriver(options);
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        if (DEBUGGER_ADDRESS == null) {
            driver.quit();
        }
        // Debug mode: leave the browser exactly where the test stopped
    }
}
```

That's the whole change. Without the property, nothing is different: CI and everyone else on the team run exactly as before. With it, the test runs inside whatever Chrome is listening on that port, and when it fails, the browser stays on the failing page.

(I use TestNG here. With JUnit 5, the same thing goes in `@BeforeEach`/`@AfterEach`.)

## Then I just ask the agent to debug it

With selenium-mcp added to Claude Code (`npx -y @voltajeba/selenium-mcp`, [setup walkthrough here](07-getting-started-claude-code-maven-tutorial.md)), my prompt is roughly this:

> "`CheckoutTest.placeOrder` is failing. Launch a debug Chrome, run the test against it with `-Ddebugger.address=127.0.0.1:9222`, and when it fails, take over the browser, find out why, fix the test, and finish the rest of the flow to check it."

Here's what the agent does from there.

**1. It starts Chrome in debugging mode.** It calls `launchDebugBrowser`, which opens Chrome with remote debugging on port 9222 and a dedicated profile folder, and connects to it. That dedicated profile matters: recent Chrome versions refuse remote debugging on your everyday profile, and the tool handles that for you.

**2. It runs my test in that Chrome session.** It runs `mvn test -Dtest=CheckoutTest#placeOrder -Ddebugger.address=127.0.0.1:9222` from the terminal. My `BaseTest` sees the property, attaches to the browser the agent just launched, and runs the test there. I can watch it happen in the visible window. The agent stays hands-off while the test runs.

**3. The test fails, and the browser stays open.** Because of the debug branch in `@AfterMethod`, the browser isn't closed. It's sitting on the payment page with whatever state the test left behind: the logged-in user, the full cart, the half-filled form.

**4. The agent takes control of the browser where the test failed.** It's still connected to that same browser, so it inspects the actual failure state instead of guessing from the stack trace:

- `getPageElementsFiltered` with the locator that failed. Most of the time this is the whole answer: the button's `id` changed, or there are now two matching buttons and the first one is hidden, or it moved into an iframe. The agent sees the real element with its ancestor chain and builds a locator that's unique ([how that works](02-stop-guessing-css-selectors-ai-agents.md)).
- `takeScreenshot` to see the visual state, such as a cookie banner or a spinner sitting on top of the button.
- `executeScript` to check application state the DOM doesn't show directly.
- `startNetworkCapture` / `startConsoleCapture`, then retry the failing step and read `getNetworkLog` / `getConsoleLogs`, if it looks like a timing problem ([more on that here](10-waits-flakiness-ai-assisted-debugging.md)).

**5. It fixes the test and finishes the rest of the flow.** Once it knows the cause, it edits my Page Object (a new locator, or a proper wait instead of the one that raced). Then it carries on from where the test stopped: it clicks the button with the corrected locator and works through the remaining steps of the flow in the live browser (confirm the order, check the confirmation page). That way it finds out whether step thirteen has a problem too, *before* I rerun anything. That's the part that used to cost me the most: fix one step, rerun the whole test, fail on the next one, repeat.

**6. It reruns the test to confirm the fix.** With the fix in place, it runs the test again, in debug mode against the same browser or normally, and reports back with the diff.

What I review at the end is a small change to a Page Object, with an explanation grounded in what the page really looked like. Not a guess.

## A few rules I learned

**Take turns, don't share the wheel.** My test and the agent are both connected to the same browser, and both *could* send commands at once. They shouldn't. While `mvn test` is running, the agent waits; once the test has stopped, the agent drives. Two drivers clicking in one tab produces failures far stranger than the one you started with.

**Closing the agent's session doesn't close the browser.** When the agent is done, `closeBrowser` only disconnects it, because a browser it attached to keeps running. I can still poke around in that window myself, or rerun the test against it.

**Network and console capture only record from the moment they start.** Requests my test made before the agent started capture aren't in the log. If I already suspect a timing problem, I tell the agent to start capture *before* it runs the test.

**Chrome and Edge only.** This relies on Chromium's remote-debugging endpoint. Firefox has no equivalent that Selenium supports reliably. My suite is cross-browser, but I debug on a Chrome run, and almost every failure that isn't engine-specific reproduces there.

**If you already have a debug Chrome running**, maybe started by hand with `--remote-debugging-port=9222 --user-data-dir=<some folder>`, the agent can join it with `connectToBrowser` instead of launching a new one. `launchDebugBrowser` does the same automatically if it finds something already listening on the port.

## Why this changed how I work

The slow part of debugging a UI test was never the fix. It was getting back to the broken state, and then fixing step after step one rerun at a time. With a two-mode `BaseTest` and selenium-mcp's debugger tools, the test drives itself to the failure, and the agent picks up in the same browser with everything still there. It inspects the real page, fixes the code, and walks through the rest of the flow before I rerun anything.

My part is down to one prompt and a code review.

Full parameter details for `launchDebugBrowser`, `connectToBrowser` and the rest are in the [tool reference](https://selenium-mcp.voltajeba.com/docs/tools). If you've got a red test sitting in your suite right now, it's a good one to try this on.
