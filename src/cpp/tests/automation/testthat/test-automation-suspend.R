
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

test_that("loaded packages are preserved on suspend + resume", {
   
   remote$commandExecute("consoleClear")
   remote$consoleExecuteExpr({
      library(tools)
      writeLines(search())
   })
   
   .rs.waitUntil("the tools package is loaded", function()
   {
      remote$keyboardExecute("<Enter>")
      ".GlobalEnv" %in% remote$consoleOutput()
   })
   
   beforeSuspend <- setdiff(remote$consoleOutput(), "local:rprofile")
   expect_contains(beforeSuspend, "package:tools")
   
   remote$commandExecute("suspendSession")
   remote$commandExecute("consoleClear")
   
   remote$consoleExecuteExpr({
      library(tools)
      writeLines(search())
   })
   
   .rs.waitUntil("the tools package is loaded", function()
   {
      remote$keyboardExecute("<Enter>")
      ".GlobalEnv" %in% remote$consoleOutput()
   })
   
   afterSuspend <- setdiff(remote$consoleOutput(), "local:rprofile")
   expect_contains(afterSuspend, "package:tools")
   
})

test_that("attached datasets are preserved on suspend + resume", {
   
   remote$consoleExecuteExpr({
      data <- list(apple = 1, banana = 2, cherry = 3)
      attach(data, name = "my-attached-dataset")
   })
   
   remote$commandExecute("suspendSession")
   
   remote$consoleExecuteExpr({
      writeLines(search())
   })
   
   .rs.waitUntil("the session has been restored", function()
   {
      remote$keyboardExecute("<Enter>")
      ".GlobalEnv" %in% remote$consoleOutput()
   })
   
   output <- remote$consoleOutput()
   expect_contains(output, "my-attached-dataset")
   
   remote$consoleExecuteExpr(apple + banana + cherry)
   output <- remote$consoleOutput()
   expect_true("[1] 6" %in% output)
   
})
