/*
 * ChunkOutputUi.java
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
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputSize;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
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
      this(docId, display, def, lineWidgetHost, null);
   }

   public ChunkOutputUi(String docId, DocDisplay display, ChunkDefinition def,
          PinnedLineWidget.Host lineWidgetHost, ChunkOutputWidget widget)
   {
      display_ = display;
      chunkId_ = def.getChunkId();
      docId_ = docId;
      def_ = def;
      boolean hasOutput = widget != null;
      if (widget == null) 
      {
         widget = new ChunkOutputWidget(docId_, def.getChunkId(), 
               def.getOptions(), def.getExpansionState(), true, this, 
               ChunkOutputSize.Default);
      }
      else
      {
         widget.setHost(this);
      }

      outputWidget_ = widget;
      // label_ should only be set by this function
      setChunkLabel(def.getChunkLabel());
      
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
      
      // if we didn't start with output, make the widget initially invisible
      // (until it gets some output)
      if (!hasOutput)
      {
         ele.getStyle().setHeight(0, Unit.PX);
         outputWidget_.setVisible(false);
      }
      
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
   
   public String getChunkLabel()
   {
      return label_;
   }

   public void setChunkLabel(String label)
   {
      label_ = label;
      if (outputWidget_ != null)
         outputWidget_.setClassId(label);
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
   
   public RmdChunkOptions getOptions()
   {
      return def_.getOptions();
   }
   
   public void setOptions(RmdChunkOptions options)
   {
      def_.setOptions(options);
      outputWidget_.setOptions(options);
      setChunkLabel(def_.getChunkLabel());
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
      lineWidget_.getLineWidget().getElement().removeFromParent();
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

      widget.getElement().getStyle().setHeight(height, Unit.PX);
      display_.onLineWidgetChanged(lineWidget_.getLineWidget());
      
      // if we need to ensure that this output is visible, wait for the event
      // loop to finish (so Ace gets a chance to adjust the line widgets and
      // do a render pass), then make sure the line beneath our widget is 
      // visible
      if (ensureVisible && renderHandlerReg_ == null)
         renderHandlerReg_ = display_.addRenderFinishedHandler(this);
   }

   @Override
   public void onOutputRemoved(ChunkOutputWidget widget)
   {
      RStudioGinjector.INSTANCE.getEventBus().fireEvent(
              new ChunkChangeEvent(docId_, chunkId_, "", 0, 
                                   ChunkChangeEvent.CHANGE_REMOVE));
   }

   @Override
   public void onRenderFinished(RenderFinishedEvent event)
   {
      // single-shot ensure visible
      renderHandlerReg_.removeHandler();
      renderHandlerReg_ = null;
      
      ensureVisible();
   }
   
   public boolean hasErrors()
   {
      return outputWidget_.hasErrors();
   }

   // Private methods ---------------------------------------------------------

   private final PinnedLineWidget lineWidget_;
   private final ChunkOutputWidget outputWidget_;
   private final DocDisplay display_;
   private final String chunkId_;
   private final String docId_;
   private final ChunkDefinition def_;

   private String label_;

   private boolean attached_ = false;
   private HandlerRegistration renderHandlerReg_ = null;

   public final static int MIN_CHUNK_HEIGHT = 25;
   public final static int CHUNK_COLLAPSED_HEIGHT = 15;
   public final static int MAX_CHUNK_HEIGHT = 650;
   
   public final static int MIN_PLOT_WIDTH = 400;
   public final static int MAX_PLOT_WIDTH = 700;
   public final static int MAX_HTMLWIDGET_WIDTH = 800;
   
   public final static double OUTPUT_ASPECT = 1.618;
}
