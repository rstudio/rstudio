
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("timestamp() adds to console history", {
   
   remote$console.executeExpr(timestamp(quiet = TRUE))
   remote$keyboard.insertText("<Up>")
   
   editor <- remote$editor.getInstance()
   line <- editor$session$getLine(0)
   expect_match(line, "^##.*##$", perl = TRUE)
   remote$keyboard.insertText("<Command + A>", "<Backspace>")
   
})
