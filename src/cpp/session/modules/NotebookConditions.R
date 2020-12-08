#
# NotebookConditions.R
#
# Copyright (C) 2020 by RStudio, PBC
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

.rs.addFunction("notebookConditions.onWarning", function(condition)
{
   prefix <- gettext("Warning:", domain = "R")
   message <- paste(condition$message, collapse = "\n")
   full <- paste(prefix, message)
   .Call("rs_signalNotebookCondition", 1L, full, PACKAGE = "(embedding)")
   invokeRestart("muffleWarning")
})

.rs.addFunction("notebookConditions.onMessage", function(condition)
{
   full <- paste(condition$message, collapse = "\n")
   .Call("rs_signalNotebookCondition", 0L, full, PACKAGE = "(embedding)")
   invokeRestart("muffleMessage")
})

# NOTE: we need to add condition handlers to the top level, but cannot
# actually do so if there is an R function context on the stack.
# To circumvent this, we use R funcitons to just provide the call object
# that needs to be executed, and then execute that call at the top level.
.rs.addFunction("notebookConditions.connectCall", function()
{
   body(.rs.notebookConditions.connectImpl)
})

.rs.addFunction("notebookConditions.connectImpl", function()
{
   handlers <- .Internal(.addCondHands(
      c("warning", "message"),
      list(
         warning = .rs.notebookConditions.onWarning,
         message = .rs.notebookConditions.onMessage
      ),
      globalenv(),
      NULL,
      TRUE
   ))
   
   envir <- as.environment("tools:rstudio")
   assign(".rs.notebookConditions.handlerStack", handlers, envir = envir)
   
   handlers
})

.rs.addFunction("notebookConditions.disconnectCall", function()
{
   body(.rs.notebookConditions.disconnectImpl)
})

.rs.addFunction("notebookConditions.disconnectImpl", function()
{
   envir <- as.environment("tools:rstudio")
   handlers <- get(".rs.notebookConditions.handlerStack", envir = envir)
   rm(".rs.notebookConditions.handlerStack", envir = envir)
   .Internal(.resetCondHands(handlers))
})
