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

.rs.addFunction("listPackagesPackrat", function(dir) {
   # get the status from the library and packrat
   packratStatus <- packrat::status(dir, quiet = TRUE)
   libraryList <- .rs.listInstalledPackages()

   # overlay packrat status on library status 
   mergedList <- merge(libraryList, 
                       packratStatus, 
                       by.x = "name", 
                       by.y = "package", 
                       all.x = TRUE,
                       all.y = TRUE)

   # for each package, indicate whether it's in the private library
   # (this is largely a convenience for the client since a lot of behavior
   # is driven from this value)
   projectPath <- normalizePath(dir) 
   libraryPaths <- normalizePath(as.character(mergedList[,"library"]))
   mergedList["in.packrat.library"] <- 
      substr(libraryPaths, 1, nchar(projectPath)) ==
      projectPath

   # exclude manipulate and rstudio packages 
   mergedList <- subset(mergedList, !(mergedList[,"name"] == "rstudio"))
   mergedList <- subset(mergedList, !(mergedList[,"name"] == "manipulate"))

   # return the combined list
   mergedList
})

.rs.addJsonRpcHandler("list_packages_packrat", function(dir) {
   .rs.listPackagesPackrat(dir)
})

