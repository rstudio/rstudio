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

      # a file R can't parse can't run a .Call() either
      exprs <- tryCatch(parse(rFile, keep.source = TRUE), error = function(e) NULL)
      if (is.null(exprs))
         next

      parseData <- getParseData(exprs)

      # a '.Call' token's parent is the function expression, whose parent
      # is the whole call, including any multi-line arguments
      isCall <- parseData$token == "SYMBOL_FUNCTION_CALL" & parseData$text == ".Call"
      fnIds <- parseData$parent[isCall]
      callIds <- parseData$parent[match(fnIds, parseData$id)]

      for (callId in callIds) {

         call <- str2lang(getParseText(parseData, callId))

         # leave alone calls that target another DLL: a routine name that
         # isn't one of ours (all are 'rs_*'), or a native symbol object
         # from another package's namespace (e.g. grDevices:::C_foo)
         routine <- call[[2L]]
         if (is.character(routine) && !startsWith(routine, "rs_"))
            next
         if (is.call(routine) && identical(routine[[1L]], as.name(":::")))
            next
         if (is.call(routine) && identical(routine[[1L]], as.name("::")))
            next

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

test_that("R source files do not use zero-padded string formats", {

   # the '0' flag is only defined for numeric conversions; with '%s',
   # C's printf() zero-pads on some platforms and space-pads on others,
   # so a format like '%05s' behaves differently across operating systems.
   # use formatC(x, width = n, flag = "0") or sprintf("%05d", as.integer(x))
   # when zero-padding is intended
   #
   # the conversion may carry a positional argument ('%1$05s') and a
   # precision ('%05.2s'); an escaped percent sign ('%%05s') is literal text
   pattern <- "(^|[^%])(%%)*%([0-9]+\\$)?[-+ #]*0[0-9]*(\\.[0-9]*)?s"

   # the offending fixtures are assembled so this file passes its own lint
   offending <- paste0('"%', c("05s", "-010s", "05.2s", "1$05s", "%%05s"), '"')
   expect_true(all(grepl(pattern, offending)))

   allowed <- c('"%%05s"', '"%10s %05d %s"', '"%100s"', '"%.5s"')
   expect_false(any(grepl(pattern, allowed)))

   offenders <- character()
   for (rFile in rSourceFiles) {

      exprs <- tryCatch(parse(rFile, keep.source = TRUE), error = function(e) NULL)
      if (is.null(exprs))
         next

      # check string literals only, so that comments and symbols are ignored
      parseData <- getParseData(exprs)
      strings <- parseData[parseData$token == "STR_CONST", ]
      matches <- strings[grepl(pattern, strings$text), ]

      if (nrow(matches)) {
         path <- sub(paste0(root, "/"), "", rFile, fixed = TRUE)
         offenders <- c(offenders, unique(paste0(path, ":", matches$line1)))
      }

   }

   failureMessage <- paste(
      c("String literals using a zero-padded '%s' format:", offenders),
      collapse = "\n"
   )

   expect(length(offenders) == 0, failureMessage)

})

# The line numbers at which a regular expression matches a file's contents.
matchLines <- function(contents, pattern) {

   starts <- gregexpr(pattern, contents, perl = TRUE)[[1L]]
   if (starts[[1L]] == -1L)
      return(integer())

   # one more than the newlines before each match
   newlines <- gregexpr("\n", contents, fixed = TRUE)[[1L]]
   findInterval(starts, newlines[newlines > 0L]) + 1L

}

# Mask comments and literals so examples in documentation are ignored and
# parentheses inside strings do not affect where a C++ call ends. Keep newlines
# so diagnostics still point at the original source.
maskCppNonCode <- function(contents) {
   rawString <- 'R"([^()\\\\[:space:]]{0,16})\\((?s:.*?)\\)\\1"'
   string <- '"(?:\\\\(?s:.)|[^"\\\\\r\n])*"'
   # A C++ numeric literal can contain apostrophes as digit separators.
   char <- "(?<![[:alnum:]_])(?:u8|[LuU])?'(?:\\\\(?s:.)|[^'\\\\\r\n])*'"
   pattern <- paste(rawString, string, char, "//[^\\n]*", "/\\*(?s:.*?)\\*/", sep = "|")
   matches <- gregexpr(pattern, contents, perl = TRUE)
   tokens <- regmatches(contents, matches)[[1L]]
   regmatches(contents, matches) <- list(gsub("[^\n]+", " ", tokens))
   contents
}

test_that("C++ sources capture and compare error codes with their categories", {

   # an errc is an errno value, never a system code
   errcAsSystem <- "\\berrc::[a-z_]+\\s*,\\s*(boost::system::|std::)?system_category\\(\\)"

   # comparing a code's value with an errc ignores its category, so a Win32
   # code matches whichever errc shares its number: compare as a condition
   # (ec == errc::X, or error.matches(errc::X))
   errcByValue <- paste0(
      "(getCode|value)\\(\\)\\s*[!=]=\\s*(boost::system::|std::)?errc::",
      "|\\berrc::[a-z_]+\\s*[!=]=\\s*[A-Za-z_][A-Za-z0-9_.>()-]*(getCode|value)\\(\\)"
   )

   # Match the whole call in either operand position. The lookahead selects a
   # portable errc as the first argument. The recursive group balances nested
   # parentheses in the remaining arguments (including ErrorLocation()).
   errcCall <- paste0(
      "(?:::)?(?:\\b\\w+::)*\\bsystemError\\s*",
      "(?=\\(\\s*(?:::)?(?:boost::system::)?errc::\\w+\\s*,)",
      "(\\((?:[^();]++|(?-1))*\\))"
   )
   errcComparison <- paste0("[!=]=\\s*", errcCall, "(?!\\s*\\.)|", errcCall, "\\s*[!=]=")

   # A raw value comparison drops the category in shared and platform-specific
   # code alike. For a native-only errno, compare the complete error_code.
   errnoByValue <- paste0(
      "(getCode|value)\\(\\)\\s*[!=]=\\s*E(?!OF\\b)[A-Z0-9]+\\b",
      "|\\bE(?!OF\\b)[A-Z0-9]+\\s*[!=]=\\s*[A-Za-z_][A-Za-z0-9_.>()-]*(getCode|value)\\(\\)"
   )

   expect_true(grepl(errcAsSystem, "error_code(errc::io_error,\n   boost::system::system_category())", perl = TRUE))
   expect_false(grepl(errcAsSystem, "error_code(errc::io_error, generic_category())", perl = TRUE))
   expect_true(grepl(errcByValue, "error.getCode() == boost::system::errc::permission_denied", perl = TRUE))
   expect_true(grepl(errcByValue, "ec.value() != errc::bad_file_descriptor", perl = TRUE))
   expect_true(grepl(errcByValue, "errc::timed_out == pError->getCode()", perl = TRUE))
   expect_false(grepl(errcByValue, "ec == boost::system::errc::too_many_files_open", perl = TRUE))
   expect_true(grepl(errnoByValue, "error.getCode() == ENOENT", perl = TRUE))
   expect_true(grepl(errnoByValue, "EACCES != error.getCode()", perl = TRUE))
   expect_true(grepl(errnoByValue, "ENOENT == e.code().value()", perl = TRUE))
   expect_false(grepl(errnoByValue, "error.getCode() == ERROR_ACCESS_DENIED", perl = TRUE))
   expect_false(grepl(errnoByValue, "input.value() == EOF", perl = TRUE))

   fixture <- paste(c(
      "if (error == ::rstudio::core::systemError(boost::system::errc::timed_out, ERROR_LOCATION))",
      "if (systemError(errc::timed_out,\n  describe(context()), ErrorLocation()) != error)",
      "if (error != core::systemError(errc::timed_out, loc))",
      "if (systemError(errc::timed_out, loc) == error)",
      "EXPECT_EQ(error, systemError(errc::timed_out, loc));",
      "if (error.matches(errc::timed_out))",
      "if (text == systemError(errc::timed_out, loc).getMessage())",
      "if (rpcError == systemError(json::errc::ParamInvalid, loc))",
      '/* error == systemError(errc::timed_out, loc) */',
      'auto example = R"doc("error" == systemError(errc::timed_out, loc))doc";',
      "auto delay = 1'000; if (error == systemError(errc::timed_out, loc))"
   ), collapse = "\n")
   code <- maskCppNonCode(fixture)
   expect_equal(matchLines(code, errcComparison), c(1L, 2L, 4L, 5L, 12L))

   fixture <- paste(c(
      "if (error.getCode() == ENOENT) {}",
      "#ifdef _WIN32",
      "if (EACCES != e.code().value()) {}",
      "#else",
      "if (error == error_code(ESTALE, system_category())) {}",
      "#endif",
      "// if (error.getCode() == ENOENT) {}"
   ), collapse = "\n")
   expect_equal(matchLines(maskCppNonCode(fixture), errnoByValue), c(1L, 3L))

   cppFiles <- list.files(
      root,
      pattern = "[.](cpp|hpp|h)$",
      full.names = TRUE,
      recursive = TRUE
   )

   offenders <- character()
   for (cppFile in cppFiles) {

      contents <- paste(readLines(cppFile, warn = FALSE), collapse = "\n")
      code <- maskCppNonCode(contents)
      path <- sub(paste0(root, "/"), "", cppFile, fixed = TRUE)
      hits <- unlist(lapply(c(errcAsSystem, errcByValue, errnoByValue), matchLines, contents = code))

      if (length(hits))
         offenders <- c(offenders, paste0(path, ":", sort(hits)))

      # Unit tests may deliberately assert exact equality between error codes.
      if (!endsWith(cppFile, "Tests.cpp") && grepl("systemError", code, fixed = TRUE)) {
         hits <- matchLines(code, errcComparison)
         if (length(hits))
            offenders <- c(offenders, paste0(path, ":", hits, " - use Error::matches() to test a portable condition"))
      }

   }

   failureMessage <- paste(
      c("Incorrect use of error codes:", offenders),
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
