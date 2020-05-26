/*
 * SessionLabelChangedEvent.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SessionLabelChangedEvent extends GwtEvent<SessionLabelChangedEvent.Handler>
{  
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final String getLabel() /*-{ return this.label; }-*/;
    }

   public interface Handler extends EventHandler
   {
      void onSessionLabelChanged(SessionLabelChangedEvent event);
   }

   public static final Type<Handler> TYPE = new Type<>();

   public SessionLabelChangedEvent(Data data)
   {
      label_ = data.getLabel();
   }

   public SessionLabelChangedEvent(String label)
   {
      label_ = label;
   }

   public String getLabel()
   {
      return label_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSessionLabelChanged(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final String label_;
}
