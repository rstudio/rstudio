#
# test-script-startup.R
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("script startup")

runSessionScript <- function(blockSourceDatabase = FALSE,
                             script = "writeLines('executed', 'script-ran')",
                             seedPortZeroRestartContext = FALSE,
                             sessionArgs = "--program-mode=desktop",
                             localPeer = FALSE,
                             inheritedEnv = character()) {
   skip_if_not_installed("processx")

   buildDir <- Sys.getenv("TESTTHAT_OUTPUT_DIR")
   skip_if(!nzchar(buildDir), "requires the configured rstudio-tests launcher")
   executable <- if (.Platform$OS.type == "windows") "rsession.exe" else "rsession"
   session <- file.path(buildDir, "session", executable)
   if (!file.exists(session))
      session <- file.path(buildDir, "session", "Debug", executable)
   expect_true(file.exists(session))

   config <- file.path(buildDir, "conf", "rsession-dev.conf")
   if (!file.exists(config))
      config <- file.path(buildDir, "conf", "rdesktop-dev.conf")
   expect_true(file.exists(config))

   root <- tempfile("rstudio-script-startup-")
   dir.create(root)
   on.exit(unlink(root, recursive = TRUE), add = TRUE)
   root <- normalizePath(root, winslash = "/")
   for (path in c("data/client-state", "config", "home", "tmp"))
      dir.create(file.path(root, path), recursive = TRUE)

   # Fail during source-database initialization, after options and R startup.
   if (blockSourceDatabase)
      writeLines("not a directory", file.path(root, "data", "sources"))

   # What a session suspended for restart would leave in this scope, were its
   # restart context named for www-port 0 rather than for the port it bound:
   # saved state for its successor to restore. Here that's an R option (see
   # .rs.restoreOptions), and the environment, with the port the suspended
   # session listened on, which a successor mustn't take for its own.
   if (seedPortZeroRestartContext)
   {
      restartContext <- file.path(root, "data", "ctx", "ctx-0")
      dir.create(restartContext, recursive = TRUE)
      opt <- list(rstudio.tests.portZeroRestartContext = "restored")
      save(opt, file = file.path(restartContext, "options"))
      writeLines("RSTUDIO_SESSION_PORT=1", file.path(restartContext, "environment_vars"))
   }

   # Set only the child's environment; changing this session's environment
   # would race its worker threads. Avoid inheriting an active IDE's identity,
   # or its local peer, which puts a desktop session on a local stream (except
   # on Windows) rather than TCP.
   env <- Sys.getenv()
   env <- env[!grepl("^(RSTUDIO_|RS_LOCAL_PEER$)", names(env))]
   if (.Platform$OS.type == "windows")
   {
      # Use the Windows default locale; inherited POSIX names such as C.UTF-8
      # are not accepted by Windows R.
      env <- env[!grepl("^(LANG|LANGUAGE|LC_.*)$", names(env))]
   }
   env[c("RSTUDIO_DATA_HOME", "RSTUDIO_CONFIG_HOME", "R_USER", "HOME",
         "R_PROFILE_USER", "R_ENVIRON_USER", "TMPDIR", "TEMP", "TMP")] <-
      file.path(root, c("data", "config", "home", "home", "no-profile",
                       "no-environ", "tmp", "tmp", "tmp"))

   # A server session won't start for a user id below this, 100 unless set,
   # and the runner may be root.
   env[["RSTUDIO_MINIMUM_USER_ID"]] <- "0"

   env[names(inheritedEnv)] <- inheritedEnv

   # Have a desktop session listen on a local stream. A socket's path has to
   # be short, so it isn't put under root. The session leaves a lock file
   # beside it, so it gets a directory of its own.
   if (localPeer)
   {
      peerDir <- tempfile("rs-peer-", tmpdir = "/tmp")
      dir.create(peerDir, mode = "0700")
      on.exit({
         # unlink(recursive = TRUE) takes the socket for a directory (its
         # mode shares S_IFDIR's bit) and fails to remove it, or peerDir
         unlink(list.files(peerDir, all.files = TRUE, full.names = TRUE, no.. = TRUE))
         unlink(peerDir, recursive = TRUE)
      }, add = TRUE)
      env[["RS_LOCAL_PEER"]] <- file.path(peerDir, "peer")
   }

   args <- c(paste0("--config-file=", config), sessionArgs,
             "--www-port=0", "--r-run-rprofile=0", "--r-restore-workspace=0",
             "--log-stderr=1", "--run-script", script)
   if (identical(Sys.info()[["sysname"]], "Darwin"))
   {
      # Match rstudio-tests: macOS needs a controlling TTY for clean teardown,
      # and rsession resolves libR symbols only when libR is preloaded. SIP
      # strips DYLD_* variables when launching /usr/bin/script, so env sets
      # the preload after script starts.
      preload <- paste0("DYLD_INSERT_LIBRARIES=", file.path(R.home("lib"), "libR.dylib"))
      args <- c("-q", "/dev/null", "/usr/bin/env", preload, session, args)
      session <- "/usr/bin/script"
   }

   result <- processx::run(
      session, args,
      wd = file.path(root, "home"),
      env = env,
      timeout = 30,
      error_on_status = FALSE
   )
   result$output <- paste(result$stdout, result$stderr, sep = "\n")
   scriptOutput <- file.path(root, "home", "script-ran")
   result$scriptRan <- file.exists(scriptOutput)
   if (result$scriptRan)
      result$scriptOutput <- readLines(scriptOutput)
   result
}

