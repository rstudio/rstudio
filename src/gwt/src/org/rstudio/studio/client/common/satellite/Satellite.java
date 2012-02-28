/*
 * Satellite.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.satellite;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.remote.ClientEventDispatcher;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class Satellite
{  
   @Inject
   public Satellite(Session session,
                    EventBus eventBus,
                    Provider<UIPrefs> pUIPrefs)
   {
      session_ = session;
      pUIPrefs_ = pUIPrefs;
      eventDispatcher_ = new ClientEventDispatcher(eventBus);
   }
   
   public void initialize(String name, 
                          CommandWithArg<JavaScriptObject> onReactivated)
   {
      onReactivated_ = onReactivated;
      initializeNative(name);
   }

   public native final void flushPendingEvents(String name) /*-{
      $wnd.opener.flushPendingEvents(name);
   }-*/;

   // satellite windows should call this during startup to setup a 
   // communication channel with the main window
   private native void initializeNative(String name) /*-{
      
      // global flag used to conditionalize behavior
      $wnd.isRStudioSatellite = true;
      $wnd.RStudioSatelliteName = name;
      
      // export setSessionInfo callback
      var satellite = this;     
      $wnd.setRStudioSatelliteSessionInfo = $entry(
         function(sessionInfo) {
            satellite.@org.rstudio.studio.client.common.satellite.Satellite::setSessionInfo(Lcom/google/gwt/core/client/JavaScriptObject;)(sessionInfo);
         }
      ); 
      
      // export setParams callback
      $wnd.setRStudioSatelliteParams = $entry(
         function(params) {
            satellite.@org.rstudio.studio.client.common.satellite.Satellite::setParams(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      ); 
      
      // export notifyReactivated callback
      $wnd.notifyRStudioSatelliteReactivated = $entry(
         function(params) {
            satellite.@org.rstudio.studio.client.common.satellite.Satellite::notifyReactivated(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      ); 
      
      // export event notification callback
      $wnd.dispatchEventToRStudioSatellite = $entry(
         function(clientEvent) {
            satellite.@org.rstudio.studio.client.common.satellite.Satellite::dispatchEvent(Lcom/google/gwt/core/client/JavaScriptObject;)(clientEvent);
         }
      ); 
      
      // register (this will call the setSessionInfo back)
      $wnd.opener.registerAsRStudioSatellite(name, $wnd);
   }-*/;
   
   
   // check whether the current window is a satellite
   public native boolean isCurrentWindowSatellite() /*-{
      return !!$wnd.isRStudioSatellite;
   }-*/;
   
   // get the name of the current satellite window (null if not a satellite)
   public native String getSatelliteName() /*-{
      return $wnd.RStudioSatelliteName;
   }-*/;
   
   public JavaScriptObject getParams()
   {
      return params_;
   }
   
   public native void focusMainWindow() /*-{
      $wnd.opener.focus();
   }-*/;
   
   // called by main window to initialize sessionInfo
   private void setSessionInfo(JavaScriptObject si)
   {
      // get the session info and set it
      SessionInfo sessionInfo = si.<SessionInfo>cast();
      session_.setSessionInfo(sessionInfo);
  
      // ensure ui prefs initialize
      pUIPrefs_.get();
   }
   
   // called by main window to setParams
   private void setParams(JavaScriptObject params)
   {
      params_ = params;
   }
   
   
   // called by main window to notify us of reactivation with a new
   // set of params
   private void notifyReactivated(JavaScriptObject params)
   {
      if (onReactivated_ != null)
         onReactivated_.execute(params);
   }
   
   // called by main window to deliver events
   private void dispatchEvent(JavaScriptObject clientEvent)
   {  
      eventDispatcher_.enqueEventAsJso(clientEvent);
   }
   
   
   private final Session session_;
   private final Provider<UIPrefs> pUIPrefs_;
   private final ClientEventDispatcher eventDispatcher_;
   private JavaScriptObject params_ = null;
   private CommandWithArg<JavaScriptObject> onReactivated_ = null;
}
