## Run the internal linter over all RStudio
## source files.
library(testthat)

context("Linter")
setwd(.rs.getProjectDirectory())

lint <- function(x) {
   .Call("rs_parseAndLintRFile", x, unlist(.rs.objectsOnSearchPath()))
}

test_that("RStudio .R files can be linted", {
   
   rFiles <- list.files(
      pattern = "R$",
      full.names = TRUE,
      recursive = TRUE
   )
   
   system.time({
      results <- lapply(rFiles, function(x) {
         cat("Linting file: '", x, "'\n", sep = "")
         lint(x)
      })
   })
   
})