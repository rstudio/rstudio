
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

test_that("loaded packages are preserved on suspend + resume", {
   
   remote$consoleExecuteExpr({
      library(tools)
      writeLines(search())
   })
   
   beforeSuspend <- remote$consoleOutput()
   expect_contains(beforeSuspend, "package:tools")
   
   remote$commandExecute("suspendSession")
   remote$commandExecute("consoleClear")
   
   remote$consoleExecuteExpr({
      library(tools)
      writeLines(search())
   })
   
   afterSuspend <- remote$consoleOutput()
   expect_contains(afterSuspend, "package:tools")
   expect_equal(beforeSuspend, afterSuspend)
   
})
