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

# given the name of a function, return the name of the first environment on the
# search path in which the function was found.
.rs.addFunction("getEnvironmentOfFunction", function(objName)
{
   envs <- search()
   for (e in envs)
   {
      env <- as.environment(e)
      if (!is.null(env) &&
          exists(objName, env, mode = "function", inherits = FALSE))
      {
         return (env)
      }
   }
   return(NULL)
})

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

.rs.addFunction("tracedSourceRefs",function(funBody, originalFunBody)
{
  if (is.symbol(funBody) || is.symbol(originalFunBody))
  {
    return (funBody)
  }

  # start with a copy of the original source references
  attr(funBody, "srcref") <- attr(originalFunBody, "srcref")

  for (idx in 1:length(funBody))
  {
    if (is.null(funBody[[idx]]) || is.pairlist(funBody[[idx]])) next

    # if this expression was replaced by trace(), copy the source references
    # from the original expression over each expression injected by trace()
    if (sum(funBody[[idx]] != originalFunBody[[idx]]) > 0)
    {
      attr(funBody[[idx]], "srcref") <-
        rep(list(attr(originalFunBody, "srcref")[[idx]]), length(funBody[[idx]]))
    }

    # recurse to symbol level
    else if (is.language(funBody[[idx]]))
    {
      funBody[[idx]] <- .rs.tracedSourceRefs(
         funBody[[idx]],
         originalFunBody[[idx]])
    }
  }
  return(funBody)
})

# this function is used to get the steps in the given function that are
# associated with the given line number, using the function's source
# references.
.rs.addFunction("getFunctionSteps", function(functionName, lineNumbers)
{
   fun <- get(functionName, envir=globalenv())
   funBody <- body(fun)

   # attempt to find the end line of the function
   funStartLine <- 0
   funEndLine <- 0
   funSrcRef <- attr(.rs.getUntracedFunction(functionName), "srcref")
   if (!is.null(funSrcRef) && length(funSrcRef) > 3)
   {
      funStartLine <- funSrcRef[1]
      funEndLine <- funSrcRef[3]
   }
   else
   {
      return(list())
   }

   # process each line on which a breakpoint was requested
   lapply(lineNumbers, function(lineNumber)
   {
      # don't try to process lines that aren't inside the body of the function
      steps <- integer()
      if (lineNumber >= funStartLine &&
          lineNumber <= funEndLine)
      {
         # if we don't find any function steps associated with the given line
         # number, keep trying the next one until we do, up to the end of the
         # function (as marked by its source references)
         repeat
         {
            steps <- .rs.stepsAtLine(funBody, lineNumber)
            if (length(steps) > 0 ||
                lineNumber >= funEndLine)
            {
               break
            }
            lineNumber <- lineNumber + 1
         }
      }

      list(
         name=functionName,
         line=lineNumber,
         at=paste(steps, collapse=","))
   })
})

.rs.addFunction("setFunctionBreakpoints", function(functionName, steps)
{
   envir <- .rs.getEnvironmentOfFunction(functionName)
   if (length(steps) == 0 || nchar(steps) == 0)
   {
      untrace(functionName)
   }
   else
   {
      # inject the browser calls
      trace(
          what = functionName,
          where = envir,
          at = lapply(strsplit(as.character(steps), ","), as.numeric),
          tracer = browser,
          print = FALSE)

      # unlock the binding if necessary to inject the source references
      lockedBinding <- FALSE

      # remap the source references so that the code injected by trace() is
      # mapped back to the line on which the breakpoint was set.  We need to
      # assign directly to the @.Data internal slot since assignment to the
      # public-facing body of the function will remove the tracing information.
      tryCatch({
         if (bindingIsLocked(functionName, envir))
         {
            unlockBinding(functionName, envir)
            lockedBinding <- TRUE
         }
         body(envir[[functionName]]@.Data) <- .rs.tracedSourceRefs(
            body(envir[[functionName]]@.Data),
            body(envir[[functionName]]@original))
         },
         finally =
         {
            # restore the lock
            if (lockedBinding)
            {
               lockBinding(functionName, envir)
            }
         }
      )
   }
   return(functionName)
})

.rs.addFunction("getUntracedFunction", function(functionName)
{
   envir <- .rs.getEnvironmentOfFunction(functionName)
   if (is.null(envir))
   {
      return(NULL)
   }
   fun <- get(functionName, mode="function", envir=envir)
   if (isS4(fun) && class(fun) == "functionWithTrace")
   {
      fun <- fun@original
   }
   return(fun)
})

.rs.addFunction("getFunctionSourceRefs", function(functionName)
{
   fun <- .rs.getUntracedFunction(functionName)
   if (is.null(fun))
   {
      return(NULL)
   }
   attr(fun, "srcref")
})

.rs.addFunction("getFunctionSourceCode", function(functionName)
{
   paste(capture.output(.rs.getFunctionSourceRefs(functionName)), collapse="\n")
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

