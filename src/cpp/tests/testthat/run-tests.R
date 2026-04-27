library(testthat)

runTests <- function(testDir = NULL, outputDir = NULL, filter = NULL) {

   options(rstudio.tests.running = TRUE)
   on.exit(options(rstudio.tests.running = FALSE), add = TRUE)

   # Disable ANSI escape codes in CI environments so that test output
   # is plain text in build logs.
   if (nzchar(Sys.getenv("JENKINS_URL")) || nzchar(Sys.getenv("CI")))
   {
      Sys.setenv(NO_COLOR = "1")
      options(cli.num_colors = 1)
   }

   "%||%" <- function(x, y) if (is.null(x)) y else x

   # print R information
   writeLines("# R version ----")
   print(R.version)
   writeLines("")

   # print out system information
   writeLines("# System information ----")
   info <- as.list(Sys.info())
   str(info)
   writeLines("")
   
   # print locale information
   writeLines("# Locale ----")
   writeLines(Sys.getlocale())
   writeLines("")

   # print library paths
   writeLines("# Library paths ----")
   writeLines(paste("-", .libPaths(), collapse = "\n"))
   writeLines("")

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
