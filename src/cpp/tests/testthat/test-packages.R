#
# test-packages.R
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

test_that(".rs.isPackageManagementCall detects namespace-qualified mutators", {
   # qualified calls use the literal namespace, so these hold whether or not the
   # packages are installed
   expect_true(.rs.isPackageManagementCall("renv::install(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("renv::update()"))
   expect_true(.rs.isPackageManagementCall("renv::remove(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("renv::restore()"))
   expect_true(.rs.isPackageManagementCall("renv::rebuild(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("renv:::update()"))
   expect_true(.rs.isPackageManagementCall("utils::install.packages(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("utils::remove.packages(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("devtools::install_github(\"r-lib/cli\")"))
   expect_true(.rs.isPackageManagementCall("devtools::load_all(\".\")"))
   expect_true(.rs.isPackageManagementCall("remotes::install_version(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("pak::pkg_install(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("pacman::p_load(dplyr)"))
   expect_true(.rs.isPackageManagementCall("BiocManager::install(\"limma\")"))
})

test_that(".rs.isPackageManagementCall detects unqualified mutators from utils", {
   # utils is attached in a default session, so these bare calls resolve to it
   expect_true(.rs.isPackageManagementCall("install.packages(\"dplyr\")"))
   expect_true(.rs.isPackageManagementCall("update.packages()"))
   expect_true(.rs.isPackageManagementCall("remove.packages(\"dplyr\")"))
})

test_that(".rs.isPackageManagementCall ignores base R remove()/update()", {
   # base::remove removes objects and stats::update refits models; neither
   # mutates the package library, even though renv exports same-named functions
   expect_false(.rs.isPackageManagementCall("remove(x)"))
   expect_false(.rs.isPackageManagementCall("remove(x, y, z)"))
   expect_false(.rs.isPackageManagementCall("update(model)"))
   expect_false(.rs.isPackageManagementCall("update(fit, . ~ . + x)"))
   expect_false(.rs.isPackageManagementCall("base::remove(x)"))
   expect_false(.rs.isPackageManagementCall("stats::update(model)"))
})

test_that(".rs.isPackageManagementCall ignores non-mutating calls", {
   expect_false(.rs.isPackageManagementCall("str_remove_all(x)"))
   expect_false(.rs.isPackageManagementCall("stringr::str_remove_all(x)"))
   expect_false(.rs.isPackageManagementCall("installed.packages()"))
   expect_false(.rs.isPackageManagementCall("old.packages()"))
   expect_false(.rs.isPackageManagementCall("library(dplyr)"))
   expect_false(.rs.isPackageManagementCall("updates <- c(1, 2)"))
   # functions that are not in the registry for their namespace
   expect_false(.rs.isPackageManagementCall("pak::pkg_update(\"dplyr\")"))
   expect_false(.rs.isPackageManagementCall("renv::snapshot()"))
})

test_that(".rs.isPackageManagementCall ignores comments and strings", {
   expect_false(.rs.isPackageManagementCall("# remove this later"))
   expect_false(.rs.isPackageManagementCall("(t0 <- Sys.time()) # remove"))
   expect_false(.rs.isPackageManagementCall("x <- \"remove\""))
   expect_false(.rs.isPackageManagementCall("y <- \"renv::update()\""))
})

test_that(".rs.isPackageManagementCall handles multi-statement and nested calls", {
   expect_true(.rs.isPackageManagementCall("x <- 1; renv::update(); y <- 2"))
   expect_true(.rs.isPackageManagementCall("lapply(pkgs, function(p) install.packages(p))"))
   expect_false(.rs.isPackageManagementCall("x <- 1; print(remove(y)); z <- 3"))
})

test_that(".rs.isPackageManagementCall tolerates empty arguments", {
   expect_false(.rs.isPackageManagementCall("View(mtcars[1, ])"))
   expect_false(.rs.isPackageManagementCall("mtcars[, 1]"))
   expect_true(.rs.isPackageManagementCall("f(x[1, ], renv::update())"))
})

test_that(".rs.isPackageManagementCall tolerates unparseable input", {
   expect_false(.rs.isPackageManagementCall(""))
   expect_false(.rs.isPackageManagementCall("install.packages("))
   expect_false(.rs.isPackageManagementCall("if (x"))
})

test_that(".rs.isPackageManagementCall does not force active bindings", {
   # resolving a bare call must not (re-)invoke an active binding, which could
   # run arbitrary user code as a side effect
   invoked <- 0L
   makeActiveBinding(
      "remove",
      function() { invoked <<- invoked + 1L; function(...) NULL },
      globalenv()
   )
   on.exit(rm("remove", envir = globalenv()), add = TRUE)

   expect_false(.rs.isPackageManagementCall("remove(x)"))
   expect_identical(invoked, 0L)
})

test_that(".rs.isPackageManagementCall does not force promises", {
   # a delayedAssign promise shadowing utils::install.packages must not be forced
   forced <- 0L
   delayedAssign(
      "install.packages",
      { forced <<- forced + 1L; function(...) NULL },
      assign.env = globalenv()
   )
   on.exit(rm("install.packages", envir = globalenv()), add = TRUE)

   expect_false(.rs.isPackageManagementCall("install.packages(x)"))
   expect_identical(forced, 0L)
})
