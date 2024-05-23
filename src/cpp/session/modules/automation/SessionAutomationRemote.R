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

.rs.setVar("automation.keyToKeyCodeMap", list(
   backspace = 8L,
   tab = 9L,
   enter = 13L,
   shift = 16L,
   ctrl = 17L,
   alt = 18L,
   pause = 19L,
   capslock = 20L,
   escape = 27L,
   space = 32L,
   pageup = 33L,
   pagedown = 34L,
   end = 35L,
   home = 36L,
   left = 37L,
   up = 38L,
   right = 39L,
   down = 40L,
   insert = 45L,
   delete = 46L,
   "0" = 48L,
   "1" = 49L,
   "2" = 50L,
   "3" = 51L,
   "4" = 52L,
   "5" = 53L,
   "6" = 54L,
   "7" = 55L,
   "8" = 56L,
   "9" = 57L,
   a = 65L,
   b = 66L,
   c = 67L,
   d = 68L,
   e = 69L,
   f = 70L,
   g = 71L,
   h = 72L,
   i = 73L,
   j = 74L,
   k = 75L,
   l = 76L,
   m = 77L,
   n = 78L,
   o = 79L,
   p = 80L,
   q = 81L,
   r = 82L,
   s = 83L,
   t = 84L,
   u = 85L,
   v = 86L,
   w = 87L,
   x = 88L,
   y = 89L,
   z = 90L,
   numpad0 = 96L,
   numpad1 = 97L,
   numpad2 = 98L,
   numpad3 = 99L,
   numpad4 = 100L,
   numpad5 = 101L,
   numpad6 = 102L,
   numpad7 = 103L,
   numpad8 = 104L,
   numpad9 = 105L,
   f1 = 112L,
   f2 = 113L,
   f3 = 114L,
   f4 = 115L,
   f5 = 116L,
   f6 = 117L,
   f7 = 118L,
   f8 = 119L,
   f9 = 120L,
   f10 = 121L,
   f11 = 122L,
   f12 = 123L,
   ";" = 186L,
   "=" = 187L,
   "," = 188L,
   "-" = 189L,
   "." = 190L,
   "/" = 191L,
   "`" = 192L,
   "[" = 219L,
   "\\" = 220L,
   "]" = 221L,
   "'" = 222L
))

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

.rs.automation.addRemoteFunction("documentExecute", function(ext, contents, expr)
{
   # Write document contents to file.
   documentPath <- tempfile("document-", fileext = ext)
   writeLines(contents, con = documentPath)
   
   # Open that document in the attached editor.
   code <- sprintf(".rs.api.documentOpen(\"%s\")", documentPath)
   self$consoleExecute(code)
   on.exit(self$documentClose(), add = TRUE)
   
   # Wait until the element is focused.
   .rs.waitUntil("source editor is focused", function()
   {
      className <- self$jsExec("document.activeElement.className")
      length(className) && grepl("ace_text-input", className)
   })
   
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
   document <- self$client$DOM.getDocument(depth = 0L)
   response <- self$client$DOM.querySelector(document$root$nodeId, selector)
   
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
      button = button
   )
   
   self$client$Input.dispatchMouseEvent(
      type = "mouseReleased",
      x = domRect$x + (domRect$width / 2),
      y = domRect$y + (domRect$height / 2),
      button = button
   )
})

.rs.automation.addRemoteFunction("editorGetTokens", function(row)
{
   jsCode <- .rs.heredoc(r'{
      var id = $RStudio.last_focused_editor_id;
      var container = document.getElementById(id);
      var editor = container.env.editor;
      var tokens = editor.session.getTokens(%i);
      return tokens;
   }', as.integer(row))
   
   self$jsExec(jsCode)
})

.rs.automation.addRemoteFunction("editorGetState", function(row)
{
   jsCode <- .rs.heredoc(r'{
      var id = $RStudio.last_focused_editor_id;
      var container = document.getElementById(id);
      var editor = container.env.editor;
      var state = editor.session.getState(%i);
      return state;
   }', as.integer(row))
   
   self$jsExec(jsCode)
})


.rs.automation.addRemoteFunction("editorSetCursorPosition", function(row, column = 0L)
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

.rs.automation.addRemoteFunction("jsCall", function(jsFunc,
                                                    objectId = NULL,
                                                    nodeId = NULL)
{
   objectId <- .rs.nullCoalesce(objectId, {
      resolvedNode <- self$client$DOM.resolveNode(nodeId)
      objectId <- resolvedNode$object$objectId
   })
   
   self$client$Runtime.callFunctionOn(
      functionDeclaration = jsFunc,
      objectId = objectId
   )
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
   code <- .rs.automation.keyToKeyCodeMap[[key]]
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
