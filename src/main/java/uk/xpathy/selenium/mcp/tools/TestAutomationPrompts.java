package uk.xpathy.selenium.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP prompts for the jobs a test automation engineer brings an agent to this server for: writing
 * Page Objects and tests, fixing broken locators, debugging failing tests and fixing flaky ones.
 * Clients usually surface them as slash commands (e.g. {@code /mcp__selenium-mcp__debug-failing-test}
 * in Claude Code).
 * <p>
 * The engineers using this already have a Java/Selenium framework, so every prompt makes the agent
 * study and extend that framework first, and treats this server's tools as a way to inspect and
 * exercise the live application, never as something the generated tests call. Prompts only
 * produce text; they don't touch the browser, so they need no synchronization.
 */
@Component
@ToolGroup("prompts")
public class TestAutomationPrompts {

    static final String FRAMEWORK_FIRST = """
            ## Work inside the existing framework
            This workspace already has a Java + Selenium test framework. Read it before writing any code, and follow it:
            - How the driver is created and shared (base test class, driver factory/manager, ThreadLocal) and which \
            runner is used (TestNG, JUnit 5, Cucumber).
            - How Page Objects are written: `By` fields or `@FindBy`/PageFactory, the base page class, fluent return \
            types, naming and package layout.
            - Its wait helpers, assertion library (TestNG Assert, JUnit, AssertJ, Hamcrest), and how test data, \
            environments, credentials and reporting are handled.

            Reuse existing Page Objects, components and helpers, and extend them instead of duplicating them. Add no new \
            dependencies and no parallel framework layer. Code you write uses the framework's own driver and helpers: \
            the selenium-mcp tools are for inspecting and exercising the live application, never something the tests \
            call. If there is no framework yet, say so and propose a minimal base test + Page Object structure first.
            """;

    static final String LOCATOR_GUIDE = """
            # Choosing stable Selenium locators

            Prefer, in this order:
            1. A dedicated test attribute: `data-testid`, `data-test`, `data-qa`, `data-cy` \
            (css `[data-testid='place-order']`).
            2. A stable `id`. Not a generated one: `ember123`, `mat-input-4`, `react-select-2-input`, ids containing \
            hashes or counters change between builds or renders.
            3. `name`, for form fields.
            4. Accessible attributes: `aria-label`, `role` with its accessible name, a `<label for>` association, \
            `placeholder`.
            5. A short CSS selector anchored on the nearest stable ancestor, e.g. `form#login button[type='submit']`.
            6. XPath on visible text, e.g. `//button[normalize-space()='Place order']`, only when the text is stable \
            and the suite doesn't run in several locales.

            Avoid absolute XPaths (`/html/body/div[3]/...`), generated or styling classes (`css-1x2abc`, `sc-AxjAm`, \
            `col-md-6`), positional indexes unless the element really is the nth item of a list, and text that is \
            localized or comes from test data. Use the locator style the framework already uses.

            Locator strategies this server accepts: `id`, `name`, `css`, `xpath`, `className`, `linkText`, \
            `partialLinkText`, `tagName`.

            ## Verify every locator against the live page
            - `getPageElementsFiltered` with the candidate and `includeAncestors=true` shows every match, plus the \
            ancestor chain and `siblingIndex`/`siblingCount` needed to make it unique.
            - `countElements` must be exactly 1 for a single element (or the expected count for a list).
            - `isDisplayed` / `isEnabled` on the match a test will interact with: a hidden duplicate matched first is \
            a common cause of "element not interactable".
            - An element inside an iframe is only reachable after `switchToFrame...`; the Page Object has to switch too.
            - For dynamic content, check the locator in each state the test sees: loading, empty, populated, error.
            """;

    private static final String SAFE_ACTIONS = """
            Don't trigger irreversible or outward-facing actions in the live application (real payments, deleting \
            shared data, sending emails or messages) without asking me first.
            """;

