if (!require("testthat", quietly = TRUE))
   stop("You must install 'testthat' to run these tests.")

if (!require("jsonlite", quietly = TRUE))
   stop("You must install 'RJSONIO' to run these tests.")

source("src/cpp/session/modules/SessionCodeTools.R")

compare <- function(object)
{
   us <- .rs.toJSON(object)
   them <- gsub("^\\s*(\\S*)\\s*$", "\\1", capture.output(print(jsonlite::toJSON(object))), perl = TRUE)
   
   if (!identical(us, them))
   {
      expect_identical(us, them)
      cat("Us:   '", us, "'\n", sep = "")
      cat("Them: '", them, "'\n", sep = "")
   }
}

test_that(".rs.toJSON works for some common cases", {
   compare(list(
      a = c(1, 2, 3),
      b = c(1L, 2L, 3L),
      c = list(a = NA, c = NA_character_, d = factor(1)),
      d = c("", "\\", "\\\"", "'\'")
   ))
   
   expect_identical(.rs.toJSON(I("a")), '"a"')
   expect_identical(.rs.toJSON("a"), '["a"]')
   
   ## Example for the async R completions
   ns <- asNamespace("jsonlite")
   exports <- getNamespaceExports(ns)
   objects <- mget(exports, ns, inherits = TRUE)
   types <- unlist(lapply(objects, .rs.getCompletionType))
   isFunction <- unlist(lapply(objects, is.function))
   functions <- objects[isFunction]
   functions <- lapply(functions, function(x) { names(formals(x)) })
   output <- list(
      package = ("jsonlite"),
      exports = exports,
      types = types,
      functions = functions
   )
   
   compare(output)
   
})