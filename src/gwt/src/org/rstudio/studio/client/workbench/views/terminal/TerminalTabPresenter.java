/*
 * TerminalTabPresenter.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BusyPresenter;
import org.rstudio.studio.client.workbench.views.terminal.events.ActivateNamedTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.AddTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.ClearTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.RemoveTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.SendToTerminalEvent;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class TerminalTabPresenter extends BusyPresenter
                                  implements SendToTerminalEvent.Handler,
                                             ClearTerminalEvent.Handler,
                                             CreateTerminalEvent.Handler,
                                             AddTerminalEvent.Handler,
                                             RemoveTerminalEvent.Handler,
                                             ActivateNamedTerminalEvent.Handler

{
   public interface Binder extends CommandBinder<Commands, TerminalTabPresenter> {}

   public interface Display extends WorkbenchView
   {
      /**
       * Ensure terminal pane is visible.
       */
      void activateTerminal();

      /**
       * Create a new terminal session
       * @param postCreateText text to insert in terminal after created, may be null
       */
      void createTerminal(String postCreateText);

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

      void renameTerminal();
      void clearTerminalScrollbackBuffer(String caption);
      void previousTerminal();
      void nextTerminal();
      void showTerminalInfo();
      void sendToTerminal(String text, boolean setFocus);
      
      /**
       * Send SIGINT to child process of the terminal shell.
       */
      void interruptTerminal();
      
      /**
       * Add a terminal to the list.
       * @param cpi information on the terminal
       * @param hasSession true if a TerminalSession has been created for this terminal
       * caption
       */
      void addTerminal(ConsoleProcessInfo cpi, boolean hasSession);
      
      /**
       * Remove a terminal that was killed via rstudioapi::terminalKill.
       * @param handle terminal to remove
       * caption
       */
      void removeTerminal(String handle);

      /**
       * Activate (display) terminal with given caption. If none specified,
       * do nothing.
       * @param caption
       */
      void activateNamedTerminal(String caption);
      
      /**
       * Send current terminal's buffer to a new editor buffer.
       */
      void sendTerminalToEditor();
   }

   @Inject
   public TerminalTabPresenter(final Display view,
                               TerminalHelper terminalHelper,
                               UIPrefs uiPrefs)
   {
      super(view);
      view_ = view;
      terminalHelper_ = terminalHelper;
      uiPrefs_ = uiPrefs;
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

   @Handler
   public void onClearTerminalScrollbackBuffer()
   {
      view_.clearTerminalScrollbackBuffer(null);
   }

   @Handler
   public void onPreviousTerminal()
   {
      view_.previousTerminal();
   }

   @Handler
   public void onNextTerminal()
   {
      view_.nextTerminal();
   }

   @Handler
   public void onShowTerminalInfo()
   {
      view_.showTerminalInfo();
   }
   
   @Handler
   public void onInterruptTerminal()
   {
      view_.interruptTerminal();
   }
   
   @Handler
   public void onSendTerminalToEditor()
   {
      view_.sendTerminalToEditor();
   }

   @Override
   public void onCreateTerminal(CreateTerminalEvent event)
   {
      onActivateTerminal();
      view_.createTerminal(event.getPostCreateText());
   }

   @Override
   public void onSendToTerminal(SendToTerminalEvent event)
   {
      view_.sendToTerminal(event.getText(), event.getSetFocus());
   }

   @Override
   public void onClearTerminal(ClearTerminalEvent event)
   {
      view_.clearTerminalScrollbackBuffer(event.getId());
   }

   @Override
   public void onAddTerminal(AddTerminalEvent event)
   {
      view_.addTerminal(event.getProcessInfo(), false /*hasSession*/);
      
      if (event.getShow())
      {
         onActivateTerminal();
         view_.activateNamedTerminal(event.getProcessInfo().getCaption());
      }
   }

   @Override
   public void onRemoveTerminal(RemoveTerminalEvent event)
   {
      view_.removeTerminal(event.getHandle());
   }

   @Override
   public void onActivateNamedTerminal(ActivateNamedTerminalEvent event)
   {
      onActivateTerminal();
      view_.activateNamedTerminal(event.getId());
   }

   public void onRepopulateTerminals(ArrayList<ConsoleProcessInfo> procList)
   {
      view_.repopulateTerminals(procList);
   }

   public void confirmClose(final Command onConfirmed)
   {
      final String caption = "Close Terminal(s) ";
      terminalHelper_.warnBusyTerminalBeforeCommand(new Command() {
         @Override
         public void execute()
         {
            shutDownTerminals();
            onConfirmed.execute();
         }
      }, caption, "Are you sure you want to close all terminals? Any running jobs " +
            "will be stopped",
            uiPrefs_.terminalBusyMode().getValue());
   }

   private void shutDownTerminals()
   {
      if (uiPrefs_.showTerminalTab().getValue())
      {
         uiPrefs_.showTerminalTab().setGlobalValue(false);
         uiPrefs_.writeUIPrefs();
      }
      view_.terminateAllTerminals();
   }

   // Injected ---- 
   private final Display view_;
   private final TerminalHelper terminalHelper_;
   private final UIPrefs uiPrefs_;
}