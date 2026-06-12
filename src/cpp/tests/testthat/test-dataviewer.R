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

test_that(".rs.describeColsByIndex() describes the requested columns", {
   tbl <- data.frame(x = 1:2, y = 1:2, x = 1:2)
   # length includes the always-present rownames column at the front
   expect_equal(length(.rs.describeColsByIndex(tbl, 1)), 2)
   expect_equal(length(.rs.describeColsByIndex(tbl, c(1, 2))), 3)
   expect_equal(length(.rs.describeColsByIndex(tbl, 1:3)), 4)
   # out-of-range indices are dropped
   expect_equal(length(.rs.describeColsByIndex(tbl, c(1, 99))), 2)
   # an empty / all-invalid request falls back to the full frame
   expect_equal(length(.rs.describeColsByIndex(tbl, integer())), 4)
   expect_equal(length(.rs.describeColsByIndex(tbl, c(0, -1))), 4)
})

test_that(".rs.describeColsByIndex() preserves order and reports absolute col_index", {
   tbl <- data.frame(a = 1:2, b = 1:2, c = 1:2, d = 1:2)

   # An arbitrary, non-contiguous request (pinned column first, then a window)
   # is honored in order, and each column carries its absolute index.
   res <- .rs.describeColsByIndex(tbl, c(1, 3, 4))
   expect_equal(length(res), 4)
   expect_equal(res[[1]]$col_type, .rs.scalar("rownames"))
   expect_equal(res[[1]]$col_index, .rs.scalar(0L))
   expect_equal(res[[2]]$col_index, .rs.scalar(1L))
   expect_equal(res[[2]]$col_name, .rs.scalar("a"))
   expect_equal(res[[3]]$col_index, .rs.scalar(3L))
   expect_equal(res[[3]]$col_name, .rs.scalar("c"))
   expect_equal(res[[4]]$col_index, .rs.scalar(4L))
   expect_equal(res[[4]]$col_name, .rs.scalar("d"))

   # The non-paginated path reports contiguous absolute indices by default.
   direct <- .rs.describeCols(tbl)
   expect_equal(direct[[1]]$col_index, .rs.scalar(0L))
   expect_equal(direct[[2]]$col_index, .rs.scalar(1L))
   expect_equal(direct[[5]]$col_index, .rs.scalar(4L))
})

test_that(".rs.describeColsByIndex() returns NULL for empty data frames", {
   tbl <- data.frame()
   expect_equal(.rs.describeColsByIndex(tbl, 1), NULL)
})

test_that(".rs.describeColsByIndex() emits a fingerprint stable across pagination", {
   tbl <- as.data.frame(setNames(
      replicate(20, 1:5, simplify = FALSE),
      paste0("X", 1:20)
   ))
   slice_a <- .rs.describeColsByIndex(tbl, 1:5)
   slice_b <- .rs.describeColsByIndex(tbl, 6:10)
   slice_c <- .rs.describeColsByIndex(tbl, 16:20)

   # The rownames slot is always the first column and is where the
   # fingerprint is attached. Anchor that here so a refactor that moves
   # the slot doesn't make the rest of the test silently pass on NULLs.
   expect_equal(slice_a[[1]]$col_type, .rs.scalar("rownames"))

   # Same underlying frame -> identical fingerprint regardless of slice.
   expect_identical(slice_a[[1]]$cols_fingerprint, slice_b[[1]]$cols_fingerprint)
   expect_identical(slice_a[[1]]$cols_fingerprint, slice_c[[1]]$cols_fingerprint)

   # A different frame produces a different fingerprint.
   other <- data.frame(a = 1:5, b = 1:5, c = 1:5)
   slice_other <- .rs.describeColsByIndex(other, 1:3)
   expect_false(identical(slice_a[[1]]$cols_fingerprint,
                          slice_other[[1]]$cols_fingerprint))

   # describeCols (the non-paginated path) also emits a fingerprint, and it
   # matches the slice-path fingerprint for the same underlying frame.
   direct <- .rs.describeCols(tbl)
   expect_identical(direct[[1]]$cols_fingerprint,
                    slice_a[[1]]$cols_fingerprint)
})

