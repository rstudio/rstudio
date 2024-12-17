
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# https://github.com/rstudio/rstudio/issues/14784
.rs.test("autocompletion doesn't trigger active bindings", {
 
   code <- .rs.heredoc('
      Test <- R6::R6Class("Test",
        active  = list(active_test  = function(value) print("active")),
        private = list(private_test = function(value) print("private")),
        public  = list(public_test  = function(value) print("public"))
      )
      
      n <- Test$new()
      nms <- .rs.getNames(n)
   ')
   
   remote$console.execute(code)
   output <- remote$console.getOutput()
   expect_false(tail(output, 1) == "[1] \"active\"")
   
   remote$keyboard.insertText("n", "<Tab>", "<Escape>", "<Backspace>")
   output <- remote$console.getOutput()
   expect_false(tail(output, 1) == "[1] \"active\"")
   
})

.rs.test("autocompletion in console produces the expected completion list for new variables", {
   
   remote$console.execute('
      foobar <- 42
      foobaz <- 42
   ')
   
   completions <- remote$completions.request("foo")
   expect_equal(completions, c("foobar", "foobaz"))
   
})

# https://github.com/rstudio/rstudio/issues/13196
.rs.test("autocompletion in console produces the expected completion list in an existing base function", {
   
   completions <- remote$completions.request("cat(")
   expect_equal(completions, c("... =", "file =", "sep =", "fill =", "labels =", "append ="))
   
})

# https://github.com/rstudio/rstudio/issues/13196
.rs.test("autocompletion in console produces the expected completion list in an existing non-base function", {
   
   completions <- remote$completions.request("stats::rnorm(")
   expect_equal(completions, c("n =", "mean =", "sd ="))
   
})

# https://github.com/rstudio/rstudio/issues/13196
.rs.test("autocompletion in console produces the expected completion list when using a new function", {
   
   # Define a function accepting some parameters.
   remote$keyboard.insertText("a <- function(x, y, z) { print(x + y) }", "<Enter>")
   
   # Request completions for that function.
   completions <- remote$completions.request("a(")
   expect_equal(completions, c("x =", "y =", "z ="))
   
})

# https://github.com/rstudio/rstudio/issues/13291
.rs.test("list names are provided as completions following '$'", {
   
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
   
   remote$console.execute(code)
   
   completions <- remote$completions.request("test_ls$")
   expect_equal(completions, c("a", "b"))
   
})

# https://github.com/rstudio/rstudio/issues/12678
.rs.test("autocompletion in console shows local variables first.", {
   
   code <- .rs.heredoc('
      library(dplyr)
      left_table <- tibble(x = 1)
   ')

   remote$console.execute(code)
   Sys.sleep(1)
   parts <- remote$completions.request("lef")
   expect_identical(parts, c("left_table", "left_join"))
   
})

# https://github.com/rstudio/rstudio/issues/13611
.rs.test("autocompletions within piped expressions work at start of document", {
   
   code <- .rs.heredoc('
      
      mtcars |> mutate(x = mean())
   ')
   
   remote$editor.executeWithContents(".R", code, function(editor)
   {
      editor$gotoLine(2L, 26L)
      completions <- remote$completions.request("")
      expect_equal(completions[1:2], c("x =", "... ="))
   })

})

# https://github.com/rstudio/rstudio/issues/15115
.rs.test(".DollarNames completions still produce types", {
   
   remote$console.executeExpr({
   
      className <- basename(tempfile(pattern = "class-"))
      registerS3method(".DollarNames", className, function(x, pattern) names(x))
      
      . <- structure(
         list(apple = identity, banana = identity),
         class = className
      )
      
   })
   
   completions <- remote$completions.request(".$")
   expect_equal(completions, c("apple", "banana"))
   
   remote$console.executeExpr({
      className <- basename(tempfile(pattern = "class-"))
      registerS3method(".DollarNames", className, function(x, pattern) c("example1()", "example2()"))
      . <- structure(list(), class = className)
   })
   
   completions <- remote$completions.request(".$")
   expect_equal(completions, c("example1", "example2"))
   
})

# https://github.com/rstudio/rstudio/issues/15046
.rs.test("Tab keypresses indent multi-line selections", {
   
   contents <- .rs.heredoc('
      ---
      title: R Markdown Document
      ---
      
      This is some prose.
      This is some more prose.
   ')
   
   remote$editor.openWithContents(ext = ".Rmd", contents = contents)
   
   editor <- remote$editor.getInstance()
   editor$gotoLine(5)
   editor$selectPageDown()
   
   remote$keyboard.insertText("<Tab>")
   expect_equal(editor$session$getLine(4), "  This is some prose.")
   expect_equal(editor$session$getLine(5), "  This is some more prose.")
})

# https://github.com/rstudio/rstudio/issues/13065
.rs.test("code_completion_include_already_used works as expected", {
   
   contents <- .rs.heredoc('
      mtcars |> write()
   ')
   
   remote$editor.openWithContents(ext = ".R", contents = contents)
   
   editor <- remote$editor.getInstance()
   editor$gotoLine(1, 16)
   completions <- remote$completions.request()
   expect_equal(completions[1:4], c("file =", "ncolumns =", "append =", "sep ="))
   
   remote$console.executeExpr({
      .rs.uiPrefs$codeCompletionIncludeAlreadyUsed$set(TRUE)
   })
   
   remote$commands.execute("activateSource")
   editor$gotoLine(1, 16)
   completions <- remote$completions.request()
   expect_equal(completions[1:5], c("x =", "file =", "ncolumns =", "append =", "sep ="))
   
   remote$console.executeExpr({
      .rs.uiPrefs$codeCompletionIncludeAlreadyUsed$clear()
   })
   
})

# https://github.com/rstudio/rstudio/issues/15161
.rs.test("dplyr piped variable names are properly quoted / unquoted", {
   
   contents <- .rs.heredoc('
      library(dplyr)
      mtcars |> rename(`zzz A` = 1, `zzz B` = 2) |> select()
   ')
   
   remote$editor.openWithContents(ext = ".R", contents = contents)
   
   editor <- remote$editor.getInstance()
   editor$gotoLine(2, 53)
   completions <- remote$completions.request("zzz")
   expect_equal(completions, c("zzz A", "zzz B"))
   
})

# https://github.com/rstudio/rstudio/issues/13290
.rs.test("column names are quoted appropriately", {
   
   remote$console.executeExpr({
      data <- list(apple = "apple", "2024" = "2024")
   })
   
   remote$keyboard.insertText("data$", "<Tab>")
   Sys.sleep(1)
   remote$keyboard.insertText("<Down>", "<Enter>", "<Enter>")
   
   output <- remote$console.getOutput()
   expect_equal(tail(output, n = 1L), "[1] \"2024\"")
   
})
