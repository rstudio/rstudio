library(testthat)

runAllTests <- function(sourceDir, outputDir, filter = NA)
{
   testThatDir <- file.path(sourceDir, "tests/testthat")
   
   if (!is.na(filter))
   {
      tests <- testthat::test_dir(testThatDir, filter = filter)
   } 
   else
   {
      tests <- testthat::test_dir(testThatDir)
   }
   
   cat(sum(as.data.frame(tests)$failed), file = file.path(outputDir, "testthat-failures.log"))
}
