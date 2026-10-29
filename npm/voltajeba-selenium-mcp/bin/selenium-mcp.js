#!/usr/bin/env node
// @voltajeba/selenium-mcp is an alias for @xpathy/selenium-mcp: it runs that launcher in
// this same process, so stdio, signals and the Windows shutdown handling are exactly the
// launcher's own. The launcher finds the npx cache from its own location, which npm
// hoists to the same place when it is installed as this package's dependency.
'use strict';

require('@xpathy/selenium-mcp/bin/selenium-mcp.js');
