#
# test-install-packages.R
#
# Copyright (C) 2025 by Posit Software, PBC
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

# --- helpers -------------------------------------------------------------------

# Build one or more source packages into a local file:// repository, so the
# tests don't depend on network access or on the active set of repositories.
# 'pkgSpecs' is a named list whose names are package names and whose values
# are named character vectors of extra DESCRIPTION fields (e.g.
# c(Imports = "otherPkg")). The default builds a single package with no
# extras for single-package tests.
.rs.tests.makeLocalSourceRepo <- function(pkgSpecs = list(rstudioTestPkg = character()))
{
   base <- tempfile("rstudio-test-repo-")
   dir.create(base)
   src     <- file.path(base, "src")
   contrib <- file.path(base, "repo", "src", "contrib")
   dir.create(src)
   dir.create(contrib, recursive = TRUE)

   pkgNames <- names(pkgSpecs)

   for (pkgName in pkgNames)
   {
      pkgDir <- file.path(src, pkgName)
      dir.create(pkgDir)
      lines <- c(
         paste0("Package: ", pkgName),
         "Version: 0.0.1",
         "Title: Demo",
         "Description: Test fixture for RStudio install-hook tests.",
         "License: GPL-3",
         "Encoding: UTF-8"
      )
      extras <- pkgSpecs[[pkgName]]
      if (length(extras))
         lines <- c(lines, paste0(names(extras), ": ", extras))
      writeLines(lines, file.path(pkgDir, "DESCRIPTION"))
      writeLines(paste0("# generated namespace for ", pkgName),
                 file.path(pkgDir, "NAMESPACE"))
   }

   # Use R's internal tar rather than 'R CMD build', so no external tool
   # (or R on the PATH) is needed; a plain archive of the package directory
   # is a valid source package.
   oldwd <- setwd(src)
   on.exit(setwd(oldwd), add = TRUE)
   tarballs <- character()
   for (pkgName in pkgNames)
   {
      tarball <- file.path(contrib, paste0(pkgName, "_0.0.1.tar.gz"))
      utils::tar(tarball, pkgName, compression = "gzip", tar = "internal")
      tarballs <- c(tarballs, tarball)
   }
   tools::write_PACKAGES(contrib, type = "source")

   # on Windows a file URL needs a third slash before the drive (file:///C:/...)
   repoDir <- file.path(base, "repo")
   reposUrl <- if (.Platform$OS.type == "windows")
      paste0("file:///", normalizePath(repoDir, winslash = "/"))
   else
      paste0("file://", repoDir)

   list(
      base     = base,
      pkgName  = pkgNames[[1L]],   # first package, for single-package tests
      pkgNames = pkgNames,
      tarballs = tarballs,
      reposUrl = reposUrl
   )
}

# Build a binary package from a source tarball, returning its path.
.rs.tests.buildBinaryPackage <- function(tarball)
{
   outdir <- tempfile("rstudio-test-binary-")
   dir.create(outdir)
   lib <- file.path(outdir, "lib")
   dir.create(lib)

   # 'R CMD INSTALL --build' writes the binary into the working directory
   oldwd <- setwd(outdir)
   on.exit(setwd(oldwd), add = TRUE)
   status <- system2(
      file.path(R.home("bin"), "R"),
      c("CMD", "INSTALL", "--build", "--library", shQuote(lib), shQuote(tarball)),
      stdout = FALSE,
      stderr = FALSE
   )
   if (!identical(status, 0L))
      stop("'R CMD INSTALL --build' failed with status ", status)

   binaries <- list.files(outdir, pattern = "\\.(tgz|zip|tar\\.gz)$", full.names = TRUE)
   if (length(binaries) != 1L)
      stop("expected exactly one binary package in '", outdir, "'")

   binaries
}

# Serve a file over http:// from R's own help server, so URL installs can be
# exercised without network access. The session normally points R's httpd
# port at its own web server, which doesn't dispatch custom handlers, so a
# dedicated server is started and the original port restored when 'frame'
# exits.
.rs.tests.serveFile <- function(file, frame = parent.frame())
{
   oldPort <- tools:::httpdPort()
   tools:::httpdPort(0L)
   port <- suppressMessages(tools::startDynamicHelp(TRUE))
   withr::defer({
      tools::startDynamicHelp(FALSE)
      tools:::httpdPort(oldPort)
   }, envir = frame)

   handlers <- get(".httpd.handlers.env", asNamespace("tools"))
   handler <- function(path, query, ...)
      list(file = file, "content-type" = "application/octet-stream", NULL, 200L)
   assign("rstudioTests", handler, envir = handlers)
   withr::defer(rm("rstudioTests", envir = handlers), envir = frame)

   sprintf("http://127.0.0.1:%d/custom/rstudioTests/%s", port, basename(file))
}

