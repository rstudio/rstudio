/*
 * Satellite.java
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
package org.rstudio.studio.client.common.satellite;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.mathjax.MathJaxLoader;
import org.rstudio.studio.client.common.satellite.events.SatelliteFocusedEvent;
import org.rstudio.studio.client.server.remote.ClientEventDispatcher;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class Satellite implements HasCloseHandlers<Satellite>
{
   @Inject
   public Satellite(Session session,
                    EventBus eventBus,
                    Commands commands,
                    Provider<UserPrefs> pUIPrefs)
   {
      session_ = session;
      pUIPrefs_ = pUIPrefs;
      commands_ = commands;
      events_ = eventBus;
      eventDispatcher_ = new ClientEventDispatcher(eventBus);
   }

   public void initialize(String name,
                          CommandWithArg<JavaScriptObject> onReactivated)
   {
      onReactivated_ = onReactivated;
      initializeNative(name);

      // load MathJax
      MathJaxLoader.ensureMathJaxLoaded();

      // load focus-visible polyfill
      RStudioGinjector.INSTANCE.getFocusVisiblePolyfill().load(null);

      // NOTE: Desktop doesn't seem to get onWindowClosing events in Qt 4.8
      // so we instead rely on an explicit callback from the desktop frame
      // to notifyRStudioSatelliteClosing
      if (!Desktop.hasDesktopFrame())
      {
         Window.addWindowClosingHandler(new ClosingHandler() {
            @Override
            public void onWindowClosing(ClosingEvent event)
            {
               fireCloseEvent();
            }
         });
      }

      // let main window know when we get focus
      WindowEx.addFocusHandler(new FocusHandler()
      {

         @Override
         public void onFocus(FocusEvent arg0)
         {
            events_.fireEvent(new SatelliteFocusedEvent(getSatelliteName()));
         }
      });
   }

   @Override
   public HandlerRegistration addCloseHandler(CloseHandler<Satellite> handler)
   {
      return handlerManager_.addHandler(CloseEvent.getType(), handler);
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlerManager_.fireEvent(event);
   }

   public boolean isReactivatePending()
   {
      return pendingReactivate_;
   }

   public boolean isClosePending()
   {
      return pendingClose_;
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
            if (params == null)
               return;
            satellite.@org.rstudio.studio.client.common.satellite.Satellite::notifyReactivated(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      );


      // export notifyClosing
      $wnd.notifyRStudioSatelliteClosing = $entry(function() {
         // see remarks in WindowEx::isClosed
         $wnd.rstudioSatelliteClosed = true;
         satellite.@org.rstudio.studio.client.common.satellite.Satellite::fireCloseEvent()();
      });

      // export event notification callback
      $wnd.dispatchEventToRStudioSatellite = $entry(
         function(clientEvent) {
            satellite.@org.rstudio.studio.client.common.satellite.Satellite::dispatchEvent(Lcom/google/gwt/core/client/JavaScriptObject;)(clientEvent);
         }
      );

      // export request activation callback
      $wnd.notifyPendingReactivate = $entry(function() {
         satellite.@org.rstudio.studio.client.common.satellite.Satellite::notifyPendingReactivate()();
      });

      // export pending closure callback
      $wnd.notifyPendingClosure = $entry(function() {
         satellite.@org.rstudio.studio.client.common.satellite.Satellite::notifyPendingClosure()();
      });

      // export point containment callback
      $wnd.isPointWithinSatellite = $entry(function(x, y) {
         // x and y are screen coordinates; translate them into window
         // (viewport) coordinates
         return $wnd.document.elementFromPoint(x - $wnd.screenX, y - $wnd.screenY) != null;
      });

      $wnd.addEventListener(
            "unload",
            $entry(function() {
               $wnd.opener.notifyRStudioSatelliteClosed(name);
            }),
            true);

      // register (this will call the setSessionInfo back)
      $wnd.opener.registerAsRStudioSatellite(name, $wnd);
   }-*/;


   // check whether the current window is a satellite
   public static native boolean isCurrentWindowSatellite() /*-{
      return !!$wnd.isRStudioSatellite;
   }-*/;

   // get the name of the current satellite window (null if not a satellite)
   public native String getSatelliteName() /*-{
      return $wnd.RStudioSatelliteName;
   }-*/;

   // satellites only: call the main window to ask if it knows about any windows
   // (including itself) at the given location
   public native String getWindowAtPoint(int x, int y) /*-{
      return $wnd.opener.getWindowAtPoint(x, y);
   }-*/;

   public JavaScriptObject getParams()
   {
      return params_;
   }

   public void focusMainWindow()
   {
      if (Desktop.hasDesktopFrame())
         focusMainWindowDesktop();
      else
         focusMainWindowWeb();
   }

   private void focusMainWindowDesktop()
   {
      Desktop.getFrame().bringMainFrameToFront();
   }

   private native void focusMainWindowWeb() /*-{
      $wnd.opener.focus();
   }-*/;

   // called by main window to initialize sessionInfo
   private void setSessionInfo(JavaScriptObject si)
   {
      // hack to ensure addins command manager initialized
      RStudioGinjector.INSTANCE.getAddinsCommandManager();

      // get the session info and set it
      SessionInfo sessionInfo = si.cast();
      session_.setSessionInfo(sessionInfo);

      // some objects wait for SessionInit in order to initialize themselves
      // with SessionInfo
      events_.fireEvent(new SessionInitEvent());

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
      pendingReactivate_ = false;
   }

   private void fireCloseEvent()
   {
      CloseEvent.fire(this, this);
   }

   // called by main window to deliver events
   private void dispatchEvent(JavaScriptObject clientEvent)
   {
      eventDispatcher_.enqueEventAsJso(clientEvent);
   }

   // called by the main window to notify us that we're about to be reactivated
   private void notifyPendingReactivate()
   {
      pendingReactivate_ = true;
   }

   // called by the main window to notify us that we're about to be closed
   private void notifyPendingClosure()
   {
      pendingClose_ = true;
   }

   private final Session session_;
   private final Provider<UserPrefs> pUIPrefs_;
   private final ClientEventDispatcher eventDispatcher_;
   private final HandlerManager handlerManager_ = new HandlerManager(this);
   private final Commands commands_;
   private final EventBus events_;
   private boolean pendingReactivate_ = false;
   private boolean pendingClose_ = false;
   private JavaScriptObject params_ = null;
   private CommandWithArg<JavaScriptObject> onReactivated_ = null;
}
