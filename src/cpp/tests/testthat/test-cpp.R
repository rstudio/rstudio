#
# test-cpp.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
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
