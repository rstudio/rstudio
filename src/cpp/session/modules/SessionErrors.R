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
   stack <- lapply(1:(length(calls) - 1), function(n)
   {
      func <- .rs.untraced(sys.function(n))
      srcref <- attr(func, "srcref")
      srcfile <- ""
      if (!is.null(srcref))
         srcfile <- capture.output(attr(srcref, "srcfile"))
      else
         srcref <- rep(0L, 8)
      c (list(func = .rs.scalar(deparse(sys.call(n))),
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
   for (n in 1:(length(calls) - 1))
   {
      func <- .rs.untraced(sys.function(n))
      srcref <- attr(func, "srcref")
      if (!is.null(srcref) && 
          !is.null(attr(srcref, "srcfile")))
      {
         # looks like user code--invoke the browser (but skip this call)
         browser(skipCalls = 2L)
         break
      }
   }
   # didn't find any source references--just handle errors in the usual way
   .rs.handleError()
})

.rs.addFunction("setErrorManagementType", function(type)
{
   if (type == 0)
      options(error = .rs.handleError)
   else if (type == 1)
      options(error = browser)
   else if (type == 2)
      options(error = .rs.handleUserError)
})

.rs.addFunction("registerErrorHandler", function()
{
   if (is.null(getOption("error")))
   {
      .rs.setErrorManagementType(0)
   }
})

