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
.rs.defineVar("automation.remoteInstance", NULL)

.rs.addFunction("test", function(desc, code)
{
   # Check test markers.
   currentMarkers <- .rs.getVar("automation.currentMarkers")
   requestedMarkers <- .rs.getVar("automation.requestedMarkers")
   .rs.setVar("automation.currentMarkers", NULL)
   
   if (length(requestedMarkers))
   {
      matches <- intersect(currentMarkers, requestedMarkers)
      if (length(matches) == 0L)
      {
         if (interactive())
            message("[i] Skipping test (does not have any matching markers).")
         return(invisible(NULL))
      }
   }
   
   # Reset the session before running this test.
   .rs.automation.remoteInstance$session.reset()
   
   # Now, run the test.
   testthat::test_that(desc, code)
})

.rs.addFunction("automation.newRemote", function(mode = NULL)
{
   # Re-use existing remote if requested.
   reuse <- Sys.getenv("RSTUDIO_AUTOMATION_REUSE_REMOTE", unset = "FALSE")
   if (reuse)
   {
      remote <- .rs.automation.remoteInstance
      if (!is.null(remote))
         return(remote)
   }
      
   # Generate the remote instance.
   mode <- .rs.automation.resolveMode(mode)
   client <- .rs.automation.initialize(mode = mode)
   assign("client", client, envir = .rs.automation.remote)
   assign("self", .rs.automation.remote, envir = .rs.automation.remotePrivateEnv)
   .rs.setVar("automation.remoteInstance", .rs.automation.remote)
   remote <- .rs.automation.remoteInstance
   
   # Return the remote instance.
   remote
})

