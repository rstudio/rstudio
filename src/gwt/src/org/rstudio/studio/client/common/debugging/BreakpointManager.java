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
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import com.google.gwt.core.client.JsArray;
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
public class BreakpointManager implements SessionInitHandler
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
   }
   
   // Public methods ---------------------------------------------------------
   public Breakpoint setBreakpoint(final String fileName, int lineNumber)
   {
      int[] lineNumbers = new int[] { lineNumber };
      final int newBreakpointId = currentBreakpointId_++;
      final Breakpoint breakpoint = Breakpoint.create(newBreakpointId,
            fileName, 
            lineNumber);
      breakpoints_.add(breakpoint);
      server_.getFunctionSteps(
            fileName, 
            lineNumbers, 
            new ServerRequestCallback<JsArray<FunctionSteps>> () {
               @Override
               public void onResponseReceived(JsArray<FunctionSteps> response)
               {
                  if (response.length() > 0)
                  {
                     FunctionSteps steps = response.get(0);
                     breakpoint.addFunctionSteps(steps.getName(),
                           steps.getLineNumber(),
                           steps.getSteps());                     
                     setFunctionBreakpoints(fileName, response.get(0).getName());
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  events_.fireEvent(
                        new BreakpointSavedEvent(breakpoint, false));
                  breakpoints_.remove(breakpoint);
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
            setFunctionBreakpoints(
                  breakpoint.getFileName(), 
                  breakpoint.getFunctionName());
         }
      }
      breakpointStateDirty_ = true;
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      // TODO: Make this persistent; temporary is only to ease debugging.
      new JSObjectStateValue(
            "debug-breakpoints",
            "debugBreakpointState",
            ClientState.TEMPORARY,
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
                   
                   // quick and dirty: populate the breakpoint in any open 
                   // editors                
                   events_.fireEvent(new BreakpointSavedEvent(
                         breakpoint, true));                     

                   breakpoints_.add(breakpoint);
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

   // Private methods ---------------------------------------------------------
   private void setFunctionBreakpoints(String fileName, String functionName)
   {
      ArrayList<Integer> steps = new ArrayList<Integer>();
      final ArrayList<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.getFunctionName().equals(functionName) &&
             breakpoint.getFileName().equals(fileName))
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
                     events_.fireEvent(new BreakpointSavedEvent(
                           breakpoint, false));                     
                  }
               }
            });
   }
   
   private ArrayList<Breakpoint> breakpoints_ = new ArrayList<Breakpoint>();
   private final DebuggingServerOperations server_;
   private final EventBus events_;
   private final Session session_;
   private boolean breakpointStateDirty_ = false;
   private int currentBreakpointId_ = 0;
}
