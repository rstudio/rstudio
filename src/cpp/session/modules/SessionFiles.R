#
# SessionFiles.R
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
   list.files(path, pattern = pattern, recursive = TRUE)
})

.rs.addJsonRpcHandler("ensure_file_exists", function(path)
{
   if (!file.exists(path))
      if (!file.create(path, recursive = TRUE))
         return(.rs.scalar(FALSE))
   
   .rs.scalar(identical(file.info(path)$isdir, FALSE))
})

.rs.addFunction("scanFiles", function(path,
                                      pattern,
                                      asRelativePath = TRUE,
                                      maxCount = 200L)
{
   .Call("rs_scanFiles",
         as.character(path),
         as.character(pattern),
         as.logical(asRelativePath),
         as.integer(maxCount))
})

.rs.addFunction("readLines", function(filePath)
{
   .Call("rs_readLines", path.expand(filePath))
})
