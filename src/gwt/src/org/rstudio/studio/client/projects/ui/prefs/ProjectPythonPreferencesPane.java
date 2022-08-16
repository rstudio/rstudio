/*
 * ProjectPythonPreferencesPane.java
 *
 * Copyright (C) 2022 by Posit, PBC
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
package org.rstudio.studio.client.projects.ui.prefs;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.prefs.views.PythonDialogResources;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;
import org.rstudio.studio.client.workbench.prefs.views.PythonPreferencesPaneBase;
import org.rstudio.studio.client.workbench.prefs.views.PythonServerOperations;

import com.google.inject.Inject;

public class ProjectPythonPreferencesPane extends PythonPreferencesPaneBase<RProjectOptions>
{
   @Inject
   public ProjectPythonPreferencesPane(PythonDialogResources res,
                                       PythonServerOperations server)
   {
      super("380px", constants_.useDefaultText(), true);
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      String pythonPath = config.getPythonPath();
      initialize(pythonPath);
   }

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      return onApply(true, (PythonInterpreter interpreter) ->
      {
         RProjectConfig config = options.getConfig();
         config.setPythonType(interpreter.getType());
         config.setPythonVersion(interpreter.getVersion());
         config.setPythonPath(interpreter.getPath());
      });
   }
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);

}
