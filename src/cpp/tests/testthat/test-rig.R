#
# test-rig.R
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

context("rig")

# A stand-in for launchctl: 'state' holds whether rig's agent is loaded and
# the RSTUDIO_WHICH_R launchd exports, 'calls' records every invocation, and
# the verbs in 'failing' fail (as a missing launchctl would, with NULL).
fakeLaunchd <- function(loaded = FALSE, whichR = "")
{
   state <- new.env(parent = emptyenv())
   state$loaded <- loaded
   state$whichR <- whichR
   state$calls <- list()
   state$failing <- character()

   launchctl <- function(args)
   {
      state$calls[[length(state$calls) + 1L]] <- args
      verb <- args[[1L]]
      if (verb %in% state$failing)
         return(NULL)

      switch(verb,
         list = if (state$loaded) "{ \"Label\" = \"io.r-lib.rig.rstudio-which-r\"; }" else NULL,
         getenv = state$whichR,
         load = { state$loaded <- TRUE; "" },
         unload = { state$loaded <- FALSE; "" },
         setenv = { state$whichR <- args[[3L]]; "" },
         unsetenv = { state$whichR <- ""; "" },
         stop("unexpected launchctl verb: ", verb))
   }

   verbs <- function()
   {
      vapply(state$calls, function(call) call[[1L]], character(1))
   }

   list(state = state, launchctl = launchctl, verbs = verbs)
}

# A stand-in for rig that answers 'rig system dirs --user --json' with the
# given directories, or fails when none are given.
fakeRig <- function(home, dirs)
{
   rig <- file.path(home, "rig")
   lines <- if (is.null(dirs))
      c("#!/bin/sh", "exit 1")
   else
      c("#!/bin/sh", sprintf("echo '{\"r_root\": \"%s\", \"binary_dir\": \"%s\"}'", dirs$r_root, dirs$binary_dir))

   writeLines(lines, rig)
   Sys.chmod(rig, "0755")
   rig
}

test_that("the bundled rig follows an explicit override and precedes system copies", {

   bundled <- file.path(tempdir(), "RStudio", "bin", "rig")
   override <- file.path(tempdir(), "custom", .rs.rig.exeName())
   candidates <- .rs.rig.candidatePaths(override = override, bundled = bundled)
   expect_identical(candidates[1:2], c(override, file.path(bundled, "bin", .rs.rig.exeName())))
   expect_identical(
      .rs.rig.candidatePaths(override = "", bundled = bundled)[[1L]],
      file.path(bundled, "bin", .rs.rig.exeName())
   )

})

test_that("the configured bundled rig is usable without an external installation", {

   # rig is bundled with the desktop only
   skip_if_not(identical(.rs.api.versionInfo()$mode, "desktop"), "not a desktop session")

   bundled <- file.path(.Call("rs_rigPath", PACKAGE = "(embedding)"), "bin", .rs.rig.exeName())
   expect_true(file.exists(bundled))
   expect_equal(.rs.rig.version(bundled), package_version(.rs.rig.pinnedVersion))
   expect_identical(
      .rs.rig.find(paths = c(tempfile("missing-rig-"), bundled)),
      normalizePath(bundled, winslash = "/")
   )

})

test_that("R versions match on major.minor only", {

   expect_true(.rs.rVersionMatches("4.4.1", "4.4.1"))
   expect_true(.rs.rVersionMatches("4.4.1", "4.4.3"))
   expect_true(.rs.rVersionMatches("4.4", "4.4.0"))
   expect_false(.rs.rVersionMatches("4.4.1", "4.5.0"))
   expect_false(.rs.rVersionMatches("4.4.1", "3.4.1"))

   # vectorized over candidates
   expect_identical(
      .rs.rVersionMatches("4.4.1", c("4.3.3", "4.4.0", "4.5.1")),
      c(FALSE, TRUE, FALSE)
   )

   # malformed versions never match
   expect_false(.rs.rVersionMatches("not-a-version", "4.4.1"))
   expect_false(.rs.rVersionMatches("4.4.1", "release"))
   expect_identical(.rs.rVersionMatches("oops", character()), logical())

})

