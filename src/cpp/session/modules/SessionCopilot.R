#
# SessionCopilot.R
#
# Copyright (C) 2023 by Posit Software, PBC
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
   .Call("rs_copilotSetLogLevel", as.integer(level), PACKAGE = "(embedding)")
})

# TODO: What's the right way to allow the Copilot Agent version to change?
# How should we handle updates?
.rs.addFunction("copilot.installCopilotAgent", function(targetDirectory)
{
   # NOTE: Copilot 1.10.3 release.
   defaultCopilotRef <- "998cf5ab1b85e844c7e8edb864a997e590df7182"
   copilotRef <- getOption("rstudio.copilot.repositoryRef", defaultCopilotRef)
   
   defaultCopilotBaseUrl <- "https://rstudio.org/links/github-copilot"
   copilotBaseUrl <- getOption("rstudio.copilot.repositoryUrl", defaultCopilotBaseUrl)
   
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
   # NOTE: The copilot agent used to be bundled within the 'copilot/dist' sub-directory,
   # but was moved to the 'dist' sub-directory in a recent release. We check both
   # just to be safe.
   copilotFolder <- setdiff(list.files(downloadDir), "copilot.tar.gz")
   for (suffix in c("copilot/dist", "dist")) {
      copilotAgentDirectory <- file.path(downloadDir, copilotFolder, suffix)
      if (file.exists(copilotAgentDirectory))
         break
   }
   
   # Copy the directory recursively.
   unlink(targetDirectory, recursive = TRUE)
   .rs.ensureDirectory(dirname(targetDirectory))
   file.copy(copilotAgentDirectory, dirname(targetDirectory), recursive = TRUE)
   
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

.rs.addFunction("copilot.networkProxy", function()
{
   # check for proxy from option
   proxy <- getOption("rstudio.copilot.networkProxy")
   if (is.null(proxy))
      return(NULL)
 
   # return as scalar list  
   .rs.scalarListFromList(proxy)
})

.rs.addFunction("copilot.parseNetworkProxyUrl", function(url)
{
   # build regex
   reProxyUrl <- paste0(
      "(?:(\\w+)://)?",         # protocol (optional)
      "(?:([^:]+):([^@]+)@)?",  # username + password (optional)
      "([^:]+):(\\d+)"          # host + port (required)
   )
   
   # attempt to match
   networkProxy <- as.list(regmatches(url, regexec(reProxyUrl, url))[[1L]])
   if (length(networkProxy) != 6L)
      warning("couldn't parse network proxy url '", url, "'")
   
   # set names of matched values
   names(networkProxy) <- c("url", "protocol", "user", "pass", "host", "port")
   
   # drop empty strings
   networkProxy[!nzchar(networkProxy)] <- NULL
   
   # validate the protocol, if it was set
   protocol <- .rs.nullCoalesce(networkProxy$protocol, "http")
   if (protocol != "http")
      warning("only 'http' network proxies are supported")
   
   # drop the 'url' and 'protocol' fields as they are not used by copilot
   networkProxy[c("url", "protocol")] <- NULL

   # return rest of the data   
   .rs.scalarListFromList(networkProxy)
})