expect_install <- function(pkgs, ...) {
   
   # create temporary library for installation
   lib <- tempfile("library-")
   dir.create(lib, recursive = TRUE)
   on.exit(unlink(lib, recursive = TRUE), add = TRUE)
   
   # install the package
   install.packages(pkgs, lib = lib, quiet = TRUE, ...)
   
   # check that the package was installed
   installedPkgs <- list.files(lib, full.names = TRUE)
   expect_length(installedPkgs, 1L)
   
   # check that we can read the package DESCRIPTION file
   pkgInfo <- .rs.readPackageDescription(installedPkgs)
   expect_true(is.list(pkgInfo))
   
}

test_that("packages can be installed", {

   r <- .rs.tests.makeLocalSourceRepo()
   on.exit(unlink(r$base, recursive = TRUE), add = TRUE)

   # from a repository
   expect_install(r$pkgName, repos = r$reposUrl, type = "source")

   # from a local source package
   expect_install(r$tarballs[[1L]], repos = NULL, type = "source")

   # from a local binary package
   if (!identical(.Platform$pkgType, "source"))
   {
      binary <- .rs.tests.buildBinaryPackage(r$tarballs[[1L]])
      on.exit(unlink(dirname(binary), recursive = TRUE), add = TRUE)
      expect_install(binary, repos = NULL, type = "binary")
   }

})

test_that(".rs.readPackageDescription fails cleanly for URL inputs", {

   # Previously a URL was handed straight to the archive reader, which
   # shelled out to 'tar -tf <URL>' and sprayed tar errors and warnings
   # into the console. https://github.com/rstudio/rstudio/issues/18198
   url <- "https://cloud.r-project.org/src/contrib/RECA_1.7.1.tar.gz"
   expect_no_warning(expect_error(.rs.readPackageDescription(url)))

})

test_that("install hook stays quiet for URL installs with repos = NULL", {

   # https://github.com/rstudio/rstudio/issues/18198
   r <- .rs.tests.makeLocalSourceRepo()
   on.exit(unlink(r$base, recursive = TRUE), add = TRUE)

   lib <- tempfile("library-")
   dir.create(lib)
   on.exit(unlink(lib, recursive = TRUE), add = TRUE)

   url <- .rs.tests.serveFile(r$tarballs[[1L]])
   expect_no_warning(
      install.packages(url, lib = lib, repos = NULL, type = "source", quiet = TRUE)
   )

   expect_true(file.exists(file.path(lib, r$pkgName, "DESCRIPTION")))

})

test_that(".rs.installPackagesWhichDeps maps the 'dependencies' argument correctly", {

   hard <- c("Depends", "Imports", "LinkingTo")
   all  <- c("Depends", "Imports", "LinkingTo", "Suggests")

   # NA (install.packages's default) and unrecognized values fall back to hard deps.
   expect_identical(.rs.installPackagesWhichDeps(NA),
                    list(direct = hard, transitive = hard))
   expect_identical(.rs.installPackagesWhichDeps(NULL),
                    list(direct = hard, transitive = hard))

   # TRUE includes Suggests directly but only recurses through strong deps,
   # mirroring install.packages's getDependencies() behavior.
   expect_identical(.rs.installPackagesWhichDeps(TRUE),
                    list(direct = all, transitive = hard))
   expect_identical(.rs.installPackagesWhichDeps(FALSE),
                    list(direct = character(), transitive = character()))

   # Explicit character vector is passed through verbatim for both direct and
   # transitive (install.packages applies the user's choice on every level).
   expect_identical(
      .rs.installPackagesWhichDeps(c("Depends", "Suggests")),
      list(direct = c("Depends", "Suggests"),
           transitive = c("Depends", "Suggests"))
   )
   expect_identical(.rs.installPackagesWhichDeps(character()),
                    list(direct = character(), transitive = character()))

   # NA values inside a character vector are dropped so they don't propagate
   # into tools::package_dependencies(which = ...).
   expect_identical(
      .rs.installPackagesWhichDeps(c("Depends", NA_character_)),
      list(direct = "Depends", transitive = "Depends")
   )

})

test_that(".rs.installedPackagesFileInfo(paths=) returns NA mtime for missing paths", {

   lib <- tempfile("library-")
   dir.create(lib)
   on.exit(unlink(lib, recursive = TRUE), add = TRUE)

   present <- file.path(lib, "pkgPresent")
   dir.create(present)
   absent  <- file.path(lib, "pkgMissing")

   info <- .rs.installedPackagesFileInfo(paths = c(present, absent))

   expect_equal(nrow(info), 2L)
   expect_setequal(info$path, c(present, absent))

   # Present directory yields a real mtime.
   presentRow <- info[info$path == present, , drop = FALSE]
   expect_false(is.na(presentRow$mtime))

   # Missing directory yields NA mtime, which the install hook relies on
   # to filter out non-existent install targets.
   absentRow <- info[info$path == absent, , drop = FALSE]
   expect_true(is.na(absentRow$mtime))

})

