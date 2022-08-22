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

.rs.setVar("files.savedBindings", new.env(parent = emptyenv()))

# save old implementations, in case we need to restore them
bindings <- c("list.files", "list.dirs", "dir")
for (binding in bindings)
{
   original <- get(binding, envir = baseenv(), inherits = FALSE)
   assign(binding, original, envir = .rs.files.savedBindings)
}

# these hooks are added to support RStudio's transition into
# the use of a UTF-8 code page
.rs.addFunction("files.replaceBindings", function()
{
   .rs.replaceBinding("list.files", "base", function(path = ".",
                                                     pattern = NULL,
                                                     all.files = FALSE,
                                                     full.names = FALSE,
                                                     recursive = FALSE,
                                                     ignore.case = FALSE,
                                                     include.dirs = FALSE,
                                                     no.. = FALSE)
   {
      "RStudio hook: restore original with `.rs.files.restoreBindings()`"
      .Call(
         "rs_listFiles",
         path,
         pattern,
         all.files,
         full.names,
         recursive,
         ignore.case,
         include.dirs,
         no..,
         PACKAGE = "(embedding)"
      )
   })

   .rs.replaceBinding("list.dirs", "base", function(path = ".",
                                                    full.names = TRUE,
                                                    recursive = TRUE)
   {
      "RStudio hook: restore original with `.rs.files.restoreBindings()`"
      .Call(
         "rs_listDirs",
         path,
         full.names,
         recursive,
         PACKAGE = "(embedding)"
      )
   })

   # dir and list.files should refer to the same thing
   .rs.replaceBinding("dir", "base", list.files)

})

# this is provided just in case users need an escape hatch
# from our overridden list.files() implementations on windows
.rs.addFunction("files.restoreBindings", function()
{
   bindings <- ls(envir = .rs.files.savedBindings)
   for (binding in bindings)
      .rs.replaceBinding(binding, "base", .rs.files.savedBindings[[binding]])
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
   .Call(
      "rs_listFiles",
      path,
      pattern,
      all.files,
      full.names,
      recursive,
      ignore.case,
      include.dirs,
      no..,
      PACKAGE = "(embedding)"
   )
})

.rs.addFunction("listDirs", function(path = ".",
                                     full.names = TRUE,
                                     recursive = TRUE)
{
   .Call(
      "rs_listDirs",
      path,
      full.names,
      recursive,
      PACKAGE = "(embedding)"
   )
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

.rs.addJsonRpcHandler("create_aliased_path", function(path)
{
   if (file.exists(path)) {
      return(.rs.scalar(.rs.createAliasedPath(path)))
   } else {
      return(.rs.scalar(""))
   }
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
