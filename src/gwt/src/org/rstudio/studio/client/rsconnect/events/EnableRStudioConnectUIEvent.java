/*
 * EnableRStudioConnectUIEvent.java
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
package org.rstudio.studio.client.rsconnect.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EnableRStudioConnectUIEvent 
   extends GwtEvent<EnableRStudioConnectUIEvent.Handler>
{ 
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
      
      public final native boolean getEnable() /*-{
         return this;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onEnableRStudioConnectUI(EnableRStudioConnectUIEvent event);
   }

   public static final GwtEvent.Type<EnableRStudioConnectUIEvent.Handler> TYPE =
      new GwtEvent.Type<EnableRStudioConnectUIEvent.Handler>();
   
   public EnableRStudioConnectUIEvent(Data enable)
   {
      enable_ = enable.getEnable();
   }
   
   public boolean getEnable()
   {
      return enable_;
   }
   
   @Override
   protected void dispatch(EnableRStudioConnectUIEvent.Handler handler)
   {
      handler.onEnableRStudioConnectUI(this);
   }

   @Override
   public GwtEvent.Type<EnableRStudioConnectUIEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private boolean enable_;
}
