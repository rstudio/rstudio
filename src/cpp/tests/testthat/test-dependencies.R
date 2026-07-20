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

# Fabricates an installed package directory named 'name' in the library at
# 'lib', with the given DESCRIPTION fields (e.g. Imports, LinkingTo, Priority).
# Used to exercise the runtime dependency walker against controlled fixtures.
fabricateInstalledPackage <- function(lib, name, ..., meta = TRUE) {
   pkgDir <- file.path(lib, name)
   dir.create(pkgDir, recursive = TRUE)

   description <- c(Package = name, Version = "1.0", c(...))
   write.dcf(t(as.matrix(description)), file.path(pkgDir, "DESCRIPTION"))

   if (meta) {
      dir.create(file.path(pkgDir, "Meta"))
      saveRDS(list(DESCRIPTION = description), file.path(pkgDir, "Meta", "package.rds"))
   }

   invisible(pkgDir)
}

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

test_that("malformed declared versions are treated as no version requirement", {
   deps <- .rs.parsePackageDependencyFields("foo (>= 1.0-), bar (>= 1..0), baz (>= 1)")

   expect_equal(deps$name, c("foo", "bar", "baz"))
   expect_equal(deps$op, c("", "", ""))
   expect_equal(deps$version, c("", "", ""))
})

