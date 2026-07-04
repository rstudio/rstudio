#
# test-object-explorer.R
#
# Copyright (C) 2026 by Posit Software, PBC
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

context("object explorer")

# Evaluate a child's access string the way explorer_inspect_object does when
# the client expands that child: '#' is replaced with the parent object and
# the resulting code is evaluated.
inspectionAccessEval <- function(object, access)
{
   extractingCode <- sub("#", "`__OBJECT__`", access, fixed = TRUE)
   envir <- new.env(parent = globalenv())
   envir[["__OBJECT__"]] <- object
   eval(parse(text = extractingCode), envir = envir)
}

test_that("list elements with duplicated names are accessed by index (#17937)", {
   object <- list(
      list(Id = 1, Age = 12),
      list(Id = 2, Age = 22),
      list(Id = 3, Age = 33)
   )
   names(object) <- rep("Passenger", 3)

   context <- .rs.explorer.createContext(recursive = 1)
   result <- .rs.explorer.inspectObject(object, context)
   children <- result$children
   expect_length(children, 3)

   # the duplicated name is still displayed
   names <- vapply(children, function(child) child$display$name, "")
   expect_equal(names, rep("Passenger", 3))

   # but access goes by index, as access-by-name would always
   # retrieve the first matching element
   access <- vapply(children, function(child) child$access, "")
   expect_equal(access, c("#[[1]]", "#[[2]]", "#[[3]]"))

   # expanding each child retrieves its own value
   for (i in seq_along(children))
      expect_equal(inspectionAccessEval(object, access[[i]]), object[[i]])
})

test_that("uniquely named list elements are still accessed by name", {
   object <- list(a = 1, b = 2)

   context <- .rs.explorer.createContext(recursive = 1)
   result <- .rs.explorer.inspectObject(object, context)
   children <- result$children

   access <- vapply(children, function(child) child$access, "")
   expect_equal(access, c("#[[\"a\"]]", "#[[\"b\"]]"))
})

test_that("unnamed and empty-named list elements are accessed by index", {
   object <- list(1, b = 2, 3)
   names(object)[[3]] <- ""

   context <- .rs.explorer.createContext(recursive = 1)
   result <- .rs.explorer.inspectObject(object, context)
   children <- result$children

   access <- vapply(children, function(child) child$access, "")
   expect_equal(access, c("#[[1]]", "#[[\"b\"]]", "#[[3]]"))
})

test_that("environment-like S4 objects are described without warnings", {
   setClass("TestS4Env", contains = "environment", where = environment())
   object <- new("TestS4Env")
   expect_no_warning(desc <- .rs.explorer.objectDesc(object))
   expect_equal(desc, "S4 object of class TestS4Env")

   generator <- setRefClass("TestRC", fields = list(x = "numeric"), where = environment())
   object <- generator$new(x = 1)
   expect_no_warning(desc <- .rs.explorer.objectDesc(object))
   expect_equal(desc, "Reference class object of class TestRC")

   # describing the object should not strip its S4 bit
   expect_true(isS4(object))
})

test_that("environments are described without mutating their class", {
   object <- new.env()
   class(object) <- c("foo", "bar")
   desc <- .rs.explorer.objectDesc(object)
   expect_match(desc, "^<environment:")
   expect_equal(class(object), c("foo", "bar"))
})

test_that("atomic vectors with duplicated names are accessed by index (#17937)", {
   object <- c(a = 1, a = 2, b = 3)

   context <- .rs.explorer.createContext(recursive = 1)
   result <- .rs.explorer.inspectObject(object, context)
   children <- result$children
   expect_length(children, 3)

   access <- vapply(children, function(child) child$access, "")
   expect_equal(access, c("#[[1]]", "#[[2]]", "#[[\"b\"]]"))

   for (i in seq_along(children))
      expect_equal(inspectionAccessEval(object, access[[i]]), object[[i]])
})

test_that("objects with non-scalar length() methods can be inspected (#18138)", {
   # mimic Formula::length.Formula, which returns a length-2 integer
   # (one count each for the left-hand and right-hand sides); register
   # the method the same way a package NAMESPACE would
   registerS3method("length", "rs_test_formula", function(x) c(1L, 1L))
   on.exit({
      table <- get(".__S3MethodsTable__.", envir = asNamespace("base"))
      rm(list = "length.rs_test_formula", envir = table)
   }, add = TRUE)

   formula <- structure(y ~ x, class = c("rs_test_formula", "formula"))
   expect_identical(length(formula), c(1L, 1L))

   # inspecting the object directly succeeds, and reports the
   # internal length rather than the dispatched one
   context <- .rs.explorer.createContext(recursive = 1)
   result <- .rs.explorer.inspectObject(formula, context)
   expect_equal(as.integer(result$length), 3L)
   expect_true(as.logical(result$expandable))
   expect_length(result$children, 3)

   # inspecting a model-like object containing it succeeds as well;
   # this is the path exercised by View() on an mlogit model
   model <- list(coefficients = c(a = 1, b = 2), formula = formula)
   result <- .rs.explorer.inspectObject(model, context)
   expect_length(result$children, 2)
})
