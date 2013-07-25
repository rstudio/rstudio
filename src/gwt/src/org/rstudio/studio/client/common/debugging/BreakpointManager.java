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
import java.util.Set;
import java.util.TreeSet;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.debugging.events.ActivePackageLoadedEvent;
import org.rstudio.studio.client.common.debugging.events.BreakpointsSavedEvent;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.debugging.model.BreakpointState;
import org.rstudio.studio.client.common.debugging.model.FunctionSteps;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputHandler;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.ContextDepthChangedEvent;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;

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
// 3) The breakpoint manager checks to see whether the source code for the 
//    function on disk is identical to the source code for the function as
//    it exists in the R session (get_function_sync_state). If it isn't, 
//    it defers setting the breakpoint.
// 4) The breakpoint manager fetches the steps and substeps of the function in 
//    which the breakpoint occurs from the server, and updates the breakpoint
//    with this information (get_function_steps) 
// 5) The breakpoint manager combines the breakpoint with all of the other 
//    breakpoints for the function, and makes a single call to the server to
//    update the function's breakpoints (set_function_breakpoints)
// 6) If successful, the breakpoint manager emits a BreakpointsSavedEvent, which
//    is picked up by the editing target, which updates the display to show that
//    the breakpoint is now enabled.

@Singleton
public class BreakpointManager 
               implements SessionInitHandler, 
                          ActivePackageLoadedEvent.Handler,
                          ContextDepthChangedEvent.Handler,
                          ConsoleWriteInputHandler
{
   @Inject
   public BreakpointManager(
         DebuggingServerOperations server,
         EventBus events,
         Session session,
         WorkbenchContext workbench)
   {
      server_ = server;
      events_ = events;
      session_ = session;
      workbench_ = workbench;

      // this singleton class is constructed before the session is initialized,
      // so wait until the session init happens to grab our persisted state
      events_.addHandler(SessionInitEvent.TYPE, this);
      events_.addHandler(ActivePackageLoadedEvent.TYPE, this);
      events_.addHandler(ConsoleWriteInputEvent.TYPE, this);      
      events_.addHandler(ContextDepthChangedEvent.TYPE, this);
   }
   
   // Public methods ---------------------------------------------------------
   
   public Breakpoint setTopLevelBreakpoint(
         final String path,
         final int lineNumber)
   {
      return addBreakpoint(Breakpoint.create(
            currentBreakpointId_++, 
            path, 
            "toplevel", 
            lineNumber, 
            Breakpoint.STATE_ACTIVE, 
            Breakpoint.TYPE_TOPLEVEL));
   }
   
   public Breakpoint setBreakpoint(
         final String path,
         final String functionName,
         int lineNumber, 
         boolean immediately)
   {
      // create the new breakpoint and arguments for the server call
      final Breakpoint breakpoint = addBreakpoint(Breakpoint.create(
            currentBreakpointId_++,
            path,
            functionName,
            lineNumber,
            immediately ?
                  Breakpoint.STATE_PROCESSING :
                  Breakpoint.STATE_INACTIVE,
            Breakpoint.TYPE_FUNCTION));
      
      // If the breakpoint is in a function that is active on the callstack, 
      // it's being set on the stored rather than the executing copy. It's 
      // possible to set it right now, but it will probably violate user 
      // expectations. Process it when the function is no longer executing.
      if (activeFunctions_.contains(
            new FileFunction(breakpoint)))
      {
         breakpoint.setPendingDebugCompletion(true);
         markInactiveBreakpoint(breakpoint);
      }      
      else if (immediately)
      {
         server_.getFunctionSyncState(functionName, path,
               new ServerRequestCallback<Boolean>()
         {
            @Override
            public void onResponseReceived(Boolean inSync)
            {
               // if the function lines up with the version on the server, set
               // the breakpoint now
               if (inSync)
               {
                  prepareAndSetFunctionBreakpoints(
                        new FileFunction(breakpoint));
               }
               // otherwise, save an inactive breakpoint--we'll revisit the
               // marker the next time the file is sourced or the package is
               // rebuilt
               else
               {
                  markInactiveBreakpoint(breakpoint);
               }
            }

            @Override
            public void onError(ServerError error)
            {
               // if we can't figure out whether the function is in sync, 
               // leave it inactive for now
               markInactiveBreakpoint(breakpoint);
            }
         });
      }
      
      breakpointStateDirty_ = true;
      return breakpoint;
   }
   
   public void removeBreakpoint(int breakpointId)
   {
      Breakpoint breakpoint = getBreakpoint(breakpointId);
      if (breakpoint != null)
      {
         breakpoints_.remove(breakpoint);
         if (breakpoint.getState() == Breakpoint.STATE_ACTIVE)
         {
            setFunctionBreakpoints(new FileFunction(breakpoint));
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
      
      // the breakpoint knows its position in the function, which needs to be
      // recalculated; do that the next time we set breakpoints on this function
      Breakpoint breakpoint = getBreakpoint(breakpointId);
      if (breakpoint != null)
      {
         breakpoint.markStepsNeedUpdate();
      }
   }
   
   public ArrayList<Breakpoint> getBreakpointsInFile(String fileName)
   {
      ArrayList<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.isInFile(fileName))
         {
            breakpoints.add(breakpoint);
         }
      }
      return breakpoints;
   }
   
   public boolean injectBreakpointsDuringSource(
         String fileName, 
         int startLine, 
         int endLine)
   {
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.isInFile(fileName) && 
             breakpoint.getLineNumber() >= startLine &&
             breakpoint.getLineNumber() <= endLine &&
             breakpoint.getType() == Breakpoint.TYPE_FUNCTION)
         {
            prepareAndSetFunctionBreakpoints(new FileFunction(breakpoint));
            return true;
         }
      }
      return false;
   }
   
   // Event handlers ----------------------------------------------------------

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      new JSObjectStateValue(
            "debug-breakpoints",
            "debugBreakpointsState",
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
                   
                }
                // this initialization happens after the source windows are
                // up, so fire an event to the editor to show all known 
                // breakpoints. as new source windows are opened, they will
                // call getBreakpointsInFile to populate themselves.
                events_.fireEvent(
                      new BreakpointsSavedEvent(breakpoints_, true));
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
   public void onActivePackageLoaded(ActivePackageLoadedEvent event)
   {
      // when the active package is loaded (e.g. during Build & Reload), we
      // lose trace state on all associated breakpoints--re-enable them.
      resetBreakpointsInPath(
            session_.getSessionInfo().getActiveProjectDir().getPath(), false);
   }

   @Override
   public void onConsoleWriteInput(ConsoleWriteInputEvent event)
   {
      // when a file is sourced, replay all the breakpoints in the file.
      RegExp sourceExp = RegExp.compile("source(.with.encoding)?\\('([^']*)'.*");
      MatchResult fileMatch = sourceExp.exec(event.getInput());
      if (fileMatch == null || fileMatch.getGroupCount() == 0)
      {
         return;
      }      
      String path = FilePathUtils.normalizePath(
            fileMatch.getGroup(2), 
            workbench_.getCurrentWorkingDir().getPath());
      resetBreakpointsInPath(path, true);
   }
   
   @Override
   public void onContextDepthChanged(ContextDepthChangedEvent event)
   {
      // When we move around in debug context and hit a breakpoint, the initial
      // evaluation state is a temporary construction that needs to be stepped
      // past to begin actually evaluating the function. Step past it
      // immediately.
      JsArray<CallFrame> frames = event.getCallFrames();
      Set<FileFunction> activeFunctions = new TreeSet<FileFunction>();
      for (int idx = 0; idx < frames.length(); idx++)
      {
         String functionName = frames.get(idx).getFunctionName();
         String fileName = frames.get(idx).getFileName();
         if (functionName.equals(".doTrace"))
         {
            events_.fireEvent(new SendToConsoleEvent("n", true));
         }
         activeFunctions.add(new FileFunction(functionName, fileName, false));
      }
      
      // For any functions that were previously active in the callstack but
      // are no longer active, enable any pending breakpoints for those 
      // functions.
      Set<FileFunction> enableFunctions = new TreeSet<FileFunction>();
      for (FileFunction function: activeFunctions_)
      {
         if (!activeFunctions.contains(function))
         {
            for (Breakpoint breakpoint: breakpoints_)
            {
               if (breakpoint.isPendingDebugCompletion() &&
                   breakpoint.getState() == Breakpoint.STATE_INACTIVE &&
                   function.containsBreakpoint(breakpoint))
               {
                  enableFunctions.add(function);
               }
            }
         }
      }
      
      for (FileFunction function: enableFunctions)
      {
         prepareAndSetFunctionBreakpoints(function);
      }
      
      // Record the new frame list.
      activeFunctions_ = activeFunctions;
   }

   // Private methods ---------------------------------------------------------

   private void setFunctionBreakpoints(FileFunction function)
   {
      ArrayList<String> steps = new ArrayList<String>();
      final ArrayList<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (function.containsBreakpoint(breakpoint))
         {
            steps.add(breakpoint.getFunctionSteps());
            breakpoints.add(breakpoint);
         }
      }
      server_.setFunctionBreakpoints(
            function.functionName,
            function.fileName,
            steps,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void v)
               {
                  for (Breakpoint breakpoint: breakpoints)
                  {
                     breakpoint.setState(Breakpoint.STATE_ACTIVE);
                  }
                  notifyBreakpointsSaved(breakpoints, true);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  discardUnsettableBreakpoints(breakpoints);
               }
            });
   }
      
   private void prepareAndSetFunctionBreakpoints(final FileFunction function)
   {
      // look over the list of breakpoints in this function and see if any are
      // marked inactive, or if they need their steps refreshed (necessary
      // when a function has had steps added or removed in the editor)
      final ArrayList<Breakpoint> inactiveBreakpoints = 
            new ArrayList<Breakpoint>();
      int[] inactiveLines = new int[]{};
      int numLines = 0;
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (function.containsBreakpoint(breakpoint) &&
             (breakpoint.getState() != Breakpoint.STATE_ACTIVE ||
              breakpoint.needsUpdatedSteps()))
         {
            inactiveBreakpoints.add(breakpoint);
            inactiveLines[numLines++] = breakpoint.getLineNumber();
         }
      }
      
      // if we found breakpoints that aren't yet active, try to get the 
      // corresponding steps from the function 
      if (inactiveBreakpoints.size() > 0)
      {
         server_.getFunctionSteps(
               function.functionName,
               function.fileName,
               inactiveLines, 
               new ServerRequestCallback<JsArray<FunctionSteps>> () {
                  @Override
                  public void onResponseReceived
                         (JsArray<FunctionSteps> response)
                  {
                     // found the function and the steps in the function; next, 
                     // ask the server to set the breakpoint
                     if (response.length() > 0)
                     {
                        processFunctionSteps(inactiveBreakpoints, response);
                        setFunctionBreakpoints(function);
                      }
                     // no results: discard the breakpoints
                     else
                     {
                        discardUnsettableBreakpoints(inactiveBreakpoints);
                     }
                  }
                  
                  @Override
                  public void onError(ServerError error)
                  {
                     discardUnsettableBreakpoints(inactiveBreakpoints);
                  }
         });
      }
      else
      {
         setFunctionBreakpoints(function);
      }
   }
   
   private void discardUnsettableBreakpoints(ArrayList<Breakpoint> breakpoints)
   {
      if (breakpoints.size() == 0)
      {
         return;
      }
      for (Breakpoint breakpoint: breakpoints)
      {
         breakpoints_.remove(breakpoint);
      }
      notifyBreakpointsSaved(breakpoints, false);
   }
   
   private void resetBreakpointsInPath(String path, boolean isFile)
   {
      Set<FileFunction> functionsToBreak = new TreeSet<FileFunction>();
      for (Breakpoint breakpoint: breakpoints_)
      {
         boolean processBreakpoint = isFile ?
               breakpoint.isInFile(path) :
               breakpoint.isInPath(path);
         if (processBreakpoint)
         {
            functionsToBreak.add(new FileFunction(breakpoint));
         }
      }
      for (FileFunction function: functionsToBreak)
      {
         prepareAndSetFunctionBreakpoints(function);
      }
   }
   
   private void markInactiveBreakpoint(Breakpoint breakpoint)
   {
      breakpoint.setState(Breakpoint.STATE_INACTIVE);
      ArrayList<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
      breakpoints.add(breakpoint);
      notifyBreakpointsSaved(breakpoints, true);
   }
   
   private void processFunctionSteps(
         ArrayList<Breakpoint> breakpoints,
         JsArray<FunctionSteps> stepList)
   {
      ArrayList<Breakpoint> unSettableBreakpoints = 
            new ArrayList<Breakpoint>();
      
      // Walk through the array of breakpoints for which we requested function
      // steps and the array of results from the server in lock-step, populating
      // each breakpoint with its steps.
      for (int i = 0; i < breakpoints.size() && 
                      i < stepList.length(); i++)
      {
         FunctionSteps steps = stepList.get(i);
         Breakpoint breakpoint = breakpoints.get(i);
         if (steps.getSteps().length() > 0)
         {
            // if the server set this breakpoint on a different line than 
            // requested, make sure there's not already a breakpoint on that
            // line; if there is, discard this one.
            if (breakpoint.getLineNumber() != steps.getLineNumber())
            {
               for (Breakpoint possibleDupe: breakpoints_)
               {
                  if (breakpoint.getPath().equals(
                         possibleDupe.getPath()) &&
                      steps.getLineNumber() == 
                         possibleDupe.getLineNumber() &&
                      breakpoint.getBreakpointId() != 
                         possibleDupe.getBreakpointId())
                  {
                     breakpoint.setState(Breakpoint.STATE_DUPLICATE);
                     unSettableBreakpoints.add(breakpoint);
                  }
               }
            }
            breakpoint.addFunctionSteps(steps.getName(),
                  steps.getLineNumber(),
                  steps.getSteps());
         }
         else
         {
            unSettableBreakpoints.add(breakpoint);
         }
      }
      discardUnsettableBreakpoints(unSettableBreakpoints);
   }
   
   private void notifyBreakpointsSaved(
         ArrayList<Breakpoint> breakpoints, 
         boolean saved)
   {
      breakpointStateDirty_ = true;
      events_.fireEvent(
            new BreakpointsSavedEvent(breakpoints, saved));
   }
   
   private Breakpoint getBreakpoint (int breakpointId)
   {
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.getBreakpointId() == breakpointId)
         {
            return breakpoint;
         }
      }
      return null;
   }
   
   private Breakpoint addBreakpoint (Breakpoint breakpoint)
   {
      breakpoints_.add(breakpoint);
      breakpointStateDirty_ = true;
      return breakpoint;
   }
     
   // Private classes ---------------------------------------------------------
   
   class FileFunction implements Comparable<FileFunction>
   {
      public String functionName;
      public String fileName;
      boolean fullPath;
      
      public FileFunction (String fun, String file, boolean useFullPath)
      {
         functionName = fun;
         fileName = file.trim();        
         fullPath = useFullPath;
      }

      public FileFunction (String fun, String file)
      {
         this(fun, file, true);
      }
      
      public FileFunction (Breakpoint breakpoint)
      {
         this(breakpoint.getFunctionName(), breakpoint.getPath());
      }
      
      public boolean containsBreakpoint(Breakpoint breakpoint)
      {
         if (!breakpoint.getFunctionName().equals(functionName))
         {
            return false;
         }
         if (fullPath)
         {
            return breakpoint.getPath().equals(fileName);
         }
         else
         {
            return FilePathUtils.friendlyFileName(breakpoint.getPath()).equals(
                  FilePathUtils.friendlyFileName(fileName));
         }  
      }
      
      @Override
      public int compareTo(FileFunction other)
      {
         int fun = functionName.compareTo(other.functionName);
         if (fun != 0)
         {
            return fun;
         }
         if (!fullPath || !other.fullPath)
         {
            return FilePathUtils.friendlyFileName(fileName).compareTo(
                  FilePathUtils.friendlyFileName(other.fileName));
         }
         return fileName.compareTo(other.fileName);
      }      
   }

   private final DebuggingServerOperations server_;
   private final EventBus events_;
   private final Session session_;
   private final WorkbenchContext workbench_;

   private ArrayList<Breakpoint> breakpoints_ = new ArrayList<Breakpoint>();
   private Set<FileFunction> activeFunctions_ = new TreeSet<FileFunction>();

   private boolean breakpointStateDirty_ = false;
   private int currentBreakpointId_ = 0;
}
