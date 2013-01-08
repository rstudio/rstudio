/*
 * BuildState.java
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

package org.rstudio.studio.client.workbench.views.buildtools.model;

import org.rstudio.studio.client.common.compile.CompileError;
import org.rstudio.studio.client.common.compile.CompileOutput;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class BuildState extends JavaScriptObject
{ 
   protected BuildState()
   {
   }
   
   public final native boolean isRunning() /*-{
      return this.running;
   }-*/;
   
   public final native String getErrorsBaseDir() /*-{
      return this.errors_base_dir;
   }-*/;
   
   public final native JsArray<CompileError> getErrors() /*-{
      return this.errors;
   }-*/;
   
   public final native JsArray<CompileOutput> getOutputs() /*-{
      return this.outputs;
   }-*/;
}
