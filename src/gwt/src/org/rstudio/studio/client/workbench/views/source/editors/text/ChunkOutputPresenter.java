/*
 * ChunkOutputPresenter.java
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

import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookHtmlMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookPlotMetadata;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.IsWidget;

public interface ChunkOutputPresenter extends IsWidget, EditorThemeListener
{
   public interface Host
   {
      void notifyHeightChanged();
   }

   // show real-time console output; invoked only interactively 
   void showConsoleText(String text);
   void showConsoleError(String error);

   // show stored and/or real time objects emitted during plot execution
   void showConsoleOutput(JsArray<JsArrayEx> output);
   void showPlotOutput(String url, NotebookPlotMetadata metadata, int ordinal, 
         Command onRenderComplete);
   void showHtmlOutput(String url, NotebookHtmlMetadata metadata, int ordinal, 
         Command onRenderComplete);
   void showErrorOutput(UnhandledError error);
   void showOrdinalOutput(int ordinal);
   void showDataOutput(JavaScriptObject data, NotebookFrameMetadata metadata,
         int ordinal);
   
   // show that plots will be redrawn, or update a particular plot
   void setPlotPending(boolean pending, String pendingStyle);
   void updatePlot(String plotUrl, String pendingStyle);

   // Handles html that can be appended to end of the chunk.
   void showCallbackHtml(String htmlOutput);

   // clear output or indicate that interactive output (or playback) is complete
   void clearOutput();
   void completeOutput();
   
   // query state
   boolean hasOutput();
   boolean hasPlots();
   boolean hasErrors();
   boolean hasHtmlWidgets();
   
   // notify of size changes
   void onResize();
}

