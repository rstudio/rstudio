/*
 * DebugCommander.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import java.util.ArrayList;

import org.rstudio.core.client.DebugFilePosition;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.debugging.events.BreakpointsSavedEvent;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.debugging.model.DebugState;
import org.rstudio.studio.client.common.debugging.model.TopLevelLineData;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent.NavigationMethod;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputHandler;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.LineData;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Singleton;


// DebugCommander is responsible for managing top-level metadata concerning
// debug sessions (both function and top-level) and for processing the basic
// debug commands (run, step, etc) in the appropriate context. 
@Singleton
public class DebugCommander
         implements ConsoleWriteInputHandler,
                    SessionInitHandler,
                    BreakpointsSavedEvent.Handler,
                    RestartStatusEvent.Handler
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
         BreakpointManager breakpointManager,
         DebuggingServerOperations debugServer,
         WorkbenchContext workbench)
   {
      eventBus_ = eventBus;
      session_ = session;
      debugServer_ = debugServer;
      breakpointManager_ = breakpointManager;
      workbench_ = workbench;
      commands_ = commands;
      
      eventBus_.addHandler(ConsoleWriteInputEvent.TYPE, this);
      eventBus_.addHandler(SessionInitEvent.TYPE, this);
      eventBus_.addHandler(BreakpointsSavedEvent.TYPE, this);
      eventBus_.addHandler(RestartStatusEvent.TYPE, this);
      
      binder.bind(commands, this);
     
      setDebugCommandsEnabled(false);
      commands_.debugBreakpoint().setEnabled(false);
      
      // The callback supplied whenever we execute a portion of a file for
      // debugging. The server's response indicates where execution paused and
      // why.
      debugStepCallback_ = new ServerRequestCallback<TopLevelLineData>()
      {
         @Override
         public void onResponseReceived(TopLevelLineData lineData)
         {
            debugStep_ = lineData.getStep();
            
            // If we're waiting for the user, introduce the debug toolbar
            if (lineData.getState() == TopLevelLineData.STATE_PAUSED)
            {
               setDebugging(true);
            }

            if (lineData.getNeedsBreakpointInjection())
            {
               // If the debugger is paused, inject into the line just 
               // evaluated rather than the current line (since the current line
               // hasn't yet been evaluated)
               LineData bpLineData = lineData.getState() == 
                     TopLevelLineData.STATE_PAUSED ?
                           previousLineData_ :
                           lineData;

               // If there are breakpoints in the range, the breakpoint manager 
               // will emit a BreakpointsSavedEvent, which we'll use as a cue
               // to continue execution.
               boolean foundBreakpoints = 
                     breakpointManager_.injectBreakpointsDuringSource(
                        debugFile_, 
                        bpLineData.getLineNumber(), 
                        bpLineData.getEndLineNumber());
              
               if (lineData.getState() == TopLevelLineData.STATE_INJECTION_SITE)
               {
                  if (foundBreakpoints)
                  {
                     waitingForBreakpointInject_ = true;
                  }
                  else
                  {
                     continueTopLevelDebugSession();
                  }
               }
            }
            previousLineData_ = lineData;
            if (lineData.getState() != TopLevelLineData.STATE_INJECTION_SITE)
            {
               highlightDebugPosition(lineData, lineData.getFinished());
            }
            if (debugMode_ == DebugMode.Function)
            {
               restoreConsolePrompt();
            }
            if (lineData.getFinished())
            {
               leaveDebugMode();
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            // If we hit an error while debugging a function, the most likely 
            // cause is that the user aborted the function (which also aborts 
            // evaluation of the routine we use to manage the top-level debug
            // session). Get the prompt back to trigger re-evaluation of the 
            // context stack. 
            if (debugMode_ == DebugMode.Function)
            {
               restoreConsolePrompt();
            }
         }   
      };
      
   }

   // Command and event handlers ----------------------------------------------

   @Handler
   void onDebugContinue()
   {
      if (debugMode_ == DebugMode.Function)
      {
         eventBus_.fireEvent(new SendToConsoleEvent("c", true, true));
      }
      else if (debugMode_ == DebugMode.TopLevel)
      {
         executeDebugStep(STEP_RUN);
      }
   }
   
   @Handler
   void onDebugStop()
   {
      if (debugMode_ == DebugMode.Function)
      {
         eventBus_.fireEvent(new SendToConsoleEvent("Q", true, true));
         if (topDebugMode_ == DebugMode.TopLevel)
         {
            haltingTopLevelDebug_ = true;
         }
      }
      else if (debugMode_ == DebugMode.TopLevel)
      {
         executeDebugStep(STEP_STOP);
      }      
   }

   @Handler
   void onDebugStep()
   {
      if (debugMode_ == DebugMode.Function)
      {
         eventBus_.fireEvent(new SendToConsoleEvent("n", true, true));
      }
      else if (debugMode_ == DebugMode.TopLevel)
      {
         executeDebugStep(STEP_SINGLE);
      }
   }
   
   @Handler
   void onDebugStepInto()
   {
      if (debugMode_ == DebugMode.Function)
      {
         eventBus_.fireEvent(new SendToConsoleEvent("s", true, true));
      }
      else if (debugMode_ == DebugMode.TopLevel)
      {
         // TODO: what's the right thing here?
      }
   }
   
   @Handler
   void onDebugFinish()
   {
      if (debugMode_ == DebugMode.Function)
      {
         eventBus_.fireEvent(new SendToConsoleEvent("f", true, true));
      }
      else if (debugMode_ == DebugMode.TopLevel)
      {
         // TODO: what's the right thing here?
      }
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
         topDebugMode_ = DebugMode.Normal;
      }
   }

   @Override
   public void onConsoleWriteInput(ConsoleWriteInputEvent event)
   {
      RegExp sourceExp = RegExp.compile("debugSource\\('([^']*)'.*");
      MatchResult fileMatch = sourceExp.exec(event.getInput());
      if (fileMatch == null || fileMatch.getGroupCount() == 0)
      {
         return;
      }     
      String path = FilePathUtils.normalizePath(
            fileMatch.getGroup(1), 
            workbench_.getCurrentWorkingDir().getPath());
      beginTopLevelDebugSession(path);      
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      DebugState debugState = 
            session_.getSessionInfo().getDebugState();
      
      if (debugState.isTopLevelDebug())
      {
         debugFile_ = debugState.getDebugFile();
         debugStep_ = debugState.getDebugStep();
         previousLineData_ = debugState.cast();
         enterDebugMode(DebugMode.TopLevel);
         setDebugging(true);
         highlightDebugPosition((LineData)debugState.cast(), false);
      }
      
      if (!session_.getSessionInfo().getHaveAdvancedStepCommands())
      {
         commands_.debugStepInto().remove();
         commands_.debugFinish().remove();
      }
   }
   
   @Override
   public void onBreakpointsSaved(BreakpointsSavedEvent event)
   {
      if (waitingForBreakpointInject_)
      {
         waitingForBreakpointInject_ = false;
         if (debugStepMode_ == STEP_RUN)
         {
            executeDebugStep(STEP_RESUME);
         }
         else
         {
            executeDebugStep(debugStepMode_);
         }
      }
   }         

   // Public methods ----------------------------------------------------------

   public void continueTopLevelDebugSession()
   {
      executeDebugStep(debugStepMode_);
   }
   
   public void enterDebugMode(DebugMode mode)
   {
      // when entering function debug context, save the current top-level debug
      // mode so we can restore it later 
      if (mode == DebugMode.Function)
      {
         setDebugging(true);
         topDebugMode_ = debugMode_;
      }
      debugMode_ = mode;
   }
   
   public void leaveDebugMode()
   {
      // when leaving function debug context, restore the top-level debug mode
      if (debugMode_ == DebugMode.Function)
      {
         debugMode_ = topDebugMode_;
         
         if (debugMode_ == DebugMode.TopLevel)
         {
            // If the user halted debugging at the function level, then we were
            // waiting for that operation to finish; now halt debugging at the
            // top level as well.
            if (haltingTopLevelDebug_)
            {
               haltingTopLevelDebug_ = false;
               executeDebugStep(STEP_STOP);
            }
            else
            {
               highlightDebugPosition(previousLineData_, false);
            }
         }
         else
         {
            setDebugging(false);
         }
      }
      else
      {
         setDebugging(false);
         debugMode_ = DebugMode.Normal;
         topDebugMode_ = DebugMode.Normal;
         debugFile_ = "";
      }
   }
   
   public DebugMode getDebugMode()
   {
      return debugMode_;
   }
   
   public boolean hasTopLevelDebugSession(String path)
   {
      return (debugMode_ == DebugMode.TopLevel ||
              topDebugMode_ == DebugMode.TopLevel) &&
             debugFile_.equalsIgnoreCase(path);
   }

   // Private methods ---------------------------------------------------------

   private void executeDebugStep(int stepMode)
   {
      debugStepMode_ = stepMode;

      // The user is free to manipulate breakpoints between steps, so we need
      // to fetch the list of breakpoints after every step and pass the updated
      // list to the server.
      ArrayList<Breakpoint> breakpoints = 
            breakpointManager_.getBreakpointsInFile(debugFile_);
      ArrayList<Integer> topBreakLines = new ArrayList<Integer>();
      ArrayList<Integer> functionBreakLines = new ArrayList<Integer>();
      for (Breakpoint breakpoint: breakpoints)
      {
         if (breakpoint.getType() == Breakpoint.TYPE_TOPLEVEL)
         {
            topBreakLines.add(breakpoint.getLineNumber());
         }
         else
         {
            functionBreakLines.add(breakpoint.getLineNumber());
         }
      }
     
      debugServer_.executeDebugSource(
            debugFile_,
            topBreakLines,
            functionBreakLines,
            debugStep_, 
            stepMode, 
            debugStepCallback_);
   }


   private void beginTopLevelDebugSession(String path)
   {
      debugStep_ = 0;
      debugFile_ = path;
      enterDebugMode(DebugMode.TopLevel);
      executeDebugStep(STEP_RUN);
   }
   
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
                                NavigationMethod.DebugEnd :
                                NavigationMethod.DebugStep));
   }

   // Hack: R doesn't always restore the console prompt after a function
   // browser when the browser was invoked during an eval from our tools, so
   // fire an <Enter> to bring it back.
   private void restoreConsolePrompt()
   {
      eventBus_.fireEvent(new SendToConsoleEvent("", true));
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
   
   // These values are understood by the server; if you change them, you'll need
   // to update the server's understanding in SessionBreakpoints.R. 
   private static final int STEP_SINGLE = 0;
   private static final int STEP_RUN = 1;
   private static final int STEP_STOP = 2;
   private static final int STEP_RESUME = 3;

   private final DebuggingServerOperations debugServer_;
   private final ServerRequestCallback<TopLevelLineData> debugStepCallback_;
   private final EventBus eventBus_;
   private final BreakpointManager breakpointManager_;
   private final Session session_;
   private final WorkbenchContext workbench_;
   private final Commands commands_;
   
   private DebugMode debugMode_ = DebugMode.Normal;
   private DebugMode topDebugMode_ = DebugMode.Normal;
   private int debugStep_ = 1;
   private int debugStepMode_ = STEP_SINGLE;
   private boolean waitingForBreakpointInject_ = false;
   private boolean haltingTopLevelDebug_ = false;
   private String debugFile_ = "";
   private LineData previousLineData_ = null;
   private boolean debugging_ = false;
}