test_that("packages with intact runtime dependency trees are satisfied", {
   # base packages are treated as having no runtime dependencies
   expect_equal(.rs.findUnsatisfiedRuntimeDependencies("stats"), list())

   # a controlled fixture: the declared requirement is satisfied exactly
   lib <- tempfile("library-")
   fabricateInstalledPackage(lib, "childpkg")
   fabricateInstalledPackage(lib, "parentpkg", Imports = "childpkg (>= 1.0)")

   libPaths <- .libPaths()
   on.exit(.libPaths(libPaths), add = TRUE)
   .libPaths(c(lib, libPaths))

   expect_equal(.rs.findUnsatisfiedRuntimeDependencies("parentpkg"), list())
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
   fabricateInstalledPackage(lib, "fakepkg",
      Imports = "missingpkg (>= 2.0), utils (>= 999.999)")

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

test_that("the strongest version requirement wins across multiple parents", {
   lib <- tempfile("library-")
   fabricateInstalledPackage(lib, "parenta", Imports = "missingpkg (>= 1.0)")
   fabricateInstalledPackage(lib, "parentb", Imports = "missingpkg (>= 3.0)")

   libPaths <- .libPaths()
   on.exit(.libPaths(libPaths), add = TRUE)
   .libPaths(c(lib, libPaths))

   # the strongest requirement is kept regardless of visit order
   unsatisfied <- .rs.findUnsatisfiedRuntimeDependencies(c("parenta", "parentb"))
   expect_equal(unsatisfied, list(list(name = "missingpkg", version = "3.0")))

   unsatisfied <- .rs.findUnsatisfiedRuntimeDependencies(c("parentb", "parenta"))
   expect_equal(unsatisfied, list(list(name = "missingpkg", version = "3.0")))
})

test_that("strict version requirements are honored at the boundary", {
   utilsVersion <- as.character(packageVersion("utils"))

   lib <- tempfile("library-")
   fabricateInstalledPackage(lib, "minpkg",
      Imports = sprintf("utils (>= %s)", utilsVersion))
   fabricateInstalledPackage(lib, "strictpkg",
      Imports = sprintf("utils (> %s)", utilsVersion))

   libPaths <- .libPaths()
   on.exit(.libPaths(libPaths), add = TRUE)
   .libPaths(c(lib, libPaths))

   # '>=' is satisfied by an equal version; '>' is not
   expect_equal(.rs.findUnsatisfiedRuntimeDependencies("minpkg"), list())
   expect_equal(.rs.findUnsatisfiedRuntimeDependencies("strictpkg"),
                list(list(name = "utils", version = utilsVersion)))
})

test_that("LinkingTo dependencies are not treated as runtime dependencies", {
   lib <- tempfile("library-")
   fabricateInstalledPackage(lib, "linkpkg", LinkingTo = "missingheaderpkg")

   libPaths <- .libPaths()
   on.exit(.libPaths(libPaths), add = TRUE)
   .libPaths(c(lib, libPaths))

   expect_equal(.rs.findUnsatisfiedRuntimeDependencies("linkpkg"), list())
})

test_that("the DESCRIPTION file is consulted when parsed metadata is missing", {
   lib <- tempfile("library-")
   fabricateInstalledPackage(lib, "nometapkg",
      Imports = "missingpkg (>= 2.0)", meta = FALSE)

   libPaths <- .libPaths()
   on.exit(.libPaths(libPaths), add = TRUE)
   .libPaths(c(lib, libPaths))

   unsatisfied <- .rs.findUnsatisfiedRuntimeDependencies("nometapkg")
   expect_equal(unsatisfied, list(list(name = "missingpkg", version = "2.0")))
})

test_that("packages with base priority are treated as having no dependencies", {
   lib <- tempfile("library-")
   fabricateInstalledPackage(lib, "fakebasepkg",
      Priority = "base", Imports = "missingpkg")

   libPaths <- .libPaths()
   on.exit(.libPaths(libPaths), add = TRUE)
   .libPaths(c(lib, libPaths))

   expect_equal(.rs.findUnsatisfiedRuntimeDependencies("fakebasepkg"), list())
})

test_that("malformed version requirements do not break the walker", {
   lib <- tempfile("library-")
   fabricateInstalledPackage(lib, "badverpkg",
      Imports = "missingpkg (>= 1.0-), utils (>= 1..0)")

   libPaths <- .libPaths()
   on.exit(.libPaths(libPaths), add = TRUE)
   .libPaths(c(lib, libPaths))

   # the missing package is still reported, minus its unparseable version;
   # the malformed requirement on the installed package is ignored
   unsatisfied <- .rs.findUnsatisfiedRuntimeDependencies("badverpkg")
   expect_equal(unsatisfied, list(list(name = "missingpkg", version = "")))
})

test_that("unreadable package metadata is treated as no dependencies", {
   deps <- .rs.installedPackageDependencies(tempfile("nonexistent-"))
   expect_equal(nrow(deps), 0L)
})

test_that("packageCRANVersionAvailable checks the available version", {
   # fabricate a package repository advertising a single package
   repo <- tempfile("repo-")
   contrib <- file.path(repo, "src", "contrib")
   dir.create(contrib, recursive = TRUE)
   writeLines(
      c("Package: fakecranpkg", "Version: 1.5", ""),
      file.path(contrib, "PACKAGES"))

   oldRepos <- getOption("repos")
   on.exit(options(repos = oldRepos), add = TRUE)
   repoUrl <- paste0("file:///", sub("^/+", "", normalizePath(repo, winslash = "/")))
   options(repos = c(CRAN = repoUrl))

   # no version requirement: satisfiable
   result <- .rs.packageCRANVersionAvailable("fakecranpkg", "", source = TRUE)
   expect_equal(result$version, "1.5")
   expect_true(result$satisfied)

   # requirement met by the available version
   result <- .rs.packageCRANVersionAvailable("fakecranpkg", "1.0", source = TRUE)
   expect_true(result$satisfied)

   # regression check: the required version was previously shadowed by the
   # available version, so unsatisfiable requirements were reported satisfied
   result <- .rs.packageCRANVersionAvailable("fakecranpkg", "2.0", source = TRUE)
   expect_equal(result$version, "1.5")
   expect_false(result$satisfied)

   # packages missing from the repository fall back to reporting whether the
   # package is installed
   result <- .rs.packageCRANVersionAvailable("utils", "1.0", source = TRUE)
   expect_equal(result$version, "")
   expect_true(result$satisfied)

   result <- .rs.packageCRANVersionAvailable("nosuchpackage54321", "1.0", source = TRUE)
   expect_false(result$satisfied)
})


