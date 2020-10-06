/*
 * SetSelectionRangesEvent.java
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
public class SetSelectionRangesEvent extends CrossWindowEvent<SetSelectionRangesEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public final native String getId() /*-{ return this["id"]; }-*/;
      public final native JsArray<Range> getRanges() /*-{

         if (this["$ranges"])
            return this["$ranges"];

         var result = [];
         var Range = $wnd.require("ace/range").Range;

         var ranges = this["ranges"];
         for (var i = 0; i < ranges.length; i++) {
            var range = ranges[i];
            result.push(new Range(range[0], range[1], range[2], range[3]));
         }

         this["$ranges"] = result;
         return result;

      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onSetSelectionRanges(SetSelectionRangesEvent event);
   }

   public SetSelectionRangesEvent()
   {
      this(null);
   }

   public SetSelectionRangesEvent(Data data)
   {
      data_ = data;
   }

   public Data getData()
   {
      return data_;
   }

   private final Data data_;

   // Boilerplate ----

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSetSelectionRanges(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

}
