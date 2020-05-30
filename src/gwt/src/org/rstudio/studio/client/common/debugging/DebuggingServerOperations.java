/*
 * DebuggingServerOperations.java
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

import java.util.ArrayList;

import com.google.gwt.core.client.JsArray;

import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.debugging.model.FunctionState;
import org.rstudio.studio.client.common.debugging.model.FunctionSteps;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface DebuggingServerOperations
{
   public void getFunctionSteps(
         String functionName,
         String fileName,
         String packageName,
         int[] lineNumbers,
         ServerRequestCallback<JsArray<FunctionSteps>> requestCallback);
   
   public void setFunctionBreakpoints(
         String functionName,
         String fileName,
         String packageName,
         ArrayList<String> steps,
         ServerRequestCallback<Void> requestCallback);
   
   public void getFunctionState(
         String functionName,
         String fileName,
         int lineNumber,
         ServerRequestCallback<FunctionState> requestCallback);
   
   public void setErrorManagementType(
         String type,
         ServerRequestCallback<Void> requestCallback);
   
   public void updateBreakpoints(
         ArrayList<Breakpoint> breakpoints,
         boolean set, 
         boolean arm, 
         ServerRequestCallback<Void> requestCallback);

   public void removeAllBreakpoints(
         ServerRequestCallback<Void> requestCallback);
}
