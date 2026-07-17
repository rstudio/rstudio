#
# test-dependencies.R
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

context("dependencies")

test_that("simple topological sort works", {
   # unsorted nodes
   nodes <- c("b", "a", "c")

   # edges a -> b -> c
   edges <- list(
      list(from = "a", to = "b"),
      list(from = "b", to = "c"))
   
   # in this configuration we'd expect to install package c, then package b, then package a
   expect_equal(.rs.topoSortPackages(nodes, edges), 
                c("c", "b", "a"))
})

test_that("simple expansion and sorting is done correctly", {
   # simulation of available.packages for a simple set of packages, foo -> bar -> baz
   available <- rbind(
         # Pkg    # Ver  # Depends                  # Imports   # LinkingTo
         c("foo", "1.0", "R (>= 3.2), bar (>= 1.1)", NA,        NA),
         c("bar", "1.1", NA,                         "baz",     NA),
         c("baz", "2.0", NA,                         NA,        NA))
   colnames(available) <- c("Package", "Version", "Depends", "Imports", "LinkingTo") 
   rownames(available) <- available[,1]

   # simulation of the dependencies we want to install; just one package
   dependencies <- list(list(
         name     = "foo",
         location = "cran",
         version  = "1.0",
         source   = FALSE))

   result <- .rs.expandDependencies(available, data.frame(), dependencies)

   expect_equal(!!result, list(
         list(name     = "bar",
              location = "cran",
              version  = "1.1",
              source   = FALSE),
         list(name     = "foo",
              location = "cran",
              version  = "1.0",
              source   = FALSE)))
})


test_that("dependencies already installed are not installed again", {

   # simulation of available.packages for a simple set of packages, foo -> bar -> baz
   available <- rbind(
         # Pkg    # Ver  # Depends                  # Imports   # LinkingTo
         c("foo", "1.0", "R (>= 3.2), bar (>= 1.1)", NA,        NA),
         c("bar", "1.1", NA,                         "baz",     NA),
         c("baz", "2.0", NA,                         NA,        NA))
   colnames(available) <- c("Package", "Version", "Depends", "Imports", "LinkingTo") 
   rownames(available) <- available[,1]

   # simulation of installed.packages
   installed <- data.frame(
         Package   = c("baz"),
         Version   = c("2.0"),
         Depends   = c("NA"),
         Imports   = c("NA"),
         LinkingTo = c("NA"),
         stringsAsFactors = FALSE)
   rownames(installed) <- installed[[1]]

   # simulation of the dependencies we want to install; just one package
   dependencies <- list(list(
         name     = "bar",
         location = "cran",
         version  = "1.1",
         source   = FALSE))

   # if the baz package is not installed, we should install it
   result <- .rs.expandDependencies(available, data.frame(), dependencies)

   expect_equal(!!result, list(
         list(name     = "baz",
              location = "cran",
              version  = "2.0",
              source   = FALSE),
         list(name     = "bar",
              location = "cran",
              version  = "1.1",
              source   = FALSE)))

   # if it is installed already, we should not install it
   result <- .rs.expandDependencies(available, installed, dependencies)

   expect_equal(!!result, list(
         list(name     = "bar",
              location = "cran",
              version  = "1.1",
              source   = FALSE)))
})

test_that("dependencies are replaced when too old", {
   # simulation of available.packages for a simple set of packages, foo -> bar -> baz
   available <- rbind(
         # Pkg    # Ver  # Depends                  # Imports   # LinkingTo
         c("foo", "1.0", "R (>= 3.2), bar (>= 1.1)", NA,        NA),
         c("bar", "1.1", NA,                         "baz",     NA),
         c("baz", "2.0", NA,                         NA,        NA))
   colnames(available) <- c("Package", "Version", "Depends", "Imports", "LinkingTo") 
   rownames(available) <- available[,1]

   # simulation of installed.packages; bar is installed but is an older version
   installed <- rbind(
         # Pkg    # Ver  # Depends                  # Imports   # LinkingTo
         c("bar", "1.0", NA,                         "baz",     NA))
   colnames(installed) <- c("Package", "Version", "Depends", "Imports", "LinkingTo") 
   rownames(installed) <- installed[,1]

   # simulation of the dependencies we want to install; just one package
   dependencies <- list(list(
         name     = "foo",
         location = "cran",
         version  = "1.0",
         source   = FALSE))

   result <- .rs.expandDependencies(available, installed, dependencies)

   # even though the 'bar' package is installed, it needs to be reinstalled since we need 1.1 and
   # have 1.0 installed
   expect_equal(!!result, list(
         list(name     = "bar",
              location = "cran",
              version  = "1.1",
              source   = FALSE),
         list(name     = "foo",
              location = "cran",
              version  = "1.0",
              source   = FALSE)))
})


