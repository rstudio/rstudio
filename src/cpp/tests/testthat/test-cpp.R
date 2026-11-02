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

    countErrors <- function() {
        diagnostics <- .rs.invokeRpc("get_cpp_diagnostics", path)
        sum(vapply(diagnostics, function(d) d$severity >= 3, logical(1)))
    }

    expect_equal(countErrors(), 25L)

    # flags from compile_flags.txt are appended, so they can override ours
    writeLines("-ferror-limit=5", file.path(dir, "compile_flags.txt"))
    expect_equal(countErrors(), 5L)
})
