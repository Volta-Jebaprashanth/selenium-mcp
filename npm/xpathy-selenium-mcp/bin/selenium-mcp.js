#!/usr/bin/env node
// @xpathy/selenium-mcp is the package's former name, kept working for existing configs: it
// runs the @voltajeba/selenium-mcp launcher in this same process, so stdio, signals and the
// Windows shutdown handling are exactly the launcher's own. The launcher finds the npx cache
// from its own location, which npm hoists to the same place when it is installed as this
// package's dependency.
'use strict';

require('@voltajeba/selenium-mcp/bin/selenium-mcp.js');
