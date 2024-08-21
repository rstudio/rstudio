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
# are given access to a special environment containing 'self', which refers
# to the remote object itself, so that other existing remote methods can be
# used. The underlying client connection can also be accessed via self$client.
#
# This isn't the most idiomatic R code, but it helps ensure that new stateful
# methods can be easily added to the 'remote' instance.

# !diagnostics suppress=client,self
.rs.defineVar("automation.remotePrivateEnv", new.env(parent = .rs.toolsEnv()))
.rs.defineVar("automation.remote", new.env(parent = emptyenv()))

.rs.setVar("automation.remoteInstance", NULL)

.rs.addFunction("automation.newRemote", function(mode = NULL)
{
   mode <- .rs.automation.resolveMode(mode)
   client <- .rs.automation.initialize(mode = mode)
   assign("client", client, envir = .rs.automation.remote)
   assign("self", .rs.automation.remote, envir = .rs.automation.remotePrivateEnv)
   .rs.setVar("automation.remoteInstance", .rs.automation.remote)
   .rs.automation.remoteInstance
})

.rs.addFunction("automation.deleteRemote", function()
{
   remote <- .rs.getVar("automation.remoteInstance")
   if (is.null(remote))
      return(remote)
   
   remote$quit()
   .rs.setVar("automation.remoteInstance", NULL)
})

.rs.addFunction("automation.getRemote", function(mode = NULL)
{
   remote <- .rs.nullCoalesce(.rs.automation.remoteInstance, {
      .rs.automation.newRemote(mode = mode)
   })
   
   .rs.setVar("automation.remoteInstance", remote)
   remote
})

.rs.addFunction("automation.addRemoteFunction", function(name, callback)
{
   environment(callback) <- .rs.automation.remotePrivateEnv
   assign(name, callback, envir = .rs.automation.remote)
})

.rs.automation.addRemoteFunction("commandExecute", function(command)
{
   code <- .rs.deparse(call(".rs.api.executeCommand", as.character(command)))
   self$consoleExecute(code)
})

.rs.automation.addRemoteFunction("consoleExecuteExpr", function(expr)
{
   code <- paste(deparse(rlang::enexpr(expr)), collapse = "\n")
   self$consoleExecute(code)
})

.rs.automation.addRemoteFunction("consoleExecute", function(code)
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
})

.rs.automation.addRemoteFunction("consoleOutput", function()
{
   consoleOutput <- self$jsObjectViaSelector("#rstudio_console_output")
   strsplit(consoleOutput$innerText, split = "\n", fixed = TRUE)[[1L]]
})

.rs.automation.addRemoteFunction("documentOpen", function(ext, contents)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   self$consoleExecute(code)
   
   self$waitForElement("#rstudio_source_text_editor")
})

.rs.automation.addRemoteFunction("documentExecute", function(ext, contents, callback)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   documentPath <- chartr("\\", "/", documentPath)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   self$consoleExecute(code)
   
   # Set an exit handler so we close and clean up the console history after.
   on.exit({
      self$documentClose()
      self$shortcutExecute("Ctrl + L")
   }, add = TRUE)
   
   # Wait until the element is focused.
   .rs.waitUntil("source editor is focused", function()
   {
      className <- self$jsExec("document.activeElement.className")
      length(className) && grepl("ace_text-input", className)
   })
   
   # Get a reference to the editor in that instance.
   editor <- self$editorGetInstance()
   
   # Invoke callback with the editor instance.
   callback(editor)
})

.rs.automation.addRemoteFunction("documentClose", function()
{
   self$consoleExecute("invisible(.rs.api.documentClose())")
})

.rs.automation.addRemoteFunction("domGetNodeId", function(selector)
{
   # Query for the requested node.
   document <- self$client$DOM.getDocument(depth = 0L)
   response <- self$client$DOM.querySelector(document$root$nodeId, selector)
   
   # Check for failure.
   nodeId <- response$nodeId
   if (nodeId == 0L)
      stop("No element matching selector '", selector, "' could be found.")
   
   # Describe the discovered node.
   nodeId
})

.rs.automation.addRemoteFunction("domClickElement", function(selector, button = "left")
{
   # Query for the requested node.
   nodeId <- self$domGetNodeId(selector)
   
   # Get a JavaScript object ID associated with this node.
   response <- self$client$DOM.resolveNode(nodeId)
   objectId <- response$object$objectId
   
   # Use that object ID to request the object's bounding rectangle.
   code <- "function() {
      return JSON.stringify(this.getBoundingClientRect());
   }"
   
   response <- self$client$Runtime.callFunctionOn(
      functionDeclaration = code,
      objectId = objectId,
   )
   
   domRect <- .rs.fromJSON(response$result$value)
   
   # Use the position of that element to simulate a click.
   self$client$Input.dispatchMouseEvent(
      type = "mousePressed",
      x = domRect$x + (domRect$width / 2),
      y = domRect$y + (domRect$height / 2),
      button = button,
      clickCount = 1L
   )
   
   self$client$Input.dispatchMouseEvent(
      type = "mouseReleased",
      x = domRect$x + (domRect$width / 2),
      y = domRect$y + (domRect$height / 2),
      button = button,
      clickCount = 1L
   )
})

