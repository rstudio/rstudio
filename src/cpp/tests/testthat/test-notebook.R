#
# test-notebook.R
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

test_that("#| comments are removed by .rs.extractChunkInnerCode() (#11606)", {
   code <- "```{sql}\n#| connection = db\n\nSELECT 'test'\n```"
   extract <- .rs.extractChunkInnerCode(code)
   expect_true(!any(grepl("#[|]", extract)))
})

test_that(".rs.rnb.extractInlineRCode() finds inline R code in prose only", {
   contents <- c(
      "---",
      "title: test",
      "output: html_notebook",
      "---",
      "",
      "```{r}",
      "x <- 42  # `r 1 + 1` within a chunk is not inline code",
      "```",
      "",
      "The answer is `r 1 + 1`.",
      "Today is `r Sys.Date()`."
   )

   code <- .rs.rnb.extractInlineRCode(contents)
   expect_equal(code, c("1 + 1", "Sys.Date()"))
})

test_that(".rs.rnb.evaluateInlineChunks() evaluates inline chunks (#17521)", {
   rmdPath <- tempfile(fileext = ".Rmd")
   writeLines(c(
      "---",
      "title: test",
      "output: html_notebook",
      "---",
      "",
      "Value: `r .rs.test.inlineValue * 2`",
      "Broken: `r .rs.test.noSuchObject`"
   ), con = rmdPath)
   on.exit(unlink(rmdPath), add = TRUE)

   # inline chunks are evaluated against the global environment
   assign(".rs.test.inlineValue", 21, envir = globalenv())
   on.exit(rm(".rs.test.inlineValue", envir = globalenv()), add = TRUE)

   cachePath <- .rs.rnb.evaluateInlineChunks(rmdPath)
   expect_true(file.exists(cachePath))
   on.exit(unlink(cachePath), add = TRUE)

   outputs <- readRDS(cachePath)
   expect_equal(outputs[[".rs.test.inlineValue * 2"]]$text, "42")
   expect_match(outputs[[".rs.test.noSuchObject"]]$error, "not found")
})

test_that(".rs.rnb.evaluateInlineChunks() returns '' when no inline code present", {
   rmdPath <- tempfile(fileext = ".Rmd")
   writeLines(c(
      "---",
      "title: test",
      "output: html_notebook",
      "---",
      "",
      "Just prose; the chunk below is not inline code.",
      "",
      "```{r}",
      "1 + 1",
      "```"
   ), con = rmdPath)
   on.exit(unlink(rmdPath), add = TRUE)

   expect_equal(.rs.rnb.evaluateInlineChunks(rmdPath), "")
})
