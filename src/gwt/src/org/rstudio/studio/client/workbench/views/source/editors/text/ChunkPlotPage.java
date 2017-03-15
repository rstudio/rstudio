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
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.rmarkdown.model.NotebookPlotMetadata;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ChunkPlotPage extends ChunkOutputPage
                           implements EditorThemeListener
{
   public ChunkPlotPage(final String url, NotebookPlotMetadata metadata, 
         int ordinal, final Command onRenderComplete, ChunkOutputSize chunkOutputSize)
   {
      super(ordinal);
      
      thumbnail_ = new HTMLPanel("");
      
      if (ChunkPlotWidget.isFixedSizePlotUrl(url))
      {
         final Image thumbnail = new Image();
         thumbnail_.add(thumbnail);
         thumbnail_.getElement().getStyle().setTextAlign(TextAlign.CENTER);

         plot_ = new ChunkPlotWidget(url, metadata, new Command() 
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
               onRenderComplete.execute();
            }
         }, chunkOutputSize);
      }
      else
      {
         // automatically expand non-fixed plots
         thumbnail_.add(new FixedRatioWidget(new Image(url), 
                     ChunkOutputUi.OUTPUT_ASPECT, 100));
         plot_ = new ChunkPlotWidget(url, metadata, onRenderComplete, chunkOutputSize);
      }
      
      // look for messages or warnings in metadata
      boolean hasMessages = false;
      boolean hasWarnings = false;
      if (metadata != null)
      {
         for (int i = 0; i < metadata.getConditions().length(); i++)
         {
            int condition = metadata.getConditions().get(i).getInt(0);
            if (condition == ChunkConditionBar.CONDITION_MESSAGE)
               hasMessages = true;
            else if (condition == ChunkConditionBar.CONDITION_WARNING)
               hasWarnings = true;
         }
      }
      
      if (hasMessages || hasWarnings)
      {
         Image condImage = new Image();
         if (hasWarnings)
            condImage.setResource(new ImageResource2x(ThemeResources.INSTANCE.warningSmall2x()));
         else if (hasMessages)
            condImage.setResource(new ImageResource2x(ThemeResources.INSTANCE.infoSmall2x()));
         condImage.setWidth("8px");
         condImage.setHeight("7px");
         Style style = condImage.getElement().getStyle();
         style.setPosition(Position.ABSOLUTE);
         style.setBottom(5, Unit.PX);
         style.setRight(5, Unit.PX);
         thumbnail_.add(condImage);
      }
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
   
   @Override
   public void onSelected()
   {
      // no action necessary for plots
   }

   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      if (conditions_ != null)
         conditions_.onEditorThemeChanged(colors);
   }
   
   public void updateImageUrl(String url, String pendingStyle)
   {
      plot_.updateImageUrl(url, pendingStyle);
   }

   public String plotUrl()
   {
      return plot_.plotUrl();
   }
   
   public Image imageWidget()
   {
      return plot_.imageWidget();
   }
   
   private final ChunkPlotWidget plot_;
   private final HTMLPanel thumbnail_;
   private ChunkConditionBar conditions_;
}
