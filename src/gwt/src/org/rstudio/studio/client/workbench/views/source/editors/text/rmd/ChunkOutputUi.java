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
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
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
   public ChunkOutputUi(String docId, DocDisplay display, ChunkDefinition def)
   {
      display_ = display;
      chunkId_ = def.getChunkId();
      docId_ = docId;
      def_ = def;
      startAnchor_ = display_.createAnchor(
            Position.create(def.getRow(), 0));
      createEndAnchor();

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
            display_.onLineWidgetChanged(lineWidget_);
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
         Debug.devlog("initial state: " + def.getExpansionState());
      outputWidget_.addExpansionStateChangeHandler(
            new ValueChangeHandler<Integer>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> event)
         {
            Debug.devlog("set expansion state to: " + event.getValue());
            def_.setExpansionState(event.getValue());
         }
      });
      
      Element ele = outputWidget_.getElement();
      ele.addClassName(ThemeStyles.INSTANCE.selectableText());
      ele.getStyle().setHeight(MIN_CHUNK_HEIGHT, Unit.PX);
      
      lineWidget_ = LineWidget.create(ChunkDefinition.LINE_WIDGET_TYPE, 
            def.getRow(), ele, def);
      lineWidget_.setFixedWidth(true);
      display_.addLineWidget(lineWidget_);

      attached_ = true;
   }
   
   // Public methods ----------------------------------------------------------

   public int getCurrentRow()
   {
      return lineWidget_.getRow();
   }
   
   public Scope getScope()
   {
      return display_.getCurrentChunk(Position.create(getCurrentRow(), 1));
   }

   public LineWidget getLineWidget()
   {
      return lineWidget_;
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

      startAnchor_.detach();
      endAnchor_.detach();
      
      // note that this triggers an event which causes us to clean up the line
      // widget if we haven't already, so it's important that remove() can't
      // be reentrant.
      display_.removeLineWidget(lineWidget_);
   }
   
   public boolean moving()
   {
      return moving_;
   }
   
   // Private methods ---------------------------------------------------------
   private void syncAnchor()
   {
      int delta = endAnchor_.getRow() - startAnchor_.getRow();
      if (delta == 0)
      {
         // this happens if the line beneath the chunk output is deleted; when
         // it happens, our anchors wind up on the same line, so reset the
         // end anchor
         endAnchor_.detach();
         createEndAnchor();
      }
      else if (delta > 1)
      {
         // this happens if a line is inserted between the chunk and the 
         // output; when it happens, we need to move the output so it remains
         // glued to the end of the chunk.
         
         // mark the chunk as moving (so we don't process this as an actual
         // chunk remove when events are triggered)
         moving_ = true;

         // move the chunk to the start anchor
         endAnchor_.detach();
         display_.removeLineWidget(lineWidget_);
         lineWidget_.setRow(startAnchor_.getRow());
         display_.addLineWidget(lineWidget_);
         createEndAnchor();

         // restore state
         moving_ = false;
      }
   }
   
   private void createEndAnchor()
   {
      endAnchor_ = display_.createAnchor(
            Position.create(startAnchor_.getRow() + 1, 0));
      endAnchor_.addOnChangeHandler(new Command()
      {
         @Override
         public void execute()
         {
            syncAnchor();
         }
      });
   }

   private final Anchor startAnchor_;
   private final LineWidget lineWidget_;
   private final ChunkOutputWidget outputWidget_;
   private final DocDisplay display_;
   private final String chunkId_;
   private final String docId_;
   private final ChunkDefinition def_;

   private Anchor endAnchor_;
   private boolean attached_ = false;
   private boolean moving_ = false;

   public final static int MIN_CHUNK_HEIGHT = 25;
   public final static int CHUNK_COLLAPSED_HEIGHT = 10;
   public final static int MAX_CHUNK_HEIGHT = 650;
}