    @McpPrompt(name = "generate-page-object", title = "Generate a Page Object",
            description = "Inspect a live page and write a Page Object for it in the style of the existing framework, "
                    + "with locators verified against the real DOM.")
    public GetPromptResult generatePageObject(
            @McpArg(name = "pageName", description = "Class name for the page or component, e.g. LoginPage or "
                    + "ShippingAddressForm.", required = true) String pageName,
            @McpArg(name = "url", description = "URL of the page. Omit to use the page already open in the browser, "
                    + "e.g. a test's browser joined with launchDebugBrowser.") String url,
            @McpArg(name = "scope", description = "Part of the page to model, e.g. 'the shipping address form'. Omit "
                    + "for the whole page.") String scope) {
        String text = """
                Create a Page Object named `%s` for %s%s.

                %s
                Look at one or two existing Page Objects and use them as the template. If a Page Object or component \
                for this page already exists, extend it instead of creating a new one.

                ## Steps
                1. Get the browser to the page. If it needs a login or earlier steps, follow the same path the \
                framework's existing helpers take. Let it settle: `waitForPageLoad`, plus `waitForNetworkIdle` for \
                single-page apps on Chrome/Edge.
                2. Discover the elements with `getPageElements`, or `getPageElementsFiltered` with \
                `includeDescendants=true` on the region in scope. Don't read the raw page source. Pick out what a test \
                acts on or asserts on: inputs, buttons, links, dropdowns, checkboxes, validation and error messages, \
                headings and confirmation text, table rows or list items, loading indicators.
                3. Choose and verify a locator for each, following the locator guide below.
                4. Write the class. Methods express user intent (`login(user, password)`, `chooseDelivery(option)`, \
                `getErrorMessage()`) rather than one click wrapper per element, unless the framework does it that way. \
                Navigation methods return the next Page Object if that's the framework's pattern. Wait for the \
                conditions you actually observed (a button disabled until the form is valid, a spinner that has to \
                disappear) using the framework's wait helpers. No `Thread.sleep`, and no assertions inside the Page \
                Object unless the framework puts them there.
                5. Exercise the main methods' steps in the live browser with `click`/`sendKeys` to confirm the \
                locators and waits work, e.g. fill the form and trigger its validation. %s
                6. Compile (e.g. `mvn -q test-compile`) and report: the file, each locator with why it's stable, and \
                any element without a stable hook, which is worth asking the developers to add a `data-testid` to.

                %s""".formatted(pageName, target(url), blank(scope) ? "" : ", covering only " + scope,
                FRAMEWORK_FIRST, SAFE_ACTIONS.strip(), LOCATOR_GUIDE);
        return prompt("Generate Page Object " + pageName, text);
    }

    @McpPrompt(name = "write-test", title = "Write a UI test",
            description = "Walk a scenario through the live application first, then write it as a test with the "
                    + "framework's Page Objects, waits and assertions, and run it until it passes reliably.")
    public GetPromptResult writeTest(
            @McpArg(name = "scenario", description = "What the test should verify, e.g. 'a registered user can "
                    + "reset their password from the login page'.", required = true) String scenario,
            @McpArg(name = "url", description = "Where the scenario starts. Omit to use the application's start URL "
                    + "from the framework's configuration.") String url,
            @McpArg(name = "testName", description = "Test class and/or method name to use, if you have one in "
                    + "mind.") String testName) {
        String text = """
                Write an automated UI test for this scenario: %s

                %s
                Find the existing tests closest to this scenario and mirror their structure (setup, test data, \
                assertions, tags or groups), and the Page Objects that already cover the pages involved.

                ## Steps
                1. Walk the scenario in the live browser before writing code, starting at %s. For each step: find \
                the element (verify the locator as in the guide below), perform the action, then work out which \
                condition shows the step has finished (element visible, URL changed, spinner gone, network idle). \
                That is the wait the test needs. Note what success looks like (text, URL, element state): those are \
                the assertions. Note what data the test depends on and how to create it (UI, API, existing fixtures). \
                %s
                2. Add missing elements and actions to Page Objects. Locators stay out of the test class.
                3. Write the test%s: arrange/act/assert, independent of other tests (its own data, no ordering \
                dependency), assertion messages that explain the failure, no fixed sleeps, and credentials and test \
                data from the framework's configuration rather than literals.
                4. Run only this test (e.g. `mvn test -Dtest=Class#method`, or the runner's equivalent) and fix it \
                until it passes. If it fails, inspect the live page instead of guessing. Run it a second time to check \
                it is stable.
                5. Report: files changed, how the steps map to assertions, and any application behaviour that looked \
                like a bug.

                %s""".formatted(scenario, FRAMEWORK_FIRST, blank(url) ? "the application's start URL" : url,
                SAFE_ACTIONS.strip(), blank(testName) ? "" : " as `" + testName + "`", LOCATOR_GUIDE);
        return prompt("Write a UI test: " + scenario, text);
    }

