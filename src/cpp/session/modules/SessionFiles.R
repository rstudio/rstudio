#
# SessionFiles.R
#
# Copyright (C) 2022 by RStudio, PBC
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

.rs.addFunction("listFilesImplOne", function(path, full.names, callback)
{
   # R may fail to invoke list.files() on a path that cannot be represented in
   # the native encoding, so just move to that directory, list the available
   # files, then return that list
   owd <- tryCatch(setwd(path), condition = identity)
   if (inherits(owd, "error"))
      return(character())
   
   # return home when we're done
   on.exit(setwd(owd), add = TRUE)
   
   # list the files
   files <- callback()
   
   # mark encoding if we're UTF-8
   if (.rs.platform.isWindows)
   {
      acp <- .Call("rs_getACP", PACKAGE = "(embedding)")
      if (identical(acp, 65001L))
      {
         Encoding(files) <- "UTF-8"
         path <- enc2utf8(path)
      }
   }
   
   # if full.names was set, we'll need to prepend the original path
   # to the created file names
   if (full.names)
      files <- paste(enc2utf8(path), files, sep = "/")
   
   # return file paths
   files
   
})

.rs.addFunction("listFilesImpl", function(path = ".",
                                          full.names = FALSE,
                                          callback)
{
   data <- vector("list", length(path))
   
   for (i in seq_along(data))
   {
      data[[i]] <- .rs.listFilesImplOne(path, full.names, callback)
   }
   
   all <- unlist(data, recursive = TRUE, use.names = FALSE)
   .rs.enc2native(all)
})

.rs.addFunction("listFiles", function(path = ".",
                                      pattern = NULL,
                                      all.files = FALSE,
                                      full.names = FALSE,
                                      recursive = FALSE,
                                      ignore.case = FALSE,
                                      include.dirs = FALSE,
                                      no.. = FALSE)
{
   .rs.listFilesImpl(path, full.names, function() {
      .Internal(
         list.files(
            ".",
            pattern,
            all.files,
            FALSE,
            recursive,
            ignore.case,
            include.dirs,
            no..
         )
      )
   })
})

.rs.addFunction("listDirs", function(path = ".",
                                     full.names = TRUE,
                                     recursive = TRUE)
{
   .rs.listFilesImpl(path, full.names, function() {
      .Internal(list.dirs(".", FALSE, recursive))
   })
})

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
