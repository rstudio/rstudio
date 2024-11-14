
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("loaded packages are preserved on suspend + resume", {
   
   # Load the 'tools' package.
   remote$consoleExecuteExpr({
      library(tools)
   })
   
   # Suspend the session.
   remote$commandExecute("suspendSession")
   Sys.sleep(1)
   
   # Check and see if 'tools' was loaded on resume.
   remote$consoleExecuteExpr({
      "tools" %in% loadedNamespaces()
   })
   
   output <- remote$consoleOutput()
   expect_contains(output, "[1] TRUE")
   
})

.rs.test("attached datasets are preserved on suspend + resume", {
   
   remote$consoleExecuteExpr({
      data <- list(apple = 1, banana = 2, cherry = 3)
      attach(data, name = "my-attached-dataset")
   })
   
   remote$commandExecute("suspendSession")
   Sys.sleep(1)
   
   remote$consoleExecuteExpr({
      writeLines(search())
   })
   
   output <- remote$consoleOutput()
   expect_contains(output, "my-attached-dataset")
   
   remote$consoleExecuteExpr(apple + banana + cherry)
   output <- remote$consoleOutput()
   expect_true("[1] 6" %in% output)
   
   remote$consoleExecuteExpr({
      detach("my-attached-dataset")
   })
   
})
