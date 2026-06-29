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

# Point the air installation roots at sandboxes by overriding the data
# directory used by .rs.air.rootDir() and the home directory used by
# .rs.air.oldRootDir(). RSTUDIO_DATA_HOME is honored on every platform and is
# used verbatim (no 'rstudio' suffix), so .rs.air.rootDir() resolves to
# '<data>/air'. Returns the (current) sandbox root.
local_air_root <- function(envir = parent.frame()) {

   data <- tempfile("air-data-")
   home <- tempfile("air-home-")
   dir.create(data, recursive = TRUE)
   dir.create(home, recursive = TRUE)

   withr::local_envvar(
      c(RSTUDIO_DATA_HOME = data, HOME = home, USERPROFILE = home),
      .local_envir = envir
   )

   withr::defer(unlink(c(data, home), recursive = TRUE), envir = envir)

   .rs.air.rootDir()
}

# Create a fake air installation for the given version, in the given root.
install_fake_air <- function(version, withExe = TRUE, root = .rs.air.rootDir()) {

   binDir <- .rs.air.binDir(version, root)
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
   install_fake_air("v0.11.0")                 # v-prefixed tag; honored
   install_fake_air("0.12.0-beta.1")           # pre-release; excluded

   expect_identical(
      .rs.air.installedVersions(),
      c("v0.11.0", "0.10.1", "0.9.0", "0.4.0")
   )

})

test_that(".rs.air.installedVersions() also discovers copies in the legacy root", {

   local_air_root()

   # one copy in the current root, one only in the legacy root
   install_fake_air("0.9.0")
   install_fake_air("0.4.0", root = .rs.air.oldRootDir())

   expect_identical(
      .rs.air.installedVersions(),
      c("0.9.0", "0.4.0")
   )

})

test_that(".rs.air.ensureAvailable() reuses a copy from the legacy root", {

   local_air_root()

   # only available in the legacy location
   install_fake_air("0.7.1", root = .rs.air.oldRootDir())

   # make sure any 'air' on the PATH is not used
   oldPath <- Sys.getenv("PATH")
   Sys.setenv(PATH = tempdir())
   on.exit(Sys.setenv(PATH = oldPath), add = TRUE)

   # never query GitHub or install; the legacy copy must be reused as-is
   options(rstudio.air.version = "0.7.1", rstudio.air.autoinstall = FALSE)
   on.exit(options(rstudio.air.version = NULL, rstudio.air.autoinstall = NULL), add = TRUE)

   exe <- .rs.air.ensureAvailable()
   expect_identical(
      normalizePath(exe),
      normalizePath(.rs.air.exePath("0.7.1", .rs.air.oldRootDir()))
   )

})

test_that(".rs.air.ensureAvailable() prefers a copy of air on the PATH", {

   local_air_root()

   # an installed copy that should *not* win over the PATH
   install_fake_air("0.9.0")

   # put a fake air executable on the PATH
   pathDir <- tempfile("air-path-")
   dir.create(pathDir)
   on.exit(unlink(pathDir, recursive = TRUE), add = TRUE)

   exeName <- if (.rs.platform.isWindows) "air.exe" else "air"
   pathExe <- file.path(pathDir, exeName)
   file.create(pathExe)
   Sys.chmod(pathExe, mode = "0755")

   oldPath <- Sys.getenv("PATH")
   Sys.setenv(PATH = pathDir)
   on.exit(Sys.setenv(PATH = oldPath), add = TRUE)

   exe <- .rs.air.ensureAvailable()
   expect_identical(normalizePath(exe), normalizePath(pathExe))

})

test_that(".rs.air.ensureAvailable() errors when missing and autoinstall is disabled", {

   local_air_root()

   # make sure any 'air' on the PATH is not used
   oldPath <- Sys.getenv("PATH")
   Sys.setenv(PATH = tempdir())
   on.exit(Sys.setenv(PATH = oldPath), add = TRUE)

   # request a version that isn't installed, with autoinstall disabled
   options(rstudio.air.version = "0.7.1", rstudio.air.autoinstall = FALSE)
   on.exit(options(rstudio.air.version = NULL, rstudio.air.autoinstall = NULL), add = TRUE)

   expect_error(.rs.air.ensureAvailable(), "Air binary not found")

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
