#!/usr/bin/env node
// Launches the selenium-mcp jar on the Java runtime bundled in the matching
// @xpathy/selenium-mcp-<platform>-<arch> package. stdout carries the MCP protocol,
// so this script must never write to it - diagnostics go to stderr only.
'use strict';

const { spawn } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const platformPackage = `@xpathy/selenium-mcp-${process.platform}-${process.arch}`;

let packageDir;
try {
  packageDir = path.dirname(require.resolve(`${platformPackage}/package.json`));
} catch {
  process.stderr.write(
    `selenium-mcp: no bundled Java runtime for ${process.platform}-${process.arch} ` +
      `(${platformPackage} is not installed).\n` +
      'Supported: win32-x64, linux-x64, darwin-x64, darwin-arm64. On other platforms, install Java 21+ ' +
      'and run the jar from https://github.com/Volta-Jebaprashanth/selenium-mcp/releases/latest\n'
  );
  process.exit(1);
}

const isWindows = process.platform === 'win32';
const java = path.join(packageDir, 'runtime', 'bin', isWindows ? 'java.exe' : 'java');
const jar = path.join(packageDir, 'selenium-mcp.jar');

if (!isWindows) {
  // Package managers don't always preserve the executable bit. jspawnhelper is what
  // the JVM uses to start child processes such as chromedriver.
  for (const file of [java, path.join(packageDir, 'runtime', 'lib', 'jspawnhelper')]) {
    try {
      fs.chmodSync(file, 0o755);
    } catch {
      // Already executable or not present; java will report a real problem itself.
    }
  }
}

const args = process.argv.slice(2);

if (args[0] === '--version') {
  // Terminal-only convenience (an MCP client never passes it): lets users pre-download
  // the package with `npx -y @xpathy/selenium-mcp --version` before the first real start.
  const { version } = require('../package.json');
  process.stdout.write(`@xpathy/selenium-mcp ${version} (${platformPackage})\n`);
  process.exit(0);
}

if (!args.some((arg) => arg.startsWith('--logging.file.name'))) {
  // The jar's default log path is relative to the working directory, which an MCP
  // client may set to somewhere unexpected or read-only.
  const logDir = path.join(os.homedir(), '.selenium-mcp');
  try {
    fs.mkdirSync(logDir, { recursive: true });
    args.push(`--logging.file.name=${path.join(logDir, 'mcp-server.log')}`);
  } catch {
    // Fall back to the jar's default log location.
  }
}

const child = spawn(java, ['-jar', jar, ...args], { stdio: 'inherit' });

for (const signal of ['SIGINT', 'SIGTERM', 'SIGHUP']) {
  process.on(signal, () => child.kill(signal));
}

child.on('error', (err) => {
  process.stderr.write(`selenium-mcp: failed to start ${java}: ${err.message}\n`);
  process.exit(1);
});

child.on('exit', (code, signal) => {
  process.exit(signal ? 128 + (os.constants.signals[signal] ?? 0) : code ?? 1);
});
