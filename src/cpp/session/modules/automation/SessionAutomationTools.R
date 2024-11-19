#
# SessionAutomationTools.R
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

.rs.addFunction("automation.tools.isAriaHidden", function(jsObject)
{
   isAriaHidden <- .rs.tryCatch(jsObject$ariaHidden)
   # no aria-hidden attribute is equivalent to FALSE
   ifelse(inherits(isAriaHidden, "error"), FALSE, as.logical(isAriaHidden))
})

.rs.addFunction("automation.tools.createTempFolder", function(remote)
{
   path <- tempfile("rstudio.automation.", tmpdir = dirname(tempdir()))
   remote$consoleExecute(sprintf("dir.create('%s', recursive = TRUE, showWarnings = FALSE)", path))
   path
})

.rs.addFunction("automation.tools.deleteFolder", function(remote, folder)
{
   remote$consoleExecute(sprintf("unlink('%s', recursive = TRUE)", folder))
})

.rs.addFunction("automation.tools.waitForProjectToOpen", function(remote, projectName)
{
   Sys.sleep(1)
   .rs.waitUntil("The new project is opened", function()
   {
      tryCatch({
         grepl(projectName, .rs.automation.tools.getProjectDropdownLabel(remote))
      }, error = function(e) FALSE)
   })
})

.rs.addFunction("automation.tools.getProjectDropdownLabel", function(remote)
{
   toolbarButton <- remote$jsObjectViaSelector("#rstudio_project_menubutton_toolbar")
   .rs.trimWhitespace(toolbarButton$innerText)
})
