#
# test-environment.R
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

context("environment")

test_that("environment object listings are correct", {
   # temporarily disable showing the .Last.value so we don't have to account for it in test results
   lastValue <- .rs.api.readRStudioPreference("show_last_dot_value")
   on.exit(.rs.api.writeRStudioPreference("show_last_dot_value", lastValue))
   .rs.api.writeRStudioPreference("show_last_dot_value", FALSE)

   # our test-runner sets runAllTests function in the global environment, so initial
   # length is one
   .rs.invokeRpc("set_environment", "R_GlobalEnv")
   expect_equal(length(.rs.invokeRpc("list_environment")), 1)

   # add some items to the global environment
   assign("obj1", 1, envir = globalenv())
   assign("obj2", "two", envir = globalenv())
   assign("obj3", list(1, 2, 3), envir = globalenv())

   # active binding
   makeActiveBinding("obj4", env = globalenv(), local({
      count <- 0
      function(){
         count <<- count + 1
         count
      }
   }))

   # promise
   delayedAssign("obj5", local({
      assign("obj6", 6, globalenv())
      5
   }), globalenv(), globalenv())

   # list the global environment
   .rs.invokeRpc("set_environment", "R_GlobalEnv")
   contents <- .rs.invokeRpc("list_environment")

   # verify contents (newly added plus the initial runAllTests function)
   expect_equal(length(contents), 6)
   obj1 <- contents[[1]]
   expect_equal(obj1[["name"]], "obj1")
   expect_equal(obj1[["value"]], "1")
   obj2 <- contents[[2]]
   expect_equal(obj2[["name"]], "obj2")
   expect_equal(obj2[["value"]], "\"two\"")
   obj3 <- contents[[3]]
   expect_equal(obj3[["name"]], "obj3")
   expect_equal(obj3[["length"]], 3)
   obj4 <- contents[[4]]
   expect_equal(obj4[["name"]], "obj4")
   expect_equal(obj4[["type"]], "active binding")
   expect_equal(obj4[["value"]], "<Active binding>")
   obj5 <- contents[[5]]
   expect_equal(obj5[["name"]], "obj5")
   expect_equal(obj5[["type"]], "promise")
   expect_equal(obj5[["value"]], "local({ <...>")

   # check that active binding wasn't forced yet
   expect_equal(get("obj4", globalenv()), 1)

   # check that the promise wasn't evaluated yet
   expect_true(!exists("obj6", globalenv()))
})

test_that("flag must be specified when removing objects", {
   expect_error(.rs.invokeRpc("remove_all_objects"))
})

test_that("all objects are removed when requested", {

   # temporarily disable showing the .Last.value so we don't have to account for it in test results
   lastValue <- .rs.api.readRStudioPreference("show_last_dot_value")
   on.exit(.rs.api.writeRStudioPreference("show_last_dot_value", lastValue))
   .rs.api.writeRStudioPreference("show_last_dot_value", FALSE)

   contents <- .rs.invokeRpc("list_environment")
   expect_true(length(contents) > 0)

   .rs.invokeRpc("remove_all_objects", TRUE)

   contents <- .rs.invokeRpc("list_environment")
   expect_equal(length(contents), 0)
})

test_that("functions with backslashes deparse correctly", {
   # character vector with code for a simple function
   code <- "function() { \"first line\\nsecond line\" }"

   # parse and evaluate the expression (yielding a function f)
   eval(parse(text = paste0("f <- ", code)))

   # immediately deparse f back into a string
   output <- .rs.deparseFunction(f, TRUE, TRUE)

   expect_equal(output, code)
})

test_that("memory usage stats are reasonable", {
   report <- .rs.invokeRpc("get_memory_usage_report")

   # ensure that we get values from R
   expect_true(report$r$cons > 0)
   expect_true(report$r$vector > 0)

   # ensure that the memory used by R is less than the memory used by the process
   r_total <- report$r$cons + report$r$vector
   expect_true(report$system$process$kb > r_total)
})

test_that("missing arguments can be described", {
   
   # simulate a missing value argument
   delayedAssign("x", quote(expr = ))
   desc <- .rs.describeObject(environment(), "x")
   expect_identical(desc$name, .rs.scalar("x"))
   
})

