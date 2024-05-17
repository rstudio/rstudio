
.rs.automation.installRequiredPackages()

library(testthat)

# Return to original working directory when we're done.
owd <- getwd()
on.exit(setwd(owd), add = TRUE)

# Create wrappers for 'test_that', which allow for timing out on error.
test_that <- function(desc, code) {
   
   # Set up some timeout handlers.
   setTimeLimit(60L, transient = TRUE)
   on.exit(setTimeLimit(), add = TRUE)
   
   # Delegate to testthat.
   call <- sys.call()
   call[[1L]] <- quote(testthat::test_that)
   eval(call, envir = parent.frame())
   
}

# Find the test directory.
testDir <- "testthat"
if (!file.exists(testDir)) {
   
   # Helper for interactive usages.
   projectRoot <- here::here()
   testDir <- file.path(projectRoot, "src/cpp/tests/automation/testthat")
   
}

# Create a junit-style reporter.
junitResultsFile <- tempfile("junit-", fileext = ".xml")
junitReporter <- testthat::JunitReporter$new(file = junitResultsFile)

# Run the tests.
status <- local({
   
   # Create an automation remote.
   remote <- .rs.automation.newRemote()
   on.exit(remote$quit(), add = TRUE)
   
   # Run tests with this active remote.
   testthat::test_dir(
      path = testDir,
      reporter = junitReporter,
      stop_on_failure = FALSE,
      stop_on_warning = FALSE
   )
   
})

# Echo the test report.
writeLines(readLines(junitResultsFile))
