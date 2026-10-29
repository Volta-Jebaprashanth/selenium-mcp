#!/usr/bin/env node
// Stamps the release version onto the launcher packages and the packages they depend on:
// @xpathy/selenium-mcp and its platform optionalDependencies, and the @voltajeba/selenium-mcp
// alias and its @xpathy/selenium-mcp dependency. A launcher only ever resolves runtimes from
// the same release.
//
// Usage: node set-version.js <version>
'use strict';

const fs = require('fs');
const path = require('path');

const version = process.argv[2];
if (!/^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$/.test(version || '')) {
  console.error('Usage: node set-version.js <semver>');
  process.exit(1);
}

for (const dir of ['selenium-mcp', 'voltajeba-selenium-mcp']) {
  const file = path.join(__dirname, '..', dir, 'package.json');
  const pkg = JSON.parse(fs.readFileSync(file, 'utf8'));
  pkg.version = version;
  for (const deps of [pkg.dependencies, pkg.optionalDependencies]) {
    for (const dep of Object.keys(deps || {})) {
      deps[dep] = version;
    }
  }
  fs.writeFileSync(file, JSON.stringify(pkg, null, 2) + '\n');
  console.log(`${pkg.name} -> ${version}`);
}
