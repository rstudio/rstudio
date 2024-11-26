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

.rs.automation.addRemoteFunction("waitForProjectToOpen", function(projectName)
{
   Sys.sleep(3)
   .rs.waitUntil("The new project is opened", function()
   {
      grepl(projectName, self$getProjectDropdownLabel())
   }, swallowErrors = TRUE)
})

.rs.automation.addRemoteFunction("projectClose", function()
{
   self$domClickElement(.rs.automation.targets[["toolbar.projectMenuButton"]])
   self$domClickElement("#rstudio_label_close_project_command")
   
   .rs.waitUntil("The project has closed", function()
   {
      .rs.trimWhitespace(self$getProjectDropdownLabel()) == "Project: (None)"
   }, swallowErrors = TRUE)
})

.rs.automation.addRemoteFunction("getProjectDropdownLabel", function()
{
   self$waitForElement(.rs.automation.targets[["toolbar.projectMenuButton"]])
   toolbarButton <- self$jsObjectViaSelector(.rs.automation.targets[["toolbar.projectMenuButton"]])
   .rs.trimWhitespace(toolbarButton$innerText)
})
