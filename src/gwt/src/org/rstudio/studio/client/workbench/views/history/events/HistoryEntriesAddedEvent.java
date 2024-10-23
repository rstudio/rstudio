/*
 * HistoryEntriesAddedEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.history.events;

import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class HistoryEntriesAddedEvent extends GwtEvent<HistoryEntriesAddedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
      
      public native final RpcObjectList<HistoryEntry> entries() /*-{ return this["entries"] }-*/;
      public native final boolean update() /*-{ return this["update"]; }-*/;
   }
   
   public HistoryEntriesAddedEvent(Data data)
   {
      data_ = data;
   }

   public RpcObjectList<HistoryEntry> getEntries()
   {
      return data_.entries();
   }
   
   public boolean update()
   {
      return data_.update();
   }
   
   private final Data data_;

   // Boilerplate ----
   
   public interface Handler extends EventHandler
   {
      void onHistoryEntriesAdded(HistoryEntriesAddedEvent event);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onHistoryEntriesAdded(this);
   }

   public static final GwtEvent.Type<HistoryEntriesAddedEvent.Handler> TYPE = new GwtEvent.Type<>();

}
