#
# test-renv.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

context("renv")

# TODO: This test appears to be unreliable; some runs appear to be
# unable to contact RSPM?
# test_that(".rs.rpc.renv_init() preserves current repositories", {
#    
#    skip_if_not_installed("renv")
#    
#    # scope repos option in this scope
#    renv:::renv_scope_options(
#       repos = list(RSPM = "https://packagemanager.posit.co/cran/latest")
#    )
#    
#    # initialize project
#    project <- tempfile("renv-project-")
#    on.exit(unlink(project, recursive = TRUE), add = TRUE)
#    renv:::quietly(.rs.rpc.renv_init(project))
#    
#    # check that the renv lockfile has the expected repositories
#    lockpath <- file.path(project, "renv.lock")
#    lockfile <- renv:::renv_lockfile_read(lockpath)
#    
#    # validate correct repositories
#    expect_identical(
#       as.list(lockfile$R$Repositories),
#       list(RSPM = "https://packagemanager.posit.co/cran/latest")
#    )
#    
# })

test_that("the R version is read from the project's lockfile", {
   
   project <- tempfile("renv-project-")
   dir.create(project)
   on.exit(unlink(project, recursive = TRUE), add = TRUE)
   
   # no lockfile
   expect_null(.rs.renv.readLockfile(project))
   expect_identical(.rs.renv.lockfileRVersion(NULL), "")
   
   # a lockfile recording an R version
   lockfile <- file.path(project, "renv.lock")
   writeLines('{"R": {"Version": "4.4.1", "Repositories": []}, "Packages": {}}', lockfile)
   expect_identical(.rs.renv.lockfileRVersion(.rs.renv.readLockfile(project)), "4.4.1")
   
   # a lockfile without an R version
   writeLines('{"R": {"Repositories": []}, "Packages": {}}', lockfile)
   expect_identical(.rs.renv.lockfileRVersion(.rs.renv.readLockfile(project)), "")
   
   # a version that isn't a version number is ignored, as it is shown to the user
   writeLines('{"R": {"Version": "<img src=x onerror=alert(1)>"}, "Packages": {}}', lockfile)
   expect_identical(.rs.renv.lockfileRVersion(.rs.renv.readLockfile(project)), "")
   
   # a malformed lockfile
   writeLines('{"R": {"Version": ', lockfile)
   expect_null(.rs.renv.readLockfile(project))
   
   # a directory in place of the lockfile
   unlink(lockfile)
   dir.create(lockfile)
   expect_null(.rs.renv.readLockfile(project))
   
})

test_that("a restore is offered only when the lockfile records packages", {
   
   expect_false(.rs.renv.lockfileHasPackages(NULL))
   expect_false(.rs.renv.lockfileHasPackages(list(R = list(Version = "4.4.1"), Packages = list())))
   
   packages <- list(jsonlite = list(Package = "jsonlite", Version = "1.8.9"))
   expect_true(.rs.renv.lockfileHasPackages(list(R = list(Version = "4.4.1"), Packages = packages)))
   
})

