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

   # return an empty list when no entries exist
   if (is.null(frame))
     return(ret)

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
   accounts <- list()
   # safely return an empty list--we want to consider there to be 0 connected
   # accounts when the rsconnect package is not installed or broken 
   # (vs. raising an error)
   tryCatch({
     accounts <- .rs.scalarListFromFrame(rsconnect::accounts())
   }, error = function(e) { })
   accounts
})

.rs.addJsonRpcHandler("remove_rsconnect_account", function(account, server) {
   rsconnect::removeAccount(account, server)
})

.rs.addJsonRpcHandler("get_rsconnect_app_list", function(account, server) {
   .rs.scalarListFromFrame(rsconnect::applications(account, server))
})

.rs.addJsonRpcHandler("get_rsconnect_deployments", function(path) {
   .rs.scalarListFromFrame(rsconnect::deployments(path))
})

.rs.addJsonRpcHandler("validate_server_url", function(url) {
   .rs.scalarListFromList(rsconnect:::validateServerUrl(url))
})

.rs.addJsonRpcHandler("get_auth_token", function(name) {
   .rs.scalarListFromList(rsconnect:::getAuthToken(name))
})

.rs.addJsonRpcHandler("get_user_from_token", function(url, token, privateKey) {
   user <- rsconnect:::getUserFromRawToken(url, token, privateKey)
   .rs.scalarListFromList(user)
})

.rs.addJsonRpcHandler("register_user_token", function(serverName, accountName,
   userId, token, privateKey) {
  rsconnect:::registerUserToken(serverName, accountName, userId, token, 
                                privateKey)
})

.rs.addJsonRpcHandler("get_rsconnect_lint_results", function(target) {
   err <- ""
   results <- NULL
   basePath <- ""

   # validate and lint the requested target
   if (!file.exists(target)) {
     err <- paste("The file or directory ", target, " does not exist.")
   } else {
     tryCatch({
       info <- file.info(target)
       if (info$isdir) {
         # a directory was specified--lint the whole thing
         basePath <- target
         results <- rsconnect::lint(basePath)
       } else {
         # a single file was specified--lint just that file
         basePath <- dirname(target)
         results <- rsconnect::lint(basePath, basename(target))
       }
    }, error = function(e) {
      err <<- e$message
    })
  }

  # empty or missing results; no need to do further work
  if (identical(length(results), 0) || !rsconnect:::hasLint(results)) {
    return(list(
      has_lint = .rs.scalar(FALSE),
      error_message = .rs.scalar(err)))
  }

  # we have a list of lint results; convert them to markers and emit them to
  # the Markers pane
  rsconnect:::showRstudioSourceMarkers(basePath, results)
  
  # return the result to the client
  list(
    has_lint = .rs.scalar(TRUE), 
    error_message = .rs.scalar(err)) 
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

.rs.addFunction("rmdDeployList", function(target) {
  deploy_frame <- rmarkdown::find_external_resources(target) 
  file_list <- c(deploy_frame$path, basename(target))
  list (
    contents = paste("./", file_list, sep = ""),
    cur_size = sum(
       file.info(file.path(dirname(target), file_list))$size))
})

.rs.addFunction("makeDeploymentList", function(target, max_size) {
   if (identical(tolower(tools::file_ext(target)), "rmd")) 
     .rs.rmdDeployList(target)
   else
     .rs.maxDirectoryList(target, ".", 0, max_size, 
                          c("rsconnect", "packrat"), "Rproj")
})

.rs.addFunction("rsconnectDeployList", function(target) {
  max_size <- 104857600   # 100MB
  dirlist <- .rs.makeDeploymentList(target, max_size)
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

.rs.addFunction("enableRStudioConnectUI", function(enable) {
  .rs.enqueClientEvent("enable_rstudio_connect", enable);
  message("RStudio Connect UI ", if (enable) "enabled" else "disabled", ".")
  invisible(enable)
})

.rs.addJsonRpcHandler("get_deployment_files", function(target) {
  .rs.rsconnectDeployList(target)
})

# The parameter to this function is a string containing the R command from
# the rsconnect service; we just need to parse and execute it directly.
# The client is responsible for verifying that the statement corresponds to
# a valid ::setAccountInfo command.
.rs.addJsonRpcHandler("connect_rsconnect_account", function(accountCmd) {
   cmd <- parse(text=accountCmd)
   eval(cmd, envir = globalenv())
})

