
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# https://github.com/rstudio/rstudio/issues/14636
test_that("variables can be referenced after restart", {
   
   remote$consoleExecute("x <- 1; y <- 2")
   remote$consoleExecute(".rs.api.restartSession('print(x + y)')")
   
   output <- NULL
   .rs.waitUntil("console output is available", function() {
      output <<- remote$consoleOutput()
      output[[length(output)]] == "[1] 3"
   })
   
   expect_equal(output[[length(output)]], "[1] 3")
})
