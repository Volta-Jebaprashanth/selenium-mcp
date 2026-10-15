# Test plan: `fix/field-feedback` branch, real-browser check via MCP

This is for a Claude Code session that exercises the new selenium-mcp build **through its MCP tools**, against a real browser. Unit tests and a Java-level Chrome integration test have already passed (`mvn test -Dselenium.it=true`). What hasn't been verified is the MCP tool layer end to end, plus the paths listed under each section.

## Ground rules

- **Use only the `selenium-mcp-dev` tools** (`mcp__selenium-mcp-dev__*`). This project's `.mcp.json` points them at `dist/selenium-mcp-dev.jar`. The older `selenium-mcp` server (a jar in Downloads) may also be loaded and doesn't have these fixes. Don't use it.
- **Don't change source code during the run.** Record what happens. If something fails, note the exact tool output and keep going.
- **Record actual output verbatim** in `dist/test-output/RESULTS.md` (format at the end). Save screenshots under `dist/test-output/`.
- "One line" below means the tool result has no newline, and none of `Build info`, `System info`, `Driver info`, `Capabilities`, `Session ID`, `(Session info:`.
- To read page state, use `executeScript` with `return JSON.stringify(window.state)`. The fixture page records every click, keystroke, hover etc. into `window.state`.
- Call `refresh` before each numbered test in section D, so `window.state` starts clean.

## Setup

1. Confirm `git branch --show-current` is `fix/field-feedback` and `dist/selenium-mcp-dev.jar` exists. If `selenium-mcp-dev` tools aren't available, stop and tell the user to approve the project MCP server (`/mcp`).
2. Start the fixture server in the background, then leave it running:
   `python -m http.server 8765 --bind 127.0.0.1 --directory "dist/test-fixture"`
3. `mkdir -p dist/test-output`
4. `openBrowser` with `browser=chrome`, `windowSize=1280x800`. Not headless, so the user can watch.
5. `navigate` to `http://127.0.0.1:8765/index.html`.
6. `executeScript`: `return navigator.userAgent`. Record the Chrome version. The fixes were built for Chrome 153–155.

Fixture layout (in `dist/test-fixture/index.html`):

| Selector | Matches, in DOM order |
|---|---|
| `.b` | `#b-hidden` (display:none), `#b-first`, `#b-second`, `#b-third` |
| `.field` | `#f-hidden` (display:none), `#f-first`, `#f-second` |
| `.lnk` | hidden link (`data-x="hidden"`), "link one" (`data-x="one"`), "link two" (`data-x="two"`) |
| `.g` | `#g-first`, `#g-second` (hover, double-click and right-click targets) |
| `.item` | `#item-0`, `#item-1`, `#item-2`, after a 3000px-tall block |
| `#do-fetch` | fetches `api.json` and stores the result in `state.fetched` |
| `#do-error` | calls `console.error('fixture-console-error')` |
| `#panel` | 200px-tall box that scrolls internally (1500px of content) |
| `#bottom-marker` | lime bar at the very bottom of the page |

---

## A. Error messages are one line (fix #1)

| ID | Call | Expected |
|---|---|---|
| A1 | `executeScript` script=`return undefined.click()` | Starts with `Script error: javascript error:`, contains `reading 'click'`, one line |
| A2 | `click` css `#does-not-exist` | `Failed to click element: no such element: ...`, one line |
| A3 | `getText` xpath `//[` | Invalid-selector error, one line |
| A4 | `waitForVisible` css `#b-hidden`, timeout 2s | Timeout error, one line |
| A5 | `switchToFrameByIndex` index 5 | No-such-frame error, one line |
| A6 | `click` locatorType `bogus`, value `x` | `Failed to click element: Unsupported locator type: bogus` |
| A7 | `acceptAlert` (no alert open) | No-alert error, one line |

## B. `executeScript`: timeout and result format (fix #3)

| ID | Call | Expected |
|---|---|---|
| B1 | script=`return new Promise(r => setTimeout(() => r('done'), 2000))`, timeoutMs=500 | `Script error: ...` script timeout, one line |
| B2 | same script, no timeoutMs | `done`. Shows the default timeout was restored after B1 |
| B3 | script=`return new Promise(r => setTimeout(() => r('long'), 35000))`, timeoutMs=45000 | `long`. Runs past the old 30s limit. If the **MCP client** times out first, record that separately: it's a client limit, not this fix |
| B4 | same as B3 but no timeoutMs | Script-timeout error after ~30s |
| B5 | any script, timeoutMs=0 | `timeoutMs must be greater than 0.` |
| B6 | `return {a: 1, b: [1, 'x'], c: null}` | `{"a":1,"b":[1,"x"],"c":null}` (JSON, not `{a=1, ...}`) |
| B7 | `return [document.getElementById('b-first'), 2]` | A short JSON array: first item is a string describing the element, second is `2`. Must return quickly with no huge output |
| B8 | `return document.title` | `selenium-mcp fixture` |
| B9 | `return null` | `(null)` |

