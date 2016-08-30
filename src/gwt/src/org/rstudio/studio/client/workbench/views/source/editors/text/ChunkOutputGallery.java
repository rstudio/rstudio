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

import org.rstudio.core.client.ColorUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookHtmlMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookPlotMetadata;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.KeyCodes;
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
      String expand();
      String content();
   }

  // Public methods ----------------------------------------------------------

   public ChunkOutputGallery(
      ChunkOutputPresenter.Host host,
      ChunkOutputSize chunkOutputSize)
   {
      pages_ = new ArrayList<ChunkOutputPage>();
      host_ = host;
      chunkOutputSize_ = chunkOutputSize;
      initWidget(uiBinder.createAndBindUi(this));
      content_ = new SimplePanel();
      viewer_.add(content_);

      if (chunkOutputSize_ == ChunkOutputSize.Full)
      {
         addStyleName(style.expand());
         content_.addStyleName(style.content());
      }
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
      // ignore if no actual text needs to be emitted (this prevents us from
      // creating a spurious console page)
      int i = 0;
      for (; i < output.length(); i++)
      {
         if (!StringUtil.isNullOrEmpty(output.get(i).getString(1)))
            break;
      }
      if (i == output.length())
         return;
      
      ensureConsole();
      console_.showConsoleOutput(output);
   }

   @Override
   public void showPlotOutput(String url, NotebookPlotMetadata metadata,
         int ordinal, Command onRenderComplete)
   {
      addPage(new ChunkPlotPage(url, metadata, ordinal, onRenderComplete, chunkOutputSize_));
   }

   @Override
   public void showHtmlOutput(String url, NotebookHtmlMetadata metadata, 
         int ordinal, Command onRenderComplete)
   {
      addPage(new ChunkHtmlPage(url, metadata, ordinal, onRenderComplete, chunkOutputSize_));
   }

   @Override
   public void showErrorOutput(UnhandledError error)
   {
      ensureConsole();
      console_.showErrorOutput(error);
      
      // switch back to the console so the user can see the error
      for (int i = 0; i < pages_.size(); i++)
      {
         if (pages_.get(i) == console_)
         {
            setActivePage(i);
            break;
         }
      }
   }

   @Override
   public void showOrdinalOutput(int ordinal)
   {
      // ordinals are used as placeholders to ensure plots are shown in the
      // correct place relative to other types of output, which isn't a
      // consideration in gallery view
   }

   @Override
   public void showDataOutput(JavaScriptObject data, 
         NotebookFrameMetadata metadata, int ordinal)
   {
      addPage(new ChunkDataPage(data, metadata, ordinal, chunkOutputSize_));
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
            plot.updateImageUrl(plotUrl, pendingStyle);
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
      if (console_ != null)
         console_.completeOutput();
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
   public boolean hasErrors()
   {
      if (console_ != null)
         return console_.hasErrors();
      return false;
   }

   @Override
   public void onEditorThemeChanged(EditorThemeListener.Colors colors)
   {
      for (Widget thumbnail: filmstrip_)
      {
         syncThumbnailColor(thumbnail, colors);
      }
      for (ChunkOutputPage page: pages_)
      {
         if (page instanceof EditorThemeListener)
            ((EditorThemeListener)page).onEditorThemeChanged(colors);
      }
   }

   public void addPage(ChunkOutputPage page)
   {
      int idx = pages_.size();

      // look for out of place inserts
      if (page.ordinal() < maxOrdinal_)
      {
         for (int i = 0; i < pages_.size() - 1; i++)
         {
            if (page.ordinal() > pages_.get(i).ordinal() &&
                page.ordinal() < pages_.get(i+1).ordinal())
            {
               idx = i+1;

               // if we picked the currently active page, move it out of the
               // way
               if (activePage_ == idx)
                  activePage_++;
               break;
            }
         }
      }
      maxOrdinal_ = Math.max(maxOrdinal_, page.ordinal());
      
      pages_.add(idx, page);
      Widget thumbnail = page.thumbnailWidget();
      thumbnail.getElement().setTabIndex(0);
      thumbnail.addStyleName(style.thumbnail());

      // apply editor color to thumbnail before 
      syncThumbnailColor(thumbnail, ChunkOutputWidget.getEditorColors());
      filmstrip_.insert(thumbnail, idx);
      
      // lock to this console if we don't have one already
      if (page instanceof ChunkConsolePage && console_ == null)
         console_ = (ChunkConsolePage)page;

      final int ordinal = page.ordinal();
      DOM.sinkEvents(thumbnail.getElement(), Event.ONCLICK | Event.ONKEYDOWN);
      DOM.setEventListener(thumbnail.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            switch(DOM.eventGetType(evt))
            {
            case Event.ONCLICK:
               // convert ordinal back to index (index can change with
               // out-of-order insertions)
               for (int i = 0; i < pages_.size(); i++)
               {
                  if (pages_.get(i).ordinal() == ordinal)
                  {
                     setActivePage(i);
                     break;
                  }
               }
               break;
            case Event.ONKEYDOWN:
               int dir = 0;
               if (evt.getKeyCode() == KeyCodes.KEY_LEFT)
                  dir = -1;
               if (evt.getKeyCode() == KeyCodes.KEY_RIGHT)
                  dir = 1;
               if (dir != 0)
                  navigateActivePage(dir);
               break;
            };
         }
      });
      
      // show this page if it's the first one, or if we don't have any errors
      // and we're adding a last page
      if (idx == 0 || 
          ((idx == pages_.size() - 1) && !hasErrors()))
         setActivePage(idx);

      host_.notifyHeightChanged();
   }

   @Override
   public void onResize()
   {
      for (ChunkOutputPage page: pages_)
      {
         if (page instanceof ChunkDataPage)
         {
            ((ChunkDataPage)page).onResize();
         }
      }
   }

   // Private methods ---------------------------------------------------------
   
   private void navigateActivePage(int delta)
   {
      // add with wraparound
      int idx = activePage_ + delta;
      if (idx >= pages_.size())
         idx = 0;
      if (idx < 0)
         idx = pages_.size() - 1;
      
      setActivePage(idx);
   }
   
   private void setActivePage(int idx)
   {
      // ignore if out of bounds or no-op
      if (idx >= pages_.size())
         return;
      if (idx == activePage_)
         return;

      content_.clear();
      content_.add(pages_.get(idx).contentWidget());
      
      // remove the selection styling from the previously active page (if any)
      // and add it to this page
      if (activePage_ >= 0)
         pages_.get(activePage_).thumbnailWidget().removeStyleName(
               style.selected());
      pages_.get(idx).thumbnailWidget().addStyleName(style.selected());
      pages_.get(idx).onSelected();
      activePage_ = idx;
      
      // this page may have a different height than its predecessor
      host_.notifyHeightChanged();
      
      if (pages_.get(idx) instanceof ChunkDataPage)
      {
         ((ChunkDataPage)pages_.get(idx)).onResize();
      }
   }
   
   private void ensureConsole()
   {
      if (console_ == null)
      {
         console_ = new ChunkConsolePage();
         addPage(console_);
      }
   }
   
   private static void syncThumbnailColor(Widget thumbnail, 
         EditorThemeListener.Colors colors)
   {
      // might happen if we aren't initialized yet
      if (colors == null || thumbnail == null)
         return;
      
      // create a border color by making the foreground color slightly
      // translucent
      ColorUtil.RGBColor fore = ColorUtil.RGBColor.fromCss(colors.foreground);
      ColorUtil.RGBColor border = new ColorUtil.RGBColor(
            fore.red(), fore.green(), fore.blue(), 0.5);
      
      // apply border color from editor
      thumbnail.getElement().getStyle().setBorderColor(border.asRgb());
      if (thumbnail instanceof EditorThemeListener)
      {
         ((EditorThemeListener)thumbnail).onEditorThemeChanged(colors);
      }
   }
   
   private final ArrayList<ChunkOutputPage> pages_;
   private final ChunkOutputPresenter.Host host_;
   private final ChunkOutputSize chunkOutputSize_;

   private ChunkConsolePage console_;
   private SimplePanel content_;
   private int activePage_ = -1;
   private int maxOrdinal_ = 0;
   
   @UiField GalleryStyle style;
   @UiField FlowPanel filmstrip_;
   @UiField HTMLPanel viewer_;
}
