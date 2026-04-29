/*
 * RStudioAPIShowMenuEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

package org.rstudio.studio.client.common.rstudioapi.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RStudioAPIShowMenuEvent extends GwtEvent<RStudioAPIShowMenuEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getTitle() /*-{
         return this.title;
      }-*/;

      public final native String getMessage() /*-{
         return this.message;
      }-*/;

      public final native JsArrayString getChoices() /*-{
         return this.choices;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onRStudioAPIShowMenuEvent(RStudioAPIShowMenuEvent event);
   }

   public RStudioAPIShowMenuEvent(Data data)
   {
      data_ = data;
   }

   public String getTitle()
   {
      return data_.getTitle();
   }

   public String getMessage()
   {
      return data_.getMessage();
   }

   public String[] getChoices()
   {
      JsArrayString jsChoices = data_.getChoices();
      int n = jsChoices == null ? 0 : jsChoices.length();
      String[] result = new String[n];
      for (int i = 0; i < n; i++)
         result[i] = jsChoices.get(i);
      return result;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRStudioAPIShowMenuEvent(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
