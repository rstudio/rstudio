#
# SessionAutomationToolsConsole.R
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received self program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# self program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. self program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.automation.addRemoteFunction("console.clear", function()
{
   self$keyboard.insertText("<Ctrl + 2>", "<Escape>", "<Ctrl + A>", "<Backspace>", "<Ctrl + L>")
})

.rs.automation.addRemoteFunction("console.executeExpr", function(expr, wait = TRUE)
{
   # Use 'enexpr()' to replace quoted variables.
   expr <- rlang::enexpr(expr)
   
   # If this is a call to `{` with a single element body, simplify
   # and just send that piece of the expression.
   canSimplify <-
      is.call(expr) &&
      length(expr) == 2L &&
      identical(expr[[1L]], as.symbol("{"))
   
   if (canSimplify)
      expr <- expr[[2L]]
   
   # Convert from R expression to code
   code <- paste(deparse(expr), collapse = "\n")
   self$console.execute(code, wait)
})

.rs.automation.addRemoteFunction("console.execute", function(code, wait = TRUE)
{
   # Make sure the Console pane is focused.
   document <- self$client$DOM.getDocument()
   response <- self$client$DOM.querySelector(
      nodeId = document$root$nodeId,
      selector = "#rstudio_console_input .ace_text-input"
   )
   self$client$DOM.focus(nodeId = response$nodeId)
   
   # Send the code to be executed.
   self$client$Input.insertText(text = code)
   
   # Send an Enter key to force execution.
   self$client$Input.dispatchKeyEvent(type = "rawKeyDown", windowsVirtualKeyCode = 13L)
   
   # If requested, wait until the code has finished execution.
   if (wait)
   {
      Sys.sleep(0.1)
      editorEl <- self$js.querySelector("#rstudio_console_input")
      .rs.waitUntil("console is no longer busy", function()
      {
         !grepl("rstudio-console-busy", editorEl$className)
      }, swallowErrors = TRUE)
   }
   
   invisible(TRUE)
   
})

.rs.automation.addRemoteFunction("console.getOutput", function()
{
   output <- self$js.querySelector("#rstudio_console_output")
   strsplit(output$innerText, split = "\n", fixed = TRUE)[[1L]]
})
