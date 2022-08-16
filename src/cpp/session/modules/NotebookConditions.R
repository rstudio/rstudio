#
# NotebookConditions.R
#
# Copyright (C) 2022 by Posit, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
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
#
# To circumvent this, we define R functions with the code we need to run,
# but explicitly extract the body of that function, and then execute that
# at the top level.
.rs.addFunction("notebookConditions.connectCall", function()
{
   body(.rs.notebookConditions.connectImpl)
})

.rs.addFunction("notebookConditions.connectImpl", function()
{
  
   # NOTE: because the body of this function will be evaluated in the
   # global environment, we need to avoid defining variables here.
   #
   # https://github.com/rstudio/rstudio/issues/8834
  .rs.notebookConditions.handlerStack <-
   .Internal(.addCondHands(
         c("warning", "message"),
         list(
            warning = .rs.notebookConditions.onWarning,
            message = .rs.notebookConditions.onMessage
         ),
         base::globalenv(),
         NULL,
         TRUE
      ))
  base::assign(x = ".rs.notebookConditions.handlerStack", 
               value = .rs.notebookConditions.handlerStack,
               envir = .rs.toolsEnv())
  base::rm(.rs.notebookConditions.handlerStack)
})

.rs.addFunction("notebookConditions.disconnectCall", function()
{
   body(.rs.notebookConditions.disconnectImpl)
})

.rs.addFunction("notebookConditions.disconnectImpl", function()
{
   # NOTE: because the body of this function will be evaluated in the
   # global environment, we need to avoid defining variables here.
   #
   # https://github.com/rstudio/rstudio/issues/8834
   .Internal(.resetCondHands(
     base::get(
       x = ".rs.notebookConditions.handlerStack",
       envir = .rs.toolsEnv()
     )
     
   ))
  base::rm(.rs.notebookConditions.handlerStack, envir = .rs.toolsEnv())
})
