#
# SessionPackrat.R
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

.rs.addJsonRpcHandler("get_packrat_status", function(dir) {
   packrat::status(dir, quiet = TRUE)
})

.rs.addFunction("listPackagesPackrat", function(dir) {
   # retrieve the raw list of packages
   libraryList <- .rs.listInstalledPackages()

   # try to get the status from packrat
   packratStatus <- tryCatch({
     packrat::status(dir, quiet = TRUE)
   }, 
   error = function(e) {
      NULL
   })

   # if we weren't able to get a data frame from packrat::status, just return 
   # the unannotated library list
   if (!is.data.frame(packratStatus))
      return(libraryList)

   # resolve symlinks (use normalizePath rather than Sys.readlink since we want
   # to resolve symlinks anywhere in the heirarchy)
   resolvedLinks <- normalizePath(by(libraryList, 1:nrow(libraryList),
         function(pkg) { 
                system.file(package = pkg$name, lib.loc = pkg$library) 
         }))

   # for links that got resolved, replace the library indicated with the 
   # actual (resolved) parent folder of the library
   symlinks <- nchar(resolvedLinks) > 3
   libraryList[symlinks,"library"] <- 
      .rs.createAliasedPath(dirname(resolvedLinks[symlinks]))

   # for each package, indicate whether it's in the private library (this is
   # largely a convenience for the client since a lot of behavior is driven
   # from this value)
   projectPath <- normalizePath(dir) 
   libraryPaths <- normalizePath(as.character(libraryList[,"library"]))
   libraryList["in.packrat.library"] <- 
      substr(libraryPaths, 1, nchar(projectPath)) ==
      projectPath

   packratList <- subset(libraryList, in.packrat.library) 
   nonPackratList <- subset(libraryList, !in.packrat.library) 

   # overlay packrat status on the packrat library status
   mergedList <- merge(packratList, 
                       packratStatus, 
                       by.x = "name", 
                       by.y = "package", 
                       all.x = TRUE,
                       all.y = TRUE)

   # mark all packages in the merged list 
   mergedList[,"in.packrat.library"] <- rep(TRUE, nrow(mergedList))

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
   capture.output(msgs <- packrat:::getActionMessages(action, dir))
   # Transform NAs into explicit missing text
   for (i in seq_along(msgs)) {
      msgs[[i]][ is.na(msgs[[i]]) ] <- "<missing>"
   }
   msgs
})

.rs.addJsonRpcHandler("get_pending_actions", function(action, dir) {
   .rs.pendingActions(action, dir)
})

