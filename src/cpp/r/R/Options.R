#
# Options.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
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

# custom browseURL implementation
options(browser = function(url)
{
   .Call("rs_browseURL", url) ;
})

# implementation of browser.internal option
options(browser.internal = function(url, fullHeight = FALSE)
{
   .Call("rs_browserInternal", url, fullHeight)   
})

# custom pager implementation
options(pager = .rs.pager)

# never allow graphical menus
options(menu.graphics = FALSE)

# set max print so that the DOM won't go haywire showing large datasets
options(max.print = 10000)

# set RStudio as the GUI
local({
   platform = .Platform
   if (platform$GUI != "RStudio") {
      platform$GUI = "RStudio"
      unlockBinding(".Platform", asNamespace("base"))
      assign(".Platform", platform, inherits=TRUE)
      lockBinding(".Platform", asNamespace("base"))
   }
})

# set default x display (see below for comment on why we need to do this)
if (is.na(Sys.getenv("DISPLAY", NA)))
   Sys.setenv(DISPLAY = ":0")

# the above two display oriented command affect the behavior of edit.data.frame
# and edit.matrix as follows: these methods will use .Internal(edit, ...) rather
# than .Internal(dataentry, ...) if DISPLAY == "" or if the .Platform$GUI is
# "unknown". since we plan on running on a server without X available we need
# to manually make sure that the DISPLAY environment variable exists and that
# the .Platform$GUI is not "unknown"












