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
    fun <- function(aaa, bbb, ccc){}

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

test_that(".rs.getCompletionsSingleBracket() uses dimnames() for matrix-like S4 objects", {
    skip_if_not_installed("Matrix")

    e <- new.env()
    e$m <- Matrix::sparseMatrix(
        i = c(1L, 2L),
        j = c(1L, 2L),
        x = c(1, 2),
        dims = c(3L, 4L)
    )
    colnames(e$m) <- c("c1", "c2", "c3", "c4")

    # m[, |  -- numCommas = 1 -> column names from dimnames()
    result <- .rs.getCompletionsSingleBracket(
        token = "",
        string = "m",
        functionCall = quote(m[]),
        numCommas = 1,
        index = 1L,
        envir = e
    )
    expect_equal(result$results, c("c1", "c2", "c3", "c4"))
})

test_that(".rs.getCompletionsSingleBracket() handles sparse matrices with no dimnames", {
    skip_if_not_installed("Matrix")

    e <- new.env()
    e$m <- Matrix::sparseMatrix(
        i = c(1L, 2L),
        j = c(1L, 2L),
        x = c(1, 2),
        dims = c(3L, 4L)
    )

    # Should return cleanly (not hang or error) with no completions when
    # no dimnames are set.
    result <- .rs.getCompletionsSingleBracket(
        token = "",
        string = "m",
        functionCall = quote(m[]),
        numCommas = 1,
        index = 1L,
        envir = e
    )
    expect_equal(length(result$results), 0L)
})

test_that(".rs.getCompletionsSingleBracket() does not offer auto-rownames for data.frames", {
    e <- new.env()
    e$df <- data.frame(x = 1:3, y = 4:6)

    # df[|  -- numCommas = 0; auto-generated rownames "1", "2", "3" must
    # NOT be offered as completions.
    result <- .rs.getCompletionsSingleBracket(
        token = "",
        string = "df",
        functionCall = quote(df[]),
        numCommas = 0,
        index = 1L,
        envir = e
    )
    expect_false(any(c("1", "2", "3") %in% result$results))
})

test_that(".rs.getCompletionsSingleBracket() offers explicit rownames for data.frames", {
    e <- new.env()
    e$mt <- mtcars

    result <- .rs.getCompletionsSingleBracket(
        token = "Mazda",
        string = "mt",
        functionCall = quote(mt[]),
        numCommas = 0,
        index = 1L,
        envir = e
    )
    expect_true("Mazda RX4" %in% result$results)
})

test_that(".rs.getCompletionsSingleBracket() offers colnames for data.frames after a comma", {
    e <- new.env()
    e$df <- data.frame(apple = 1:3, banana = 4:6)

    # df[, |
    result <- .rs.getCompletionsSingleBracket(
        token = "",
        string = "df",
        functionCall = quote(df[]),
        numCommas = 1,
        index = 1L,
        envir = e
    )
    expect_setequal(result$results, c("apple", "banana"))
})
