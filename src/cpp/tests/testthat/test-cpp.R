#
# test-cpp.R
#
# Copyright (C) 2022 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
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

test_that("cppProjectStyle() correctly guesses cpp flavor", {
    path <- tempfile()
    on.exit(unlink(path, recursive = TRUE))

    dir.create(path)
    writeLines("LinkingTo: Rcpp", file.path(path, "DESCRIPTION"))
    expect_equal(.rs.cppProjectStyle(path), "Rcpp")

    writeLines("LinkingTo: cpp11", file.path(path, "DESCRIPTION"))
    expect_equal(.rs.cppProjectStyle(path), "cpp11")

    writeLines("Type: Package", file.path(path, "DESCRIPTION"))
    expect_equal(.rs.cppProjectStyle(path), "")

    writeLines("Type/Package", file.path(path, "DESCRIPTION"))
    expect_equal(.rs.cppProjectStyle(path), "")

    dir.create(file.path(path, "inst", "include"), recursive = TRUE)
    writeLines("", file.path(path, "inst", "include", "cpp11.hpp"))
    expect_equal(.rs.cppProjectStyle(path), "cpp11")
})
