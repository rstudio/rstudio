
library(testthat)

self <- remote <- .rs.automation.newRemote()
on.exit(.rs.automation.deleteRemote(), add = TRUE)

# https://github.com/rstudio/rstudio/issues/14784
test_that("autocompletion doesn't trigger active bindings", {
   remote$consoleClear()
   
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
   
   remote$consoleClear()
   
   remote$consoleExecute('
      foobar <- 42
      foobaz <- 42
   ')
   
   remote$keyboardExecute("foo", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("foobar", completionText, fixed = TRUE))
   expect_true(grepl("foobaz", completionText, fixed = TRUE))
   
   remote$consoleClear()   
})

# WIP
test_that("autocompletion in source editor produces the expected completion list for new variables", {
   
   remote$consoleClear()
   
   documentContents <- .rs.heredoc('
    foobar <- 42
    foobaz <- 42
   ')
   
   remote$documentOpen(".R", documentContents)
   editor <- remote$editorGetInstance()
   remote$keyboardExecute("foo", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("foobar", completionText, fixed = TRUE))
   expect_true(grepl("foobaz", completionText, fixed = TRUE))
   
   remote$documentClose()
   remote$consoleClear()  
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list in an existing base function", {
   
   remote$consoleClear() 
   
   remote$keyboardExecute("cat(", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("...", completionText, fixed = TRUE))
   expect_true(grepl("file", completionText, fixed = TRUE))
   expect_true(grepl("sep", completionText, fixed = TRUE))
   
   remote$consoleClear()   
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list in an existing non-base function", {
   
   remote$consoleClear() 
   
   remote$keyboardExecute("dplyr::left_join(", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("...", completionText, fixed = TRUE))
   expect_true(grepl("x", completionText, fixed = TRUE))
   expect_true(grepl("suffix", completionText, fixed = TRUE))
   
   remote$consoleClear() 
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list in a new function defintion", {
   
   remote$consoleClear() 
   
   remote$keyboardExecute("sumWithEllipsesArg <- function(x, y, ..., a, b) { x+y+a+b+sum(", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("...", completionText, fixed = TRUE))
   expect_true(grepl("na.rm", completionText, fixed = TRUE))
   expect_true(grepl("sumWithEllipsesArg", completionText, fixed = TRUE))
   
   remote$consoleClear() 
})

# https://github.com/rstudio/rstudio/issues/13196
test_that("autocompletion in console produces the expected completion list when using a new function", {
   
   remote$consoleClear() 
   
   remote$keyboardExecute("a <- function (..., x, y) { print(x + y) }", "<Enter>")
   remote$keyboardExecute("a(", "<Tab>")
   completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   completionText <- completionListEl$innerText
   
   expect_true(grepl("...", completionText, fixed = TRUE))
   expect_true(grepl("x", completionText, fixed = TRUE))
   expect_true(grepl("y", completionText, fixed = TRUE))
   
   remote$consoleClear() 
})

# https://github.com/rstudio/rstudio/issues/13291
test_that("autocompletion in console produces the expected completion list in a preview of data frames that are part of another object.", {
   
   remote$consoleClear() 
   
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
   
   remote$consoleClear() 
})

# https://github.com/rstudio/rstudio/issues/12678
# test_that("autocompletion in console shows local variables first.", {
#    
#    remote$consoleClear() 
#    
#    code <- .rs.heredoc('
#       library(dplyr)
#       left_table <- tibble(x = 1)
#    ')
#    
#    remote$consoleExecute(code)
#    
#    remote$keyboardExecute("lef", "<Tab>")
#    Sys.sleep(0.5)
#    completionListEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
#    completionText <- completionListEl$innerText
#    
#    # Extract just the completion items (remove package annotations)
#    parts <- strsplit(completionText, "\n{2,}")[[1]]
#    parts <- gsub("\\n.*", "", parts)
#    expect_identical(parts, c("left_table", "left_join"))
# 
#    remote$consoleClear()
# })

# https://github.com/rstudio/rstudio/issues/12678
test_that("autocompletion in console shows local variables first.", {
   
   remote$consoleClear() 
   
   code <- .rs.heredoc('
      library(dplyr)
      left_table <- tibble(x = 1)
   ')

   remote$consoleExecute(code)
   
   parts <- remote$getCompletionList("lef")
   expect_identical(parts, c("left_table", "left_join"))

# WIP: Clear to clean up? Or don't clear, to see end state?
#   remote$consoleClear()
})

