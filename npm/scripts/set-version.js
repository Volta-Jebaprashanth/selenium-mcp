#!/usr/bin/env node
// Stamps the release version onto the @xpathy/selenium-mcp launcher package and its
// platform optionalDependencies, so a launcher only ever resolves runtimes from the
// same release.
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

const file = path.join(__dirname, '..', 'selenium-mcp', 'package.json');
const pkg = JSON.parse(fs.readFileSync(file, 'utf8'));
pkg.version = version;
for (const dep of Object.keys(pkg.optionalDependencies)) {
  pkg.optionalDependencies[dep] = version;
}
fs.writeFileSync(file, JSON.stringify(pkg, null, 2) + '\n');
console.log(`${pkg.name} -> ${version}`);
