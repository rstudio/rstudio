#
# SessionStan.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addJsonRpcHandler("stan_get_completions", function(line)
{
   .rs.stan.getCompletions(line)
})

.rs.addJsonRpcHandler("stan_get_arguments", function(f)
{
   .rs.stan.getArguments(f)
})

.rs.addJsonRpcHandler("stan_run_diagnostics", function(file)
{
   .rs.stan.runDiagnostics(file)
})

.rs.addFunction("stan.getCompletions", function(line)
{
   Encoding(line) <- "UTF-8"
   completions <- .rs.emptyCompletions()
   
   # extract token from line
   parts <- .rs.strsplit(line, "\\W+")
   token <- tail(parts, n = 1)
   
   # TODO: what kind of completions to supply when we have no token?
   if (!nzchar(token))
      return(.rs.emptyCompletions(language = "Stan"))
   
   # construct keyword completions
   keywords <- c(
      .rs.stan.keywords(),
      .rs.stan.types(),
      .rs.stan.blocks()
   )
   
   completions <- .rs.appendCompletions(
      completions,
      .rs.makeCompletions(
         token   = token,
         results = .rs.selectFuzzyMatches(keywords, token),
         type = .rs.acCompletionTypes$KEYWORD
      )
   )
   
   # construct function completions
   rosetta <- .rs.stan.rosetta()
   matches <- .rs.fuzzyMatches(rosetta$StanFunction, token)
   
   completions <- .rs.appendCompletions(
      completions,
      .rs.makeCompletions(
         token    = token,
         results  = rosetta$StanFunction[matches],
         packages = "function",
         type     = .rs.acCompletionTypes$FUNCTION,
         meta     = rosetta$Arguments[matches]
      )
   )
   
   completions
   
})

.rs.addFunction("stan.getArguments", function(f)
{
   none <- .rs.scalar("")
   
   rosetta <- .rs.stan.rosetta()
   if (is.null(rosetta))
      return(none)
   
   idx <- match(f, rosetta$StanFunction)
   if (is.na(idx))
      return(none)
   
   arguments <- rosetta$Arguments[idx]
   
   # if this is a distribution function, borrow arguments from the
   # corresponding '_lpdf' function
   if (identical(arguments, "~"))
   {
      lpdf <- paste(f, "lpdf", sep = "_")
      idx <- match(lpdf, rosetta$StanFunction)
      if (is.na(idx))
         return(none)
      
      arguments <- rosetta$Arguments[idx]
   }
   
   .rs.scalar(arguments)
})

.rs.addFunction("stan.runDiagnostics", function(file)
{
   if (!requireNamespace("rstan", quietly = TRUE))
      return(list())
   
   # invoke stan compiler and capture messages
   messages <- c()
   result <- tryCatch(
      withCallingHandlers(
         rstan::stanc_builder(file),
         message = function(m) {
            messages <<- c(messages, conditionMessage(m))
            invokeRestart("muffleMessage")
         }
      ),
      error = identity
   )
   if (!inherits(result, "error"))
      return(list())
   
   # search for relevant information
   pattern <- "^\\s*error in '([^']+)' at line (\\d+), column (\\d+)"
   line <- grep(pattern, messages, value = TRUE)
   if (length(line) != 1)
      return(list())
   
   m <- regexec(pattern, line)
   matches <- regmatches(line, m)[[1]]
   if (length(matches) != 4)
      return(list())
   
   # keep all lines without indent for messages
   lines <- grep("^\\s+", messages, value = TRUE, invert = TRUE)
   message <- paste(tail(lines, n = -1), collapse = "")
   
   # TODO: although we only ever get one error now, return a vector of
   # error objects in anticipation of that changing in the future
   errors <- list(
      list(
         row    = as.numeric(matches[[3]]) - 1,
         column = as.numeric(matches[[4]]),
         type   = "error",
         text   = message
      )
   )
   
   # make all entries scalar
   rapply(errors, .rs.scalar, how = "replace")
   
})

.rs.addFunction("stan.extractFromNamespace", function(key)
{
   if (!requireNamespace("rstan", quietly = TRUE))
      return(NULL)
   
   rstan <- asNamespace("rstan")
   rstan[[key]]
})

.rs.addFunction("stan.keywords", function()
{
   keywords <- .rs.stan.extractFromNamespace("stan_kw1")
   if (is.character(keywords))
      return(keywords)
   
   c("for", "in", "while", "repeat", "until",
     "if", "then", "else",  "true", "false")
})

.rs.addFunction("stan.types", function()
{
   types <- .rs.stan.extractFromNamespace("stan_kw2")
   if (is.character(types))
      return(types)
   
   c("int", "real", "vector", "simplex", "ordered",
     "positive_ordered", "row_vector", "matrix",
     "corr_matrix", "cov_matrix", "lower",  "upper")
})

.rs.addFunction("stan.blocks", function()
{
   blocks <- .rs.stan.extractFromNamespace("stan_kw3")
   if (is.character(blocks))
      return(blocks)
   
   c("model", "data", "parameters",
     "quantities", "transformed", "generated")
})

.rs.addFunction("stan.rosetta", function()
{
   rosetta <- .rs.stan.extractFromNamespace("rosetta")
   if (!is.data.frame(rosetta))
      return(data.frame())
   
   # remove operator functions
   rosetta <- rosetta[grep("^operator", rosetta$StanFunction, invert = TRUE), ]
   
   # remove duplicates
   rosetta <- rosetta[!duplicated(rosetta$StanFunction), ]
   
   rosetta
})
