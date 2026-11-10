#
# test-cpp.R
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

context("cpp")

test_that("cppSourceFile() uses correct package", {
    tf <- tempfile(fileext = ".cpp")
    
    writeLines('#include <Rcpp.h>\n//[[Rcpp::export]]\nvoid fun(){}', tf)
    call <- .rs.cppSourceFile(tf)
    expect_match(call, "Rcpp::sourceCpp")

    writeLines('void fun(){}', tf)
    call <- .rs.cppSourceFile(tf)
    expect_match(call, "Rcpp::sourceCpp")

    writeLines('#include <cpp11.hpp>\n[[cpp11::register]]\nvoid fun(){}', tf)
    call <- .rs.cppSourceFile(tf)
    expect_match(call, "cpp11::cpp_source")
    
    unlink(tf)
    call <- .rs.cppSourceFile(tf)
    expect_match(call, "Rcpp::sourceCpp")
})

test_that("diagnostics continue past clang's default error limit", {
    skip_if_not_installed("Rcpp")
    skip_if_not(.rs.isClangAvailable())

    # precompiled headers are slow to build and irrelevant here
    op <- options(rstudio.libclang.usePrecompiledHeaders = FALSE)
    on.exit(options(op), add = TRUE)

    dir <- tempfile("cpp-diagnostics-")
    dir.create(dir)
    on.exit(unlink(dir, recursive = TRUE), add = TRUE)

    # 25 errors, well past clang's default limit of 19
    path <- file.path(dir, "errors.cpp")
    writeLines(c(
        "#include <Rcpp.h>",
        "void f() {",
        sprintf("   undeclared_%d = %d;", 1:25, 1:25),
        "}"
    ), path)

    # severity 3 is an error; clang's "too many errors" stop is a fatal (4)
    diagnostics <- function() .rs.invokeRpc("get_cpp_diagnostics", path)
    countErrors <- function() {
        sum(vapply(diagnostics(), function(d) d$severity == 3, logical(1)))
    }

    expect_equal(countErrors(), 25L)

    # flags from compile_flags.txt are appended, so they can override ours
    writeLines("-ferror-limit=5", file.path(dir, "compile_flags.txt"))
    expect_equal(countErrors(), 5L)

    # and clang's explanation for stopping, which has no location of its own,
    # is reported at the top of the file rather than dropped
    messages <- vapply(diagnostics(), `[[`, character(1), "message")
    expect_match(messages, "too many errors emitted", all = FALSE)
})

test_that("compile_flags.txt paths resolve against its directory", {
    skip_if_not_installed("Rcpp")
    skip_if_not(.rs.isClangAvailable())

    op <- options(rstudio.libclang.usePrecompiledHeaders = FALSE)
    on.exit(options(op), add = TRUE)

    dir <- tempfile("cpp-flags-")
    dir.create(file.path(dir, "inc"), recursive = TRUE)
    on.exit(unlink(dir, recursive = TRUE), add = TRUE)

    writeLines("#define ANSWER 42", file.path(dir, "inc", "answer.h"))
    path <- file.path(dir, "answer.cpp")
    writeLines(c(
        "#include <Rcpp.h>",
        "#include <answer.h>",
        "int f() { return ANSWER; }"
    ), path)

    messages <- function() {
        diagnostics <- .rs.invokeRpc("get_cpp_diagnostics", path)
        vapply(diagnostics, `[[`, character(1), "message")
    }

    expect_match(messages(), "'answer.h' file not found", all = FALSE)

    # a relative -I is relative to the flags file, not the session's working
    # directory (as in clangd); blank lines and CRLF line endings are tolerated
    writeLines(c("", "-Iinc", ""), file.path(dir, "compile_flags.txt"), sep = "\r\n")
    expect_length(messages(), 0L)
})

test_that("files are parsed with the default precompiled header", {
    skip_if_not_installed("Rcpp")
    skip_if_not(.rs.isClangAvailable())

    op <- options(rstudio.libclang.usePrecompiledHeaders = TRUE)
    on.exit(options(op), add = TRUE)

    dir <- tempfile("cpp-pch-")
    dir.create(dir)
    on.exit(unlink(dir, recursive = TRUE), add = TRUE)

    # a file libclang fails to parse reports no diagnostics at all, so ask
    # for one. R's default standard is C++20 as of R 4.6, where libclang
    # refuses a precompiled header built with other preprocessor options
    # than the file including it (#19559)
    path <- file.path(dir, "parsed.cpp")
    writeLines(c(
        "#include <Rcpp.h>",
        "static_assert(false, \"file was parsed\");"
    ), path)

    diagnostics <- .rs.invokeRpc("get_cpp_diagnostics", path)
    messages <- vapply(diagnostics, `[[`, character(1), "message")
    expect_match(messages, "file was parsed", fixed = TRUE, all = FALSE)
})

