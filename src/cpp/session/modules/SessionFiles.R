#
# SessionFiles.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
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
   list.files(path, pattern=pattern, recursive=T)
})
