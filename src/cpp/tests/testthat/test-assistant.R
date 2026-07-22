#
# test-assistant.R
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

context("assistant")

test_that("describeChildren caps children at maxChildren", {
   value <- as.list(seq_len(60))
   names(value) <- paste0("x", seq_len(60))

   children <- .rs.assistant.describeChildren(value, 50L)

   expect_length(children, 50L)
   expect_identical(unclass(children[[1]]$name), "x1")
   expect_identical(unclass(children[[50]]$name), "x50")
})

test_that("describeChildren does not use S3 dispatch when subsetting (#18317)", {
   # simulate a data.table-style '[' method, which treats indices
   # as row indices and so returns all columns
   registerS3method("[", "rowsFirst", function(x, i) unclass(x))

   value <- structure(as.list(rep(0, 51)), class = "rowsFirst")

   expect_no_warning(children <- .rs.assistant.describeChildren(value, 50L))
   expect_length(children, 50L)
})

test_that("describeChildren handles wide data.tables without warnings (#18317)", {
   skip_if_not_installed("data.table")

   df <- data.frame(matrix(rep(0, 51), nrow = 1))
   data.table::setDT(df)

   expect_no_warning(children <- .rs.assistant.describeChildren(df, 50L))
   expect_length(children, 50L)
   expect_identical(unclass(children[[1]]$name), "X1")
})
