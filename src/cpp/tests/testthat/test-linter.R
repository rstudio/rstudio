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

test_that("C++ sources do not put errc values in the system category", {

   # an errc value is a portable errno condition, so it belongs in the generic
   # category; on Windows the system category reads it as an unrelated Win32
   # code (errc::io_error, 5, as "Access is denied"). systemError(errc) picks
   # the generic category, but an error_code built by hand bypasses it
   pattern <- "errc::[A-Za-z_]+\\s*,\\s*((boost|std)::system::|std::)?system_category\\(\\)"

   # the offending fixtures are assembled so this file passes its own lint
   category <- paste0("system", "_category()")
   offending <- c(
      paste0("error_code(errc::io_error, ", category, ")"),
      paste0("error_code(boost::system::errc::io_error,\n   boost::system::", category, ")")
   )
   expect_true(all(grepl(pattern, offending, useBytes = TRUE)))

   allowed <- c(
      "error_code(errc::io_error, generic_category())",
      paste0("error_code(ERROR_ACCESS_DENIED, ", category, ")")
   )
   expect_false(any(grepl(pattern, allowed, useBytes = TRUE)))

   cppFiles <- list.files(
      root,
      pattern = "[.](cpp|hpp|mm)$",
      full.names = TRUE,
      recursive = TRUE
   )

   # sources are UTF-8 and the pattern is ASCII, so match bytes rather than
   # characters in the session's native encoding (which may not be UTF-8)
   offenders <- character()
   for (cppFile in cppFiles) {
      contents <- paste(readLines(cppFile, warn = FALSE), collapse = "\n")
      if (grepl(pattern, contents, useBytes = TRUE))
         offenders <- c(offenders, sub(paste0(root, "/"), "", cppFile, fixed = TRUE))
   }

   failureMessage <- paste(
      c("errc values built in the system category:", offenders),
      collapse = "\n"
   )

   expect(length(offenders) == 0, failureMessage)

})

test_that("C++ sources test error conditions with matches(), not equality", {

   # an error built from an errc value is in the generic category, while Boost
   # reports the same errno condition in the system category (and on Windows a
   # Win32 code for it), so comparing the two with == is quietly false. likewise
   # comparing getCode() or value() with an errc value ignores the category, so
   # on Windows it compares a Win32 code with an errno number. use
   # error.matches(errc::X), or Boost's ec == errc::X for a raw error_code
   #
   # the error constructors are matched with up to one level of nested
   # parentheses, e.g. systemError(errc::timed_out, ErrorLocation())
   call <- "\\((?:[^;()]|\\([^;()]*\\))*\\)"
   factory <- "(?:rstudio::)?(?:core::)?(?:systemError|Error)\\s*"
   accessor <- "(?:getCode|value)\\s*\\(\\s*\\)"
   errc <- "(?:(?:boost::system::|std::)?errc::)"
   patterns <- c(
      paste0("[!=]=\\s*", factory, "\\(\\s*", errc),
      paste0("[!=]=\\s*(?:rstudio::)?(?:core::)?systemError\\s*\\("),
      paste0("\\bsystemError\\s*", call, "\\s*[!=]="),
      paste0(accessor, "\\s*[!=]=\\s*", errc),
      paste0(errc, "\\w+\\s*[!=]=\\s*[\\w.>()-]*", accessor)
   )
   pattern <- paste(patterns, collapse = "|")

   offending <- c(
      "error == systemError(errc::timed_out, ErrorLocation())",
      "error != core::systemError(\n   boost::system::errc::timed_out, ERROR_LOCATION)",
      "error == systemError(code, ERROR_LOCATION)",
      "error == Error(errc::no_such_file_or_directory, ERROR_LOCATION)",
      "systemError(errc::timed_out, ErrorLocation()) == error",
      "error.getCode() == boost::system::errc::permission_denied",
      "ec.value() != errc::too_many_files_open",
      "errc::io_error == error.getCode()"
   )
   expect_true(all(grepl(pattern, offending, perl = TRUE)))

   allowed <- c(
      "error.matches(errc::timed_out)",
      "ec == boost::system::errc::timed_out",
      "ec == boost::asio::error::operation_aborted",
      "error.getCode() == ERROR_ACCESS_DENIED",
      "EXPECT_EQ(error, systemError(errc::io_error, ERROR_LOCATION))",
      "Error error = systemError(errc::timed_out, ERROR_LOCATION);"
   )
   expect_false(any(grepl(pattern, allowed, perl = TRUE)))

   cppFiles <- list.files(
      root,
      pattern = "[.](cpp|hpp|mm)$",
      full.names = TRUE,
      recursive = TRUE
   )

   offenders <- character()
   for (cppFile in cppFiles) {
      contents <- paste(readLines(cppFile, warn = FALSE), collapse = "\n")
      starts <- gregexpr(pattern, contents, perl = TRUE, useBytes = TRUE)[[1L]]
      if (starts[[1L]] == -1L)
         next

      # report the line each match starts on
      newlines <- gregexpr("\n", contents, fixed = TRUE, useBytes = TRUE)[[1L]]
      lines <- findInterval(starts, newlines[newlines > 0L]) + 1L
      path <- sub(paste0(root, "/"), "", cppFile, fixed = TRUE)
      offenders <- c(offenders, paste0(path, ":", lines))
   }

   failureMessage <- paste(
      c("error conditions compared with == instead of matches():", offenders),
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
