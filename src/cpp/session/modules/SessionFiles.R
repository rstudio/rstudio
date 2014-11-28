#
# SessionFiles.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

.rs.addFunction("listZipFile", function(zipfile)
{
   as.character(utils::unzip(zipfile, list=TRUE)$Name)
})


.rs.addFunction("createZipFile", function(zipfile, parent, files)
{
   # set working dir to parent (and restore on exit)
   previous_wd = getwd()
   setwd(parent)
   on.exit(setwd(previous_wd))
   
   # build zip command
   quotedFiles = paste(shQuote(files), collapse = " ")
   createZipCommand = paste("zip", "-r", shQuote(zipfile), quotedFiles, "2>&1")  
   
   # execute the command (return output of command)
   system(createZipCommand, intern = TRUE)
})

.rs.addJsonRpcHandler("list_all_files", function(path, pattern) {
   if ((ag <- Sys.which("ag")) != "")
   {
      if (!identical(path, getwd()))
      {
         owd <- getwd()
         setwd(path)
         on.exit(setwd(owd))
      }
      
      if (pattern == "")
         pattern <- "."
         
      command <- paste(
         shQuote(ag),
         "--nocolor",
         "-g",
         pattern
      )
      
      files <- suppressWarnings(system(command, intern = TRUE))
      attributes(files) <- NULL # drop status attr
      return(files)
   }
   
   if (nzchar(pattern))
      list.files(path, pattern = pattern, recursive = TRUE)
   else
      list.files(path, recursive = TRUE)
})
