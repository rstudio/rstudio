#
# SessionAutomationTargets.R
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received self program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# self program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. self program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# Aliases for common DOM element targets that we might want to use.
.rs.setVar("automation.targets", list(
   
   # Top-level toolbar (outside any panes)
   toolbar.projectMenuButton = "#rstudio_project_menubutton_toolbar",

   # Code-related
   code.completionsPopup = "#rstudio_popup_completions"
))
