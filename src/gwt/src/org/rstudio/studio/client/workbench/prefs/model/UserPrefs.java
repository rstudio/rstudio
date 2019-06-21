/*
 * UserPrefs.java
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
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.notebook.CompileNotebookPrefs;
import org.rstudio.studio.client.notebookv2.CompileNotebookv2Prefs;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

@Singleton
public class UserPrefs extends UserPrefsComputed implements UserPrefsChangedHandler, SessionInitHandler
{
   @Inject
   public UserPrefs(Session session, 
                  EventBus eventBus,
                  PrefsServerOperations server,
                  SatelliteManager satelliteManager)
   {
      super(session.getSessionInfo(),
            (session.getSessionInfo() == null ? 
               JsArray.createArray().cast() :
               session.getSessionInfo().getPrefs()));

      session_ = session;
      server_ = server;
      satelliteManager_ = satelliteManager;

      eventBus.addHandler(UserPrefsChangedEvent.TYPE, this);
      eventBus.addHandler(SessionInitEvent.TYPE, this);
   }
   
   public void writeUserPrefs()
   {
      server_.setUserPrefs(
         session_.getSessionInfo().getUserPrefs(),
         new ServerRequestCallback<Void>() 
         {
            @Override
            public void onResponseReceived(Void v)
            {
               UserPrefsChangedEvent event = new UserPrefsChangedEvent(
                     session_.getSessionInfo().getUserPrefLayer());

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
   public void onUserPrefsChanged(UserPrefsChangedEvent e)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void onSessionInit(SessionInitEvent e)
   {
      // First update the theme and flat theme so the event will trigger.
      /*
      SessionInfo sessionInfo = session_.getSessionInfo();
      JsObject jsUiPrefs = sessionInfo.getUserPrefs();
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
   
   public static final int LAYER_DEFAULT = 0;
   public static final int LAYER_SYSTEM  = 1;
   public static final int LAYER_USER    = 2;
   public static final int LAYER_PROJECT = 3;
   
   private final Session session_;
   private final PrefsServerOperations server_;
   private final SatelliteManager satelliteManager_;
}
