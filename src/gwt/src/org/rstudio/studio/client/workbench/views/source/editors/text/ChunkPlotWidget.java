/*
 * ChunkPlotWidget.java
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
import org.rstudio.studio.client.rmarkdown.model.NotebookPlotMetadata;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkPlotWidget extends Composite
                             implements EditorThemeListener
{
   public ChunkPlotWidget(String url, NotebookPlotMetadata metadata, 
         final Command onRenderComplete, ChunkOutputSize chunkOutputSize)
   {
      plot_ = new Image();
      url_ = url;
      metadata_ = metadata;
      chunkOutputSize_ = chunkOutputSize;

      DOM.sinkEvents(plot_.getElement(), Event.ONLOAD);
      DOM.setEventListener(plot_.getElement(), 
         new EventListener()
         {
            @Override
            public void onBrowserEvent(Event event)
            {
               if (DOM.eventGetType(event) != Event.ONLOAD)
                  return;
               
               // if the image is of fixed size, just clamp its width to the
               // editor surface while preserving its aspect ratio
               if (isFixedSizePlotUrl(plot_.getUrl()))
               {
                  ImageElementEx img = plot_.getElement().cast();
                  img.getStyle().setProperty("height", "auto");
                  img.getStyle().setProperty("maxWidth", "100%");
               }
                  
               plot_.setVisible(true);
               if (onRenderComplete != null)
                  onRenderComplete.execute();
            }
         });
      
      // start loading
      plot_.setUrl(url);
      Widget root = plot_;
      
      if (isFixedSizePlotUrl(url))
      {
         if (chunkOutputSize_ == ChunkOutputSize.Full) {
            HTMLPanel panel = new HTMLPanel("");
            panel.add(plot_);
            host_ = panel;
            root = panel;
         }
         else {
            // if the plot is of fixed size, emit it directly, but make it
            // initially invisible until we get sizing information (as we may 
            // have to downsample)
            plot_.setVisible(false);
         }
      }
      else if (chunkOutputSize_ == ChunkOutputSize.Full)
      {
         HTMLPanel panel = new HTMLPanel("");
         
         panel.getElement().getStyle().setWidth(100, Unit.PCT);
         
         panel.getElement().getStyle().setProperty("display", "-ms-flexbox");
         panel.getElement().getStyle().setProperty("display", "-webkit-flex");
         panel.getElement().getStyle().setProperty("display", "flex");

         plot_.getElement().getStyle().setProperty("display", "-ms-flexbox");
         plot_.getElement().getStyle().setProperty("display", "-webkit-flex");
         plot_.getElement().getStyle().setProperty("display", "flex");

         plot_.getElement().getStyle().setWidth(100, Unit.PCT);
         
         panel.add(plot_);
         host_ = panel;
         root = panel;
      }
      else
      {
         // if we can scale the plot, scale it
         FixedRatioWidget fixedFrame = new FixedRatioWidget(plot_, 
                     ChunkOutputUi.OUTPUT_ASPECT, 
                     ChunkOutputUi.MAX_PLOT_WIDTH);
         host_ = fixedFrame;
         root = fixedFrame;
      }
      
      // if there's metadata to display, further wrap the widget with it
      if (metadata != null && metadata.getConditions().length() > 0)
      {
         // otherwise, group with metadata
         VerticalPanel outer = new VerticalPanel();
         conditions_ = new ChunkConditionBar(metadata.getConditions());
         conditions_.onEditorThemeChanged(ChunkOutputWidget.getEditorColors());
         outer.add(conditions_);
         outer.add(root);
         outer.setHeight("100%");
         outer.setWidth("100%");
         root = outer;
      }
      
      initWidget(root);
   }

   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      if (conditions_ != null)
         conditions_.onEditorThemeChanged(colors);
   }
   
   public Image imageWidget()
   {
      return plot_;
   }
   
   public String plotUrl()
   {
      return url_;
   }
   
   public NotebookPlotMetadata getMetadata()
   {
      return metadata_;
   }

   public static boolean isFixedSizePlotUrl(String url)
   {
      return url.contains("fixed_size=1");
   }
   
   public void updateImageUrl(String plotUrl, String pendingStyle)
   {
      String plotFile = FilePathUtils.friendlyFileName(plotUrl);

      // get the existing URL and strip off the query string 
      String url = plot_.getUrl();
      int idx = url.lastIndexOf('?');
      if (idx > 0)
         url = url.substring(0, idx);
      
      // verify that the plot being refreshed is the same one this widget
      // contains
      if (FilePathUtils.friendlyFileName(url) != plotFile)
         return;
      
      if (host_ != null)
         host_.removeStyleName(pendingStyle);
      
      // the only purpose of this resize counter is to ensure that the
      // plot URL changes when its geometry does (it's not consumed by
      // the server)
      plot_.setUrl(plotUrl + "?resize=" + resizeCounter_++);
   }
   
   private static int resizeCounter_ = 0;
   private final String url_;
   private final Image plot_;
   private final NotebookPlotMetadata metadata_;
   private final ChunkOutputSize chunkOutputSize_;
   private Widget host_;
   private ChunkConditionBar conditions_;
}
