library(testthat)

runAllTests <- function(sourceDir, outputDir, filter = NULL)
{
   # get path to testthat tests
   testsRoot <- file.path(sourceDir, "tests/testthat")
   
   # run tests
   results <- testthat::test_dir(
      path = testsRoot,
      filter = filter,
      stop_on_failure = FALSE,
      stop_on_warning = FALSE
   )
   
   # compute number of failures
   df <- as.data.frame(results)
   failures <- sum(df$failed)
   
   # write to file
   cat(failures, file = file.path(outputDir, "testthat-failures.log"))
}
