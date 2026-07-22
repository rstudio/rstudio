#
# test-packages.R
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

test_that(".rs.isPackageManagementCall detects namespace-qualified mutators", {
   # qualified calls use the literal namespace, so these hold whether or not the
   # packages are installed
   expect_true(.rs.isPackageManagementCall("renv::install(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("renv::update()"))
   expect_true(.rs.isPackageManagementCall("renv::remove(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("renv::restore()"))
   expect_true(.rs.isPackageManagementCall("renv::rebuild(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("renv:::update()"))
   expect_true(.rs.isPackageManagementCall("utils::install.packages(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("utils::remove.packages(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("devtools::install_github(\"r-lib/cli\")"))
   expect_true(.rs.isPackageManagementCall("devtools::load_all(\".\")"))
   expect_true(.rs.isPackageManagementCall("remotes::install_version(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("pak::pkg_install(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("pacman::p_load(dplyr)"))
   expect_true(.rs.isPackageManagementCall("BiocManager::install(\"limma\")"))
})

test_that(".rs.isPackageManagementCall detects unqualified mutators from utils", {
   # utils is attached in a default session, so these bare calls resolve to it
   expect_true(.rs.isPackageManagementCall("install.packages(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("update.packages()"))
   expect_true(.rs.isPackageManagementCall("remove.packages(\"dplyr\")"))
})

test_that(".rs.isPackageManagementCall ignores base R remove()/update()", {
   # base::remove removes objects and stats::update refits models; neither
   # mutates the package library, even though renv exports same-named functions
   expect_false(.rs.isPackageManagementCall("remove(x)"))
   expect_false(.rs.isPackageManagementCall("remove(x, y, z)"))
   expect_false(.rs.isPackageManagementCall("update(model)"))
   expect_false(.rs.isPackageManagementCall("update(fit, . ~ . + x)"))
   expect_false(.rs.isPackageManagementCall("base::remove(x)"))
   expect_false(.rs.isPackageManagementCall("stats::update(model)"))
})

test_that(".rs.isPackageManagementCall ignores non-mutating calls", {
   expect_false(.rs.isPackageManagementCall("str_remove_all(x)"))
   expect_false(.rs.isPackageManagementCall("stringr::str_remove_all(x)"))
   expect_false(.rs.isPackageManagementCall("installed.packages()"))
   expect_false(.rs.isPackageManagementCall("old.packages()"))
   expect_false(.rs.isPackageManagementCall("library(dplyr)"))
   expect_false(.rs.isPackageManagementCall("updates <- c(1, 2)"))
   # functions that are not in the registry for their namespace
   expect_false(.rs.isPackageManagementCall("pak::pkg_update(\"dplyr\")"))
   expect_false(.rs.isPackageManagementCall("renv::snapshot()"))
})

test_that(".rs.isPackageManagementCall ignores comments and strings", {
   expect_false(.rs.isPackageManagementCall("# remove this later"))
   expect_false(.rs.isPackageManagementCall("(t0 <- Sys.time()) # remove"))
   expect_false(.rs.isPackageManagementCall("x <- \"remove\""))
   expect_false(.rs.isPackageManagementCall("y <- \"renv::update()\""))
})

test_that(".rs.isPackageManagementCall handles multi-statement and nested calls", {
   expect_true(.rs.isPackageManagementCall("x <- 1; renv::update(); y <- 2"))
   expect_true(.rs.isPackageManagementCall("lapply(pkgs, function(p) install.packages(p))"))
   expect_false(.rs.isPackageManagementCall("x <- 1; print(remove(y)); z <- 3"))
})

test_that(".rs.isPackageManagementCall tolerates empty arguments", {
   expect_false(.rs.isPackageManagementCall("View(mtcars[1, ])"))
   expect_false(.rs.isPackageManagementCall("mtcars[, 1]"))
   expect_true(.rs.isPackageManagementCall("f(x[1, ], renv::update())"))
})

test_that(".rs.isPackageManagementCall tolerates unparseable input", {
   expect_false(.rs.isPackageManagementCall(""))
   expect_false(.rs.isPackageManagementCall("install.packages("))
   expect_false(.rs.isPackageManagementCall("if (x"))
})

test_that(".rs.isPackageManagementCall does not force active bindings", {
   # resolving a bare call must not (re-)invoke an active binding, which could
   # run arbitrary user code as a side effect
   invoked <- 0L
   makeActiveBinding(
      "remove",
      function() { invoked <<- invoked + 1L; function(...) NULL },
      globalenv()
   )
   on.exit(rm("remove", envir = globalenv()), add = TRUE)

   expect_false(.rs.isPackageManagementCall("remove(x)"))
   expect_identical(invoked, 0L)
})

test_that(".rs.isPackageManagementCall does not force promises", {
   # a delayedAssign promise shadowing utils::install.packages must not be forced
   forced <- 0L
   delayedAssign(
      "install.packages",
      { forced <<- forced + 1L; function(...) NULL },
      assign.env = globalenv()
   )
   on.exit(rm("install.packages", envir = globalenv()), add = TRUE)

   expect_false(.rs.isPackageManagementCall("install.packages(x)"))
   expect_identical(forced, 0L)
})

# Pump background processing until 'pkg' appears in .rs.hookedPackages, or
# the timeout elapses; returns TRUE if the package was hooked. The library
# scan runs on a background thread and hooks are registered from a scheduled
# command on the main thread, so tests must pump background processing
# manually (the headless test harness has no scheduled-command pump). Note
# that scan state is process-global: a scan kicked off outside the test (e.g.
# at deferred init) may still be in flight when a test starts; polling until
# the package of interest is hooked tolerates that.
waitUntilHooked <- function(pkg, timeout = 30)
{
   deadline <- Sys.time() + timeout
   while (!pkg %in% .rs.hookedPackages && Sys.time() < deadline)
   {
      .Call("rs_performBackgroundProcessing", FALSE, PACKAGE = "(embedding)")
      Sys.sleep(0.1)
   }
   pkg %in% .rs.hookedPackages
}

# testthat::local_mocked_bindings cannot mock functions in the locked
# "tools:rstudio" environment, so save, replace, and restore via assign().
withRsMock <- function(targetName, newValue, expr)
{
   rsEnv <- as.environment("tools:rstudio")
   original <- get(targetName, envir = rsEnv)
   on.exit(assign(targetName, original, envir = rsEnv), add = TRUE)
   assign(targetName, newValue, envir = rsEnv)
   force(expr)
}

test_that(".rs.registerPackageEventHooks registers hooks once per package", {
   fakePkg <- basename(tempfile("rstudioFakePkg"))
   expect_false(fakePkg %in% .rs.hookedPackages)

   result <- withVisible(.rs.registerPackageEventHooks(fakePkg))
   expect_false(result$visible)
   expect_identical(result$value, fakePkg)

   expect_true(fakePkg %in% .rs.hookedPackages)

   # each event must be bound to its matching handler; a transposition here
   # would invert the attached flag reported to the client
   expect_identical(getHook(packageEvent(fakePkg, "attach")), list(.rs.notifyPackageAttached))
   expect_identical(getHook(packageEvent(fakePkg, "onLoad")), list(.rs.notifyPackageLoaded))
   expect_identical(getHook(packageEvent(fakePkg, "onUnload")), list(.rs.notifyPackageUnloaded))
   expect_identical(getHook(packageEvent(fakePkg, "detach")), list(.rs.notifyPackageDetached))

   # re-registering an already-hooked package is a no-op
   result <- .rs.registerPackageEventHooks(fakePkg)
   expect_length(result, 0L)
   expect_length(getHook(packageEvent(fakePkg, "attach")), 1L)
})

test_that(".rs.registerPackageEventHooks dedupes duplicate package names", {
   # the same package name can appear under multiple library paths, so a
   # single scan can hand us duplicates; each package must be hooked once
   fakePkg <- basename(tempfile("rstudioFakePkg"))
   .rs.registerPackageEventHooks(c(fakePkg, fakePkg))
   expect_length(getHook(packageEvent(fakePkg, "attach")), 1L)
})

test_that("package attach and detach hooks report the matching attached flag", {
   events <- list()
   recorder <- function(type, data = NULL)
      events[[length(events) + 1L]] <<- list(type = type, data = data)

   withRsMock(".rs.enqueClientEvent", recorder, {
      .rs.notifyPackageAttached("stats")
      .rs.notifyPackageDetached("stats")
   })

   expect_length(events, 2L)
   expect_identical(events[[1L]]$type, "package_status_changed")
   expect_identical(as.character(events[[1L]]$data$name), "stats")
   expect_true(as.logical(events[[1L]]$data$attached))
   expect_identical(events[[2L]]$type, "package_status_changed")
   expect_identical(as.character(events[[2L]]$data$name), "stats")
   expect_false(as.logical(events[[2L]]$data$attached))
})

test_that(".rs.updatePackageEvents hooks packages found on the library paths", {
   libDir <- tempfile("rstudioFakeLib")
   fakePkg <- basename(tempfile("rstudioFakePkg"))
   dir.create(file.path(libDir, fakePkg), recursive = TRUE)

   oldPaths <- .libPaths()
   on.exit(.libPaths(oldPaths), add = TRUE)
   on.exit(unlink(libDir, recursive = TRUE), add = TRUE)
   .libPaths(c(libDir, oldPaths))

   .rs.updatePackageEvents()

   expect_true(
      waitUntilHooked(fakePkg),
      info = "timed out waiting for the package event scan to hook the fake package"
   )
   expect_length(getHook(packageEvent(fakePkg, "onLoad")), 1L)
})

test_that(".rs.updatePackageEvents coalesces overlapping scans into a follow-up pass", {
   libDir1 <- tempfile("rstudioFakeLib")
   libDir2 <- tempfile("rstudioFakeLib")
   pkgA <- basename(tempfile("rstudioFakePkg"))
   pkgB <- basename(tempfile("rstudioFakePkg"))
   dir.create(file.path(libDir1, pkgA), recursive = TRUE)
   dir.create(file.path(libDir2, pkgB), recursive = TRUE)

   oldPaths <- .libPaths()
   on.exit(.libPaths(oldPaths), add = TRUE)
   on.exit(unlink(c(libDir1, libDir2), recursive = TRUE), add = TRUE)
   .libPaths(c(libDir1, oldPaths))

   # after this call a scan is in flight: either this call started one, or it
   # queued a follow-up pass behind a scan that was already running
   .rs.updatePackageEvents()

   # make libDir2 visible only now, after any in-flight scan has already
   # snapshotted the library paths: pkgB can only be hooked by a follow-up
   # pass started after the in-flight scan completes
   .libPaths(c(libDir2, libDir1, oldPaths))

   # no background processing has been pumped since the call above, so the
   # scan is still in flight and this call must take the coalescing branch
   expect_false(.rs.updatePackageEvents())

   expect_true(
      waitUntilHooked(pkgB),
      info = "timed out waiting for the follow-up package event scan to hook the fake package"
   )
})

test_that("the watchdog abandons a scan that does not complete in time", {
   libDir <- tempfile("rstudioFakeLib")
   fakePkg <- basename(tempfile("rstudioFakePkg"))
   dir.create(file.path(libDir, fakePkg), recursive = TRUE)

   oldPaths <- .libPaths()
   on.exit(.libPaths(oldPaths), add = TRUE)
   on.exit(unlink(libDir, recursive = TRUE), add = TRUE)
   .libPaths(c(libDir, oldPaths))

   # a zero-second timeout makes the watchdog due at the first pump; it is
   # queued when the scan starts, ahead of the scan's completion command, so
   # the scan is deterministically abandoned no matter how quickly the
   # background thread finishes
   options(rstudio.packageEventScanTimeout = 0L)
   on.exit(options(rstudio.packageEventScanTimeout = NULL), add = TRUE)

   # a scan started outside this test (e.g. at deferred init, or coalesced
   # behind one from an earlier test) uses the default timeout; pump until we
   # can start a scan of our own, which picks up the zero-second timeout
   deadline <- Sys.time() + 30
   started <- .rs.updatePackageEvents()
   while (!started && Sys.time() < deadline)
   {
      .Call("rs_performBackgroundProcessing", FALSE, PACKAGE = "(embedding)")
      Sys.sleep(0.1)
      started <- .rs.updatePackageEvents()
   }
   expect_true(started, info = "timed out waiting to start a zero-timeout scan")

   # pump: the watchdog abandons the scan, and its completion (arriving
   # whenever the background thread finishes) is dropped, so the package
   # must not be hooked
   expect_false(waitUntilHooked(fakePkg, timeout = 1))

   # the watchdog must also have reset the coalescing state: with the
   # timeout restored, a fresh scan can start immediately and hooks the
   # package the abandoned scan never delivered
   options(rstudio.packageEventScanTimeout = NULL)
   expect_true(.rs.updatePackageEvents())
   expect_true(
      waitUntilHooked(fakePkg),
      info = "timed out waiting for the post-abandonment scan to hook the fake package"
   )
})