test_that("an installed R is matched by major.minor, preferring the exact patch", {

   installed <- data.frame(
      version = c("4.3.3", "4.4.0", "4.4.2", "4.5.1"),
      path    = paste0("/opt/R/", c("4.3.3", "4.4.0", "4.4.2", "4.5.1")),
      binary  = paste0("/opt/R/", c("4.3.3", "4.4.0", "4.4.2", "4.5.1"), "/bin/R"),
      home    = paste0("/opt/R/", c("4.3.3", "4.4.0", "4.4.2", "4.5.1"), "/lib/R"),
      stringsAsFactors = FALSE
   )

   # exact match wins
   match <- .rs.findInstalledRVersion("4.4.0", installed, architecture = NULL)
   expect_equal(match$version, .rs.scalar("4.4.0"))
   expect_equal(match$binary, .rs.scalar("/opt/R/4.4.0/bin/R"))
   expect_equal(match$home, .rs.scalar("/opt/R/4.4.0/lib/R"))

   # otherwise the newest patch release of the same minor
   match <- .rs.findInstalledRVersion("4.4.1", installed, architecture = NULL)
   expect_equal(match$version, .rs.scalar("4.4.2"))

   # a version spec without a patch component also works
   match <- .rs.findInstalledRVersion("4.3", installed, architecture = NULL)
   expect_equal(match$version, .rs.scalar("4.3.3"))

   # nothing installed from that minor
   expect_null(.rs.findInstalledRVersion("4.2.0", installed, architecture = NULL))
   expect_null(.rs.findInstalledRVersion("4.4.1", installed[0, ], architecture = NULL))
   expect_null(.rs.findInstalledRVersion("bogus", installed, architecture = NULL))

})

test_that("the architectures of an R build are read from libR", {

   root <- tempfile("r-builds-")
   on.exit(unlink(root, recursive = TRUE), add = TRUE)

   # thin images carry their CPU type after the magic; universal images
   # carry a table of them (arm64 is 0x0100000c, x86_64 is 0x01000007)
   headers <- list(
      arm64     = as.raw(c(0xcf, 0xfa, 0xed, 0xfe, 0x0c, 0x00, 0x00, 0x01)),
      x86_64    = as.raw(c(0xcf, 0xfa, 0xed, 0xfe, 0x07, 0x00, 0x00, 0x01)),
      universal = as.raw(c(
         0xca, 0xfe, 0xba, 0xbe, 0x00, 0x00, 0x00, 0x02,
         0x01, 0x00, 0x00, 0x07, rep(0x00, 16),
         0x01, 0x00, 0x00, 0x0c, rep(0x00, 16)
      )),
      text      = charToRaw("#!/bin/sh\n")
   )

   for (name in names(headers))
   {
      dir.create(file.path(root, name, "lib"), recursive = TRUE)
      writeBin(headers[[name]], file.path(root, name, "lib", "libR.dylib"))
   }

   expect_identical(.rs.rInstallations.architectures(file.path(root, "arm64")), "arm64")
   expect_identical(.rs.rInstallations.architectures(file.path(root, "x86_64")), "x86_64")
   expect_identical(.rs.rInstallations.architectures(file.path(root, "universal")), c("x86_64", "arm64"))
   expect_identical(.rs.rInstallations.architectures(file.path(root, "text")), character())

   # a universal image whose table is cut short
   dir.create(file.path(root, "truncated", "lib"), recursive = TRUE)
   writeBin(headers$universal[1:10], file.path(root, "truncated", "lib", "libR.dylib"))
   expect_identical(.rs.rInstallations.architectures(file.path(root, "truncated")), character())
   expect_identical(.rs.rInstallations.architectures(file.path(root, "missing")), character())

   # a build for the preferred architecture wins over an emulated one
   installed <- data.frame(
      version = c("4.4.1", "4.4.1"),
      path    = file.path(root, c("x86_64", "arm64")),
      binary  = file.path(root, c("x86_64", "arm64"), "bin", "R"),
      home    = file.path(root, c("x86_64", "arm64")),
      stringsAsFactors = FALSE
   )

   match <- .rs.findInstalledRVersion("4.4.1", installed, architecture = "arm64")
   expect_equal(match$home, .rs.scalar(file.path(root, "arm64")))

   # an emulated build is still used when it is the only one
   match <- .rs.findInstalledRVersion("4.4.1", installed[1L, ], architecture = "arm64")
   expect_equal(match$home, .rs.scalar(file.path(root, "x86_64")))

   # no preference otherwise
   match <- .rs.findInstalledRVersion("4.4.1", installed, architecture = NULL)
   expect_equal(match$home, .rs.scalar(file.path(root, "x86_64")))

})

test_that("only version numbers are accepted as R versions", {

   expect_true(.rs.rVersionIsValid("4.4"))
   expect_true(.rs.rVersionIsValid("4.4.1"))

   expect_false(.rs.rVersionIsValid("release"))
   expect_false(.rs.rVersionIsValid("4"))
   expect_false(.rs.rVersionIsValid("4.4.1.1"))
   expect_false(.rs.rVersionIsValid("<img src=x onerror=alert(1)>"))
   expect_false(.rs.rVersionIsValid("4.4.1 <b>"))
   expect_false(.rs.rVersionIsValid(NA_character_))
   expect_false(.rs.rVersionIsValid(c("4.4.1", "4.5.0")))
   expect_false(.rs.rVersionIsValid(441))
   expect_false(.rs.rVersionIsValid(NULL))

})

