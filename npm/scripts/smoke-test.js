#!/usr/bin/env node
// Starts an MCP server command, performs the initialize + tools/list handshake over
// stdio, and fails unless the server answers with a non-empty tool list. CI runs this
// against each bundled runtime, so a jlink module list that's missing something
// fails the build instead of a user's first launch.
//
// Usage: node smoke-test.js <command> [args...]
'use strict';

const { spawn } = require('child_process');

const [command, ...args] = process.argv.slice(2);
if (!command) {
  console.error('Usage: node smoke-test.js <command> [args...]');
  process.exit(1);
}

const TIMEOUT_MS = 120_000;
const child = spawn(command, args, { stdio: ['pipe', 'pipe', 'inherit'] });
let buffer = '';
let finished = false;

function finish(code, message) {
  if (finished) return;
  finished = true;
  (code === 0 ? console.log : console.error)(message);
  child.kill();
  process.exit(code);
}

function send(message) {
  child.stdin.write(JSON.stringify(message) + '\n');
}

const timer = setTimeout(() => finish(1, `No tools/list response within ${TIMEOUT_MS / 1000}s`), TIMEOUT_MS);
timer.unref();

child.on('error', (err) => finish(1, `Failed to start ${command}: ${err.message}`));
child.on('exit', (code) => finish(1, `Server exited early with code ${code}`));

child.stdout.setEncoding('utf8');
child.stdout.on('data', (chunk) => {
  buffer += chunk;
  let newline;
  while ((newline = buffer.indexOf('\n')) >= 0) {
    const line = buffer.slice(0, newline).trim();
    buffer = buffer.slice(newline + 1);
    if (!line) continue;

    let message;
    try {
      message = JSON.parse(line);
    } catch {
      finish(1, `Non-JSON output on stdout (would corrupt the MCP stream): ${line.slice(0, 200)}`);
      return;
    }

    if (message.id === 1) {
      send({ jsonrpc: '2.0', method: 'notifications/initialized' });
      send({ jsonrpc: '2.0', id: 2, method: 'tools/list' });
    } else if (message.id === 2) {
      const count = message.result?.tools?.length ?? 0;
      finish(count > 0 ? 0 : 1, `tools/list returned ${count} tools`);
    }
  }
});

send({
  jsonrpc: '2.0',
  id: 1,
  method: 'initialize',
  params: {
    protocolVersion: '2025-06-18',
    capabilities: {},
    clientInfo: { name: 'selenium-mcp-smoke-test', version: '1.0.0' },
  },
});
