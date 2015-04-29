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

.rs.addFunction("getRSConnectDeployments", function(path, rpubsUploadId) {
   # start with an empty list
   deploymentsFrame <- data.frame(
     name = character(0),
     account = character(0),
     server = character(0),
     bundleId = character(0),
     asStatic = logical(0),
     when = numeric(0))
   deployments <- list()
     
   # attempt to populate the list from rsconnect; this can throw if e.g. the
   # package is not installed. in the case of any error we'll safely return 
   # an empty list, or a stored RPubs upload ID if one was given (below)
   tryCatch({
     deploymentsFrame <- rsconnect::deployments(path)
     deployments <- .rs.scalarListFromFrame(deploymentsFrame)
   }, error = function(e) { })

   # no RPubs upload IDs to consider
   if (!is.character(rpubsUploadId) || nchar(rpubsUploadId) == 0) {
     return(deployments)
   }

   # if there's already a deployment to rpubs.com, ignore legacy deployment
   if ("rpubs.com" %in% deployments$server) {
     return(deployments)
   }

   # create a new list with the same names as the one we're about to return,
   # and populate the fields we know from RPubs. leave all others, including
   # user-defined fields, blank; this allows us to tolerate changes to the
   # deployment frame format.
   rpubsDeployment <- list()
   for (col in colnames(deploymentsFrame)) {
     if (col == "name")
       rpubsDeployment[col] = ""
     else if (col == "account")
       rpubsDeployment[col] = "rpubs"
     else if (col == "server")
       rpubsDeployment[col] = "rpubs.com"
     else if (col == "bundleId") 
       rpubsDeployment[col] = rpubsUploadId
     else if (col == "asStatic")
       rpubsDeployment[col] = TRUE
     else if (col == "when") 
       rpubsDeployment[col] = 0
     else 
       rpubsDeployment[col] = NA
   }

   # combine the deployments rsconnect knows about with the deployments we know
   # about
   c(deployments, list(.rs.scalarListFromList(rpubsDeployment)))
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

.rs.addFunction("docDeployList", function(target, asMultipleDoc) {
  file_list <- c()

  # if deploying multiple documents, find all the files in the with a matching
  # extension; otherwise, just use the single document we were given
  if (asMultipleDoc) {
    targets <- list.files(path = dirname(target), 
      pattern = glob2rx(paste("*", tools::file_ext(target), sep = ".")), 
      ignore.case = TRUE, full.names = TRUE)
  } else {
    targets <- target
  }

  # find the resources used by each document
  for (t in targets) {
    deploy_frame <- NULL
    tryCatch({
      # this operation can be expensive and could also throw if e.g. the 
      # document fails to parse or render
      deploy_frame <- rmarkdown::find_external_resources(t) 
    },
    error = function(e) {
      # errors are not fatal here; we just might miss some resources, which
      # the user will have to add manually
    })
    if (!is.null(deploy_frame)) {
      file_list <- c(file_list, deploy_frame$path, basename(t))
    }
  }

  # discard any duplicates (the same resource may be depended upon by multiple
  # R Markdown documents)
  file_list <- unique(file_list)

  # compose the result
  list (
    contents = paste("./", file_list, sep = ""),
    cur_size = sum(
       file.info(file.path(dirname(target), file_list))$size))
})

.rs.addFunction("makeDeploymentList", function(target, asMultipleDoc, 
                                               max_size) {
   ext <- tolower(tools::file_ext(target))
   if (ext %in% c("rmd", "html", "htm", "md"))
     .rs.docDeployList(target, asMultipleDoc)
   else
     .rs.maxDirectoryList(target, ".", 0, max_size, 
                          c("rsconnect", "packrat"), "Rproj")
})

.rs.addFunction("rsconnectDeployList", function(target, asMultipleDoc) {
  max_size <- 104857600   # 100MB
  dirlist <- .rs.makeDeploymentList(target, asMultipleDoc, max_size)
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

.rs.addJsonRpcHandler("get_deployment_files", function(target, asMultipleDoc) {
  .rs.rsconnectDeployList(target, asMultipleDoc)
})

# The parameter to this function is a string containing the R command from
# the rsconnect service; we just need to parse and execute it directly.
# The client is responsible for verifying that the statement corresponds to
# a valid ::setAccountInfo command.
.rs.addJsonRpcHandler("connect_rsconnect_account", function(accountCmd) {
   cmd <- parse(text=accountCmd)
   eval(cmd, envir = globalenv())
})


.rs.addJsonRpcHandler("get_rmd_publish_details", function(target) {
  # check for multiple R Markdown documents in the directory 
  rmds <- list.files(path = dirname(target), pattern = glob2rx("*.Rmd"),
                     all.files = FALSE, recursive = FALSE, ignore.case = TRUE,
                     include.dirs = FALSE)

  # see if this format is self-contained (defaults to true for HTML-based 
  # formats)
  selfContained <- TRUE
  lines <- readLines(target, warn = FALSE)
  outputFormat <- rmarkdown:::output_format_from_yaml_front_matter(lines)
  if (is.list(outputFormat$options) &&
      identical(outputFormat$options$self_contained, FALSE)) {
    selfContained <- FALSE
  }

  # extract the document's title
  title <- ""
  frontMatter <- rmarkdown:::parse_yaml_front_matter(lines) 
  if (is.list(frontMatter) && is.character(frontMatter$title)) {
    title <- frontMatter$title
  }

  # check to see if this is an interactive doc (i.e. needs to be run rather
  # rather than rendered)
  renderFunction <- .rs.getCustomRenderFunction(target)
  list(
    is_multi_rmd      = .rs.scalar(length(rmds) > 1), 
    is_shiny_rmd      = .rs.scalar(renderFunction == "rmarkdown::run"),
    is_self_contained = .rs.scalar(selfContained),
    title             = .rs.scalar(title))
})
