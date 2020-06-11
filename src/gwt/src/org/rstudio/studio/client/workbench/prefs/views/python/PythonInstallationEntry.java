/*
 * PythonInstallationEntry.java
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
package org.rstudio.studio.client.workbench.prefs.views.python;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class PythonInstallationEntry extends Composite
{
   // TODO: Move to own file?
   private static class PythonInstallationData extends JavaScriptObject
   {
      protected PythonInstallationData()
      {
      }
      
      public final native String pythonType()    /*-{ return this["type"];    }-*/;
      public final native String pythonVersion() /*-{ return this["version"]; }-*/;
      public final native String pythonPath()    /*-{ return this["path"];    }-*/;
   }
   
   public PythonInstallationEntry(PythonInstallationData data)
   {
      panel_ = create(data);
      initWidget(panel_);
   }
   
   private HorizontalPanel create(PythonInstallationData data)
   {
      HorizontalPanel panel = new HorizontalPanel();
      
      // TODO: Add icon appropriate for environment type. (system vs. virtualenv vs. Conda)
      panel.add(new Label("[" + data.pythonType() + "]"));
      panel.add(new Label("Python " + data.pythonVersion()));
      panel.add(new Label("(" + data.pythonPath() + ")"));
      
      return panel;
   }
   
   private final HorizontalPanel panel_;
}
