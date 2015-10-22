/*
 * ApplicationVisibility.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationVisibility 
{
   @Inject
   public ApplicationVisibility(ApplicationServerOperations server,
                                final EventBus eventBus,
                                SatelliteManager satelliteManager,
                                final Session session)
   {
      server_ = server;
      satelliteManager_ = satelliteManager;
      
      // don't register for visibility changed events in desktop mode
      if (Desktop.isDesktop())
         return;
     
      // initialize after we have session info
      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler() {
         @Override
         public void onSessionInit(SessionInitEvent sie)
         {
            // only register for page visibility events if we are 
            // in multi-session mode
            SessionInfo sessionInfo = session.getSessionInfo();
            if (sessionInfo.getMultiSession())
            {
               // don't allow exceptions to escape (this code is being added 
               // late in the release cycle and will run at workbench startup 
               // so we need to make sure that unexpected errors don't bring 
               // the entire ide down with them
               try
               {
                  // register for page visibility changed events
                  registerPageVisibilityChangedHandler();   
               }
               catch(Exception e)
               {
                  Debug.logException(e);
               }
            }  
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
               thiz.@org.rstudio.studio.client.application.ApplicationVisibility::onVisibilityChanged(Lcom/google/gwt/core/client/JavaScriptObject;)(e);
            }), 
            false);
      }
   }-*/;
   
   private void onVisibilityChanged(JavaScriptObject data)
   {
      boolean hidden = isHidden();
      boolean haveNoSatellitesOpen = !satelliteManager_.getSatellitesOpen();
      
      // stop listening or restart listening as appropriate
      boolean stop = hidden && haveNoSatellitesOpen;
      if (stop)
         server_.stopEventListener();
      else
         server_.ensureEventListener();   
      
      // optional debug output
      /*
      if (stop)
         Debug.logToRConsole("Stopped Listening");
      else
         Debug.logToRConsole("Listening to Events");
      Debug.logToRConsole(" {hidden: " + hidden +
                          ", noSatellites: " + haveNoSatellitesOpen +"}");
      */
   }
   
   private final ApplicationServerOperations server_;
   private final SatelliteManager satelliteManager_;
}
