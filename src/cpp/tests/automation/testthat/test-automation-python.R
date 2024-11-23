
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# https://github.com/rstudio/rstudio/issues/14560
.rs.test("attributes like __foo__ are not quoted when inserted via completions", {
   
   # Get a Python REPL up and running.
   remote$skipIfNotInstalled("reticulate")
   remote$consoleExecute("reticulate::repl_python()")
   remote$consoleExecute("import sys")
   remote$keyboardExecute("sys.__name")
   
   # Wait for code to finish execution, and then request completions.
   Sys.sleep(3)
   remote$keyboardExecute("<Tab>", "<Enter>")
   
   # Check for expected output.
   output <- remote$consoleOutput()
   expect_equal(tail(output, n = 2L), c(">>> sys.__name__", "'sys'"))
   
   # Exit the Python REPL and clean up.
   remote$consoleExecute("exit")
   remote$consoleClear()
   
})
