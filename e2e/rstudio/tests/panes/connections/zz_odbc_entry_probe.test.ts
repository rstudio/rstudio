/**
 * TEMPORARY DIAGNOSTIC -- delete before this branch merges.
 *
 * On Windows the New Connection wizard lists the machine's real ODBC drivers
 * but silently omits the two the suite registers, even though
 * odbc::odbcListDrivers() in the same session reports them (the driver-visible
 * gate in the other specs passes). The loss therefore happens inside
 * .rs.connectionReadOdbc(), whose per-driver helper
 * .rs.connectionReadOdbcEntry() wraps its whole body in a tryCatch that turns
 * any error into warning() plus NULL -- and a NULL entry just disappears from
 * the list.
 *
 * This prints what that function actually returns, plus the warnings it
 * swallowed, so the cause is read rather than guessed at. It asserts nothing.
 */

import * as fs from 'fs';
import * as path from 'path';
import { test } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { rPathLiteral } from '@utils/r';

// Written to a file and sourced, rather than squeezed into one console line:
// the diagnostic needs several statements and nested quotes, and escaping all
// of that through the console is where a probe like this goes wrong.
const PROBE_R = String.raw`
local({
  out <- character()
  say <- function(...) out <<- c(out, paste0(...))

  drv <- try(odbc::odbcListDrivers(), silent = TRUE)
  if (inherits(drv, "try-error")) {
    say("odbcListDrivers() failed: ", as.character(drv))
  } else {
    say("== odbcListDrivers(): unique names ==")
    say(sort(unique(drv$name)))
    say("== distinct attribute values seen ==")
    say(paste(sort(unique(drv$attribute)), collapse = " | "))
  }

  say("== connectionReadWindowsRegistry() ==")
  reg <- try(.rs.connectionReadWindowsRegistry(), silent = TRUE)
  if (inherits(reg, "try-error")) {
    say("failed: ", as.character(reg))
  } else {
    say(paste0(reg$name, " -> ", reg$value))
  }

  # The heart of it: reproduce, per driver name, the exact selection
  # connectionReadOdbcEntry makes, and report how many values it yields. A
  # length other than 1 is what makes `if (dir.exists(snippetsDir))` throw
  # "the condition has length > 1" and get swallowed into a NULL entry.
  say("== currentDriver length per name (as connectionReadOdbcEntry selects it) ==")
  combined <- try({
    d <- odbc::odbcListDrivers()
    if (.Platform$OS.type == "windows") d <- rbind(d, .rs.connectionReadWindowsRegistry())
    d
  }, silent = TRUE)
  if (inherits(combined, "try-error")) {
    say("could not build combined table: ", as.character(combined))
  } else {
    for (nm in sort(unique(combined$name))) {
      fromList <- sum(drv$attribute == "Driver" & drv$name == nm, na.rm = TRUE)
      vals <- combined[combined$attribute == "Driver" & combined$name == nm, ]$value
      say("  ", nm,
          "  n=", length(vals),
          "  (odbcListDrivers Driver rows=", fromList, ")")
      if (length(vals) != 1) {
        for (v in vals) say("      value: ", v)
      }
    }
  }

  say("== connectionReadOdbc(): surviving entries and swallowed warnings ==")
  msgs <- character()
  entries <- withCallingHandlers(
    try(.rs.connectionReadOdbc(), silent = TRUE),
    warning = function(w) {
      msgs <<- c(msgs, paste0("WARN: ", conditionMessage(w)))
      invokeRestart("muffleWarning")
    }
  )
  if (inherits(entries, "try-error")) {
    say("connectionReadOdbc() failed outright: ", as.character(entries))
  } else {
    say(paste0("entries returned: ", length(entries)))
    for (e in entries) {
      if (is.null(e)) say("  <NULL entry (dropped)>")
      else say("  name=", as.character(e$name), "  source=", as.character(e$source))
    }
  }
  if (length(msgs) == 0) say("(no warnings)") else say(msgs)

  writeLines(out, Sys.getenv("PW_ODBC_PROBE_OUT"))
  TRUE
})
`;

test('probe: what connectionReadOdbc returns for the sandbox drivers', async ({
  rstudioPage: page,
}) => {
  const sandbox = process.env.PW_SANDBOX;
  test.skip(!sandbox, 'PW_SANDBOX not set (run through the suite harness)');

  const scriptPath = path.join(sandbox!, 'odbc-probe.R');
  const outPath = path.join(sandbox!, 'odbc-probe.txt');
  fs.writeFileSync(scriptPath, PROBE_R);

  const console_ = new ConsolePaneActions(page);
  const ok = await console_.evalRLogical(
    `{ Sys.setenv(PW_ODBC_PROBE_OUT = ${rPathLiteral(outPath)}); ` +
      `isTRUE(source(${rPathLiteral(scriptPath)})$value) }`,
  );

  // Printed rather than asserted: the point is to read it in the CI log.
  if (fs.existsSync(outPath)) {
    console.log(`\n===== ODBC PROBE =====\n${fs.readFileSync(outPath, 'utf8')}\n======================\n`);
  } else {
    console.log(`===== ODBC PROBE: no output file (source returned ${ok}) =====`);
  }
});
