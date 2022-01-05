#
# SessionPlots.R
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

# register hook to be invoked before creating a new plot
setHook(
  hookName = "before.plot.new", 
  value = function() { 
    .Call("rs_emitBeforeNewPlot", PACKAGE = "(embedding)")
  },
  action = "append")

setHook(
  hookName = "before.grid.newpage", 
  value = function() { 
    .Call("rs_emitBeforeNewGridPage", PACKAGE = "(embedding)")
  },
  action = "append")

# register hook to be invoked when a new plot has been created
setHook(
  hookName = "plot.new", 
  value = function() { 
    .Call("rs_emitNewPlot", PACKAGE = "(embedding)")
  },
  action = "append")


