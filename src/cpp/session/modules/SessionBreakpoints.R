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

# given a function name and filename, find the environment that contains a
# function with the given name that originated from the given file.
.rs.addFunction("getEnvironmentOfFunction", function(objName, fileName)
{
   env <- globalenv()
   while (environmentName(env) != "R_EmptyEnv")
   {
      # if the function with the given name exists in this environment...
      if (!is.null(env) &&
          exists(objName, env, mode = "function", inherits = FALSE))
      {
         # we need the source reference to look up the filename; we may need to
         # access the original copy of a traced function to get this
         fun <- get(objName, env)
         if (isS4(fun) && class(fun) == "functionWithTrace")
         {
            fun <- fun@original
         }
         srcref <- attr(fun, "srcref")
         if (!is.null(srcref))
         {
            # get the name of the file from which the function originated, and
            # trim off the trailing space; see if it matches the filename
            srcfile <- capture.output(attr(srcref, "srcfile"))
            srcfile <- substr(srcfile, 1, nchar(srcfile) - 1)
            if (normalizePath(srcfile) == normalizePath(fileName))
            {
               return (env)
            }
         }
      }
      env <- parent.env(env)
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

# given a traced function body and the original function body, recursively copy
# the source references from the original body to the traced body, adding
# source references to the injected trace code from the line being traced
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
    if (length(funBody[[idx]]) != length(originalFunBody[[idx]]) ||
        sum(funBody[[idx]] != originalFunBody[[idx]]) > 0)
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
.rs.addFunction("getFunctionSteps", function(
   functionName,
   fileName,
   lineNumbers)
{
   fun <- .rs.getUntracedFunction(functionName, fileName)
   funBody <- body(fun)

   # attempt to find the end line of the function
   funStartLine <- 0
   funEndLine <- 0
   funSrcRef <- attr(fun, "srcref")
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

.rs.addFunction("setFunctionBreakpoints", function(
   functionName,
   fileName,
   steps)
{
   envir <- .rs.getEnvironmentOfFunction(functionName, fileName)
   if (is.null(envir))
   {
      return (NULL)
   }
   if (length(steps) == 0 || nchar(steps) == 0)
   {
      # Restore the function to its original state. Note that trace/untrace
      # emit messages when they act on a function in a package environment; hide
      # those messages since they're just noise to the user.
      suppressMessages(untrace(
         what = functionName,
         where = envir))
   }
   else
   {
      # inject the browser calls
      suppressMessages(trace(
          what = functionName,
          where = envir,
          at = lapply(strsplit(as.character(steps), ","), as.numeric),
          tracer = browser,
          print = FALSE))

      # unlock the binding if necessary to inject the source references;
      # bindings are often locked in package environments
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

.rs.addFunction("getUntracedFunction", function(functionName, fileName)
{
   envir <- .rs.getEnvironmentOfFunction(functionName, fileName)
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

.rs.addFunction("getFunctionSourceRefs", function(functionName, fileName)
{
   fun <- .rs.getUntracedFunction(functionName, fileName)
   if (is.null(fun))
   {
      return(NULL)
   }
   attr(fun, "srcref")
})

.rs.addFunction("getFunctionSourceCode", function(functionName, fileName)
{
   paste(capture.output(
      .rs.getFunctionSourceRefs(functionName, fileName)), collapse="\n")
})

.rs.addFunction("sourceForDebugging", function(
   fileName,
   topLevelBreakpoints,
   functionBreakpoints)
{
   # establish state for debugging sources
   topDebugState <- new.env()

   # save arguments for later use below
   assign("currentDebugFile", fileName, topDebugState)
   assign("topLevelBreakpoints", topLevelBreakpoints, topDebugState)
   assign("functionBreakpoints", functionBreakpoints, topDebugState)

   # parse the file and store the parsed expressions
   assign("parsedForDebugging",
      suppressWarnings(parse(fileName)), topDebugState)

   # cache debug state inside the RStudio tools environment
   .rs.setVar("topDebugState", topDebugState)

   return(TRUE)
})


# Executes a portion of a previously parsed file, pausing on breakpoints.
#
# Modes (input)
# 0 - single step (execute one expression)
# 1 - run (execute until a breakpoint is hit)
# 2 - stop (abort execution)
#
# Results (output)
# 0 - paused for user (on a breakpoint or step)
# 1 - paused on a function breakpoint injection site
# 2 - evaluation finished
#
# Note that there is special behavior on the client attached to the name of
# this function.
.rs.addFunction("executeDebugSource", function(fileName, step, mode)
{
   topDebugState <- .rs.topDebugState
   parsed <- topDebugState[["parsedForDebugging"]]
   stepBegin <- step
   srcref <- integer()
   executionState <- 0L  # Paused for user
   needsBreakpointInjection <- FALSE
   if (mode == 2)
   {
      executionState <- 2L   # Finished
   }
   else repeat
   {
      # get the expression to evaluate and its location in the file
      expr <- parsed[[step]]
      srcref <- attr(parsed, "srcref")[[step]]

      # if this is a top-level breakpoint and not the step we were asked to
      # execute, don't execute it
      if (srcref[1] %in% topDebugState[["topLevelBreakpoints"]] &&
          step > stepBegin)
      {
         break
      }

      # evaluate it!
      eval(expr, envir = globalenv())

      # move to the next expression
      step <- step + 1L

      if (step > length(parsed))
      {
         executionState <- 2L  # Finished
      }

      # if there are any function breakpoints inside the expression, pause and
      # let the client evaluate them
      for (bp in topDebugState[["functionBreakpoints"]])
      {
         if (bp$line >= srcref[1] && bp$line <= srcref[3])
         {
            needsBreakpointInjection <- TRUE
            if (mode == 1 && executionState != 2)
            {
               executionState <- 1L  # Paused for breakpoint injection
            }
            break
         }
      }
      if (executionState == 1 || executionState == 2) break

      if (mode == 0)  # Single-step execution mode
      {
         srcref <- attr(parsed, "srcref")[[step]]
         break
      }
   }

   if (executionState == 2)
   {
      if (step > length(parsed))
         message(paste("Sourced '", topDebugState[["currentDebugFile"]],
                       "' with debug information", sep=""))
      else
         message(paste("Source of '", topDebugState[["currentDebugFile"]],
                       "' aborted", sep=""))
   }

   if (length(srcref) > 0)
   {
      return(list(
         step = .rs.scalar(step),
         state = .rs.scalar(executionState),
         needs_breakpoint_injection = .rs.scalar(needsBreakpointInjection),
         line_number = .rs.scalar(srcref[1]),
         end_line_number = .rs.scalar(srcref[3]),
         character_number = .rs.scalar(srcref[5]),
         end_character_number = .rs.scalar(srcref[6])))
   }
   else
   {
      return(list(
         step = .rs.scalar(0L),
         state = .rs.scalar(executionState),
         needs_breakpoint_injection = .rs.scalar(needsBreakpointInjection),
         line_number = .rs.scalar(0L),
         end_line_number = .rs.scalar(0L),
         character_number = .rs.scalar(0L),
         end_character_number = .rs.scalar(0L)))
   }
})

.rs.addJsonRpcHandler("set_function_breakpoints", function(
   functionName,
   fileName,
   steps)
{
   .rs.setFunctionBreakpoints(functionName, fileName, steps)
})

.rs.addJsonRpcHandler("get_function_steps", function(
   functionName,
   fileName,
   lineNumbers)
{
   results <- .rs.getFunctionSteps(functionName, fileName, lineNumbers)
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

.rs.addJsonRpcHandler("source_for_debugging", function(
   fileName,
   topLevelBreakpoints,
   functionBreakpoints)
{
   .rs.sourceForDebugging(fileName, topLevelBreakpoints, functionBreakpoints)
})

.rs.addJsonRpcHandler("execute_debug_source", function(fileName, step, mode)
{
   .rs.executeDebugSource(fileName, step[[1]], mode)
})

