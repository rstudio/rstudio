/*
 * TerminalTabPresenter.java
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

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BusyPresenter;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateTerminalEvent;

public class TerminalTabPresenter extends BusyPresenter
{
   public interface Binder extends CommandBinder<Commands, TerminalTabPresenter> {}

   public interface Display extends WorkbenchView
   {
      /**
       * Ensure terminal pane is visible.
       */
      void activateTerminal();
      
      /**
       *  Ensure terminal pane has at least one session loaded.
       */
      void ensureTerminal();
      
      /**
       * Create a new terminal session.
       */
      void createTerminal();
      
      /**
       * Terminate current terminal.
       */
      void terminateCurrentTerminal();
   }

   @Inject
   public TerminalTabPresenter(Display view)
   {
      super(view);
      view_ = view;
   }
  
   @Handler
   public void onActivateTerminal()
   {
      view_.activateTerminal();
   }
  
   @Handler
   public void onCloseTerminal()
   {
      view_.terminateCurrentTerminal();
   }
   
   public void initialize()
   {
   }
   
   public void onCreateTerminal(CreateTerminalEvent event)
   {
      view_.activateTerminal();
      view_.createTerminal();
   }
   
   // Injected ---- 
   private final Display view_;
}