#
# test-data-import.R
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

context("data import")

test_that("assembleDataImport escapes column names against R code injection", {
   skip_if_not_installed("readr")

   # A column name containing R syntax that would, without escaping, close the
   # cols() argument and inject a top-level call.
   evil <- 'foo"); .rs.injection.sentinel <- TRUE; readr::cols("'

   opts <- list(
      mode = "text",
      importLocation = "data.csv",
      delimiter = ",",
      columnDefinitions = list(
         list(
            name = evil,
            assignedType = "character",
            only = NULL,
            parseString = NULL,
            index = 0,
            rType = "character"
         )
      ),
      openDataViewer = FALSE
   )

   info <- .rs.assembleDataImport(opts)

   # The assembled previewCode must parse as a single top-level expression.
   # If the column name escapes the cols() argument, parse() yields multiple
   # expressions instead.
   parsed <- parse(text = info$previewCode)
   expect_length(parsed, 1L)

   # The single expression must be a call to readr::read_csv.
   expect_equal(as.character(parsed[[1L]][[1L]]), c("::", "readr", "read_csv"))

   # The column name should round-trip: the col_types argument should contain
   # one named slot whose name equals the original (unescaped) malicious string.
   call <- parsed[[1L]]
   col_types <- call$col_types
   expect_false(is.null(col_types))
   expect_equal(names(eval(col_types)$cols), evil)
})
