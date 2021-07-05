/*
 * NewQuartoProjectOptions.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.projects.model;

import org.rstudio.studio.client.quarto.model.QuartoConstants;

import com.google.gwt.core.client.JavaScriptObject;

public class NewQuartoProjectOptions extends JavaScriptObject
{
   protected NewQuartoProjectOptions()
   {
   }
   
   public final static NewQuartoProjectOptions createDefault()
   {
      return create(QuartoConstants.PROJECT_DEFAULT, QuartoConstants.ENGINE_KNITR, "python3", "");
   }
   
   public native final static NewQuartoProjectOptions create(String type, String engine, String kernel, String venv) 
   /*-{
      var options = new Object();
      options.type = type;
      options.engine = engine;
      options.kernel = kernel;
      options.venv= venv;
      return options;
   }-*/;
   
   public final native String getType() /*-{ return this["type"]; }-*/;
   public final native String getEngine() /*-{ return this["engine"]; }-*/;
   public final native String getKernel() /*-{ return this["kernel"]; }-*/;
   public final native String getVenv() /*-{ return this["venv"]; }-*/;
}