test_that("hasExternalPointer finds external pointers", {
   xp <- .Call("rs_newTestExternalPointer", FALSE, PACKAGE = "(embedding)")
   nullxp <- .Call("rs_newTestExternalPointer", TRUE, PACKAGE = "(embedding)")

   # NULL
   expect_true(!.rs.hasExternalPointer(NULL))

   # external pointers
   expect_true(.rs.hasExternalPointer(xp))
   expect_true(!.rs.hasExternalPointer(xp, nullPtr = TRUE))
   expect_true(.rs.hasExternalPointer(nullxp, TRUE))
   expect_true(!.rs.hasExternalPointer(nullxp, nullPtr = FALSE))

   # lists
   v <- list(1, "a", xp, nullxp)
   expect_true(.rs.hasExternalPointer(v))
   expect_true(.rs.hasExternalPointer(v, nullPtr = TRUE))

   # attributes
   x <- structure(1, foo = xp)
   # expect_true(.rs.hasExternalPointer(x))
   
   x <- structure(1, bar = v)
   # expect_true(.rs.hasExternalPointer(x))

   x <- structure(1, foo = nullxp)
   # expect_true(.rs.hasExternalPointer(x, nullPtr = TRUE))

   x <- structure(1, bar = nullxp)
   # expect_true(.rs.hasExternalPointer(x, nullPtr = TRUE))

   # hashed environment
   h <- new.env(hash = TRUE, parent = emptyenv())
   h$a <- 1
   h$b <- xp
   expect_true(.rs.hasExternalPointer(h))

   # non hashed environment
   h <- new.env(hash = FALSE, parent = emptyenv())
   h$a <- 1
   h$b <- nullxp
   expect_true(.rs.hasExternalPointer(h, nullPtr = TRUE))

   # pairlist
   expect_true(.rs.hasExternalPointer(pairlist(a = 1, b = xp)))
   expect_true(.rs.hasExternalPointer(pairlist(a = 1, b = nullxp), nullPtr = TRUE))

   # calls
   expect_true(.rs.hasExternalPointer(call("foo", a = 1, b = xp)))
   expect_true(.rs.hasExternalPointer(call("bar", a = 1, b = nullxp), nullPtr = TRUE))

   # active bindings are not triggered
   count <- 0
   h <- new.env(hash = TRUE, parent = emptyenv())
   makeActiveBinding("b", function() {
      count <<- count + 1
      42
   }, h)
   expect_true(!.rs.hasExternalPointer(h))
   expect_true(count == 0)

   # promises are not forced
   local({
      h <- new.env(hash = TRUE, parent = emptyenv())
      delayedAssign("p", .Call("rs_newTestExternalPointer", FALSE, PACKAGE = "(embedding)"), assign.env = h)
      expect_true(count == 0)
      expect_true(!.rs.hasExternalPointer(h))
      
      # ... but if they are, the value is checked
      force(h$p)
      expect_true(.rs.hasExternalPointer(h))
   })
   
   # S4 
   Env <- setClass("Env", contains = "environment")
   hEnv <- Env(
      new.env(hash = TRUE, parent = emptyenv())
   )
   hEnv$a <- xp
   expect_true(.rs.hasExternalPointer(hEnv))

   hEnv <- Env(
      new.env(hash = FALSE, parent = emptyenv())
   )
   hEnv$a <- nullxp
   expect_true(.rs.hasExternalPointer(hEnv, nullPtr = TRUE))
})

test_that(".rs.hasExternalPointer() finds xp in functions", {
   
   expect_false(.rs.hasExternalPointer(function(x = 2) x))
   
   # formals
   expect_true(
      .rs.hasExternalPointer(
         local({
            `formals<-`(function(x = 42) x, value = pairlist(x = .Call("rs_newTestExternalPointer", FALSE, PACKAGE = "(embedding)")))
         })
      )
   )

   # body
   expect_true(
      .rs.hasExternalPointer(
         substitute(function() x, list(x = .Call("rs_newTestExternalPointer", FALSE, PACKAGE = "(embedding)")))
      )
   )

   # environment
   expect_true(
      .rs.hasExternalPointer(
         local({
            xp <- .Call("rs_newTestExternalPointer", FALSE, PACKAGE = "(embedding)")
            function(x = 2) x
         })
      )
   )

})
