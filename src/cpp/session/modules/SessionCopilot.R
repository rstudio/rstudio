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

.rs.addFunction("copilot.sendRequest", function(method, params = list())
{
   .Call("rs_copilotSendRequest", as.character(method), as.list(params), PACKAGE = "(embedding)")
})

.rs.addFunction("copilot.downloadCopilotAgent", function(copilotUrl, destfile)
{
   # Disable warnings in this scope
   op <- options(warn = -1L)
   on.exit(options(op), add = TRUE)
   
   # Attempt to download the file
   status <- tryCatch(
      download.file(copilotUrl, destfile, mode = "wb"),
      error = identity
   )
   
   # If the download failed, and we're on Windows, try again with a different
   # download method. This may be necessary for certain proxy setups.
   #
   # https://github.com/rstudio/rstudio/issues/13868
   tryAgain <-
      .rs.platform.isWindows &&
      inherits(status, "error") &&
      !identical(getOption("download.file.method"), "wininet")
   
   if (tryAgain)
   {
      # Try to download the file using the 'wininet' method
      status <- tryCatch(
         download.file(copilotUrl, destfile, method = "wininet", mode = "wb", extra = NULL),
         error = identity
      )
   }
   
   # If the download still failed, raise the error now
   if (inherits(status, "error"))
      stop(status)
   
})

.rs.addFunction("copilot.installCopilotAgent", function(targetDirectory)
{
   copilotRef <- .Call("rs_copilotAgentCommitHash", PACKAGE = "(embedding)")
   
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
   .rs.copilot.downloadCopilotAgent(copilotUrl, destfile)
   
   # Confirm the tarball exists.
   if (!file.exists(destfile)) {
      fmt <- "Copilot Agent installation failed: '%s' does not exist."
      msg <- sprintf(fmt, destfile)
      stop(msg, call. = FALSE)
   }
   
   # Extract the tarball. Make sure things get unpacked into the download dir.
   local({
      
      # Move to download directory.
      owd <- setwd(downloadDir)
      on.exit(setwd(owd), add = TRUE)
      
      # Make sure we have a valid tar set up.
      # https://github.com/rstudio/rstudio/issues/13746
      tar <- Sys.getenv("TAR")
      if (!file.exists(tar))
         tar <- Sys.which("tar")
      
      # Extract the archive.
      untar(destfile, tar = tar)
      
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
   
   # Write out a meta.json object so we can detect whether this installation
   # of Copilot is out-of-date.
   metaPath <- file.path(targetDirectory, "../version.json")
   dir.create(dirname(metaPath), recursive = TRUE, showWarnings = FALSE)
   meta <- list(commit_hash = copilotRef)
   writeLines(.rs.toJSON(meta, unbox = TRUE), con = metaPath)
   
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
      "^",
      "(?:(\\w+)://)?",         # protocol (optional)
      "(?:([^:]+):([^@]+)@)?",  # username + password (optional)
      "([^:]+):(\\d+)",         # host + port (required)
      "/?",                     # optional trailing slash, just in case
      "$"
   )
   
   # attempt to match
   networkProxy <- as.list(regmatches(url, regexec(reProxyUrl, url))[[1L]])
   if (length(networkProxy) != 6L)
      warning("couldn't parse network proxy url '", url, "'", call. = FALSE)
   
   # set names of matched values
   names(networkProxy) <- c("url", "protocol", "user", "pass", "host", "port")
   
   # drop empty strings
   networkProxy[!nzchar(networkProxy)] <- NULL
   
   # validate the protocol, if it was set
   protocol <- .rs.nullCoalesce(networkProxy$protocol, "http")
   if (protocol != "http")
      warning("only 'http' network proxies are supported by the GitHub Copilot agent", call. = FALSE)
   
   # drop the 'url' and 'protocol' fields as they are not used by copilot
   networkProxy[c("url", "protocol")] <- NULL
   
   # port needs to be a number
   networkProxy[["port"]] <- as.integer(networkProxy[["port"]])

   # return rest of the data   
   .rs.scalarListFromList(networkProxy)
})

