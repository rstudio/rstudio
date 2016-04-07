/*
 * ChunkOutputUi.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;

public class ChunkOutputUi
             implements ChunkOutputHost,
                        RenderFinishedEvent.Handler
{
   public ChunkOutputUi(String docId, DocDisplay display, ChunkDefinition def,
         PinnedLineWidget.Host lineWidgetHost)
   {
      display_ = display;
      chunkId_ = def.getChunkId();
      docId_ = docId;
      def_ = def;

      outputWidget_ = new ChunkOutputWidget(def.getChunkId(), 
            def.getExpansionState(), this);
      
      // sync the widget's expanded/collapsed state to the underlying chunk
      // definition (which is persisted)
      outputWidget_.addExpansionStateChangeHandler(
            new ValueChangeHandler<Integer>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> event)
         {
            def_.setExpansionState(event.getValue());
         }
      });
      
      Element ele = outputWidget_.getElement();
      ele.addClassName(ThemeStyles.INSTANCE.selectableText());
      
      // make the widget initially invisible (until it gets some output)
      ele.getStyle().setHeight(0, Unit.PX);
      outputWidget_.setVisible(false);
      
      lineWidget_ = new PinnedLineWidget(ChunkDefinition.LINE_WIDGET_TYPE, 
            display_, outputWidget_, def.getRow(), def, lineWidgetHost);
      
      attached_ = true;
   }
   
   // Public methods ----------------------------------------------------------

   public int getCurrentRow()
   {
      return lineWidget_.getRow();
   }
   
   public String getChunkId()
   {
      return def_.getChunkId();
   }
   
   public Scope getScope()
   {
      return display_.getCurrentChunk(Position.create(getCurrentRow(), 1));
   }

   public LineWidget getLineWidget()
   {
      return lineWidget_.getLineWidget();
   }

   public ChunkOutputWidget getOutputWidget()
   {
      return outputWidget_;
   }
   
   public void remove()
   {
      if (!attached_)
         return;
      attached_ = false;

      // note that this triggers an event which causes us to clean up the line
      // widget if we haven't already, so it's important that remove() can't
      // be reentrant.
      lineWidget_.detach();
   }
   
   public boolean moving()
   {
      return lineWidget_.moving();
   }
   
   public void ensureVisible()
   {
      // we want to be sure the user can see the row beneath the output 
      // (this is just a convenient way to determine whether the entire 
      // output is visible)
      int targetRow = getCurrentRow() + 1;
      
      // if the chunk has no visible output yet, just make sure it's not too
      // close to the bottom of the screen
      if (!outputWidget_.isVisible())
      {
         targetRow = Math.min(display_.getRowCount(), targetRow + 1);
      }
   
      if (display_.getLastVisibleRow() < targetRow)
      {
         Scope chunk = getScope();
         Rectangle bounds = display_.getPositionBounds(chunk.getPreamble());
         display_.scrollToY(display_.getScrollTop() + 
               (bounds.getTop() - (display_.getBounds().getTop() + 60)),
               400);
      }
   }

   @Override
   public void onOutputHeightChanged(int outputHeight, boolean ensureVisible)
   {
      int height = 
            outputWidget_.getExpansionState() == ChunkOutputWidget.COLLAPSED ?
               CHUNK_COLLAPSED_HEIGHT :
               Math.max(MIN_CHUNK_HEIGHT, 
                 Math.min(outputHeight, MAX_CHUNK_HEIGHT));
      outputWidget_.getElement().getStyle().setHeight(height, Unit.PX);
      display_.onLineWidgetChanged(lineWidget_.getLineWidget());
      
      // if we need to ensure that this output is visible, wait for the event
      // loop to finish (so Ace gets a chance to adjust the line widgets and
      // do a render pass), then make sure the line beneath our widget is 
      // visible
      if (ensureVisible)
         renderHandlerReg_ = display_.addRenderFinishedHandler(this);
   }

   @Override
   public void onOutputRemoved()
   {
      RStudioGinjector.INSTANCE.getEventBus().fireEvent(
              new ChunkChangeEvent(docId_, chunkId_, 0, 
                                   ChunkChangeEvent.CHANGE_REMOVE));
   }

   @Override
   public void onRenderFinished(RenderFinishedEvent event)
   {
      // single-shot ensure visible
      renderHandlerReg_.removeHandler();
      ensureVisible();
   }

   // Private methods ---------------------------------------------------------

   private final PinnedLineWidget lineWidget_;
   private final ChunkOutputWidget outputWidget_;
   private final DocDisplay display_;
   private final String chunkId_;
   private final String docId_;
   private final ChunkDefinition def_;

   private boolean attached_ = false;
   private HandlerRegistration renderHandlerReg_;

   public final static int MIN_CHUNK_HEIGHT = 25;
   public final static int CHUNK_COLLAPSED_HEIGHT = 10;
   public final static int MAX_CHUNK_HEIGHT = 650;
}
