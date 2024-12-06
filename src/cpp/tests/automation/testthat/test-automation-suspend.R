
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("loaded packages are preserved on suspend + resume", {
   
   # Load the 'tools' package.
   remote$console.executeExpr({
      library(tools)
   })
   
   # Suspend the session.
   remote$commands.execute("suspendSession")
   Sys.sleep(1)
   
   # Check and see if 'tools' was loaded on resume.
   remote$console.executeExpr({
      "tools" %in% loadedNamespaces()
   })
   
   output <- remote$console.getOutput()
   expect_contains(output, "[1] TRUE")
   
})

.rs.test("attached datasets are preserved on suspend + resume", {
   
   remote$console.executeExpr({
      data <- list(apple = 1, banana = 2, cherry = 3)
      attach(data, name = "my-attached-dataset")
   })
   
   remote$commands.execute("suspendSession")
   Sys.sleep(1)
   
   remote$console.executeExpr({
      writeLines(search())
   })
   
   output <- remote$console.getOutput()
   expect_contains(output, "my-attached-dataset")
   
   remote$console.executeExpr(apple + banana + cherry)
   output <- remote$console.getOutput()
   expect_true("[1] 6" %in% output)
   
   remote$console.executeExpr({
      detach("my-attached-dataset")
   })
   
})
