library(testthat)

runTests <- function(testDir = NULL, outputDir = NULL, filter = NULL) {
 
   "%||%" <- function(x, y) if (is.null(x)) y else x
   
   testDir   <- testDir   %||% Sys.getenv("TESTTHAT_TESTS_DIR",  unset = NA)
   outputDir <- outputDir %||% Sys.getenv("TESTTHAT_OUTPUT_DIR", unset = NA)
   filter    <- filter    %||% Sys.getenv("TESTTHAT_FILTER",     unset = "")
   
   # run tests
   results <- testthat::test_dir(
      path            = testDir,
      filter          = filter,
      stop_on_failure = FALSE,
      stop_on_warning = FALSE
   )
   
   # compute number of failures
   df <- as.data.frame(results)
   failures <- sum(df$failed)
   
   # write to file
   cat(failures, file = file.path(outputDir, "testthat-failures.log"))
   
}
