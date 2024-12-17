
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# https://github.com/rstudio/rstudio/issues/14560
.rs.test("attributes like __foo__ are not quoted when inserted via completions", {
   
   # TODO: doesn't work on CI?
   skip_on_ci()
   
   # Skip if we don't have reticulate.
   installed <- remote$package.isInstalled("reticulate")
   testthat::skip_if_not(installed, "reticulate is not installed")
   
   # Get a Python REPL up and running.
   remote$console.execute("reticulate::repl_python()")
   remote$console.execute("import sys")
   remote$keyboard.insertText("sys.__name")
   
   # Wait for code to finish execution, and then request completions.
   Sys.sleep(3)
   remote$keyboard.insertText("<Tab>", "<Enter>")
   
   # Check for expected output.
   output <- remote$console.getOutput()
   expect_equal(tail(output, n = 2L), c(">>> sys.__name__", "'sys'"))
   
   # Exit the Python REPL and clean up.
   remote$console.execute("exit")
   remote$console.clear()
   
})
