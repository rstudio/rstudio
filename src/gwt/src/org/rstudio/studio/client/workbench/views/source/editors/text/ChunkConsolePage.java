/*
 * ChunkConsolePage.java
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
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkConsolePage extends ChunkOutputPage
                              implements ChunkOutputPresenter.Host
{
   public ChunkConsolePage(int ordinal, ChunkOutputSize chunkOutputSize)
   {
      super(ordinal);
      
      chunkOutputSize_ = chunkOutputSize;
      init(new ChunkOutputStream(this));
   }

   public ChunkConsolePage(int ordinal, ChunkOutputStream stream, 
         ChunkOutputSize chunkOutputSize)
   {
      super(ordinal);

      chunkOutputSize_ = chunkOutputSize;
      init(stream);
   }

   @Override
   public Widget thumbnailWidget()
   {
      return thumbnail_;
   }

   @Override
   public Widget contentWidget()
   {
      return content_;
   }
   
   @Override
   public void notifyHeightChanged()
   {
      scrollToBottom();
   }
   
   @Override
   public void onSelected()
   {
      scrollToBottom();
   }

   public void showConsoleText(String text)
   {
      preview_.addText(text);
      stream_.showConsoleText(text);
   }
   
   public void showConsoleError(String error)
   {
      preview_.addText(error);
      stream_.showConsoleError(error);
   }
   
   public void showErrorOutput(UnhandledError err)
   {
      stream_.showErrorOutput(err);
   }
   
   public void showConsoleOutput(JsArray<JsArrayEx> output)
   {
      for (int i = 0; i < output.length(); i++)
      {
         preview_.addText(output.get(i).getString(1));
      }
      stream_.showConsoleOutput(output);
   }
   
   public boolean hasErrors()
   {
      return stream_.hasErrors();
   }
   
   public void completeOutput()
   {
      stream_.completeOutput();
   }
   
   // Private methods ---------------------------------------------------------
   
   private void scrollToBottom()
   {
      panel_.setVerticalScrollPosition(
            panel_.getMaximumVerticalScrollPosition());
   }
   
   private void init(ChunkOutputStream stream)
   {
      preview_ = new ChunkConsolePreview();
      preview_.addText(stream.getAllConsoleText());
      stream_ = stream;
      panel_ = new ScrollPanel();
      panel_.add(stream);

      if (chunkOutputSize_ != ChunkOutputSize.Full) {
         content_ = new FixedRatioWidget(panel_, ChunkOutputUi.OUTPUT_ASPECT, 
               ChunkOutputUi.MAX_PLOT_WIDTH);
      }
      else { 
         panel_.getElement().getStyle().setWidth(100, Unit.PCT);
         content_ = panel_;
      }

      thumbnail_ = new ChunkOutputThumbnail("R Console", "", preview_, 
            ChunkOutputWidget.getEditorColors());
   }

   private ScrollPanel panel_;
   private ChunkOutputStream stream_;
   private Widget thumbnail_;
   private Widget content_;
   private ChunkConsolePreview preview_;
   
   public final static int CONSOLE_INPUT  = 0;
   public final static int CONSOLE_OUTPUT = 1;
   public final static int CONSOLE_ERROR  = 2;

   private final ChunkOutputSize chunkOutputSize_;
}
