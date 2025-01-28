#
# SessionAutomationToolsProjects.R
#
# Copyright (C) 2024 by Posit Software, PBC
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

#' Create a new project
#' 
#' Creates a new project of a specified type, and opens it.
#' 
#' @param type The type of project to create.
#' @return None
#'
.rs.automation.addRemoteFunction("project.create", function(projectName = NULL,
                                                            type = c("default", "package"))
{
   # Resolve the project name and path.
   projectName <- .rs.nullCoalesce(projectName, {
      id <- substring(.rs.createUUID(), 1L, 8L)
      paste("rstudio.automation", id, sep = ".")
   })
   
   projectPath <- file.path(tempdir(), projectName)
   
   # If that path already exists, remove it.
   unlink(projectPath, recursive = TRUE)
   
   # Resolve the project type.
   type <- match.arg(type)
   
   # Initialize the project.
   if (type == "package")
   {
      self$console.executeExpr({
         .rs.rpc.package_skeleton(
            packageName = !!projectName,
            packageDirectory = !!projectPath,
            sourceFiles = character(),
            usingRcpp = FALSE
         )
      })
   }
   else if (type == "default")
   {
      self$console.executeExpr({
         .rs.api.initializeProject(!!projectPath)
      })
   }
   else
   {
      stop("unimplemented or unknown project type '", type, "'")
   }
   
   # Now open the newly-initialized project
   self$console.executeExpr({
      .rs.api.openProject(!!projectPath)
   })
   
   # Wait until the project has been successfully opened
   .rs.waitUntil("The new project is opened", function()
   {
      grepl(projectName, self$project.getLabel(), fixed = TRUE)
   }, swallowErrors = TRUE)
})

#' Close the current project
#' 
#' Closes the currently opened project.
#' 
#' @return None
#'
.rs.automation.addRemoteFunction("project.close", function()
{
   self$dom.clickElement("#rstudio_project_menubutton_toolbar")
   self$dom.clickElement("#rstudio_label_close_project_command")
   
   .rs.waitUntil("The project has closed", function()
   {
      .rs.trimWhitespace(self$project.getLabel()) == "Project: (None)"
   }, swallowErrors = TRUE)
})

#' Get the project dropdown button's label
#' 
#' Get the label from the project dropdown button on the toolbar.
#' 
#' @return The button's label.
#' 
.rs.automation.addRemoteFunction("project.getLabel", function()
{
   self$dom.waitForElement("#rstudio_project_menubutton_toolbar")
   toolbarButton <- self$js.querySelector("#rstudio_project_menubutton_toolbar")
   .rs.trimWhitespace(toolbarButton$innerText)
})
