/*
 * JumpToFunctionEvent.java
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
package org.rstudio.studio.client.workbench.views.environment.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class JumpToFunctionEvent
        extends GwtEvent<JumpToFunctionEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getFileName() /*-{
         return this.file_name;
      }-*/;

      public final native int getLineNumber() /*-{
         return this.line_number;
      }-*/;

      public final native int getColumnNumber() /*-{
         return this.column_number;
      }-*/;

      public final native boolean getMoveCursor() /*-{
         return this.move_cursor;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onJumpToFunction(JumpToFunctionEvent event);
   }

   public JumpToFunctionEvent(Data data)
   {
      data_ = data;
   }

   public int getLineNumber()
   {
      return data_.getLineNumber();
   }

   public int getColumnNumber()
   {
      return data_.getColumnNumber();
   }

   public String getFileName()
   {
      return data_.getFileName();
   }

   public boolean getMoveCursor()
   {
      return data_.getMoveCursor();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onJumpToFunction(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
   private final Data data_;
}
