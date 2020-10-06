/*
 * ReticulateEvent.java
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
package org.rstudio.studio.client.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ReticulateEvent extends GwtEvent<ReticulateEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      // Event data accessors ----
      public final native String getType() /*-{ return this["type"]; }-*/;

   }

   public ReticulateEvent(Data data)
   {
      data_ = data;
   }

   public Data getData()
   {
      return data_;
   }

   public String getType()
   {
      return data_.getType();
   }

   private final Data data_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onReticulate(ReticulateEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onReticulate(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   // synchronize with SessionReticulate.R
   public static final String TYPE_PYTHON_INITIALIZED = "python_initialized";
   public static final String TYPE_REPL_INITIALIZED   = "repl_initialized";
   public static final String TYPE_REPL_ITERATION     = "repl_iteration";
   public static final String TYPE_REPL_TEARDOWN      = "repl_teardown";
}