test_that("compile_flags.txt flags are honored with precompiled headers", {
    skip_if_not_installed("Rcpp")
    skip_if_not(.rs.isClangAvailable())

    # R's default C++ standard has been C++14 or newer since R 4.1
    skip_if(getRversion() < "4.1.0")

    op <- options(rstudio.libclang.usePrecompiledHeaders = TRUE)
    on.exit(options(op), add = TRUE)

    dir <- tempfile("cpp-pch-flags-")
    dir.create(dir)
    on.exit(unlink(dir, recursive = TRUE), add = TRUE)

    path <- file.path(dir, "standard.cpp")
    writeLines(c(
        "#include <Rcpp.h>",
        "static_assert(__cplusplus >= 201402L, \"needs C++14\");"
    ), path)

    messages <- function() {
        diagnostics <- .rs.invokeRpc("get_cpp_diagnostics", path)
        vapply(diagnostics, `[[`, character(1), "message")
    }

    # parsed against the default Rcpp precompiled header
    expect_length(messages(), 0L)

    # a different standard gets its own precompiled header, built with the
    # flag, and the file is parsed with both
    flagsFile <- file.path(dir, "compile_flags.txt")
    writeLines("-std=c++11", flagsFile)
    expect_match(messages(), "needs C++14", fixed = TRUE, all = FALSE)

    # removing the override goes back to the default one
    unlink(flagsFile)
    expect_length(messages(), 0L)
})

test_that("compile_flags.txt include directories feed header completion", {
    skip_if_not_installed("Rcpp")
    skip_if_not(.rs.isClangAvailable())

    op <- options(rstudio.libclang.usePrecompiledHeaders = FALSE)
    on.exit(options(op), add = TRUE)

    dir <- tempfile("cpp-header-completion-")
    dir.create(file.path(dir, "inc"), recursive = TRUE)
    on.exit(unlink(dir, recursive = TRUE), add = TRUE)
    file.create(file.path(dir, "inc", "widget.h"))

    path <- file.path(dir, "main.cpp")
    writeLines(c("#include <Rcpp.h>", "#include <wid"), path)
    flagsFile <- file.path(dir, "compile_flags.txt")

    completions <- function(line) {
        result <- .rs.invokeRpc("get_cpp_completions", line, path, "", 1L, nchar(line), "wid")
        vapply(result$completions, `[[`, character(1), "typed_text")
    }

    expect_false("widget.h" %in% completions("#include <wid"))

    # an include directory added by any of clang's spellings, not just -I,
    # is searched for header completions
    writeLines("--include-directory-after=inc", flagsFile)
    expect_true("widget.h" %in% completions("#include <wid"))

    # but one clang only searches for "quoted" includes is offered for those alone
    writeLines("-iquoteinc", flagsFile)
    expect_false("widget.h" %in% completions("#include <wid"))
    expect_true("widget.h" %in% completions('#include "wid'))
})

test_that("problems with compile_flags.txt are reported, not swallowed", {
    skip_if_not_installed("Rcpp")
    skip_if_not(.rs.isClangAvailable())

    op <- options(rstudio.libclang.usePrecompiledHeaders = FALSE)
    on.exit(options(op), add = TRUE)

    lint <- function(flags) {
        dir <- tempfile("cpp-bad-flags-")
        dir.create(dir)
        on.exit(unlink(dir, recursive = TRUE), add = TRUE)

        path <- file.path(dir, "ok.cpp")
        writeLines(c("#include <Rcpp.h>", "int f() { return 1; }"), path)
        writeLines(flags, file.path(dir, "compile_flags.txt"))
        .rs.invokeRpc("get_cpp_diagnostics", path)
    }

    # clang reports a bad flag against the command line, with no location;
    # we anchor those to the top of the file
    diagnostics <- lint("-Wno-unused-paramter")
    expect_length(diagnostics, 1L)
    expect_match(diagnostics[[1]]$message, "unknown warning option")
    expect_equal(diagnostics[[1]]$position$line, 1)

    diagnostics <- lint("--bogus-flag")
    expect_length(diagnostics, 1L)
    expect_match(diagnostics[[1]]$message, "unknown argument")

    # a bad value is quoted apart from its option, which the parse still survives
    diagnostics <- lint("-fsanitize=bogus")
    expect_length(diagnostics, 1L)
    expect_match(diagnostics[[1]]$message, "unsupported argument 'bogus'")
    expect_equal(diagnostics[[1]]$position$line, 1)

    # an invalid value makes libclang fail to parse the file at all
    diagnostics <- lint("-std=c++99")
    expect_length(diagnostics, 1L)
    expect_match(diagnostics[[1]]$message, "check the flags in .*compile_flags.txt")
    expect_equal(diagnostics[[1]]$severity, 3)
})
