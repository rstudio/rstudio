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

test_that(".rs.makeCompletions() handle optional $suggestOnAccept", {
    expect_null(
        .rs.makeCompletions("", c("a", "b"))$suggestOnAccept
    )
    expect_equal(
        .rs.makeCompletions("", c("a", "b"), suggestOnAccept = logical())$suggestOnAccept, 
        c(FALSE, FALSE)
    )
    expect_equal(
        .rs.makeCompletions("", c("a", "b"), suggestOnAccept = TRUE)$suggestOnAccept, 
        c(TRUE, TRUE)
    )
    expect_equal(
        .rs.makeCompletions("", c("a", "b"), suggestOnAccept = c(TRUE, FALSE))$suggestOnAccept, 
        c(TRUE, FALSE)
    )
})

test_that(".rs.appendCompletions() handle optional $suggestOnAccept", {
    expect_null(
        .rs.appendCompletions(
            .rs.makeCompletions("", c("a", "b")), 
            .rs.makeCompletions("", c("c", "d", "e"))
        )$suggestOnAccept
    )

    expect_equal(
        .rs.appendCompletions(
            .rs.makeCompletions("", c("a", "b"), suggestOnAccept = c(TRUE, FALSE)), 
            .rs.makeCompletions("", c("c", "d", "e"))
        )$suggestOnAccept, 
        c(TRUE, FALSE, FALSE, FALSE, FALSE)
    )

    expect_equal(
        .rs.appendCompletions(
            .rs.makeCompletions("", c("a", "b")), 
            .rs.makeCompletions("", c("c", "d", "e"), suggestOnAccept = c(TRUE, FALSE, TRUE))
        )$suggestOnAccept, 
        c(FALSE, FALSE, TRUE, FALSE, TRUE)
    )

    expect_equal(
        .rs.appendCompletions(
            .rs.makeCompletions("", c("a", "b")     , suggestOnAccept = c(FALSE, TRUE)), 
            .rs.makeCompletions("", c("c", "d", "e"), suggestOnAccept = c(TRUE, FALSE, TRUE))
        )$suggestOnAccept, 
        c(FALSE, TRUE, TRUE, FALSE, TRUE)
    )
})

test_that(".rs.subsetCompletions() handle optional $suggestOnAccept", {
    expect_equal(
        .rs.subsetCompletions(
            .rs.makeCompletions("", c("c", "d", "e"), suggestOnAccept = c(TRUE, FALSE, TRUE)), 
            1
        )$suggestOnAccept, 
        TRUE
    )

    expect_equal(
        .rs.subsetCompletions(
            .rs.makeCompletions("", c("c", "d", "e"), suggestOnAccept = c(TRUE, FALSE, TRUE)), 
            c(TRUE, TRUE, FALSE)
        )$suggestOnAccept, 
        c(TRUE, FALSE)
    )
    
    expect_null(
        .rs.subsetCompletions(
            .rs.makeCompletions("", c("c", "d", "e")), 
            1
        )$suggestOnAccept
    )
})
