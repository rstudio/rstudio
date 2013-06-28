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

   refs <- attr(funBody, "srcref")
   for (idx in 1:length(funBody))
   {
      # if there's a source ref on this line, check it against the line number
      # provided by the caller
      ref <- refs[[idx]]
      if (length(ref) > 0)
      {
         if (line > ref[3] || line < ref[1])
         {
            next
         }
      }

      # check for sub-steps--these exist when there's a nested function
      nestedFunSteps <- .rs.stepsAtLine(funBody[[idx]], line)
      if (!is.null(nestedFunSteps))
      {
         return(c(idx, nestedFunSteps))
      }

      # match functions rather than opening brackets
      if (length(ref) > 0 &&
          !(typeof(funBody[[idx]]) == "symbol" &&
            identical(as.character(funBody[[idx]]), "{")))
      {
         return(idx)
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
         at=paste(.rs.stepsAtLine(funBody, lineNumber), collapse=",")))
   }))
})

.rs.addFunction("setFunctionBreakpoints", function(functionName, steps)
{
   if (length(steps) == 0 || nchar(steps) == 0)
   {
      untrace(functionName)
   }
   else
   {
      trace(
          what = functionName,
          at = lapply(strsplit(as.character(steps), ","), as.numeric),
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
      at = character(0),
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
   formattedResults$at <- as.character(formattedResults$at)
   return(formattedResults)
})

