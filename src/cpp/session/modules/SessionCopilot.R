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

.rs.addFunction("copilot.installCopilotAgent", function(targetDirectory)
{
   # Get path to copilot payload
   defaultCopilotUrl <- "https://rstudio-buildtools.s3.amazonaws.com/copilot/copilot.tar.gz"
   copilotUrl <- getOption("rstudio.copilot.agentUrl", default = defaultCopilotUrl)
   
   # Download to temporary directory
   destfile <- tempfile("rstudio-copilot-", fileext = ".tar.gz")
   download.file(
      url = copilotUrl,
      destfile = destfile,
      mode = "wb"
   )
   
   # Extract to target directory
   .rs.ensureDirectory(targetDirectory)
   untar(destfile, exdir = targetDirectory)
   
   # Confirm the agent runtime exists
   file.exists(file.path(targetDirectory, "agent.js"))
})

.rs.addFunction("copilot.agentPid", function()
{
   .Call("rs_copilotAgentPid", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.uriFromDocumentId", function(id)
{
   sprintf("rstudio-document://%s", id)
})

.rs.addFunction("copilot.startAgent", function()
{
   .Call("rs_copilotStartAgent", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.restartAgent", function()
{
   .Call("rs_copilotStartAgent", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.isAgentRunning", function()
{
   .Call("rs_copilotAgentRunning", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.ensureAgentRunning", function()
{
   if (.rs.copilot.isAgentRunning())
      .rs.copilot.agentPid()
   else
      .rs.copilot.startAgent()
})

.rs.addFunction("copilot.sendRequestImpl", function(method, id, params)
{
   # NOTE: We convert 'params' to JSON here so we can control unboxing.
   params <- .rs.toJSON(params, unbox = TRUE)
   .Call("rs_copilotSendRequest", method, id, params, PACKAGE = "(embedding)")
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


