
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# https://github.com/rstudio/rstudio/issues/14784
test_that("autocompletion doesn't trigger active bindings", {
   
   code <- .rs.heredoc('
      Test <- R6::R6Class("Test",
        active  = list(active_test  = function(value) print("active")),
        private = list(private_test = function(value) print("private")),
        public  = list(public_test  = function(value) print("public"))
      )
      
      n <- Test$new()
      nms <- .rs.getNames(n)
   ')
   
   remote$consoleExecute(code)
   output <- remote$consoleOutput()
   expect_false(tail(output, 1) == "[1] \"active\"")
   
   remote$keyboardExecute("n", "<Tab>", "<Escape>", "<Backspace>")
   output <- remote$consoleOutput()
   expect_false(tail(output, 1) == "[1] \"active\"")
   
})

test_that("autocompletion in console produces the expected completion list for new variables", {
   
   remote$consoleExecute('
      foobar <- 42
      foobaz <- 42
   ')
   
   completions <- remote$completionsRequest("foo")
   expect_equal(completions, c("foobar", "foobaz"))
   
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list in an existing base function", {
   
   completions <- remote$completionsRequest("cat(")
   expect_equal(completions, c("... =", "file =", "sep =", "fill =", "labels =", "append ="))
   
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list in an existing non-base function", {
   
   completions <- remote$completionsRequest("stats::rnorm(")
   expect_equal(completions, c("n =", "mean =", "sd ="))
   
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list when using a new function", {
   
   # Define a function accepting some parameters.
   remote$keyboardExecute("a <- function(x, y, z) { print(x + y) }", "<Enter>")
   
   # Request completions for that function.
   completions <- remote$completionsRequest("a(")
   expect_equal(completions, c("x =", "y =", "z ="))
   
})

# https://github.com/rstudio/rstudio/issues/13291
test_that("list names are provided as completions following '$'", {
   
   code <- .rs.heredoc('
      test_df <- data.frame(
         col1 = rep(1, 3),
         col2 = rep(2, 3),
         col3 = rep(3, 3)
      )

      test_ls <- list(
         a = test_df,
         b = test_df
      )
   ')
   
   remote$consoleExecute(code)
   
   completions <- remote$completionsRequest("test_ls$")
   expect_equal(completions, c("a", "b"))
   
})

# https://github.com/rstudio/rstudio/issues/12678
test_that("autocompletion in console shows local variables first.", {
   
   code <- .rs.heredoc('
      library(dplyr)
      left_table <- tibble(x = 1)
   ')

   remote$consoleExecute(code)
   Sys.sleep(1)
   parts <- remote$completionsRequest("lef")
   expect_identical(parts, c("left_table", "left_join"))
   
})

# https://github.com/rstudio/rstudio/issues/13611
test_that("autocompletions within piped expressions work at start of document", {
   
   code <- .rs.heredoc('
      
      mtcars |> mutate(x = mean())
   ')
   
   remote$documentExecute(".R", code, function(editor)
   {
      editor$gotoLine(2L, 26L)
      completions <- remote$completionsRequest("")
      expect_equal(completions[1:2], c("x =", "... ="))
   })

})

# https://github.com/rstudio/rstudio/issues/15115
test_that(".DollarNames completions still produce types", {
   
   remote$consoleExecuteExpr({
   
      className <- basename(tempfile(pattern = "class-"))
      registerS3method(".DollarNames", className, function(x, pattern) names(x))
      
      . <- structure(
         list(apple = identity, banana = identity),
         class = className
      )
      
   })
   
   completions <- remote$completionsRequest(".$")
   expect_equal(completions, c("apple", "banana"))
   
   remote$consoleExecuteExpr({
      className <- basename(tempfile(pattern = "class-"))
      registerS3method(".DollarNames", className, function(x, pattern) c("example1()", "example2()"))
      . <- structure(list(), class = className)
   })
   
   completions <- remote$completionsRequest(".$")
   expect_equal(completions, c("example1", "example2"))
   
})

# https://github.com/rstudio/rstudio/issues/15046
test_that("Tab keypresses indent multi-line selections", {
   
   contents <- .rs.heredoc('
      ---
      title: R Markdown Document
      ---
      
      This is some prose.
      This is some more prose.
   ')
   
   remote$documentOpen(ext = ".Rmd", contents = contents)
   
   editor <- remote$editorGetInstance()
   editor$gotoLine(5)
   editor$selectPageDown()
   
   remote$keyboardExecute("<Tab>")
   expect_equal(editor$session$getLine(4), "  This is some prose.")
   expect_equal(editor$session$getLine(5), "  This is some more prose.")
   
   remote$documentClose()
})

# https://github.com/rstudio/rstudio/issues/13065
test_that("code_completion_include_already_used works as expected", {
   
   contents <- .rs.heredoc('
      mtcars |> write()
   ')
   
   remote$documentOpen(ext = ".R", contents = contents)
   on.exit(remote$documentClose(), add = TRUE)
   
   editor <- remote$editorGetInstance()
   editor$gotoLine(1, 16)
   completions <- remote$completionsRequest()
   expect_equal(completions[1:4], c("file =", "ncolumns =", "append =", "sep ="))
   
   remote$consoleExecuteExpr({
      .rs.uiPrefs$codeCompletionIncludeAlreadyUsed$set(TRUE)
   })
   
   remote$commandExecute("activateSource")
   editor$gotoLine(1, 16)
   completions <- remote$completionsRequest()
   expect_equal(completions[1:5], c("x =", "file =", "ncolumns =", "append =", "sep ="))
   
   remote$consoleExecuteExpr({
      .rs.uiPrefs$codeCompletionIncludeAlreadyUsed$clear()
   })
   
})

# https://github.com/rstudio/rstudio/issues/15161
test_that("dplyr piped variable names are properly quoted / unquoted", {
   
   contents <- .rs.heredoc('
      library(dplyr)
      mtcars |> rename(`zzz A` = 1, `zzz B` = 2) |> select()
   ')
   
   remote$documentOpen(ext = ".R", contents = contents)
   on.exit(remote$documentClose(), add = TRUE)
   
   editor <- remote$editorGetInstance()
   editor$gotoLine(2, 53)
   completions <- remote$completionsRequest("zzz")
   expect_equal(completions, c("zzz A", "zzz B"))
   
})
