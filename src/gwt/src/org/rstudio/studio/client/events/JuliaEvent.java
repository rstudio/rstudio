/*
 * JuliaEvent.java
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
package org.rstudio.studio.client.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

// The console's Julia REPL starting, ending, or evaluating (SessionJulia.R).
public class JuliaEvent extends GwtEvent<JuliaEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getType()           /*-{ return this["type"]; }-*/;
      public final native JavaScriptObject getData() /*-{ return this["data"]; }-*/;
   }

   // the payload of a REPL_INITIALIZED event
   public static class ReplInfo extends JavaScriptObject
   {
      protected ReplInfo()
      {
      }

      public final native String getVersion()    /*-{ return this["version"]; }-*/;
      public final native String getExecutable() /*-{ return this["executable"]; }-*/;
   }

   public JuliaEvent(Data data)
   {
      data_ = data;
   }

   public JavaScriptObject getPayload()
   {
      return data_.getData();
   }

   public String getType()
   {
      return data_.getType();
   }

   private final Data data_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onJulia(JuliaEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onJulia(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   // synchronize with SessionJulia.R
   public static final String TYPE_REPL_INITIALIZED = "repl_initialized";
   public static final String TYPE_REPL_BUSY        = "repl_busy";
   public static final String TYPE_REPL_TEARDOWN    = "repl_teardown";
}
