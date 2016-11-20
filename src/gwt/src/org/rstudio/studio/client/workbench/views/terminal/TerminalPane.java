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
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * Holds the contents of the Terminal pane, including the toolbar and
 * zero or more Terminal instances.
 */
public class TerminalPane extends WorkbenchPane
                          implements ClickHandler,
                                     TerminalTabPresenter.Display
{
   @Inject
    protected TerminalPane(Commands commands,
                           WorkbenchServerOperations server,
                           Session session)
   {
      super("Terminal");
      commands_ = commands;
      server_ = server;
      session_ = session;
      host_ = new ResizeLayoutPanel();
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      terminalSessionsPanel_ = new TerminalSessionsPanel(server_);
      host_.add(terminalSessionsPanel_);
      return host_;
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
   }
   
   @Override
   public void activateTerminal()
   {
      ensureVisible();
      bringToFront();
   }
   
   @Override
   public void ensureInitialSession()
   {
      activateTerminal();
      createTerminalSession(); // TODO (gary) only if zero sessions
   }
   
   @Override
   public void createTerminalSession()
   {
   }
   
   private final ResizeLayoutPanel host_;
   private TerminalSessionsPanel terminalSessionsPanel_;
   private WorkbenchServerOperations server_;
   private Commands commands_;
   private Session session_;
   private TerminalPopupMenu activeTerminalToolbarButton_;
}