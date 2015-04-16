## Run the internal linter over all RStudio
## source files.
library(testthat)

context("Diagnostics")
setwd(.rs.getProjectDirectory())

lint <- function(x) {
   invisible(.rs.lintRFile(x))
}

test_that("RStudio .R files can be linted", {
   
   rFiles <- list.files(
      pattern = "R$",
      full.names = TRUE,
      recursive = TRUE
   )
   
   print(system.time({
      results <- lapply(rFiles, function(x) {
         results <- lint(x)
         errors <- results[unlist(lapply(results, function(x) {
            x$type == "error"
         }))]
         if (length(errors))
            warning("Lint errors in file: '",
                    x,
                    "'",
                    call. = FALSE)
      }
   )}))
   
})
