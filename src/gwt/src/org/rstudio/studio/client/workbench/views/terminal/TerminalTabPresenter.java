/*
 * TerminalTabPresenter.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.terminal.events.ActivateNamedTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.AddTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.ClearTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateNewTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.RemoveTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.SendToTerminalEvent;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class TerminalTabPresenter extends BasePresenter
                                  implements SendToTerminalEvent.Handler,
                                             ClearTerminalEvent.Handler,
                                             AddTerminalEvent.Handler,
                                             RemoveTerminalEvent.Handler,
                                             ActivateNamedTerminalEvent.Handler,
                                             CreateNewTerminalEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, TerminalTabPresenter> {}

   public interface Display extends WorkbenchView
   {
      /**
       * Ensure terminal pane is visible. Callback to perform actions after pane has
       * been made visible and received onSelected.
       */
      void activateTerminal(Command displaySelected);

      /**
       * Create a new terminal session
       * @param postCreateText text to insert in terminal after created, may be null
       * @param initialDirectory working directory of terminal, may be null to use default
       */
      void createTerminal(String postCreateText, String initialDirectory);

      /**
       * Terminate current terminal.
       */
      void terminateCurrentTerminal();

      /**
       * Attach a list of server-side terminals to the pane.
       * @param procList list of terminals on server
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

      /**
       * Terminate all terminals, whether busy or not. This kills any server-side
       * process and removes it from the list of known processes.
       *
       * @param tabClosing is the terminal tab itself being closed?
       */
      void terminateAllTerminals(boolean tabClosing);

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
       * @param createdByApi terminal just created via rstudioapi?
       */
      void activateNamedTerminal(String caption, boolean createdByApi);

      /**
       * Send current terminal's buffer to a new editor buffer.
       */
      void sendTerminalToEditor();

      /**
       * Send "cd path" to terminal where "path" is RStudio's current working directory
       */
      void goToCurrentDirectory();

      /**
       * Ensure there is at least one terminal.
       */
      void ensureTerminal();
   }

   @Inject
   public TerminalTabPresenter(final Display view,
                               TerminalHelper terminalHelper,
                               UserPrefs uiPrefs)
   {
      super(view);
      view_ = view;
      terminalHelper_ = terminalHelper;
      userPrefs_ = uiPrefs;
   }

   @Handler
   public void onNewTerminal()
   {
      view_.activateTerminal(() -> view_.createTerminal(null, null));
   }

   @Handler
   public void onActivateTerminal()
   {
      // "Move focus to terminal" command; does same thing as clicking the 
      // terminal tab
      view_.activateTerminal(null);
   }

   @Handler
   public void onCloseTerminal()
   {
      view_.terminateCurrentTerminal();
   }

   @Handler
   public void onCloseAllTerminals()
   {
      // Close all terminals but leave the Terminal tab showing
      confirmClose(false, null);
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

   @Handler
   public void onSetTerminalToCurrentDirectory()
   {
      view_.goToCurrentDirectory();
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
   public void onAddTerminal(final AddTerminalEvent event)
   {
      // A new terminal was created server-side via the API. Now add it to the
      // client side terminal list
      view_.addTerminal(event.getProcessInfo(), false /*hasSession*/);
      if (event.getShow())
      {
         // And optionally bring tab forward and select the requested terminal
         view_.activateTerminal(
               () -> view_.activateNamedTerminal(event.getProcessInfo().getCaption(),
                                                 true /*createdByApi*/));
      }
   }

   @Override
   public void onRemoveTerminal(RemoveTerminalEvent event)
   {
      view_.removeTerminal(event.getHandle());
   }

   @Override
   public void onActivateNamedTerminal(final ActivateNamedTerminalEvent event)
   {
      // Request to display the terminal tab and optionally select a specific terminal; if
      // no terminal is specified, then make sure there is an active terminal
      view_.activateTerminal(() ->
      {
         if (StringUtil.isNullOrEmpty(event.getId()))
            view_.ensureTerminal();
         else
            view_.activateNamedTerminal(event.getId(), false /*createdByApi*/);
      });
   }

   @Override
   public void onCreateNewTerminal(final CreateNewTerminalEvent event)
   {
      view_.activateTerminal(() -> view_.createTerminal(null, event.getStartingFolder()));
   }

   public void onRepopulateTerminals(ArrayList<ConsoleProcessInfo> procList)
   {
      view_.repopulateTerminals(procList);
   }

   public void confirmClose(boolean tabClosing, final Command onConfirmed)
   {
      Command command = () ->
      {
         shutDownTerminals(tabClosing);
         if (onConfirmed != null)
            onConfirmed.execute();
      };

      terminalHelper_.warnBusyTerminalBeforeCommand(
            command,
            "Close All Terminals",
            "Are you sure you want to close all terminals? Any running jobs will be stopped",
            userPrefs_.busyDetection().getValue()
      );
   }

   private void shutDownTerminals(boolean tabClosing)
   {
      view_.terminateAllTerminals(tabClosing);
   }

   // Injected ---- 
   private final Display view_;
   private final TerminalHelper terminalHelper_;
   private final UserPrefs userPrefs_;
}
