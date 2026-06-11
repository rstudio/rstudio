#
# test-air.R
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

context("air")

# Point the air installation root at a sandbox by overriding the home
# directory used by .rs.air.rootDir(). Returns the sandbox root.
local_air_root <- function(envir = parent.frame()) {

   home <- tempfile("air-home-")
   dir.create(home, recursive = TRUE)

   withr::local_envvar(
      c(HOME = home, USERPROFILE = home),
      .local_envir = envir
   )

   withr::defer(unlink(home, recursive = TRUE), envir = envir)

   .rs.air.rootDir()
}

# Create a fake air installation for the given version.
install_fake_air <- function(version, withExe = TRUE) {

   binDir <- .rs.air.binDir(version)
   dir.create(binDir, recursive = TRUE, showWarnings = FALSE)

   if (withExe) {
      exe <- if (.rs.platform.isWindows) "air.exe" else "air"
      file.create(file.path(binDir, exe))
   }
}

test_that(".rs.air.installedVersions() finds and sorts installed versions", {

   local_air_root()

   # nothing installed yet
   expect_identical(.rs.air.installedVersions(), character())

   install_fake_air("0.4.0")
   install_fake_air("0.10.1")
   install_fake_air("0.9.0")
   install_fake_air("0.5.0", withExe = FALSE)  # missing binary; excluded
   install_fake_air("not-a-version")           # unparseable; excluded

   expect_identical(
      .rs.air.installedVersions(),
      c("0.10.1", "0.9.0", "0.4.0")
   )

})

test_that(".rs.air.ensureAvailable() prefers installed versions over GitHub", {

   local_air_root()

   install_fake_air("0.7.1")
   install_fake_air("0.9.0")

   # make sure any 'air' on the PATH is not used
   oldPath <- Sys.getenv("PATH")
   Sys.setenv(PATH = tempdir())
   on.exit(Sys.setenv(PATH = oldPath), add = TRUE)

   # fail if we attempt to resolve the default version, as doing
   # so requires querying GitHub for the latest release
   trace(.rs.air.defaultVersion, quote(stop("GitHub was queried")), print = FALSE)
   on.exit(untrace(.rs.air.defaultVersion), add = TRUE)

   # with no version requested, the newest installed version is used
   exe <- .rs.air.ensureAvailable()
   expect_identical(normalizePath(exe), normalizePath(.rs.air.exePath("0.9.0")))

   # an explicitly-requested version takes precedence
   options(rstudio.air.version = "0.7.1")
   on.exit(options(rstudio.air.version = NULL), add = TRUE)

   exe <- .rs.air.ensureAvailable()
   expect_identical(normalizePath(exe), normalizePath(.rs.air.exePath("0.7.1")))

})
