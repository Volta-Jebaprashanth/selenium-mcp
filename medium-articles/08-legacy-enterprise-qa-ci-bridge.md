# Your Bank's QA Team Isn't Rewriting Ten Years of Selenium Tests for Anything. Here's What They Can Actually Adopt.

> **Quick context:** [selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) is a Java-based [Model Context Protocol](https://modelcontextprotocol.io) server that gives an AI coding assistant a real Selenium WebDriver session, built specifically for test automation engineers working in **Java, Selenium, and REST Assured**. It drives Chrome, Firefox, or Edge directly, finds elements with enough structure to build locators that are unique on the first try, and adds Chrome DevTools Protocol network/console tooling plus a standalone REST client — as a development-time tool that plugs into an existing Selenium/Maven suite. Repo: [github.com/Volta-Jebaprashanth/selenium-mcp](https://github.com/Volta-Jebaprashanth/selenium-mcp) · Docs: [selenium-mcp.xpathy.uk](https://selenium-mcp.xpathy.uk/)

Enterprise QA doesn't move like a startup. If you're on a test automation team at a bank, an insurer, a healthcare company, or basically anywhere with a compliance department, you've got a Selenium suite that's been growing for years, a Selenium Grid or a cloud grid provider running it in CI, TestNG or JUnit reporting feeding into whatever dashboard your stakeholders check, and a very good reason not to touch any of it lightly: it works, it's audited, and "we rewrote our test framework" is not a sentence anyone wants to say after a compliance-relevant regression slips through.

That's exactly the environment where most "AI-powered testing" pitches fall apart. They assume you're starting fresh, or that you're willing to adopt a new framework, a new language, a new CI integration. Enterprise QA teams correctly don't do that. I built **selenium-mcp** to be adoptable by teams in exactly this position — because it doesn't ask you to change anything about how your suite runs. It only changes how new tests get written and how existing ones get debugged.

## Nothing about your CI pipeline changes

This is worth stating plainly because it's the first question a lead will ask: `selenium-mcp` is a development-time tool. It runs on an engineer's machine, alongside their MCP-capable coding assistant, while they're *writing or debugging* a test. The tests it helps produce are plain Java files using plain Selenium and, where relevant, a plain REST client. They get committed to the same repo, run through the same Maven build, execute on the same Selenium Grid, and report through the same TestNG/JUnit pipeline your team already has. Nothing about the CI side of your pipeline needs to know this tool exists.

Compare that to the alternative most teams imagine when they hear "AI-assisted testing" — a hosted platform that runs your tests in its own cloud sandbox, with its own reporting, its own auth model, its own vendor relationship for a compliance team to evaluate. That's a much bigger adoption decision, and for a lot of enterprise teams, a non-starter on procurement grounds alone before anyone even evaluates whether it's good.

## Where it actually helps day to day

Two workflows come up constantly on established Selenium teams, and both are exactly what this tool targets:

**Writing new tests against pages your team didn't build.** Enterprise apps are frequently a patchwork of internal teams, contractors, and acquired products, which means your QA engineers are constantly writing locators against markup nobody on your team wrote and nobody documented. `getPageElementsFiltered` — the tool that finds every match for a locator and returns its ancestor chain and sibling position — turns that into an inspection an agent can do directly against your actual staging environment, instead of your engineer manually reverse-engineering someone else's `<div>` soup in DevTools every time.

**Debugging flaky tests in an established suite.** A suite with years of history has years of accumulated flaky tests nobody's had time to properly fix — the ones with a `Thread.sleep()` some engineer added years ago as a stopgap that never got revisited. `NetworkTools`, particularly `waitForNetworkIdle` and `getNetworkLog`, gives an agent a way to actually diagnose *why* a wait was needed in the first place, built on Chrome DevTools Protocol access that Selenium 4 already has but almost nobody wires up by hand.

## The audit trail question

If your organization needs to know what changed and why — normal for regulated industries — the answer here is straightforward: every change an agent makes shows up as a normal diff in a normal pull request, same as if an engineer wrote it. There's no separate system generating or modifying tests outside your existing review process. The agent is a tool an engineer uses while writing a PR, not a system with its own write access to your codebase or CI.

## Where to start if this is your situation

Don't roll this out to a whole QA org on day one. Pick one engineer, one moderately painful area of the suite — a flaky test nobody's had time to fix, or a page with locators nobody trusts — and let them try `selenium-mcp` against it for a week. The output is a plain Java diff either way; it's reviewable with exactly the rigor your team already applies to any PR. [Quickstart's here](https://selenium-mcp.xpathy.uk/) if that's the plan.

Full tool reference, including everything mentioned above, is on the [docs site](https://selenium-mcp.xpathy.uk/docs/tools).
