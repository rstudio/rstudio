#
# SessionBreakpoints.R
#
# Copyright (C) 2020 by RStudio, PBC
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
   objName <- .rs.unquote(objName)
   
   # assume fileName is UTF-8 encoded unless it's already got a labelled
   # encoding
   if (Encoding(fileName) == "unknown") {
      Encoding(fileName) <- "UTF-8"
   }

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
            # trim off the trailing space
            fileattr <- attr(srcref, "srcfile")

            # if the srcref has a srcfile, resolve it (if it exists) and
            # compare with the input we were given
            if (!is.null(fileattr) &&
                (normalizePath(fileattr$filename, mustWork = FALSE) == 
                 normalizePath(fileName)))
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

# Given the body of the function, return the substeps of the function on which
# a breakpoint was set (not the substep of the breakpoint itself).
.rs.addFunction("findBreakpointSteps", function(funBody)
{
   if (typeof(funBody) != "language")
   {
      return(NULL)
   }
   for (idx in 1:length(funBody))
   {
      # if this is a doTrace call, we found a breakpoint; stop recursion here
      if (is.call(funBody[[idx]]) && 
          identical(as.character(funBody[[idx]][[1]])[[1]], ".doTrace"))
      {
         return(idx + 1)
      }
      nestedSteps <- .rs.findBreakpointSteps(funBody[[idx]])
      if (!is.null(nestedSteps))
      {
         return(c(idx, nestedSteps))
      }
   }
})

