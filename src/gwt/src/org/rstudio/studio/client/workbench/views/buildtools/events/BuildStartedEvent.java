/*
 * BuildStartedEvent.java
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
package org.rstudio.studio.client.workbench.views.buildtools.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BuildStartedEvent extends GwtEvent<BuildStartedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getType() /*-{
         return this.type;
      }-*/;

      public final native String getSubType() /*-{
         return this.sub_type;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onBuildStarted(BuildStartedEvent event);
   }

   public BuildStartedEvent(Data data)
   {
      data_ = data;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onBuildStarted(this);
   }

   public String getType()
   {
      return data_.getType();
   }

   public String getSubType()
   {
      return data_.getSubType();
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
