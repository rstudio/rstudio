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

test_that("the rig download URL matches the platform and architecture", {

   url <- function(...) .rs.rig.downloadUrl(version = "0.10.0", ...)

   expect_match(url(sysname = "Darwin", machine = "arm64"),
                "/v0.10.0/rig-macos-arm64-0.10.0.tar.gz$")
   expect_match(url(sysname = "Darwin", machine = "x86_64"),
                "/rig-macos-x86_64-0.10.0.tar.gz$")
   expect_match(url(sysname = "Linux", machine = "x86_64"),
                "/rig-linux-x86_64-0.10.0.tar.gz$")
   expect_match(url(sysname = "Linux", machine = "aarch64"),
                "/rig-linux-aarch64-0.10.0.tar.gz$")

   # on Windows, the machine architecture comes from the environment
   expect_match(url(sysname = "Windows", machine = "x86-64", processorArch = "AMD64"),
                "/rig-windows-x86_64-0.10.0.zip$")
   expect_match(url(sysname = "Windows", machine = "x86-64", processorArch = "ARM64"),
                "/rig-windows-arm64-0.10.0.zip$")

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

test_that("the pinned rig archives have known checksums", {

   for (sysname in c("Darwin", "Linux", "Windows"))
   {
      for (machine in c("x86_64", "arm64"))
      {
         url <- .rs.rig.downloadUrl(
            sysname = sysname,
            machine = machine,
            processorArch = if (machine == "arm64") "ARM64" else "AMD64"
         )
         expect_match(.rs.rig.archiveChecksum(url), "^[0-9a-f]{64}$", info = url)
      }
   }

   # other releases are unknown
   expect_identical(.rs.rig.archiveChecksum(.rs.rig.downloadUrl(version = "0.9.0")), "")

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

test_that("a rig archive is extracted into the install root", {

   skip_on_os("windows")

   # build an archive with the layout of a rig release
   staging <- tempfile("rig-archive-")
   dir.create(file.path(staging, "bin"), recursive = TRUE)
   dir.create(file.path(staging, "share"), recursive = TRUE)
   writeLines("#!/bin/sh\necho rig", file.path(staging, "bin", "rig"))

   archive <- tempfile(fileext = ".tar.gz")
   owd <- setwd(staging)
   on.exit(setwd(owd), add = TRUE)
   utils::tar(archive, files = c("bin", "share"), compression = "gzip", tar = "internal")
   setwd(owd)

   root <- file.path(tempfile("rig-root-"), "rig")
   on.exit(unlink(dirname(root), recursive = TRUE), add = TRUE)

   rig <- .rs.rig.installArchive(archive, root = root)
   expect_true(file.exists(rig))
   expect_equal(basename(rig), "rig")
   expect_equal(normalizePath(dirname(dirname(rig))), normalizePath(root))
   expect_true(file.access(rig, mode = 1L) == 0L)

   # reinstalling replaces the previous copy
   writeLines("#!/bin/sh\necho rig2", file.path(staging, "bin", "rig"))
   setwd(staging)
   utils::tar(archive, files = c("bin", "share"), compression = "gzip", tar = "internal")
   setwd(owd)

   rig <- .rs.rig.installArchive(archive, root = root)
   expect_match(readLines(rig)[[2]], "rig2")

   # an archive without a rig binary is rejected, leaving the install intact
   bad <- tempfile(fileext = ".tar.gz")
   setwd(staging)
   utils::tar(bad, files = "share", compression = "gzip", tar = "internal")
   setwd(owd)

   expect_error(.rs.rig.installArchive(bad, root = root))
   expect_true(file.exists(rig))

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
