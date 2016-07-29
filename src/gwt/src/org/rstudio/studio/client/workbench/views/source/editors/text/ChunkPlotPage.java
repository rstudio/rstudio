/*
 * ChunkPlotPage.java
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

import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ChunkPlotPage implements ChunkOutputPage
{
   public ChunkPlotPage(String url)
   {
      thumbnail_ = new FixedRatioWidget(new Image(url), 
                  ChunkOutputUi.OUTPUT_ASPECT, 100);
      plot_ = new FixedRatioWidget(new Image(url), 
                  ChunkOutputUi.OUTPUT_ASPECT, ChunkOutputUi.MAX_PLOT_WIDTH);
      
   }

   @Override
   public Widget thumbnailWidget()
   {
      return thumbnail_;
   }

   @Override
   public Widget contentWidget()
   {
      return plot_;
   }
   
   private Widget plot_;
   private Widget thumbnail_;
}
