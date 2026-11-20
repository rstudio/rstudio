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

test_that("C++ sources don't pass errno values as system error codes", {

   # systemError(int) and systemCallError(name, int) build a system-category
   # code, which holds Win32 error codes on Windows, so an errno value passed
   # to them reads as an unrelated Win32 error there. for an errno constant,
   # use systemError(boost::system::errc::invalid_argument, ...), with a
   # description in place of systemCallError()'s name; for a C runtime errno,
   # use systemError(std::error_code(errno, std::generic_category()), ...).
   # a POSIX-only test that needs the native code can build it with
   # system_category() from the errno constant
   # the code is systemError()'s first argument, systemCallError()'s second
   codeArgument <- "(\\bsystemError\\(|\\bsystemCallError\\([^,()]*,)\\s*"
   errnoConstant <- paste0(codeArgument, "E[A-Z0-9]+\\s*[,)]")

   # R_ERRNO is the C runtime's errno on every platform
   rErrno <- paste0(codeArgument, "(R_ERRNO|MSVC_ERRNO)\\b")

   # in Windows-only files (Win32 in the name), errno comes from the C runtime
   crtErrno <- paste0(codeArgument, "errno\\b")

   # an errc is an errno value, never a system code
   errcAsSystem <- "\\berrc::[a-z_]+\\s*,\\s*(boost::system::|std::)?system_category\\(\\)"

   offending <- c(
      "systemError(EINVAL, ERROR_LOCATION)",
      "systemError(\n   EIO,\n   \"description\", ERROR_LOCATION)",
      "systemCallError(\"open\", ENOENT, ERROR_LOCATION)",
      "systemCallError(operation, ENOENT, ERROR_LOCATION)"
   )
   expect_true(all(grepl(errnoConstant, offending, perl = TRUE)))

   allowed <- c(
      "systemError(errno, ERROR_LOCATION)",
      "systemCallError(\"open\", errno, ERROR_LOCATION)",
      "systemError(ERROR_ACCESS_DENIED, ERROR_LOCATION)",
      "systemError(boost::system::errc::io_error, ERROR_LOCATION)"
   )
   expect_false(any(grepl(errnoConstant, allowed, perl = TRUE)))

   expect_true(grepl(rErrno, "systemError(R_ERRNO, ERROR_LOCATION)", perl = TRUE))
   expect_true(grepl(crtErrno, "systemCallError(\"_popen\", errno, ERROR_LOCATION)", perl = TRUE))
   expect_true(grepl(errcAsSystem, "error_code(errc::io_error,\n   boost::system::system_category())", perl = TRUE))
   expect_false(grepl(errcAsSystem, "error_code(errc::io_error, generic_category())", perl = TRUE))

   cppFiles <- list.files(
      root,
      pattern = "[.](cpp|hpp|h)$",
      full.names = TRUE,
      recursive = TRUE
   )

   offenders <- character()
   for (cppFile in cppFiles) {

      contents <- paste(readLines(cppFile, warn = FALSE), collapse = "\n")
      patterns <- c(errnoConstant, rErrno, errcAsSystem)
      if (grepl("Win32", basename(cppFile), fixed = TRUE))
         patterns <- c(patterns, crtErrno)

      for (pattern in patterns) {
         starts <- gregexpr(pattern, contents, perl = TRUE)[[1L]]
         if (starts[[1L]] == -1L)
            next

         # line numbers of the matches: one more than the newlines before them
         newlines <- gregexpr("\n", contents, fixed = TRUE)[[1L]]
         lines <- findInterval(starts, newlines[newlines > 0L]) + 1L
         path <- sub(paste0(root, "/"), "", cppFile, fixed = TRUE)
         offenders <- c(offenders, paste0(path, ":", lines))
      }

   }

   failureMessage <- paste(
      c("errno values passed as system error codes:", offenders),
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
