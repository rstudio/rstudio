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

   calls <- list()
   launchctl <- function(args)
   {
      calls[[length(calls) + 1L]] <<- args
      if (identical(args[[1L]], "getenv")) "/opt/homebrew/bin/R" else ""
   }

   dirs <- list(r_root = root, binary_dir = bin)
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, platform = "macos")
   expect_setequal(names(paths), c("current", "R", "Rscript", "launchAgent"))

   # a quick link from before the install is kept
   writeLines("#!/bin/sh", paths[["Rscript"]])
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchctl, registry = NULL)

   # nothing added: nothing removed, and launchd left alone
   expect_identical(.rs.rig.userDefaultRestore(snapshot, launchctl, registry = NULL), character())
   expect_length(calls, 1L)

   # rig sets a default: the links (one of them dangling) and the agent
   installed <- file.path(root, "4.3.3")
   dir.create(installed)
   expect_true(file.symlink(installed, paths[["current"]]))
   expect_true(file.symlink(file.path(installed, "bin", "R"), paths[["R"]]))
   dir.create(dirname(paths[["launchAgent"]]), recursive = TRUE)
   writeLines("<plist/>", paths[["launchAgent"]])

   removed <- .rs.rig.userDefaultRestore(snapshot, launchctl, registry = NULL)
   expect_setequal(removed, unname(paths[c("current", "R", "launchAgent")]))
   expect_false(any(.rs.rig.pathExists(paths[c("current", "R", "launchAgent")])))
   expect_true(file.exists(paths[["Rscript"]]))
   expect_true(dir.exists(installed))

   # the agent is unloaded, and the variable it exported put back
   expect_identical(calls[[2L]], c("unload", paths[["launchAgent"]]))
   expect_identical(calls[[3L]], c("setenv", "RSTUDIO_WHICH_R", "/opt/homebrew/bin/R"))
   expect_length(calls, 3L)

})

test_that("RSTUDIO_WHICH_R is unset again when launchd had no value for it", {

   root <- tempfile("rig-r-")
   bin <- tempfile("rig-bin-")
   home <- tempfile("rig-home-")
   for (dir in c(root, bin, home))
      dir.create(dir)
   on.exit(unlink(c(root, bin, home), recursive = TRUE), add = TRUE)

   calls <- list()
   launchctl <- function(args)
   {
      calls[[length(calls) + 1L]] <<- args
      ""
   }

   dirs <- list(r_root = root, binary_dir = bin)
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, platform = "macos")
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchctl, registry = NULL)

   dir.create(dirname(paths[["launchAgent"]]), recursive = TRUE)
   writeLines("<plist/>", paths[["launchAgent"]])
   expect_identical(.rs.rig.userDefaultRestore(snapshot, launchctl, registry = NULL), unname(paths[["launchAgent"]]))
   expect_identical(calls[[3L]], c("unsetenv", "RSTUDIO_WHICH_R"))

   # off macOS there is no agent, and launchd is never consulted
   paths <- .rs.rig.userDefaultPaths(dirs, home = home, platform = "linux")
   expect_identical(names(paths), c("current", "R", "Rscript"))
   expect_identical(basename(paths[["current"]]), "current")
   calls <- list()
   snapshot <- .rs.rig.userDefaultSnapshot(paths, launchctl, registry = NULL)
   expect_length(calls, 0L)

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

