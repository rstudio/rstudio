/*
 * ViewerNavigateEvent.java
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

      public native final int getHeight() /*-{
         return this.height;
      }-*/;

      public native final boolean isHTMLWidget() /*-{
         return this.html_widget;
      }-*/;

      public native final boolean hasNext() /*-{
         return this.has_next;
      }-*/;

      public native final boolean hasPrevious() /*-{
         return this.has_previous;
      }-*/;

      public native final boolean bringToFront() /*-{
         return this.bring_to_front;
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

   public int getHeight()
   {
      return data_.getHeight();
   }

   public boolean isHTMLWidget()
   {
      return data_.isHTMLWidget();
   }

   public boolean getHasNext()
   {
      return data_.hasNext();
   }

   public boolean getHasPrevious()
   {
      return data_.hasPrevious();
   }

   public boolean getBringToFront()
   {
      return data_.bringToFront();
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

   public static final Type<Handler> TYPE = new Type<>();
}
