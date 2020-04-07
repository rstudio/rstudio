#
# test-codetools.R
#
# Copyright (C) 2009-20 by RStudio, PBC
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

context("codetools")

test_that(".rs.CRANDownloadOptionsString() generates a valid R expression", {
   
   # restore options when done
   op <- options()
   on.exit(options(op), add = TRUE)
   
   options(
      repos = c(CRAN = "https://cran.rstudio.com"),
      download.file.method = "libcurl",
      download.file.extra = NULL,
      HTTPUserAgent = "dummy"
   )
   
   actual <- .rs.CRANDownloadOptionsString()
   expected <- "options(repos = c(CRAN = 'https://cran.rstudio.com'), download.file.method = 'libcurl', HTTPUserAgent = 'dummy')"
   expect_equal(actual, expected)
   
   # https://github.com/rstudio/rstudio/issues/6597
   options(download.file.extra = "embedded 'quotes'")
   actual <- .rs.CRANDownloadOptionsString()
   expected <- "options(repos = c(CRAN = 'https://cran.rstudio.com'), download.file.method = 'libcurl', download.file.extra = 'embedded \\'quotes\\'', HTTPUserAgent = 'dummy')"
   expect_equal(actual, expected)
   
   options(download.file.extra = "embedded \"quotes\"")
   actual <- .rs.CRANDownloadOptionsString()
   expected <- "options(repos = c(CRAN = 'https://cran.rstudio.com'), download.file.method = 'libcurl', download.file.extra = 'embedded \\'quotes\\'', HTTPUserAgent = 'dummy')"
   expect_equal(actual, expected)
   
})
