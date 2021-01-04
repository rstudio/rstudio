/*
 * QuitEvent.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

public class QuitEvent extends GwtEvent<QuitEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final boolean getSwitchProjects() /*-{ return this.switch_projects; }-*/;
      public native final String getNextSessionUrl() /*-{ return this.next_session_url; }-*/;
   }
   
   public static final Type<Handler> TYPE = new Type<>();
   
   public QuitEvent(Data data)
   {
      data_ = data;
   }
   
   public boolean getSwitchProjects()
   {
      return data_.getSwitchProjects();
   }
   
   public String getNextSessionUrl()
   {
      return data_.getNextSessionUrl();
   }
   
   @Override
   protected void dispatch(Handler handler)
   {
      handler.onQuit(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final Data data_;

   public interface Handler extends EventHandler
   {
      void onQuit(QuitEvent event);
   }
}
