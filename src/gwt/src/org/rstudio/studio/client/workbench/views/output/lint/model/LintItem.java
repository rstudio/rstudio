/*
 * LintItem.java
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
package org.rstudio.studio.client.workbench.views.output.lint.model;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class LintItem extends JavaScriptObject
{
   protected LintItem() {}
   
   public static final native LintItem create(int startRow,
                                              int startColumn,
                                              int endRow,
                                              int endColumn,
                                              String text,
                                              String type) /*-{
      
      return {
         "start.row": startRow,
         "start.column": startColumn,
         "end.row": endRow,
         "end.column": endColumn,
         "text": text,
         "type": type
      };
                                
   }-*/;
   
   public final native int getStartRow() /*-{
      return this["start.row"];
   }-*/;
   
   public final native int getStartColumn() /*-{
      return this["start.column"];
   }-*/;
   
   public final native int getEndRow() /*-{
      return this["end.row"];
   }-*/;
   
   public final native int getEndColumn() /*-{
      return this["end.column"];
   }-*/;
   
   public final native String getText() /*-{
      return this["text"];
   }-*/;
   
   public final native String getType() /*-{
      return this["type"];
   }-*/;
   
   public final Range asRange()
   {
      return Range.fromPoints(
            Position.create(getStartRow(), getStartColumn()),
            Position.create(getEndRow(), getEndColumn()));
   }
   
   public final AceAnnotation asAceAnnotation()
   {
      return AceAnnotation.create(
            getStartRow(),
            getStartColumn(),
            getText(),
            getType());
   }
   
   public static final native JsArray<AceAnnotation> asAceAnnotations(
         JsArray<LintItem> items)
   /*-{
      
      var aceAnnotations = [];
      
      for (var key in items)
      {
         var type = items[key]["type"];
         if (type === "style" || type === "note")
            type = "info";
            
         aceAnnotations.push({
            row: items[key]["start.row"],
            column: items[key]["start.column"],
            text: items[key]["text"],
            type: type
         });
      }
      
      return aceAnnotations;
         
   }-*/;
   
}
