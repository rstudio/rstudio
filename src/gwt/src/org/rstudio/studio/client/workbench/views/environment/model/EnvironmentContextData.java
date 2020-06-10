/*
 * EnvironmentContextData.java
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
package org.rstudio.studio.client.workbench.views.environment.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class EnvironmentContextData extends JavaScriptObject
{
   protected EnvironmentContextData() { }

   public final native String language() /*-{
      return this.language || "R";
   }-*/;
   
   public final native int contextDepth() /*-{
      return this.context_depth;
   }-*/;

   public final native String functionName() /*-{
      return this.function_name;
   }-*/;

   public final native JsArray<CallFrame> callFrames() /*-{
      return this.call_frames;
   }-*/;
   
   public final native boolean useProvidedSource() /*-{
      return this.use_provided_source;
   }-*/;
   
   public final native String functionCode() /*-{
      return this.function_code;
   }-*/;
  
   public final native JsArray<RObject> environmentList() /*-{
      return this.environment_list;
   }-*/;

   public final native String environmentName() /*-{
      return this.environment_name;
   }-*/;
   
   public final native boolean environmentIsLocal() /*-{
      return this.environment_is_local;
   }-*/;
   
   public final native String functionEnvName() /*-{
      return this.function_environment_name;
   }-*/;
   
   public final native boolean environmentMonitoring() /*-{
      return this.environment_monitoring;
   }-*/;
}
