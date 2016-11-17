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

import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.views.BusyPresenter;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateTerminalEvent;

public class TerminalTabPresenter extends BusyPresenter
{
   // TODO (gary) bind commands
   // public interface Binder extends CommandBinder<Commands, TerminalTabPresenter> {}
   // TODO (gary) provide a view to hold the TerminalPanes
   // public interface Display extends WorkbenchView {}
   
   @Inject
   public TerminalTabPresenter( //Display view,
                               Commands commands,
                               WorkbenchServerOperations server,
                               final Session session)
   {
      super(new TerminalPane(commands, server, session.getSessionInfo()));
      pane_ = (TerminalPane) getView();
      // view_ = view;
   }
   
   public void initialize()
   {
   }
   
   public void onCreateTerminal(CreateTerminalEvent event)
   {
      pane_.ensureVisible();
      pane_.bringToFront();
   }

   private final TerminalPane pane_;
   // private final Display view_;
}