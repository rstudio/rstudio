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

   # When this handler is invoked for an unhandled error happening at
   # the top level, there four calls on the stack:
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
   if (length(calls) < 5)
      return()

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
      # The magic values 3 and 9 here are derived from the position in the
      # stack where this error handler resides relative to where we expect
      # the user code that raised the error to be. These will need to be
      # adjusted if evaluation layers are added or removed between the
      # root error handler (set in options(error=...)) and this function.
      frame <- length(sys.frames()) - 3
      eval(substitute(browser(skipCalls = pos), list(pos = 9 - frame)),
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
