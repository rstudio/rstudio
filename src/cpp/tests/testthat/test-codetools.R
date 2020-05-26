#
# test-codetools.R
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

library(testthat)

context("codetools")

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
