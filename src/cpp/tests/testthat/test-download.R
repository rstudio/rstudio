#
# test-download.R
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

context("download")

expect_download <- function(url, destfile = NULL, method = "libcurl") {

   destfile <- .rs.nullCoalesce(destfile, {
       .rs.mapChr(seq_along(url), function(i) tempfile())
   })

   lhs <- .rs.downloadFile(
      url      = url,
      destfile = destfile,
      method   = method,
      quiet    = TRUE
   )

   rhs <- utils::download.file(
      url      = url,
      destfile = destfile,
      method   = method,
      quiet    = TRUE
   )

   expect_equal(lhs, rhs)

}

test_that("download.file hooks work as expected", {

   url <- "https://cran.rstudio.com"

   expect_download(url)
   expect_download(c(url, url))

})

# https://github.com/rstudio/rstudio/issues/16446
test_that("download hooks don't set an empty authorization header", {

   skip_if(!"headers" %in% names(formals(utils::download.file)))   
   
   url <- "https://cran.rstudio.com"
   
   trace(.rs.downloadFile, quote({
      expect_identical(headers, NULL)
      stop("exiting early")
   }), print = FALSE)
   on.exit(untrace(.rs.downloadFile))
   
   tryCatch(
      download.file(url, tempfile()),
      error = function(cnd) NULL
   )
   
})