test_that("the pinned rig version is the bundled one", {

   # the bundled release is pinned in dependencies/common/rig.cmake, reached
   # from the source tree the tests are run from
   testsDir <- Sys.getenv("TESTTHAT_TESTS_DIR", unset = NA)
   skip_if(is.na(testsDir), "TESTTHAT_TESTS_DIR is not set")
   cmake <- file.path(testsDir, "..", "..", "..", "..", "dependencies", "common", "rig.cmake")
   skip_if_not(file.exists(cmake), "dependencies/common/rig.cmake not found")
   contents <- readLines(cmake, warn = FALSE)

   version <- grep("^set\\(RSTUDIO_RIG_VERSION ", contents, value = TRUE)
   expect_length(version, 1L)
   expect_identical(sub('.*"(.*)".*', "\\1", version), as.character(.rs.rig.pinnedVersion))

})

test_that("the home of an R installation is found from its root", {

   root <- tempfile("r-install-")
   on.exit(unlink(root, recursive = TRUE), add = TRUE)

   # nothing that looks like R
   dir.create(root)
   expect_identical(.rs.rInstallations.homeFromPath(root), "")
   expect_identical(.rs.rInstallations.homeFromPath(""), "")

   # a Linux-style prefix, with the home under lib/R
   home <- file.path(root, "lib", "R")
   dir.create(file.path(home, "library", "base"), recursive = TRUE)
   expect_identical(
      .rs.rInstallations.homeFromPath(root),
      normalizePath(home, winslash = "/")
   )

   # the version comes from the installation's headers
   expect_identical(.rs.rInstallations.versionFromHome(home), "")
   dir.create(file.path(home, "include"))
   writeLines(
      c("#define R_MAJOR  \"4\"", "#define R_MINOR  \"4.2\"", "#define R_STATUS \"\""),
      file.path(home, "include", "Rversion.h")
   )
   expect_identical(.rs.rInstallations.versionFromHome(home), "4.4.2")

})

test_that("R installations found without rig are well-formed", {

   installed <- .rs.rInstallations.scan()
   expect_identical(names(installed), c("version", "path", "binary", "home"))
   expect_true(all(vapply(installed$version, .rs.rVersionIsValid, logical(1))))
   expect_true(all(file.exists(installed$binary)))

})

test_that("listing installed R versions without rig gives an empty table", {

   installed <- .rs.rig.list(rig = "")
   expect_s3_class(installed, "data.frame")
   expect_equal(nrow(installed), 0L)
   expect_identical(names(installed), c("version", "path", "binary", "home"))

})

test_that("only the minimum R version is enforced", {

   expect_true(.rs.rVersionSupported("4.4.1", "3.6.0"))
   expect_true(.rs.rVersionSupported("3.6.0", "3.6.0"))

   # newer than RStudio was tested with still runs
   expect_true(.rs.rVersionSupported("4.7.0", "3.6.0"))
   expect_true(.rs.rVersionSupported("5.0.0", "3.6.0"))

   expect_false(.rs.rVersionSupported("3.5.3", "3.6.0"))
   expect_false(.rs.rVersionSupported("release", "3.6.0"))

})

test_that("the home of a macOS framework install is derived from its path", {
   
   binary <- "/Library/Frameworks/R.framework/Versions/4.4-arm64/Resources/bin/R"
   expect_identical(
      .rs.rig.rHome(binary),
      "/Library/Frameworks/R.framework/Versions/4.4-arm64/Resources"
   )
   
   # a missing binary elsewhere has no known home
   expect_identical(.rs.rig.rHome("/opt/R/none/bin/R"), "")
   
})

test_that("macOS framework versions that run the default R are recognized", {

   root <- tempfile("r-framework-")
   on.exit(unlink(root, recursive = TRUE), add = TRUE)

   home <- file.path(root, "R.framework", "Versions", "4.4-arm64", "Resources")
   dir.create(file.path(home, "bin"), recursive = TRUE)

   # as installed by CRAN: the launcher names the framework's shared home
   writeLines(
      c("#!/bin/sh", "R_HOME_DIR=/Library/Frameworks/R.framework/Resources"),
      file.path(home, "bin", "R")
   )
   expect_false(.rs.rInstallations.isOrthogonal(home))

   # as rewritten by 'rig system make-orthogonal'
   writeLines(
      c("#!/bin/sh", "R_HOME_DIR=/Library/Frameworks/R.framework/Versions/4.4-arm64/Resources"),
      file.path(home, "bin", "R")
   )
   expect_true(.rs.rInstallations.isOrthogonal(home))

   # other installations are unaffected
   expect_true(.rs.rInstallations.isOrthogonal("/opt/R/4.4.1/lib/R"))

})

