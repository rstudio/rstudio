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

   expect_equal(names(flat1), c("x", "df_col$y", "df_col$z", "mat_col[, 1]", "mat_col[, 2]"))
   expect_equal(names(flat2), c("x", "df_col$y", "df_col$z", 'mat_col[, "a"]', 'mat_col[, "b"]'))
   expect_equal(names(flat3), c("x", "df_col$y", "df_col$z", "df_col$df$y", "df_col$df$z", 'mat_col[, "a"]', 'mat_col[, "b"]'))
})

test_that(".rs.describeColSlice() describes the correct number of column slices", {
   tbl <- data.frame(x = 1:2, y = 1:2, x = 1:2)
   # valid sliceStart and sliceEnd values
   expect_equal(length(.rs.describeColSlice(tbl, 0, 1)), 2)
   expect_equal(length(.rs.describeColSlice(tbl, 1, 1)), 2)
   expect_equal(length(.rs.describeColSlice(tbl, 1, 2)), 3)
   expect_equal(length(.rs.describeColSlice(tbl, 1, 3)), 4)
   # sliceStart == 0
   expect_equal(length(.rs.describeColSlice(tbl, 0, 1)), 2)
   # negative sliceStart
   expect_equal(length(.rs.describeColSlice(tbl, -1, 1)), 2)
   # sliceStart > sliceEnd
   expect_equal(length(.rs.describeColSlice(tbl, 2, 1)), 2)
   # sliceEnd == 0, sliceEnd < sliceStart
   expect_equal(length(.rs.describeColSlice(tbl, 1, 0)), 4)
   # negative sliceEnd
   expect_equal(length(.rs.describeColSlice(tbl, 1, -1)), 4)
   # sliceEnd out of bounds
   expect_equal(length(.rs.describeColSlice(tbl, 1, 4)), 4)
})

test_that(".rs.describeColSlice() returns NULL for empty data frames", {
   tbl <- data.frame()
   expect_equal(.rs.describeColSlice(tbl, 1, 1), NULL)
})

# Helper: strip the rs.scalar class wrapper so tests can compare against
# bare R values without forcing every expect_equal to know the wrapping.
.rs.summarize.bare <- function(x) {
   if (is.null(x)) return(NULL)
   unclass(x)
}

test_that(".rs.summarizeColumn() reports out-of-range column indices", {
   df <- data.frame(x = 1:3, y = letters[1:3], stringsAsFactors = FALSE)
   expect_true(!is.null(.rs.summarizeColumn(df, 0)$error))
   expect_true(!is.null(.rs.summarizeColumn(df, -1)$error))
   expect_true(!is.null(.rs.summarizeColumn(df, 99)$error))
})

test_that(".rs.summarizeColumn() summarizes numeric columns", {
   df <- data.frame(x = c(1, 2, NA, 4, 5))
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(.rs.summarize.bare(r$n), 5L)
   expect_equal(.rs.summarize.bare(r$n_na), 1L)
   expect_equal(.rs.summarize.bare(r$n_unique), 4L)
   expect_equal(.rs.summarize.bare(r$min), 1)
   expect_equal(.rs.summarize.bare(r$max), 5)
   expect_equal(.rs.summarize.bare(r$mean), 3)
   expect_equal(.rs.summarize.bare(r$median), 3)
})

test_that(".rs.summarizeColumn() handles all-NA numeric columns without producing min/max", {
   df <- data.frame(x = NA_real_)
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(.rs.summarize.bare(r$n), 1L)
   expect_equal(.rs.summarize.bare(r$n_na), 1L)
   expect_null(r$min)
   expect_null(r$max)
   expect_null(r$mean)
})

test_that(".rs.summarizeColumn() summarizes character columns", {
   df <- data.frame(
      x = c("apple", "banana", "", NA, "cherry"),
      stringsAsFactors = FALSE
   )
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(.rs.summarize.bare(r$n_unique), 4L)
   expect_equal(.rs.summarize.bare(r$min_length), 0L)
   expect_equal(.rs.summarize.bare(r$max_length), 6L)
   # n_empty counts only "" not NA values
   expect_equal(.rs.summarize.bare(r$n_empty), 1L)
})

test_that(".rs.summarizeColumn() preserves factor level order and aligns counts", {
   # Use a non-alphabetical level ordering to confirm we don't sort.
   df <- data.frame(
      x = factor(c("medium", "high", "low", "medium", "low", "low"),
                 levels = c("low", "medium", "high"))
   )
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(r$top_levels, c("low", "medium", "high"))
   # top_counts must align positionally with top_levels.
   expect_equal(r$top_counts, c(3L, 2L, 1L))
   expect_null(r$truncated)
})

test_that(".rs.summarizeColumn() truncates factors with > 50 levels and flags truncated", {
   lvls <- sprintf("L%03d", 1:75)
   df <- data.frame(x = factor(sample(lvls, 200, replace = TRUE), levels = lvls))
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(length(r$top_levels), 50L)
   expect_equal(length(r$top_counts), 50L)
   # Truncation preserves encoding order: first 50 levels, in order.
   expect_equal(r$top_levels, lvls[1:50])
   expect_equal(.rs.summarize.bare(r$truncated), TRUE)
})

test_that(".rs.summarizeColumn() summarizes logical columns", {
   df <- data.frame(x = c(TRUE, TRUE, FALSE, NA, TRUE))
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(.rs.summarize.bare(r$n_true), 3L)
   expect_equal(.rs.summarize.bare(r$n_false), 1L)
})

test_that(".rs.summarizeColumn() summarizes Date columns", {
   df <- data.frame(x = as.Date(c("2024-01-01", "2024-06-15", NA, "2024-12-31")))
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(.rs.summarize.bare(r$min), "2024-01-01")
   expect_equal(.rs.summarize.bare(r$max), "2024-12-31")
})

test_that(".rs.summarizeColumn() summarizes POSIXct columns", {
   df <- data.frame(x = as.POSIXct(c("2024-01-01 09:00:00", "2024-06-15 17:30:00"),
                                   tz = "UTC"))
   r <- .rs.summarizeColumn(df, 1)
   expect_match(.rs.summarize.bare(r$min), "^2024-01-01")
   expect_match(.rs.summarize.bare(r$max), "^2024-06-15")
})

test_that(".rs.summarizeColumn() returns base stats for unsupported column types", {
   # complex and raw don't match any of the typed branches.
   df <- data.frame(x = as.complex(c(1, 2, NA)))
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(.rs.summarize.bare(r$n), 3L)
   expect_equal(.rs.summarize.bare(r$n_na), 1L)
   # No type-specific stats keys.
   expect_null(r$min)
   expect_null(r$top_levels)
   expect_null(r$n_true)
})

test_that(".rs.summarizeColumn() handles zero-row data frames", {
   df <- data.frame(x = integer(0))
   r <- .rs.summarizeColumn(df, 1)
   expect_equal(.rs.summarize.bare(r$n), 0L)
   expect_equal(.rs.summarize.bare(r$n_na), 0L)
   expect_equal(.rs.summarize.bare(r$n_unique), 0L)
   expect_null(r$min)
})
