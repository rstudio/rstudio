/*
 * MarkersChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.output.markers.events;

import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class MarkersChangedEvent extends GwtEvent<MarkersChangedEvent.Handler>
{
   static public class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final MarkersState getMarkersState() /*-{
         return this.markers_state;
      }-*/;

      public native final int getAutoSelect() /*-{
         return this.auto_select;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onMarkersChanged(MarkersChangedEvent event);
   }

   public MarkersChangedEvent(Data data)
   {
      data_ = data;
   }

   public MarkersState getMarkersState()
   {
      return data_.getMarkersState();
   }

   public int getAutoSelect()
   {
      return data_.getAutoSelect();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onMarkersChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final Data data_;
}
