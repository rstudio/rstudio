#
# SessionShinyViewer.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

.rs.addFunction("invokeShinyTutorialViewer", function(url, meta) {
   invisible(.Call("rs_shinyviewer", url, getwd(), "tutorial", meta, PACKAGE = "(embedding)"))
}, attrs = list(shinyViewerType = "tutorial"), envir = baseenv())

.rs.addFunction("invokeShinyPaneViewer", function(url) {
   invisible(.Call("rs_shinyviewer", url, getwd(), "pane", NULL, PACKAGE = "(embedding)"))
}, attrs = list(shinyViewerType = "pane"), envir = baseenv())

.rs.addFunction("invokeShinyWindowViewer", function(url) {
   invisible(.Call("rs_shinyviewer", url, getwd(), "window", NULL, PACKAGE = "(embedding)"))
}, attrs = list(shinyViewerType = "window"), envir = baseenv())

.rs.addFunction("invokeShinyWindowExternal", function(url) {
   invisible(.Call("rs_shinyviewer", url, getwd(), "browser", NULL, PACKAGE = "(embedding)"))
}, attrs = list(shinyViewerType = "browser"), envir = baseenv())

.rs.addFunction("setShinyViewerType", function(type) {
   if (identical(type, "none"))
      options(shiny.launch.browser = FALSE)
   else if (identical(type, "tutorial"))
      options(shiny.launch.browser = .rs.invokeShinyTutorialViewer)
   else if (identical(type, "pane"))
      options(shiny.launch.browser = .rs.invokeShinyPaneViewer)
   else if (identical(type, "window"))
      options(shiny.launch.browser = .rs.invokeShinyWindowViewer)
   else if (identical(type, "browser"))
      options(shiny.launch.browser = .rs.invokeShinyWindowExternal)
})

.rs.addFunction("getShinyViewerType", function() {
   viewer <- getOption("shiny.launch.browser")
   if (identical(viewer, FALSE))
      return("none")
   else if (identical(viewer, TRUE))
      return("browser")
   else if (is.function(viewer) && is.character(attr(viewer, "shinyViewerType")))
      return(attr(viewer, "shinyViewerType"))
   return("user")
})

.rs.addFunction("refreshShinyLaunchBrowserOption", function()
{
   # read viewer option
   viewer <- getOption("shiny.launch.browser")
   if (!is.function(viewer))
      return(FALSE)
   
   type <- attr(viewer, "shinyViewerType", exact = TRUE)
   if (is.null(type))
      return(FALSE)
   
   # NOTE: in RStudio v1.2, Shiny viewer types were stored as numeric values;
   # now, they're stored as explicit names. we handle both cases here; the
   # indices of the following 'types' vector matches the types used for v1.2
   if (is.numeric(type) && length(type) == 1L)
   {
      types <- c("none", "pane", "window", "browser")
      type <- types[[type]]
   }
   
   # validate that we now have a character type
   if (!is.character(type) || length(type) != 1L)
      return(FALSE)
   
   # set the viewer type
   .rs.setShinyViewerType(type)
   TRUE
})

.rs.addJsonRpcHandler("get_shiny_viewer_type", function() {
   .rs.scalar(.rs.getShinyViewerType())
})

.rs.addJsonRpcHandler("stop_shiny_app", function(id)
{
   # If the app is running in the foreground, tell Shiny to stop it directly
   if (identical(id, "foreground"))
   {
      # Suspend interrupts while stopping the application if possible.
      suspendInterrupts <- if (exists("suspendInterrupts", envir = .BaseNamespaceEnv))
         base::suspendInterrupts
      else
         identity
      
      suspendInterrupts(
         withCallingHandlers(
            shiny::stopApp(),
            interrupt = function(cnd) {
               restart <- findRestart("resume")
               if (isRestart(restart))
                  invokeRestart(restart)
            }
         )
      )
   }
   else
   {
      # it's running in the background; stop the associated job
      .rs.api.stopJob(id)
   }
})
