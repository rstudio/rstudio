/*
 * BreakpointManager.java
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
import java.util.TreeSet;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.events.BreakpointSavedEvent;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.debugging.model.BreakpointState;
import org.rstudio.studio.client.common.debugging.model.FunctionSteps;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleInputProcessedEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleInputProcessedHandler;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStatusChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStatusChangedHandler;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// Provides management for breakpoints. 
//
// The typical workflow for interactively adding a new breakpoint is as follows:
// 1) The user clicks on the gutter of the editor, which generates an editor
//    event (BreakpointSetEvent)
// 2) The editing target (which maintains a reference to the breakpoint manager)
//    asks the manager to create a breakpoint, and passes the new breakpoint 
//    back to the editing surface (addOrUpdateBreakpoint)
// 3) The breakpoint manager fetches the name of the function and the step of
//    the function in which the breakpoint occurs from the server, and updates
//    the breakpoint with this information (get_function_steps) 
// 4) The breakpoint manager combines the breakpoint with all of the other 
//    breakpoints for the function, and makes a single call to the server to
//    update the function's breakpoints (set_function_breakpoints)
// 5) If successful, the breakpoint manager emits a BreakpointSavedEvent, which
//    is picked up by the editing target, which updates the display to show that
//    the breakpoint is now enabled.

@Singleton
public class BreakpointManager 
               implements SessionInitHandler, 
                          PackageStatusChangedHandler,
                          ConsoleInputProcessedHandler
{
   @Inject
   public BreakpointManager(
         DebuggingServerOperations server,
         EventBus events,
         Session session)
   {
      server_ = server;
      events_ = events;
      session_ = session;

      // this singleton class is constructed before the session is initialized,
      // so wait until the session init happens to grab our persisted state
      events_.addHandler(SessionInitEvent.TYPE, this);
      events_.addHandler(PackageStatusChangedEvent.TYPE, this);
      events_.addHandler(ConsoleInputProcessedEvent.TYPE, this);
   }
   
   // Public methods ---------------------------------------------------------
   
   public Breakpoint setBreakpoint(
         final String fileName,
         final String functionName,
         int lineNumber)
   {
      // create the new breakpoint and arguments for the server call
      int[] lineNumbers = new int[] { lineNumber };
      final int newBreakpointId = currentBreakpointId_++;
      final Breakpoint breakpoint = Breakpoint.create(newBreakpointId,
            fileName, 
            functionName,
            lineNumber);
      breakpoints_.add(breakpoint);
      
      server_.getFunctionSteps(
            functionName, 
            lineNumbers, 
            new ServerRequestCallback<JsArray<FunctionSteps>> () {
               @Override
               public void onResponseReceived(JsArray<FunctionSteps> response)
               {
                  // found the function and the steps in the function; next, 
                  // ask the server to set the breakpoint
                  if (response.length() > 0)
                  {
                     FunctionSteps steps = response.get(0);
                     breakpoint.addFunctionSteps(steps.getName(),
                           steps.getLineNumber(),
                           steps.getSteps());                     
                     setFunctionBreakpoints(functionName);
                  }
                  // didn't find anything; remove this breakpoint
                  else
                  {
                     discardUnsettableBreakpoint(breakpoint);
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  // didn't find anything on that line that we could use to set
                  // a breakpoint; remove it 
                  discardUnsettableBreakpoint(breakpoint);
               }
      });
      breakpointStateDirty_ = true;
      return breakpoint;
   }
   
   public void removeBreakpoint(int breakpointId)
   {
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.getBreakpointId() == breakpointId)
         {
            breakpoints_.remove(breakpoint);
            setFunctionBreakpoints(breakpoint.getFunctionName());
            break;
         }
      }
      breakpointStateDirty_ = true;
   }
   
   public void moveBreakpoint(int breakpointId)
   {
      // because of Java(Script)'s reference semantics, the editor's instance
      // of the breakpoint object is the same one we have here, so we don't
      // need to update the line number--we just need to persist the new state.
      breakpointStateDirty_ = true;      
   }
   
   public ArrayList<Breakpoint> getBreakpointsInFile(String fileName)
   {
      ArrayList<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.getFileName().equals(fileName))
         {
            breakpoints.add(breakpoint);
         }
      }
      return breakpoints;
   }

   // Event handlers ----------------------------------------------------------

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      new JSObjectStateValue(
            "debug-breakpoints",
            "debugBreakpointState",
            ClientState.PROJECT_PERSISTENT,
            session_.getSessionInfo().getClientState(),
            false)
       {
          @Override
          protected void onInit(JsObject value)
          {
             if (value != null)
             {          
                BreakpointState state = value.cast();

                // restore all of the breakpoints
                JsArray<Breakpoint> breakpoints = 
                      state.getPersistedBreakpoints();
                for (int idx = 0; idx < breakpoints.length(); idx++)
                {
                   Breakpoint breakpoint = breakpoints.get(idx);
                   
                   // make sure the next breakpoint we create after a restore 
                   // has a value larger than any existing breakpoint
                   currentBreakpointId_ = Math.max(
                         currentBreakpointId_, 
                         breakpoint.getBreakpointId() + 1);
                   
                   breakpoints_.add(breakpoint);
                   
                   // this initialization happens after the source windows are
                   // up, so fire an event to the editor to show this 
                   // breakpoint. as new source windows are opened, they will
                   // call getBreakpointsInFile to populate themselves.
                   events_.fireEvent(new BreakpointSavedEvent(
                         breakpoint, true));                     
                }
             }
          }
   
          @Override
          protected JsObject getValue()
          {
             BreakpointState state = 
                   BreakpointState.create();
             for (Breakpoint breakpoint: breakpoints_)
             {
                state.addPersistedBreakpoint(breakpoint);
             }
             breakpointStateDirty_ = false;
             return state.cast();
          }
   
          @Override
          protected boolean hasChanged()
          {
             return breakpointStateDirty_;
          }
       };
   }
   
   @Override
   public void onPackageStatusChanged(PackageStatusChangedEvent event)
   {
      // discard the event if we aren't in package development mode
      String type = session_.getSessionInfo().getBuildToolsType();
      if (!type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         return;
      }

      // ignore package unload events
      if (!event.getPackageStatus().isLoaded())
      {
         return;
      }
      
      // figure out if the package that just loaded was the one we're currently
      // developing; if it isn't, discard the event
      FileSystemItem projectDir = session_.getSessionInfo()
            .getActiveProjectDir();
      String packageName = projectDir.getStem();
      String eventPackageName = event.getPackageStatus().getName(); 
      if (packageName != eventPackageName)
      {
         return;
      }         
      
      // enable any breakpoints inside files that are inside the project folder
      resetBreakpointsInPath(projectDir.getPath(), false);
   }

   @Override
   public void onConsoleInputProcessed(ConsoleInputProcessedEvent event)
   {
      RegExp sourceExp = RegExp.compile("source\\('([^']*)'\\)");
      MatchResult fileMatch = sourceExp.exec(event.getInput());
      if (fileMatch == null || fileMatch.getGroupCount() == 0)
      {
         return;
      }
      
      resetBreakpointsInPath(fileMatch.getGroup(1), true);
   }

   // Private methods ---------------------------------------------------------

   private void setFunctionBreakpoints(String functionName)
   {
      ArrayList<String> steps = new ArrayList<String>();
      final ArrayList<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.getFunctionName().equals(functionName) &&
             functionName.length() > 0)
         {
            steps.add(breakpoint.getFunctionSteps());
            breakpoints.add(breakpoint);
         }
      }
      server_.setFunctionBreakpoints(
            functionName, 
            steps,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void v)
               {
                  for (Breakpoint breakpoint: breakpoints)
                  {
                     breakpoint.activate();
                     events_.fireEvent(new BreakpointSavedEvent(
                           breakpoint, true));                     
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  for (Breakpoint breakpoint: breakpoints)
                  {
                     discardUnsettableBreakpoint(breakpoint);
                  }
               }
            });
   }
   
   private void discardUnsettableBreakpoint(Breakpoint breakpoint)
   {
      events_.fireEvent(
            new BreakpointSavedEvent(breakpoint, false));
      breakpoints_.remove(breakpoint);
   }
   
   private void resetBreakpointsInPath(String path, boolean isFile)
   {
      TreeSet<String> functionsToBreak = new TreeSet<String>();
      for (Breakpoint breakpoint: breakpoints_)
      {
         boolean processBreakpoint = isFile ?
               breakpoint.getFileName().equals(path) :
               breakpoint.getFileName().startsWith(path);
         if (processBreakpoint)
         {
            functionsToBreak.add(breakpoint.getFunctionName());
         }
      }
      for (String functionName: functionsToBreak)
      {
         setFunctionBreakpoints(functionName);
      }
   }
   
   private ArrayList<Breakpoint> breakpoints_ = new ArrayList<Breakpoint>();
   private final DebuggingServerOperations server_;
   private final EventBus events_;
   private final Session session_;
   private boolean breakpointStateDirty_ = false;
   private int currentBreakpointId_ = 0;
}
