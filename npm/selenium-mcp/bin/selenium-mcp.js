#!/usr/bin/env node
// Launches the selenium-mcp jar on the Java runtime bundled in the matching
// @xpathy/selenium-mcp-<platform>-<arch> package. stdout carries the MCP protocol,
// so this script must never write to it - diagnostics go to stderr only.
'use strict';

const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const { version } = require('../package.json');
const SUPPORTED = ['win32-x64', 'linux-x64', 'darwin-x64', 'darwin-arm64'];
const target = `${process.platform}-${process.arch}`;
const platformPackage = `@xpathy/selenium-mcp-${target}`;

function resolvePlatformPackage() {
  try {
    return path.dirname(require.resolve(`${platformPackage}/package.json`));
  } catch {
    return null;
  }
}

// npm skips an optionalDependency it can't fetch (e.g. a run that raced a release
// still being processed by the registry), and npx then reuses that incomplete install
// from its cache on every later run. When we're running from the npx cache, install
// the missing platform package into it so the next start doesn't hit this again.
function repairNpxInstall() {
  const npxInstallDir = path.resolve(__dirname, '..', '..', '..', '..');
  const npmCli = process.env.npm_execpath;
  if (path.basename(path.dirname(npxInstallDir)) !== '_npx' || !npmCli || !/npm-cli\.js$/.test(npmCli)) {
    return null;
  }
  process.stderr.write(`selenium-mcp: ${platformPackage} is missing from the npx cache - installing it...\n`);
  const result = spawnSync(
    process.execPath,
    [npmCli, 'install', '--no-save', '--no-audit', '--no-fund', `${platformPackage}@${version}`],
    // npm's own output goes to stderr (fd 2): stdout belongs to the MCP client.
    { cwd: npxInstallDir, stdio: ['ignore', 2, 2] }
  );
  return result.status === 0 ? resolvePlatformPackage() : null;
}

let packageDir = resolvePlatformPackage();
if (!packageDir && SUPPORTED.includes(target)) {
  packageDir = repairNpxInstall();
}
if (!packageDir) {
  const npxInstallDir = path.resolve(__dirname, '..', '..', '..', '..');
  process.stderr.write(
    SUPPORTED.includes(target)
      ? `selenium-mcp: ${platformPackage}@${version} is not installed, and installing it failed.\n` +
          (path.basename(path.dirname(npxInstallDir)) === '_npx'
            ? `Delete ${npxInstallDir} and restart the server so npx reinstalls it.\n`
            : `Reinstall @xpathy/selenium-mcp without --omit=optional / --no-optional.\n`)
      : `selenium-mcp: no bundled Java runtime for ${target}. Supported: ${SUPPORTED.join(', ')}.\n` +
          'On other platforms, install Java 21+ and run the jar from ' +
          'https://github.com/Volta-Jebaprashanth/selenium-mcp/releases/latest\n'
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
