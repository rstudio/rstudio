/*
 * MonitoredFileChangedEvent.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.files.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class MonitoredFileChangedEvent extends GwtEvent<MonitoredFileChangedEvent.Handler>
{
   // File change type constants matching core::system::FileChangeEvent::Type
   public static final int NONE = 0;
   public static final int FILE_ADDED = 1;
   public static final int FILE_REMOVED = 3;
   public static final int FILE_MODIFIED = 4;

   public interface Handler extends EventHandler
   {
      void onMonitoredFileChanged(MonitoredFileChangedEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final String getPath() /*-{
         return this.path;
      }-*/;

      public native final int getType() /*-{
         return this.type;
      }-*/;
   }

   public MonitoredFileChangedEvent(Data data)
   {
      data_ = data;
   }

   public Data getData()
   {
      return data_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onMonitoredFileChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final Data data_;
}