test_that("the current macOS framework version runs as itself", {

   skip_on_os("windows")

   root <- tempfile("r-framework-")
   on.exit(unlink(root, recursive = TRUE), add = TRUE)

   versions <- file.path(root, "R.framework", "Versions")
   for (name in c("4.4-arm64", "4.5-arm64"))
   {
      home <- file.path(versions, name, "Resources")
      dir.create(file.path(home, "bin"), recursive = TRUE)
      writeLines(
         c("#!/bin/sh", "R_HOME_DIR=/Library/Frameworks/R.framework/Resources"),
         file.path(home, "bin", "R")
      )
   }

   # the shared paths resolve to the current version, so it needs no rewrite
   file.symlink("4.5-arm64", file.path(versions, "Current"))
   expect_true(.rs.rInstallations.isOrthogonal(file.path(versions, "4.5-arm64", "Resources")))
   expect_false(.rs.rInstallations.isOrthogonal(file.path(versions, "4.4-arm64", "Resources")))

   # also when the current version is asked about through the link
   expect_true(.rs.rInstallations.isOrthogonal(file.path(versions, "Current", "Resources")))

})

test_that("installed R versions are listed once per session unless refreshed", {

   cache <- .rs.rInstallationsCache
   old <- cache$installed
   on.exit({
      if (is.null(old))
         rm(list = "installed", envir = cache)
      else
         cache$installed <- old
   }, add = TRUE)

   installed <- data.frame(
      version = "4.4.1",
      path    = "/opt/R/4.4.1",
      binary  = "/opt/R/4.4.1/bin/R",
      home    = "/opt/R/4.4.1/lib/R",
      stringsAsFactors = FALSE
   )
   cache$installed <- installed

   expect_identical(.rs.rInstallations.cached(), installed)
   match <- .rs.findInstalledRVersion("4.4", architecture = NULL, refresh = FALSE)
   expect_equal(match$binary, .rs.scalar("/opt/R/4.4.1/bin/R"))

})

test_that("rig older than the pinned release is passed over", {

   dir <- tempfile("rig-bin-")
   dir.create(dir)
   on.exit(unlink(dir, recursive = TRUE), add = TRUE)

   rig <- file.path(dir, if (.rs.platform.isWindows) "rig.cmd" else "rig")
   writeRig <- function(output)
   {
      writeLines(c(if (.rs.platform.isWindows) "@echo off" else "#!/bin/sh",
                   paste("echo", output)), rig)
      Sys.chmod(rig, "0755")
   }
   writeRig("RIG -- The R Installation Manager 0.9.1")
   expect_equal(.rs.rig.version(rig), package_version("0.9.1"))
   expect_identical(.rs.rig.find(paths = rig), "")

   writeRig(paste("RIG -- The R Installation Manager", .rs.rig.pinnedVersion))
   expect_equal(.rs.rig.version(rig), package_version(.rs.rig.pinnedVersion))
   expect_identical(.rs.rig.find(paths = rig), normalizePath(rig, winslash = "/"))

   writeRig("not rig")
   expect_null(.rs.rig.version(rig))

})

test_that("the machine architecture is reported as is where it can't differ", {

   expect_identical(.rs.rig.machine("arm64"), "arm64")
   if (!.rs.platform.isMacos)
      expect_identical(.rs.rig.machine("x86_64"), "x86_64")

})

test_that("a user-mode install leaves rig's default R setup as it was", {

   skip_if(.rs.platform.isWindows, "symlinks are not available")

   root <- tempfile("rig-r-")
   bin <- tempfile("rig-bin-")
   home <- tempfile("rig-home-")
   for (dir in c(root, bin, home))
      dir.create(dir)
   on.exit(unlink(c(root, bin, home), recursive = TRUE), add = TRUE)

   launchd <- fakeLaunchd(whichR = "/opt/homebrew/bin/R")
   dirs <- list(r_root = root, binary_dir = bin)
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, macos = TRUE)
   expect_setequal(names(paths), c("current", "R", "Rscript", "launchAgent"))

   # a quick link from before the install is kept
   writeLines("#!/bin/sh", paths[["Rscript"]])
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchd$launchctl)
   expect_identical(snapshot$pid, Sys.getpid())
   expect_false(snapshot$loaded)
   expect_identical(snapshot$whichR, "/opt/homebrew/bin/R")

   # nothing added: nothing removed, and launchd only consulted
   nothing <- list(removed = character(), restored = character(), failed = character())
   expect_identical(.rs.rig.userDefaultRestore(snapshot, launchd$launchctl), nothing)
   expect_identical(launchd$verbs(), c("list", "getenv", "list", "getenv"))
   expect_identical(.rs.rig.restoreMessages(nothing), character())

   # rig sets a default: the links (one of them dangling) and the agent,
   # which it loads, and loading sets RSTUDIO_WHICH_R
   installed <- file.path(root, "4.3.3")
   dir.create(installed)
   expect_true(file.symlink(installed, paths[["current"]]))
   expect_true(file.symlink(file.path(installed, "bin", "R"), paths[["R"]]))
   dir.create(dirname(paths[["launchAgent"]]), recursive = TRUE)
   writeLines("<plist/>", paths[["launchAgent"]])
   launchd$state$loaded <- TRUE
   launchd$state$whichR <- file.path(installed, "bin", "R")
   launchd$state$calls <- list()

   result <- .rs.rig.userDefaultRestore(snapshot, launchd$launchctl)
   expect_setequal(result$removed, unname(paths[c("current", "R", "launchAgent")]))
   expect_identical(result$restored, character())
   expect_identical(result$failed, character())
   expect_false(any(.rs.rig.pathExists(paths[c("current", "R", "launchAgent")])))
   expect_true(file.exists(paths[["Rscript"]]))
   expect_true(dir.exists(installed))

   # the agent is unloaded, and the variable it exported put back
   expect_identical(launchd$verbs(), c("list", "unload", "getenv", "setenv"))
   expect_false(launchd$state$loaded)
   expect_identical(launchd$state$whichR, "/opt/homebrew/bin/R")

   messages <- .rs.rig.restoreMessages(result)
   expect_length(messages, 1L)
   expect_match(messages, "^Removed the default R setup rig added outside RStudio: ")

})

