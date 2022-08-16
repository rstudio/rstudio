#
# test-dependencies.R
#
# Copyright (C) 2022 by Posit, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
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