.rs.automation.addRemoteFunction("editorGetInstance", function()
{
   jsCode <- .rs.heredoc('
      var id = $RStudio.last_focused_editor_id;
      var container = document.getElementById(id);
      container.env.editor
   ')
   
   response <- self$client$Runtime.evaluate(expression = jsCode)
   .rs.automation.wrapJsResponse(self, response)
})


.rs.automation.addRemoteFunction("jsExec", function(expression)
{
   # Implicit return for single-line expressions.
   expression <- strsplit(expression, "\n", fixed = TRUE)[[1L]]
   if (length(expression) == 1L && !.rs.startsWith(expression, "return "))
      expression <- paste("return", expression)
   
   # Build a command that executes the Javascript, and returns as JSON.
   jsonExpression <- sprintf(
      "JSON.stringify((function() { %s })())",
      paste(expression, collapse = "\n")
   )
   
   # Execute it.
   self$client$Runtime.evaluate(expression = jsonExpression)
   jsonResponse <- self$client$Runtime.evaluate(expression = jsonExpression)
   
   # Check for error.
   if (!is.null(jsonResponse$exceptionDetails))
      stop(jsonResponse$exceptionDetails$exception$description)
   
   # Marshal values back to R.
   .rs.fromJSON(jsonResponse$result$value)
})

.rs.automation.addRemoteFunction("jsCall", function(objectId, jsFunc)
{
   # Wrap provided code into a function if necessary.
   if (!grepl("^function\\b", jsFunc))
   {
      # Implicit return for single-line expressions.
      lines <- strsplit(jsFunc, "\n", fixed = TRUE)[[1L]]
      if (length(lines) == 1L && !.rs.startsWith(lines, "return "))
         lines <- paste("return", lines)
      
      # Wrap into a function.
      jsFunc <- sprintf("function() { %s }", paste(lines, collapse = "\n"))
   }
   
   # Wrap so that we can return JSON.
   self$client$Runtime.callFunctionOn(
      functionDeclaration = jsFunc,
      objectId = objectId
   )
})

.rs.automation.addRemoteFunction("jsObjectViaExpression", function(expression)
{
   response <- .rs.waitFor(expression, function()
   {
      self$client$Runtime.evaluate(expression)
   })
   
   .rs.automation.wrapJsResponse(self, response)
})

.rs.automation.addRemoteFunction("jsObjectViaSelector", function(selector)
{
   response <- .rs.waitFor(selector, function()
   {
      nodeId <- self$domGetNodeId(selector)
      self$client$DOM.resolveNode(nodeId)
   })
   
   .rs.automation.wrapJsResponse(self, response)
})

.rs.automation.addRemoteFunction("keyboardExecute", function(...)
{
   reShortcut <- "^\\<(.*)\\>$"
   for (input in list(...))
   {
      if (grepl(reShortcut, input, perl = TRUE))
      {
         shortcut <- sub(reShortcut, "\\1", input, perl = TRUE)
         self$shortcutExecute(shortcut)
      }
      else
      {
         self$client$Input.insertText(input)
      }
   }
})

.rs.automation.addRemoteFunction("quit", function()
{
   # Close the websocket connection.
   self$client$socket$close()
   
   # Wait until the socket is closed.
   .rs.waitUntil("websocket closed", function()
   {
      self$client$socket$readyState() == 3L
   })
   
   # Kill the running process.
   .rs.automation.agentProcess$kill()
   
   # Wait until it's no longer around.
   alive <- TRUE
   .rs.waitUntil("process stopped", function()
   {
      alive <<- !.rs.automation.agentProcess$is_alive()
   })
   
   invisible(alive)
})

.rs.automation.addRemoteFunction("shortcutExecute", function(shortcut)
{
   parts <- tolower(strsplit(shortcut, "\\s*\\+\\s*", perl = TRUE)[[1L]])
   
   modifiers <- 0L
   if ("alt" %in% parts)
      modifiers <- bitwOr(modifiers, 1L)
   if ("ctrl" %in% parts)
      modifiers <- bitwOr(modifiers, 2L)
   if ("meta" %in% parts)
      modifiers <- bitwOr(modifiers, 4L)
   if ("shift" %in% parts)
      modifiers <- bitwOr(modifiers, 8L)
   
   key <- tail(parts, n = 1L)
   code <- .rs.automationConstants.keyToKeyCodeMap[[key]]
   if (is.null(code))
      stop(sprintf("couldn't convert key '%s' to key code", key))
   
   self$client$Input.dispatchKeyEvent(
      type                  = "keyDown",
      modifiers             = modifiers,
      windowsVirtualKeyCode = code
   )
   
   self$client$Input.dispatchKeyEvent(
      type                  = "keyUp",
      modifiers             = modifiers,
      windowsVirtualKeyCode = code
   )
   
})

.rs.automation.addRemoteFunction("waitForElement", function(selector,
                                                            predicate = NULL)
{
   # Query for the requested node.
   document <- self$client$DOM.getDocument(depth = 0L)
   
   # Wait until we have a node ID.
   nodeId <- 0L
   .rs.waitUntil(selector, function()
   {
      response <- self$client$DOM.querySelector(document$root$nodeId, selector)
      nodeId <<- response$nodeId
   })
   
   # Wait until the predicate is true.
   if (!is.null(predicate))
   {
      .rs.waitUntil(selector, function()
      {
         predicate(nodeId)
      })
   }
   
   # Return the resolved node id.
   nodeId
})
