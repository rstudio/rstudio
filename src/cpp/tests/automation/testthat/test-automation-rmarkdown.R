
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

test_that("the warn option is preserved when running chunks", {
   
   contents <- .rs.heredoc('
      ---
      title: Chunk Warnings
      ---
      
      ```{r warning=TRUE}
      # check current option
      getOption("warn")
      # setting a global option
      options(warn = 2)
      ```
   ')
   
   remote$consoleExecuteExpr({ options(warn = 0) })
   remote$consoleExecuteExpr({ getOption("warn") })
   output <- remote$consoleOutput()
   expect_equal(tail(output, n = 1L), "[1] 0")
   
   id <- remote$documentOpen(".Rmd", contents)
   editor <- remote$editorGetInstance()
   editor$gotoLine(6)
   remote$keyboardExecute("<Ctrl + Shift + Enter>")
   remote$consoleExecuteExpr({ getOption("warn") })
   output <- remote$consoleOutput()
   expect_equal(tail(output, n = 1L), "[1] 2")
   
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
   
})
