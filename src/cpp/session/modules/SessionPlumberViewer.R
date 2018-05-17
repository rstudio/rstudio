#
# SessionPlumberViewer.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
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
   invisible(.Call("rs_plumberviewer", url, getwd(), 2))
}, attrs = list(plumberViewerType = 2))

.rs.addFunction("invokePlumberWindowViewer", function(url) {
   invisible(.Call("rs_plumberviewer", url, getwd(), 3))
}, attrs = list(plumberViewerType = 3))

.rs.addFunction("invokePlumberWindowExternal", function(url) {
   invisible(.Call("rs_plumberviewer", url, getwd(), 4))
}, attrs = list(plumberViewerType = 4))

.rs.addFunction("setPlumberViewerType", function(type) {
   if (type == 1)
      options(plumber.swagger.url = NULL)
   else if (type == 2)
      options(plumber.swagger.url = .rs.invokePlumberPaneViewer)
   else if (type == 3)
      options(plumber.swagger.url = .rs.invokePlumberWindowViewer)
   else if (type == 4)
      options(plumber.swagger.url = .rs.invokePlumberWindowExternal)
})

.rs.addFunction("getPlumberViewerType", function() {
   viewer <- getOption("plumber.swagger.url")
   if (identical(viewer, FALSE))
      return(1)
   else if (identical(viewer, TRUE))
      return(4)
   else if (is.function(viewer) && is.numeric(attr(viewer, "plumberViewerType")))
      return(attr(viewer, "plumberViewerType"))
   return(0)
})

.rs.addJsonRpcHandler("get_plumber_viewer_type", function() {
   list(viewerType = .rs.scalar(.rs.getPlumberViewerType()))
})

