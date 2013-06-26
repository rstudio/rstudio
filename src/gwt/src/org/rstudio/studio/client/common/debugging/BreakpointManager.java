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

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.events.BreakpointSavedEvent;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.debugging.model.FunctionSteps;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BreakpointManager
{
   @Inject
   public BreakpointManager(
         DebuggingServerOperations server,
         EventBus events)
   {
      server_ = server;
      events_ = events;
   }
   
   // Public methods ---------------------------------------------------------
   public Breakpoint setBreakpoint(final String fileName, int lineNumber)
   {
      int[] lineNumbers = new int[] { lineNumber };
      final int newBreakpointId = currentBreakpointId_++;
      final Breakpoint breakpoint = new Breakpoint(newBreakpointId,
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
                     breakpoint.addFunctionSteps(response.get(0));
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
   private int currentBreakpointId_ = 0;
}
