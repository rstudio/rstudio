#
# test-linter.R
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

library(testthat)

context("diagnostics")
setwd("../../session/modules")

# every .R file under src/cpp
root <- normalizePath("../..", mustWork = TRUE)
rSourceFiles <- list.files(
   root,
   pattern = "[.]R$",
   full.names = TRUE,
   recursive = TRUE
)

lint <- function(x) {
   invisible(.rs.lintRFile(x))
}

test_that("R source files do not contain non-ASCII characters", {

   nonAsciiFiles <- Filter(function(path) {
      bytes <- readBin(path, what = "raw", n = file.info(path)$size)
      any(bytes > as.raw(0x7f))
   }, rSourceFiles)

   nonAsciiFiles <- sub(paste0(root, "/"), "", nonAsciiFiles, fixed = TRUE)

   expect_equal(
      nonAsciiFiles,
      character(),
      label = "Files with non-ASCII characters"
   )

})

test_that("R source files pass PACKAGE = \"(embedding)\" to .Call()", {

   # without PACKAGE, .Call() searches every loaded DLL for the routine
   # rather than resolving it against those registered by rsession
   offenders <- character()
   for (rFile in rSourceFiles) {

      parseData <- getParseData(parse(rFile, keep.source = TRUE))

      # a '.Call' token's parent is the function expression, whose parent
      # is the whole call, including any multi-line arguments
      isCall <- parseData$token == "SYMBOL_FUNCTION_CALL" & parseData$text == ".Call"
      fnIds <- parseData$parent[isCall]
      callIds <- parseData$parent[match(fnIds, parseData$id)]

      for (callId in callIds) {
         call <- str2lang(getParseText(parseData, callId))
         if (!identical(call[["PACKAGE"]], "(embedding)")) {
            line <- parseData$line1[parseData$id == callId]
            path <- sub(paste0(root, "/"), "", rFile, fixed = TRUE)
            offenders <- c(offenders, paste0(path, ":", line))
         }
      }

   }

   failureMessage <- paste(
      c(".Call() sites without PACKAGE = \"(embedding)\":", offenders),
      collapse = "\n"
   )

   expect(length(offenders) == 0, failureMessage)

})

test_that("RStudio .R files can be linted", {
   
   rFiles <- list.files(
      pattern = "R$",
      full.names = TRUE,
      recursive = TRUE
   )
   
   lapply(rFiles, function(x) {
      results <- lint(x)
      errors <- results[unlist(lapply(results, function(x) {
         x$type == "error"
      }))]
      expect_equal(length(errors), 0)
      if (length(errors))
         warning("Lint errors in file: '",
                 x,
                 "'",
                 call. = FALSE)
   })
})
