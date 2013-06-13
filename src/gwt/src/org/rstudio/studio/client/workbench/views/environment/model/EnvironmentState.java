/*
 * EnvironmentState.java
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class EnvironmentState extends JavaScriptObject
{
   protected EnvironmentState()
   {
   }
   
   public final native int contextDepth() /*-{
      return this.context_depth;
   }-*/;

   public final native String functionName() /*-{
      return this.function_name;
   }-*/;

   public final native JsArray<CallFrame> callFrames() /*-{
      return this.call_frames;
   }-*/;
   
   public final native boolean getUseProvidedSource() /*-{
      return this.use_provided_source;
   }-*/;
   
   public final native String getFunctionCode() /*-{
      return this.function_code;
   }-*/;
}
