#
# SessionProjectTemplate.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("getProjectTemplateRegistry", function()
{
   .Call("rs_getProjectTemplateRegistry", PACKAGE = "(embedding)")
})

.rs.addFunction("initializeProjectFromTemplate", function(projectFilePath,
                                                          projectPath,
                                                          description,
                                                          inputs)
{
   # move to parent path for project directory
   parentPath <- dirname(projectPath)
   .rs.ensureDirectory(parentPath)
   owd <- setwd(parentPath)
   on.exit(setwd(owd), add = TRUE)
   
   # detect whether this is a 'local' template; in such a case
   # we'll just attempt to discover the function name in the
   # global environment
   fn <- if (identical(description$package, "(local)"))
      as.name(description$binding)
   else
      call(":::", as.name(description$package), as.name(description$binding))
   
   # construct skeleton call
   skeleton <- c(
      fn,
      basename(projectPath),
      inputs
   )
   
   # evaluate skeleton function in global environment
   eval(as.call(skeleton), envir = .GlobalEnv)
   
   # add first run documents
   .rs.addFirstRunDocumentsForTemplate(
      projectFilePath,
      projectPath,
      description$open_files
   )
   
   TRUE
})

.rs.addFunction("addFirstRunDocumentsForTemplate", function(projectFilePath,
                                                            projectPath,
                                                            openFiles)
{
   # list all files in project directory
   projectFiles <- list.files(projectPath, recursive = TRUE)
   
   # convert from glob to regular expression
   reOpenFiles <- glob2rx(openFiles)
   
   # keep all files that match one of the regular expressions
   files <- Reduce(union, lapply(reOpenFiles, function(regex) {
      grep(regex, projectFiles, value = TRUE)
   }))
   
   # compute scratch path
   scratchPaths <- .Call("rs_computeScratchPaths", projectFilePath, PACKAGE = "(embedding)")
   scratchPath <- scratchPaths$scratch_path
   
   # add these as first-run documents
   .Call("rs_addFirstRunDoc", scratchPath, files, PACKAGE = "(embedding)")
})

