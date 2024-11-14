
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("timestamp() adds to console history", {
   
   remote$consoleExecuteExpr(timestamp(quiet = TRUE))
   remote$keyboardExecute("<Up>")
   
   editor <- remote$editorGetInstance()
   line <- editor$session$getLine(0)
   expect_match(line, "^##.*##$", perl = TRUE)
   remote$keyboardExecute("<Command + A>", "<Backspace>")
   
})
