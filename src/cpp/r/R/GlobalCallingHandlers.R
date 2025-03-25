#
# GlobalCallingHandlers.R
#
# Copyright (C) 2025 by Posit Software, PBC
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

.rs.addFunction("globalCallingHandlers.initialize", function()
{
   # Remove any previously-registered handlers.
   globalCallingHandlers(NULL)
   
   # Install our handlers.
   globalCallingHandlers(
      error   = .rs.globalCallingHandlers.onError,
      warning = .rs.globalCallingHandlers.onWarning
   )
   
})

.rs.addFunction("globalCallingHandlers.initializeCall", function()
{
   body(.rs.globalCallingHandlers.initialize)
})

.rs.addFunction("globalCallingHandlers.onError", function(cnd)
{
   .rs.globalCallingHandlers.onErrorImpl(cnd)
})

.rs.addFunction("globalCallingHandlers.onErrorImpl", function(cnd)
{
   if (!.rs.globalCallingHandlers.shouldHandleError(cnd))
      return()
   
   highlight <- function(text) {
      .rs.ansiYellow(.rs.ansiBold(text))
   }
   
   msg <- if (is.null(conditionCall(cnd)))
   {
      sprintf(
         "%s%s",
         .rs.ansiYellow(.rs.ansiBold(gettext("Error: ", domain = "R"))),
         conditionMessage(cnd)
      )
   }
   else
   {
      # Hacky way to respect R's available translations while only colouring
      # the first word in the prefix
      prefix <- gettext("Error in ", domain = "R")
      parts <- strsplit(prefix, " ", fixed = TRUE)[[1L]]
      parts[[1L]] <- highlight(parts[[1L]])
      prefix <- paste(parts, collapse = " ")
      
      sprintf(
         "%s %s: %s",
         prefix,
         format(conditionCall(cnd)),
         conditionMessage(cnd)
      )
   }
   
   cat(msg, file = stderr())
   .rs.recordTraceback(TRUE, .rs.enqueueError)
   
   invokeRestart("abort")
})

.rs.addFunction("globalCallingHandlers.onWarning", function(cnd)
{
   .rs.globalCallingHandlers.onWarningImpl(cnd)
})

.rs.addFunction("globalCallingHandlers.onWarningImpl", function(cnd)
{
   if (inherits(cnd, "rlang_warning"))
      return()
   
   str(cnd)
})

.rs.addFunction("globalCallingHandlers.shouldHandleError", function(cnd)
{
   # don't handle errors with custom classes
   custom <-
      !identical(class(cnd), c("simpleError", "error", "condition")) &&
      !identical(class(cnd), c("error", "condition"))
   
   if (custom)
      return(FALSE)
   
   # don't handle errors if the error handler has changed
   error <- getOption("error")
   if (is.call(error) && length(error) == 1L)
   {
      error <- error[[1L]]
      type <- attr(error, "errorHandlerType", exact = TRUE)
      if (is.null(type))
         return(FALSE)
   }
   
   # okay, we can handle it
   TRUE
})

.rs.addFunction("ansiRed", function(text)
{
   paste0("\033[31m", text, "\033[39m")
})

.rs.addFunction("ansiYellow", function(text)
{
   paste0("\033[33m", text, "\033[39m")
})

.rs.addFunction("ansiBold", function(text)
{
   paste0("\033[1m", text, "\033[22m")
})
