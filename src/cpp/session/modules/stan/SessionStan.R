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

.rs.addFunction("stan.getCompletions", function(line)
{
   Encoding(line) <- "UTF-8"
   
   # extract token from line
   parts <- .rs.strsplit(line, "\\W+")
   token <- tail(parts, n = 1)
   
   # TODO: what kind of completions to supply when we have no token?
   if (!nzchar(token))
      return(.rs.emptyCompletions(language = "Stan"))
   
   # TODO: different types for each of these?
   completionSources <- list(
      keywords = list(type = "[keyword]", candidates = .rs.stan.keywords()), 
      types    = list(type = "[type]", candidates = .rs.stan.types()),
      blocks   = list(type = "[block]", candidates = .rs.stan.blocks())
   )
   
   completions <- lapply(completionSources, function(source) {
      matches <- .rs.selectFuzzyMatches(source$candidates, token)
      .rs.makeCompletions(token, matches, source$type, type = .rs.acCompletionTypes$KEYWORD)
   })
   
   result <- Reduce(.rs.appendCompletions, completions)
   result$language <- "Stan"
   result
   
})

.rs.addFunction("stan.extractFromNamespace", function(key)
{
   if (!requireNamespace("rstan", quietly = TRUE))
      return(NULL)
   
   rstan <- asNamespace("rstan")
   keywords <- rstan[[key]]
   if (!is.character(keywords))
      return(NULL)
   
   keywords
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
     "quantities", "transformed",  "generated")
})