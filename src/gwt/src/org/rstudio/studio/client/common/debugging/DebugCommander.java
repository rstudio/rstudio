/*
 * DebugCommander.java
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

package org.rstudio.studio.client.common.debugging;

import org.rstudio.core.client.DebugFilePosition;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.ApplicationInterrupt;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.LineData;

import com.google.inject.Inject;
import com.google.inject.Singleton;


// DebugCommander is responsible for managing top-level metadata concerning
// debug sessions (both function and top-level) and for processing the basic
// debug commands (run, step, etc) in the appropriate context.
@Singleton
public class DebugCommander
         implements SessionInitEvent.Handler,
                    RestartStatusEvent.Handler,
                    BusyEvent.Handler
{
   public interface Binder
      extends CommandBinder<Commands, DebugCommander> {}

   public enum DebugMode
   {
      Normal,
      Function,
      TopLevel
   }

   @Inject
   public DebugCommander(
         Binder binder,
         Commands commands,
         EventBus eventBus,
         Session session,
         ApplicationInterrupt interrupt)
   {
      eventBus_ = eventBus;
      session_ = session;
      commands_ = commands;
      interrupt_ = interrupt;

      eventBus_.addHandler(SessionInitEvent.TYPE, this);
      eventBus_.addHandler(RestartStatusEvent.TYPE, this);
      eventBus_.addHandler(BusyEvent.TYPE, this);

      binder.bind(commands, this);

      setDebugCommandsEnabled(false);
      commands_.debugBreakpoint().setEnabled(false);
   }

   // Command and event handlers ----------------------------------------------

   @Handler
   void onDebugContinue()
   {
      eventBus_.fireEvent(new SendToConsoleEvent(
            CONTINUE_COMMAND, true, true));
   }

   @Handler
   void onDebugStop()
   {
      // If R is busy when a debug stop is requested, interrupt it and wait
      // for the interrupt to complete before killing the debugger.
      if (busy_)
      {
         interrupt_.interruptR(new ApplicationInterrupt.InterruptHandler()
         {
            @Override
            public void onInterruptFinished()
            {
               stopDebugging();
            }
         });
      }
      else
      {
         stopDebugging();
      }
   }

   @Handler
   void onDebugStep()
   {
      eventBus_.fireEvent(new SendToConsoleEvent(NEXT_COMMAND, true, true));
   }

   @Handler
   void onDebugStepInto()
   {
      eventBus_.fireEvent(new SendToConsoleEvent(STEP_INTO_COMMAND, true, true));
   }

   @Handler
   void onDebugFinish()
   {
      eventBus_.fireEvent(new SendToConsoleEvent(FINISH_COMMAND, true, true));
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED)
      {
         // Restarting R cleans up the state we use to persist information about
         // the debug session on the server, so we need to kill the client's
         // debug session when this happens
         if (debugMode_ == DebugMode.TopLevel)
         {
            highlightDebugPosition(previousLineData_, true);
            leaveDebugMode();
         }
      }
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      if (!session_.getSessionInfo().getHaveAdvancedStepCommands())
      {
         commands_.debugStepInto().remove();
         commands_.debugFinish().remove();
      }
   }

   @Override
   public void onBusy(BusyEvent event)
   {
      busy_ = event.isBusy();
   }

   // Public methods ----------------------------------------------------------

   public void enterDebugMode(DebugMode mode)
   {
      // when entering function debug context, save the current top-level debug
      // mode so we can restore it later
      if (mode == DebugMode.Function)
      {
         setDebugging(true);
      }
      setAdvancedCommandsVisible(mode == DebugMode.Function);
      debugMode_ = mode;
   }

   public void leaveDebugMode()
   {
      setDebugging(false);
      debugMode_ = DebugMode.Normal;
      debugFile_ = "";
   }

   public DebugMode getDebugMode()
   {
      return debugMode_;
   }

   // Private methods ---------------------------------------------------------

   private void highlightDebugPosition(LineData lineData, boolean finished)
   {
      FileSystemItem sourceFile = FileSystemItem.createFile(debugFile_);
      DebugFilePosition position = DebugFilePosition.create(
            lineData.getLineNumber(),
            lineData.getEndLineNumber(),
            lineData.getCharacterNumber(),
            lineData.getEndCharacterNumber());
      eventBus_.fireEvent(new OpenSourceFileEvent(sourceFile,
                             (FilePosition) position.cast(),
                             FileTypeRegistry.R,
                             finished ?
                                NavigationMethods.DEBUG_END :
                                NavigationMethods.DEBUG_STEP));
   }

   private void setDebugging(boolean debugging)
   {
      if (debugging_ != debugging)
      {
         debugging_ = debugging;
         setDebugCommandsEnabled(debugging_);
         eventBus_.fireEvent(new DebugModeChangedEvent(debugging_));
      }
   }

   private void setDebugCommandsEnabled(boolean enabled)
   {
      commands_.debugContinue().setEnabled(enabled);
      commands_.debugStep().setEnabled(enabled);
      commands_.debugStop().setEnabled(enabled);
      commands_.debugStepInto().setEnabled(enabled);
      commands_.debugFinish().setEnabled(enabled);
   }

   private void setAdvancedCommandsVisible(boolean visible)
   {
      commands_.debugFinish().setVisible(visible);
      commands_.debugStepInto().setVisible(visible);
   }

   private void stopDebugging()
   {
      eventBus_.fireEvent(new SendToConsoleEvent(STOP_COMMAND, true, true));
   }

   public static final String STOP_COMMAND = "Q";
   public static final String NEXT_COMMAND = "n";
   public static final String CONTINUE_COMMAND = "c";
   public static final String STEP_INTO_COMMAND = "s";
   public static final String FINISH_COMMAND = "f";

   private final EventBus eventBus_;
   private final Session session_;
   private final Commands commands_;
   private final ApplicationInterrupt interrupt_;

   private DebugMode debugMode_ = DebugMode.Normal;
   private String debugFile_ = "";
   private LineData previousLineData_ = null;
   private boolean debugging_ = false;
   private boolean busy_ = false;
}
