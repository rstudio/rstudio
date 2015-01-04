#
# SessionRSConnect.R
#
# Copyright (C) 2009-14 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("scalarListFromFrame", function(frame)
{
   ret <- list()
   cols <- names(frame)

   # take apart the frame and compose a list of scalars from each row
   for (i in seq_len(nrow(frame))) {
      row <- lapply(cols, 
                    function(col) { if (is.null(frame[i,col])) NULL 
                                    else .rs.scalar(unlist(frame[i,col])) })
      names(row) <- cols
      ret[[i]] <- row
   }
   return(ret)
})


.rs.addJsonRpcHandler("get_rsconnect_account_list", function() {
   accounts <- rsconnect::accounts(server = "shinyapps.io")
   if (is.null(accounts))
      character()
   else
      accounts$name
})

.rs.addJsonRpcHandler("remove_rsconnect_account", function(account) {
   rsconnect::removeAccount(account, server = "shinyapps.io")
})

.rs.addJsonRpcHandler("get_rsconnect_app_list", function(account) {
   .rs.scalarListFromFrame(rsconnect::applications(account,
                                                   server = "shinyapps.io"))
})

.rs.addJsonRpcHandler("get_rsconnect_deployments", function(dir) {
   .rs.scalarListFromFrame(rsconnect::deployments(dir))
})

.rs.addFunction("maxDirectoryList", function(dir, root, cur_size, max_size, 
                                             exclude_dirs, exclude_ext) {
  # generate a list of files at this level
  contents <- list.files(dir, recursive = FALSE, all.files = FALSE,
                         include.dirs = TRUE, no.. = TRUE, full.names = FALSE)
  
  # exclude those with a forbidden extension
  contents <- contents[regexpr(glob2rx(paste("*", exclude_ext, sep=".")),
                               contents) < 0]
  
  # sum the size of the files in the directory
  info <- file.info(file.path(dir, contents))
  size <- sum(info$size)
  if (is.na(size))
    size <- 0
  cur_size <- cur_size + size
  subdir_contents <- NULL

  # if we haven't exceeded the maximum size, check each subdirectory
  if (cur_size < max_size) {
    subdirs <- contents[info$isdir]
    for (subdir in subdirs) {
      if (subdir %in% exclude_dirs)
        next;

      # get the list of files in the subdirectory
      dirList <- .rs.maxDirectoryList(file.path(dir, subdir), 
                                      file.path(root, subdir), 
                                      cur_size, max_size, 
                                      exclude_dirs, exclude_ext)
      cur_size <- cur_size + dirList$size
      subdir_contents <- append(subdir_contents, dirList$contents)

      # abort if we've reached the maximum size
      if (cur_size > max_size)
        break;
    }
  }

  # return the new size and accumulated contents
  list(
    size = size,
    cur_size = cur_size,
    contents = append(file.path(root, contents[!info$isdir]), 
                      subdir_contents))
})

.rs.addFunction("rsconnectDeployList", function(dir) {
  max_size <- 104857600   # 100MB
  dirlist <- .rs.maxDirectoryList(dir, ".", 0, max_size, 
                                  c("rsconnect", "packrat"), "Rproj")
  list (
    # if the directory is too large, no need to bother sending a potentially
    # large blob of data to the client
    dir_list = if (dirlist$cur_size >= max_size)
                  NULL 
               else
                  substr(dirlist$contents, 3, nchar(dirlist$contents)),
    max_size = .rs.scalar(max_size), 
    dir_size = .rs.scalar(dirlist$cur_size))
})

.rs.addJsonRpcHandler("get_deployment_files", function(dir) {
   .rs.rsconnectDeployList(dir)
})

# The parameter to this function is a string containing the R command from
# the rsconnect service; we just need to parse and execute it directly.
# The client is responsible for verifying that the statement corresponds to
# a valid ::setAccountInfo command.
.rs.addJsonRpcHandler("connect_rsconnect_account", function(accountCmd) {
   cmd <- parse(text=accountCmd)
   eval(cmd, envir = globalenv())
})
