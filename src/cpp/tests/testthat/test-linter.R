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

# Whether each line of a C++ file is compiled only on Windows: every line of a
# file with Win32 in its name, else the lines in an '#ifdef _WIN32' region or
# the '#else' of an '#ifndef _WIN32'. Other conditions count as neither.
windowsOnlyLines <- function(lines, windowsOnlyFile) {

   if (windowsOnlyFile)
      return(rep(TRUE, length(lines)))

   isWindows <- "^\\s*#\\s*(ifdef\\s+_WIN32\\b|if\\s+defined\\s*\\(?\\s*_WIN32\\s*\\)?\\s*$)"
   isNotWindows <- "^\\s*#\\s*(ifndef\\s+_WIN32\\b|if\\s+!\\s*defined\\s*\\(?\\s*_WIN32\\s*\\)?\\s*$)"

   result <- logical(length(lines))
   directives <- which(grepl("^\\s*#\\s*(if|elif|else|endif)", lines, perl = TRUE))

   # one entry per open #if: "windows", "not-windows", or "unknown"
   stack <- character()
   previous <- 0L
   for (i in directives) {

      # the lines since the previous directive share its state
      if (i > previous + 1L && "windows" %in% stack)
         result[(previous + 1L):(i - 1L)] <- TRUE

      line <- lines[[i]]
      top <- length(stack)
      if (grepl(isWindows, line, perl = TRUE)) {
         stack <- c(stack, "windows")
      } else if (grepl(isNotWindows, line, perl = TRUE)) {
         stack <- c(stack, "not-windows")
      } else if (grepl("^\\s*#\\s*if", line, perl = TRUE)) {
         stack <- c(stack, "unknown")
      } else if (top > 0L && grepl("^\\s*#\\s*elif", line, perl = TRUE)) {
         stack[[top]] <- "unknown"
      } else if (top > 0L && grepl("^\\s*#\\s*else", line, perl = TRUE)) {
         stack[[top]] <- switch(stack[[top]], "windows" = "not-windows", "not-windows" = "windows", "unknown")
      } else if (top > 0L) {
         stack <- stack[-top]
      }

      previous <- i
   }

   if (length(lines) > previous && "windows" %in% stack)
      result[(previous + 1L):length(lines)] <- TRUE

   result

}

test_that("C++ sources don't treat errno values as system error codes", {

   # systemError(int) and systemCallError(name, int) build a system-category
   # code, which holds Win32 error codes on Windows, so an errno value passed
   # to them reads as an unrelated Win32 error there. for an errno constant,
   # use systemError(boost::system::errc::invalid_argument, ...), with a
   # description in place of systemCallError()'s name; for a C runtime errno,
   # save it right after the failed call (ERROR_LOCATION may be evaluated
   # first and change it), then pass it to genericError(). a POSIX-only test
   # that needs the native code can build it with system_category() from the
   # errno constant
   # the code is systemError()'s first argument, systemCallError()'s second
   codeArgument <- "(\\bsystemError\\(|\\bsystemCallError\\([^,()]*,)\\s*"
   errnoConstant <- paste0(codeArgument, "E[A-Z0-9]+\\s*[,)]")

   # R_ERRNO is the C runtime's errno on every platform
   rErrno <- paste0(codeArgument, "(R_ERRNO|MSVC_ERRNO)\\b")

   # an errc is an errno value, never a system code
   errcAsSystem <- "\\berrc::[a-z_]+\\s*,\\s*(boost::system::|std::)?system_category\\(\\)"

   # comparing a code's value with an errc ignores its category, so a Win32
   # code matches whichever errc shares its number: compare as a condition
   # (ec == errc::X, or error == systemError(errc::X, ErrorLocation()))
   errcByValue <- paste0(
      "(getCode|value)\\(\\)\\s*[!=]=\\s*(boost::system::|std::)?errc::",
      "|\\berrc::[a-z_]+\\s*[!=]=\\s*[A-Za-z_][A-Za-z0-9_.>-]*(getCode|value)\\(\\)"
   )

   # in code only Windows compiles, errno comes from the C runtime, and a
   # code's value is a Win32 code, not an errno value. (cross-platform code
   # isn't checked for these: POSIX-only files aren't marked as such, and
   # some errno values, e.g. ESTALE, have no errc to compare with)
   crtErrno <- paste0(codeArgument, "errno\\b")
   errnoByValue <- paste0(
      "(getCode|value)\\(\\)\\s*[!=]=\\s*E[A-Z0-9]+\\b",
      "|\\bE[A-Z0-9]+\\s*[!=]=\\s*[A-Za-z_][A-Za-z0-9_.>-]*(getCode|value)\\(\\)"
   )

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
   expect_true(grepl(errcByValue, "error.getCode() == boost::system::errc::permission_denied", perl = TRUE))
   expect_true(grepl(errcByValue, "ec.value() != errc::bad_file_descriptor", perl = TRUE))
   expect_true(grepl(errcByValue, "errc::timed_out == pError->getCode()", perl = TRUE))
   expect_false(grepl(errcByValue, "ec == boost::system::errc::too_many_files_open", perl = TRUE))
   expect_true(grepl(errnoByValue, "error.getCode() == ENOENT", perl = TRUE))
   expect_true(grepl(errnoByValue, "EACCES != error.getCode()", perl = TRUE))
   expect_false(grepl(errnoByValue, "error.getCode() == ERROR_ACCESS_DENIED", perl = TRUE))

   fixture <- c(
      "systemError(errno, ERROR_LOCATION);",
      "#ifdef _WIN32",
      "systemError(errno, ERROR_LOCATION);",
      "#else",
      "systemError(errno, ERROR_LOCATION);",
      "#endif",
      "#ifndef _WIN32",
      "x",
      "#elif defined(FOO)",
      "x",
      "#else",
      "x",
      "#endif",
      "#if !defined(_WIN32)",
      "x",
      "#else",
      "x",
      "#endif"
   )
   expect_identical(which(windowsOnlyLines(fixture, FALSE)), c(3L, 17L))
   expect_true(all(windowsOnlyLines(fixture, TRUE)))

   cppFiles <- list.files(
      root,
      pattern = "[.](cpp|hpp|h)$",
      full.names = TRUE,
      recursive = TRUE
   )

   offenders <- character()
   for (cppFile in cppFiles) {

      lines <- readLines(cppFile, warn = FALSE)
      contents <- paste(lines, collapse = "\n")
      path <- sub(paste0(root, "/"), "", cppFile, fixed = TRUE)

      hits <- unlist(lapply(c(errnoConstant, rErrno, errcAsSystem, errcByValue), matchLines, contents = contents))

      windowsHits <- unlist(lapply(c(crtErrno, errnoByValue), matchLines, contents = contents))
      if (length(windowsHits)) {
         windowsOnly <- windowsOnlyLines(lines, grepl("Win32", basename(cppFile), fixed = TRUE))
         hits <- c(hits, windowsHits[windowsOnly[windowsHits]])
      }

      if (length(hits))
         offenders <- c(offenders, paste0(path, ":", sort(hits)))

   }

   failureMessage <- paste(
      c("errno values treated as system error codes:", offenders),
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
