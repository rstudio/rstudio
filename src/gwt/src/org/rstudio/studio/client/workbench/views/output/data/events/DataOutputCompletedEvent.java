/*
 * DataOutputCompletedEvent.java
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
package org.rstudio.studio.client.workbench.views.output.data.events;

import org.rstudio.studio.client.workbench.views.output.data.model.DataOutputResult;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class DataOutputCompletedEvent extends GwtEvent<DataOutputCompletedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onDataOutputCompleted(DataOutputCompletedEvent event);
   }

   public DataOutputCompletedEvent(DataOutputResult data)
   {
      data_ = data;
   }

   public JavaScriptObject getData()
   {
      return data_;
   }

   public String getTitle()
   {
      return data_.getTitle();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDataOutputCompleted(this);
   }

   private final DataOutputResult data_;

   public static final Type<Handler> TYPE = new Type<>();
}
