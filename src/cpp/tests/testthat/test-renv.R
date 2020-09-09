#
# test-renv.R
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("renv")

test_that(".rs.rpc.renv_init() preserves current repositories", {
   
   skip_if_not_installed("renv")
   
   # scope repos option in this scope
   renv:::renv_scope_options(
      repos = list(RSPM = "https://packagemanager.rstudio.com/cran/latest")
   )
   
   # initialize project
   project <- tempfile("renv-project-")
   on.exit(unlink(project, recursive = TRUE), add = TRUE)
   renv:::quietly(.rs.rpc.renv_init(project))
   
   # check that the renv lockfile has the expected repositories
   lockpath <- file.path(project, "renv.lock")
   lockfile <- renv:::renv_lockfile_read(lockpath)
   
   # validate correct repositories
   expect_identical(
      as.list(lockfile$R$Repositories),
      list(RSPM = "https://packagemanager.rstudio.com/cran/latest")
   )
   
})
