#
# SessionCopilot.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

.rs.setVar("copilot.state", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("copilot_code_completion", function(id, row, column)
{
   # make sure copilot is running
   .rs.copilot.ensureAgentRunning()
   
   # get document properties
   uri <- .rs.copilot.uriFromDocumentId(id)
   text <- .rs.api.documentContents(id)
   
   # request completions at cursor position
   response <- .rs.copilot.sendRequest("getCompletions", list(
      doc = list(
         position = list(line = row, character = column),
         uri = uri,
         version = 1L
      )
   ))
   
   .rs.scalarListFromList(response)
   
})

.rs.addFunction("copilot.tracingEnabled", function()
{
   getOption("rstudio.githubCopilot.tracingEnabled", default = FALSE)
})

.rs.addFunction("copilot.trace", function(fmt, ...)
{
   if (.rs.copilot.tracingEnabled())
   {
     payload <- sprintf(fmt, ...)
     writeLines(paste("[copilot]", payload))
   }
})

.rs.addFunction("copilot.uriFromDocumentId", function(id)
{
   sprintf("rstudio-document://%s", id)
})

.rs.addFunction("copilot.onDocAdded", function(id)
{
   .rs.copilot.trace("[%s] onDocAdded", id)
   .rs.copilot.ensureAgentRunning()
   .rs.copilot.sendNotification("textDocument/didOpen", list(
      textDocument = list(
         uri = .rs.copilot.uriFromDocumentId(id),
         languageId = "r",
         version = 1L,
         text = ""
      )
   ))
})

.rs.addFunction("copilot.onDocUpdated", function(id, contents)
{
   .rs.copilot.trace("[%s] onDocUpdated", id)
   .rs.copilot.ensureAgentRunning()
   
   # TODO: Consider using 'textDocument/didChange' with deltas so we can
   # avoid needing to push the whole document into Copilot on each change.
   .rs.copilot.sendNotification("textDocument/didOpen", list(
      textDocument = list(
         uri = .rs.copilot.uriFromDocumentId(id),
         languageId = "r",
         version = 1L,
         text = contents
      )
   ))
})

.rs.addFunction("copilot.onDocRemoved", function(id)
{
   .rs.copilot.trace("[%s] onDocRemoved", id)
   .rs.copilot.ensureAgentRunning()
   .rs.copilot.sendNotification("textDocument/didClose", list(
      textDocument = list(
         uri = .rs.copilot.uriFromDocumentId(id)
      )
   ))
})

.rs.addFunction("copilot.startAgent", function()
{
   # TODO: We'll want to bundle these.
   nodePath <- Sys.which("node")
   agentPath <- path.expand("~/projects/copilot.vim/copilot/dist/agent.uglify.js")
   
   # TODO: Windows?
   inputFile <- tempfile("copilot-input-")
   inputPipe <- fifo(inputFile, open = "w+b", blocking = FALSE)
   
   outputFile <- tempfile("copilot-output-")
   system(paste("mkfifo", outputFile))
   outputPipe <- fifo(outputFile, open = "r+b", blocking = FALSE)
   
   # TODO: Use our own tooling so we can more easily find the process PID.
   args <- c(agentPath, "<", inputFile, ">", outputFile)
   status <- system2(
      command = nodePath,
      args    = agentPath,
      stdin   = inputFile,
      stdout  = outputFile,
      wait = FALSE
   )
   
   # TODO: barf
   pid <- system("pgrep -nx sh", intern = TRUE)
   
   state <- list(
      pid    = pid,
      stdin  = inputPipe,
      stdout = outputPipe
   )
   
   list2env(state, envir = .rs.copilot.state)
   
   .rs.copilot.sendRequest("initialize", list(
      processId = Sys.getpid(),
      clientInfo = list(
         name = "RStudio",
         version = "1.0.0"
      ),
      capabilities = list()
   ))
   
   state
   
})

.rs.addFunction("copilot.restartAgent", function()
{
   # Shut down the existing agent.
   pid <- .rs.copilot.state$pid
   if (!is.null(pid))
      tools:::pskill(as.integer(pid), signal = tools:::SIGTERM)
   
   # Start a new agent.
   .rs.copilot.startAgent()
})

.rs.addFunction("copilot.isAgentRunning", function()
{
   for (conn in list(.rs.copilot.state$stdin, .rs.copilot.state$stdout))
      if (is.null(conn) || !isOpen(conn))
         return(FALSE)
   
   TRUE
})

.rs.addFunction("copilot.ensureAgentRunning", function()
{
   if (!.rs.copilot.isAgentRunning())
      .rs.copilot.startAgent()
})

.rs.addFunction("copilot.sendRequestImpl", function(method, id, params)
{
   # ensure params are named, even for empty case
   if (is.null(names(params)))
      names(params) <- rep.int("", length(params))
   
   # construct data
   data <- list()
   data$jsonrpc <- "2.0"
   data$id <- id
   data$method <- method
   data$params <- params
   
   # build JSON payload
   json <- .rs.toJSON(data, unbox = TRUE)
   
   # include content length header
   header <- paste("Content-Length:", nchar(json, type = "bytes"))
   message <- paste(c(header, "", json), collapse = "\r\n")
   
   # send the request
   result <- tryCatch(
      writeBin(charToRaw(message), .rs.copilot.state$stdin),
      condition = identity
   )
   
   if (inherits(result, "condition")) {
      warning(result)
      .rs.copilot.restartAgent()
   }
   
   # if this is a notification (that is, no id) then return early
   if (is.null(id))
      return(NULL)
   
   # otherwise, read lines from stdout until we get a response
   repeat {
      
      line <- readLines(.rs.copilot.state$stdout, n = 1L, warn = FALSE)
      response <- tryCatch(.rs.fromJSON(line), error = identity)
      if (inherits(response, "error") || is.null(response$id)) {
         str(response)
         next
      }
      
      if (!identical(response$id, id)) {
         fmt <- "dropping response with unknown id '%s'"
         warning(sprintf(fmt, response$id))
         next
      }

      str(response)
      return(response)
      
   }
   
})

.rs.addFunction("copilot.sendNotification", function(method,
                                                     params = list(),
                                                     callback = NULL)
{
   .rs.copilot.sendRequestImpl(method, NULL, params)
})

.rs.addFunction("copilot.sendRequest", function(method,
                                                params = list(),
                                                callback = NULL)
{
   .rs.copilot.sendRequestImpl(method, uuid::UUIDgenerate(), params)
})

.rs.addFunction("copilot.signInInitiate", function()
{
   .rs.copilot.ensureAgentRunning()
   
   response <- .rs.copilot.sendRequest("signInInitiate")
   if (identical(response$result$status, "AlreadySignedIn"))
   {
      msg <- sprintf("[copilot] Already signed in as user '%s'.", response$result$user)
      writeLines(msg)
   }
   else if (identical(response$result$status, "PromptUserDeviceFlow"))
   {
      writeLines(paste("Navigating to", response$result$verificationUri), "...")
      writeLines(paste("Verification code:", response$result$userCode))
      Sys.sleep(3)
      browseURL(response$result$verificationUri)
      
      readline("Press enter after you have finished verification.")
      
      response <- .rs.copilot.sendRequest("checkStatus")
      status <- response$result$status
      if (!identical(status, "OK"))
         stop(sprintf("Error during authentication: %s", status))
      
      writeLines(sprintf("Logged in as user '%s'.", response$result$user))
   }
   
   TRUE
   
})

.rs.addFunction("copilot.signOut", function()
{
   .rs.copilot.ensureAgentRunning()
   
   response <- .rs.copilot.sendRequest("checkStatus")
   if (identical(response$result$status, "NotSignedIn"))
   {
      writeLines("[copilot] No user is currently signed in; nothing to do.")
   }
   else if (identical(response$result$status, "NotSignedIn"))
   {
      writeLines("[copilot] Successfully signed out.")
   }
})

.rs.addFunction("copilot.checkStatus", function()
{
   .rs.copilot.ensureAgentRunning()
   .rs.copilot.sendRequest("checkStatus")
})
