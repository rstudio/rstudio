/*
 * UserState.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.events.UserStateChangedEvent;

@Singleton
public class UserState extends UserStateAccessor implements UserStateChangedEvent.Handler, SessionInitHandler
{
   @Inject
   public UserState(Session session, 
                  EventBus eventBus,
                  PrefsServerOperations server,
                  SatelliteManager satelliteManager)
   {
      super(session.getSessionInfo(),
            (session.getSessionInfo() == null ? 
               JsArray.createArray().cast() :
               session.getSessionInfo().getUserState()));

      session_ = session;
      server_ = server;
      satelliteManager_ = satelliteManager;

      eventBus.addHandler(UserStateChangedEvent.TYPE, this);
      eventBus.addHandler(SessionInitEvent.TYPE, this);
   }
   
   public void writeState()
   {
      server_.setUserState(
         session_.getSessionInfo().getUserState(),
         new ServerRequestCallback<Void>() 
         {
            @Override
            public void onResponseReceived(Void v)
            {
               UserStateChangedEvent event = new UserStateChangedEvent(
                              session_.getSessionInfo().getUserStateLayer());

               if (Satellite.isCurrentWindowSatellite())
               {
                  RStudioGinjector.INSTANCE.getEventBus()
                     .fireEventToMainWindow(event);
               }
               else
               {
                  // let satellites know prefs have changed
                  satelliteManager_.dispatchCrossWindowEvent(event);
               }
            }
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
   }
   
   @Override
   public void onUserStateChanged(UserStateChangedEvent e)
   {
      syncPrefs(e.getName(), e.getValues());
   }

   @Override
   public void onSessionInit(SessionInitEvent e)
   {
      /*
       * TODO
      // First update the theme and flat theme so the event will trigger.
      SessionInfo sessionInfo = session_.getSessionInfo();
      JsObject jsUiPrefs = sessionInfo.getUserState();
      AceTheme aceTheme = jsUiPrefs.getElement("rstheme");
      if (null != aceTheme)
      {
         theme().setGlobalValue(aceTheme);
      }
      
      String flatTheme = jsUiPrefs.getString("flat_theme");
      if (null != flatTheme)
      {
         getFlatTheme().setGlobalValue(flatTheme);
      }
      
      // The satellite window has just received the session info, so update it now.
      UpdateSessionInfo(sessionInfo);
      */
   }
   
   private final Session session_;
   private final PrefsServerOperations server_;
   private final SatelliteManager satelliteManager_;
}