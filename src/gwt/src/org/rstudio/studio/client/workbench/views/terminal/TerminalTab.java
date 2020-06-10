/*
 * TerminalTab.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.console.ConsoleProcess.ConsoleProcessFactory;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.terminal.events.ActivateNamedTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.AddTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.ClearTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateNewTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.RemoveTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.SendToTerminalEvent;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class TerminalTab extends DelayLoadWorkbenchTab<TerminalTabPresenter>
{
   public interface Binder extends CommandBinder<Commands, Shim> {}

   public abstract static class Shim
      extends DelayLoadTabShim<TerminalTabPresenter, TerminalTab>
      implements SendToTerminalEvent.Handler,
                 ClearTerminalEvent.Handler,
                 AddTerminalEvent.Handler,
                 RemoveTerminalEvent.Handler,
                 ActivateNamedTerminalEvent.Handler,
                 CreateNewTerminalEvent.Handler
   {
      @Handler
      public abstract void onNewTerminal();

      @Handler
      public abstract void onActivateTerminal();

      @Handler
      public abstract void onCloseTerminal();

      @Handler
      public abstract void onCloseAllTerminals();

      @Handler
      public abstract void onRenameTerminal();

      @Handler
      public abstract void onClearTerminalScrollbackBuffer();

      @Handler
      public abstract void onPreviousTerminal();

      @Handler
      public abstract void onNextTerminal();

      @Handler
      public abstract void onShowTerminalInfo();

      @Handler
      public abstract void onInterruptTerminal();

      @Handler
      public abstract void onSendTerminalToEditor();

      @Handler
      public abstract void onSetTerminalToCurrentDirectory();

      /**
       * Attach a list of server-side terminals to the pane.
       * @param procList list of terminals on server
       */
      abstract void onRepopulateTerminals(ArrayList<ConsoleProcessInfo> procList);

      abstract void confirmClose(boolean tabClosing, Command onConfirmed);
   }

   @Inject
   public TerminalTab(Shim shim,
                      EventBus events,
                      Commands commands,
                      Binder binder,
                      Provider<ConsoleProcessFactory> pConsoleProcessFactory,
                      final Session session)
   {
      super("Terminal", shim);
      shim_ = shim;
      pConsoleProcessFactory_ = pConsoleProcessFactory;

      binder.bind(commands, shim_);
      events.addHandler(SendToTerminalEvent.TYPE, shim_);
      events.addHandler(ClearTerminalEvent.TYPE, shim_);
      events.addHandler(AddTerminalEvent.TYPE, shim_);
      events.addHandler(RemoveTerminalEvent.TYPE, shim_);
      events.addHandler(ActivateNamedTerminalEvent.TYPE, shim_);
      events.addHandler(CreateNewTerminalEvent.TYPE, shim_);

      events.addHandler(SessionInitEvent.TYPE, sie ->
      {
         JsArray<ConsoleProcessInfo> procs =
               session.getSessionInfo().getConsoleProcesses();
         final ArrayList<ConsoleProcessInfo> procList = new ArrayList<>();

         for (int i = 0; i < procs.length(); i++)
         {
            final ConsoleProcessInfo proc = procs.get(i);
            if (proc.isTerminal())
            {
               addTerminalProcInfo(procList, proc);
            }
         }
         if (!procList.isEmpty())
            shim_.onRepopulateTerminals(procList);
      });
   }

   @Override
   public boolean closeable()
   {
      return true;
   }

   @Override
   public void confirmClose(Command onConfirmed)
   {
      // closing the entire Terminal pane
      shim_.confirmClose(true, onConfirmed);
   }

   /**
    * Add process to list of processes, sorted in ascending order by
    * terminal sequence number. If duplicate sequence numbers are
    * encountered, all but the first will have the process killed.
    *
    * @param procInfoList (in/out) sorted list of terminal processes
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

   private final Shim shim_;

   private final Provider<ConsoleProcessFactory> pConsoleProcessFactory_;
}
