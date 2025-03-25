#
# SessionErrors.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

.rs.addFunction("isSourceCall", function(call)
{
   symbols <- list(
      quote(source),
      quote(debugSource)
   )
   
   fun <- call[[1L]]
   for (symbol in symbols)
      if (identical(fun, symbol))
         return(TRUE)
   
   FALSE
})

.rs.addFunction("recordTraceback", function(userOnly, errorReporter)
{
   calls <- sys.calls()
   foundUserCode <- FALSE
   inSource <- FALSE
   
   # Drop calls associated with the emission of the error. Note that the
   # calls on the stack will differ depending on whether the error was
   # emitted via an R-level call to `stop()`, versus a call to `Rf_error`
   # or `Rf_errorcall` at the C level.
   recordTracebackSym <- as.symbol(".rs.recordTraceback")
   n <- length(calls)
   if (identical(calls[[n]][[1L]], recordTracebackSym))
   {
      n <- n - 1L
      call <- calls[[n]]
      if (is.call(call) && length(call) == 1L)
      {
         call <- call[[1L]]
         if (is.call(call) && identical(call[[1L]], recordTracebackSym))
         {
            n <- n - 1L
         }
      }
   }
   
   # If there's only one call on the stack, this appears to be an error
   # in a top-level code execution -- just drop it.
   if (n == 1L)
      return(NULL)
   
   # create the traceback for the client
   stack <- lapply(seq_len(n), function(i)
   {
      call <- calls[[i]]
      
      # don't show debugger-hidden functions
      if (isTRUE(attr(call[[1L]], "hideFromDebugger")))
      {
         # RStudio injects source-refs on the top call, which might be
         # its own error handler in some cases -- basically simulating
         # source references so we can see where the error occurred.
         # Detect this scenario and update the 'foundUserCode' flag.
         if (i == n && !foundUserCode)
         {
            srcref <- attr(call[[1L]], "srcref", exact = TRUE)
            srcfile <- attr(srcref, "srcfile", exact = TRUE)
            if (is.null(srcfile))
               return(NULL)
         }
         else
         {
            return(NULL)
         }
      }

      # we want to ignore the first user code entry after a call to source(),
      # since that call happens at the top level
      isSourceCall <- FALSE
      if (.rs.isSourceCall(call))
      {
         isSourceCall <- TRUE
         inSource <<- TRUE
      }
      
      srcref <- attr(call, "srcref")
      srcfile <- ""
      if (!is.null(srcref))
      {
         fileattr <- attr(srcref, "srcfile")
         srcfile <- fileattr$filename
         if (!is.null(srcfile))
         {
            if (inSource && !isSourceCall)
               inSource <<- FALSE
            else
               foundUserCode <<- TRUE
         }
      }
      else
      {
         srcref <- rep(0L, 8)
      }
     
      # don't display more than 4 lines of a long expression
      lines <- .rs.deparseCall(call)
      
      if (length(lines) > 4) 
      {
         lines <- lines[1:4]
         lines[4] <- paste(lines[4], "...")
      }

      c(
         list(
            func = .rs.scalar(paste(lines, collapse = "\n")),
            file = .rs.scalar(srcfile)
         ),
         .rs.lineDataList(srcref)
      )
         
   })

   # remove hidden entries from the stack
   stack <- Filter(Negate(is.null), stack)

   # look for python entry point and fill in the stack from reticulate if we can
   amended_stack <- list()
   lapply(stack, function(x)
   {
      func <- x$func
      if (.rs.hasPythonStackTrace(func))
      {
         foundUserCode <<- TRUE # python code always includes src references
         python_stack_trace <- .rs.getActivePythonStackTrace()
         for (item in python_stack_trace)
            amended_stack[[length(amended_stack) + 1]] <<- item
      }
      else
      {
         amended_stack[[length(amended_stack) + 1]] <<- x
      }
   })
   
   # if we found user code (or weren't looking for it), tell the client
   if (foundUserCode || !userOnly)
   {
      error <- list(
         frames = amended_stack,
         message = .rs.scalar(geterrmessage())
      )
      errorReporter(error)
   }
})

.rs.addFunction("hasPythonStackTrace", function(func) {
   grepl("^py_call_impl\\(", func) && 
         .rs.isPackageVersionInstalled("reticulate", "0.7.0.9005") &&
         !is.null(reticulate::py_last_error())
})