## C. Screenshots (fix #2)

Check each saved PNG's pixel size with Python: `python -c "import struct,sys;d=open(sys.argv[1],'rb').read(24);print(struct.unpack('>II',d[16:24]))" <file>`. Then view it with the Read tool.

| ID | Call | Expected |
|---|---|---|
| C1 | `takeScreenshot` filePath=`<abs>/dist/test-output/viewport.png` | `Viewport screenshot saved to ...`. Height is about the viewport (under 1000px); lime bottom bar NOT visible |
| C2 | `takeScreenshot` filePath=`<abs>/dist/test-output/full.png`, fullPage=true | `Full-page screenshot saved to ...`. Height over 3000px; lime "bottom of page" bar visible at the bottom |
| C3 | Inspect `full.png` around `#panel` | The panel shows only its first 200px. This is the **expected, documented limitation** for content that scrolls inside its own container. Record it, don't count it as a failure |
| C4 | `takeElementScreenshot` css `.b`, index=1, visibleOnly=true, filePath=`<abs>/dist/test-output/b-second.png` | Saved. Image shows the button labelled `second` |
| C5 | `takeElementScreenshot` css `.b`, index=7, filePath=`<abs>/dist/test-output/none.png` | One-line no-such-element error; no file written |
| C6 | `takeScreenshot` fullPage=true, filePath relative or into a missing folder (e.g. `Z:/nope/x.png`) | One-line error, no crash |
| C7 (optional) | Ask the user for the real "learning path" URL they had trouble with. `navigate` there, then run `takeScreenshot` with fullPage=true | Report whether the content is captured. If it's in an inner scroll container, it'll be cut off as in C3. Note which case it is |

Don't call `takeScreenshot` without `filePath` and with fullPage=true. The base64 of a full page is very large and wastes tokens.

## D. `index` / `visibleOnly` (fix #7)

`refresh` before each. Check with `executeScript` `return JSON.stringify(window.state)`.

| ID | Call | Expected |
|---|---|---|
| D1 | `click` css `.b` (no index) | Fails: the first match is hidden (element-not-interactable), one line. This is the original problem |
| D2 | `click` css `.b`, visibleOnly=true | `state.clicked` = `b-first` |
| D3 | `click` css `.b`, index=2, visibleOnly=true | `b-third` |
| D4 | `click` css `.b`, index=2 (visibleOnly omitted) | `b-second` (index counts the hidden match) |
| D5 | `click` css `.b`, index=9 | One-line no-such-element error |
| D6 | `click` css `.b`, index=-1 | Error mentioning `index must be 0 or greater` |
| D7 | `sendKeys` css `.field`, text=`hello`, index=1, visibleOnly=true | `state.typed` = `{"f-second":"hello"}` |
| D8 | Then `sendKeys` same locator, text=`bye`, clearFirst=true, index=1, visibleOnly=true | `f-second` value is `bye`. Check with `return document.getElementById('f-second').value` |
| D9 | `getAttribute` css `.lnk`, attributeName=`data-x`, index=0, visibleOnly=true | `one` |
| D10 | `getAttribute` css `.lnk`, attributeName=`data-x`, index=0 | `hidden` |
| D11 | `getText` css `.lnk`, index=1, visibleOnly=true | `link two` |
| D12 | `hover` css `.g`, index=1 | `Hovered over element.`; `state.hovered` = `g-second` |
| D13 | `doubleClick` css `.g`, index=1 | `Double-clicked element.`; `state.dblclicked` = `g-second` |
| D14 | `rightClick` css `.g`, index=0 | `Right-clicked element.`; `state.rightclicked` = `g-first` |
| D15 | `scrollIntoView` css `.item`, index=2 | `Scrolled element into view.`. Then `executeScript` `const r=document.getElementById('item-2').getBoundingClientRect(); return r.top>=0 && r.bottom<=innerHeight` returns `true` |
| D16 | `click` xpath `//button[contains(@class,'b')]`, index=1, visibleOnly=true | `b-second`. Index works with xpath too |

## E. Init scripts (fix #6)