test_that("RSTUDIO_WHICH_R is unset again when launchd had no value for it", {

   root <- tempfile("rig-r-")
   bin <- tempfile("rig-bin-")
   home <- tempfile("rig-home-")
   for (dir in c(root, bin, home))
      dir.create(dir)
   on.exit(unlink(c(root, bin, home), recursive = TRUE), add = TRUE)

   launchd <- fakeLaunchd()
   dirs <- list(r_root = root, binary_dir = bin)
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, macos = TRUE)
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchd$launchctl)
   expect_identical(snapshot$whichR, "")

   dir.create(dirname(paths[["launchAgent"]]), recursive = TRUE)
   writeLines("<plist/>", paths[["launchAgent"]])
   launchd$state$loaded <- TRUE
   launchd$state$whichR <- "/some/R"
   launchd$state$calls <- list()

   result <- .rs.rig.userDefaultRestore(snapshot, launchd$launchctl)
   expect_identical(result$removed, unname(paths[["launchAgent"]]))
   expect_identical(launchd$verbs(), c("list", "unload", "getenv", "unsetenv"))
   expect_identical(launchd$state$whichR, "")

   # off macOS there is no agent, and launchd is never consulted
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, macos = FALSE)
   expect_identical(names(paths), c("current", "R", "Rscript"))
   expect_identical(basename(paths[["current"]]), "current")
   launchd$state$calls <- list()
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchd$launchctl)
   expect_identical(.rs.rig.userDefaultRestore(snapshot, launchd$launchctl)$failed, character())
   expect_length(launchd$state$calls, 0L)

})

test_that("links rig repoints and an agent it rewrites are put back", {

   skip_if(.rs.platform.isWindows, "symlinks are not available")

   root <- tempfile("rig-r-")
   bin <- tempfile("rig-bin-")
   home <- tempfile("rig-home-")
   for (dir in c(root, bin, home))
      dir.create(dir)
   on.exit(unlink(c(root, bin, home), recursive = TRUE), add = TRUE)

   dirs <- list(r_root = root, binary_dir = bin)
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, macos = TRUE)

   # the default R was removed with 'rig rm': 'Current' and the quick links
   # dangle, and the agent (which rig keeps while any R is left) still names
   # the removed R, with launchd still exporting it from the last login
   removed <- file.path(root, "4.3.3")
   expect_true(file.symlink(removed, paths[["current"]]))
   expect_true(file.symlink(file.path(removed, "bin", "R"), paths[["R"]]))
   expect_true(file.symlink(file.path(removed, "bin", "Rscript"), paths[["Rscript"]]))
   dir.create(dirname(paths[["launchAgent"]]), recursive = TRUE)
   oldAgent <- sprintf("<plist>%s</plist>\n", file.path(removed, "bin", "R"))
   cat(oldAgent, file = paths[["launchAgent"]])
   launchd <- fakeLaunchd(loaded = TRUE, whichR = file.path(removed, "bin", "R"))
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchd$launchctl)
   expect_true(all(snapshot$existed))
   expect_identical(snapshot$agent, charToRaw(oldAgent))
   expect_true(snapshot$loaded)

   # rig sees no default (a dangling link does not exist to it), repoints
   # 'Current' at the new R, cannot replace the dangling quick links, and
   # rewrites, reloads and so re-exports the agent
   installed <- file.path(root, "4.2.3")
   dir.create(installed)
   unlink(paths[["current"]])
   expect_true(file.symlink(installed, paths[["current"]]))
   cat(sprintf("<plist>%s</plist>\n", file.path(installed, "bin", "R")), file = paths[["launchAgent"]])
   launchd$state$whichR <- file.path(installed, "bin", "R")
   launchd$state$calls <- list()

   result <- .rs.rig.userDefaultRestore(snapshot, launchd$launchctl)
   expect_identical(result$removed, character())
   expect_setequal(result$restored, unname(paths[c("current", "launchAgent")]))
   expect_identical(result$failed, character())

   # the links dangle as before, the agent is the old one, loaded, and
   # exporting the old R again
   expect_identical(unname(Sys.readlink(paths[["current"]])), removed)
   expect_identical(unname(Sys.readlink(paths[["R"]])), file.path(removed, "bin", "R"))
   expect_identical(.rs.rig.readBytes(paths[["launchAgent"]]), charToRaw(oldAgent))
   expect_identical(launchd$verbs(), c("list", "unload", "load", "getenv", "setenv"))
   expect_true(launchd$state$loaded)
   expect_identical(launchd$state$whichR, file.path(removed, "bin", "R"))
   expect_match(.rs.rig.restoreMessages(result), "^Put back the default R setup rig changed outside RStudio: ")

   # an agent rig merely loads (one that was up to date but unloaded) is
   # unloaded again
   launchd$state$calls <- list()
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchd$launchctl)
   launchd$state$loaded <- FALSE
   snapshot$loaded <- FALSE
   launchd$state$loaded <- TRUE
   launchd$state$calls <- list()
   result <- .rs.rig.userDefaultRestore(snapshot, launchd$launchctl)
   expect_identical(result, list(removed = character(), restored = character(), failed = character()))
   expect_identical(launchd$verbs(), c("list", "unload", "getenv"))
   expect_false(launchd$state$loaded)

})

