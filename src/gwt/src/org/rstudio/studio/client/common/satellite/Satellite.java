package org.rstudio.studio.client.common.satellite;

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
   
   // satellite windows should call this during startup to setup a 
   // communication channel with the main window
   public native void initialize() /*-{
      
      // global flag used to conditionalize behavior
      $wnd.isRStudioSatellite = true;
      
      // export setSessionInfo callback
      var satellite = this;     
      $wnd.setRStudioSatelliteSessionInfo = $entry(
         function(sessionInfo) {
            satellite.@org.rstudio.studio.client.common.satellite.Satellite::setSessionInfo(Lcom/google/gwt/core/client/JavaScriptObject;)(sessionInfo);
         }
      ); 
      
      // export event notification callback
      $wnd.dispatchEventToRStudioSatellite = $entry(
         function(clientEvent) {
            satellite.@org.rstudio.studio.client.common.satellite.Satellite::dispatchEvent(Lcom/google/gwt/core/client/JavaScriptObject;)(clientEvent);
         }
      ); 
      
      // register (this will call the setSessionInfo back)
      $wnd.opener.registerAsRStudioSatellite($wnd);
   }-*/;
   
   
   // check whether the current window is a satellite
   public native boolean isCurrentWindowSatellite() /*-{
      return !!$wnd.isRStudioSatellite;
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
   
   // called by main window to deliver events
   private void dispatchEvent(JavaScriptObject clientEvent)
   {  
      eventDispatcher_.enqueEventAsJso(clientEvent);
   }
   
   
   private final Session session_;
   private final Provider<UIPrefs> pUIPrefs_;
   private final ClientEventDispatcher eventDispatcher_;
}
