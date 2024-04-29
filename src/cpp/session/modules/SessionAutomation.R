#
# SessionAutomation.R
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

.rs.setVar("automation.messageId", 0L)
.rs.setVar("automation.callbacks", new.env(parent = emptyenv()))
.rs.setVar("automation.responses", new.env(parent = emptyenv()))

.rs.addFunction("automation.installRequiredPackages", function()
{
   packages <- c("httr", "later", "websocket")
   for (package in packages)
   {
      if (!requireNamespace(package, quietly = TRUE))
      {
         install.packages(package)
         loadNamespace(package)
      }
   }
})

.rs.addFunction("automation.createClient", function(socket)
{
   list(
      
      .socket = socket,
      
      Browser.getVersion = function() {
         .rs.automation.sendSynchronousRequest(
            socket = socket,
            method = "Browser.getVersion",
            params = list()
         )
      },
      
      Runtime.evaluate = function(expression) {
         .rs.automation.sendSynchronousRequest(
            socket = socket,
            method = "Runtime.evaluate",
            params = list(
               expression = I(expression)
            )
         )
      }
      
   )
})

.rs.addFunction("automation.onMessage", function(event)
{
   # Small bit of indirection to make hot reloading easier.
   .rs.automation.onMessageImpl(event)
})

.rs.addFunction("automation.onMessageImpl", function(event)
{
   # Get the data associated with this request.
   print(event)
   data <- .rs.fromJSON(event[["data"]])
   
   # Check for an error status.
   
   # Check for a callback associated with this id.
   id <- data[["id"]]
   if (is.null(id)) {
      warning("response missing 'id' parameter")
      return()
   }
   
   # Retrieve the stored callback.
   callback <- .rs.automation.callbacks[[as.character(id)]]
   if (is.null(callback)) {
      warning("no callback registered for response with id '", id, "'")
      return()
   }
   
   tryCatch(
      
      # Invoke the callback.
      callback(data),
      
      # Treat errors as warnings.
      error = function(cnd) {
         warning(conditionMessage(cnd))
      },
      
      # Remove the callback when we're done.
      finally = {
        rm(list = as.character(id), envir = .rs.automation.callbacks)
     }
     
   )
   
})

.rs.addFunction("automation.onError", function(event)
{
   print(event)
})

.rs.addFunction("automation.onClose", function(event)
{
   print(event)
})

.rs.addFunction("automation.sendRequest", function(socket, method, params, callback = NULL)
{
   # Handle lazy callers.
   if (is.function(params) && is.null(callback))
   {
      callback <- params
      params <- list()
   }
   
   # Generate an id for this request.
   id <- .rs.automation.messageId
   .rs.automation.messageId <<- .rs.automation.messageId + 1L
   
   # Register a callback for this message id.
   assign(as.character(id), callback, envir = .rs.automation.callbacks)
   
   # Generate the request.
   request <- list(
      id     = id,
      method = method,
      params = params
   )
   
   # Fire it off.
   json <- .rs.toJSON(request, unbox = TRUE)
   socket$send(json)
   
   # Return the id.
   invisible(id)
})

.rs.addFunction("automation.sendSynchronousRequest", function(socket, method, params = list())
{
   names(params) <- .rs.nullCoalesce(
      names(params),
      rep.int("", length(params))
   )
   
   # Send the request.
   response <- NULL
   callback <- function(response)
   {
      response <<- response
   }
   
   .rs.automation.sendRequest(socket, method, params, callback)
   
   # Wait for a response. Pump the later event loop while we wait.
   for (i in 1:100)
   {
      later::run_now()
      if (!is.null(response))
         break
      
      Sys.sleep(0.1)
   }
   
   # Return the received data.
   response
   
})

.rs.addFunction("automation.start", function(rstudioPath = NULL)
{
   .rs.automation.installRequiredPackages()
   
   if (is.null(rstudioPath))
   {
      if (.rs.platform.isMacos)
      {
         rstudioPath <- "/Applications/RStudio.app/Contents/MacOS/RStudio"
      }
   }
   
   # # Start RStudio with debugging port enabled.
   # system2(
   #    rstudioPath,
   #    "--remote-debugging-port=9999",
   #    wait = FALSE
   # )
   
   # Get the websocket debugger URL.
   response <- httr::GET("http://localhost:9999/json/version")
   version <- .rs.fromJSON(rawToChar(response$content))
   socket <- websocket::WebSocket$new(version$webSocketDebuggerUrl)
   
   # Handle websocket messages.
   socket$onMessage(.rs.automation.onMessage)
   socket$onError(.rs.automation.onError)
   socket$onClose(.rs.automation.onClose)
   
   .rs.automation.createClient(socket)
   
})

if (FALSE)
{
   # TODO:
   # - Get current target.
   # - Set a session id.
   # - Create a client for that session.
   client <- .rs.automation.start()
   
   # List the available targets.
   targets <- .rs.automation.sendSynchronousRequest(
      socket = client$.socket,
      method = "Target.getTargets"
   )
   
   sessionId <- .rs.automation.sendSynchronousRequest(
      socket = client$.socket,
      method = "Target.attachToTarget",
      params = list(targetId = targets$result$targetInfos[[2]]$targetId)
   )
   
   .rs.automation.sendSynchronousRequest(
      socket = client$.socket,
      method = "Target.activateTarget",
      params = list(targetId = targets$result$targetInfos[[2]]$targetId)
   )
   
   .rs.automation.sendSynchronousRequest(
      socket    = client$.socket,
      method    = "Runtime.evaluate",
      sessionId = 
      params = list(expression = "1 + 1")
   )
}
