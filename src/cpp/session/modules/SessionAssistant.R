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

# Constants for variable description limits
.rs.setVar("assistant.kMaxVariables", 100L)
.rs.setVar("assistant.kMaxChildren", 50L)

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

#' Build child descriptions for a recursive object.
#' @param value A recursive object (list, data.frame, etc.)
#' @param maxChildren Maximum number of children to include
#' @return A list of child descriptions with name and type
.rs.addFunction("assistant.describeChildren", function(value, maxChildren = .rs.assistant.kMaxChildren)
{
   n <- min(length(value), maxChildren)
   idxs <- seq_len(n)
   nms <- names(value)
   keys <- if (is.null(nms)) character(n) else nms[idxs]
   vals <- value[idxs]

   .mapply(function(idx, key, val) {
      list(
         name = .rs.scalar(if (nzchar(key)) key else paste0("[[", idx, "]]")),
         type = .rs.scalar(.rs.assistant.variableType(val))
      )
   }, list(idxs, keys, vals), NULL)
})

#' Describe a single variable for assistant context.
#' Returns a list with name, type, optionally action, and optionally children.
#'
#' @param name The variable name
#' @param value The variable value (can be NULL for delete action)
#' @param children Controls child element inclusion:
#'   - FALSE or 0: don't include children
#'   - TRUE: include up to 50 children (default max)
#'   - integer > 0: include up to that many children
#' @param action Optional action field ("create", "modify", or "delete")
.rs.addFunction("assistant.describeVariable", function(name, value, children = FALSE, action = NULL)
{
   # For delete actions, return minimal info
   if (identical(action, "delete"))
   {
      return(list(
         name = .rs.scalar(name),
         type = .rs.scalar(""),
         action = .rs.scalar(action)
      ))
   }

   type <- .rs.assistant.variableType(value)
   result <- list(name = .rs.scalar(name), type = .rs.scalar(type))

   # Add action field if provided
   if (!is.null(action))
      result$action <- .rs.scalar(action)

   # Determine maxChildren from the children parameter
   # Handle both integer and numeric 0
   if (identical(children, FALSE) || (is.numeric(children) && children == 0))
      return(result)

   # Skip if this object is not recursive
   if (is.null(value) || !is.recursive(value))
      return(result)

   # Determine the maximum number of children to include
   maxChildren <- if (isTRUE(children)) .rs.assistant.kMaxChildren else as.integer(children)

   # Build child descriptions
   result$children <- .rs.assistant.describeChildren(value, maxChildren)

   result
})

# Describe a single variable from an environment.
# Returns NULL if the variable doesn't exist or can't be accessed.
.rs.addFunction("assistant.describeVariableFromEnv", function(varName,
                                                              envir,
                                                              examineChildren,
                                                              maxChildren,
                                                              action)
{
   if (!exists(varName, envir = envir, inherits = FALSE))
      return(NULL)

   value <- get(varName, envir = envir, inherits = FALSE)
   children <- if (examineChildren) maxChildren else FALSE
   .rs.assistant.describeVariable(varName, value, children, action)
})

# Check if a variable should have its children examined.
# @param varName The variable name to check
# @param names The names parameter from variableDescriptions (NULL or character vector)
# @param variablesInScope TRUE, FALSE, or character vector of variable names
.rs.addFunction("assistant.shouldExamineChildren", function(varName, names, variablesInScope)
{
   !is.null(names) ||
      isTRUE(variablesInScope) ||
      (is.character(variablesInScope) && varName %in% variablesInScope)
})

# Generate variable descriptions for the current R session.
# Returns a list suitable for JSON serialization in the format:
# list(
#    list(name = "x", type = "numeric"),
#    list(name = "df", type = "data.frame", children = list(...))
# )
#
# @param envir The environment to examine
# @param names Optional character vector of specific variable names to describe.
#   If NULL (default), describes all variables in the environment.
# @param variablesInScope Controls which variables get children examined (only used when names=NULL):
#   - TRUE: examine children on all recursive objects
#   - character vector: examine children only for named variables
#   - FALSE: don't examine children
# @param maxVariables Maximum number of variables to include (only used when names=NULL)
# @param maxChildren Maximum number of children to include for recursive objects
# @param action Optional action field ("create", "modify", or "delete")
.rs.addFunction("assistant.variableDescriptions", function(envir = globalenv(),
                                                           names = NULL,
                                                           variablesInScope = TRUE,
                                                           maxVariables = .rs.assistant.kMaxVariables,
                                                           maxChildren = .rs.assistant.kMaxChildren,
                                                           action = NULL)
{
   # Get variable names - either specified or all from environment
   if (is.null(names))
   {
      varNames <- ls(envir = envir)
      length(varNames) <- min(length(varNames), maxVariables)
   }
   else
   {
      varNames <- names
   }

   # Build descriptions for all variables
   descriptions <- lapply(varNames, function(varName) {
      examineChildren <- .rs.assistant.shouldExamineChildren(varName, names, variablesInScope)

      tryCatch(
         .rs.assistant.describeVariableFromEnv(varName, envir, examineChildren, maxChildren, action),
         error = function(e) NULL
      )
   })

   # Remove NULL entries (failed accesses)
   Filter(Negate(is.null), descriptions)
})