| ID | Call | Expected |
|---|---|---|
| E1 | `addInitScript` script=`window.__helper = () => 'helper-ok';` | `Init script registered with id <ID1>.` Then `executeScript` `return window.__helper()` gives `helper-ok` (runNow defaults to true) |
| E2 | `refresh`, then `return window.__helper()` | `helper-ok` (survives reload) |
| E3 | `navigate` to `http://127.0.0.1:8765/api.json`, then `return typeof window.__helper` | `function` (survives navigation). `navigate` back to `index.html` afterwards |
| E4 | `addInitScript` script=`window.__second = 1;`, runNow=false. Then `return typeof window.__second` | `undefined` before reload. After `refresh`, `return window.__second` gives `1` |
| E5 | `removeInitScript` id=`<ID1>`, then `refresh` | `Removed init script <ID1>.`. `typeof window.__helper` is `undefined`; `window.__second` is still `1` |
| E6 | `removeInitScript` with no id, then `refresh` | `Removed 1 init script(s).`. `typeof window.__second` is `undefined` |
| E7 | `removeInitScript` id=`nope` | `No init script registered with id nope` |
| E8 | Practical use: `addInitScript` script=`window.__errors=[]; const o=console.error; console.error=(...a)=>{window.__errors.push(a.join(' ')); o(...a);};`. Then `refresh`, `click` css `#do-error`, `return window.__errors` | `["fixture-console-error"]`. Clean up with `removeInitScript` (no id) |
| E9 (limitation) | `addInitScript` `window.__tabcheck=1;`, `openNewTab`, `switchToWindow` to the new tab, `navigate` to the fixture, `return typeof window.__tabcheck` | Expected `undefined`: init scripts are per tab. Record it, close the tab, switch back, `removeInitScript` (no id) |

## F. Console and network capture on current Chrome (fixes #4, #5)

These failed on Chrome 155 with the old Selenium 4.46 (`no-op implementation of the CDP`). They should now work.

| ID | Call | Expected |
|---|---|---|
| F1 | `startConsoleCapture`, `click` `#do-error`, wait ~1s (`executeScript` `return new Promise(r=>setTimeout(r,1000))`), `getConsoleLogs` type=`error` | JSON array with an entry whose message contains `fixture-console-error` |
| F2 | `startNetworkCapture` urlPattern=`api.json`, `click` `#do-fetch`, wait ~1s, `getNetworkLog` | An entry for `.../api.json` with status 200 and a responseBody containing `"ok"` |
| F3 | `mockResponse` for `api.json` with status 200 and body `{"mocked":true}` (see the tool schema for parameter names), `refresh`, `click` `#do-fetch`, wait, `return JSON.stringify(state.fetched)` | `{"mocked":true}`. Then `clearMockResponses` |
| F4 | `blockRequests` `api.json`, `refresh`, `click` `#do-fetch`, wait, `return state.fetched === undefined` | `true`. Then `clearBlockedRequests` |
| F5 | `waitForNetworkIdle` (defaults) | `Network is idle.` |

## G. Edge (optional, installed on this machine)

`closeBrowser`, then `openBrowser` browser=`edge`, windowSize=`1280x800`, and navigate to the fixture. Record the Edge version from `navigator.userAgent`. Re-run **A1, B6, C2, D3, E1, E2, F1, F2**. If F1 or F2 fail with `no-op implementation of the CDP`, Edge is newer than Selenium 4.51's CDP support (153–155). Record the version: that's an expected limitation, not a bug in this branch. C2 and E1/E2 should work regardless, because they send raw CDP commands.

## H. Attached browser mode (`launchDebugBrowser`)

`closeBrowser`, then `launchDebugBrowser` browser=`chrome`, port=`9333`. Navigate to the fixture and re-run **C2, D3, E1, E2, F1**. Then `closeBrowser`: expected result says it disconnected, and the Chrome window **stays open**. Ask the user to close that window by hand.

## I. Tool groups (fix #8): needs a server restart, so do this last and with the user

1. Ask the user to edit `.mcp.json` and add `"--selenium.mcp.exclude-tool-groups=api,network"` to `args` after the jar path, then restart the server with `/mcp`.
2. Expected: `httpGet`, `httpRequest`, `startNetworkCapture`, `getConsoleLogs` are **gone**. `openBrowser`, `click`, `executeScript`, `takeScreenshot` are still present.
3. Ask the user to revert `.mcp.json` and restart again.

## J. Firefox: skip

Firefox isn't installed on this machine. Full-page screenshots on Firefox (native `getFullPageScreenshotAs`) stay untested. Note that in the results.

## Cleanup

`closeBrowser`, stop the background `http.server`, and leave `dist/test-output/` in place for the user.

## Results file format (`dist/test-output/RESULTS.md`)

```markdown
# Results: fix/field-feedback, <date>
Chrome <version> · Edge <version or "skipped">

| ID | Result | Actual output (verbatim, trimmed to 200 chars) | Notes |
|----|--------|-----------------------------------------------|-------|
| A1 | PASS   | Script error: javascript error: Cannot read ... |       |

## Failures
For each FAIL: the exact call, full output, and your best guess at the cause (file:line if you looked).

## Expected limitations observed
C3, E9, plus anything from G/J.
```

Finish by telling the user the PASS/FAIL count per section and listing every FAIL.
