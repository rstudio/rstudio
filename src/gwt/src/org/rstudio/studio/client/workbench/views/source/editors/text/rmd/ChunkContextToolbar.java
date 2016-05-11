/*
 * ChunkContextToolbar.java
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

import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ChunkContextToolbar extends Composite
{

   private static ChunkContextToolbarUiBinder uiBinder = GWT
         .create(ChunkContextToolbarUiBinder.class);

   interface ChunkContextToolbarUiBinder
         extends UiBinder<Widget, ChunkContextToolbar>
   {
   }
   
   public interface Host
   {
      void runPreviousChunks();
      void runChunk();
      void showOptions(int x, int y);
      void interruptChunk();
      void dequeueChunk();
   }
   
   public final static ChunkContextResources RES = 
         GWT.create(ChunkContextResources.class);

   public ChunkContextToolbar(Host host, boolean dark, boolean runPrevious, 
         boolean run)
   {
      host_ = host;
      state_ = STATE_RESTING;
      initWidget(uiBinder.createAndBindUi(this));
      
      initOptions(dark);

      initRunPrevious(dark);
      setRunPrevious(runPrevious);
      
      initRun();
      setRun(run);
   }

   // Public methods ----------------------------------------------------------

   public void setState(int state)
   {
      switch(state)
      {
      case STATE_RESTING:
         run_.setResource(RES.runChunk());
         break;
      case STATE_QUEUED:
         run_.setResource(RES.runChunkPending());
         break;
      case STATE_EXECUTING:
         run_.setResource(RES.interruptChunk());
         break;
      }
      state_ = state;
   }
   
   public void setRunPrevious(boolean visible)
   {
      runPrevious_.setVisible(visible);
   }
   
   public void setRun(boolean visible)
   {
      run_.setVisible(visible);
      runPrevious_.getElement().getStyle().setMarginRight(visible ? 5 : 33, 
            Unit.PX);
   }
   
   // Private methods ---------------------------------------------------------
   
   private void initOptions(boolean dark)
   {
      options_.setResource(dark ? RES.chunkOptionsDark() :
                                  RES.chunkOptionsLight());
      
      DOM.sinkEvents(options_.getElement(), Event.ONCLICK);
      DOM.setEventListener(options_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONCLICK)
            {
               host_.showOptions(event.getClientX(), event.getClientY());
            }
         }
      });
   }
   
   private void initRun()
   {
      setState(state_);
      run_.setTitle(RStudioGinjector.INSTANCE.getCommands()
                    .executeCurrentChunk().getMenuLabel(false));
      DOM.sinkEvents(run_.getElement(), Event.ONCLICK);
      DOM.setEventListener(run_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONCLICK)
            {
               switch(state_)
               {
               case STATE_RESTING:
                  host_.runChunk();
                  break;
               case STATE_QUEUED:
                  host_.dequeueChunk();
                  break;
               case STATE_EXECUTING:
                  host_.interruptChunk();
                  break;
               }
            }
         }
      });
   }
   
   private void initRunPrevious(boolean dark)
   {
      runPrevious_.setTitle(RStudioGinjector.INSTANCE.getCommands()
                            .executePreviousChunks().getMenuLabel(false));
      runPrevious_.setResource(dark ? RES.runPreviousChunksDark() :
                                      RES.runPreviousChunksLight());
      DOM.sinkEvents(runPrevious_.getElement(), Event.ONCLICK);
      DOM.setEventListener(runPrevious_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONCLICK)
            {
               host_.runPreviousChunks();
            }
         }
      });
   }
   
   @UiField Image options_;
   @UiField Image runPrevious_;
   @UiField Image run_;
   
   private final Host host_;
   private int state_;
   
   public final static int STATE_QUEUED    = 0;
   public final static int STATE_EXECUTING = 1;
   public final static int STATE_RESTING   = 2;

   public final static String LINE_WIDGET_TYPE = "ChunkToolbar";
}