test_that("what could not be undone after a user-mode install is reported", {

   skip_if(.rs.platform.isWindows, "symlinks are not available")
   skip_if(identical(Sys.info()[["effective_user"]], "root"), "root can remove anything")

   root <- tempfile("rig-r-")
   bin <- tempfile("rig-bin-")
   home <- tempfile("rig-home-")
   for (dir in c(root, bin, home))
      dir.create(dir)
   on.exit({
      Sys.chmod(bin, "0755")
      unlink(c(root, bin, home), recursive = TRUE)
   }, add = TRUE)

   dirs <- list(r_root = root, binary_dir = bin)
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, macos = TRUE)
   launchd <- fakeLaunchd()
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchd$launchctl)

   # rig sets a default, but the agent will not unload and the quick-link
   # directory cannot be written to any more
   installed <- file.path(root, "4.3.3")
   dir.create(installed)
   expect_true(file.symlink(installed, paths[["current"]]))
   expect_true(file.symlink(file.path(installed, "bin", "R"), paths[["R"]]))
   dir.create(dirname(paths[["launchAgent"]]), recursive = TRUE)
   writeLines("<plist/>", paths[["launchAgent"]])
   launchd$state$loaded <- TRUE
   launchd$state$whichR <- file.path(installed, "bin", "R")
   launchd$state$failing <- c("unload", "unsetenv")
   Sys.chmod(bin, "0555")

   result <- .rs.rig.userDefaultRestore(snapshot, launchd$launchctl)
   expect_setequal(result$removed, unname(paths[c("current", "launchAgent")]))
   expect_identical(result$restored, character())
   expect_length(result$failed, 3L)
   expect_match(result$failed, "launchctl unload", all = FALSE)
   expect_match(result$failed, "launchctl unsetenv", all = FALSE)
   expect_match(result$failed, paste("could not remove", paths[["R"]]), fixed = TRUE, all = FALSE)
   expect_true(.rs.rig.pathExists(paths[["R"]]))

   # one line per failure, after what did get done
   messages <- .rs.rig.restoreMessages(result)
   expect_length(messages, 4L)
   expect_match(messages[[1L]], "^Removed the default R setup")
   expect_match(messages[-1L], "^Could not undo a change rig made outside RStudio: ")

})

