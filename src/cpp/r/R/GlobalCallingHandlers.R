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
      warning = .rs.globalCallingHandlers.onWarning,
      message = .rs.globalCallingHandlers.onMessage
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
   
   msg <- .rs.globalCallingHandlers.formatCondition(cnd, "Error")
   writeLines(msg, con = stderr())
   .rs.recordTraceback(TRUE, .rs.enqueueError)
   
   invokeRestart("abort")
})

.rs.addFunction("globalCallingHandlers.onWarning", function(cnd)
{
   .rs.globalCallingHandlers.onWarningImpl(cnd)
})

.rs.addFunction("globalCallingHandlers.onWarningImpl", function(cnd)
{
   if (!.rs.globalCallingHandlers.shouldHandleWarning(cnd))
      return()
   
   msg <- .rs.globalCallingHandlers.formatCondition(cnd, "Warning")
   writeLines(msg, con = stderr())
   
   invokeRestart("muffleWarning")
})

.rs.addFunction("globalCallingHandlers.onMessage", function(cnd)
{
   .rs.globalCallingHandlers.onMessageImpl(cnd)
})

.rs.addFunction("globalCallingHandlers.onMessageImpl", function(cnd)
{
   if (identical(class(cnd), c("packageStartupMessage", "simpleMessage", "message", "condition")))
   {
      msg <- conditionMessage(cnd)
      txt <- .rs.globalCallingHandlers.highlight(msg, type = "message")
      cat(txt, file = stderr())
      invokeRestart("muffleMessage")
   }
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

.rs.addFunction("globalCallingHandlers.shouldHandleWarning", function(cnd)
{
   # If the user is opting into bundling warnings, just let the default
   # R warning handler take over. We also need to ignore if warnings are
   # disabled entirely (via a negative value for the option).
   warn <- getOption("warn", default = 0L)
   if (warn <= 0L)
      return(FALSE)
   
   # rlang doesn't apply any custom styles to emitted warnings,
   # so handle warnings even if they have custom classes
   TRUE
})

.rs.addFunction("globalCallingHandlers.formatCondition", function(cnd, label)
{
   if (is.null(conditionCall(cnd)))
   {
      # Hacky way to respect R's available translations while only colouring
      # the first word in the prefix
      prefix <- gettext(sprintf("%s: ", label), domain = "R")
      colonIndex <- regexpr(":", prefix, fixed = TRUE)
      lhs <- substr(prefix, 1L, colonIndex - 1L)
      rhs <- substr(prefix, colonIndex, .Machine$integer.max)
      prefix <- paste0(.rs.globalCallingHandlers.highlight(lhs), rhs)
      sprintf("%s%s", prefix, conditionMessage(cnd))
   }
   else
   {
      # Hacky way to respect R's available translations while only colouring
      # the first word in the prefix
      prefix <- gettext(sprintf("%s in ", label), domain = "R")
      parts <- strsplit(prefix, " ", fixed = TRUE)[[1L]]
      parts[[1L]] <- .rs.globalCallingHandlers.highlight(parts[[1L]])
      prefix <- paste(parts, collapse = " ")
      
      cll <- format(conditionCall(cnd))
      msg <- conditionMessage(cnd)
      sprintf("%s %s: %s", prefix, cll, msg)
   }
   
})

.rs.addFunction("globalCallingHandlers.highlight", function(text, type = "error")
{
   # We use a custom CSI escape sequence of the form:
   #
   #    CSI n Z <text> ST
   #
   # where 'n' is an integer and is used to map to different styles for output.
   itype <- switch(tolower(type), error = "1", warning = "2", message = "3")
   prefix <- paste0("\033\133", itype, "Z")
   suffix <- "\033\134"
   paste0(prefix, text, suffix)
})