.rs.addFunction("getActivePythonStackTrace", function() {
   stack <- list()
   # silence errors so anything unexpected doesn't mess with global RStudio error handling
   tryCatch({
      traceback <- reticulate::py_last_error()$traceback
      for (i in 1:length(traceback)) {
         lineData <- .rs.pyStackItemToLineDataList(traceback[[i]])
         if (!is.null(lineData))
            stack[[length(stack) + 1]] <- lineData
      }
   }, error = function(e) NULL)
   stack
})

.rs.addFunction("pyStackItemToLineDataList", function(item) {
   matches <- regmatches(item, regexec('^\\s+\\w+ \\"([^\\"]+)\\", \\w+ (\\d+), \\w+ ([A-Za-z_0-9]+).*$',item))[[1]]
   if (length(matches) == 4) {
      list(
         func = .rs.scalar(matches[[4]]),
         file = .rs.scalar(matches[[2]]),
         line_number = .rs.scalar(matches[[3]]),
         end_line_nubmer = .rs.scalar(matches[[3]]),
         character_number = .rs.scalar(0),
         character_number = .rs.scalar(0)
      ) 
   } else {
      NULL
   }
})

.rs.addFunction("breakOnError", function(userOnly)
{
   calls <- sys.calls()
   if (length(calls) < 4)
      return()

   foundUserCode <- FALSE
   inSource <- FALSE
   if (userOnly)
   {
      for (n in 1:(length(calls) - 1))
      {
         isSourceCall <- FALSE
         if (.rs.isSourceCall(sys.call(n)))
         {
            isSourceCall <- TRUE
            inSource <- TRUE
         }
         func <- .rs.untraced(sys.function(n))
         srcref <- attr(func, "srcref")
         if (!is.null(srcref) && 
             !is.null(attr(srcref, "srcfile")))
         {
            if (inSource && !isSourceCall)
            {
               inSource <- FALSE
            }
            else
            {
               # looks like non-top-level user code--invoke the browser below
               foundUserCode <- TRUE
               break
            }
         }
      }
   }
   if (foundUserCode || !userOnly)
   {
      # The magic value 2 here is derived from the position in the
      # stack where this error handler resides relative to where we expect
      # the user code that raised the error to be. These will need to be
      # adjusted if evaluation layers are added or removed between the
      # root error handler (set in options(error=...)) and this function.
      frame <- length(sys.frames()) - 2

      # move the frame backwards if it's on stop or stopifnot
      if (identical(deparse(sys.call(frame)[[1]]), "stop"))
         frame <- frame - 1
      if (identical(deparse(sys.call(frame)[[1]]), "stopifnot"))
         frame <- frame - 1

      eval(substitute(browser(skipCalls = pos), 
                      list(pos = (length(sys.frames()) - frame) + 2)),
           envir = sys.frame(frame))
   }
},
attrs = list(hideFromDebugger = TRUE))

.rs.addFunction("enqueueError", function(err) {
  .rs.enqueClientEvent("unhandled_error", err)
})

.rs.addFunction("recordAnyTraceback", function()
{
   .rs.recordTraceback(FALSE, .rs.enqueueError)
},
attrs = list(hideFromDebugger = TRUE,
             errorHandlerType = "traceback"))

.rs.addFunction("recordUserTraceback", function()
{
   .rs.recordTraceback(TRUE, .rs.enqueueError)
},
attrs = list(hideFromDebugger = TRUE,
             errorHandlerType = "traceback"))

.rs.addFunction("breakOnAnyError", function()
{
   .rs.breakOnError(FALSE)
},
attrs = list(hideFromDebugger = TRUE, 
             errorHandlerType = "break"))

.rs.addFunction("breakOnUserError", function()
{
   .rs.breakOnError(TRUE)
},
attrs = list(hideFromDebugger = TRUE,
             errorHandlerType = "break"))

.rs.addFunction("setErrorManagementType", function(type, userOnly)
{
   if (identical(type, "message"))
      options(error = NULL)
   else if (identical(type, "traceback") && userOnly)
      options(error = .rs.recordUserTraceback) 
   else if (identical(type, "traceback") && !userOnly)
      options(error = .rs.recordAnyTraceback)
   else if (identical(type, "break") && userOnly)
      options(error = .rs.breakOnUserError)
   else if (identical(type, "break") && !userOnly)
      options(error = .rs.breakOnAnyError)
})
