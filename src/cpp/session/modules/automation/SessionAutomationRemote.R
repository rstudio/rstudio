#
# SessionAutomationRemote.R
#
# Copyright (C) 2024 by Posit Software, PBC
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

# NOTE: The functions created by '.rs.automation.addRemoteFunction()' here
# are given access to a special environment containing:
#
# - client: The client instance used to communicate with RStudio,
# - self / this: The remote instance itself, so that other methods can be used.
#
# This isn't the most idiomatic R code, but it helps ensure that new stateful
# methods can be easily added to the 'remote' instance.

# !diagnostics suppress=client,self,this
.rs.setVar("automation.remotePrivateEnv", new.env(parent = .rs.toolsEnv()))
.rs.setVar("automation.remote", new.env(parent = emptyenv()))

.rs.addFunction("automation.createRemote", function(client = NULL)
{
   client <- .rs.nullCoalesce(client, .rs.automation.initialize())
   assign("client", client, envir = .rs.automation.remotePrivateEnv)
   assign("self", .rs.automation.remote, envir = .rs.automation.remotePrivateEnv)
   assign("this", .rs.automation.remote, envir = .rs.automation.remotePrivateEnv)
   .rs.automation.remote
})

.rs.addFunction("automation.addRemoteFunction", function(name, callback)
{
   environment(callback) <- .rs.automation.remotePrivateEnv
   assign(name, callback, envir = .rs.automation.remote)
})

.rs.automation.addRemoteFunction("getClient", function()
{
   client
})

.rs.automation.addRemoteFunction("executeCode", function(code)
{
   # Make sure the Console pane is focused.
   document <- client$DOM.getDocument()
   response <- client$DOM.querySelector(
      nodeId = document$root$nodeId,
      selector = "#rstudio_console_input .ace_text-input"
   )
   
   client$DOM.describeNode(nodeId = response$nodeId)
   client$DOM.focus(nodeId = response$nodeId)
   
   # Send the code to be executed.
   client$Input.insertText(text = code)
   
   # Send an Enter key to force execution.
   client$Input.dispatchKeyEvent(type = "rawKeyDown", windowsVirtualKeyCode = 13L)
})

.rs.automation.addRemoteFunction("documentExecute", function(ext, contents, expr)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   this$executeCode(code)
   
   # Run the associated expression.
   force(expr)
   
   # Close the document when we're done.
   this$documentClose()
})

.rs.automation.addRemoteFunction("documentClose", function()
{
   this$executeCode(".rs.api.documentClose()")
})

.rs.automation.addRemoteFunction("evaluateJavascript", function(expression)
{
   client$Runtime.evaluate(expression = expression)
})

.rs.automation.addRemoteFunction("getAceTokens", function(row)
{
   # Get a reference to the Ace editor instance.
   document <- client$DOM.getDocument()
   client$DOM.querySelector(document, '#rstudio_source_text_editor')
})

.rs.automation.addRemoteFunction("waitFor", function(callback)
{
   client
})
