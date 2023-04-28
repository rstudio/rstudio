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
   .rs.copilot.ensureRunning()
   
   # get document properties
   properties <- .Call("rs_documentProperties", id, TRUE)
   uri <- path.expand(properties$path)
   text <- properties$contents
   
   # send document contents to copilot
   .rs.copilot.sendNotification("textDocument/didOpen", list(
      textDocument = list(
         uri = uri,
         languageId = "r",
         version = 1L,
         text = text
      )
   ))
   
   # now, request completions at the cursor position
   response <- .rs.copilot.sendRequest("getCompletionsCycling", list(
      doc = list(
         position = list(line = row, character = column),
         path = path.expand(.rs.api.documentPath()),
         uri = path.expand(.rs.api.documentPath()),
         version = 1L
      )
   ))
   
   response
   
})

.rs.addFunction("copilot.startRunning", function()
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

.rs.addFunction("copilot.isRunning", function()
{
   for (conn in list(.rs.copilot.state$stdin, .rs.copilot.state$stdout))
      if (is.null(conn) || !isOpen(conn))
         return(FALSE)
   
   TRUE
})

.rs.addFunction("copilot.ensureRunning", function()
{
   if (!.rs.copilot.isRunning())
      .rs.copilot.startRunning()
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
   json <- jsonlite::toJSON(data, auto_unbox = TRUE)
   
   # include content length header
   header <- paste("Content-Length:", nchar(json, type = "bytes"))
   message <- paste(c(header, "", json), collapse = "\r\n")
   
   # send the request
   writeBin(charToRaw(message), .rs.copilot.state$stdin)
   
   # if this is a notification (that is, no id) then return early
   if (is.null(id))
      return(NULL)
   
   # otherwise, read lines from stdout until we get a response
   while (TRUE) {
      
      line <- readLines(.rs.copilot.state$stdout, n = 1L, warn = FALSE)
      response <- tryCatch(jsonlite::fromJSON(line), error = identity)
      if (inherits(response, "error") || is.null(response$id))
         next
      
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
