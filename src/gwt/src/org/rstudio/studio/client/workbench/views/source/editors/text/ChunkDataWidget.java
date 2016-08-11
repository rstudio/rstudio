/*
 * ChunkDataWidget.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;

public class ChunkDataWidget extends SimplePanel
                             implements EditorThemeListener
{
   public ChunkDataWidget(JavaScriptObject data)
   {
      showDataOutputNative(data, getElement());
      onDataOutputChange();
   }

   public static void injectPagedTableResources()
   {
      injectStyleElement("rmd_data/pagedtable.css", "pagedtable-css");
      ScriptInjector.fromUrl("rmd_data/pagedtable.js").inject();
   }
   
   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      applyDataOutputStyleNative(getElement(), 
            colors.highlight, colors.border);
   }
   
   // Private methods ---------------------------------------------------------

   private void onDataOutputChange()
   {
      EditorThemeListener.Colors colors = ChunkOutputWidget.getEditorColors();
      applyDataOutputStyleNative(getElement(), colors.highlight, colors.border);
   }

   private static final native void injectStyleElement(String url, String id) /*-{
      var linkElement = $doc.getElementById(id);
      if (linkElement === null) {
         linkElement = $doc.createElement("link");
         linkElement.setAttribute("id", id);
         linkElement.setAttribute("href", url);
         linkElement.setAttribute("rel", "stylesheet");
         
         $doc.getElementsByTagName("head")[0].appendChild(linkElement);
      }
   }-*/;

   private final native void showDataOutputNative(JavaScriptObject data, 
         Element parent) /*-{
      var pagedTable = $doc.createElement("div");
      pagedTable.setAttribute("data-pagedtable", "");
      parent.appendChild(pagedTable);

      var pagedTableSource = $doc.createElement("script");
      pagedTableSource.setAttribute("data-pagedtable-source", "");
      pagedTableSource.setAttribute("type", "application/json");
      pagedTableSource.appendChild($doc.createTextNode(JSON.stringify(data)))
      pagedTable.appendChild(pagedTableSource);

      var pagedTableInstance = new PagedTable(pagedTable);

      var chunkWidget = this;
      pagedTableInstance.onChange(function() {
         chunkWidget.@org.rstudio.studio.client.workbench.views.source.editors.text.ChunkDataWidget::onDataOutputChange()();
      });
      
      pagedTableInstance.render();
   }-*/;

   private static final native void applyDataOutputStyleNative(
      Element parent,
      String highlightColor,
      String lowlightColor) /*-{
      
      var allHighlights = parent.querySelectorAll(
         ".pagedtable tr.even," +
         "a.pagedtable-index-current"); 
      for (var idx = 0; idx < allHighlights.length; idx++) {
         allHighlights[idx].style.backgroundColor = highlightColor;
      }
      
      var allBottomBorders = parent.querySelectorAll(".pagedtable th,.pagedtable td");
      for (var idx = 0; idx < allBottomBorders.length; idx++) {
         allBottomBorders[idx].style.borderBottomColor = lowlightColor;
      }
      
      var allTopBorders = parent.querySelectorAll(".pagedtable-not-empty .pagedtable-footer");
      for (var idx = 0; idx < allTopBorders.length; idx++) {
         allTopBorders[idx].style.borderTopColor = lowlightColor;
      }
      
      var allDisabledText = parent.querySelectorAll(".pagedtable-index-nav-disabled");
      for (var idx = 0; idx < allDisabledText.length; idx++) {
         allDisabledText[idx].style.color = lowlightColor;
      }
   }-*/;
}
