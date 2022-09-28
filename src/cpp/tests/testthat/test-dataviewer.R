#
# test-dataviewer.R
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

context("data viewer")

test_that(".rs.formatDataColumnDispatch() iterates over the classes", {
    registerS3method("[", "foo",  function(x, ...) {
        structure(NextMethod(), class = class(x))
    })
    registerS3method("format", "foo", function(x, ...) {
        rep("Hi there!", length(x))
    })
    
    x <- structure("x", class = c("foo", "bar", "character"))
    expect_equal(.rs.formatDataColumnDispatch(x), "Hi there!")
    expect_equal(.rs.formatDataColumn(x, 1, 1), "Hi there!")

    x <- structure("x", class = c("bar", "foo", "character"))
    expect_equal(.rs.formatDataColumnDispatch(x), "Hi there!")
    expect_equal(.rs.formatDataColumn(x, 1, 1), "Hi there!")
})

test_that(".rs.formatDataColumn() truncates lists", {
    col <- list(
        paste0(rep("a", 100), collapse = ""), 
        "small"
    )
    x <- .rs.formatDataColumn(col, 1, 2)
    expect_equal(
        grepl("\\[\\.{3}\\]$", x), 
        c(TRUE, FALSE)
    )
})

test_that(".rs.flattenFrame() handles matrices and data frames", {
   tbl1 <- data.frame(x = 1:2)
   df_col <- data.frame(y = 1:2, z = 1:2)
   tbl1$df_col <- df_col
   tbl1$mat_col <- matrix(1:4, ncol = 2)
   flat1 <- .rs.flattenFrame(tbl1)

   # same but matrix has colnames
   tbl2 <- tbl1
   tbl2$mat_col <- matrix(1:4, ncol = 2, dimnames = list(1:2, c("a", "b")))
   flat2 <- .rs.flattenFrame(tbl2)
   
   # further df nesting 
   tbl3 <- tbl2
   df_col$df <- df_col
   tbl3$df_col <- df_col
   flat3 <- .rs.flattenFrame(tbl3)

   expect_equal(names(flat1), c("x", "df_col$y", "df_col$z", "mat_col[,1]", "mat_col[,2]"))
   expect_equal(names(flat2), c("x", "df_col$y", "df_col$z", 'mat_col[,"a"]', 'mat_col[,"b"]'))
   expect_equal(names(flat3), c("x", "df_col$y", "df_col$z", "df_col$df$y", "df_col$df$z", 'mat_col[,"a"]', 'mat_col[,"b"]'))
})
