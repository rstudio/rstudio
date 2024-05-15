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

# Global variable for tracking the active automation agent.
.rs.setVar("automation.agentProcess", NULL)


.rs.addFunction("automation.installRequiredPackages", function()
{
   packages <- c("here", "httr", "later", "processx", "ps", "websocket", "withr", "xml2")
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

.rs.addFunction("automation.applicationPath", function(mode)
{
   switch(
      mode,
      server  = .rs.automation.applicationPathServer(),
      desktop = .rs.automation.applicationPathDesktop(),
   )
})

.rs.addFunction("automation.ensureRunningServerInstance", function()
{
   # Check and see if we already have an rserver instance listening.
   procs <- subset(ps::ps(), name == "rserver")
   for (i in seq_len(nrow(procs)))
   {
      proc <- procs[i, ]
      conns <- ps::ps_connections(proc$ps_handle[[1L]])
      if (8788L %in% conns$lport)
         return(TRUE)
   }
   
   # See if we can figure out how the parent was launched, and use that
   # to infer whether we can launch the automation helper.
   parentHandle <- ps::ps_parent()
   parentEnv <- ps::ps_environ(parentHandle)
   parentPwd <- parentEnv[["PWD"]]
   automationScript <- file.path(parentPwd, "rserver-automation")
   if (file.exists(automationScript))
   {
      message("-- Starting rserver-automation ...")
      withr::with_dir(parentPwd, system2(automationScript, wait = FALSE))
   }
   else
   {
      stop("rserver does not appear to be running on port 8788")
   }
})

.rs.addFunction("automation.initialize", function(appPath = NULL,
                                                  mode = c("server", "desktop"),
                                                  port = NULL)
{
   # Make sure all requisite packages are installed.
   .rs.automation.installRequiredPackages()
   
   # Resolve arguments.
   mode <- match.arg(mode)
   port <- .rs.nullCoalesce(port, if (mode == "server") 9999L else 9998L)
   
   # Ensure that we have a running rserver instance.
   if (mode == "server")
      .rs.automation.ensureRunningServerInstance()
   
   # Check for an existing session we can attach to.
   baseUrl <- sprintf("http://localhost:%i", port)
   jsonVersionUrl <- file.path(baseUrl, "json/version")
   response <- .rs.tryCatch(httr::GET(jsonVersionUrl))
   if (!inherits(response, "error"))
      return(.rs.automation.attach(baseUrl, mode))
   
   # No existing session; start a new one and attach to it.
   appPath <- .rs.nullCoalesce(appPath, .rs.automation.applicationPath(mode))
   
   # Set up environment for newly-launched RStudio instance.
   envVars <- as.list(Sys.getenv())
   
   # Unset any RStudio-specific environment variables, so that this looks
   # like a "fresh" RStudio session.
   rstudioEnvVars <- grep("^(?:RS|RSTUDIO)_", names(envVars))
   envVars[rstudioEnvVars] <- list(NULL)
   envVars["R_SESSION_TMPDIR"] <- list(NULL)
   
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
   
   # Build argument list.
   # https://github.com/GoogleChrome/chrome-launcher/blob/main/docs/chrome-flags-for-tools.md
   args <- c(
      sprintf("--remote-debugging-port=%i", port),
      sprintf("--user-data-dir=%s", tempdir()),
      if (mode == "desktop") "--automation-agent",
      if (mode == "server") c(
         "--no-default-browser-check",
         "--no-first-run",
         "--disable-extensions",
         "--disable-features=PrivacySandboxSettings4",
         "http://localhost:8788"
      )
   )
   
   # Start up RStudio.
   process <- withr::with_envvar(envVars, {
      owd <- setwd(stateDir)
      on.exit(setwd(owd), add = TRUE)
      processx::process$new(appPath, args)
   })
   
   # Wait until the process is running.
   while (process$get_status() != "running")
      Sys.sleep(0.1)
   
   # Start pinging the Chromium HTTP server.
   response <- NULL
   .rs.waitUntil(function()
   {
      response <<- .rs.tryCatch(httr::GET(jsonVersionUrl))
      !inherits(response, "error")
   })
   
   # We have a live process; save it so we can interact with it later.
   .rs.setVar("automation.agentProcess", process)
   
   # The session is ready; attach now.
   jsonResponse <- .rs.fromJSON(rawToChar(response$content))
   .rs.automation.attach(baseUrl, mode, jsonResponse$webSocketDebuggerUrl)
   
})

.rs.addFunction("automation.attach", function(baseUrl, mode, url = NULL)
{
   # Clear a previous session ID if necessary.
   .rs.setVar("automation.client", NULL)
   .rs.setVar("automation.sessionId", NULL)
   
   # Get the websocket debugger URL.
   url <- .rs.nullCoalesce(url, {
      jsonVersionUrl <- file.path(baseUrl, "json/version")
      response <- httr::GET(jsonVersionUrl)
      jsonResponse <- .rs.fromJSON(rawToChar(response$content))
      jsonResponse$webSocketDebuggerUrl
   })
   
   # Create the websocket.
   socket <- websocket::WebSocket$new(url)
   
   # Handle websocket messages.
   socket$onMessage(.rs.automation.onMessage)
   socket$onError(.rs.automation.onError)
   socket$onClose(.rs.automation.onClose)
   
   # Wait until the socket is open.
   .rs.waitUntil(function()
   {
      later::run_now()
      socket$readyState() == 1L
   }, waitTimeSecs = 0.1)
   
   # Create the automation client.
   client <- .rs.automation.createClient(socket)
   
   # Find and record the active session id.
   .rs.automation.attachToSession(client, mode)
   
   # Wait until the Console is available.
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

.rs.addFunction("automation.attachToSession", function(client, mode)
{
   callback <- switch(mode,
                      desktop = .rs.automation.attachToSessionDesktop,
                      server  = .rs.automation.attachToSessionServer
   )
   
   for (i in 1:10)
   {
      sessionId <- tryCatch(callback(client), error = identity)
      if (is.character(sessionId))
         return(sessionId)
      
      Sys.sleep(1)
   }
   
   stop("Couldn't attach to session")
})

.rs.addFunction("automation.attachToSessionDesktop", function(client)
{
   # Try to get the available targets.
   targets <- .rs.tryCatch(client$Target.getTargets())
   if (inherits(targets, "error"))
      return(NULL)
   
   # Check for the RStudio window.
   currentTarget <- Find(function(target) target$title == "RStudio", targets$targetInfos)
   if (is.null(currentTarget))
      return(NULL)
   
   # Attach to this target.
   currentTargetId <- currentTarget$targetId
   response <- client$Target.attachToTarget(targetId = currentTargetId, flatten = TRUE)
   sessionId <- response$sessionId
   
   # Update our global variables.
   .rs.setVar("automation.client", client)
   .rs.setVar("automation.sessionId", sessionId)
   
   # Return the discovered session ID.
   sessionId
})

.rs.addFunction("automation.attachToSessionServer", function(client)
{
   # Try to get the available targets.
   targets <- .rs.tryCatch(client$Target.getTargets())
   if (inherits(targets, "error"))
      return(NULL)
   
   # If we don't have any targets, then create a new session.
   if (length(targets) == 0L)
   {
      client$Target.createTarget(url = "about:blank")
      targets <- .rs.tryCatch(client$Target.getTargets())
      if (inherits(targets, "error"))
         return(NULL)
   }
   
   # Find a page.
   currentTarget <- Find(function(target) target$type == "page", targets$targetInfos)
   if (is.null(currentTarget))
      return(NULL)
   
   # Attach to this target.
   currentTargetId <- currentTarget$targetId
   response <- client$Target.attachToTarget(targetId = currentTargetId, flatten = TRUE)
   sessionId <- response$sessionId
   
   # Update our global variables.
   .rs.setVar("automation.client", client)
   .rs.setVar("automation.sessionId", sessionId)
   
   # TODO: Handle input of authentication credentials?
   # Should that happen here, or elsewhere?
   
   # Return the session id.
   sessionId
})

.rs.addFunction("automation.run", function(ref = NULL, mode = c("server", "desktop"))
{
   # Figure out the commit reference to use.
   ref <- .rs.nullCoalesce(ref, {
      productInfo <- .Call("rs_getProductInfo", PACKAGE = "(embedding)")
      productInfo$commit
   })
   
   # Work in a temporary directory.
   automationDir <- tempfile("rstudio-automation-")
   dir.create(automationDir)
   owd <- setwd(automationDir)
   on.exit(setwd(owd), add = TRUE)
   
   # Perform a sparse checkout to retrieve the automation files.
   gitScript <- .rs.heredoc('
      git init -b %1$s
      git remote add origin git@github.com:rstudio/rstudio.git
      git config core.sparseCheckout true
      echo "src/cpp/tests/automation" >> .git/info/sparse-checkout
      git fetch --depth=1 origin %1$s
      git checkout %1$s
   ', shQuote(ref))
   
   fileext <- if (.rs.platform.isWindows) ".bat" else ".sh"
   gitScriptFile <- tempfile("git-checkout-", fileext = fileext)
   writeLines(gitScript, con = gitScriptFile)
   Sys.chmod(gitScriptFile, mode = "0755")
   system2(gitScriptFile)
   
   # Move to the automation directory.
   setwd("src/cpp/tests/automation")
   
   # Run the tests.
   envVars <- list(RSTUDIO_AUTOMATION_MODE = mode)
   withr::with_envvar(envVars, source("testthat.R"))
})