test_that("a Windows user-mode install leaves the registry default and user PATH as they were", {

   bin <- tempfile("rig-bin-")
   dir.create(bin)
   on.exit(unlink(bin, recursive = TRUE), add = TRUE)

   # an in-memory registry in place of PowerShell
   store <- new.env(parent = emptyenv())
   registry <- function() as.list(store)
   registryWrite <- function(ops)
   {
      for (op in ops)
      {
         id <- .rs.rig.registryId(op$key, op$name)
         if (identical(op$op, "remove"))
            rm(list = id, envir = store)
         else
            assign(id, list(type = op$type, data = op$data), envir = store)
      }
   }

   pathId  <- .rs.rig.registryId("HKCU:\\Environment", "Path")
   rId     <- .rs.rig.registryId("HKCU:\\Software\\R-core\\R", "Current Version")
   rPathId <- .rs.rig.registryId("HKCU:\\Software\\R-core\\R", "InstallPath")
   r64Id   <- .rs.rig.registryId("HKCU:\\Software\\R-core\\R64", "Current Version")
   assign(pathId, list(type = "ExpandString", data = "C:\\Windows;%USERPROFILE%\\bin"), envir = store)
   assign(rId, list(type = "String", data = "4.6.1"), envir = store)

   paths <- .rs.rig.userDefaultPaths(list(binary_dir = bin), platform = "windows")
   expect_identical(basename(unname(paths)), c("R.exe", "RS.exe", "Rscript.exe"))
   snapshot <- .rs.rig.userDefaultSnapshot(paths, registry = registry, binaryDir = bin)

   # nothing changed: nothing to revert
   expect_identical(
      .rs.rig.userDefaultRestore(snapshot, registry = registry, registryWrite = registryWrite),
      character()
   )

   # rig sets its default: the shims, the registry default, and its PATH entry
   for (shim in paths)
      writeLines("", shim)
   assign(pathId, list(type = "ExpandString", data = paste0(bin, ";C:\\Windows;%USERPROFILE%\\bin")), envir = store)
   assign(rId, list(type = "String", data = "4.2.3"), envir = store)
   assign(rPathId, list(type = "String", data = "C:\\Users\\me\\AppData\\Roaming\\rig\\data\\r\\R-4.2.3"), envir = store)
   assign(r64Id, list(type = "String", data = "4.2.3"), envir = store)

   reverted <- .rs.rig.userDefaultRestore(snapshot, registry = registry, registryWrite = registryWrite)
   expect_false(any(file.exists(paths)))
   expect_setequal(reverted, c(unname(paths), rId, rPathId, r64Id, sprintf("user PATH entry %s", bin)))
   expect_identical(store[[pathId]], list(type = "ExpandString", data = "C:\\Windows;%USERPROFILE%\\bin"))
   expect_identical(store[[rId]], list(type = "String", data = "4.6.1"))
   expect_false(exists(rPathId, envir = store, inherits = FALSE))
   expect_false(exists(r64Id, envir = store, inherits = FALSE))

   # a registry that can't be read is reported, not fatal
   broken <- function() stop("no powershell")
   expect_warning(snapshot <- .rs.rig.userDefaultSnapshot(paths, registry = broken, binaryDir = bin))
   expect_null(snapshot$registry)
   expect_identical(.rs.rig.userDefaultRestore(snapshot, registry = broken, registryWrite = registryWrite), character())

})

test_that("rig's PATH entry is taken out without disturbing other changes", {

   bin <- "C:\\Users\\me\\.local\\bin"
   pathId <- .rs.rig.registryId("HKCU:\\Environment", "Path")

   # no user PATH before, only rig's entry after: the value goes away
   ops <- .rs.rig.registryRestoreOps(list(), setNames(list(list(type = "ExpandString", data = bin)), pathId), bin)
   expect_length(ops, 1L)
   expect_identical(ops[[1L]]$op, "remove")

   # rig's entry (in another case) plus one added by something else: only rig's goes
   before <- setNames(list(list(type = "String", data = "C:\\Windows")), pathId)
   after <- setNames(list(list(type = "String", data = paste0(toupper(bin), ";C:\\Windows;D:\\tools"))), pathId)
   ops <- .rs.rig.registryRestoreOps(before, after, bin)
   expect_length(ops, 1L)
   expect_identical(ops[[1L]]$op, "set")
   expect_identical(ops[[1L]]$type, "String")
   expect_identical(ops[[1L]]$data, "C:\\Windows;D:\\tools")

   # already on the PATH beforehand: left alone, as is a value that disappeared
   expect_length(.rs.rig.registryRestoreOps(after, after, bin), 0L)
   expect_length(.rs.rig.registryRestoreOps(before, list(), bin), 0L)

})

test_that("the registry scripts quote their PowerShell strings", {

   script <- .rs.rig.registryWriteScript(list(
      list(op = "set", key = "HKCU:\\Environment", name = "Path", type = "ExpandString", data = "C:\\it's;D:\\x"),
      list(op = "remove", key = "HKCU:\\Software\\R-core\\R", name = "Current Version")
   ))
   expect_identical(
      script[[1L]],
      "Set-ItemProperty -LiteralPath 'HKCU:\\Environment' -Name 'Path' -Type ExpandString -Value 'C:\\it''s;D:\\x'"
   )
   expect_identical(
      script[[2L]],
      "Remove-ItemProperty -LiteralPath 'HKCU:\\Software\\R-core\\R' -Name 'Current Version' -ErrorAction SilentlyContinue"
   )

   read <- .rs.rig.registryReadScript()
   expect_true(any(grepl("@('HKCU:\\Environment', 'Path')", read, fixed = TRUE)))
   expect_identical(tail(read, 1L), "ConvertTo-Json -InputObject @($values) -Compress")

})