    @McpPrompt(name = "fix-broken-locator", title = "Fix a broken locator",
            description = "Find out on the live page why a locator fails (changed, ambiguous, hidden, in an iframe, "
                    + "covered, or too early) and fix the actual cause.")
    public GetPromptResult fixBrokenLocator(
            @McpArg(name = "locator", description = "The failing locator as it appears in the code, e.g. "
                    + "By.id(\"submitBtn\") or a @FindBy annotation.", required = true) String locator,
            @McpArg(name = "error", description = "The exception message, e.g. NoSuchElementException or "
                    + "ElementNotInteractableException text.") String error,
            @McpArg(name = "url", description = "URL of the page the element should be on. Omit to use the page "
                    + "already open in the browser.") String url) {
        String text = """
                The locator `%s` is failing%s.

                ## Steps
                1. Find it in the code: which Page Object and methods use it, and what element it's meant to be \
                (field and method names, how the tests use it).
                2. Get the browser to %s. If reaching it takes several steps, follow the same steps the failing test \
                takes.
                3. Diagnose with `getPageElementsFiltered` on the current locator with `includeAncestors=true`:
                   - **0 matches**: an attribute changed, the element was renamed or restructured, it isn't rendered \
                yet, it's inside an iframe (look for `iframe` elements), or it's in another window. Find the intended \
                element by its text, label, role or neighbours instead.
                   - **Several matches**: work out which one the test means. Try `visibleOnly`: a hidden duplicate \
                (responsive layout, modal template) matched first is a common cause.
                   - **1 match but the action fails**: check `isDisplayed`/`isEnabled`, and `takeScreenshot` to look \
                for something covering it (cookie banner, spinner, toast, sticky header). Check whether it only \
                becomes ready after a request finishes.
                4. Fix the actual cause: a new stable locator, a frame switch, a wait for the right condition, or \
                dismissing the overlay. Don't cover it up with an index or a sleep.
                5. Verify the fix on the live page (`countElements` is 1, and the interaction succeeds once). Search \
                for other uses of the same or a similar locator and fix them the same way.
                6. Report the cause, the old and new locator, and why the new one is more stable.

                %s
                %s""".formatted(locator, blank(error) ? "" : " with:\n\n```\n" + error.strip() + "\n```",
                blank(url) ? "the page where the element should be (if no browser is open, ask me which page)"
                        : url, FRAMEWORK_FIRST, LOCATOR_GUIDE);
        return prompt("Fix broken locator " + locator, text);
    }

    @McpPrompt(name = "debug-failing-test", title = "Debug a failing test",
            description = "Run a failing test inside a browser this server is attached to, inspect the real failure "
                    + "state, tell a test bug from an application bug, fix the test and confirm.")
    public GetPromptResult debugFailingTest(
            @McpArg(name = "test", description = "The failing test, e.g. CheckoutTest#placeOrder or a Cucumber "
                    + "scenario name.", required = true) String test,
            @McpArg(name = "error", description = "Failure message or stack trace excerpt. Omit and the test will be "
                    + "run to get it.") String error) {
        String text = """
                `%s` is failing%s. Find the root cause in the real browser state where it fails, fix it, and confirm \
                the fix.

                ## Steps
                1. Read the test, the Page Objects and step definitions it uses, and the failure%s. Identify the \
                failing step.
                2. Get into the failure state. On Chrome/Edge, run the test inside a browser this server is connected to:
                   - Check that the framework's driver setup can join a running browser through \
                `ChromeOptions.setExperimentalOption("debuggerAddress", ...)` (EdgeOptions works the same way) and \
                skips `driver.quit()` in that mode, e.g. switched on by `-Ddebugger.address=127.0.0.1:9222`. If it \
                can't, propose that small opt-in change (default behaviour unchanged) and ask before making it.
                   - Call `launchDebugBrowser`. If timing or a backend problem is possible, call \
                `startNetworkCapture` and `startConsoleCapture` now, because capture only records from the moment it \
                starts.
                   - Run only this test against it, e.g. `mvn test -Dtest=... -Ddebugger.address=127.0.0.1:9222`. \
                Leave the browser alone while the test runs. The test and this server take turns, never drive at once.
                   If attaching isn't possible (Firefox, or no change allowed), open a browser and repeat the test's \
                steps up to the failing one with the tools.
                3. Inspect the failure state: `getCurrentUrl` and `isAlertPresent`, `getPageElementsFiltered` on the \
                failing locator with `includeAncestors=true`, `takeScreenshot`, `getNetworkLog` (look for 4xx/5xx \
                responses), `getConsoleLogs` with type `error`, and `executeScript` for application state the DOM \
                doesn't show.
                4. Classify the cause:
                   - **Test bug**: stale locator, wrong or missing wait, wrong frame or window, wrong test data \
                assumption, dependency on another test.
                   - **Environment or data**: missing or expired test data or account, configuration, a third-party \
                outage.
                   - **Application bug**: a 5xx response, a JavaScript error, behaviour that contradicts the \
                requirement. Don't change the test to make an application bug pass. Report it with the evidence \
                (request and response, console error, screenshot).
                5. For a test bug, fix the Page Object or test following the framework's conventions. Then carry on \
                through the rest of the test's flow in the live browser from where it stopped, so the next broken \
                step turns up before the rerun, not after.
                6. Rerun the test normally to confirm. `closeBrowser` only disconnects from a browser joined this way; \
                it keeps running.
                7. Report: the root cause with evidence, its classification, the diff, and anything for the \
                developers.

                %s""".formatted(test, blank(error) ? "" : " with:\n\n```\n" + error.strip() + "\n```\n",
                blank(error) ? " (run it once to get the failure)" : "", FRAMEWORK_FIRST);
        return prompt("Debug failing test " + test, text);
    }

