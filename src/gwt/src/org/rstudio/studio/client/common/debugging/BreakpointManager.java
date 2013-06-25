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
   public BreakpointManager(DebuggingServerOperations server)
   {
      server_ = server;
   }
   
   // Public methods ---------------------------------------------------------
   public void setBreakpoint(final String fileName, int lineNumber)
   {
      int[] lineNumbers = new int[] { lineNumber };
      server_.getFunctionSteps(
            fileName, 
            lineNumbers, 
            new ServerRequestCallback<JsArray<FunctionSteps> > () {
               @Override
               public void onResponseReceived(JsArray<FunctionSteps> response)
               {
                  breakpoints_.add(new Breakpoint(fileName, response.get(0)));
                  setFunctionBreakpoints(fileName, response.get(0).getName());
               }
               
               @Override
               public void onError(ServerError error)
               {
                   
               }
      });
   }
   
   public void removeBreakpoint(final String fileName, int lineNumber)
   {
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.getFileName().equals(fileName) &&
             breakpoint.getLineNumber() == lineNumber)
         {
            breakpoints_.remove(breakpoint);
            setFunctionBreakpoints(fileName, breakpoint.getFunctionName());
         }
      }
   }
  
   // Private methods ---------------------------------------------------------
   private void setFunctionBreakpoints(String fileName, String functionName)
   {
      ArrayList<Integer> steps = new ArrayList<Integer>();
      for (Breakpoint breakpoint: breakpoints_)
      {
         if (breakpoint.getFunctionName().equals(functionName) &&
             breakpoint.getFileName().equals(fileName))
         {
            steps.add(breakpoint.getFunctionSteps());
         }
      }
      server_.setFunctionBreakpoints(
            functionName, 
            steps,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onError(ServerError error)
               {
                   
               }
            });
   }
   
   private ArrayList<Breakpoint> breakpoints_ = new ArrayList<Breakpoint>();
   private final DebuggingServerOperations server_;
}
