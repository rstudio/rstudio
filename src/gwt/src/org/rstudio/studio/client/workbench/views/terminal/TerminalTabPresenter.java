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

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.console.ConsoleProcess.ConsoleProcessFactory;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
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
      
      /**
       * Attach a list of server-side terminals to the pane.
       * @param event list of terminals on server
       */
      void repopulateTerminals(ArrayList<ConsoleProcessInfo> procList);
   }

   @Inject
   public TerminalTabPresenter(final Display view,
                               final Session session,
                               Provider<ConsoleProcessFactory> pConsoleProcessFactory)
   {
      super(view);
      view_ = view;
      session_ = session;
      pConsoleProcessFactory_ = pConsoleProcessFactory;
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
   
   public void onSessionInit(SessionInitEvent sie)
   {
      JsArray<ConsoleProcessInfo> procs =
            session_.getSessionInfo().getConsoleProcesses();
      final ArrayList<ConsoleProcessInfo> procList = new ArrayList<ConsoleProcessInfo>();

      for (int i = 0; i < procs.length(); i++)
      {
         final ConsoleProcessInfo proc = procs.get(i);
         if (proc.isTerminal())
         {
            addTerminalProcInfo(procList, proc);
         }
      }
      view_.repopulateTerminals(procList);
   }

   /**
    * Add process to list of processes, sorted in ascending order by
    * terminal sequence number. If duplicate sequence numbers are
    * encountered, all but the first will have the process killed.
    * 
    * @param terminalProcs (in/out) sorted list of terminal processes
    * @param procInfo process to insert in the list
    */
   private void addTerminalProcInfo(ArrayList<ConsoleProcessInfo> procInfoList,
                                    ConsoleProcessInfo procInfo)
   {
      int newSequence = procInfo.getTerminalSequence();
      if (newSequence < 1)
      {
         Debug.logWarning("Invalid terminal sequence " + newSequence + 
               ", killing unrecognized process");
         pConsoleProcessFactory_.get().reap(procInfo);
         return;
      }

      for (int i = 0; i < procInfoList.size(); i++)
      {
         int currentSequence = procInfoList.get(i).getTerminalSequence();

         if (newSequence == currentSequence)
         {
            Debug.logWarning("Duplicate terminal sequence " + newSequence + 
                  ", killing duplicate process");
            pConsoleProcessFactory_.get().reap(procInfo);
            return;
         }

         if (newSequence < currentSequence)
         {
            procInfoList.add(i, procInfo);
            return;
         }
      }
      procInfoList.add(procInfo);
   }

   // Injected ---- 
   private final Provider<ConsoleProcessFactory> pConsoleProcessFactory_;
   private final Display view_;
   private final Session session_;
}