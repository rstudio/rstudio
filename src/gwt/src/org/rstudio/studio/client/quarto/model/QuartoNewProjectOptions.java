/*
 * QuartoNewProjectOptions.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.quarto.model;

import com.google.gwt.core.client.JavaScriptObject;

public class QuartoNewProjectOptions extends JavaScriptObject
{
   protected QuartoNewProjectOptions()
   {
   }
   
   public final static QuartoNewProjectOptions createDefault()
   {
      return create(QuartoCommandConstants.PROJECT_DEFAULT, QuartoCommandConstants.ENGINE_KNITR, "python3",
                    "", "", "matplotlib pandas", QuartoCommandConstants.EDITOR_VISUAL);
   }
   
   public native final static QuartoNewProjectOptions create(String type, String engine, String kernel, 
                                                             String venv, String condaenv,
                                                             String packages, String editor) 
   /*-{
      var options = new Object();
      options.type = type;
      options.engine = engine;
      options.kernel = kernel;
      options.venv = venv;
      options.condaenv = condaenv;
      options.packages = packages;
      options.editor = editor;
      return options;
   }-*/;
   
   public final native String getType() /*-{ return this["type"]; }-*/;
   public final native String getEngine() /*-{ return this["engine"]; }-*/;
   public final native String getKernel() /*-{ return this["kernel"]; }-*/;
   public final native String getVenv() /*-{ return this["venv"]; }-*/;
   public final native String getCondaenv() /*-{ return this["condaenv"] || ""; }-*/;
   public final native String getPackages() /*-{ return this["packages"] || ""; }-*/ ;
   public final native String getEditor() /*-{ return this["editor"] || ""; }-*/ ;
}
