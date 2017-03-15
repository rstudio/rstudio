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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
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
      void switchChunk(String chunkType);
   }
   
   public final static ChunkContextResources RES = 
         GWT.create(ChunkContextResources.class);

   public ChunkContextToolbar(Host host, boolean dark, boolean runPrevious, 
         String engine)
   {
      host_ = host;
      state_ = STATE_RESTING;
      initWidget(uiBinder.createAndBindUi(this));
      
      initOptions(dark);

      initRunPrevious(dark);
      setRunPrevious(runPrevious);
      
      initRun();
   }

   // Public methods ----------------------------------------------------------

   public void setState(int state)
   {
      switch(state)
      {
      case STATE_RESTING:
         run_.setResource(new ImageResource2x(RES.runChunk2x()));
         break;
      case STATE_QUEUED:
         run_.setResource(new ImageResource2x(RES.runChunkPending2x()));
         break;
      case STATE_EXECUTING:
         run_.setResource(new ImageResource2x(RES.interruptChunk2x()));
         break;
      }
      state_ = state;
   }
   
   public void setRunPrevious(boolean visible)
   {
      runPrevious_.setVisible(visible);
   }
   
   public void setEngine(String engine)
   {
      chunkTypeLabel_.setText(engine);
   }
   
   // Private methods ---------------------------------------------------------
   
   private void initOptions(boolean dark)
   {
      options_.setResource(new ImageResource2x(
         dark ? RES.chunkOptionsDark2x() :
         RES.chunkOptionsLight2x()));
      
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
      runPrevious_.setResource(new ImageResource2x(
         dark ? RES.runPreviousChunksDark2x() :
         RES.runPreviousChunksLight2x()));
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
   
   private MenuItem createMenuItemForType(final AppCommand command, final String chunkType)
   {
      SafeHtml menuHTML = new SafeHtmlBuilder().appendHtmlConstant(
         command.getMenuHTML(false)).toSafeHtml();
      
      MenuItem menuItem = new MenuItem(
         menuHTML,
         new Command()
         {
            public void execute()
            {
               host_.switchChunk(chunkType);
            }
         });
      
      return menuItem;
   }

   @SuppressWarnings("unused")
   private void initChangeChunkEngine(String engine)
   {
      chunkTypeLabel_.setText(engine);
      DOM.sinkEvents(chunkTypePanel_.getElement(), Event.ONCLICK);
      DOM.setEventListener(chunkTypePanel_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) == Event.ONCLICK)
            {
               Commands commands = RStudioGinjector.INSTANCE.getCommands();
               String engineLabel = chunkTypeLabel_.getText();
               
               final ToolbarPopupMenu switchChunksMenu = new ToolbarPopupMenu();
               
               if (engineLabel != "r") {
                  switchChunksMenu.addItem(createMenuItemForType(
                        commands.switchToChunkR(), "r"));
                  switchChunksMenu.addSeparator();
               }
               
               if (!BrowseCap.isWindowsDesktop() && engineLabel != "bash") {
                  switchChunksMenu.addItem(createMenuItemForType(
                        commands.switchToChunkBash(), "bash"));
               }

               if (engineLabel != "python") {
                  switchChunksMenu.addItem(createMenuItemForType(
                        commands.switchToChunkPython(), "python"));
               }
               
               if (engineLabel != "rcpp") {
                  switchChunksMenu.addItem(createMenuItemForType(
                        commands.switchToChunkRCPP(), "rcpp"));
               }
               
               if (engineLabel != "sql") {
                  switchChunksMenu.addItem(createMenuItemForType(
                        commands.switchToChunkSQL(), "sql"));
               }
               
               if (engineLabel != "stan") {
                  switchChunksMenu.addItem(createMenuItemForType(
                        commands.switchToChunkStan(), "stan"));
               }
               
               switchChunksMenu.setPopupPositionAndShow(new PositionCallback() 
               {
                  @Override
                  public void setPosition(int offsetWidth, 
                                          int offsetHeight)
                  {
                     switchChunksMenu.setPopupPosition(
                        chunkTypePanel_.getAbsoluteLeft() +
                        chunkTypePanel_.getOffsetWidth() -
                        offsetWidth + 15, 
                        chunkTypePanel_.getAbsoluteTop() + 
                        chunkTypePanel_.getOffsetHeight());
                  } 
               });
            }
         }
      });
   }
   
   @UiField Image options_;
   @UiField Image runPrevious_;
   @UiField Image run_;
   @UiField Label chunkTypeLabel_;
   @UiField HTMLPanel chunkTypePanel_;

   private final Host host_;
   private int state_;
   
   public final static int STATE_QUEUED    = 0;
   public final static int STATE_EXECUTING = 1;
   public final static int STATE_RESTING   = 2;

   public final static String LINE_WIDGET_TYPE = "ChunkToolbar";
}
