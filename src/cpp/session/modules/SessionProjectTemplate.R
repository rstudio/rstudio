#
# SessionProjectTemplate.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
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

.rs.addFunction("getProjectTemplateRegistry", function()
{
   .Call("rs_getProjectTemplateRegistry")
})

.rs.addFunction("addFirstRunDocumentsForTemplate", function(projectFilePath,
                                                            projectPath,
                                                            openFiles)
{
   # list all files in project directory
   projectFiles <- list.files(projectPath, recursive = TRUE)
   
   # expand environment variables within 'openFiles' specification
   openFiles <- .rs.expandEnvironmentVariables(openFiles)
   
   # convert from glob to regular expression
   reOpenFiles <- glob2rx(openFiles)
   
   # keep all files that match one of the regular expressions
   files <- Reduce(union, lapply(reOpenFiles, function(regex) {
      grep(regex, projectFiles, value = TRUE)
   }))
   
   # add these as first-run documents
   .Call("rs_addFirstRunDoc", projectFilePath, files)
})

.rs.addFunction("expandEnvironmentVariables", function(fields)
{
   transformed <- fields
   .rs.enumerate(Sys.getenv(), function(key, val) {
      
      patterns <- c(
         paste("\\$", key, "\\b", sep = ""),       # $FOO
         paste("\\$\\{", key, "\\}\\b", sep = ""), # ${FOO}
         paste("%", key, "%\\b", sep = "")         # %FOO%
      )
      
      for (pattern in patterns)
         transformed <<- gsub(pattern, val, transformed, ignore.case = TRUE)
   })
   transformed
})
