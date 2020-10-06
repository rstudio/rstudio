/*
 * HighlightEvent.java
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
package org.rstudio.core.client.events;

import org.rstudio.core.client.JsVector;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class HighlightEvent extends GwtEvent<HighlightEvent.Handler>
{
   public static class Data extends JsVector<HighlightQuery>
   {
      protected Data()
      {
      }
   }

   public static class HighlightQuery extends JavaScriptObject
   {
      protected HighlightQuery()
      {
      }

      public static final native HighlightQuery create(String query, int parent, String callback)
      /*-{
         return {
            query: query || "",
            parent: parent || 0,
            callback: callback || ""
         }
      }-*/;

      public final native String getQuery()
      /*-{
         return this.query || "";
      }-*/;

      public final native int getParent()
      /*-{
         return this.parent || 0;
      }-*/;

      public final native String getCallback()
      /*-{
         return this.callback || "";
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onHighlight(HighlightEvent event);
   }

   public HighlightEvent(Data data)
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
      handler.onHighlight(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
