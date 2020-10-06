/*
 * RequestOpenProjectActionEvent.java
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

package org.rstudio.studio.client.projects.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RequestOpenProjectEvent
   extends GwtEvent<RequestOpenProjectEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getProjectFile()
      /*-{
         return this["project_file"];
      }-*/;

      public final native boolean isNewSession()
      /*-{
         return this["new_session"];
      }-*/;
   }

   public RequestOpenProjectEvent(Data data)
   {
      data_ = data;
   }

   public String getProjectFile()
   {
      return data_.getProjectFile();
   }

   public boolean isNewSession()
   {
      return data_.isNewSession();
   }

   private final Data data_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onRequestOpenProjectEvent(RequestOpenProjectEvent event);
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRequestOpenProjectEvent(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   public static final Type<Handler> TYPE = new Type<>();
}
