/*
 * CompilePdfState.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.common.compilepdf.model;

import org.rstudio.studio.client.common.compile.CompileError;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class CompilePdfState extends JavaScriptObject
{ 
   protected CompilePdfState()
   {
   }
   
   public final native boolean isTabVisible() /*-{
      return this.tab_visible;
   }-*/;
   
   public final native boolean isRunning() /*-{
      return this.running;
   }-*/;
   
   public final native String getTargetFile() /*-{
      return this.target_file;
   }-*/;
   
   public final native String getOutput() /*-{
      return this.output;
   }-*/;

   public final native JsArray<CompileError> getErrors() /*-{
      return this.errors;
   }-*/;
}