test_that("install hook tags DESCRIPTION correctly across a no-op reinstall", {

   r <- .rs.tests.makeLocalSourceRepo()
   on.exit(unlink(r$base, recursive = TRUE), add = TRUE)

   lib <- tempfile("library-")
   dir.create(lib)
   on.exit(unlink(lib, recursive = TRUE), add = TRUE)

   install.packages(
      r$pkgName,
      lib   = lib,
      repos = r$reposUrl,
      type  = "source",
      quiet = TRUE
   )

   descPath <- file.path(lib, r$pkgName, "DESCRIPTION")
   expect_true(file.exists(descPath))

   firstDesc <- read.dcf(descPath)
   expect_true("RemoteType" %in% colnames(firstDesc))
   expect_true("RemoteRepos" %in% colnames(firstDesc))
   firstRemote <- firstDesc[1, "RemoteType"]

   # Reinstall the same package; install.packages will overwrite the
   # DESCRIPTION, then the hook should re-apply the same tagging.
   install.packages(
      r$pkgName,
      lib   = lib,
      repos = r$reposUrl,
      type  = "source",
      quiet = TRUE
   )

   secondDesc <- read.dcf(descPath)
   expect_true("RemoteType" %in% colnames(secondDesc))
   expect_true("RemoteRepos" %in% colnames(secondDesc))
   expect_identical(secondDesc[1, "RemoteType"], firstRemote)

})

test_that(".rs.recordPackageSource(db=) tags without a network call", {

   r <- .rs.tests.makeLocalSourceRepo()
   on.exit(unlink(r$base, recursive = TRUE), add = TRUE)

   lib <- tempfile("library-")
   dir.create(lib)
   on.exit(unlink(lib, recursive = TRUE), add = TRUE)

   install.packages(
      r$pkgName,
      lib   = lib,
      repos = r$reposUrl,
      type  = "source",
      quiet = TRUE
   )
   pkgPath <- file.path(lib, r$pkgName)

   # Drop any tagging the install hook applied so we can re-tag from a
   # synthesized 'db' below. packageDescription() prefers Meta/package.rds
   # over the DESCRIPTION file, so both must be stripped.
   descPath <- file.path(pkgPath, "DESCRIPTION")
   desc <- read.dcf(descPath)
   desc <- desc[, setdiff(colnames(desc), grep("^Remote", colnames(desc), value = TRUE)),
                drop = FALSE]
   write.dcf(desc, file = descPath)

   metaPath <- file.path(pkgPath, "Meta", "package.rds")
   if (file.exists(metaPath))
   {
      meta <- readRDS(metaPath)
      meta$DESCRIPTION <- meta$DESCRIPTION[
         !grepl("^Remote", names(meta$DESCRIPTION))
      ]
      saveRDS(meta, metaPath)
   }

   # Force any network lookup to fail loudly: an unreachable URL would
   # throw or return empty if recordPackageSource fell back to it.
   withr::local_options(repos = c(CRAN = "http://localhost:1"))

   db <- data.frame(
      Package    = r$pkgName,
      Version    = "0.0.1",
      Repository = paste0(r$reposUrl, "/src/contrib"),
      stringsAsFactors = FALSE
   )

   expect_no_error(.rs.recordPackageSource(pkgPath, db = db))

   tagged <- read.dcf(descPath)
   expect_true("RemoteType" %in% colnames(tagged))
   expect_identical(unname(tagged[1, "RemoteType"]), "standard")
   expect_identical(unname(tagged[1, "RemotePkgRef"]), r$pkgName)

})

test_that(".rs.recordPackageSource is a no-op for empty pkgPaths", {

   # Pass a non-empty 'db' so the function skips its network-fetch
   # branch entirely; the loop body should then be a zero-iteration
   # no-op rather than tripping any code path.
   db <- data.frame(
      Package    = "ignored",
      Version    = "0.0.1",
      Repository = "ignored",
      stringsAsFactors = FALSE
   )
   expect_no_error(.rs.recordPackageSource(character(), db = db))

})

test_that("install hook tags transitive dependency DESCRIPTIONs", {

   r <- .rs.tests.makeLocalSourceRepo(list(
      rstudioTestPkgA = character(),
      rstudioTestPkgB = c(Imports = "rstudioTestPkgA")
   ))
   on.exit(unlink(r$base, recursive = TRUE), add = TRUE)

   lib <- tempfile("library-")
   dir.create(lib)
   on.exit(unlink(lib, recursive = TRUE), add = TRUE)

   install.packages(
      "rstudioTestPkgB",
      lib   = lib,
      repos = r$reposUrl,
      type  = "source",
      quiet = TRUE
   )

   # Both the requested package and its transitive dependency should
   # have their DESCRIPTIONs tagged with the source repo. Before this
   # change, only packages directly named to install.packages got
   # tagged; the dependency closure was scanned but not recorded.
   for (name in c("rstudioTestPkgA", "rstudioTestPkgB"))
   {
      descPath <- file.path(lib, name, "DESCRIPTION")
      expect_true(file.exists(descPath), info = name)
      desc <- read.dcf(descPath)
      expect_true("RemoteType" %in% colnames(desc), info = name)
      expect_true("RemoteRepos" %in% colnames(desc), info = name)
      expect_identical(unname(desc[1, "RemoteType"]), "standard", info = name)
   }

})
