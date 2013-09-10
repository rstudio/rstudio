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
.rs.addFunction("getEnvironmentOfFunction", function(
   objName, fileName, packageName)
{
   isPackage <- nchar(packageName) > 0

   # when searching specifically for a function in a package, search from the
   # package namespace to the global environment (considers package imports and
   # non-exported functions); otherwise, search from the global environment to
   # the empty namespace
   lastEnvir <- if (isPackage) "R_GlobalEnv" else "R_EmptyEnv"
   env <- if (isPackage)
             asNamespace(packageName)
          else
             globalenv()
   while (environmentName(env) != lastEnvir)
   {
      # if the function with the given name exists in this environment...
      if (!is.null(env) &&
          exists(objName, env, mode = "function", inherits = FALSE))
      {
         # we need the source reference to look up the filename; we may need to
         # access the original copy of a traced function to get this
         srcref <- .rs.getSrcref(get(objName, env))
         if (!is.null(srcref))
         {
            # get the name of the file from which the function originated, and
            # trim off the trailing space; see if it matches the filename
            fileattr <- attr(srcref, "srcfile")
            if (normalizePath(fileattr$filename) == normalizePath(fileName))
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
    # check to see if this is one of the several types of objects we can't do
    # equality testing for. Note that these object types are all leaf nodes in
    # the parse tree, so it's safe to stop recursion here.
    if (is.null(funBody[[idx]]) || 
        identical(funBody[[idx]], NA) || 
        is.pairlist(funBody[[idx]])) 
       next

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
   packageName,
   lineNumbers)
{
   fun <- .rs.getUntracedFunction(functionName, fileName, packageName)
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
         name=.rs.scalar(functionName),
         line=.rs.scalar(lineNumber),
         at=.rs.scalar(paste(steps, collapse=",")))
   })
})

