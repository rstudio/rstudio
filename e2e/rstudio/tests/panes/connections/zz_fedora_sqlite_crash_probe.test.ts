/**
 * TEMPORARY DIAGNOSTIC -- delete before this branch merges.
 *
 * On fedora-44-x86_64, holding a live SQLite ODBC connection open crashes the
 * R session outright ("R Session Aborted: R encountered a fatal error"),
 * confirmed via the ApplicationEndedPopupPanel dialog captured in a failed
 * run's snapshot. Two things that DON'T crash rule out the obvious causes:
 *
 *   - the wizard's Test button (a brief connect-then-drop) succeeds
 *   - "Connect from New R Script" succeeds, but that destination never
 *     actually EXECUTES the generated code -- it only writes it into a buffer
 *
 * So neither passing test actually proves a held-open dbConnect() survives.
 * The crash could be in the connect call itself, or in whatever the odbc
 * package's connectionObserver registration calls afterward to populate the
 * pane (dbGetInfo, odbcListObjectTypes, odbcListObjects -- the same calls
 * SessionConnections.R and the pane's object browser rely on).
 *
 * Each step below writes a breadcrumb to a FILE (not stdout) immediately
 * after it completes. If R dies partway through, stdout capture dies with
 * it, but the file already has every step that finished -- so whatever the
 * last line in the file is names the exact call that killed the session.
 */

import * as fs from 'fs';
import * as path from 'path';
import { test } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { rPathLiteral } from '@utils/r';

const PROBE_R = String.raw`
local({
  out <- Sys.getenv("PW_CRASH_PROBE_OUT")
  crumb <- function(...) {
    con <- file(out, open = "a")
    on.exit(close(con))
    writeLines(paste0(Sys.time(), " ", ...), con)
  }
  crumb("start")

  con <- try(DBI::dbConnect(
    odbc::odbc(),
    Driver = "SQLite3 (pw)",
    Database = Sys.getenv("PW_SQLITE_DB_PATH"),
    timeout = 10
  ), silent = TRUE)
  if (inherits(con, "try-error")) {
    crumb("dbConnect FAILED (not a crash, a normal R error): ", as.character(con))
    return(invisible(NULL))
  }
  crumb("dbConnect survived, connection held open")

  ok <- try(DBI::dbIsValid(con), silent = TRUE)
  crumb("dbIsValid survived: ", as.character(ok))

  info <- try(DBI::dbGetInfo(con), silent = TRUE)
  crumb("dbGetInfo survived: ", if (inherits(info, "try-error")) as.character(info) else "ok")

  types <- try(odbc::odbcListObjectTypes(con), silent = TRUE)
  crumb("odbcListObjectTypes survived: ", if (inherits(types, "try-error")) as.character(types) else "ok")

  objects <- try(odbc::odbcListObjects(con), silent = TRUE)
  crumb("odbcListObjects survived: ", if (inherits(objects, "try-error")) as.character(objects) else "ok")

  cols <- try(odbc::odbcListColumns(con, table = "orders"), silent = TRUE)
  crumb("odbcListColumns survived: ", if (inherits(cols, "try-error")) as.character(cols) else "ok")

  # Mirrors the real connect path: register with the pane's own
  # connectionObserver, the thing that never runs during the raw seedDatabase
  # probes elsewhere in this suite (those deliberately null it out).
  observer <- getOption("connectionObserver")
  if (!is.null(observer)) {
    reg <- try(observer$connectionOpened(
      type = "SQLite",
      host = Sys.getenv("PW_SQLITE_DB_PATH"),
      finder = function(...) {},
      connectCode = "",
      disconnect = function() {},
      listObjectTypes = function() odbc::odbcListObjectTypes(con),
      listObjects = function(...) odbc::odbcListObjects(con, ...),
      listColumns = function(...) odbc::odbcListColumns(con, ...),
      previewObject = function(...) NULL,
      connectionObject = con
    ), silent = TRUE)
    crumb("connectionObserver$connectionOpened survived: ",
          if (inherits(reg, "try-error")) as.character(reg) else "ok")
  } else {
    crumb("no connectionObserver registered (unexpected in a Desktop session)")
  }

  try(DBI::dbDisconnect(con), silent = TRUE)
  crumb("dbDisconnect survived -- ALL STEPS COMPLETED, no crash")
  TRUE
})
`;

test('probe: which step crashes R when holding a live SQLite connection open', async ({
  rstudioPage: page,
}) => {
  const sandbox = process.env.PW_SANDBOX;
  test.skip(!sandbox, 'PW_SANDBOX not set (run through the suite harness)');

  const dbPath = path.join(sandbox!, 'db', 'sqlite', 'pwsqlite.db');
  const outPath = path.join(sandbox!, 'crash-probe.txt');
  fs.writeFileSync(outPath, '');
  const scriptPath = path.join(sandbox!, 'crash-probe.R');
  fs.writeFileSync(scriptPath, PROBE_R);

  const console_ = new ConsolePaneActions(page);
  // wait: false is load-bearing here, not a style choice: the default
  // (true) blocks on a new console prompt via a prompt-count increase, which
  // never happens if this command is exactly the one that kills the session.
  // Errors from the call itself are swallowed for the same reason -- a
  // crash's own failure mode (the RPC connection dropping mid-call) is not
  // something to fail the test on; the breadcrumb file is the actual result.
  try {
    await console_.executeInConsole(
      `Sys.setenv(PW_CRASH_PROBE_OUT = ${rPathLiteral(outPath)}, ` +
        `PW_SQLITE_DB_PATH = ${rPathLiteral(dbPath)}); ` +
        `source(${rPathLiteral(scriptPath)})`,
      { wait: false },
    );
  } catch (err) {
    console.log(`[crash-probe] executeInConsole itself threw (may be expected on a crash): ${err}`);
  }

  // Generous, since a real crash needs time to tear the session down and
  // show the "R Session Aborted" dialog before the breadcrumb file's final
  // state is meaningful to read.
  await page.waitForTimeout(15000);

  const contents = fs.existsSync(outPath) ? fs.readFileSync(outPath, 'utf8') : '(file never created)';
  console.log(`\n===== CRASH PROBE =====\n${contents}\n=======================\n`);
});
