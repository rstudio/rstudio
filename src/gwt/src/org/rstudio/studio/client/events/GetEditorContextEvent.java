/*
 * GetEditorContextEvent.java
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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

@JavaScriptSerializable
public class GetEditorContextEvent extends CrossWindowEvent<GetEditorContextEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public static final native Data create() /*-{ return 0; }-*/;
      public final native int getType() /*-{ return this; }-*/;
   }

   public static class DocumentSelection extends JavaScriptObject
   {
      protected DocumentSelection() {}

      public static final native DocumentSelection create(Range range, String text)
      /*-{
         return {
            "range": [range.start.row, range.start.column, range.end.row, range.end.column],
            "text": text
         };
      }-*/;
   }

   public static class SelectionData extends JavaScriptObject
   {
      protected SelectionData() {}

      public static final native SelectionData create()
      /*-{
         return {
            "id": "",
            "path": "",
            "contents": "",
            "selection": []
         };
      }-*/;

      public static final native SelectionData create(String id,
                                                      String path,
                                                      String contents,
                                                      JsArray<DocumentSelection> selection)
      /*-{
         return {
            "id": id,
            "path": path,
            "contents": contents,
            "selection": selection,
         };
      }-*/;

      public final native String getId() /*-{ return this["id"]; }-*/;
      public final native String getPath() /*-{ return this["path"]; }-*/;
      public final native String getContents() /*-{ return this["contents"]; }-*/;
      public final native DocumentSelection getSelection() /*-{ return this["selection"]; }-*/;
   }

   public GetEditorContextEvent()
   {
      this(Data.create());
   }

   public GetEditorContextEvent(Data data)
   {
      data_ = data;
   }

   public Data getData()
   {
      return data_;
   }

   private final Data data_;

   public static final int TYPE_ACTIVE_EDITOR  = 0;
   public static final int TYPE_CONSOLE_EDITOR = 1;
   public static final int TYPE_SOURCE_EDITOR  = 2;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onGetEditorContext(GetEditorContextEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onGetEditorContext(this);
   }

   public static final Type<Handler> TYPE = new Type<>();


}
