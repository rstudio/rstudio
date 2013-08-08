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

.rs.addFunction("handleError", function()
{
   calls <- sys.calls()

   # if there's just one call on the stack, this error happened at the top
   # level; no need to generate a traceback
   if (length(calls) < 2) 
      return()

   # create the traceback for the client
   stack <- lapply(calls[1:length(calls)-1], function(call)
   {
      srcref <- attr(call, "srcref")
      srcfile <- ""
      if (!is.null(srcref))
      {
         fileattr <- attr(srcref, "srcfile")
         srcfile <- fileattr$filename
      }
      else
         srcref <- rep(0L, 8)
      c (list(func = .rs.scalar(deparse(call)),
              file = .rs.scalar(srcfile)),
         .rs.lineDataList(srcref))
   })
   event <- list(
      frames = stack,
      message = .rs.scalar(geterrmessage()))
   .rs.enqueClientEvent("unhandled_error", event)
})

.rs.addFunction("handleUserError", function()
{
   calls <- sys.calls()
   foundUser <- FALSE
   for (n in 1:(length(calls) - 1))
   {
      func <- .rs.untraced(sys.function(n))
      srcref <- attr(func, "srcref")
      if (!is.null(srcref) && 
          !is.null(attr(srcref, "srcfile")))
      {
         # looks like user code--invoke the browser below
         foundUser <- TRUE
         break
      }
   }
   if (foundUser)
      browser(skipCalls = 2L)
   else
      .rs.handleError()
},
hideFromDebugger = TRUE)

.rs.addFunction("setErrorManagementType", function(type)
{
   if (type == 0)
      options(error = .rs.handleError)
   else if (type == 1)
      options(error = browser)
   else if (type == 2)
      options(error = .rs.handleUserError)
   else if (type == 3)
      options(error = NULL)
})
