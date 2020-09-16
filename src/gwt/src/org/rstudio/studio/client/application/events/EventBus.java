/*
 * EventBus.java
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
package org.rstudio.studio.client.application.events;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JavaScriptSerializer;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class EventBus extends HandlerManager implements FireEvents
{
   @Inject
   public EventBus(Provider<Satellite> pSatellite,
                   Provider<SatelliteManager> pManager)
   {
      super(null);
      serializer_ = GWT.create(JavaScriptSerializer.class);
      pSatellite_ = pSatellite;
      pManager_ = pManager;
      exportNativeCallbacks();
   }
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      fireEvent(event, false);
   }
   
   public void dispatchEvent(GwtEvent<?> event)
   {
      fireEvent(event, true);
   }
   
   private void fireEvent(GwtEvent<?> event, boolean fromOtherWindow)
   {
      // // uncomment this if you want to see a stream of dispatched client
      // // events logged into the JavaScript debug console
      //
      // debugLogEvents(event);
      
      // if this is a cross-window event that originated in this satellite 
      // window (and wasn't itself forwarded from somewhere else), pass it to
      // the main window
      if (event instanceof CrossWindowEvent &&
          Satellite.isCurrentWindowSatellite() &&
          !fromOtherWindow)
      {
         CrossWindowEvent<?> crossWindow = (CrossWindowEvent<?>)(event);
         if (crossWindow.forward())
         {
            JavaScriptObject jso = serializer_.serialize(event);

            // raise the main window if requested
            if (crossWindow.focusMode() == CrossWindowEvent.MODE_FOCUS)
               pSatellite_.get().focusMainWindow();
            else if (crossWindow.focusMode() == CrossWindowEvent.MODE_AUXILIARY &&
                     Desktop.hasDesktopFrame())
               Desktop.getFrame().bringMainFrameBehindActive();

            fireEventToMainWindow(jso, pSatellite_.get().getSatelliteName());
         }
         else
         {
            super.fireEvent(event);
         }
      }
      else
      {
         super.fireEvent(event);
      }
      
   }
   
   @Override
   public void fireEventToAllSatellites(CrossWindowEvent<?> event)
   {
      pManager_.get().dispatchCrossWindowEvent(event);
   }
   
   @Override
   public void fireEventToSatellite(CrossWindowEvent<?> event,
                                    WindowEx satelliteWindow)
   {
      fireEventToSatellite(serializer_.serialize(event), 
            satelliteWindow);
   }
   
   @Override
   public void fireEventToMainWindow(CrossWindowEvent<?> event)
   {
      if (Satellite.isCurrentWindowSatellite())
      {
         fireEventToMainWindow(serializer_.serialize(event), 
               pSatellite_.get().getSatelliteName());
      }
      else
      {
         fireEvent(event);
      }
   }

   /**
    * Similar to 2-arg form of addHandler, but automatically removes handler
    * when the HasAttachHandlers object detaches.
    *
    * If the HasAttachHandlers object detaches and reattaches, the handler
    * will NOT automatically resubscribe.
    */
   public <H extends EventHandler> void addHandler(
         HasAttachHandlers removeWhenDetached, Type<H> type, H handler)
   {
      final HandlerRegistration reg = addHandler(type, handler);
      removeWhenDetached.addAttachHandler(new Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (!event.isAttached())
               reg.removeHandler();
         }
      });
   }
   
   private final native void exportNativeCallbacks() /*-{
      var thiz = this;
      $wnd.fireRStudioEventExternal = $entry(
         function(eventData, windowName) {
            thiz.@org.rstudio.studio.client.application.events.EventBus::fireEventFromOtherWindow(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(eventData, windowName);
         }
      ); 
   }-*/;
   
   private void fireEventFromOtherWindow(JavaScriptObject data, 
         String windowName)
   {
      CrossWindowEvent<?> evt = 
            (CrossWindowEvent<?>)serializer_.deserialize(data);
      evt.setOriginWindowName(windowName);
      fireEvent(evt, true);
   }
   
   private final native void fireEventToMainWindow(JavaScriptObject data,
         String windowName) /*-{
      $wnd.opener.fireRStudioEventExternal(data, windowName);
   }-*/;
   
   private final native void fireEventToSatellite(JavaScriptObject data,
         WindowEx target) /*-{
      target.fireRStudioEventExternal(data, "");
   }-*/;
   
   private void debugLogEvents(GwtEvent<?> event)
   {
      Debug.logObject(event);
   }
   
   private Provider<Satellite> pSatellite_;
   private Provider<SatelliteManager> pManager_;
   private JavaScriptSerializer serializer_;
}
