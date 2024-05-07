#
# SessionAutomationRemote.R
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

# NOTE: The functions created by '.rs.automation.addRemoteFunction()' here
# are given access to a special environment containing:
#
# - client: The client instance used to communicate with RStudio,
# - self / self: The remote instance itself, so that other methods can be used.
#
# self isn't the most idiomatic R code, but it helps ensure that new stateful
# methods can be easily added to the 'remote' instance.

# !diagnostics suppress=client,self,self
.rs.defineVar("automation.remotePrivateEnv", new.env(parent = .rs.toolsEnv()))
.rs.defineVar("automation.remote", new.env(parent = emptyenv()))

.rs.addFunction("automation.createRemote", function(client = NULL)
{
   client <- .rs.nullCoalesce(client, .rs.automation.initialize())
   assign("client", client, envir = .rs.automation.remote)
   assign("client", client, envir = .rs.automation.remotePrivateEnv)
   assign("self", .rs.automation.remote, envir = .rs.automation.remotePrivateEnv)
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

.rs.automation.addRemoteFunction("aceLineTokens", function(row)
{
   jsCode <- .rs.heredoc(r'{
      var container = document.getElementById("rstudio_source_text_editor");
      var editor = container.env.editor;
      var tokens = editor.session.getTokens(%i);
      JSON.stringify(tokens);
   }')
   
   jsCode <- sprintf(jsCode, as.integer(row))
   
   response <- self$evaluateJavascript(jsCode)
   .rs.fromJSON(response$result$value)
})

.rs.automation.addRemoteFunction("aceSetCursorPosition", function(row, column = 0L)
{
   jsCode <- .rs.heredoc(r'{
      var container = document.getElementById("rstudio_source_text_editor");
      var editor = container.env.editor;
      var position = { row: %i, column: %i };
      var range = { start: position, end: position };
      editor.selection.setSelectionRange(range);
   }', as.integer(row), as.integer(column))
   
   self$evaluateJavascript(jsCode)
})

.rs.automation.addRemoteFunction("commandExecute", function(command)
{
   code <- .rs.deparse(call(".rs.api.executeCommand", as.character(command)))
   self$consoleExecute(code)
})

.rs.automation.addRemoteFunction("consoleExecute", function(code)
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

.rs.automation.addRemoteFunction("documentOpen", function(ext, contents)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   self$consoleExecute(code)
   
   # TODO: Wait until source editor is focused.
})

.rs.automation.addRemoteFunction("documentExecute", function(ext, contents, expr)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   self$consoleExecute(code)
   on.exit(self$documentClose(), add = TRUE)
   
   # TODO: Wait until source editor is focused.
   
   # Run the associated expression.
   force(expr)
})

.rs.automation.addRemoteFunction("documentClose", function()
{
   self$consoleExecute(".rs.api.documentClose()")
})

.rs.automation.addRemoteFunction("domGetNodeId", function(selector)
{
   # Query for the requested node.
   document <- client$DOM.getDocument(depth = 0L)
   response <- client$DOM.querySelector(document$root$nodeId, selector)
   
   # Check for failure.
   nodeId <- response$nodeId
   if (nodeId == 0L)
      stop("No element matching selector '", nodeId, "' could be found.")
   
   # Return discovered node ID.
   nodeId
})

.rs.automation.addRemoteFunction("evaluateJavascript", function(expression)
{
   client$Runtime.evaluate(expression = expression)
})

.rs.automation.addRemoteFunction("quit", function()
{
   client$Browser.close()
})

.rs.automation.addRemoteFunction("waitFor", function(callback)
{
   client
})
