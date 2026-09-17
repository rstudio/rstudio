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

runSessionScript <- function(blockSourceDatabase = FALSE) {
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

   # Set only the child's environment; changing this session's environment
   # would race its worker threads. Avoid inheriting an active IDE's identity.
   env <- Sys.getenv()
   env <- env[!grepl("^RSTUDIO_", names(env))]
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

   args <- c(paste0("--config-file=", config), "--program-mode=desktop",
             "--www-port=0", "--r-run-rprofile=0", "--r-restore-workspace=0",
             "--log-stderr=1", "--run-script", "writeLines('executed', 'script-ran')")
   if (identical(Sys.info()[["sysname"]], "Darwin"))
   {
      # Match rstudio-tests: macOS needs a controlling TTY for clean teardown.
      args <- c("-q", "/dev/null", session, args)
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
   result$scriptRan <- file.exists(file.path(root, "home", "script-ran"))
   result
}

test_that("run-script executes after successful initialization", {
   result <- runSessionScript()
   expect_equal(result$status, 0L, info = result$output)
   expect_true(result$scriptRan, info = result$output)
})

test_that("run-script stops when source-database initialization fails", {
   result <- runSessionScript(blockSourceDatabase = TRUE)
   expect_match(result$output, "sources")
   expect_false(result$scriptRan, info = result$output)
   expect_equal(result$status, 1L, info = result$output)
})
