#
# SessionErrors.R
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

.rs.addFunction("isSourceCall", function(call)
{
   fun <- deparse(call[[1]])
   return (fun == "source" ||
           fun == "debugSource")
})

.rs.addFunction("recordTraceback", function(userOnly)
{
   calls <- sys.calls()
   foundUserCode <- FALSE
   inSource <- FALSE

   # When this handler is invoked for an unhandled error happening at
   # the top level, there are four calls on the stack:
   # 1. This function
   # 2. The anonymous error handler (set via options below)
   # 3. The error invoker (e.g. stop)
   # 4. The function from which the error was raised
   # So we want there to be at least 5 calls on the stack--otherwise the error
   # is likely to be top-level.
   if (length(calls) < 5)
      return()

   # create the traceback for the client
   stack <- lapply(calls[1:(length(calls) - 2)], function(call)
   {
      # don't show debugger-hidden functions
      if (isTRUE(attr(call[[1]], "hideFromDebugger")))
         return(NULL)

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
         srcref <- rep(0L, 8)
     
      # don't display more than 4 lines of a long expression
      lines <- deparse(call)
      if (length(lines) > 4) 
      {
         lines <- lines[1:4]
         lines[4] <- paste(lines[4], "...")
      }

      c (list(func = .rs.scalar(paste(lines, collapse="\n")),
              file = .rs.scalar(srcfile)),
         .rs.lineDataList(srcref))
   })

   # remove hidden entries from the stack
   stack <- stack[!sapply(stack, is.null)]

   # if we found user code (or weren't looking for it), tell the client
   if (foundUserCode || !userOnly)
   {
      event <- list(
         frames = stack,
         message = .rs.scalar(geterrmessage()))
      .rs.enqueClientEvent("unhandled_error", event)
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

.rs.addFunction("recordAnyTraceback", function()
{
   .rs.recordTraceback(FALSE)
},
attrs = list(hideFromDebugger = TRUE,
             errorHandlerType = 1L))

.rs.addFunction("recordUserTraceback", function()
{
   .rs.recordTraceback(TRUE)
},
attrs = list(hideFromDebugger = TRUE,
             errorHandlerType = 1L))

.rs.addFunction("breakOnAnyError", function()
{
   .rs.breakOnError(FALSE)
},
attrs = list(hideFromDebugger = TRUE, 
             errorHandlerType = 2L))

.rs.addFunction("breakOnUserError", function()
{
   .rs.breakOnError(TRUE)
},
attrs = list(hideFromDebugger = TRUE,
             errorHandlerType = 2L))

.rs.addFunction("setErrorManagementType", function(type, userOnly)
{
   if (type == 0)
      options(error = NULL)
   else if (type == 1 && userOnly)
      options(error = .rs.recordUserTraceback) 
   else if (type == 1 && !userOnly)
      options(error = .rs.recordAnyTraceback)
   else if (type == 2 && userOnly)
      options(error = .rs.breakOnUserError)
   else if (type == 2 && !userOnly)
      options(error = .rs.breakOnAnyError)
})
