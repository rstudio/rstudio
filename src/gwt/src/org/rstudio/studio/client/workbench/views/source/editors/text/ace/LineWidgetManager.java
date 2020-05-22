/*
 * LineWidgetManager.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class LineWidgetManager extends JavaScriptObject
{
   protected LineWidgetManager() {}
   
   public native final void addLineWidget(LineWidget widget) /*-{
      
      // avoid duplicating a pre-existing line widget
      var widgets = this.session.lineWidgets || {};
      if (widgets[widget.row] === widget)
         return;
         
      this.addLineWidget(widget);
      
   }-*/;
   
   public native final void removeLineWidget(LineWidget widget) /*-{
      this.removeLineWidget(widget);
   }-*/;
   
   public native final void removeAllLineWidgets() /*-{
      var lineWidgetsByRow = this.session.lineWidgets;
      if (lineWidgetsByRow) {
         var self = this;
         lineWidgetsByRow.forEach(function(w, i) {
            if (w) 
               self.removeLineWidget(w);
         });
      }
   }-*/;
   
   public native final void onWidgetChanged(LineWidget widget) /*-{
      this.onWidgetChanged(widget);
   }-*/;
   
   public native final JsArray<LineWidget> getLineWidgets() /*-{
      var lineWidgetsByRow = this.session.lineWidgets;
      if (!lineWidgetsByRow) 
         return [];
      var lineWidgets = [];  
      lineWidgetsByRow.forEach(function(w, i) {
         if (w) 
            lineWidgets.push(w);
      });
      return lineWidgets;
   }-*/;
   
   public native final LineWidget getLineWidgetForRow(int row) /*-{
      if (this.session.lineWidgets && this.session.lineWidgets[row])
         return this.session.lineWidgets[row];
      else
         return null;
   }-*/;
   
   public native final boolean hasLineWidgets() /*-{
      return this.session.lineWidgets && this.session.lineWidgets.length > 0;
   }-*/;

   public final void syncLineWidgetHeights()
   {
      JsArray<LineWidget> widgets = getLineWidgets();
      for (int i = 0; i < widgets.length(); i++)
      {
         onWidgetChanged(widgets.get(i));
      }
   }
}
