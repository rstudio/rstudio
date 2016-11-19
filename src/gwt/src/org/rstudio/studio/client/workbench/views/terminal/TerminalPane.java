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
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Holds the contents of the Terminal pane, including the toolbar and
 * the region that holds zero or more Terminal instances.
 */
public class TerminalPane extends WorkbenchPane
                          implements ClickHandler
{
    protected TerminalPane(Commands commands,
                           WorkbenchServerOperations server,
                           SessionInfo sessionInfo)
   {
      super("Terminal");
      commands_ = commands;
      server_ = server;
      sessionInfo_ = sessionInfo;
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

      activeTerminalToolbarButton_ = new TerminalPopupMenu(sessionInfo_,
                                                           commands_);
      
      toolbar.addLeftWidget(activeTerminalToolbarButton_.getToolbarButton());

      return toolbar;
   }
  
   @Override
   public void onSelected()
   {
      super.onSelected();
   }
   
   private final ResizeLayoutPanel host_;
   private TerminalSessionsPanel terminalSessionsPanel_;
   private WorkbenchServerOperations server_;
   private Commands commands_;
   private SessionInfo sessionInfo_;
   private TerminalPopupMenu activeTerminalToolbarButton_;
}