# Given a function, return the function with all of the breakpoints removed.
.rs.addFunction("removeBreakpoints", function(fun)
{
   repeat
   {
      # Find the step on which a breakpoint was set
      inner <- .rs.findBreakpointSteps(body(fun))
      if (length(inner) < 2)
         break;

      # Replace the outer expression (which contains the breakpoint) with the
      # inner one (which contains the expression formerly wrapped by the
      # breakpoint)
      outer <- inner[1:(length(inner)-1)]
      body(fun)[[outer]] <- body(fun)[[inner]]
   }
   fun
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
    # Check to see if this is one of the several types of objects we can't do
    # equality testing for. Note that these object types are all leaf nodes in
    # the parse tree, so it's safe to stop recursion here. Also note that we
    # can't use the helpful is.na() here since that function emits warnings
    # for some types of objects in the parse tree.
    if (is.null(funBody[[idx]]) || 
        identical(funBody[[idx]], NA) || 
        identical(funBody[[idx]], NA_character_) || 
        identical(funBody[[idx]], NA_complex_) || 
        identical(funBody[[idx]], NA_integer_) || 
        identical(funBody[[idx]], NA_real_) || 
        identical(funBody[[idx]], NaN) ||
        is.pairlist(funBody[[idx]])) 
       next

    # if this expression was replaced by trace(), copy the source references
    # from the original expression over each expression injected by trace()
    if (length(funBody[[idx]]) != length(originalFunBody[[idx]]) ||
        isTRUE(sum(funBody[[idx]] != originalFunBody[[idx]]) > 0))
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

.rs.addFunction("getFunctionSteps", function(fun, functionName, lineNumbers)
{
   funBody <- body(fun)
   lineNumbers <- unique(lineNumbers)

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

# this function is used to get the steps in the given function that are
# associated with the given line number, using the function's source
# references.
.rs.addFunction("getSteps", function(
   functionName,
   fileName,
   packageName,
   lineNumbers)
{
   .rs.getFunctionSteps(
                  .rs.getUntracedFunction(functionName, fileName, packageName),
                  functionName,
                  lineNumbers)
})

.rs.addFunction("setFunctionBreakpoints", function(
   functionName,
   envir,
   steps)
{
   functionName <- .rs.unquote(functionName)
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
   functionName <- .rs.unquote(functionName)
   envir <- .rs.getEnvironmentOfFunction(functionName, fileName, packageName)
   if (is.null(envir))
   {
      return(NULL)
   }
   .rs.untraced(get(functionName, mode = "function", envir = envir))
})

.rs.addFunction("getFunctionSourceRefs", function(
   functionName, fileName, packageName)
{
   functionName <- .rs.unquote(functionName)
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
   functionName <- .rs.unquote(functionName)
   paste(capture.output(
      .rs.getFunctionSourceRefs(functionName, fileName, packageName)),
      collapse="\n")
})

# Parses and executes a file for debugging
.rs.addFunction("executeDebugSource", function(fileName, encoding, breaklines, local)
{
   envir <-
      if (isTRUE(local)) {
         # If local is TRUE, we take the first frame as it's what was artificially
         # constructed in earlier code
         sys.frames()[[1]]
      } else if (identical(local, FALSE)) {
         .GlobalEnv
      } else if (is.environment(local)) {
         local
      } else {
         stop("'local' must be TRUE, FALSE or an environment")
      }

   # Create a function containing the parsed contents of the file
   env <- new.env(parent = emptyenv())
   env$fun <- .rs.makeSourceEquivFunction(fileName, encoding, envir)
   breakSteps <- character()

   # Inject the breakpoints
   if (length(breaklines) > 0)
   {
      steps <- .rs.getFunctionSteps(env$fun, "fun", breaklines)
      breakSteps <- unlist(lapply(steps, function(step) step$at))
      suppressWarnings(.rs.setFunctionBreakpoints(
            "fun", env, lapply(steps, function(step) { step$at } )))
   }

   # Run it!
   env$fun()

   # We injected function breakpoints above, but we don't want to leave them
   # in the in-memory copies of the functions. Replay the assignment that 
   # created the original copy of the function. The client will set proper
   # breakpoints on the functions later.
   env$fun <- .rs.removeBreakpoints(env$fun)
   breakSteps <- breakSteps[nchar(breakSteps) > 6]
   for (steps in breakSteps) {
      step <- as.numeric(strsplit(breakSteps, ",")[[1]][3]) 
      op <- deparse(body(env$fun)[[2]][[2]][[step]][[1]]) 
      if (op == "<-" || op == "=") 
         eval(body(env$fun)[[2]][[2]][[step]], envir = globalenv())
   }

   return(NULL)

}, attrs = list(hideFromDebugger = TRUE))

.rs.addFunction("getShinyFunction", function(name, where)
{
   if (is(where, "refClass"))
      where$field(name)
   else 
      get(name, where)
})

.rs.addFunction("setShinyFunction", function(name, where, val)
{
   if (is(where, "refClass"))
      where$field(name, val)
   else
      assign(name, val, where)
})

.rs.addFunction("setShinyBreakpoints", function(name, where, lines)
{
   # Create a blank environment and load the function into it
   env <- new.env(parent = emptyenv())
   env$fun <- .rs.getShinyFunction(name, where) 

   # Get the steps of the function corresponding to the lines on
   # which breakpoints are to be set, and set breakpoints there
   steps <- .rs.getFunctionSteps(env$fun, "fun", lines)

   suppressWarnings(.rs.setFunctionBreakpoints(
         "fun", env, lapply(steps, function(step) { step$at } )))

   # Store the updated copy of the function back into Shiny
   .rs.setShinyFunction(name, where, env$fun)
})

# Given a filename, creates a source-equivalent function: a function that,
# when executed, has the same effect as sourcing the file.
.rs.addFunction("makeSourceEquivFunction", function(filename, encoding, envir = globalenv())
{
   content <- suppressWarnings(parse(filename, encoding = encoding))

   # Create an empty function to host the expressions in the file
   fun <- function() 
   {
      evalq({ 1 }, envir = envir)
   }

   # Copy each statement from the file into the eval body of the function
   for (i in 1:length(content)) {
     body(fun)[[2]][[2]][[i + 1]] <- content[[i]]
   }

   # Set up the source references 
   refs <- attr(content, "srcref")
   lastref <- length(refs)
   attr(body(fun), "srcfile") <- attr(content, "srcfile")

   # Simulate a source reference that contains the whole function by 
   # combining the first and last source references of each statement in 
   # the function
   ref <- structure(c(refs[[1]][1], refs[[1]][2], 
                      refs[[lastref]][3], refs[[lastref]][[4]], 
                      refs[[1]][5], refs[[lastref]][6], 
                      refs[[1]][1], refs[[lastref]][3]), 
                    srcfile = attr(content, "srcfile"), 
                    class = "srcref")
   attr(body(fun), "srcref")[[2]] <- ref
   linerefs <- list(attr(content, "srcref")[[1]])
   for (i in 1:length(content)) {
      linerefs[[i + 1]] <- attr(content, "srcref")[[i]]
   }
   attr(body(fun)[[2]][[2]], "srcref") <- linerefs
   attr(fun, "srcref") <- ref
   return(fun)
})

.rs.addGlobalFunction("debugSource", function(fileName,
                                              echo = FALSE,
                                              encoding = "unknown",
                                              local = FALSE)
{
   # NYI: Consider whether we need to implement source with echo for debugging.
   # This would likely involve injecting print statements into the generated
   # source-equivalent function.

   # convert filename to UTF-8 before proceeding 
   invisible(.Call("rs_debugSourceFile", enc2utf8(fileName), encoding, local))
})

# Parameters expected to be in environment:
# where - environment or reference object 
# name - name of function or reference field name
# label - friendly label for function to show in callstack
.rs.addGlobalFunction("registerShinyDebugHook", function(params)
{
   # Get the function from storage in Shiny, and remove any breakpoints it may
   # already contain
   fun <- .rs.getShinyFunction(params$name, params$where)
   fun <- .rs.removeBreakpoints(fun)
   params$expr <- body(fun)

   # Copy source refs to the body of the function 
   attr(fun, "srcref") <- attr(body(fun), "wholeSrcref")
   params$fun <- fun

   # Register the function with RStudio (may set breakpoints)
   .Call("rs_registerShinyFunction", params)
})

.rs.addJsonRpcHandler("get_function_steps", function(
   functionName,
   fileName,
   packageName,
   lineNumbers)
{
   .rs.getSteps(functionName, fileName, packageName, lineNumbers)
})

.rs.addFunction("haveAdvancedSteppingCommands", function() {
   getRversion() >= "3.1" && .rs.haveRequiredRSvnRev(63400)
})

.rs.addFunction("unquote", function(strings)
{
   tryCatch(
      .rs.unquoteImpl(strings),
      error = function(e) strings
   )
})

.rs.addFunction("unquoteImpl", function(strings)
{
   if (!is.character(strings))
      return(strings)
   parsed <- parse(text = strings)
   vapply(parsed, as.character, FUN.VALUE = character(1))
})

