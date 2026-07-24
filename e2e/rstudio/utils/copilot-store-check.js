// Reads a GitHub Copilot auth.db in a short-lived child process, invoked by
// isCopilotStoreAuthenticated in auth.ts. The read runs here, not in the
// Playwright worker, on purpose: better-sqlite3's native addon has
// intermittently segfaulted the worker during N-API registration on CI
// (macOS arm64 and Windows). Isolating the require + read in a disposable
// child means such a crash kills only this process -- the parent detects the
// signal and retries -- instead of taking down the whole worker.
//
// better-sqlite3 is real SQLite, so it applies the -wal sidecar: the
// copilot-language-server keeps the oauth_tokens table and token in an
// uncheckpointed WAL, which a pure-WASM reader (sql.js) cannot see but this
// can. Runs on the same node as the parent (process.execPath) so the prebuilt
// binding matches the ABI.
//
// Contract: prints "1" if oauth_tokens holds >= 1 row, else "0". Any logical
// read error (missing table, unreadable file) prints "0" (fail closed). A
// native crash exits via signal with no output; the parent treats that as a
// retryable failure.
const Database = require('better-sqlite3');

const dbPath = process.argv[2];
try {
  const db = new Database(dbPath, { readonly: true, fileMustExist: true });
  const row = db.prepare('SELECT COUNT(*) AS n FROM oauth_tokens').get();
  db.close();
  process.stdout.write(Number(row.n) > 0 ? '1' : '0');
} catch (err) {
  process.stderr.write(String(err && err.message));
  process.stdout.write('0');
}
