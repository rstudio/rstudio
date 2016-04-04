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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;

public class ChunkOutputUi
{
   public ChunkOutputUi(String docId, DocDisplay display, ChunkDefinition def,
         PinnedLineWidget.Host lineWidgetHost)
   {
      display_ = display;
      chunkId_ = def.getChunkId();
      docId_ = docId;
      def_ = def;

      outputWidget_ = new ChunkOutputWidget(def.getChunkId(), 
            def.getExpansionState(),
            new CommandWithArg<Integer>()
      {
         @Override
         public void execute(Integer arg)
         {
            int height = 
                  outputWidget_.getExpansionState() == ChunkOutputWidget.COLLAPSED ?
                     CHUNK_COLLAPSED_HEIGHT :
                     Math.max(MIN_CHUNK_HEIGHT, 
                       Math.min(arg.intValue(), MAX_CHUNK_HEIGHT));
            outputWidget_.getElement().getStyle().setHeight(height, Unit.PX);
            display_.onLineWidgetChanged(lineWidget_.getLineWidget());
         }
      },
      new Command()
      {
         @Override
         public void execute()
         {
            RStudioGinjector.INSTANCE.getEventBus().fireEvent(
                  new ChunkChangeEvent(docId_, chunkId_, 0, 
                                       ChunkChangeEvent.CHANGE_REMOVE));
         }
      });
      
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
      ele.getStyle().setHeight(MIN_CHUNK_HEIGHT, Unit.PX);
      
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
   
   // Private methods ---------------------------------------------------------

   private final PinnedLineWidget lineWidget_;
   private final ChunkOutputWidget outputWidget_;
   private final DocDisplay display_;
   private final String chunkId_;
   private final String docId_;
   private final ChunkDefinition def_;

   private boolean attached_ = false;

   public final static int MIN_CHUNK_HEIGHT = 25;
   public final static int CHUNK_COLLAPSED_HEIGHT = 10;
   public final static int MAX_CHUNK_HEIGHT = 650;
}
