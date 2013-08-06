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
   stack <- lapply(1:(length(calls) - 1), function(n) {
      func <- sys.function(n)
      if (isS4(func) && class(func) == "functionWithTrace")
         func <- func@original
      srcref <- attr(func, "srcref")
      srcfile <- ""
      if (!is.null(srcref))
         srcfile <- capture.output(attr(srcref, "srcfile"))
      else
         srcref <- rep(0L, 8)
      list(
         func = .rs.scalar(deparse(sys.call(n))),
         file = .rs.scalar(srcfile),
         line_number = .rs.scalar(srcref[1]),
         end_line_number = .rs.scalar(srcref[3]),
         character_number = .rs.scalar(srcref[5]),
         end_character_number = .rs.scalar(srcref[6])
      )
   })
   event <- list(
      frames = stack,
      message = .rs.scalar(geterrmessage()))
   .rs.enqueClientEvent("unhandled_error", event)
})

.rs.addFunction("setErrorManagementType", function(type)
{
   if (type == 0)
      options(error = .rs.handleError)
   else if (type == 1)
      options(error = browser)
})

.rs.addFunction("registerErrorHandler", function()
{
   if (is.null(getOption("error")))
   {
      .rs.setErrorManagementType(0)
   }
})
