# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
once it reaches a 1.0.0 release.

## [Unreleased]

### Added

- MCP server exposing Selenium browser automation (Chrome/Firefox/Edge) over stdio, built with Spring Boot and Spring AI's MCP server starter.
- Browser lifecycle & navigation, element queries, explicit waits, alerts, frames, windows/tabs, cookies, screenshots, JS execution & scrolling, mouse/keyboard actions, dropdown handling, and file upload/PDF printing tools.
- Chrome DevTools Protocol (CDP) integration for network capture, request mocking/blocking, simulated network conditions, and console log capture (Chrome/Edge only).
- Standalone REST client (`ApiTools`) for API automation independent of the browser session.
- Project documentation: [README.md](README.md), [AGENTS.md](AGENTS.md).
- Open-source project scaffolding: [LICENSE.md](LICENSE.md), [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), [SECURITY.md](SECURITY.md).

[Unreleased]: https://github.com/JebaprashanthBlt/selenium-mcp/commits/main
