/*
 * ChunkOutputCodeUi.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputSize;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget.Host;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * A host for notebook chunk output for the code editing mode; it wraps a
 * ChunkOutputWidget. It is complemented by ChunkOutputPanmirrorUi, which wraps
 * chunk output in visual editing mode; these classes can both wrap the same
 * output widget, and trade ownership of it via detach/reattach.
 */
public class ChunkOutputCodeUi extends ChunkOutputUi
{
   public ChunkOutputCodeUi(String docId, DocDisplay display, ChunkDefinition def,
         Host lineWidgetHost, ChunkOutputWidget widget)
   {
      super(docId, def, widget);
      display_ = display;

      wrapper_ = new SimplePanel();

      ChunkOutputWidget outputWidget = getOutputWidget();
      outputWidget.setEmbeddedStyle(false);
      wrapper_.add(outputWidget);
      wrapped_ = true;

      lineWidget_ = new PinnedLineWidget(ChunkDefinition.LINE_WIDGET_TYPE, 
            display_, wrapper_, def.getRow(), def, lineWidgetHost);
   }
   
   public ChunkOutputCodeUi(ChunkOutputPanmirrorUi visual, DocDisplay display, 
                            Host lineWidgetHost)
   {
      this(visual.getDocId(), display, visual.getDefinition(), 
            lineWidgetHost, visual.getOutputWidget());
   }

   @Override
   public int getCurrentRow()
   {
      return lineWidget_.getRow();
   }

   @Override
   public Scope getScope()
   {
      return display_.getChunkAtPosition(Position.create(getCurrentRow(), 1));
   }

   @Override
   public void onOutputHeightChanged(ChunkOutputWidget widget,
                                     int outputHeight,
                                     boolean ensureVisible)
   {
      // don't process if we aren't attached 
      if (!attached_)
         return;
      
      // if ensuring visible, also ensure that the associated code is unfolded
      if (ensureVisible)
      {
         Scope scope = getScope();
         if (scope != null)
         {
            display_.unfold(Range.fromPoints(scope.getPreamble(), 
                                             scope.getEnd()));
         }
      }

      int height = 
            widget.getExpansionState() == ChunkOutputWidget.COLLAPSED ?
               CHUNK_COLLAPSED_HEIGHT :
               Math.max(MIN_CHUNK_HEIGHT, outputHeight);

      applyHeight(height);
      display_.onLineWidgetChanged(lineWidget_.getLineWidget());
      
      // if we need to ensure that this output is visible, wait for the event
      // loop to finish (so Ace gets a chance to adjust the line widgets and
      // do a render pass), then make sure the line beneath our widget is 
      // visible
      if (ensureVisible && renderHandlerReg_ == null)
         renderHandlerReg_ = display_.addRenderFinishedHandler(this);
   }

   @Override
   public void onRenderFinished(RenderFinishedEvent event)
   {
      // single-shot ensure visible
      renderHandlerReg_.removeHandler();
      renderHandlerReg_ = null;
      
      ensureVisible();
   }
   
   @Override
   public void remove()
   {
      super.remove();

      // note that this triggers an event which causes us to clean up the line
      // widget if we haven't already, so it's important that remove() can't
      // be reentrant.
      lineWidget_.detach();
      lineWidget_.getLineWidget().getElement().removeFromParent();
   }
   
   @Override
   public void ensureVisible()
   {
      // we want to be sure the user can see the row beneath the output 
      // (this is just a convenient way to determine whether the entire 
      // output is visible)
      int targetRow = Math.min(display_.getRowCount() - 1, getCurrentRow() + 1);
      
      // if the chunk has no visible output yet, just make sure it's not too
      // close to the bottom of the screen
      if (!outputWidget_.isVisible())
      {
         targetRow = Math.min(display_.getRowCount() - 1, targetRow + 1);
      }
      
      // if the chunk and output are already taking up the whole viewport, don't
      // do any auto-scrolling
      if (display_.getLastVisibleRow() <= targetRow && 
          display_.getFirstVisibleRow() >= getScope().getPreamble().getRow())
      {
         return;
      }
      
      // scroll into view if the output is not visible (in either direction)
      if (display_.getLastVisibleRow() < targetRow ||
          display_.getFirstVisibleRow() > getCurrentRow())
      {
         Scope chunk = getScope();
         Rectangle bounds = display_.getPositionBounds(chunk.getPreamble());

         // compute the distance we need to scroll 
         int delta = bounds.getTop() - (display_.getBounds().getTop() + 60);

         // scroll!
         display_.scrollToY(display_.getScrollTop() + delta, 400);
      }
   }

   public LineWidget getLineWidget()
   {
      return lineWidget_.getLineWidget();
   }

   public boolean moving()
   {
      return lineWidget_.moving();
   }

   @Override
   public void reattach()
   {
      if (wrapped_)
         return;
      
      outputWidget_.setEmbeddedStyle(false);
      applyHeight(height_);
      wrapper_.add(outputWidget_);
      wrapped_ = true;
   }

   @Override
   public void detach()
   {
      if (!wrapped_)
         return;

      wrapper_.clear();
      wrapped_ = false;
   }

   @Override
   public ChunkOutputSize getChunkOutputSize()
   {
      return ChunkOutputSize.Default;
   }

   public void applyHeight(int heightPx)
   {
      outputWidget_.getElement().getStyle().setHeight(heightPx, Unit.PX);
      height_ = heightPx;
   }

   private HandlerRegistration renderHandlerReg_ = null;
   private boolean wrapped_;

   private final PinnedLineWidget lineWidget_;
   private final DocDisplay display_;
   private final SimplePanel wrapper_;
}
