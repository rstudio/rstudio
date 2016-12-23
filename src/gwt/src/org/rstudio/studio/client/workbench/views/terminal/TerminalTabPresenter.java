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

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.console.ConsoleProcess.ConsoleProcessFactory;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BusyPresenter;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalStatusEvent;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class TerminalTabPresenter extends BusyPresenter
                                  implements TerminalStatusEvent.Handler

{
   public interface Binder extends CommandBinder<Commands, TerminalTabPresenter> {}

   public interface Display extends WorkbenchView
   {
      /**
       * Ensure terminal pane is visible.
       */
      void activateTerminal();

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

      /**
       * @return Are any terminals busy (a terminal in canonical mode is 
       * considered busy, but a full-screen app such as vim, which is not in 
       * canonical mode, is not). All "busy" terminals will also be "active"
       * terminals.
       */
      boolean busyTerminals();

      /**
       * @return Are any terminals active (a terminal whose shell has any
       * subprocesses is considered active and should not silently killed).
       */
      boolean activeTerminals();

      /**
       * Terminate all terminals, whether busy or not. This kills any server-side
       * process and removes it from the list of known processes. This should
       * only be invoked when the terminal tab itself is being unloaded.
       */
      void terminateAllTerminals();

      /**
       * Rename (change caption) of current terminal.
       */
      void renameTerminal();
   }

   @Inject
   public TerminalTabPresenter(final Display view,
                               final Session session,
                               GlobalDisplay globalDisplay,
                               UIPrefs uiPrefs,
                               Provider<ConsoleProcessFactory> pConsoleProcessFactory,
                               EventBus events)
   {
      super(view);
      view_ = view;
      session_ = session;
      globalDisplay_ = globalDisplay;
      uiPrefs_ = uiPrefs;
      pConsoleProcessFactory_ = pConsoleProcessFactory;
      events.addHandler(TerminalStatusEvent.TYPE, this);
   }

   @Handler
   public void onActivateTerminal()
   {
      if (!uiPrefs_.showTerminalTab().getValue())
      {
         uiPrefs_.showTerminalTab().setGlobalValue(true);
         uiPrefs_.writeUIPrefs();
      }
      view_.activateTerminal();
   }

   @Handler
   public void onCloseTerminal()
   {
      view_.terminateCurrentTerminal();
   }

   @Handler
   public void onRenameTerminal()
   {
      view_.renameTerminal();
   }

   @Override
   public void onTerminalStatus(TerminalStatusEvent event)
   {
      setIsBusy(view_.busyTerminals());
   }

   public void initialize()
   {
   }

   public void onCreateTerminal(CreateTerminalEvent event)
   {
      onActivateTerminal();
      view_.createTerminal();
      setIsBusy(true);
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
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            setIsBusy(view_.busyTerminals());
         }
      });
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
         pConsoleProcessFactory_.get().interruptAndReap(procInfo.getHandle());
         return;
      }

      for (int i = 0; i < procInfoList.size(); i++)
      {
         int currentSequence = procInfoList.get(i).getTerminalSequence();

         if (newSequence == currentSequence)
         {
            Debug.logWarning("Duplicate terminal sequence " + newSequence + 
                  ", killing duplicate process");
            pConsoleProcessFactory_.get().interruptAndReap(procInfo.getHandle());
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

   public void confirmClose(final Command onConfirmed)
   {
      if (view_.busyTerminals())
      {
         globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION, 
               "Close Terminal(s) ", 
               "Are you sure you want to close all terminals? Any running jobs " +
                     "will be stopped.", false, 
                     new Operation()
         {
            @Override
            public void execute()
            {
               shutDownTerminals();
               onConfirmed.execute();
            }
         }, null, null, "Close Terminals", "Cancel", true);
      }
      else
      {
         shutDownTerminals();
         onConfirmed.execute(); 
      }
   }

   private void shutDownTerminals()
   {
      if (uiPrefs_.showTerminalTab().getValue())
      {
         uiPrefs_.showTerminalTab().setGlobalValue(false);
         uiPrefs_.writeUIPrefs();
      }
      view_.terminateAllTerminals();
      setIsBusy(false);
   }

   // Injected ---- 
   private final Provider<ConsoleProcessFactory> pConsoleProcessFactory_;
   private final Display view_;
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final UIPrefs uiPrefs_;
}