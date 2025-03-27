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
   if (.rs.uiPrefs$consoleHighlightConditions$get() == "errors_warnings_messages")
   {
      globalCallingHandlers(
         error   = .rs.globalCallingHandlers.onError,
         warning = .rs.globalCallingHandlers.onWarning,
         message = .rs.globalCallingHandlers.onMessage
      )
   }
   else if (.rs.uiPrefs$consoleHighlightConditions$get() == "errors_warnings")
   {
      globalCallingHandlers(
         error   = .rs.globalCallingHandlers.onError,
         warning = .rs.globalCallingHandlers.onWarning
      )
   }
   else if (.rs.uiPrefs$consoleHighlightConditions$get() == "errors")
   {
      globalCallingHandlers(
         error   = .rs.globalCallingHandlers.onError
      )
   }
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
   if (.rs.globalCallingHandlers.shouldHandleError(cnd))
   {
      msg <- .rs.globalCallingHandlers.formatCondition(cnd, "Error", "error")
      writeLines(msg, con = stderr())
      .rs.recordTraceback(TRUE, .rs.enqueueError)
      invokeRestart("abort")
   }
})

.rs.addFunction("globalCallingHandlers.onWarning", function(cnd)
{
   .rs.globalCallingHandlers.onWarningImpl(cnd)
})

.rs.addFunction("globalCallingHandlers.onWarningImpl", function(cnd)
{
   if (.rs.globalCallingHandlers.shouldHandleWarning(cnd))
   {
      msg <- .rs.globalCallingHandlers.formatCondition(cnd, "Warning", "warning")
      writeLines(msg, con = stderr())
      invokeRestart("muffleWarning")
   }
})

.rs.addFunction("globalCallingHandlers.onMessage", function(cnd)
{
   .rs.globalCallingHandlers.onMessageImpl(cnd)
})

.rs.addFunction("globalCallingHandlers.onMessageImpl", function(cnd)
{
   if (.rs.globalCallingHandlers.shouldHandleMessage(cnd))
   {
      msg <- .rs.globalCallingHandlers.formatCondition(cnd, NULL, "message")
      cat(msg, file = stderr())
      invokeRestart("muffleMessage")
   }
})

.rs.addFunction("globalCallingHandlers.shouldHandleMessage", function(cnd)
{
   !inherits(cnd, "rlang_message")
})

.rs.addFunction("globalCallingHandlers.shouldHandleError", function(cnd)
{
   # don't handle rlang errors
   if (inherits(cnd, "rlang_error"))
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

.rs.addFunction("globalCallingHandlers.formatCondition", function(cnd, label, type)
{
   msg <- conditionMessage(cnd)
   text <- if (is.null(label))
   {
      .rs.globalCallingHandlers.highlight(msg, type)
   }
   else if (is.null(conditionCall(cnd)))
   {
      # Hacky way to respect R's available translations while only colouring
      # the first word in the prefix
      prefix <- gettext(sprintf("%s: ", label), domain = "R")
      colonIndex <- regexpr(":", prefix, fixed = TRUE)
      lhs <- substr(prefix, 1L, colonIndex - 1L)
      rhs <- substr(prefix, colonIndex, .Machine$integer.max)
      prefix <- paste0(.rs.globalCallingHandlers.highlight(lhs, type), rhs)
      sprintf("%s%s", prefix, msg)
   }
   else
   {
      # Hacky way to respect R's available translations while only colouring
      # the first word in the prefix
      prefix <- gettext(sprintf("%s in ", label), domain = "R")
      parts <- strsplit(prefix, " ", fixed = TRUE)[[1L]]
      parts[[1L]] <- .rs.globalCallingHandlers.highlight(parts[[1L]], type)
      prefix <- paste(parts, collapse = " ")
      
      cll <- format(conditionCall(cnd))
      sprintf("%s %s :\n  %s", prefix, cll, msg)
   }
   
   # Enclose whole message in escapes so we can process it as a unit.
   .rs.globalCallingHandlers.group(text, type)
})

.rs.addFunction("globalCallingHandlers.group", function(text, type)
{
   itype <- switch(tolower(type), error = "1", warning = "2", message = "3")
   if (is.null(itype))
      return(text)
   
   paste0("\033G", itype, ";", text, "\033g")
})

.rs.addFunction("globalCallingHandlers.highlight", function(text, type = "error")
{
   itype <- switch(tolower(type), error = "1", warning = "2", message = "3")
   if (is.null(itype))
      return(text)
   
   paste0("\033H", itype, ";", text, "\033h")
})
