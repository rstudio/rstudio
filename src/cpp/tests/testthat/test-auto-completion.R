#
# test-auto-completion.R
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

context("auto-completion")

test_that(".rs.matchCall() respects numCommas=", {
    fun <- function(aaa, bbb, ccc){}

    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, c))), 
        quote(fun(aaa = a, bbb = b, ccc = c))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, c)), numCommas = 0), 
        quote(fun())
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, c)), numCommas = 1), 
        quote(fun(aaa = a))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, c)), numCommas = 2), 
        quote(fun(aaa = a, bbb = b))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, c)), numCommas = 3), 
        quote(fun(aaa = a, bbb = b, ccc = c))
    )

    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, aaa = c)), numCommas = 0), 
        quote(fun(aaa = c))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, aaa = c)), numCommas = 1), 
        quote(fun(aaa = c, bbb = a))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, aaa = c)), numCommas = 2), 
        quote(fun(aaa = c, bbb = a, ccc = b))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, aaa = c)), numCommas = 3), 
        quote(fun(aaa = c, bbb = a, ccc = b))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, b, aaa = c))), 
        quote(fun(aaa = c, bbb = a, ccc = b))
    )
    
    expect_identical(
        .rs.matchCall(fun, quote(fun(aaa = a, b, c)), numCommas = 0), 
        quote(fun(aaa = a))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(aaa = a, b, c)), numCommas = 1), 
        quote(fun(aaa = a))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(aaa = a, b, c)), numCommas = 2), 
        quote(fun(aaa = a, bbb = b))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(aaa = a, b, c)), numCommas = 3), 
        quote(fun(aaa = a, bbb = b, ccc = c))
    )

    expect_identical(
        .rs.matchCall(fun, quote(fun(a, bbb = d, c)), numCommas = 0), 
        quote(fun(bbb = d))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, bbb = d, c)), numCommas = 1), 
        quote(fun(aaa = a, bbb = d))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, bbb = d, c)), numCommas = 2), 
        quote(fun(aaa = a, bbb = d))
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(a, bbb = d, c)), numCommas = 3), 
        quote(fun(aaa = a, bbb = d, ccc = c))
    )
})

test_that(".rs.matchCall() removes named arguments not in the formals", {
    expect_identical(
        .rs.matchCall(fun, quote(fun(ddd = a))), 
        quote(fun())
    )
    expect_identical(
        .rs.matchCall(fun, quote(fun(ddd = a, a))), 
        quote(fun(aaa = a))
    )
})