test_that(".rs.describeCols() fingerprint distinguishes hyphen-collisions", {
   # Reassignments like c("a-b", "c") vs c("a", "b-c") must not produce
   # identical fingerprints, otherwise the per-object state check intended
   # to detect object reassignment would silently apply mismatched indices.
   x1 <- setNames(data.frame(1:3, 4:6), c("a-b", "c"))
   x2 <- setNames(data.frame(1:3, 4:6), c("a", "b-c"))
   fp1 <- .rs.describeCols(x1)[[1]]$cols_fingerprint
   fp2 <- .rs.describeCols(x2)[[1]]$cols_fingerprint
   expect_false(identical(fp1, fp2))
})

test_that(".rs.dataViewer.colsFingerprint() returns NA for empty/NULL names", {
   # The client treats a null/missing fingerprint as always-mismatch and
   # discards any saved per-object state -- there's no anchor to align
   # positional indices against, so applying them would be unsafe.
   expect_true(is.na(.rs.dataViewer.colsFingerprint(data.frame())))

   noNames <- data.frame(1:3, 4:6)
   names(noNames) <- NULL
   expect_true(is.na(.rs.dataViewer.colsFingerprint(noNames)))

   # Empty character vector (the zero-length-names case the client guards
   # against) must hash to NA, not to digest("")
   expect_true(is.na(.rs.dataViewer.colsFingerprint(structure(list(), names = character()))))
})

test_that(".rs.digest() distinguishes character vectors that concat-collide", {
   # A naive delimiter join would let c("a","b") and c("ab") hash
   # identically; serialize() encodes element boundaries unambiguously
   # so these always differ.
   expect_false(identical(.rs.digest(c("a", "b")), .rs.digest("ab")))
   expect_false(identical(.rs.digest(c("a-b", "c")), .rs.digest(c("a", "b-c"))))

   # Stable across calls.
   expect_identical(.rs.digest(c("x", "y")), .rs.digest(c("x", "y")))
})

test_that(".rs.digest() matches the canonical Adler-32 reference value", {
   # 0x024d0127 is the standard Adler-32 of "abc" -- guards against
   # accidental algorithm drift in the chunked integer rewrite.
   expect_identical(.rs.digest(charToRaw("abc")), "024d0127")

   # Adler-32 of the empty input is defined as 1.
   expect_identical(.rs.digest(raw()), "00000001")

   # Pin a serialize()-routed input. Real callers (colsFingerprint,
   # log-once dedup) pass character vectors. .rs.digest() pins
   # serialize(version = 2L) and zeroes the writer-version field so the
   # hash is stable across R upgrades; if either of those guards is
   # dropped, this expectation will fire on whichever R version doesn't
   # match the value the test was originally pinned against.
   expect_identical(.rs.digest(c("a", "b")), "1732015b")
})

test_that(".rs.digest() zeroes the v2 writer-version field", {
   # serialize(version = 2L) embeds the writer's R version in bytes
   # 7-10 of its output. The character-input path of .rs.digest()
   # zeroes those bytes before hashing so the colsFingerprint
   # persisted alongside data-viewer state stays stable across R
   # upgrades. Verify by reproducing the post-patch bytes manually
   # and feeding them in via the raw path (which hashes directly,
   # without re-patching).
   manuallyPatched <- serialize(c("a", "b"), connection = NULL,
                                ascii = FALSE, version = 2L)
   manuallyPatched[7:10] <- as.raw(0L)
   expect_identical(.rs.digest(c("a", "b")), .rs.digest(manuallyPatched))

   # And without the patch, the writer-version bytes change the hash
   # -- if .rs.digest() ever stops patching, this would silently
   # invalidate every persisted data-viewer state token after an R
   # upgrade.
   unpatched <- serialize(c("a", "b"), connection = NULL,
                          ascii = FALSE, version = 2L)
   expect_false(identical(.rs.digest(c("a", "b")), .rs.digest(unpatched)))
})

