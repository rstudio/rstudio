/*
 * DebuggingServerOperations.java
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

import com.google.gwt.core.client.JsArray;

import org.rstudio.studio.client.common.debugging.model.FunctionSteps;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface DebuggingServerOperations
{
   public void getFunctionSteps(
         String fileName, 
         int[] lineNumbers,
         ServerRequestCallback<JsArray<FunctionSteps>> requestCallback);
   
   public void setFunctionBreakpoints(
         String functionName,
         ArrayList<String> steps,
         ServerRequestCallback<Void> requestCallback);
   
   public void getFunctionSyncState(
         String functionName,
         ServerRequestCallback<Boolean> requestCallback);
}