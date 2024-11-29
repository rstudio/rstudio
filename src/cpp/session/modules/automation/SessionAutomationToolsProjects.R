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
#' Creates a new project of a specified type. The console is used
#' to issue commands so must be available.
#' 
#' @param type The type of project to create, currently only "package" is supported.
#' @return None
#'
.rs.automation.addRemoteFunction("projectCreate", function(type = "")
{
   # Create a package project.
   if (type == "package")
   {
      self$consoleExecuteExpr({
         projectPath <- tempfile("rstudio", tmpdir = normalizePath(dirname(tempdir())))
         usethis::create_package(path = projectPath, open = FALSE)
         .rs.api.openProject(projectPath)
      }, wait = FALSE)
   }
   else
   {
      stop("unimplemented or unknown project type '", type, "'")
   }
   
   # Wait until the new project is open.
   self$waitForProjectToOpen("rstudio")
})

#' Wait for the project to open
#' 
#' Waits until the specified project is opened.
#' 
#' @param projectName The name of the project to wait for.
#' @return TRUE if the project is opened, FALSE otherwise.
#'
.rs.automation.addRemoteFunction("waitForProjectToOpen", function(projectName)
{
   .rs.waitUntil("The new project is opened", function()
   {
      grepl(projectName, self$getProjectDropdownLabel())
   }, swallowErrors = TRUE)
})

#' Close the current project
#' 
#' Closes the currently opened project.
#' 
#' @return None
#'
.rs.automation.addRemoteFunction("projectClose", function()
{
   self$domClickElement(.rs.automation.targets[["toolbar.projectMenuButton"]])
   self$domClickElement("#rstudio_label_close_project_command")
   
   .rs.waitUntil("The project has closed", function()
   {
      .rs.trimWhitespace(self$getProjectDropdownLabel()) == "Project: (None)"
   }, swallowErrors = TRUE)
})

#' Get the project dropdown button's label
#' 
#' Get the label from the project dropdown button on the toolbar.
#' 
#' @return The button's label.
#' 
.rs.automation.addRemoteFunction("getProjectDropdownLabel", function()
{
   self$waitForElement(.rs.automation.targets[["toolbar.projectMenuButton"]])
   toolbarButton <- self$jsObjectViaSelector(.rs.automation.targets[["toolbar.projectMenuButton"]])
   .rs.trimWhitespace(toolbarButton$innerText)
})