.rs.addFunction("setFunctionBreakpoints", function(
   functionName,
   envir,
   steps)
{
   if (length(steps) == 0 || nchar(steps) == 0)
   {
      # Restore the function to its original state. Note that trace/untrace
      # emit messages when they act on a function in a package environment; hide
      # those messages since they're just noise to the user.
      fun <- get(functionName, envir = envir)
      if (.rs.isTraced(fun))
      {
         suppressMessages(untrace(
            what = functionName,
            where = envir))
      }
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

.rs.addFunction("getUntracedFunction", function(
   functionName, fileName, packageName)
{
   envir <- .rs.getEnvironmentOfFunction(functionName, fileName, packageName)
   if (is.null(envir))
   {
      return(NULL)
   }
   .rs.untraced(get(functionName, mode="function", envir=envir))
})

.rs.addFunction("getFunctionSourceRefs", function(
   functionName, fileName, packageName)
{
   fun <- .rs.getUntracedFunction(functionName, fileName, packageName)
   if (is.null(fun))
   {
      return(NULL)
   }
   attr(fun, "srcref")
})

.rs.addFunction("getFunctionSourceCode", function(
   functionName, fileName, packageName)
{
   paste(capture.output(
      .rs.getFunctionSourceRefs(functionName, fileName, packageName)),
      collapse="\n")
})

.rs.addGlobalFunction("debugSource", function(
   path, encoding="unknown", echo=FALSE)
{
   # establish state for debugging sources
   topDebugState <- new.env()

   # parse the file and store the parsed expressions
   topDebugState$currentDebugFile <- path
   topDebugState$parsedForDebugging <- suppressWarnings(
      parse(path, encoding=encoding))
   topDebugState$currentDebugStep <- 0L
   topDebugState$currentDebugSrcref <- rep(0L, 8)
   topDebugState$echo <- echo

   # cache debug state inside the RStudio tools environment
   .rs.setVar("topDebugState", topDebugState)
})


# Executes a portion of a previously parsed file, pausing on breakpoints. Relies
# on state created by debugSource.
#
# Modes (input)
# 0 - single step (execute one expression)
# 1 - run (execute until a breakpoint is hit)
# 2 - stop (abort execution)
# 3 - resume (continue execution after pausing for breakpoint injection)
#
# Results (output)
# 0 - paused for user (on a breakpoint or step)
# 1 - paused on a function breakpoint injection site
# 2 - evaluation finished
#
# Note that there is special behavior on the client attached to the name of
# this function.
.rs.addFunction("executeDebugSource", function(
   fileName, topBreakLines, functionBreakLines, step, mode)
{
   # set up state for this evaluation session
   topDebugState <- environment()
   parsed <- expression()
   stepBegin <- step
   srcref <- rep(0L, 8)
   executionState <- 0L  # Paused for user
   needsBreakpointInjection <- FALSE
   if (step == 0)
      step <- step + 1L

   # check to ensure the internal top-level debugging state environment is
   # present; if not, exit immediately
   if (exists(".rs.topDebugState"))
   {
      topDebugState <- .rs.topDebugState
      parsed <- topDebugState$parsedForDebugging
   }
   else
   {
      mode <- 2  # Stop
   }

   if (mode == 2)
   {
      executionState <- 2L   # Finished
   }
   else repeat
   {
      # get the expression to evaluate and its location in the file
      expr <- parsed[[step]]
      srcref <- attr(parsed, "srcref")[[step]]

      # Pause if this is a top-level breakpoint. We want to hit the breakpoint
      # if either it isn't the step we were asked to execute or this is a
      # resumed step (so we can't have already hit the breakpoint).
      if (srcref[1] %in% topBreakLines && (step > stepBegin || mode == 3))
      {
         break
      }

      # evaluate it, with echo if desired
      if (topDebugState$echo)
         print(srcref)
      tryCatch(
         {
            result <- withVisible(eval(expr, envir=globalenv()))
            if (topDebugState$echo)
            {
               if (result$visible)
                  print(result$value)
               writeLines("")
            }
         },
         error = function(e)
         {
            # If an error is encountered, print it and move on--we still need to
            # send debug state to the client, and evaluate the rest of the file
            # (consider: this doesn't match the behavior of source(), which
            # stops evaluation when an error is encountered.)
            print(e)
         }
      )

      # move to the next expression
      step <- step + 1L
      if (step > length(parsed))
      {
         executionState <- 2L  # Finished
      }

      # if there are any function breakpoints inside the expression, pause and
      # let the client evaluate them
      for (bp in functionBreakLines)
      {
         if (bp >= srcref[1] && bp <= srcref[3])
         {
            needsBreakpointInjection <- TRUE
            # if running or resuming and not finished
            if ((mode == 1 || mode == 3) && executionState != 2)
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

   # when finished running, clean up any debug state we were holding on to
   if (executionState == 2)
   {
      if (exists(".rs.topDebugState"))  .rs.clearVar("topDebugState")
   }
   # if still running, save the step and line so we can emit them to the client
   # as session information
   else if (exists(".rs.topDebugState"))
   {
      .rs.topDebugState$currentDebugStep <- step
      .rs.topDebugState$currentDebugSrcref <- srcref
   }

   return(c(list(
               step = .rs.scalar(step),
               state = .rs.scalar(executionState),
               needs_breakpoint_injection = 
                     .rs.scalar(needsBreakpointInjection)),
            .rs.lineDataList(srcref)))
})

.rs.addJsonRpcHandler("get_function_steps", function(
   functionName,
   fileName,
   packageName,
   lineNumbers)
{
   .rs.getFunctionSteps(functionName, fileName, packageName, lineNumbers)
})

.rs.addJsonRpcHandler("execute_debug_source", function(
   fileName,
   topBreakLines,
   functionBreakLines,
   step,
   mode)
{
   .rs.executeDebugSource(
      fileName,
      topBreakLines,
      functionBreakLines,
      step,
      mode)
})