    @McpPrompt(name = "fix-flaky-test", title = "Fix a flaky test",
            description = "Reproduce an intermittent failure with network and console evidence, find the race, and "
                    + "replace sleeps or retries with a wait on the condition that actually signals readiness.")
    public GetPromptResult fixFlakyTest(
            @McpArg(name = "test", description = "The flaky test, e.g. SearchTest#filtersResults.",
                    required = true) String test,
            @McpArg(name = "symptom", description = "How it fails, e.g. 'StaleElementReferenceException on the "
                    + "results table, about 1 run in 5, mostly in CI'.") String symptom) {
        String text = """
                `%s` is flaky%s. Find the race and replace it with a wait on the condition that actually signals \
                readiness, not a longer sleep or a retry.

                ## Steps
                1. Read the test and its Page Objects. List every `Thread.sleep`, implicit wait, fixed timeout and \
                retry wrapper, and every action followed straight away by an assertion or another action on content \
                that loads asynchronously.
                2. Reproduce with evidence. Open Chrome or Edge (network and console capture need CDP), call \
                `startNetworkCapture` and `startConsoleCapture`, and walk through the test's steps with the tools. \
                Straight after each suspect action, check what is still happening: `getPendingRequestCount`, \
                `getNetworkLog` (which request the UI is waiting for and how long it takes), whether a spinner or \
                overlay is showing, whether the element gets re-rendered (stale references), whether a list fills in \
                over several batches. CI is usually slower than a workstation: `setNetworkConditions` with some \
                latency (e.g. 500 ms) often makes the race happen every time. Call `clearNetworkConditions` afterwards.
                3. Choose the precise wait for each race, using the framework's own wait helpers:
                   - element appears or becomes usable: visibility or clickability
                   - loading indicator: invisibility of the spinner
                   - content updated in place: text or attribute to be
                   - list rendered in batches: number of elements to be
                   - navigation: URL or title contains
                   - a request must finish: the DOM result of that request, or a network-idle wait if the framework \
                has one and the suite only runs on Chrome/Edge
                   - StaleElementReferenceException: wait for the re-render to finish and look the element up again \
                by its `By`, rather than holding on to a `WebElement`
                   - an application readiness signal (e.g. `jQuery.active == 0`, Angular stable) through a custom \
                JavaScript condition, where that's what the app exposes
                4. Apply the waits and remove the sleeps and retries they replace. Keep the framework's default \
                timeouts unless a measurement says otherwise.
                5. Run the test repeatedly (5 to 10 times, with throttled network too if you can) to show it's stable.
                6. Report each race: the step, the evidence (what was still pending), and the fix.

                %s""".formatted(test, blank(symptom) ? "" : ": " + symptom.strip(), FRAMEWORK_FIRST);
        return prompt("Fix flaky test " + test, text);
    }

