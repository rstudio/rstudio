/*
 * ChunkOutputGallery.java
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

import java.util.ArrayList;

import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkOutputGallery extends Composite
                                        implements ChunkOutputPresenter
{

   private static ChunkOutputGalleryUiBinder uiBinder = GWT
         .create(ChunkOutputGalleryUiBinder.class);

   interface ChunkOutputGalleryUiBinder
         extends UiBinder<Widget, ChunkOutputGallery>
   {
   }

   public interface GalleryStyle extends CssResource
   {
      String thumbnail();
      String selected();
   }

  // Public methods ----------------------------------------------------------

   public ChunkOutputGallery(ChunkOutputPresenter.Host host)
   {
      pages_ = new ArrayList<ChunkOutputPage>();
      host_ = host;
      initWidget(uiBinder.createAndBindUi(this));
      content_ = new SimplePanel();
      viewer_.add(new FixedRatioWidget(content_, ChunkOutputUi.OUTPUT_ASPECT, 
            ChunkOutputUi.MAX_PLOT_WIDTH));
   }

   @Override
   public void showConsoleText(String text)
   {
      ensureConsole();
      console_.showConsoleText(text);
   }

   @Override
   public void showConsoleError(String error)
   {
      ensureConsole();
      console_.showConsoleError(error);
   }

   @Override
   public void showConsoleOutput(JsArray<JsArrayEx> output)
   {
      ensureConsole();
      console_.showConsoleOutput(output);
   }

   @Override
   public void showPlotOutput(String url, int ordinal, Command onRenderComplete)
   {
      addPage(new ChunkPlotPage(url));
   }

   @Override
   public void showHtmlOutput(String url, int ordinal, Command onRenderComplete)
   {
      addPage(new ChunkHtmlPage(url, onRenderComplete));
   }

   @Override
   public void showErrorOutput(UnhandledError error)
   {
      ensureConsole();
      console_.showErrorOutput(error);
   }

   @Override
   public void showOrdinalOutput(int ordinal)
   {
      // ordinals are used as placeholders to ensure plots are shown in the
      // correct place relative to other types of output, which isn't a
      // consideration in gallery view
   }

   @Override
   public void setPlotPending(boolean pending, String pendingStyle)
   {
      for (ChunkOutputPage page: pages_)
      {
         if (page instanceof ChunkPlotPage)
         {
            ChunkPlotPage plot = (ChunkPlotPage)page;
            if (pending)
               plot.contentWidget().addStyleName(pendingStyle);
            else
               plot.contentWidget().removeStyleName(pendingStyle);
         }
      }
   }

   @Override
   public void updatePlot(String plotUrl, String pendingStyle)
   {
      for (ChunkOutputPage page: pages_)
      {
         if (page instanceof ChunkPlotPage)
         {
            ChunkPlotPage plot = (ChunkPlotPage)page;
            ChunkPlotPage.updateImageUrl(plot.contentWidget(), 
                  plot.imageWidget(), plotUrl, pendingStyle);
         }
      }
   }

   @Override
   public void clearOutput()
   {
      content_.clear();
      pages_.clear();
      filmstrip_.clear();
   }

   @Override
   public void completeOutput()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean hasOutput()
   {
      return pages_.size() > 0;
   }

   @Override
   public boolean hasPlots()
   {
      for (ChunkOutputPage page: pages_)
      {
         if (page instanceof ChunkPlotPage)
            return true;
      }
      return false;
   }

   @Override
   public void syncEditorColor(String color)
   {
      // TODO Auto-generated method stub
      
   }

   public void addPage(ChunkOutputPage page)
   {
      final int index = pages_.size();
      pages_.add(page);
      Widget thumbnail = page.thumbnailWidget();
      thumbnail.addStyleName(style.thumbnail());
      filmstrip_.add(thumbnail);

      DOM.sinkEvents(thumbnail.getElement(), Event.ONCLICK);
      DOM.setEventListener(thumbnail.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            switch(DOM.eventGetType(evt))
            {
            case Event.ONCLICK:
               setActivePage(index);
               break;
            };
         }
      });
      if (pages_.size() == 1)
         content_.add(page.contentWidget());
      host_.notifyHeightChanged();
   }

   // Private methods ---------------------------------------------------------
   
   private void setActivePage(int idx)
   {
      if (idx >= pages_.size())
         return;
      content_.clear();
      content_.add(pages_.get(idx).contentWidget());
   }
   
   private void ensureConsole()
   {
      if (console_ == null)
      {
         console_ = new ChunkConsolePage();
         addPage(console_);
      }
   }
   
   private final ArrayList<ChunkOutputPage> pages_;
   private final ChunkOutputPresenter.Host host_;

   private ChunkConsolePage console_;
   private SimplePanel content_;
   
   @UiField GalleryStyle style;
   @UiField FlowPanel filmstrip_;
   @UiField HTMLPanel viewer_;
}
