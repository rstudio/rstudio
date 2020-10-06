/*
 * RmdParamsEditEvent.java
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

package org.rstudio.studio.client.rmarkdown.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ShinyGadgetDialogEvent extends GwtEvent<ShinyGadgetDialogEvent.Handler>
{

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getCaption() /*-{
         return this.caption;
      }-*/;

      public final native String getUrl() /*-{
         return this.url;
      }-*/;

      public final native int getPreferredWidth() /*-{
         return this.width;
      }-*/;

      public final native int getPreferredHeight() /*-{
         return this.height;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onShinyGadgetDialog(ShinyGadgetDialogEvent event);
   }

   public ShinyGadgetDialogEvent(Data data)
   {
      data_ = data;
   }

   public String getCaption()
   {
      return data_.getCaption();
   }

   public String getUrl()
   {
      return data_.getUrl();
   }

   public int getPreferredWidth()
   {
      return data_.getPreferredWidth();
   }

   public int getPreferredHeight()
   {
      return data_.getPreferredHeight();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onShinyGadgetDialog(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
