/*
 * PythonInterpreter.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.JavaScriptObject;

public class PythonInterpreter extends JavaScriptObject
{
   protected PythonInterpreter()
   {
   }
   
   public static final native PythonInterpreter create()
   /*-{
      return {};
   }-*/;
   
   public final native String getPath()          /*-{ return this["path"]        || "";    }-*/;
   public final native String getType()          /*-{ return this["type"]        || "";    }-*/;
   public final native String getVersion()       /*-{ return this["version"]     || "";    }-*/;
   public final native String getDescription()   /*-{ return this["description"] || "";    }-*/;
   public final native boolean isValid()         /*-{ return this["valid"]       || false; }-*/;
   public final native String getInvalidReason() /*-{ return this["reason"]      || "";    }-*/;
}
