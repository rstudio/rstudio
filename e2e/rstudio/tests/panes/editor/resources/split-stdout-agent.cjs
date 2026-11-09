// A stand-in for the Posit Assistant language server that runs the real one
// (SPLIT_AGENT_TARGET) and relays its stdout with each message's header and
// body written separately, a pause apart, so rsession always reads them in
// different chunks (#19520). The real server also writes them separately, but
// back to back, so a read only rarely lands between them.
//
// Each relayed message is counted in SPLIT_AGENT_COUNT_FILE, so the test can
// tell this proxy was in use.

'use strict';

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const target = process.env.SPLIT_AGENT_TARGET;
const countFile = process.env.SPLIT_AGENT_COUNT_FILE;
const pauseMs = Number(process.env.SPLIT_AGENT_PAUSE_MS || 100);

if (!target || !fs.existsSync(target)) {
  process.stderr.write(`split-stdout-agent: SPLIT_AGENT_TARGET '${target}' does not exist\n`);
  process.exit(1);
}

const child = spawn(process.execPath, [target, ...process.argv.slice(2)], {
  cwd: path.dirname(target),
  stdio: ['pipe', 'pipe', 'inherit'],
});

process.stdin.pipe(child.stdin);
child.on('exit', (code, signal) => process.exit(code ?? (signal ? 1 : 0)));
process.on('SIGTERM', () => child.kill('SIGTERM'));

const SEPARATOR = Buffer.from('\r\n\r\n');
let pending = Buffer.alloc(0);
let relayed = 0;

// Writes are chained so messages keep their order across the pauses.
let writes = Promise.resolve();
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const write = (data) => new Promise((resolve, reject) =>
  process.stdout.write(data, (error) => (error ? reject(error) : resolve())));

function relay(header, body) {
  writes = writes.then(async () => {
    await write(header);
    await sleep(pauseMs);
    await write(body);
    relayed++;
    if (countFile)
      fs.writeFileSync(countFile, String(relayed));
  });
}

child.stdout.on('data', (chunk) => {
  pending = Buffer.concat([pending, chunk]);
  for (;;) {
    const headerEnd = pending.indexOf(SEPARATOR);
    if (headerEnd === -1)
      return;
    const match = /Content-Length:\s*(\d+)/i.exec(pending.subarray(0, headerEnd).toString('ascii'));
    if (!match) {
      process.stderr.write('split-stdout-agent: message without Content-Length\n');
      process.exit(1);
    }
    const bodyStart = headerEnd + SEPARATOR.length;
    const bodyEnd = bodyStart + Number(match[1]);
    if (pending.length < bodyEnd)
      return;
    relay(pending.subarray(0, bodyStart), pending.subarray(bodyStart, bodyEnd));
    pending = pending.subarray(bodyEnd);
  }
});
