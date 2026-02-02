#
# SessionAssistant.R
#
# Copyright (C) 2023 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("assistant.setLogLevel", function(level = 0L)
{
   .Call("rs_assistantSetLogLevel", as.integer(level), PACKAGE = "(embedding)")
})

# Extract variable names referenced in R code that also exist in the global environment.
# Returns a character vector of variable names.
.rs.addFunction("assistant.extractVariablesInScope", function(code, envir = globalenv())
{
   # Try to parse the code; return empty if parsing fails
   expr <- tryCatch(
      parse(text = code, keep.source = FALSE),
      error = function(e) NULL
   )

   if (is.null(expr))
      return(character())

   # Get all names referenced in the code
   referencedNames <- all.names(expr, unique = TRUE)

   # Filter to only include names that exist in the environment
   existingNames <- ls(envir = envir)
   intersect(referencedNames, existingNames)
})

.rs.addFunction("assistant.variableType", function(object)
{
   if (is.data.frame(object))
   {
      "data.frame"
   }
   else
   {
      mode(object)
   }
})

#' Describe a single variable for assistant context.
#' Returns a list with name, type, and optionally children.
#'
#' @param name The variable name
#' @param value The variable value
#' @param children Controls child element inclusion:
#'   - FALSE or 0: don't include children
#'   - TRUE: include up to 50 children (default max)
#'   - integer > 0: include up to that many children
.rs.addFunction("assistant.describeVariable", function(name, value, children = FALSE)
{
   type <- .rs.assistant.variableType(value)
   result <- list(name = .rs.scalar(name), type = .rs.scalar(type))

   # Determine maxChildren from the children parameter
   # Handle both integer and numeric 0
   if (identical(children, FALSE) || (is.numeric(children) && children == 0))
      return(result)

   # Skip if this object is not recursive
   if (!is.recursive(value))
      return(result)
   
   # Determine the maximum number of children to include
   maxChildren <- if (isTRUE(children)) 50L else as.integer(children)

   # Get indices, keys, and values (limited to maxChildren)
   n <- min(length(value), maxChildren)
   idxs <- seq_len(n)
   nms <- names(value)
   keys <- if (is.null(nms)) character(n) else nms[idxs]
   vals <- value[idxs]

   # Iterate over these to build child descriptions
   result$children <- .mapply(function(idx, key, val) {
      list(
         name = .rs.scalar(if (nzchar(key)) key else paste0("[[", idx, "]]")),
         type = .rs.scalar(.rs.assistant.variableType(val))
      )
   }, list(idxs, keys, vals), NULL)
   
   # Return result
   result
})

# Generate variable descriptions for the current R session.
# Returns a list suitable for JSON serialization in the format:
# list(
#    list(name = "x", type = "numeric"),
#    list(name = "df", type = "data.frame", children = list(...))
# )
.rs.addFunction("assistant.variableDescriptions", function(envir = globalenv(),
                                                           variablesInScope = TRUE,
                                                           maxVariables = 100,
                                                           maxChildren = 50)
{
   # Get variable names from the environment
   varNames <- ls(envir = envir)
   length(varNames) <- min(length(varNames), maxVariables)

   # Build descriptions for all variables
   descriptions <- lapply(varNames, function(varName) {
      tryCatch({
         value <- get(varName, envir = envir, inherits = FALSE)

         # Determine if we should examine children:
         # - TRUE: examine children on all recursive objects
         # - character vector: examine children only for named variables
         # - FALSE: don't examine children
         shouldExamineChildren <- isTRUE(variablesInScope) ||
            (is.character(variablesInScope) && varName %in% variablesInScope)

         children <- if (shouldExamineChildren) maxChildren else FALSE
         .rs.assistant.describeVariable(varName, value, children)
      }, error = function(e) {
         # Skip variables that can't be accessed
         NULL
      })
   })

   # Remove NULL entries (failed accesses)
   descriptions <- Filter(Negate(is.null), descriptions)

   descriptions
})
