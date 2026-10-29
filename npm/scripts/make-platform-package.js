#!/usr/bin/env node
// Turns a jlink'd runtime + the server jar into the @voltajeba/selenium-mcp-<platform>-<arch>
// npm package directory. Run by CI on each target OS, after jlink.
//
// Usage: node make-platform-package.js <version> <outDir>
//   <outDir> must already contain runtime/ (jlink output) and selenium-mcp.jar.
//   Platform and arch are taken from the machine it runs on, so the package
//   always matches the runtime jlink just produced.
'use strict';

const fs = require('fs');
const path = require('path');

const [version, outDir] = process.argv.slice(2);
if (!version || !outDir) {
  console.error('Usage: node make-platform-package.js <version> <outDir>');
  process.exit(1);
}

for (const required of ['runtime', 'selenium-mcp.jar']) {
  if (!fs.existsSync(path.join(outDir, required))) {
    console.error(`${path.join(outDir, required)} is missing`);
    process.exit(1);
  }
}

const name = `@voltajeba/selenium-mcp-${process.platform}-${process.arch}`;
const pkg = {
  name,
  version,
  description: `Java runtime and server jar for @voltajeba/selenium-mcp on ${process.platform}-${process.arch}. Install @voltajeba/selenium-mcp instead of this package.`,
  homepage: 'https://selenium-mcp.voltajeba.com/',
  repository: {
    type: 'git',
    url: 'git+https://github.com/Volta-Jebaprashanth/selenium-mcp.git',
  },
  license: 'MIT',
  os: [process.platform],
  cpu: [process.arch],
  files: ['runtime/', 'selenium-mcp.jar'],
  // Yarn PnP: the runtime must exist as real files on disk to be executed.
  preferUnplugged: true,
};

fs.writeFileSync(path.join(outDir, 'package.json'), JSON.stringify(pkg, null, 2) + '\n');
fs.writeFileSync(
  path.join(outDir, 'README.md'),
  `# ${name}\n\nPlatform-specific part of [@voltajeba/selenium-mcp](https://www.npmjs.com/package/@voltajeba/selenium-mcp): ` +
    'a trimmed OpenJDK runtime (built with jlink) plus the selenium-mcp server jar.\n\n' +
    'Don\'t install this directly - use `npx -y @voltajeba/selenium-mcp`, which picks the right platform package automatically.\n\n' +
    'OpenJDK is licensed under GPLv2 with the Classpath Exception; its notices are in `runtime/legal/`.\n'
);
console.log(`Wrote ${name}@${version} to ${outDir}`);
