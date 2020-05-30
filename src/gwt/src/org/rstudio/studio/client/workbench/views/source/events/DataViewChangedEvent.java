/*
 * DataViewChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class DataViewChangedEvent extends GwtEvent<DataViewChangedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      
      public final native String getCacheKey() /*-{
         return this.cache_key;
      }-*/;

      public final native boolean structureChanged() /*-{
         return this.structure_changed;
      }-*/;
   }

   public static final GwtEvent.Type<DataViewChangedEvent.Handler> TYPE =
      new GwtEvent.Type<DataViewChangedEvent.Handler>();

   public interface Handler extends EventHandler
   {
      void onDataViewChanged(DataViewChangedEvent event);
   }

   public DataViewChangedEvent(Data data)
   {
      data_ = data;
   }
   
   public Data getData()
   {
      return data_;
   }
   
   @Override
   protected void dispatch(DataViewChangedEvent.Handler handler)
   {
      handler.onDataViewChanged(this);
   }

   @Override
   public GwtEvent.Type<DataViewChangedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private Data data_;
}
