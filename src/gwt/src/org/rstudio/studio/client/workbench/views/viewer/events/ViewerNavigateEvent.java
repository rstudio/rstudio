/*
 * ViewerNavigateEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.viewer.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ViewerNavigateEvent extends GwtEvent<ViewerNavigateEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
      
      public native final String getURL() /*-{
         return this.url;
      }-*/;
      
      public native final boolean getFullHeight() /*-{
         return this.full_height;
      }-*/;
      
      
   }
   
   public interface Handler extends EventHandler
   {
      void onViewerNavigate(ViewerNavigateEvent event);
   }

   public ViewerNavigateEvent(Data data)
   {
      data_ = data;
   }
   
   public String getURL()
   {
      return data_.getURL();
   }
   
   public boolean getFullHeight()
   {
      return data_.getFullHeight();
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onViewerNavigate(this);
   }
   
   private final Data data_;
  
   public static final Type<Handler> TYPE = new Type<Handler>();
}
