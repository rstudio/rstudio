/*
 * ProjectPythonPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.prefs.views.PythonDialogResources;
import org.rstudio.studio.client.workbench.prefs.views.PythonPreferencesPaneBase;
import org.rstudio.studio.client.workbench.prefs.views.PythonServerOperations;

import com.google.inject.Inject;

public class ProjectPythonPreferencesPane extends PythonPreferencesPaneBase<RProjectOptions>
{
   @Inject
   public ProjectPythonPreferencesPane(PythonDialogResources res,
                                       PythonServerOperations server)
   {
      super("380px", "(Use default)");
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
      RestartRequirement requirement = new RestartRequirement();
      RProjectConfig config = options.getConfig();
      
      String oldValue = config.getPythonPath();
      String newValue = tbPythonInterpreter_.getText();
      
      boolean isSet =
            interpreter_ != null &&
            interpreter_.isValid() &&
            !StringUtil.isNullOrEmpty(newValue) &&
            !StringUtil.equals(newValue, placeholderText_);
      
      if (isSet && !StringUtil.equals(oldValue, newValue))
      {
         config.setPythonType(interpreter_.getType());
         config.setPythonVersion(interpreter_.getVersion());
         config.setPythonPath(interpreter_.getPath());
         requirement.setSessionRestartRequired(true);
      }
      
      return requirement;
   }

}
