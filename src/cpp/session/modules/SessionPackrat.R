#
# SessionPackrat.R
#
# Copyright (C) 2020 by RStudio, PBC
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

# attempts to fetch the Packrat status for a project directory as quietly as
# possible -- ignores warnings returns NULL in the case of an error
.rs.addFunction("quietPackratStatus", function(dir) {
   tryCatch({
      suppressWarnings(packrat::status(dir, quiet = TRUE))
   },
   error = function(e) {
      NULL
   })
})

.rs.addFunction ("installPackratActionHook", function() {
  setHook("packrat.onAction", function(project, action, running) {
    .Call("rs_onPackratAction", project, action, running)
  })
})

.rs.addFunction("isPackratModeOn", function(dir) {
   on <- packrat:::isPackratModeOn(dir)
   if (on) {
      identical(.libPaths()[[1]], packrat:::libDir(dir))
   } else {
      FALSE
   }
})

.rs.addFunction("isPackified", function(project) {
   
   # perform Packrat-independent check first, to avoid loading
   # Packrat if un-necessary
   lockPath <- file.path(project, "packrat/packrat.lock")
   if (!file.exists(lockPath))
      return(FALSE)
   
   # otherwise, looks like Packrat, so load Packrat
   packrat:::checkPackified(project = project, quiet = TRUE)
   
})

.rs.addJsonRpcHandler("get_packrat_status", function(dir) {
   .rs.quietPackratStatus(dir)
})

.rs.addFunction("listPackagesPackrat", function(dir) {

   # retrieve the raw list of packages
   libraryList <- .rs.listInstalledPackages()

   # try to get the status from packrat
   packratStatus <- .rs.quietPackratStatus(dir)

   # if we weren't able to get a data frame from packrat::status, just return 
   # the unannotated library list
   if (!is.data.frame(packratStatus))
      return(libraryList)

   # for each package, indicate whether it's in the private library (this is
   # largely a convenience for the client since a lot of behavior is driven
   # from this value). do this before resolving symlinks since some packages
   # may be symlinked out of the Packrat private library to a global cache.

   # NOTE: On Windows with R >= 3.1.0, 'file.path' strips the terminating
   # filepath delimiter. In addition, normalizePath uses '\' for slashes,
   # while 'file.path' uses '/'. Since we are comparing paths as strings,
   # we need to ensure that we use the same path delimiter here. So we use
   # 'paste' explicitly so that we can ensure the correct delimiter.
   # note that .Platform$file.sep is still '/' even on Windows.
   sep <- if (tolower(.Platform$OS.type) == "windows") "\\" else "/"

   projectPath <- paste(normalizePath(dir), "packrat", "lib", "", sep = sep)
   libraryPaths <- normalizePath(as.character(libraryList[,"library"]))
   libraryList["in.project.library"] <- 
      substr(libraryPaths, 1, nchar(projectPath)) == projectPath

   # resolve symlinks (use normalizePath rather than Sys.readlink since we want
   # to resolve symlinks anywhere in the heirarchy)
   resolvedLinks <- normalizePath(by(libraryList, 1:nrow(libraryList),
                                     function(pkg) {
                                        system.file(package = pkg$name, lib.loc = pkg$library)
                                     }))

   # for any packages in lib-R, these might be junction points on Windows;
   # hard-code the path back to the system library. unfortunately R provides
   # no mechanism for asking whether the file at a path is a junction point,
   # nor does normalizePath follow junction points. for robustness, we should
   # explicitly check whether these files are junction points
   if (tolower(.Platform$OS.type) == "windows") {
      .Library <- packrat:::.packrat_mutables$get(".Library")
      if (!is.null(.Library)) {
         idx <- grep("\\lib-R\\", resolvedLinks, fixed = TRUE)
         resolvedLinks[idx] <-
            normalizePath(file.path(.Library, basename(resolvedLinks[idx])))
      }
   }

   libraryList[,"source.library"] <- 
      .rs.createAliasedPath(libraryList[,"library"])
   libraryList[,"library"] <- 
      .rs.createAliasedPath(dirname(resolvedLinks))

   packratList <- subset(libraryList, in.project.library) 
   nonPackratList <- subset(libraryList, !in.project.library) 

   # overlay packrat status on the packrat library status
   mergedList <- merge(packratList, 
                       packratStatus, 
                       by.x = "name", 
                       by.y = "package", 
                       all.x = TRUE,
                       all.y = TRUE)

   # mark all packages in the merged list 
   mergedList[,"in.project.library"] <- rep(TRUE, nrow(mergedList))

   # exclude manipulate and rstudio packages 
   mergedList <- subset(mergedList, !(mergedList[,"name"] == "rstudio"))
   mergedList <- subset(mergedList, !(mergedList[,"name"] == "manipulate"))

   if (nrow(nonPackratList) > 0) {
      # if there are non-Packrat packages, create empty packrat columns for
      # the packages 
      packratCols <- setdiff(colnames(mergedList), colnames(nonPackratList))
      nonPackratList[,packratCols] <- NA

      # return the combined list
      rbind(mergedList, nonPackratList)
   } else {
      # only Packrat packages, just return the merged list
      mergedList
   }
})

.rs.addFunction("getAutoSnapshotCmd", function(dir) {
   paste(packrat:::buildSnapshotHookCall(dir), collapse = "; ")
})

.rs.addFunction("pendingActions", function(action, dir) {
   capture.output(msgs <- tryCatch({
        suppressWarnings(packrat:::getActionMessages(action, dir))
      }, 
      error = function(e) {
         NULL
      }))
   # Transform NAs into explicit missing text
   if (!is.null(msgs)) {
      for (i in seq_along(msgs)) {
         msgs[[i]][ is.na(msgs[[i]]) ] <- "<missing>"
      }
   }
   msgs
})

.rs.addJsonRpcHandler("get_pending_actions", function(action, dir) {
   .rs.pendingActions(action, dir)
})
