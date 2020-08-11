/*
 * EditorCommandEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

@JavaScriptSerializable
public class EditorCommandEvent extends CrossWindowEvent<EditorCommandEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      public final native String getType() /*-{ return this["type"]; }-*/;
      public final native JavaScriptObject getData() /*-{ return this["data"]; }-*/;
   }

   public EditorCommandEvent()
   {
      type_ = "";
      data_ = JavaScriptObject.createObject();
   }

   public EditorCommandEvent(Data data)
   {
      type_ = data.getType();
      data_ = data.getData();
   }

   public final String getType()
   {
      return type_;
   }

   @SuppressWarnings("unchecked")
   public final <T extends JavaScriptObject> T getData()
   {
      T casted = (T) data_;
      return casted;
   }

   private final String type_;
   private final JavaScriptObject data_;

   public static final String TYPE_REPLACE_RANGES = "replace_ranges";
   public static final String TYPE_SET_SELECTION_RANGES = "set_selection_ranges";
   public static final String TYPE_EDITOR_CONTEXT = "editor_context";

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onEditorCommand(EditorCommandEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onEditorCommand(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

}