.rs.addFunction("automation.deleteRemote", function(force = FALSE)
{
   # Avoid deleting remote if requested.
   if (!force)
   {
      reuse <- Sys.getenv("RSTUDIO_AUTOMATION_REUSE_REMOTE", unset = "FALSE")
      if (reuse)
         return(invisible(NULL))
   }
   
   remote <- .rs.getVar("automation.remoteInstance")
   if (is.null(remote))
      return(remote)
   
   remote$session.quit()
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


.rs.automation.addRemoteFunction("dom.elementExists", function(selector)
{
   # Query for the requested node.
   document <- self$client$DOM.getDocument(depth = 0L)
   response <- self$client$DOM.querySelector(document$root$nodeId, selector)
   
   # Check for failure.
   nodeId <- response$nodeId
   nodeId != 0L
})

.rs.automation.addRemoteFunction("dom.querySelector", function(selector)
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


.rs.automation.addRemoteFunction("dom.querySelectorAll", function(selector)
{
   # Query for all nodes matching the selector.
   document <- self$client$DOM.getDocument(depth = 0L)
   response <- self$client$DOM.querySelectorAll(document$root$nodeId, selector)
   
   # Check for failure.
   nodeIds <- response$nodeIds
   if (length(nodeIds) == 0L)
      stop("No elements matching selector '", selector, "' could be found.")
   
   # Return the list of discovered nodes.
   nodeIds
})

.rs.automation.addRemoteFunction("dom.clickElement", function(selector = NULL,
                                                              objectId = NULL,
                                                              nodeId = NULL,
                                                              verticalOffset = 0L,
                                                              horizontalOffset = 0L,
                                                              button = "left")
{
   # Resolve jsObject from provided parameters.
   objectId <- .rs.nullCoalesce(objectId, {
      
      # Query for the requested node.
      nodeId <- .rs.nullCoalesce(nodeId, {
         .rs.waitUntil(selector, function()
         {
            self$dom.querySelector(selector)
         }, swallowErrors = TRUE)
      })
      
      # Get a JavaScript object ID associated with this node.
      response <- self$client$DOM.resolveNode(nodeId)
      response$object$objectId
      
   })
   
   
   # Use that object ID to request the object's bounding rectangle.
   code <- "function() {
      return JSON.stringify(this.getBoundingClientRect());
   }"
   
   response <- self$client$Runtime.callFunctionOn(
      functionDeclaration = code,
      objectId = objectId
   )
   
   # Compute coordinates for click action. We'll try to target
   # the center of the requested element.
   domRect <- .rs.fromJSON(response$result$value)
   x <- domRect$x + (domRect$width / 2) + horizontalOffset
   y <- domRect$y + (domRect$height / 2) + verticalOffset
   
   # Use the position of that element to simulate a click.
   self$client$Input.dispatchMouseEvent(
      type = "mousePressed",
      x = x,
      y = y,
      button = button,
      clickCount = 1L
   )
   
   self$client$Input.dispatchMouseEvent(
      type = "mouseReleased",
      x = x,
      y = y,
      button = button,
      clickCount = 1L
   )
})

.rs.automation.addRemoteFunction("editor.getInstance", function()
{
   jsCode <- .rs.heredoc('
      var id = $RStudio.last_focused_editor_id;
      var container = document.getElementById(id);
      container.env.editor
   ')
   
   response <- self$client$Runtime.evaluate(expression = jsCode)
   .rs.automation.wrapJsResponse(self, response)
})


.rs.automation.addRemoteFunction("js.eval", function(jsExpr)
{
   # Implicit return for single-line expressions.
   jsExpr <- strsplit(jsExpr, "\n", fixed = TRUE)[[1L]]
   if (length(jsExpr) == 1L && !.rs.startsWith(jsExpr, "return "))
      jsExpr <- paste("return", jsExpr)
   
   # Build a command that executes the Javascript, and returns as JSON.
   jsStringifyExpr <- sprintf(
      "JSON.stringify((function() { %s })())",
      paste(jsExpr, collapse = "\n")
   )
   
   # Execute it.
   jsResponse <- self$client$Runtime.evaluate(expression = jsStringifyExpr)
   
   # Check for error.
   if (!is.null(jsResponse$exceptionDetails))
      stop(jsResponse$exceptionDetails$exception$description)
   
   # Marshal values back to R.
   .rs.fromJSON(jsResponse$result$value)
})

.rs.automation.addRemoteFunction("js.call", function(objectId, jsFunc)
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

.rs.automation.addRemoteFunction("js.querySelector", function(selector)
{
   response <- .rs.waitUntil(selector, function()
   {
      nodeId <- self$dom.querySelector(selector)
      self$client$DOM.resolveNode(nodeId)
   }, swallowErrors = TRUE)
   
   .rs.automation.wrapJsResponse(self, response)
})

.rs.automation.addRemoteFunction("js.querySelectorAll", function(selector)
{
   response <- .rs.waitUntil(selector, function()
   {
      nodeIds <- self$dom.querySelectorAll(selector)
      lapply(nodeIds, self$client$DOM.resolveNode)
   }, swallowErrors = TRUE)

   .rs.automation.wrapJsResponse(self, response)
})

.rs.automation.addRemoteFunction("modals.click", function(buttonName)
{
   .rs.tryCatch({
      buttonSelector <- sprintf("#rstudio_dlg_%s", buttonName)
      buttonId <- self$dom.querySelector(buttonSelector)
      self$dom.clickElement(objectId = buttonId)
      TRUE
   })
})

.rs.automation.addRemoteFunction("session.quit", function()
{
   # Try to gracefully shut down the browser. We use a timeout
   # so we can close the socket cleanly while the browser window
   # is still alive.
   self$js.eval('setTimeout(function() { window.close(); }, 1000)')
   
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

.rs.automation.addRemoteFunction("session.restart", function()
{
   # Invoke the restart command.
   self$commands.execute("restartR")
   
   # Send a command to the console, and wait for it to be executed.
   msg <- sprintf("Waiting for restart [token %s]", .rs.createUUID())
   self$console.executeExpr({ writeLines(!!msg) })
   
   # Wait until we have console output.
   .rs.waitUntil("session is ready", function()
   {
      any(self$console.getOutput() == msg)
   })
})

.rs.automation.addRemoteFunction("session.reset", function()
{
   # Clear any popups that might be visible.
   self$keyboard.insertText("<Escape>")
   
   # Clear any text that might be set in the console.
   self$commands.execute("activateConsole")
   self$keyboard.insertText("<Command + A>", "<Backspace>")
   
   # Close any open documents.
   self$console.executeExpr({
      .rs.api.closeAllSourceBuffersWithoutSaving()
      rm(list = ls())
   }, wait = FALSE)
   
   # Clear the console.
   self$keyboard.insertText("<Ctrl + L>")
})

