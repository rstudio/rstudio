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

.rs.addJsonRpcHandler("get_packrat_status", function(dir) {
   packrat::status(dir, quiet = TRUE)
})

.rs.addJsonRpcHandler("get_packrat_restore_actions", function(dir) {
   msgs <- packrat:::getRestoreActionMessages(dir)
   # Transform NAs into explicit missing text
   for (i in seq_along(msgs)) {
      msgs[[i]][ is.na(msgs[[i]]) ] <- "<missing>"
   }
   msgs
})

.rs.addFunction("listPackagesPackrat", function(dir) {
   # get the status from the library and packrat
   packratStatus <- packrat::status(dir, quiet = TRUE)
   libraryList <- .rs.listInstalledPackages()

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

   # create empty packrat columns for the packages in non-Packrat libraries
   packratCols <- setdiff(colnames(mergedList), colnames(nonPackratList))
   nonPackratList[,packratCols] <- NA

   # return the combined list
   rbind(mergedList, nonPackratList)
})

.rs.addJsonRpcHandler("list_packages_packrat", function(dir) {
   .rs.listPackagesPackrat(dir)
})

