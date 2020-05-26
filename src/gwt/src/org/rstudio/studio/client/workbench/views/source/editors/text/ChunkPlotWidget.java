/*
 * ChunkPlotWidget.java
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

import org.rstudio.core.client.dom.ImageElementEx;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.rmarkdown.model.NotebookPlotMetadata;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
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
               {
                  // it seems that the 'onload' event can be fired after the
                  // image has been loaded, but before the browser has actually
                  // sized and rendered the image. defer the 'onRenderComplete()'
                  // action just so the browser gets a chance to render the image
                  Scheduler.get().scheduleDeferred(new ScheduledCommand()
                  {
                     @Override
                     public void execute()
                     {
                        onRenderComplete.execute();
                     }
                  });
               }
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

         panel.getElement().getStyle().setProperty("msFlexGrow", "1");
         panel.getElement().getStyle().setProperty("webkitFlexGrow", "1");
         panel.getElement().getStyle().setProperty("flexGrow", "1");

         panel.getElement().getStyle().setProperty("backgroundImage", "url(\"" + url + "\")");
         panel.getElement().getStyle().setProperty("backgroundSize", "100% 100%");

         plotDiv_ = panel;
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
         HTMLPanel outer = new HTMLPanel("");
         conditions_ = new ChunkConditionBar(metadata.getConditions(), chunkOutputSize_);
         conditions_.onEditorThemeChanged(ChunkOutputWidget.getEditorColors());
         outer.add(conditions_);
         outer.add(root);
         outer.setWidth("100%");
         
         if (chunkOutputSize_ == ChunkOutputSize.Full)
         {
            outer.getElement().getStyle().setProperty("display", "-ms-flexbox");
            outer.getElement().getStyle().setProperty("display", "-webkit-flex");
            outer.getElement().getStyle().setProperty("display", "flex");

            outer.getElement().getStyle().setProperty("msFlexDirection", "column");
            outer.getElement().getStyle().setProperty("webkitFlexDirection", "column");
            outer.getElement().getStyle().setProperty("flexDirection", "column");

            outer.getElement().getStyle().setProperty("msFlexGrow", "1");
            outer.getElement().getStyle().setProperty("webkitFlexGrow", "1");
            outer.getElement().getStyle().setProperty("flexGrow", "1");
         }
         else
         {
            outer.setHeight("100%");
         }
         
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
      String plotUrlRefresh = plotUrl + "?resize=" + resizeCounter_++;

      plot_.setUrl(plotUrlRefresh);

      if (plotDiv_ != null)
         plotDiv_.getElement().getStyle().setProperty("backgroundImage", "url(\"" + plotUrlRefresh + "\")");
   }
   
   private static int resizeCounter_ = 0;
   private final String url_;
   private final Image plot_;
   private HTMLPanel plotDiv_ = null;
   private final NotebookPlotMetadata metadata_;
   private final ChunkOutputSize chunkOutputSize_;
   private Widget host_;
   private ChunkConditionBar conditions_;
}