test_that("rig's default R setup is remembered before, and put back after, a user-mode install", {

   skip_if(.rs.platform.isWindows, "symlinks are not available")

   home <- tempfile("rig-home-")
   dir.create(home)
   on.exit(unlink(home, recursive = TRUE), add = TRUE)
   root <- file.path(home, "R")
   bin <- file.path(home, ".local", "bin")
   dir.create(root)

   rig <- fakeRig(home, list(r_root = root, binary_dir = bin))
   file <- file.path(home, "rig-install-snapshot.rds")
   launchd <- fakeLaunchd(whichR = "/opt/homebrew/bin/R")
   state <- .rs.rig.installState

   # the snapshot is taken (with rig's quick-link directory created first)
   # and kept on disk, for a session that does not outlive the install
   withr::with_envvar(c(HOME = home), {
      expect_identical(.rs.rig.userInstallBegin(rig, file, launchd$launchctl), character())
   })
   expect_true(dir.exists(bin))
   expect_true(file.exists(file))
   expect_identical(state$file, file)
   expect_identical(readRDS(file), state$snapshot)
   expect_identical(state$snapshot$pid, Sys.getpid())
   expect_false(any(state$snapshot$existed))
   if (.rs.platform.isMacos)
   {
      expect_identical(basename(state$snapshot$paths[["launchAgent"]]), "io.r-lib.rig.rstudio-which-r.plist")
      expect_true(startsWith(state$snapshot$paths[["launchAgent"]], home))
   }
   else
   {
      expect_false("launchAgent" %in% names(state$snapshot$paths))
   }

   # rig sets a default while installing
   paths <- state$snapshot$paths
   installed <- file.path(root, "4.3.3")
   dir.create(installed)
   expect_true(file.symlink(installed, paths[["current"]]))
   expect_true(file.symlink(file.path(installed, "bin", "R"), paths[["R"]]))
   expect_true(file.symlink(file.path(installed, "bin", "Rscript"), paths[["Rscript"]]))

   # once rig runs, its process is recorded in the snapshot too
   expect_identical(.rs.rig.userInstallStarted(4242L), character())
   expect_identical(state$snapshot$rigPid, 4242L)
   expect_identical(readRDS(file)$rigPid, 4242L)

   # which the end of the install undoes, dropping the snapshot
   messages <- .rs.rig.userInstallEnd(launchd$launchctl)
   expect_length(messages, 1L)
   expect_match(messages, "^Removed the default R setup rig added outside RStudio: ")
   expect_false(any(.rs.rig.pathExists(paths)))
   expect_true(dir.exists(installed))
   expect_false(file.exists(file))
   expect_null(state$snapshot)
   expect_null(state$file)

   # with no install under way there is nothing to undo
   expect_identical(.rs.rig.userInstallEnd(launchd$launchctl), character())

   # a snapshot that cannot be saved is reported, and still undone from memory
   unwritable <- file.path(home, "missing", "snapshot.rds")
   withr::with_envvar(c(HOME = home), {
      messages <- .rs.rig.userInstallBegin(rig, unwritable, launchd$launchctl)
   })
   expect_length(messages, 1L)
   expect_match(messages, "^Could not save rig's default R setup to ")
   expect_false(is.null(state$snapshot))
   expect_true(file.symlink(installed, paths[["current"]]))
   messages <- .rs.rig.userInstallEnd(launchd$launchctl)
   expect_match(messages, "^Removed the default R setup")
   expect_false(.rs.rig.pathExists(paths[["current"]]))

   # when rig cannot say where its files are, nothing is remembered
   failing <- fakeRig(home, NULL)
   expect_error(.rs.rig.userInstallBegin(failing, file, launchd$launchctl), "rig system dirs --user failed")
   expect_null(state$snapshot)
   expect_false(file.exists(file))
   expect_identical(.rs.rig.userInstallEnd(launchd$launchctl), character())

   # the snapshot is kept while something is left to undo, for a later try
   rig <- fakeRig(home, list(r_root = root, binary_dir = bin))
   withr::with_envvar(c(HOME = home), {
      expect_identical(.rs.rig.userInstallBegin(rig, file, launchd$launchctl), character())
   })
   expect_true(file.symlink(installed, paths[["current"]]))
   Sys.chmod(root, "0555")
   messages <- .rs.rig.userInstallEnd(launchd$launchctl)
   Sys.chmod(root, "0755")
   expect_match(messages, "^Could not undo a change rig made outside RStudio: could not remove ", all = FALSE)
   expect_true(file.exists(file))
   expect_null(state$snapshot)
   messages <- .rs.rig.userInstallRecover(file, launchctl = launchd$launchctl)
   expect_match(messages, "^Removed the default R setup")
   expect_false(file.exists(file))

})

