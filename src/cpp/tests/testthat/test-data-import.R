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

   # Each entry is a column name that, without escaping, would either escape
   # the cols() argument and inject a top-level call (the quote/paren case),
   # or break parse() outright (the backslash/newline cases).
   evilNames <- list(
      injection = 'foo"); .rs.injection.sentinel <- TRUE; readr::cols("',
      backslash = 'foo\\bar',
      newline   = 'foo\nbar'
   )

   for (evil in evilNames)
   {
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

      # The single expression must be a call to readr::read_csv. R deparses
      # readr::read_csv as a 3-element call object: `::`, `readr`, `read_csv`.
      expect_equal(as.character(parsed[[1L]][[1L]]), c("::", "readr", "read_csv"))

      # The column name should round-trip: the col_types argument should contain
      # one named slot whose name equals the original (unescaped) malicious string.
      call <- parsed[[1L]]
      col_types <- call$col_types
      expect_false(is.null(col_types))
      expect_equal(names(eval(col_types)$cols), evil)
   }
})

test_that("assembleDataImport escapes character option values", {
   skip_if_not_installed("readr")

   # Same shape of test as the column-name case, but exercising the
   # "character" option-type path used for read_csv arguments such as `na`.
   evilValues <- list(
      injection = 'NA"); .rs.injection.sentinel <- TRUE; c("',
      backslash = 'na\\sentinel',
      newline   = 'na\nsentinel'
   )

   for (evil in evilValues)
   {
      opts <- list(
         mode = "text",
         importLocation = "data.csv",
         delimiter = ",",
         na = evil,
         openDataViewer = FALSE
      )

      info <- .rs.assembleDataImport(opts)

      parsed <- parse(text = info$previewCode)
      expect_length(parsed, 1L)
      expect_equal(as.character(parsed[[1L]][[1L]]), c("::", "readr", "read_csv"))

      # The na argument should round-trip to the original (unescaped) string.
      expect_equal(parsed[[1L]]$na, evil)
   }
})

test_that("assembleDataImport escapes locale option values", {
   skip_if_not_installed("readr")

   # Locale values reach the generated code via the "locale" option-type
   # branch, which formerly pasted the value into "..." with no escaping.
   evil <- 'UTC"); .rs.injection.sentinel <- TRUE; readr::locale("'

   opts <- list(
      mode = "text",
      importLocation = "data.csv",
      delimiter = ",",
      locale = list(
         dateName     = "en",
         dateFormat   = "%AD",
         timeFormat   = "%AT",
         decimalMark  = ".",
         groupingMark = ",",
         tz           = evil,
         encoding     = "UTF-8",
         asciify      = FALSE
      ),
      openDataViewer = FALSE
   )

   info <- .rs.assembleDataImport(opts)

   parsed <- parse(text = info$previewCode)
   expect_length(parsed, 1L)

   # The malicious tz value should round-trip in the parsed locale() call.
   # We inspect the AST rather than eval() since readr::locale validates
   # tz strings against the system tz database.
   localeCall <- parsed[[1L]]$locale
   expect_false(is.null(localeCall))
   expect_equal(localeCall$tz, evil)
})

test_that("assembleDataImport escapes import URLs in cache code", {
   skip_if_not_installed("readr")

   # When canCacheData is TRUE and importLocation is a URL, the assembled
   # importCode includes assignments such as `url <- "<importLocation>"`.
   # The URL must be encoded as an R string literal.
   evilUrl <- 'https://example.com/"); .rs.injection.sentinel <- TRUE; x <- c("data.csv'

   opts <- list(
      mode = "text",
      importLocation = evilUrl,
      delimiter = ",",
      canCacheData = TRUE,
      openDataViewer = FALSE
   )

   info <- .rs.assembleDataImport(opts)

   # importCode must parse as a sequence of top-level expressions; if the URL
   # escapes its string literal, parse() will reject the input.
   parsed <- parse(text = info$importCode)
   expect_gt(length(parsed), 0L)

   # Locate the `url <- "..."` assignment and confirm it round-trips.
   urlValue <- NULL
   for (expr in as.list(parsed))
   {
      if (is.call(expr) && identical(as.character(expr[[1L]]), "<-") &&
          identical(as.character(expr[[2L]]), "url"))
      {
         urlValue <- expr[[3L]]
         break
      }
   }
   expect_false(is.null(urlValue))
   expect_equal(urlValue, evilUrl)
})
