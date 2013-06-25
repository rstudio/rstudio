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

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.debugging.model.FunctionSteps;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BreakpointManager
{
   @Inject
   public BreakpointManager(DebuggingServerOperations server)
   {
      Debug.log("Hi! Here I am to manage your breakpoints!");
      server_ = server;
   }
   
   public void setBreakpoint(final String fileName, int lineNumber)
   {
      Debug.log("Breakpoint requested at file " + fileName + " and line " + lineNumber);
      int[] lineNumbers = new int[] { lineNumber };
      server_.getFunctionSteps(fileName, lineNumbers, 
            new ServerRequestCallback<JsArray<FunctionSteps> > () {
               @Override
               public void onResponseReceived(JsArray<FunctionSteps> response)
               {
                  // TODO: Set the breakpoint
                  Debug.log("Breakpoint will be set at "+ response.get(0).getName() + ":" + response.get(0).getSteps());
               }
               
               @Override
               public void onError(ServerError error)
               {
                   
               }
      });
   }
   
   private ArrayList<Breakpoint> breakpoints_;
   private final DebuggingServerOperations server_;
}
