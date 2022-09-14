/*
 * QuartoPythonCapabilities.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.quarto.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;


public class QuartoPythonCapabilities extends JavaScriptObject
{
   protected QuartoPythonCapabilities()
   {
   }
   
   public final native int getVersionMajor() /*-{
      return this.versionMajor;
   }-*/;
   
   public final native int getVersionMinor() /*-{
      return this.versionMinor;
   }-*/;
   
   public final native String getExecPrefix() /*-{
      return this.execPrefix;
   }-*/;
   
   public final native String getExecutable() /*-{
      return this.executable;
   }-*/;

   public final native String getJupyterCore() /*-{
      return this.jupyter_core;
   }-*/;


   public final native String getNbformat() /*-{
      return this.nbformat;
   }-*/;

   
   public final native String getNbclient() /*-{
      return this.nbclient;
   }-*/;
   
   
   public final native String getIpykerel() /*-{
      return this.ipykernel;
   }-*/;
   
   public final native JsArray<QuartoJupyterKernel> getKernels() /*-{
      return this.kernels;
   }-*/;  
   
   public final native boolean getVenv() /*-{
      return this.venv === true;
   }-*/;
   
   public final native boolean getConda() /*-{
      return this.conda === true;
   }-*/;

}
