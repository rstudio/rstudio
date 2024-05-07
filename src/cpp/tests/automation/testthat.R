
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

# Execute the tests.
projectRoot <- here::here()
testRoot <- file.path(projectRoot, "src/cpp/tests/automation")
testthat::test_dir(file.path(testRoot, "testthat"))
