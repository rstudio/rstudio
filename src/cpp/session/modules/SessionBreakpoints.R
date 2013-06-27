#
# SessionBreakpoints.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

# given the body of a function, search recursively through its parsed
# representation for a step with a given source reference line number.
.rs.addFunction("stepsAtLine", function(funBody, line)
{
   if (typeof(funBody) != "language")
   {
      return(NULL)
   }

   srcrefs <- attr(funBody, "srcref")
   for (i in seq_along(funBody))
   {
      srcref <- srcrefs[[i]]

      if (!is.null(srcref) && (srcref[1] > line || line > srcref[3]))
      {
         next
      }

      finer <- .rs.stepsAtLine(funBody[[i]], line)
      if (!is.null(finer))
      {
         return(c(i, finer))
      }

      if (!is.null(srcref) &&
          !(typeof(funBody[[i]]) == "symbol" &&
            identical(as.character(funBody[[i]]), "{")))
      {
         return(i)
      }
   }
   return(NULL)
})

# this function is used to get the steps in the given function that are
# associated with the given line number, using the function's source
# references.
.rs.addFunction("getFunctionSteps", function(functionName, lineNumbers)
{
   funBody <- body(get(functionName, envir=globalenv()))
   return(lapply(lineNumbers, function(lineNumber)
   {
      return(list(
         name=functionName,
         line=lineNumber,
         at=.rs.stepsAtLine(funBody, lineNumber)))
   }))
})

.rs.addFunction("setFunctionBreakpoints", function(functionName, steps)
{
   if (length(steps) == 0)
   {
      untrace(functionName)
   }
   else
   {
      trace(
          what = functionName,
          at = steps,
          tracer = browser,
          print = FALSE)
   }
})

.rs.addJsonRpcHandler("set_function_breakpoints", function(functionName, steps)
{
   .rs.setFunctionBreakpoints(functionName, steps)
})

.rs.addJsonRpcHandler("get_function_steps", function(functionName, lineNumbers)
{
   results <- .rs.getFunctionSteps(functionName, lineNumbers)
   formattedResults <- data.frame(
      line = numeric(0),
      name = character(0),
      at = numeric(0),
      stringsAsFactors = FALSE)
   for (result in results)
   {
      formattedResult <- list(
         line = result$line,
         name = result$name,
         at = result$at)
      formattedResults <- rbind(formattedResults, formattedResult)
   }
   formattedResults$name <- as.character(formattedResults$name)
   return(formattedResults)
})

