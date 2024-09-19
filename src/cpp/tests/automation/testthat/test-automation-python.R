
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# https://github.com/rstudio/rstudio/issues/14560
test_that("attributes like __foo__ are not quoted when inserted via completions", {
   remote$consoleExecute("reticulate::repl_python()")
   remote$consoleExecute("import sys")
   remote$keyboardExecute("sys.__name")
   Sys.sleep(0.5)
   remote$keyboardExecute("<Tab>", "<Enter>")
   output <- remote$consoleOutput()
   expect_equal(tail(output, n = 2L), c(">>> sys.__name__", "'sys'"))
   remote$consoleExecute("exit")
   remote$consoleClear()
})
