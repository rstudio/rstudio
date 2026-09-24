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
      stringsAsFactors = FALSE
   )

   # exact match wins
   match <- .rs.rig.findRVersion("4.4.0", installed)
   expect_equal(match$version, .rs.scalar("4.4.0"))
   expect_equal(match$binary, .rs.scalar("/opt/R/4.4.0/bin/R"))

   # otherwise the newest patch release of the same minor
   match <- .rs.rig.findRVersion("4.4.1", installed)
   expect_equal(match$version, .rs.scalar("4.4.2"))

   # a version spec without a patch component also works
   match <- .rs.rig.findRVersion("4.3", installed)
   expect_equal(match$version, .rs.scalar("4.3.3"))

   # nothing installed from that minor
   expect_null(.rs.rig.findRVersion("4.2.0", installed))
   expect_null(.rs.rig.findRVersion("4.4.1", installed[0, ]))
   expect_null(.rs.rig.findRVersion("bogus", installed))

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
   expect_identical(names(installed), c("version", "path", "binary"))

})

test_that("the requested R version is compared with the running one", {

   status <- .rs.rVersionStatus("4.4.1", "3.6.0", "4.6.1", current = "4.4.3")
   expect_equal(status$requested, .rs.scalar("4.4.1"))
   expect_equal(status$current, .rs.scalar("4.4.3"))
   expect_true(status$matches)
   expect_true(status$supported)

   status <- .rs.rVersionStatus("4.2.0", "3.6.0", "4.6.1", current = "4.4.3")
   expect_false(status$matches)
   expect_true(status$supported)

   # outside the supported range
   expect_false(.rs.rVersionStatus("3.5.3", "3.6.0", "4.6.1", current = "4.4.3")$supported)
   expect_false(.rs.rVersionStatus("4.7.0", "3.6.0", "4.6.1", current = "4.4.3")$supported)

   # bounds are inclusive
   expect_true(.rs.rVersionStatus("3.6.0", "3.6.0", "4.6.1", current = "4.4.3")$supported)
   expect_true(.rs.rVersionStatus("4.6.1", "3.6.0", "4.6.1", current = "4.4.3")$supported)

   # a malformed version is neither a match nor supported
   status <- .rs.rVersionStatus("release", "3.6.0", "4.6.1", current = "4.4.3")
   expect_false(status$matches)
   expect_false(status$supported)

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
