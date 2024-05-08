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

# Global handlers for WebSocket messages and responses.
.rs.setVar("automation.callbacks", new.env(parent = emptyenv()))
.rs.setVar("automation.messageId", 0L)
.rs.setVar("automation.responses", new.env(parent = emptyenv()))

# Global state tracking the active client + session ID.
.rs.setVar("automation.client", NULL)
.rs.setVar("automation.sessionId", NULL)

# The default application mode.
.rs.setVar("automation.applicationMode", "desktop")

.rs.addFunction("automation.installRequiredPackages", function()
{
   packages <- c("here", "httr", "later", "websocket", "withr", "xml2")
   pkgLocs <- find.package(packages, quiet = TRUE)
   if (length(packages) == length(pkgLocs))
      return()
   
   writeLines("==> Installing Packages")
   for (package in packages)
   {
      if (!requireNamespace(package, quietly = TRUE))
      {
         install.packages(package)
         loadNamespace(package)
      }
   }
})

.rs.addFunction("automation.onMessage", function(event)
{
   # Small bit of indirection to make hot reloading easier.
   .rs.automation.onMessageImpl(event)
})

.rs.addFunction("automation.onMessageImpl", function(event)
{
   # Get the data associated with this request.
   data <- .rs.fromJSON(event[["data"]])
   
   # TODO: Check for an error status.
   
   # TODO: Handle events. Right now we just ignore them.
   if (!is.null(data[["method"]]))
      return()
   
   # Check for a callback associated with this id.
   id <- data[["id"]]
   if (is.null(id)) {
      print(event)
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
   .rs.setVar("automation.messageId", .rs.automation.messageId + 1L)
   
   # Register a callback for this message id.
   assign(as.character(id), callback, envir = .rs.automation.callbacks)
   
   # Generate the request.
   request <- list(
      id     = id,
      method = method,
      params = params
   )
   
   # Attach a session ID if one is available.
   if (!is.null(.rs.automation.sessionId))
      request[["sessionId"]] <- .rs.automation.sessionId
   
   # Fire it off.
   json <- .rs.toJSON(request, unbox = TRUE)
   socket$send(json)
   
   # Return the id.
   invisible(id)
})

.rs.addFunction("automation.sendSynchronousRequest", function(socket, method, params = list())
{
   # Drop NULL parameters.
   params <- params[!vapply(params, is.null, FUN.VALUE = logical(1))]
   
   # Make sure parameters are named.
   names(params) <- .rs.nullCoalesce(names(params), rep.int("", length(params)))
   
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
   
   # Handle errors.
   error <- response[["error"]]
   if (!is.null(error)) {
      fmt <- "execution of '%s' failed: %s [error code %i]"
      msg <- sprintf(fmt, method, error[["message"]], error[["code"]])
      stop(msg, call. = FALSE)
   }
   
   # Return the received data.
   response[["result"]]
   
})

.rs.addFunction("automation.applicationPathServer", function()
{
   if (.rs.platform.isMacos)
   {
      "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
   }
   else if (.rs.platform.isWindows)
   {
      "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe"
   }
   else
   {
      Sys.which("chromium")
   }
})

.rs.addFunction("automation.applicationPathDesktop", function()
{
   if (.rs.platform.isMacos)
   {
      "/Applications/RStudio.app/Contents/MacOS/RStudio"
   }
   else if (.rs.platform.isWindows)
   {
      "C:/Program Files/RStudio/rstudio.exe"
   }
   else
   {
      "/usr/bin/rstudio"
   }
})

.rs.addFunction("automation.applicationPath", function()
{
   override <- Sys.getenv("RSTUDIO_AUTOMATION_APPLICATION_PATH", unset = NA)
   if (!is.na(override))
      return(override)
   
   switch(
      .rs.automation.applicationMode,
      server  = .rs.automation.applicationPathServer(),
      desktop = .rs.automation.applicationPathDesktop(),
   )
})

.rs.addFunction("automation.initialize", function(appPath = NULL, port = 9999L)
{
   # Make sure all requisite packages are installed.
   .rs.automation.installRequiredPackages()
   
   # Check for an existing session we can attach to.
   baseUrl <- sprintf("http://localhost:%i", port)
   jsonVersionUrl <- file.path(baseUrl, "json/version")
   response <- .rs.tryCatch(httr::GET(jsonVersionUrl))
   if (!inherits(response, "error"))
      return(.rs.automation.attach(baseUrl))
   
   # No existing session; start a new one and attach to it.
   appPath <- .rs.nullCoalesce(appPath, .rs.automation.applicationPath())
   
   # TODO: Do something smarter so we can get the PID. For example, start
   # RStudio with a flag indicating it should write its PID to a file at
   # some location.
   oldPid <- suppressWarnings(
      system(paste("pgrep -nx", basename(appPath)), intern = TRUE)
   )
   
   # Set up environment for newly-launched RStudio instance.
   envVars <- Sys.getenv()
   envVars <- envVars[grep("^(?:RS|RSTUDIO)_", names(envVars), invert = TRUE)]
   
   # Ensure that the new RStudio instance uses temporary storage.
   stateDir <- tempfile("rstudio-automation-state-")
   dir.create(stateDir, recursive = TRUE)
   
   configHome <- file.path(stateDir, "config-home")
   configDir  <- file.path(stateDir, "config-dir")
   dataHome   <- file.path(stateDir, "data-home")
   
   envVars[["RSTUDIO_CONFIG_HOME"]] <- configHome
   envVars[["RSTUDIO_CONFIG_DIR"]]  <- configDir
   envVars[["RSTUDIO_DATA_HOME"]]   <- dataHome
   
   # Create a default JSON configuration file.
   config <- list(
      auto_save_on_idle = "none",
      continue_comments_on_newline = FALSE,
      windows_terminal_shell = "win-cmd"
   )
   
   configJson <- .rs.toJSON(config, unbox = TRUE)
   configPath <- file.path(configHome, "rstudio-prefs.json")
   .rs.ensureDirectory(dirname(configPath))
   writeLines(configJson, con = configPath)
   
   # Avoid displaying modal dialogs on startup.
   envVars[["RS_NO_SPLASH"]] <- "1"
   envVars[["RS_CRASH_HANDLER_PROMPT"]] <- "false"
   envVars[["RSTUDIO_DISABLE_CHECK_FOR_UPDATES"]] <- "1"
   
   # Start up RStudio.
   withr::with_envvar(envVars, {
      remoteDebuggingPortArg <- sprintf("--remote-debugging-port=%i", port)
      args <- c("--automation-agent", remoteDebuggingPortArg)
      system2(appPath, args, wait = FALSE)
   })
         
   # Wait a bit until we have a new RStudio instance.
   while (TRUE)
   {
      newPid <- suppressWarnings(
         system(paste("pgrep -nx", basename(appPath)), intern = TRUE)
      )
      
      if (!identical(oldPid, newPid))
         break
   }
   
   # Sleep for a second to let the Chromium HTTP server come to life.
   Sys.sleep(3)
   
   # The session is ready; attach now.
   .rs.automation.attach(baseUrl)
   
})

.rs.addFunction("automation.attach", function(baseUrl)
{
   # Clear a previous session ID if necessary.
   .rs.setVar("automation.client", NULL)
   .rs.setVar("automation.sessionId", NULL)
   
   # Get the websocket debugger URL.
   jsonVersionUrl <- file.path(baseUrl, "json/version")
   response <- httr::GET(jsonVersionUrl)
   version <- .rs.fromJSON(rawToChar(response$content))
   socket <- websocket::WebSocket$new(version$webSocketDebuggerUrl)
   
   # Handle websocket messages.
   socket$onMessage(.rs.automation.onMessage)
   socket$onError(.rs.automation.onError)
   socket$onClose(.rs.automation.onClose)
   
   # Wait until the socket is open.
   while (socket$readyState() != 1L)
   {
      later::run_now()
      Sys.sleep(0.1)
   }
   
   # Create the automation client.
   client <- .rs.automation.createClient(socket)
   
   # Find and record the active session id.
   for (i in 1:10) {
      
      # Try to get the available targets.
      targets <- .rs.tryCatch(client$Target.getTargets())
      if (inherits(targets, "error"))
      {
         Sys.sleep(1)
         next
      }
      
      # Check for the RStudio window.
      currentTarget <- Find(function(target) target$title == "RStudio", targets$targetInfos)
      if (is.null(currentTarget))
      {
         Sys.sleep(1)
         next
      }
      
      currentTargetId <- currentTarget$targetId
      response <- client$Target.attachToTarget(targetId = currentTargetId, flatten = TRUE)
      break
   }
   
   # Update our global variables.
   .rs.setVar("automation.client", client)
   .rs.setVar("automation.sessionId", response$sessionId)
   
   # Wait until we have an RStudio console.
   document <- client$DOM.getDocument(depth = 0L)
   .rs.waitUntil(function()
   {
      consoleNode <- client$DOM.querySelector(document$root$nodeId, "#rstudio_console_input")
      consoleNode$nodeId != 0L
   })
   
   # Save the root document node ID, as we'll need it for queries later.
   client$documentNodeId <- document$root$nodeId
   
   # Return the client.
   client
   
})

.rs.addFunction("automation.run", function(mode = c("server", "desktop"))
{
   mode <- match.arg(mode)
   .rs.setVar("automation.applicationMode", mode)
   
   projectRoot <- .rs.api.getActiveProject()
   entrypoint <- file.path(projectRoot, "src/cpp/tests/automation/testthat.R")
   source(entrypoint)
})