test_that("dependencies are installed only once", {
   # simulation of available.packages; toast for breakfast and lunch
   available <- rbind(
         # Pkg          # Ver  # Depends                    # Imports   # LinkingTo
         c("breakfast", "1.0", "toast",                     NA,         NA),
         c("lunch",     "1.1", "breakfast, toast (>= 1.0)", NA,         NA),
         c("toast",     "2.0", NA,                          NA,         NA))
   colnames(available) <- c("Package", "Version", "Depends", "Imports", "LinkingTo") 
   rownames(available) <- available[,1]

   # simulation of the dependencies we want to install -- breakfast and lunch
   dependencies <- list(list(
         name     = "breakfast",
         location = "cran",
         version  = "1.0",
         source   = FALSE),
      list(
         name     = "lunch",
         location = "cran",
         version  = "1.1",
         source   = FALSE))

   result <- .rs.expandDependencies(available, data.frame(), dependencies)

   # this will require us to install breakfast, lunch, and toast -- but only once each!
   expect_equal(!!result, list(
         list(name     = "toast",
              location = "cran",
              version  = "2.0",
              source   = FALSE),
         list(name     = "breakfast",
              location = "cran",
              version  = "1.0",
              source   = FALSE),
         list(name     = "lunch",
              location = "cran",
              version  = "1.1",
              source   = FALSE)))
})

test_that("dependency fields are parsed correctly", {
   contents <- "R (>= 3.5), foo (>= 1.0), bar,\n  baz.qux (> 2.0-1)"
   deps <- .rs.parsePackageDependencyFields(contents)

   expect_equal(deps$name, c("foo", "bar", "baz.qux"))
   expect_equal(deps$op, c(">=", "", ">"))
   expect_equal(deps$version, c("1.0", "", "2.0-1"))

   # multiple fields are combined; NA fields are ignored
   deps <- .rs.parsePackageDependencyFields(c(Depends = "foo", Imports = NA))
   expect_equal(deps$name, "foo")

   deps <- .rs.parsePackageDependencyFields(character())
   expect_equal(nrow(deps), 0L)
})

test_that("packages with intact runtime dependency trees are satisfied", {
   expect_equal(.rs.findUnsatisfiedRuntimeDependencies("stats"), list())
   expect_equal(.rs.findUnsatisfiedRuntimeDependencies("testthat"), list())
})

test_that("packages which are not installed are reported as unsatisfied", {
   unsatisfied <- .rs.findUnsatisfiedRuntimeDependencies("nosuchpackage54321")
   expect_equal(unsatisfied, list(list(name = "nosuchpackage54321", version = "")))
})

test_that("missing and outdated runtime dependencies are reported", {
   # fabricate an installed package whose declared dependencies cannot be
   # satisfied: one is not installed, and one is installed (utils) but cannot
   # meet the declared version requirement
   lib <- tempfile("library-")
   pkgDir <- file.path(lib, "fakepkg")
   dir.create(file.path(pkgDir, "Meta"), recursive = TRUE)

   description <- c(
      Package = "fakepkg",
      Version = "1.0",
      Imports = "missingpkg (>= 2.0), utils (>= 999.999)")
   write.dcf(t(as.matrix(description)), file.path(pkgDir, "DESCRIPTION"))
   saveRDS(list(DESCRIPTION = description), file.path(pkgDir, "Meta", "package.rds"))

   libPaths <- .libPaths()
   on.exit(.libPaths(libPaths), add = TRUE)
   .libPaths(c(lib, libPaths))

   unsatisfied <- .rs.findUnsatisfiedRuntimeDependencies("fakepkg")
   names <- vapply(unsatisfied, `[[`, "", "name")
   versions <- vapply(unsatisfied, `[[`, "", "version")

   expect_setequal(names, c("missingpkg", "utils"))
   expect_equal(versions[names == "missingpkg"], "2.0")
   expect_equal(versions[names == "utils"], "999.999")
})


