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

.rs.addFunction("tracebackCalls", function()
{
   status <- sys.status()
   
   for (n in seq_along(status$sys.calls))
   {
      fn <- sys.function(n)
      
      # Check for a registered RStudio error handler.
      type <- attr(fn, "rstudioErrorHandler", exact = TRUE)
      if (identical(type, TRUE))
      {
         n <- n - 1L
         break
      }
      
      # Check for other potential error handlers on the stack.
      if (identical(fn, .rs.globalCallingHandlers.onError))
      {
         n <- n - 1L
         break
      }
      else if (identical(fn, .handleSimpleError))
      {
         call <- status$sys.calls[[n]]
         if (is.null(attr(call, "srcref", exact = TRUE)))
         {
            n <- n - 1L
            break
         }
      }
      else if (identical(fn, stop))
      {
         break
      }
   }
   
   head(status$sys.calls, n)
})

.rs.addFunction("recordTraceback", function(userOnly, errorReporter)
{
   calls <- .rs.tracebackCalls()
   n <- length(calls)
   if (n <= 1L)
      return(NULL)
   
   # create the traceback for the client
   stack <- lapply(seq_len(n), function(i)
   {
      call <- calls[[i]]
      
      # don't show debugger-hidden functions
      if (isTRUE(attr(call[[1L]], "hideFromDebugger")))
         return(NULL)

      # retrieve call
      lines <- .rs.deparseCall(call)
      
      # don't display more than 4 lines of a long expression
      if (length(lines) > 4) 
      {
         lines <- lines[1:4]
         lines[4] <- paste(lines[4], "...")
      }

      srcref <- .rs.nullCoalesce(
         attr(call, "srcref", exact = TRUE),
         rep.int(0L, 8L)
      )
      
      srcfile <- attr(srcref, "srcfile", exact = TRUE)
      filename <- .rs.nullCoalesce(srcfile$filename, "")
      
      c(
         list(
            func = .rs.scalar(paste(lines, collapse = "\n")),
            file = .rs.scalar(filename)
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
   if (n >= 2 || !userOnly)
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
attrs = list(rstudioErrorHandler = TRUE,
             hideFromDebugger = TRUE,
             errorHandlerType = "traceback"))

.rs.addFunction("recordUserTraceback", function()
{
   .rs.recordTraceback(TRUE, .rs.enqueueError)
},
attrs = list(rstudioErrorHandler = TRUE,
             hideFromDebugger = TRUE,
             errorHandlerType = "traceback"))

.rs.addFunction("breakOnAnyError", function()
{
   .rs.breakOnError(FALSE)
},
attrs = list(rstudioErrorHandler = TRUE,
             hideFromDebugger = TRUE, 
             errorHandlerType = "break"))

.rs.addFunction("breakOnUserError", function()
{
   .rs.breakOnError(TRUE)
},
attrs = list(rstudioErrorHandler = TRUE,
             hideFromDebugger = TRUE,
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
