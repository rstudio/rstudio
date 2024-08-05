
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


test_that("Documents can be reformatted on save", {
   
   remote$consoleExecute(".rs.writeUserPref(\"reformat_on_save\", TRUE)")
   remote$consoleExecute(".rs.writeUserPref(\"code_formatter\", \"styler\")")

   documentContents <- .rs.heredoc("2+2")
   
   remote$documentOpen(".R", documentContents)
   editor <- remote$editorGetInstance()
   editor$insert("1+1; ")
   remote$keyboardExecute("<Ctrl + S>")
   Sys.sleep(1)
   contents <- editor$session$doc$getValue()
   expect_equal(contents, "1 + 1\n2 + 2\n")
   remote$documentClose()
   
})
