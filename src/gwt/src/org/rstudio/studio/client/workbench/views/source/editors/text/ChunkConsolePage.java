/*
 * ChunkConsolePage.java
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

import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkConsolePage implements ChunkOutputPage,
                                         ChunkOutputPresenter.Host
{
   public ChunkConsolePage()
   {
      init(new ChunkOutputStream(this));
   }

   public ChunkConsolePage(ChunkOutputStream stream)
   {
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
      // in paged mode we have a fixed height
   }
   
   public void showConsoleText(String text)
   {
      stream_.showConsoleText(text);
   }
   
   public void showConsoleError(String error)
   {
      stream_.showConsoleError(error);
   }
   
   public void showErrorOutput(UnhandledError err)
   {
      stream_.showErrorOutput(err);
   }
   
   public void showConsoleOutput(JsArray<JsArrayEx> output)
   {
      stream_.showConsoleOutput(output);
   }
   
   // Private methods ---------------------------------------------------------
   
   private void init(ChunkOutputStream stream)
   {
      stream_ = stream;
      ScrollPanel panel = new ScrollPanel();
      panel.add(stream);
      content_ = new FixedRatioWidget(panel, ChunkOutputUi.OUTPUT_ASPECT, 
            ChunkOutputUi.MAX_PLOT_WIDTH);
      thumbnail_ = new ChunkOutputThumbnail("R Console", "", null);
   }

   private ChunkOutputStream stream_;
   private Widget thumbnail_;
   private FixedRatioWidget content_;
   
   public final static int CONSOLE_INPUT  = 0;
   public final static int CONSOLE_OUTPUT = 1;
   public final static int CONSOLE_ERROR  = 2;
}