    @McpPrompt(name = "review-page-object", title = "Audit a Page Object's locators",
            description = "Check every locator in a Page Object against the live page, flag broken, ambiguous and "
                    + "brittle ones, and replace them with verified stable locators.")
    public GetPromptResult reviewPageObject(
            @McpArg(name = "pageObject", description = "Page Object class or file, e.g. CheckoutPage.",
                    required = true) String pageObject,
            @McpArg(name = "url", description = "URL of the page. Omit to navigate the way the framework does, or to "
                    + "use the page already open.") String url) {
        String text = """
                Audit every locator in `%s` against the live application and fix the broken and brittle ones.

                ## Steps
                1. Read the class and list every locator: `By` fields, `@FindBy`/`@FindBys`/`@FindAll`, inline \
                `By.*` calls in methods, and locators built from strings.
                2. Get the browser to %s. Some elements only exist in certain states (validation errors, empty \
                results, dialogs). Bring those states about where it's safe, so they can be checked too.
                3. Check each locator with `countElements` and `getPageElementsFiltered`, and build a table with one \
                status per locator:
                   - **OK**
                   - **BROKEN**: no match
                   - **AMBIGUOUS**: several matches where one is expected (note whether the first one is hidden)
                   - **BRITTLE**: works, but relies on an absolute XPath, a generated id or class, an index or \
                localized text
                   - **NOT CHECKED**: the state it belongs to couldn't be reached (say why)
                4. For every locator that isn't OK, propose a replacement and verify it on the live page. Show the \
                table, then apply the replacements, keeping field and method names so the tests don't change.
                5. Also flag: locators duplicated across Page Objects, `Thread.sleep` in the class, and methods that \
                act on an element without waiting for the state they need.

                %s
                %s""".formatted(pageObject,
                blank(url) ? "the page this Page Object models (navigate the way the framework does, or use the page "
                        + "already open)" : url, FRAMEWORK_FIRST, LOCATOR_GUIDE);
        return prompt("Audit Page Object " + pageObject, text);
    }

    @McpPrompt(name = "api-ui-test", title = "Write an API + UI test",
            description = "Write a test that sets up and checks data through the API and drives only the behaviour "
                    + "under test through the UI, reusing the framework's REST Assured layer.")
    public GetPromptResult apiUiTest(
            @McpArg(name = "scenario", description = "What the test should verify, e.g. 'an order created through "
                    + "the API shows up in the customer's order history'.", required = true) String scenario,
            @McpArg(name = "apiBaseUrl", description = "Base URL of the API in the test environment. Omit to use the "
                    + "one in the framework's configuration.") String apiBaseUrl) {
        String text = """
                Write a test for this scenario: %s

                Use the API to set up preconditions and to check backend state, and the UI only for the behaviour \
                under test.

                %s
                Also find the framework's API layer: REST Assured (or other) clients, request specifications, \
                authentication and token handling, payload models or builders, and base URLs per environment. Reuse \
                it.

                ## Steps
                1. Split the scenario: preconditions (create a user, an order, reference data) and backend checks go \
                through the API; what the user actually does goes through the UI. Keep the UI part as short as the \
                scenario allows.
                2. Explore the API with `httpGet`/`httpPost`/`httpPut`/`httpDelete` against %s: endpoints, required \
                headers and authentication, request and response shapes, and how to clean up. Test environments only, \
                never production.
                3. Explore the UI part in the browser. `startNetworkCapture` while doing the steps by hand shows which \
                API calls the UI makes, which often gives you the setup endpoints and payloads. Verify locators as in \
                the guide below.
                4. Write the test: API setup with data unique to each run, UI steps through Page Objects, assertions \
                on the UI (and on backend state through the API where that adds confidence), and cleanup that runs \
                even when the test fails.
                5. Run it until it passes, run it again to show it's repeatable, and report.

                %s
                %s""".formatted(scenario, FRAMEWORK_FIRST,
                blank(apiBaseUrl) ? "the base URL from the framework's configuration" : apiBaseUrl,
                SAFE_ACTIONS.strip(), LOCATOR_GUIDE);
        return prompt("API + UI test: " + scenario, text);
    }

    private static String target(String url) {
        return blank(url)
                ? "the page already open in the browser (check it with getCurrentUrl; if no browser is open, ask me "
                        + "which page)"
                : "the page at " + url + " (call openBrowser first if no browser is open)";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static GetPromptResult prompt(String description, String text) {
        PromptMessage message = PromptMessage.builder(Role.USER, TextContent.builder(text).build()).build();
        return GetPromptResult.builder(List.of(message)).description(description).build();
    }
}