test_that("a user-mode install waits for, and undoes, one another session started", {

   skip_if(.rs.platform.isWindows, "symlinks are not available")

   home <- tempfile("rig-home-")
   dir.create(home)
   on.exit(unlink(home, recursive = TRUE), add = TRUE)
   root <- file.path(home, "R")
   bin <- file.path(home, ".local", "bin")
   for (dir in c(root, bin))
      dir.create(dir, recursive = TRUE)

   rig <- fakeRig(home, list(r_root = root, binary_dir = bin))
   file <- file.path(home, "rig-install-snapshot.rds")
   launchd <- fakeLaunchd()
   state <- .rs.rig.installState
   paths <- .rs.rig.userDefaultPaths(list(r_root = root, binary_dir = bin), home = home, macos = FALSE)

   # another session took a snapshot and its rig has set the default
   theirs <- .rs.rig.userDefaultSnapshot(paths, launchd$launchctl)
   theirs$pid <- 999998L
   theirs$rigPid <- 999999L
   saveRDS(theirs, file)
   installed <- file.path(root, "4.3.3")
   dir.create(installed)
   expect_true(file.symlink(installed, paths[["current"]]))

   # while that session, or just its rig, runs, this one does not install
   alive <- function(pids) function(pid) pid %in% pids
   withr::with_envvar(c(HOME = home), {
      expect_error(.rs.rig.userInstallBegin(rig, file, launchd$launchctl, alive(999998L)), "another RStudio session")
      expect_error(.rs.rig.userInstallBegin(rig, file, launchd$launchctl, alive(999999L)), "another RStudio session")
   })
   expect_null(state$snapshot)
   expect_true(file.exists(file))
   expect_true(.rs.rig.pathExists(paths[["current"]]))

   # once both are gone, their default is undone before this install's
   # snapshot is taken, so it does not pass for setup to keep
   withr::with_envvar(c(HOME = home), {
      messages <- .rs.rig.userInstallBegin(rig, file, launchd$launchctl, alive(integer()))
   })
   expect_match(messages, "^Removed the default R setup")
   expect_false(.rs.rig.pathExists(paths[["current"]]))
   expect_identical(readRDS(file)$pid, Sys.getpid())
   expect_false(any(state$snapshot$existed))
   expect_identical(.rs.rig.userInstallEnd(launchd$launchctl), character())
   expect_false(file.exists(file))

})

test_that("an install a previous session did not get to undo is undone at startup", {

   skip_if(.rs.platform.isWindows, "symlinks are not available")

   home <- tempfile("rig-home-")
   dir.create(home)
   on.exit(unlink(home, recursive = TRUE), add = TRUE)
   root <- file.path(home, "R")
   bin <- file.path(home, ".local", "bin")
   for (dir in c(root, bin))
      dir.create(dir, recursive = TRUE)

   dirs <- list(r_root = root, binary_dir = bin)
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, macos = TRUE)
   launchd <- fakeLaunchd()
   file <- file.path(home, "rig-install-snapshot.rds")

   # no snapshot: nothing to do
   expect_identical(.rs.rig.userInstallRecover(file, launchctl = launchd$launchctl), character())

   # the session that took the snapshot exited mid-install, after rig had
   # set the default
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchd$launchctl)
   snapshot$pid <- 999999L
   saveRDS(snapshot, file)
   installed <- file.path(root, "4.3.3")
   dir.create(installed)
   expect_true(file.symlink(installed, paths[["current"]]))
   expect_true(file.symlink(file.path(installed, "bin", "R"), paths[["R"]]))

   # while that session runs, its install may still be under way
   stillRunning <- function(pid) identical(pid, 999999L)
   expect_identical(.rs.rig.userInstallRecover(file, stillRunning, launchd$launchctl), character())
   expect_true(file.exists(file))
   expect_true(.rs.rig.pathExists(paths[["current"]]))

   # as it may while the rig it started runs on without it
   snapshot$rigPid <- 999997L
   saveRDS(snapshot, file)
   rigRunning <- function(pid) identical(pid, 999997L)
   expect_identical(.rs.rig.userInstallRecover(file, rigRunning, launchd$launchctl), character())
   expect_true(file.exists(file))

   # once it is gone, the default is undone and the snapshot dropped
   gone <- function(pid) FALSE
   messages <- .rs.rig.userInstallRecover(file, gone, launchd$launchctl)
   expect_match(messages, "^Removed the default R setup rig added outside RStudio: ")
   expect_false(any(.rs.rig.pathExists(paths)))
   expect_false(file.exists(file))

   # a snapshot that cannot be read is dropped, and said so
   writeLines("not an rds file", file)
   messages <- .rs.rig.userInstallRecover(file, gone, launchd$launchctl)
   expect_match(messages, "^Could not read rig's default R setup from ")
   expect_false(file.exists(file))

   # this session's own process counts as running, a finished one does not
   expect_true(.rs.rig.processRunning(Sys.getpid()))
   expect_false(.rs.rig.processRunning(999999L))

})

test_that("rig's quick-link directory is created before a user-mode install", {

   home <- tempfile("rig-home-")
   dir.create(home)
   on.exit(unlink(home, recursive = TRUE), add = TRUE)

   # a fresh home has no ~/.local/bin yet (r-lib/rig#491)
   bin <- file.path(home, ".local", "bin")
   expect_false(dir.exists(bin))
   expect_true(.rs.rig.ensureBinaryDir(list(binary_dir = bin)))
   expect_true(dir.exists(bin))

   # an existing directory is left alone, contents included
   marker <- file.path(bin, "R-4.6.1")
   writeLines("#!/bin/sh", marker)
   expect_true(.rs.rig.ensureBinaryDir(list(binary_dir = bin)))
   expect_true(file.exists(marker))

   # nothing to create when rig did not report a directory
   expect_false(.rs.rig.ensureBinaryDir(list()))
   expect_false(.rs.rig.ensureBinaryDir(list(binary_dir = "")))

})
