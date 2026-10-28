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

   skip_on_os("windows")

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
   if (!.rs.platform.isMacos && !.rs.platform.isWindows)
      expect_identical(.rs.rig.machine("x86_64"), "x86_64")

})

# a minimal PE image: a DOS header pointing at the PE signature, then the machine
writePe <- function(path, machine)
{
   dir.create(dirname(path), recursive = TRUE, showWarnings = FALSE)
   dos <- c(charToRaw("MZ"), rep(as.raw(0x00), 58L), as.raw(c(0x40, 0x00, 0x00, 0x00)))
   pe <- c(as.raw(c(0x50, 0x45, 0x00, 0x00)), machine)
   writeBin(c(dos, pe), path)
}

test_that("the architecture of a Windows program is read from its PE header", {

   root <- tempfile("pe-")
   on.exit(unlink(root, recursive = TRUE), add = TRUE)

   # 0xAA64 is ARM64, 0x8664 x64, 0x014C 32-bit, 0xA641 ARM64EC
   writePe(file.path(root, "arm64.dll"), as.raw(c(0x64, 0xaa)))
   writePe(file.path(root, "x64.dll"), as.raw(c(0x64, 0x86)))
   writePe(file.path(root, "i386.dll"), as.raw(c(0x4c, 0x01)))
   writePe(file.path(root, "arm64ec.dll"), as.raw(c(0x41, 0xa6)))
   expect_identical(.rs.rInstallations.peMachine(file.path(root, "arm64.dll")), "arm64")
   expect_identical(.rs.rInstallations.peMachine(file.path(root, "x64.dll")), "x86_64")
   expect_identical(.rs.rInstallations.peMachine(file.path(root, "i386.dll")), "i386")
   expect_identical(.rs.rInstallations.peMachine(file.path(root, "arm64ec.dll")), "")

   # not a PE image, cut short, or missing
   writeLines("not a program", file.path(root, "text.dll"))
   expect_identical(.rs.rInstallations.peMachine(file.path(root, "text.dll")), "")
   writeBin(charToRaw("MZ"), file.path(root, "short.dll"))
   expect_identical(.rs.rInstallations.peMachine(file.path(root, "short.dll")), "")
   expect_identical(.rs.rInstallations.peMachine(file.path(root, "missing.dll")), "")

})

test_that("the architectures of a Windows R build are read from R.dll", {

   skip_if_not(.rs.platform.isWindows, "not Windows")

   root <- tempfile("r-builds-")
   on.exit(unlink(root, recursive = TRUE), add = TRUE)

   # an ARM64 R keeps R.dll in bin; an x64 R in bin\x64; an older R also in bin\i386
   writePe(file.path(root, "arm64", "bin", "R.dll"), as.raw(c(0x64, 0xaa)))
   writePe(file.path(root, "x86_64", "bin", "x64", "R.dll"), as.raw(c(0x64, 0x86)))
   writePe(file.path(root, "multiarch", "bin", "x64", "R.dll"), as.raw(c(0x64, 0x86)))
   writePe(file.path(root, "multiarch", "bin", "i386", "R.dll"), as.raw(c(0x4c, 0x01)))
   expect_identical(.rs.rInstallations.architectures(file.path(root, "arm64")), "arm64")
   expect_identical(.rs.rInstallations.architectures(file.path(root, "x86_64")), "x86_64")
   expect_identical(.rs.rInstallations.architectures(file.path(root, "multiarch")), c("x86_64", "i386"))
   expect_identical(.rs.rInstallations.architectures(file.path(root, "missing")), character())

   # the R running these tests
   expected <- c(aarch64 = "arm64", x86_64 = "x86_64")[[R.version$arch]]
   expect_identical(.rs.rInstallations.architectures(R.home()), expected)

   # the ARM64 build wins over an x64 build of the same version, whichever is listed first
   installed <- data.frame(
      version = c("4.6.1", "4.6.1"),
      path    = file.path(root, c("x86_64", "arm64")),
      binary  = file.path(root, c("x86_64", "arm64"), "bin", "R.exe"),
      home    = file.path(root, c("x86_64", "arm64")),
      stringsAsFactors = FALSE
   )
   match <- .rs.findInstalledRVersion("4.6.1", installed, architecture = "arm64")
   expect_equal(match$home, .rs.scalar(file.path(root, "arm64")))

   # an x64 build is still used when it is the only one
   match <- .rs.findInstalledRVersion("4.6.1", installed[1L, ], architecture = "arm64")
   expect_equal(match$home, .rs.scalar(file.path(root, "x86_64")))

   # the x64 build is found when it was the one asked for (installing R)
   match <- .rs.findInstalledRVersion("4.6.1", installed[2:1, ], architecture = "x86_64")
   expect_equal(match$home, .rs.scalar(file.path(root, "x86_64")))

})

test_that("installing R offers a choice of build only where both builds run", {

   if (.rs.platform.isMacos || .rs.platform.isWindows)
      expect_identical(.rs.rig.installArchitectures("arm64"), c("arm64", "x86_64"))
   else
      expect_identical(.rs.rig.installArchitectures("arm64"), character())

   expect_identical(.rs.rig.installArchitectures("x86-64"), character())
   expect_identical(.rs.rig.installArchitectures("x86_64"), character())

})

test_that("an ARM64 Windows machine is recognized even from an emulated R", {

   skip_if_not(.rs.platform.isWindows, "not Windows")

   root <- tempfile("kernel32-")
   on.exit(unlink(root, recursive = TRUE), add = TRUE)

   writePe(file.path(root, "arm64", "kernel32.dll"), as.raw(c(0x64, 0xaa)))
   writePe(file.path(root, "x64", "kernel32.dll"), as.raw(c(0x64, 0x86)))

   # an x64 R on an ARM64 machine reports x86-64; the system DLL tells the truth
   expect_identical(.rs.rig.machine("x86-64", file.path(root, "arm64", "kernel32.dll")), "arm64")
   expect_identical(.rs.rig.machine("aarch64", file.path(root, "arm64", "kernel32.dll")), "arm64")
   expect_identical(.rs.rig.machine("x86-64", file.path(root, "x64", "kernel32.dll")), "x86-64")
   expect_identical(.rs.rig.machine("x86-64", file.path(root, "missing.dll")), "x86-64")

   # only an ARM64 machine prefers a build
   expect_identical(.rs.rInstallations.preferredArchitecture("arm64"), "arm64")
   expect_null(.rs.rInstallations.preferredArchitecture("x86-64"))

})
