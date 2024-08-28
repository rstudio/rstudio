
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

# https://github.com/rstudio/rstudio/issues/13611
test_that("autocompletions within piped expressions work at start of document", {
   
   code <- .rs.heredoc('
      
      mtcars |> mutate(x = mean())
   ')
   
   remote$documentOpen(".R", code)
   editor <- remote$editorGetInstance()
   editor$gotoLine(2L, 26L)
   remote$keyboardExecute("<Tab>")
   popupEl <- remote$jsObjectViaSelector("#rstudio_popup_completions")
   popupText <- popupEl$innerText
   expect_true(grepl("trim =",  popupText, fixed = TRUE))
   expect_true(grepl("na.rm =", popupText, fixed = TRUE))
   expect_true(grepl("... =",   popupText, fixed = TRUE))
   remote$keyboardExecute("<Escape>")
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")

})
