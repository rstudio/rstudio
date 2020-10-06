/*
 * ApplicationVisibility.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.application;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.application.events.ApplicationVisibilityChangedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationVisibility
{
   @Inject
   public ApplicationVisibility(ApplicationServerOperations server,
                                EventBus eventBus,
                                SatelliteManager satelliteManager,
                                final Session session)
   {
      server_ = server;
      eventBus_ = eventBus;
      satelliteManager_ = satelliteManager;

      // don't register for visibility changed events in desktop mode
      if (Desktop.hasDesktopFrame())
         return;

      // initialize after we have session info
      eventBus_.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         // check for multi session
         isMultiSession_ = session.getSessionInfo().getMultiSession();

         // don't allow exceptions to escape (this code is being added
         // late in the release cycle and will run at workbench startup
         // so we need to make sure that unexpected errors don't bring
         // the entire ide down with them
         try
         {
            // register for page visibility changed events
            registerPageVisibilityChangedHandler();

            // check for being hidden 5 seconds after startup
            // and stop the event listener if we are (handles
            // cases where we never get visibility events because
            // a browser tab was "restored" or opened as part of
            // an "open all sessions" command
            new Timer() {
               @Override
               public void run()
               {
                  if (isHidden())
                     handleApplicationVisibilityChanged();
               }
            }.schedule(5000);
         }
         catch(Exception e)
         {
            Debug.logException(e);
         }

      });
   }

   private native final boolean isHidden() /*-{
      if (typeof $wnd.document.hidden !== "undefined")
         return $wnd.document.hidden;
      else if (typeof $wnd.document.mozHidden !== "undefined")
         return $wnd.document.mozHidden;
      else if (typeof $wnd.document.msHidden !== "undefined")
         return $wnd.document.msHidden;
      else if (typeof $wnd.document.webkitHidden !== "undefined")
         return $wnd.document.webkitHidden;
      else
         return false;
   }-*/;


   private native final void registerPageVisibilityChangedHandler() /*-{
      // determine name of visibilityChange event
      var visibilityChange;
      if (typeof $wnd.document.hidden !== "undefined")
         visibilityChange = "visibilitychange";
      else if (typeof $wnd.document.mozHidden !== "undefined")
         visibilityChange = "mozvisibilitychange";
      else if (typeof $wnd.document.msHidden !== "undefined")
         visibilityChange = "msvisibilitychange";
      else if (typeof $wnd.document.webkitHidden !== "undefined")
         visibilityChange = "webkitvisibilitychange";
      else
         visibilityChange = null;

      // add the event listener if we can
      if (typeof $wnd.document.addEventListener !== "undefined" ||
          visibilityChange !== null)
      {
         var thiz = this;
         $wnd.document.addEventListener(
            visibilityChange,
            $entry(function(e) {
               thiz.@org.rstudio.studio.client.application.ApplicationVisibility::handleApplicationVisibilityChanged()();
            }),
            false);
      }
   }-*/;

   private void handleApplicationVisibilityChanged()
   {
      // if we are multi session then manage the event listener
      // (to prevent an overload of long-polling connections being
      // opened to the server)
      if (isMultiSession_)
         manageEventListener();

      // fire visibility changed event
      eventBus_.fireEvent(new ApplicationVisibilityChangedEvent(isHidden()));
   }

   private void manageEventListener()
   {
      try
      {
         if (shouldStopEventListener())
         {
            // Stop the event listener but do it on a delay to provide the
            // system the time to confirm receipt of existing events.
            // This is necessary because some events (like browseURL) can
            // actually cause the de-activation of the window. If we stop
            // the event listener right away then the next get_events call
            // which acknowledges receipt of the event (e.g. browseURL) is
            // actually aborted before it can acknowledge receipt. The
            // subsequent call to start() then resets the lastEventId to -1
            // causing a re-delivery of the original event.
            //
            // Note that the specified delay (5 seconds) is somewhat arbitrary
            // The reason that we stop in the first place is to avoid
            // saturation of per-domain request limits so the stop needs to
            // occur reasonably soon but not right away.
            if (stopTimer_.isRunning())
               stopTimer_.cancel();
            stopTimer_.schedule(5000);
         }
         else
         {
            server_.ensureEventListener();
         }
      }
      catch(Exception e)
      {
         Debug.logException(e);
      }
   }

   private boolean shouldStopEventListener()
   {
      return isHidden() && !satelliteManager_.getSatellitesOpen();
   }

   private final Timer stopTimer_ = new Timer() {
      @Override
      public void run()
      {
         if (shouldStopEventListener())
            server_.stopEventListener();
      }
   };

   private final ApplicationServerOperations server_;
   private final EventBus eventBus_;
   private final SatelliteManager satelliteManager_;
   private boolean isMultiSession_ = false;
}
