
library(testthat)

self <- remote <- .rs.automation.newRemote()
on.exit(.rs.automation.deleteRemote(), add = TRUE)

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
   
   remote$consoleClear()
   
})

test_that("autocompletion in console produces the expected completion list for new variables", {
   
   remote$consoleExecute('
      foobar <- 42
      foobaz <- 42
   ')
   
   remote$keyboardExecute("foo", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("foobar", completionText, fixed = TRUE))
   expect_true(grepl("foobaz", completionText, fixed = TRUE))
   
   remote$keyboardExecute("<Escape>")
   remote$consoleClear()
   
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list in an existing base function", {
   
   remote$keyboardExecute("cat(", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("...", completionText, fixed = TRUE))
   expect_true(grepl("file", completionText, fixed = TRUE))
   expect_true(grepl("sep", completionText, fixed = TRUE))
   
   remote$keyboardExecute("<Escape>")
   remote$consoleClear()
   
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list in an existing non-base function", {
   
   remote$keyboardExecute("stats::rnorm(", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("n =", completionText, fixed = TRUE))
   expect_true(grepl("mean =", completionText, fixed = TRUE))
   expect_true(grepl("sd =", completionText, fixed = TRUE))
   
   remote$keyboardExecute("<Escape>")
   remote$consoleClear()
   
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list in a new function defintion", {
   
   remote$keyboardExecute("sumWithEllipsesArg <- function(x, y, ..., a, b) { x+y+a+b+sum(", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("...", completionText, fixed = TRUE))
   expect_true(grepl("na.rm", completionText, fixed = TRUE))
   expect_true(grepl("sumWithEllipsesArg", completionText, fixed = TRUE))
 
   remote$keyboardExecute("<Escape>")  
   remote$consoleClear()
   
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list when using a new function", {
   
   remote$keyboardExecute("a <- function (..., x, y) { print(x + y) }", "<Enter>")
   remote$keyboardExecute("a(", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("...", completionText, fixed = TRUE))
   expect_true(grepl("x", completionText, fixed = TRUE))
   expect_true(grepl("y", completionText, fixed = TRUE))
   
   remote$keyboardExecute("<Escape>")
   remote$consoleClear() 
   
})

# https://github.com/rstudio/rstudio/issues/13291
test_that("autocompletion in console produces the expected completion list in a preview of data frames that are part of another object.", {
   
   code <- .rs.heredoc('
      test_df <- data.frame(col1 = rep(1, 3),
         col2 = rep(2, 3),
         col3 = rep(3, 3))

      test_ls <- list(a = test_df,
         b = test_df)
   ')
   
   remote$consoleExecute(code)
   
   remote$keyboardExecute("test_ls$", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("a", completionText, fixed = TRUE))
   expect_true(grepl("b", completionText, fixed = TRUE))
   
   remote$keyboardExecute("<Escape>")
   remote$consoleClear()
   
})

# https://github.com/rstudio/rstudio/issues/12678
test_that("autocompletion in console shows local variables first.", {
   
   code <- .rs.heredoc('
      library(dplyr)
      left_table <- tibble(x = 1)
   ')

   remote$consoleExecute(code)
   
   parts <- remote$getCompletionList("lef")
   expect_identical(parts, c("left_table", "left_join"))

   remote$keyboardExecute("<Escape>")
   remote$consoleClear()
   
})

