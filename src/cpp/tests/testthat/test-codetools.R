#
# test-codetools.R
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

library(testthat)

context("codetools")

test_that(".rs.detectFreeVars() works as expected", {
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         paste(apple + banana)
      }),
      
      c("apple", "banana")
      
   )
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         base::paste(apple + banana)
      }),
      
      c("apple", "banana")
      
   )
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         userDefinedFunction(apple, banana)
      }),
      
      c("userDefinedFunction", "apple", "banana")
      
   )
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         for (i in 1:10) {
            print(i + j)
         }
      }),
      
      c("j")
      
   )
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         udf <- function(apple, banana) {
            apple + banana + cherry + danish
         }
      }),
      
      c("cherry", "danish")
      
   )
   
   
})

test_that(".rs.CRANDownloadOptionsString() generates a valid R expression", {
   
   # restore options when done
   op <- options()
   on.exit(options(op), add = TRUE)
   
   # set up options used by download string
   options(
      repos = c(CRAN = "https://cran.rstudio.com"),
      download.file.method = "libcurl",
      download.file.extra = NULL,
      HTTPUserAgent = "dummy"
   )

   # create a dummy environment that makes it easier for us
   # to 'capture' the result of an options call
   envir <- new.env(parent = globalenv())
   envir[["options"]] <- base::list
   
   # check that we construct the right kind of R object after parse
   #
   # NOTE: we don't depend on the exact representation of the string as the
   # code returned by R's deparser might differ from version to version,
   # but should still produce the same result after evaluation
   string <- .rs.CRANDownloadOptionsString()
   actual <- eval(parse(text = string), envir = envir)
   for (item in c("repos", "download.file.method", "HTTPUserAgent"))
      expect_equal(actual[[item]], getOption(item))
   
   # https://github.com/rstudio/rstudio/issues/6597
   options(download.file.extra = "embedded 'quotes'")
   string <- .rs.CRANDownloadOptionsString()
   actual <- eval(parse(text = string), envir = envir)
   expect_equal(actual$download.file.extra, "embedded 'quotes'")
   
   # NOTE: double-quotes are translated to single-quotes here as
   # a workaround for issues with quotation of arguments when running
   # commands on Windows
   options(download.file.extra = "embedded \"quotes\"")
   string <- .rs.CRANDownloadOptionsString()
   actual <- eval(parse(text = string), envir = envir)
   expect_equal(actual$download.file.extra, "embedded 'quotes'")
   
})

test_that(".rs.CRANDownloadOptionsString() fills missing CRAN repo", {
   # read default CRAN URL from user preferences
   cran <- .rs.readUiPref("cran_mirror")$url

   # unset repos
   options(repos = NULL)

   # create dummy environment
   envir <- new.env(parent = globalenv())
   envir[["options"]] <- base::list

   # evaluate the download string and confirm that it contains the CRAN URL from user prefs
   string <- .rs.CRANDownloadOptionsString()
   actual <- eval(parse(text = string), envir = envir)
   expect_equal(unlist(actual[["repos"]]["CRAN"]), c(CRAN = cran))
})


test_that("HTML escaping escapes HTML entities", {
   fake_header <- "<h1>Not a real header.</h1>"

   escaped <- .rs.htmlEscape(fake_header)
   expect_false(grepl(escaped, "<"))
   expect_false(grepl(escaped, ">"))
})

test_that(".rs.recoverPackageSourcePath() recovers sources from the srcref database", {

   # create a small package with a couple of source files
   sourceDir <- tempfile("srcrefpkg-src-")
   pkgDir <- file.path(sourceDir, "srcrefpkg")
   dir.create(file.path(pkgDir, "R"), recursive = TRUE)

   writeLines(
      c("Package: srcrefpkg",
        "Version: 0.1.0",
        "Title: Test Package for Srcref Recovery",
        "Description: Test package.",
        "Author: RStudio Tests",
        "Maintainer: RStudio Tests <tests@example.com>",
        "License: MIT"),
      file.path(pkgDir, "DESCRIPTION")
   )

   appleLines <- c(
      "# a comment preserved by keep.source",
      "apple <- function() {",
      "   \"apple\"",
      "}"
   )

   bananaLines <- c(
      "banana <- function() {",
      "   \"banana\"",
      "}"
   )

   writeLines(appleLines, file.path(pkgDir, "R", "apple.R"))
   writeLines(bananaLines, file.path(pkgDir, "R", "banana.R"))
   writeLines("exportPattern(\"^[a-z]\")", file.path(pkgDir, "NAMESPACE"))

   # install it with srcrefs preserved
   lib <- tempfile("srcrefpkg-lib-")
   dir.create(lib)

   R <- file.path(R.home("bin"), if (.rs.platform.isWindows) "R.exe" else "R")
   status <- suppressWarnings(system2(
      R,
      c("--vanilla", "CMD", "INSTALL", "--with-keep.source",
        "-l", shQuote(lib), shQuote(pkgDir)),
      stdout = FALSE,
      stderr = FALSE
   ))
   expect_equal(status, 0L)

   loadNamespace("srcrefpkg", lib.loc = lib)

   on.exit({
      unloadNamespace("srcrefpkg")
      unlink(c(sourceDir, lib), recursive = TRUE)
   }, add = TRUE)

   # remove the package sources, so that the srcref paths become stale,
   # as happens for the temporary staging directory used when installing
   # a package from a tarball
   unlink(sourceDir, recursive = TRUE)

   ns <- asNamespace("srcrefpkg")
   applePath <- attr(attr(ns$apple, "srcref"), "srcfile")$filename
   bananaPath <- attr(attr(ns$banana, "srcref"), "srcfile")$filename
   expect_false(file.exists(applePath))
   expect_false(file.exists(bananaPath))

   # both files should be recoverable, with contents intact
   recoveredApple <- .rs.recoverPackageSourcePath(applePath)
   expect_true(nzchar(recoveredApple))
   expect_equal(readLines(recoveredApple), appleLines)

   recoveredBanana <- .rs.recoverPackageSourcePath(bananaPath)
   expect_true(nzchar(recoveredBanana))
   expect_equal(readLines(recoveredBanana), bananaLines)

   # paths which don't resolve to a known srcref are not recovered
   expect_equal(.rs.recoverPackageSourcePath(""), "")
   expect_equal(.rs.recoverPackageSourcePath("/no/such/pkg/R/file.R"), "")
   expect_equal(
      .rs.recoverPackageSourcePath(file.path(dirname(applePath), "missing.R")),
      ""
   )

})

test_that("heredoc trims trailing newlines + whitespace", {
   
   actual <- .rs.heredoc('
      c(
         person(
            "Jane", "Doe",
            email = "jane@example.com",
            role = c("aut", "cre")
         )
      )
   ')
   
   expected <- 'c(
   person(
      "Jane", "Doe",
      email = "jane@example.com",
      role = c("aut", "cre")
   )
)'
   
   expect_equal(actual, expected)
   
})
