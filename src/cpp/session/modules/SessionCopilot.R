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
   copilotRef <- getOption(
      "rstudio.copilot.repositoryRef",
      default = "1358e8e45ecedc53daf971924a0541ddf6224faf"
   )
   
   copilotBaseUrl <- getOption(
      "rstudio.copilot.repositoryUrl",
      default = "https://rstudio.org/link/github-copilot"
   )
   
   # Get path to copilot payload
   copilotUrl <- paste(c(copilotBaseUrl, copilotRef), collapse = "/")
   
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
