/*
 * ReplaceRangesEvent.java
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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
public class ReplaceRangesEvent extends CrossWindowEvent<ReplaceRangesEvent.Handler>
{
   public static class ReplacementData extends JavaScriptObject
   {
      protected ReplacementData() {}
      
      public final native Range getRange() /*-{ return this["range"]; }-*/;
      public final native String getText() /*-{ return this["text"]; }-*/;
   }
   
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      
      public final native JsArray<ReplacementData> getReplacementData() /*-{
         
         if (this["result"] != null)
            return this["result"];
            
         var Range = $wnd.require("ace/range").Range;
         
         var ranges = this["ranges"];
         var text = this["text"];
         
         var isTextScalar = text.length === 1;
         
         var result = [];
         for (var i = 0; i < ranges.length; i++) {
            var range = ranges[i];
            result.push({
               range: new Range(range[0] - 1, range[1] - 1, range[2] - 1, range[3] - 1),
               text: isTextScalar ? text[0] : text[i]
            });
         }
         
         this["result"] = result;
         return result;
      
      }-*/;
   }
   
   public interface Handler extends EventHandler
   {
      void onReplaceRanges(ReplaceRangesEvent event);
   }
   
   public ReplaceRangesEvent()
   {
      this(null);
   }
   
   public ReplaceRangesEvent(Data data)
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
      handler.onReplaceRanges(this);
   }
   
   public static final Type<Handler> TYPE = new Type<Handler>();

}
