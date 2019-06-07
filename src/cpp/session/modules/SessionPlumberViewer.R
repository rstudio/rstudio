#
# SessionPlumberViewer.R
#
# Copyright (C) 2009-19 by RStudio, Inc.
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

.rs.addFunction("invokePlumberPaneViewer", function(url) {
   invisible(.Call("rs_plumberviewer", url, getwd(), "pane"))
}, attrs = list(plumberViewerType = "pane"))

.rs.addFunction("invokePlumberWindowViewer", function(url) {
   invisible(.Call("rs_plumberviewer", url, getwd(), "window"))
}, attrs = list(plumberViewerType = "window"))

.rs.addFunction("invokePlumberWindowExternal", function(url) {
   invisible(.Call("rs_plumberviewer", url, getwd(), "browser"))
}, attrs = list(plumberViewerType = "browser"))

.rs.addFunction("setPlumberViewerType", function(type) {
   if (identical(type, "none"))
      options(plumber.swagger.url = NULL)
   else if (identical(type, "pane"))
      options(plumber.swagger.url = .rs.invokePlumberPaneViewer)
   else if (identical(type, "window"))
      options(plumber.swagger.url = .rs.invokePlumberWindowViewer)
   else if (identical(type, "browser"))
      options(plumber.swagger.url = .rs.invokePlumberWindowExternal)
})

.rs.addFunction("getPlumberViewerType", function() {
   viewer <- getOption("plumber.swagger.url")
   if (identical(viewer, FALSE))
      return("none")
   else if (identical(viewer, TRUE))
      return("browser")
   else if (is.function(viewer) && is.numeric(attr(viewer, "plumberViewerType")))
      return(attr(viewer, "plumberViewerType"))
   return(0)
})

.rs.addJsonRpcHandler("get_plumber_viewer_type", function() {
   .rs.scalar(.rs.getPlumberViewerType())
})

