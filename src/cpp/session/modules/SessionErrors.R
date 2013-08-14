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

.rs.addFunction("recordTraceback", function(userOnly)
{
   calls <- sys.calls()
   foundUserCode <- FALSE

   # when this handler is invoked for an unhandled error that didn't happen at
   # the top level, there are at least four calls on the stack:
   # 1. this function
   # 2. the anonymous error handler (set via options below)
   # 3. the error invoker (e.g. stop)
   # 4. the function from which the error was raised
   if (length(calls) < 4)
      return()

   # create the traceback for the client
   stack <- lapply(calls[1:(length(calls) - 2)], function(call)
   {
      srcref <- attr(call, "srcref")
      srcfile <- ""
      if (!is.null(srcref))
      {
         fileattr <- attr(srcref, "srcfile")
         srcfile <- fileattr$filename
         if (!is.null(srcfile))
            foundUserCode <<- TRUE
      }
      else
         srcref <- rep(0L, 8)
      c (list(func = .rs.scalar(deparse(call)),
              file = .rs.scalar(srcfile)),
         .rs.lineDataList(srcref))
   })
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
   foundUserCode <- FALSE
   if (userOnly)
   {
      for (n in 1:(length(calls) - 1))
      {
         func <- .rs.untraced(sys.function(n))
         srcref <- attr(func, "srcref")
         if (!is.null(srcref) && 
             !is.null(attr(srcref, "srcfile")))
         {
            # looks like user code--invoke the browser below
            foundUserCode <- TRUE
            break
         }
      }
   }
   if (foundUserCode || !userOnly)
   {
      frame <- length(sys.frames()) - 3
      eval(substitute(browser(skipCalls = skip), list(skip = 7 - frame)), 
           envir = sys.frame(frame))
   }
},
hideFromDebugger = TRUE)

.rs.addFunction("breakOnAnyError", function()
{
   .rs.breakOnError(FALSE)
},
hideFromDebugger = TRUE)

.rs.addFunction("breakOnUserError", function()
{
   .rs.breakOnError(TRUE)
},
hideFromDebugger = TRUE)

.rs.addFunction("setErrorManagementType", function(type, userOnly)
{
   if (type == 0)
      options(error = NULL)
   else if (type == 1)
      options(error = function() { .rs.recordTraceback(userOnly) })
   else if (type == 2 && userOnly)
      options(error = .rs.breakOnUserError)
   else if (type == 2 && !userOnly)
      options(error = .rs.breakOnAnyError)
})
