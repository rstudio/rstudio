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
import org.rstudio.studio.client.common.FilePathUtils;
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
      url_ = url;
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
   
   public String getPlotUrl()
   {
      return url_;
   }
   
   public Image imageWidget()
   {
      if (plot_ == null || !(plot_.getWidget() instanceof Image))
         return null;
      return (Image)plot_.getWidget();
   }
   
   public static void updateImageUrl(Widget host, Image plot, String plotUrl, 
         String pendingStyle)
   {
      String plotFile = FilePathUtils.friendlyFileName(plotUrl);

      // get the existing URL and strip off the query string 
      String url = plot.getUrl();
      int idx = url.lastIndexOf('?');
      if (idx > 0)
         url = url.substring(0, idx);
      
      // verify that the plot being refreshed is the same one this widget
      // contains
      if (FilePathUtils.friendlyFileName(url) != plotFile)
         return;
      
      host.removeStyleName(pendingStyle);
      
      // the only purpose of this resize counter is to ensure that the
      // plot URL changes when its geometry does (it's not consumed by
      // the server)
      plot.setUrl(plotUrl + "?resize=" + resizeCounter_++);
   }
   
   private final FixedRatioWidget plot_;
   private final Widget thumbnail_;
   private String url_;
   private static int resizeCounter_ = 0;
}