test_that("the lockfile is found where renv looks for it", {
   
   project <- tempfile("renv-project-")
   dir.create(project)
   on.exit(unlink(project, recursive = TRUE), add = TRUE)
   
   vars <- c("RENV_PATHS_LOCKFILE", "RENV_PROFILE")
   old <- Sys.getenv(vars, unset = NA, names = TRUE)
   Sys.unsetenv(vars)
   on.exit({
      set <- old[!is.na(old)]
      Sys.unsetenv(vars)
      if (length(set))
         do.call(Sys.setenv, as.list(set))
   }, add = TRUE)
   
   expect_identical(.rs.renv.lockfilePath(project), file.path(project, "renv.lock"))
   
   # relative to the project
   Sys.setenv(RENV_PATHS_LOCKFILE = "lockfiles/dev.lock")
   expect_identical(.rs.renv.lockfilePath(project), file.path(project, "lockfiles/dev.lock"))
   
   # a trailing slash names a directory holding renv.lock
   Sys.setenv(RENV_PATHS_LOCKFILE = "lockfiles/")
   expect_identical(.rs.renv.lockfilePath(project), file.path(project, "lockfiles/renv.lock"))
   
   # absolute
   lockfile <- file.path(project, "elsewhere.lock")
   Sys.setenv(RENV_PATHS_LOCKFILE = lockfile)
   expect_identical(.rs.renv.lockfilePath(project), lockfile)
   
   writeLines('{"R": {"Version": "4.3.2"}}', lockfile)
   expect_identical(.rs.renv.lockfileRVersion(.rs.renv.readLockfile(project)), "4.3.2")
   Sys.unsetenv("RENV_PATHS_LOCKFILE")
   
   # the active profile's lockfile, named by the environment or the project
   profileLockfile <- file.path(project, "renv", "profiles", "dev", "renv.lock")
   Sys.setenv(RENV_PROFILE = "dev")
   expect_identical(.rs.renv.lockfilePath(project), profileLockfile)
   
   Sys.unsetenv("RENV_PROFILE")
   dir.create(file.path(project, "renv"))
   writeLines("dev", file.path(project, "renv", "profile"))
   expect_identical(.rs.renv.lockfilePath(project), profileLockfile)
   
   # the default profile uses the project's own lockfile
   writeLines("default", file.path(project, "renv", "profile"))
   expect_identical(.rs.renv.lockfilePath(project), file.path(project, "renv.lock"))
   
})

test_that("the project's R version is compared with the running one", {
   
   project <- tempfile("renv-project-")
   dir.create(project)
   on.exit(unlink(project, recursive = TRUE), add = TRUE)
   
   old <- Sys.getenv(c("RENV_PATHS_LOCKFILE", "RENV_PROFILE"), unset = NA, names = TRUE)
   Sys.unsetenv(names(old))
   on.exit({
      set <- old[!is.na(old)]
      if (length(set))
         do.call(Sys.setenv, as.list(set))
   }, add = TRUE)
   
   writeLines(
      '{"R": {"Version": "4.2.3"}, "Packages": {"jsonlite": {"Package": "jsonlite"}}}',
      file.path(project, "renv.lock")
   )
   
   # nothing requested
   empty <- tempfile("renv-project-")
   dir.create(empty)
   on.exit(unlink(empty, recursive = TRUE), add = TRUE)
   check <- .rs.projectRVersionCheck(empty, "", "3.6.0", FALSE, FALSE, current = "4.4.1")
   expect_equal(check$type, .rs.scalar("none"))
   
   # a mismatch with the lockfile
   check <- .rs.projectRVersionCheck(project, "", "3.6.0", TRUE, FALSE, current = "4.4.1")
   expect_equal(check$type, .rs.scalar("mismatch"))
   expect_equal(check$requested, .rs.scalar("4.2.3"))
   expect_equal(check$current, .rs.scalar("4.4.1"))
   expect_equal(check$source, .rs.scalar("lockfile"))
   expect_true(check$supported)
   expect_null(check$installed)
   
   # the project file wins over the lockfile
   check <- .rs.projectRVersionCheck(project, "4.1", "3.6.0", TRUE, FALSE, current = "4.4.1")
   expect_equal(check$requested, .rs.scalar("4.1"))
   expect_equal(check$source, .rs.scalar("project"))
   
   # versions older than RStudio supports
   check <- .rs.projectRVersionCheck(project, "3.5.3", "3.6.0", TRUE, FALSE, current = "4.4.1")
   expect_false(check$supported)
   
   # an invalid version in the project file is ignored
   check <- .rs.projectRVersionCheck(project, "<b>4.1</b>", "3.6.0", TRUE, FALSE, current = "4.4.1")
   expect_equal(check$type, .rs.scalar("none"))
   
   # a match with an empty renv library offers a restore, but only with renv
   # active in the project
   check <- .rs.projectRVersionCheck(project, "", "3.6.0", TRUE, FALSE, current = "4.2.1")
   expect_equal(check$type, .rs.scalar("restore"))
   check <- .rs.projectRVersionCheck(project, "", "3.6.0", FALSE, FALSE, current = "4.2.1")
   expect_equal(check$type, .rs.scalar("none"))
   
   # no restore for a version set in the project file
   check <- .rs.projectRVersionCheck(project, "4.2", "3.6.0", TRUE, FALSE, current = "4.2.1")
   expect_equal(check$type, .rs.scalar("none"))
   
})
