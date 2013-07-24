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
import org.rstudio.studio.client.common.debugging.events.BreakpointsSavedEvent;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.debugging.model.TopLevelLineData;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent.NavigationMethod;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputHandler;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent.DebugMode;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DebugCommander
   implements ConsoleWriteInputHandler
{
   public interface Binder
      extends CommandBinder<Commands, DebugCommander> {}

   @Inject
   public DebugCommander(
         Binder binder,
         Commands commands,
         EventBus eventBus,
         BreakpointManager breakpointManager,
         DebuggingServerOperations debugServer)
   {
      eventBus_ = eventBus;
      debugServer_ = debugServer;
      breakpointManager_ = breakpointManager;
      
      eventBus_.addHandler(ConsoleWriteInputEvent.TYPE, this);      
      
      binder.bind(commands, this);

      debugStepCallback_ = new ServerRequestCallback<TopLevelLineData>()
      {
         @Override
         public void onResponseReceived(TopLevelLineData lineData)
         {
            debugStep_ = lineData.getStep();
            if (lineData.getNeedsBreakpointInjection())
            {
               // If the server is paused for breakpoint injection, inject into
               // the current line; otherwise, inject into the line we just 
               // evaluated
               TopLevelLineData bpLineData = lineData.getState() == 
                     TopLevelLineData.STATE_INJECTION_SITE ?
                           lineData :
                           previousLineData_;

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
            if (lineData.getState() != TopLevelLineData.STATE_INJECTION_SITE)
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
                                      lineData.getFinished() ?
                                            NavigationMethod.DebugEnd :
                                            NavigationMethod.DebugStep));
            }
            if (lineData.getFinished())
            {
               leaveDebugMode();
            }
            previousLineData_ = lineData;
         }
         
         @Override
         public void onError(ServerError error)
         {
            leaveDebugMode();
         }   
      };
      
      eventBus_.addHandler(
            BreakpointsSavedEvent.TYPE, 
            new BreakpointsSavedEvent.Handler()
      {
         @Override
         public void onBreakpointsSaved(BreakpointsSavedEvent event)
         {
            if (waitingForBreakpointInject_)
            {
               waitingForBreakpointInject_ = false;
               continueTopLevelDebugSession();
            }
         }         
      });

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
         
         // Abandon top-level debug mode, too--"Stop" exits all debug contexts
         // simultaneously
         topDebugMode_ = DebugMode.Normal;
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
   
   @Override
   public void onConsoleWriteInput(ConsoleWriteInputEvent event)
   {
      RegExp sourceExp = RegExp.compile("source.for.debug\\('([^']*)'.*");
      MatchResult fileMatch = sourceExp.exec(event.getInput());
      if (fileMatch == null || fileMatch.getGroupCount() == 0)
      {
         return;
      }      
      beginTopLevelDebugSession(fileMatch.getGroup(1));      
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
         topDebugMode_ = debugMode_;
      }
      debugMode_ = mode;
      eventBus_.fireEvent(new DebugModeChangedEvent(debugMode_));
   }
   
   public void leaveDebugMode()
   {
      // when leaving function debug context, restore the top-level debug mode
      if (debugMode_ == DebugMode.Function)
      {
         eventBus_.fireEvent(new DebugModeChangedEvent(topDebugMode_));
         debugMode_ = topDebugMode_;
      }
      else
      {
         eventBus_.fireEvent(new DebugModeChangedEvent(DebugMode.Normal));
         debugMode_ = DebugMode.Normal;
         topDebugMode_ = DebugMode.Normal;
      }
   }
   
   public DebugMode getDebugMode()
   {
      return debugMode_;
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

   private void beginTopLevelDebugSession(
         String filename, 
         boolean hasTopLevelBreakpoints)
   {
      debugStep_ = 1;
      debugFile_ = filename;
      if (hasTopLevelBreakpoints)
      {
         enterDebugMode(DebugMode.TopLevel);
      }
      executeDebugStep(STEP_RUN);
   }

   private void beginTopLevelDebugSession(String fileName)
   {
      // Initiate the debug session on the server
      // See if any of the breakpoints are top-level (if they are, we 
      // need to show the top-level debug toolbar)
      ArrayList<Breakpoint> breakpoints = 
            breakpointManager_.getBreakpointsInFile(fileName);
      boolean hasTopLevelBreakpoints = false;
      for (Breakpoint breakpoint: breakpoints)
      {
         if (breakpoint.getType() == Breakpoint.TYPE_TOPLEVEL)
         {
            hasTopLevelBreakpoints = true;
            break;
         }
      }            

      beginTopLevelDebugSession(fileName, hasTopLevelBreakpoints);
   }
   
   // These values are understood by the server; if you change them, you'll need
   // to update the server's understanding in SessionBreakpoints.R. 
   private static final int STEP_SINGLE = 0;
   private static final int STEP_RUN = 1;
   private static final int STEP_STOP = 2;

   private final DebuggingServerOperations debugServer_;
   private final ServerRequestCallback<TopLevelLineData> debugStepCallback_;
   private final EventBus eventBus_;
   private final BreakpointManager breakpointManager_;
   
   private DebugMode debugMode_ = DebugMode.Normal;
   private DebugMode topDebugMode_ = DebugMode.Normal;
   private int debugStep_ = 1;
   private int debugStepMode_ = STEP_SINGLE;
   private boolean waitingForBreakpointInject_ = false;
   private String debugFile_ = "";
   private TopLevelLineData previousLineData_ = null;
}
