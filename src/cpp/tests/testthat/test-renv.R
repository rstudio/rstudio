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
   expect_identical(.rs.renv.lockfileRVersion(project), "")
   
   # a lockfile recording an R version
   lockfile <- file.path(project, "renv.lock")
   writeLines('{"R": {"Version": "4.4.1", "Repositories": []}, "Packages": {}}', lockfile)
   expect_identical(.rs.renv.lockfileRVersion(project), "4.4.1")
   
   # a lockfile without an R version
   writeLines('{"R": {"Repositories": []}, "Packages": {}}', lockfile)
   expect_identical(.rs.renv.lockfileRVersion(project), "")
   
   # a malformed lockfile
   writeLines('{"R": {"Version": ', lockfile)
   expect_identical(.rs.renv.lockfileRVersion(project), "")
   
})

test_that("a restore is offered only when the lockfile records packages", {
   
   project <- tempfile("renv-project-")
   dir.create(project)
   on.exit(unlink(project, recursive = TRUE), add = TRUE)
   
   lockfile <- file.path(project, "renv.lock")
   expect_false(.rs.renv.lockfileHasPackages(project))
   
   writeLines('{"R": {"Version": "4.4.1"}, "Packages": {}}', lockfile)
   expect_false(.rs.renv.lockfileHasPackages(project))
   
   writeLines('{"R": {"Version": "4.4.1"}, "Packages": {"jsonlite": {"Package": "jsonlite", "Version": "1.8.9"}}}', lockfile)
   expect_true(.rs.renv.lockfileHasPackages(project))
   
   writeLines('{"R": {', lockfile)
   expect_false(.rs.renv.lockfileHasPackages(project))
   
})

test_that("a custom lockfile location is honored", {
   
   project <- tempfile("renv-project-")
   dir.create(project)
   on.exit(unlink(project, recursive = TRUE), add = TRUE)
   
   expect_identical(.rs.renv.lockfilePath(project), file.path(project, "renv.lock"))
   
   # relative to the project
   old <- Sys.getenv("RENV_PATHS_LOCKFILE", unset = NA)
   on.exit({
      if (is.na(old)) Sys.unsetenv("RENV_PATHS_LOCKFILE") else Sys.setenv(RENV_PATHS_LOCKFILE = old)
   }, add = TRUE)
   
   Sys.setenv(RENV_PATHS_LOCKFILE = "lockfiles/dev.lock")
   expect_identical(.rs.renv.lockfilePath(project), file.path(project, "lockfiles/dev.lock"))
   
   # absolute
   lockfile <- file.path(project, "elsewhere.lock")
   Sys.setenv(RENV_PATHS_LOCKFILE = lockfile)
   expect_identical(.rs.renv.lockfilePath(project), lockfile)
   
   writeLines('{"R": {"Version": "4.3.2"}}', lockfile)
   expect_identical(.rs.renv.lockfileRVersion(project), "4.3.2")
   
})
