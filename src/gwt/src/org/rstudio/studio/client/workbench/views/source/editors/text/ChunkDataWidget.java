/*
 * ChunkDataWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.SimplePanel;

public class ChunkDataWidget extends SimplePanel
                             implements EditorThemeListener
{
   public ChunkDataWidget(JavaScriptObject data, NotebookFrameMetadata metadata,
                          ChunkOutputSize chunkOutputSize)
   {
      data_ = data;
      metadata_ = metadata;
      chunkOutputSize_ = chunkOutputSize;

      if (chunkOutputSize_ == ChunkOutputSize.Full)
      {
         getElement().getStyle().setWidth(100, Unit.PCT);

         getElement().getStyle().setProperty("display", "-ms-flexbox");
         getElement().getStyle().setProperty("display", "-webkit-flex");
         getElement().getStyle().setProperty("display", "flex");

         getElement().getStyle().setProperty("msFlexGrow", "1");
         getElement().getStyle().setProperty("webkitFlexGrow", "1");
         getElement().getStyle().setProperty("flexGrow", "1");
      }

      initPagedTableOrDelay();
   }

   private void initPagedTableOrDelay()
   {
      if (pagedTableExists()) {
         pagedTable_ = showDataOutputNative(
            data_,
            metadata_.getSummary(),
            getElement(),
            chunkOutputSize_ == ChunkOutputSize.Full);

         onDataOutputChange();
      }
      else {
         Timer t = new Timer() {
           public void run() {
             initPagedTableOrDelay();
           }
         };

         t.schedule(200);
      }
   }

   public static void injectPagedTableResources()
   {
      if (injectStyleElement("rmd_data/pagedtable.css", "pagedtable-css")) {
         ScriptInjector.fromUrl("rmd_data/pagedtable.js").inject();
      }
   }
   
   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      applyDataOutputStyleNative(getElement(), 
            colors.highlight, colors.border);
   }
   
   public void onResize()
   {
      if (pagedTable_ != null)
      {
         resizeDataOutputStyleNative(
            pagedTable_,
            getElement().getOffsetWidth(),
            getElement().getOffsetHeight()
         );
      }
   }
   
   public JavaScriptObject getData()
   {
      return data_;
   }
   
   // Private methods ---------------------------------------------------------

   private void onDataOutputChange()
   {
      EditorThemeListener.Colors colors = ChunkOutputWidget.getEditorColors();
      if (colors != null)
      {
         applyDataOutputStyleNative(getElement(), colors.highlight, colors.border);
      }
   }

   private static final native boolean injectStyleElement(String url, String id) /*-{
      var linkElement = $doc.getElementById(id);
      if (linkElement === null) {
         linkElement = $doc.createElement("link");
         linkElement.setAttribute("id", id);
         linkElement.setAttribute("href", url);
         linkElement.setAttribute("rel", "stylesheet");
         
         $doc.getElementsByTagName("head")[0].appendChild(linkElement);
         return true;
      }

      return false;
   }-*/;

   private final native boolean pagedTableExists() /*-{
      return typeof(PagedTable) != "undefined";
   }-*/;

   private final native JavaScriptObject showDataOutputNative(JavaScriptObject data, 
         JavaScriptObject metadata, Element parent, boolean fullSize) /*-{
      var pagedTable = $doc.createElement("div");
      pagedTable.setAttribute("data-pagedtable", "false");

      if (fullSize) {
         pagedTable.setAttribute("class", "pagedtable-expand");
         pagedTable.style.width = "100%";
      }

      parent.appendChild(pagedTable);

      var pagedTableSource = $doc.createElement("script");
      pagedTableSource.setAttribute("data-pagedtable-source", "");
      pagedTableSource.setAttribute("type", "application/json");

      var originalOptions = JSON.parse(JSON.stringify(data.options));
      if (fullSize) {
         data.options.rows.min = 1;
         data.options.rows.max = null;
         data.options.columns.max = null;
      }
      data.metadata = metadata;

      pagedTableSource.appendChild($doc.createTextNode(JSON.stringify(data)))
      pagedTable.appendChild(pagedTableSource);

      data.options = originalOptions;

      var pagedTableInstance = new PagedTable(pagedTable);

      var chunkWidget = this;
      pagedTableInstance.onChange(function() {
         chunkWidget.@org.rstudio.studio.client.workbench.views.source.editors.text.ChunkDataWidget::onDataOutputChange()();
      });
      
      pagedTableInstance.init();
      
      return pagedTableInstance;
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
   
   private static final native void resizeDataOutputStyleNative(
      JavaScriptObject pagedTable,
      int newWidth,
      int newHeight) /*-{
     pagedTable.resize(newWidth, newHeight);
   }-*/;
   
   private JavaScriptObject pagedTable_ = null;
   private final JavaScriptObject data_;
   private final NotebookFrameMetadata metadata_;
   private final ChunkOutputSize chunkOutputSize_;
}
