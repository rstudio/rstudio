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
# - client: The client instance used to communicate with the RStudio agent,
# - self:   The remote itself, so that other existing remote methods can be used.
#
# This isn't the most idiomatic R code, but it helps ensure that new stateful
# methods can be easily added to the 'remote' instance.

# !diagnostics suppress=client,self
.rs.defineVar("automation.remotePrivateEnv", new.env(parent = .rs.toolsEnv()))
.rs.defineVar("automation.remote", new.env(parent = emptyenv()))

.rs.addFunction("automation.createRemote", function(mode = NULL)
{
   mode <- .rs.automation.resolveMode(mode)
   client <- .rs.automation.initialize(mode = mode)
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
      var id = $RStudio.last_focused_editor_id;
      var container = document.getElementById(id);
      var editor = container.env.editor;
      var tokens = editor.session.getTokens(%i);
      JSON.stringify(tokens);
   }')
   
   jsCode <- sprintf(jsCode, as.integer(row))
   
   response <- self$jsExec(jsCode)
   .rs.fromJSON(response$result$value)
})

.rs.automation.addRemoteFunction("aceSetCursorPosition", function(row, column = 0L)
{
   jsCode <- .rs.heredoc(r'{
      var id = $RStudio.last_focused_editor_id;
      var container = document.getElementById(id);
      var editor = container.env.editor;
      var position = { row: %i, column: %i };
      var range = { start: position, end: position };
      editor.selection.setSelectionRange(range);
   }', as.integer(row), as.integer(column))
   
   self$jsExec(jsCode)
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
   # TODO: Use JavaScript API?
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
   # TODO: Use JavaScript API?
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
   
   # Describe the discovered node.
   nodeId
})

.rs.automation.addRemoteFunction("domClickElement", function(selector, button = "left")
{
   # Query for the requested node.
   nodeId <- self$domGetNodeId(selector)
   
   # Get a JavaScript object ID associated with this node.
   response <- client$DOM.resolveNode(nodeId)
   objectId <- response$object$objectId
   
   # Use that object ID to request the object's bounding rectangle.
   code <- "function() {
      return JSON.stringify(this.getBoundingClientRect());
   }"
   
   response <- client$Runtime.callFunctionOn(
      functionDeclaration = code,
      objectId = objectId,
   )
   
   domRect <- .rs.fromJSON(response$result$value)
   
   # Use the position of that element to simulate a click.
   client$Input.dispatchMouseEvent(
      type = "mousePressed",
      x = domRect$x + (domRect$width / 2),
      y = domRect$y + (domRect$height / 2),
      button = button
   )
   
   client$Input.dispatchMouseEvent(
      type = "mouseReleased",
      x = domRect$x + (domRect$width / 2),
      y = domRect$y + (domRect$height / 2),
      button = button
   )
})

.rs.automation.addRemoteFunction("jsExec", function(expression)
{
   client$Runtime.evaluate(expression = expression)
})

.rs.automation.addRemoteFunction("jsCall", function(jsFunc,
                                                    objectId = NULL,
                                                    nodeId = NULL)
{
   objectId <- .rs.nullCoalesce(objectId, {
      resolvedNode <- client$DOM.resolveNode(nodeId)
      objectId <- resolvedNode$object$objectId
   })
   
   client$Runtime.callFunctionOn(
      functionDeclaration = jsFunc,
      objectId = objectId
   )
})

.rs.automation.addRemoteFunction("quit", function()
{
   # TODO: First attempt a graceful shutdown, and then later just
   # forcefully kill the process if necessary.
   client$Browser.close()
   
   alive <- TRUE
   .rs.waitUntil(retryCount = 10, waitTimeSecs = 0.5, function()
   {
      alive <<- !.rs.automation.agentProcess$is_alive()
   })
   
   # If the process is still alive, forcefully kill it.
   if (alive)
      .rs.automation.agentProcess$kill()
})