test_that("run-script executes after successful initialization", {
   result <- runSessionScript()
   expect_equal(result$status, 0L, info = result$output)
   expect_true(result$scriptRan, info = result$output)
})

test_that("a desktop session on www-port 0 publishes the port it listens on", {
   # rpostback and other requests back into the session go to the published
   # port, so it must be one the session accepts connections on (#19744)
   script <- paste(
      "port <- Sys.getenv('RSTUDIO_SESSION_PORT')",
      "reachable <- tryCatch({ close(socketConnection('127.0.0.1', as.integer(port), open = 'r+b', timeout = 5)); TRUE }, condition = function(cnd) FALSE)",
      "writeLines(c(port, reachable), 'script-ran')",
      sep = "; "
   )

   result <- runSessionScript(script = script)
   expect_equal(result$status, 0L, info = result$output)
   expect_true(result$scriptRan, info = result$output)

   # the script wrote nothing to check; the failures above say why
   if (!result$scriptRan)
      return()

   port <- result$scriptOutput[[1L]]
   expect_match(port, "^[1-9][0-9]*$")
   expect_equal(result$scriptOutput[[2L]], "TRUE", info = port)
})

test_that("a desktop session on www-port 0 doesn't restore a restart context for port 0", {
   # the restart context is named for the port the session bound, so sessions
   # started with www-port 0 in the same scope don't restore each other's
   # (#19744)
   script <- "writeLines(getOption('rstudio.tests.portZeroRestartContext', 'not restored'), 'script-ran')"

   result <- runSessionScript(script = script, seedPortZeroRestartContext = TRUE)
   expect_equal(result$status, 0L, info = result$output)
   expect_true(result$scriptRan, info = result$output)

   # the script wrote nothing to check; the failures above say why
   if (!result$scriptRan)
      return()

   expect_equal(result$scriptOutput, "not restored", info = result$output)
})

test_that("a server session on www-port 0 restores a restart context for port 0, but not its port", {
   # a server session has a scope of its own and keeps the configured port as
   # its restart context's name. This is also what shows that the context
   # seeded for the test above is one a session would restore. The port saved
   # with it isn't restored: a server session publishes none, and
   # sendSessionRequest would take one for its own
   script <- paste(
      "writeLines(c(getOption('rstudio.tests.portZeroRestartContext', 'not restored'),",
      "Sys.getenv('RSTUDIO_SESSION_PORT', 'unset')), 'script-ran')"
   )

   result <- runSessionScript(script = script, seedPortZeroRestartContext = TRUE,
                              sessionArgs = c("--program-mode=server", "--standalone=1"))
   expect_equal(result$status, 0L, info = result$output)
   expect_true(result$scriptRan, info = result$output)

   # the script wrote nothing to check; the failures above say why
   if (!result$scriptRan)
      return()

   expect_equal(result$scriptOutput, c("restored", "unset"), info = result$output)
})

test_that("a desktop session on a local stream publishes no session port", {
   # sessions on Windows listen on TCP, whatever RS_LOCAL_PEER says
   skip_on_os("windows")

   # requests back into the session go to RS_LOCAL_PEER only when none of
   # these is set, whether inherited (as from an IDE session this one was
   # started in) or saved with the restart context it restores. With no port
   # bound, that context is the one for www-port 0
   vars <- c("RSTUDIO_SESSION_PORT", "RSTUDIO_STANDALONE_PORT", "RSTUDIO_SESSION_STREAM")
   inheritedEnv <- setNames(c("2", "3", "parent-session"), vars)
   script <- sprintf(
      "writeLines(c(getOption('rstudio.tests.portZeroRestartContext', 'not restored'), Sys.getenv(c(%s), 'unset')), 'script-ran')",
      paste0("'", vars, "'", collapse = ", ")
   )

   result <- runSessionScript(script = script, localPeer = TRUE,
                              seedPortZeroRestartContext = TRUE,
                              inheritedEnv = inheritedEnv)
   expect_equal(result$status, 0L, info = result$output)
   expect_true(result$scriptRan, info = result$output)

   # the script wrote nothing to check; the failures above say why
   if (!result$scriptRan)
      return()

   expect_equal(result$scriptOutput, c("restored", "unset", "unset", "unset"),
                info = result$output)
})

test_that("a standalone session on www-port 0 gives help the port it listens on", {
   # a server session takes help over from R: R's help server is given the
   # session's port and never started. Given port 0, R starts one of its own
   # the first time help is asked for
   script <- paste(
      "port <- Sys.getenv('RSTUDIO_STANDALONE_PORT')",
      "writeLines(c(port, .rs.httpdPort(), suppressMessages(tools::startDynamicHelp(NA))), 'script-ran')",
      sep = "; "
   )

   result <- runSessionScript(script = script,
                              sessionArgs = c("--program-mode=server", "--standalone=1"))
   expect_equal(result$status, 0L, info = result$output)
   expect_true(result$scriptRan, info = result$output)

   # the script wrote nothing to check; the failures above say why
   if (!result$scriptRan)
      return()

   port <- result$scriptOutput[[1L]]
   expect_match(port, "^[1-9][0-9]*$")
   expect_equal(result$scriptOutput[-1L], c(port, port), info = result$output)
})

test_that("run-script stops when source-database initialization fails", {
   result <- runSessionScript(blockSourceDatabase = TRUE)
   expect_match(result$output, "sources")
   expect_false(result$scriptRan, info = result$output)
   expect_equal(result$status, 1L, info = result$output)
})
