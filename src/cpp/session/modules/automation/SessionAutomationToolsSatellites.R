#
# SessionAutomationToolsSatellites.R
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

# !diagnostics suppress=self

#' List satellite windows
#'
#' Returns a list of satellite window targets by querying CDP for all
#' targets and filtering out the main RStudio window.
#'
#' @return A list of satellite target info objects.
.rs.automation.addRemoteFunction("satellites.list", function()
{
   # Target.getTargets() is a browser-level command, so we need to
   # temporarily clear the sessionId to route it to the browser context.
   savedSessionId <- .rs.automation.sessionId
   .rs.setVar("automation.sessionId", NULL)
   on.exit(.rs.setVar("automation.sessionId", savedSessionId), add = TRUE)

   targets <- self$client$Target.getTargets()
   mainTargetId <- .rs.automation.targetId

   # Filter to page targets that are not the main window.
   satellites <- Filter(function(target) {
      target$type == "page" && target$targetId != mainTargetId
   }, targets$targetInfos)

   satellites
})

#' Check if a satellite window is open
#'
#' @param name The title of the satellite window to check for.
#' @return TRUE if the satellite is open, FALSE otherwise.
.rs.automation.addRemoteFunction("satellites.isOpen", function(name)
{
   satellites <- self$satellites.list()
   any(vapply(satellites, function(target) {
      identical(target$title, name)
   }, FUN.VALUE = logical(1)))
})

#' Wait for a satellite window to open
#'
#' Polls until a satellite window with the given title appears.
#'
#' @param name The title of the satellite window to wait for.
#' @param timeout Maximum time to wait, in seconds.
.rs.automation.addRemoteFunction("satellites.waitForOpen", function(name,
                                                                    retryCount = 50L,
                                                                    waitTimeSecs = 0.2)
{
   fmt <- "satellite '%s' to open"
   .rs.waitUntil(sprintf(fmt, name), function() {
      self$satellites.isOpen(name)
   }, waitTimeSecs = waitTimeSecs, retryCount = retryCount)
})

#' Switch to a satellite window
#'
#' Attaches to the satellite target and sets the global sessionId so
#' that all subsequent CDP commands route to it.
#'
#' @param name The title of the satellite window to switch to.
.rs.automation.addRemoteFunction("satellites.switchTo", function(name)
{
   satellites <- self$satellites.list()
   target <- Find(function(t) identical(t$title, name), satellites)
   if (is.null(target))
      stop(sprintf("No satellite window found with title '%s'", name))

   targetId <- target$targetId
   cache <- .rs.automation.satelliteSessions

   # Attach if we haven't cached a session for this target.
   if (!exists(targetId, envir = cache))
   {
      response <- self$client$Target.attachToTarget(
         targetId = targetId,
         flatten = TRUE
      )
      assign(targetId, response$sessionId, envir = cache)
   }

   sessionId <- get(targetId, envir = cache)
   .rs.setVar("automation.sessionId", sessionId)

   # Enable DOM domain for this session.
   self$client$DOM.enable()
})

#' Switch back to the main RStudio window
#'
#' Restores the global sessionId to the main window's session.
.rs.automation.addRemoteFunction("satellites.switchToMain", function()
{
   mainSessionId <- .rs.automation.mainSessionId
   if (is.null(mainSessionId))
      stop("No main session ID recorded")

   .rs.setVar("automation.sessionId", mainSessionId)
})

#' Execute a callback in a satellite window context
#'
#' Switches to the named satellite, runs the callback, then switches
#' back to the main window (even if the callback errors).
#'
#' @param name The title of the satellite window.
#' @param callback A function to execute while targeting the satellite.
#' @return The return value of the callback.
.rs.automation.addRemoteFunction("satellites.execute", function(name,
                                                                callback)
{
   self$satellites.switchTo(name)
   on.exit(self$satellites.switchToMain(), add = TRUE)
   callback()
})
