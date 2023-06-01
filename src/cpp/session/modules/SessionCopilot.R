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

.rs.addFunction("copilot.setLogLevel", function(level = 0L)
{
   .Call("rs_copilotSetLogLevel", as.integer(level))
})

# TODO: What's the right way to allow the Copilot Agent version to change?
# How should we handle updates?
.rs.addFunction("copilot.installCopilotAgent", function(targetDirectory)
{
   # NOTE: Copilot 1.8.4 release.
   defaultCopilotRef <- "1358e8e45ecedc53daf971924a0541ddf6224faf"
   
   # Get path to copilot payload
   defaultCopilotUrl <- file.path(
      "https://api.github.com/repos/github/copilot.vim/tarball",
      copilotRef
   )
   
   # Check for overrides.
   copilotRef <- getOption("rstudio.copilot.repositoryRef", default = defaultCopilotRef)
   copilotUrl <- getOption("rstudio.copilot.repositoryUrl", default = defaultCopilotUrl)
   
   # Create and use a temporary directory to host the download.
   downloadDir <- tempfile("copilot-")
   .rs.ensureDirectory(downloadDir)
   on.exit(unlink(downloadDir, recursive = TRUE), add = TRUE)
   
   # Download the tarball.
   destfile <- file.path(downloadDir, "copilot.tar.gz")
   download.file(copilotUrl, destfile = destfile, mode = "wb")
   
   # Confirm the tarball exists.
   if (!file.exists(destfile)) {
      fmt <- "Copilot Agent installation failed: '%s' does not exist."
      msg <- sprintf(fmt, destfile)
      stop(msg, call. = FALSE)
   }
   
   # Extract the tarball. Make sure things get unpacked into the download dir.
   local({
      owd <- setwd(downloadDir)
      on.exit(setwd(owd), add = TRUE)
      untar(destfile)
   })
   
   
   # Find the unpacked directory.
   copilotFolder <- setdiff(list.files(downloadDir), "copilot.tar.gz")
   copilotAgentPath <- file.path(downloadDir, copilotFolder, "copilot/dist")
   copilotAgentFiles <- list.files(copilotAgentPath, all.files = TRUE, full.names = TRUE)
   
   # Copy those files to our target directory.
   .rs.ensureDirectory(targetDirectory)
   file.copy(copilotAgentFiles, targetDirectory)
   
   # Confirm the agent runtime exists
   agentPath <- file.path(targetDirectory, "agent.js")
   if (!file.exists(agentPath))
   {
      fmt <- "Copilot Agent installation failed: '%s' does not exist."
      msg <- sprintf(fmt, agentPath)
      stop(msg, call. = FALSE)
   }
   
   TRUE
})

.rs.addFunction("copilot.agentPid", function()
{
   .Call("rs_copilotAgentPid", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.isEnabled", function()
{
   .Call("rs_copilotEnabled", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.startAgent", function()
{
   .Call("rs_copilotStartAgent", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.isAgentRunning", function()
{
   .Call("rs_copilotAgentRunning", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.restartAgent", function()
{
   .Call("rs_copilotStartAgent", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.stopAgent", function()
{
   .Call("rs_copilotStopAgent", PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.ensureAgentRunning", function()
{
   if (.rs.copilot.isAgentRunning())
      .rs.copilot.agentPid()
   else if (.rs.copilot.isEnabled())
      .rs.copilot.startAgent()
   else
      FALSE
})

.rs.addFunction("copilot.sendRequestImpl", function(method, id, params)
{
   # NOTE: We convert 'params' to JSON here so we can control unboxing.
   params <- if (length(params) == 0L)
      "{}"
   else
      .rs.toJSON(params, unbox = TRUE)
   
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
   if (!.rs.copilot.ensureAgentRunning())
      return()
   
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
   if (!.rs.copilot.ensureAgentRunning())
      return()
   
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
   if (!.rs.copilot.ensureAgentRunning())
      return()
   
   .rs.copilot.sendRequest("checkStatus")
})


