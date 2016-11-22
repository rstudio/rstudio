/*
 * TerminalPane.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.shell.ShellSecureInput;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * Holds the contents of the Terminal pane, including the toolbar and
 * zero or more Terminal instances.
 */
public class TerminalPane extends WorkbenchPane
                          implements ClickHandler,
                                     TerminalTabPresenter.Display,
                                     TerminalSessionStartedEvent.Handler,
                                     TerminalSessionStoppedEvent.Handler
{
   @Inject
    protected TerminalPane(Commands commands,
                           Session session,
                           EventBus events)
   {
      super("Terminal");
      commands_ = commands;
      session_ = session;
      events.addHandler(TerminalSessionStartedEvent.TYPE, this);
      events.addHandler(TerminalSessionStoppedEvent.TYPE, this);
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      terminalSessionsPanel_ = new DeckLayoutPanel();
      return terminalSessionsPanel_;
   }

   @Override
   public void onClick(ClickEvent event)
   {
      // TODO (gary) implement
      
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();

      activeTerminalToolbarButton_ = new TerminalPopupMenu(session_.getSessionInfo(),
                                                           commands_);
      
      toolbar.addLeftWidget(activeTerminalToolbarButton_.getToolbarButton());

      return toolbar;
   }
  
   @Override
   public void onSelected()
   {
      super.onSelected();
      activateTerminal();
      ensureTerminal();
   }
   
   @Override
   public void activateTerminal()
   {
      ensureVisible();
      bringToFront();
   }
   
   @Override
   public void ensureTerminal()
   {
      if (getTerminalCount() == 0)
      {
         createTerminal();
      }
   }
   
   @Override
   public void createTerminal()
   {
      if (secureInput_ == null)
      {
         secureInput_ = new ShellSecureInput();  
      }
      
      TerminalSession newSession = new TerminalSession(secureInput_);
      newSession.connect();
   }

   @Override
   public void onTerminalSessionStarted(TerminalSessionStartedEvent event)
   {
      terminalSessionsPanel_.add(event.getTerminalWidget());
      terminalSessionsPanel_.showWidget(event.getTerminalWidget());
      
      // TODO (gary) update dropdown
   } 
   
   @Override
   public void onTerminalSessionStopped(TerminalSessionStoppedEvent event)
   {
      Widget currentTerminal = event.getTerminalWidget();
      int currentIndex = terminalSessionsPanel_.getWidgetIndex(currentTerminal);
      if (currentIndex > 0)
      {
         terminalSessionsPanel_.showWidget(currentIndex - 1);
         // TODO (gary) set focus on the widget
      }
      terminalSessionsPanel_.remove(currentTerminal);
      
      // TODO (gary) update dropdown
   }

   public int getTerminalCount()
   {
      return terminalSessionsPanel_.getWidgetCount();
   }

   private DeckLayoutPanel terminalSessionsPanel_;
   private Commands commands_;
   private Session session_;
   private TerminalPopupMenu activeTerminalToolbarButton_;
   private ShellSecureInput secureInput_;
}