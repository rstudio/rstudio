#
# SessionPlumberViewer.R
#
# Copyright (C) 2020 by RStudio, PBC
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
   invisible(.Call("rs_plumberviewer", url, getwd(), "pane", PACKAGE = "(embedding)"))
}, attrs = list(plumberViewerType = "pane"), envir = baseenv())

.rs.addFunction("invokePlumberWindowViewer", function(url) {
   invisible(.Call("rs_plumberviewer", url, getwd(), "window", PACKAGE = "(embedding)"))
}, attrs = list(plumberViewerType = "window"), envir = baseenv())

.rs.addFunction("invokePlumberWindowExternal", function(url) {
   invisible(.Call("rs_plumberviewer", url, getwd(), "browser", PACKAGE = "(embedding)"))
}, attrs = list(plumberViewerType = "browser"), envir = baseenv())

.rs.addFunction("setPlumberViewerType", function(type) {
   viewer <-
      if (identical(type, "none"))
          NULL
      else if (identical(type, "pane"))
          .rs.invokePlumberPaneViewer
      else if (identical(type, "window"))
          .rs.invokePlumberWindowViewer
      else if (identical(type, "browser"))
          .rs.invokePlumberWindowExternal
  options(
    # plumber >= v1.0.0
    plumber.docs.callback = viewer,
    # plumber < v1.0.0
    plumber.swagger.url = viewer
  )
})

.rs.addFunction("getPlumberViewerType", function() {
   viewer <- getOption("plumber.docs.callback", getOption("plumber.swagger.url"))
   if (identical(viewer, FALSE))
      return("none")
   else if (identical(viewer, TRUE))
      return("browser")
   else if (is.function(viewer) && is.character(attr(viewer, "plumberViewerType")))
      return(attr(viewer, "plumberViewerType"))
   return("user")
})

.rs.addJsonRpcHandler("get_plumber_viewer_type", function() {
   .rs.scalar(.rs.getPlumberViewerType())
})