test_that(".rs.digest() chunked path matches the recursive Adler-32 spec", {
   # Cross-check the chunked implementation against a byte-at-a-time
   # reference taken straight from the spec. Inputs deliberately span
   # several chunks (chunk size is 1024) and include a non-multiple
   # length so the partial trailing chunk is exercised.
   refAdler32 <- function(bytes) {
      a <- 1L
      b <- 0L
      for (byte in as.integer(bytes))
      {
         a <- (a + byte) %% 65521L
         b <- (b + a) %% 65521L
      }
      sprintf("%04x%04x", b, a)
   }

   uniform <- as.raw(rep(255L, 5000L))
   expect_identical(.rs.digest(uniform), refAdler32(uniform))

   mixed <- as.raw(rep_len(0:255, 5000L))
   expect_identical(.rs.digest(mixed), refAdler32(mixed))

   # Bracket the chunk-size boundary: n=1023 (just under, single short
   # chunk), n=1024 (exact single chunk), n=1025 (two iterations, with
   # a degenerate k=1 trailing chunk). An off-by-one in either
   # `end <- min(pos + size - 1L, n)` or the `pos <= n` loop condition
   # would slip through a multi-chunk test like n=2048 but get caught
   # by at least one of these.
   under <- as.raw(rep_len(0:255, 1023L))
   expect_identical(.rs.digest(under), refAdler32(under))

   exact <- as.raw(rep_len(0:255, 1024L))
   expect_identical(.rs.digest(exact), refAdler32(exact))

   over <- as.raw(rep_len(0:255, 1025L))
   expect_identical(.rs.digest(over), refAdler32(over))

   # Two full chunks -- guards against the chunk-stitching arithmetic
   # (k*a carry between iterations) drifting from the recursive form.
   twoChunks <- as.raw(rep_len(0:255, 2048L))
   expect_identical(.rs.digest(twoChunks), refAdler32(twoChunks))
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

test_that(".rs.describeCols() reports col_type as typeof() and col_class as class()", {
   df <- data.frame(
      d = c(1.5, 2.5),
      i = 1:2,
      c = c("a", "b"),
      l = c(TRUE, FALSE),
      f = factor(c("x", "y")),
      stringsAsFactors = FALSE
   )
   # a list column whose elements are themselves atomic vectors -- this is the
   # shape from #17863 that the old first-element heuristic misclassified as
   # "unknown" rather than "list"
   df$lst <- list(1:3, 4:6)

   cols <- .rs.describeCols(df)

   # cols[[1]] is the synthetic rownames column
   expect_equal(cols[[1]]$col_type, .rs.scalar("rownames"))
   expect_equal(cols[[1]]$col_class, "rownames")

   # data columns follow, in order, reporting typeof() and class()
   types   <- vapply(cols[-1], function(col) as.character(col$col_type), "")
   classes <- lapply(cols[-1], function(col) col$col_class)
   expect_equal(types, c("double", "integer", "character", "logical", "integer", "list"))
   expect_equal(classes, list("numeric", "integer", "character", "logical", "factor", "list"))

   # the Data Import preview keys its column-type detection on col_class[[1]]
   # (read as col_class[0] in DataImportOptions.java), so the most-specific
   # class must be the first element of the array for every column
   first_class <- vapply(cols[-1], function(col) col$col_class[[1]], "")
   expect_equal(first_class, c("numeric", "integer", "character", "logical", "factor", "list"))
})

test_that(".rs.describeCols() handles integer columns whose range overflows", {
   # max_v - min_v overflows integer arithmetic here; the integer-breaks
   # heuristic must compute the range in double precision rather than
   # erroring with "missing value where TRUE/FALSE needed". See #17951.
   df <- data.frame(id = c(-2000000000L, 2000000000L))

   cols <- .rs.describeCols(df)

   col <- cols[[2L]]
   expect_equal(col$col_search_type, .rs.scalar("numeric"))
   expect_true(length(col$col_breaks) > 1)
   expect_equal(sum(col$col_counts), 2)
})

test_that(".rs.describeCols() reports a nested data.frame column faithfully", {
   df <- data.frame(a = 1:2)
   # a column that is itself a data.frame. describeCols does not flatten (that
   # happens upstream in the viewer pipeline via .rs.flattenFrame), so it must
   # report the column as typeof() "list" with class() "data.frame" -- the
   # shape the frontend's isDataFrameColumn relies on to tell list and
   # data.frame columns apart.
   df$nested <- data.frame(x = c(1.5, 2.5), y = c(3L, 4L))

   cols <- .rs.describeCols(df)

   # cols[[1]] rownames, cols[[2]] = a, cols[[3]] = nested
   nested <- cols[[3L]]
   expect_equal(nested$col_type, .rs.scalar("list"))
   expect_equal(nested$col_class, "data.frame")
})

test_that(".rs.applyTransform() does not error when sorting a list column", {
   df <- data.frame(a = c(3L, 1L, 2L))
   df$b <- list(10:12, 1:2, 100)

   # column 2 is the list column; sorting it must be a no-op rather than an
   # error ("unimplemented type 'list' in 'orderVector1'"). See #17863.
   asc  <- .rs.applyTransform(df, character(), "", 2L, "asc")
   desc <- .rs.applyTransform(df, character(), "", 2L, "desc")
   expect_equal(asc$a, df$a)
   expect_equal(desc$a, df$a)

   # sorting an atomic column still works
   sorted <- .rs.applyTransform(df, character(), "", 1L, "asc")
   expect_equal(sorted$a, c(1L, 2L, 3L))
})

test_that(".rs.applyTransform() applies a character filter as case-insensitive substring", {
   df <- data.frame(x = c("apple", "Grape", "pineapple", "cherry"),
                    y = 1:4)

   # substring match, not anchored to the start
   out <- .rs.applyTransform(df, c("character|apple", ""), "", integer(), character())
   expect_equal(out$x, c("apple", "pineapple"))

   # case-insensitive
   out <- .rs.applyTransform(df, c("character|GRAPE", ""), "", integer(), character())
   expect_equal(out$x, "Grape")

   # regex metacharacters are matched literally
   df <- data.frame(x = c("a.b", "axb"))
   out <- .rs.applyTransform(df, "character|a.b", "", integer(), character())
   expect_equal(out$x, "a.b")
})

test_that(".rs.applyTransform() applies a factor filter by level index", {
   df <- data.frame(f = factor(c("b", "a", "c", "a"), levels = c("a", "b", "c")),
                    y = 1:4)

   # "factor|1" selects the first level ("a"), not the first value
   out <- .rs.applyTransform(df, c("factor|1", ""), "", integer(), character())
   expect_equal(as.character(out$f), c("a", "a"))
   expect_equal(out$y, c(2L, 4L))

   # NA entries never match
   df$f[1] <- NA
   out <- .rs.applyTransform(df, c("factor|2", ""), "", integer(), character())
   expect_equal(nrow(out), 0L)
})

test_that(".rs.applyTransform() applies numeric range and equality filters", {
   df <- data.frame(x = c(1, 5, 10, NA, Inf, 7), y = 1:6)

   # inclusive range; NA and infinite values are dropped implicitly
   out <- .rs.applyTransform(df, c("numeric|5_10", ""), "", integer(), character())
   expect_equal(out$x, c(5, 10, 7))

   # single-value equality
   out <- .rs.applyTransform(df, c("numeric|7", ""), "", integer(), character())
   expect_equal(out$y, 6L)

   # negative bounds
   df <- data.frame(x = c(-10, -5, 0, 5))
   out <- .rs.applyTransform(df, "numeric|-7_0", "", integer(), character())
   expect_equal(out$x, c(-5, 0))
})

test_that(".rs.applyTransform() applies a boolean filter and drops NA rows", {
   df <- data.frame(b = c(TRUE, FALSE, NA, TRUE), y = 1:4)

   out <- .rs.applyTransform(df, c("boolean|TRUE", ""), "", integer(), character())
   expect_equal(out$y, c(1L, 4L))

   out <- .rs.applyTransform(df, c("boolean|FALSE", ""), "", integer(), character())
   expect_equal(out$y, 2L)
})

test_that(".rs.applyTransform() ignores malformed and empty column filters", {
   df <- data.frame(x = c("a", "b"), y = 1:2)

   # no type prefix: skipped rather than applied or errored
   out <- .rs.applyTransform(df, c("justtext", ""), "", integer(), character())
   expect_equal(nrow(out), 2L)

   # empty filter strings leave the frame untouched
   out <- .rs.applyTransform(df, c("", ""), "", integer(), character())
   expect_equal(nrow(out), 2L)
})

test_that(".rs.applyTransform() global search matches data and row names", {
   df <- data.frame(x = c("apple", "banana"), y = c("zucchini", "apricot"),
                    row.names = c("first", "second"))

   # substring across all columns, case-insensitive
   out <- .rs.applyTransform(df, character(2L), "AP", integer(), character())
   expect_equal(rownames(out), c("first", "second"))

   out <- .rs.applyTransform(df, character(2L), "zucc", integer(), character())
   expect_equal(rownames(out), "first")

   # matches against row names too
   out <- .rs.applyTransform(df, character(2L), "second", integer(), character())
   expect_equal(out$x, "banana")
})

test_that(".rs.applyTransform() column filters compose with search and sort", {
   df <- data.frame(x = c("ab", "ab", "ab", "cd"),
                    y = c("u", "v", "u", "u"),
                    z = c(3, 1, 2, 0))

   out <- .rs.applyTransform(df, c("character|ab", "", ""), "u", 3L, "desc")
   expect_equal(out$z, c(3, 2))
})

test_that(".rs.applyTransform() sorts non-numeric column types in both directions", {
   # character
   df <- data.frame(x = c("pear", "apple", "fig"))
   expect_equal(.rs.applyTransform(df, character(), "", 1L, "asc")$x,
                c("apple", "fig", "pear"))
   expect_equal(.rs.applyTransform(df, character(), "", 1L, "desc")$x,
                c("pear", "fig", "apple"))

   # factor: sorts by level order, not alphabetically
   df <- data.frame(f = factor(c("high", "low", "mid"),
                               levels = c("low", "mid", "high")))
   expect_equal(as.character(.rs.applyTransform(df, character(), "", 1L, "asc")$f),
                c("low", "mid", "high"))
   expect_equal(as.character(.rs.applyTransform(df, character(), "", 1L, "desc")$f),
                c("high", "mid", "low"))

   # logical
   df <- data.frame(b = c(TRUE, FALSE, TRUE))
   expect_equal(.rs.applyTransform(df, character(), "", 1L, "asc")$b,
                c(FALSE, TRUE, TRUE))
   expect_equal(.rs.applyTransform(df, character(), "", 1L, "desc")$b,
                c(TRUE, TRUE, FALSE))

   # Date
   df <- data.frame(d = as.Date(c("2024-06-01", "2023-01-15", "2025-12-31")))
   expect_equal(.rs.applyTransform(df, character(), "", 1L, "asc")$d,
                sort(df$d))
   expect_equal(.rs.applyTransform(df, character(), "", 1L, "desc")$d,
                sort(df$d, decreasing = TRUE))

   # POSIXct
   df <- data.frame(t = as.POSIXct(c("2024-06-01 12:00:00",
                                     "2024-06-01 09:30:00",
                                     "2024-06-01 23:59:59"), tz = "UTC"))
   expect_equal(.rs.applyTransform(df, character(), "", 1L, "desc")$t,
                sort(df$t, decreasing = TRUE))
})

test_that(".rs.applyTransform() applies every key in a multi-column sort", {
   # primary key 'a' has ties that the tiebreaker 'c' resolves; 'b' is a list
   # column sitting between them so it is skipped without disturbing the
   # dirs/vals alignment of the remaining keys. The old "for (i in length(cols))"
   # loop ran once with the last key only, sorting by 'c' alone.
   df <- data.frame(a = c(1L, 1L, 2L, 2L))
   df$b <- list(1, 2:3, 4:6, 7)
   df$c <- c(2L, 1L, 2L, 1L)

   sorted <- .rs.applyTransform(df, character(3L), "",
                                c(1L, 2L, 3L), c("asc", "asc", "asc"))

   # correct multi-key result: primary 'a' ascending, ties broken by 'c'
   expect_equal(sorted$a, c(1L, 1L, 2L, 2L))
   expect_equal(sorted$c, c(1L, 2L, 1L, 2L))
})
