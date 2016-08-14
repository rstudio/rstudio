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

import org.rstudio.core.client.dom.ImageElementEx;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkPlotPage extends ChunkOutputPage
{
   public ChunkPlotPage(final String url, int ordinal)
   {
      super(ordinal);
      if (isFixedSizePlotUrl(url))
      {
         final Image thumbnail = new Image();
         thumbnail_ = new SimplePanel(thumbnail);
         thumbnail_.getElement().getStyle().setTextAlign(TextAlign.CENTER);

         plot_ = new Image();
         plot_.setVisible(false);
         listenForRender(plot_, "auto", "100%", "", new Command() 
         {
            @Override
            public void execute()
            {
               ImageElementEx plot = plot_.getElement().cast();
               ImageElementEx img = thumbnail.getElement().cast();
               if (plot.naturalHeight() < plot.naturalWidth())
               {
                  img.getStyle().setProperty("width", "100%");
                  img.getStyle().setProperty("height", "auto");
               }
               else
               {
                  img.getStyle().setProperty("height", "100%");
                  img.getStyle().setProperty("width", "auto");
               }
               thumbnail.setUrl(url);
            }
         });
         plot_.setUrl(url);
         content_ = plot_;
      }
      else
      {
         // automatically expand non-fixed plots
         thumbnail_ = new FixedRatioWidget(new Image(url), 
                     ChunkOutputUi.OUTPUT_ASPECT, 100);
         plot_ = new Image(url);
         content_ = new FixedRatioWidget(plot_, ChunkOutputUi.OUTPUT_ASPECT, 
               ChunkOutputUi.MAX_PLOT_WIDTH);
      }
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
      return content_;
   }
   
   @Override
   public void onSelected()
   {
      // no action necessary for plots
   }

   public String getPlotUrl()
   {
      return url_;
   }
   
   public Image imageWidget()
   {
      return plot_;
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
   
   public static void listenForRender(final Image plot, final String height, 
         final String maxWidth, final String maxHeight,
         final Command onRenderComplete)
   {
      DOM.sinkEvents(plot.getElement(), Event.ONLOAD);
      DOM.setEventListener(plot.getElement(), 
         new EventListener()
         {
            @Override
            public void onBrowserEvent(Event event)
            {
               if (DOM.eventGetType(event) != Event.ONLOAD)
                  return;
               
               // if the image is of fixed size, just clamp its width to the
               // editor surface while preserving its aspect ratio
               if (ChunkPlotPage.isFixedSizePlotUrl(plot.getUrl()))
               {
                  ImageElementEx img = plot.getElement().cast();
                  img.getStyle().setProperty("height", height);
                  img.getStyle().setProperty("maxWidth", maxWidth);
                  img.getStyle().setProperty("maxHeight", maxHeight);
               }
                  
               plot.setVisible(true);
               if (onRenderComplete != null)
                  onRenderComplete.execute();
            }
         });
   }

   public static boolean isFixedSizePlotUrl(String url)
   {
      return url.contains("fixed_size=1");
   }
   
   private final Widget content_;
   private final Image plot_;
   private final Widget thumbnail_;
   private String url_;
   private static int resizeCounter_ = 0;
}
