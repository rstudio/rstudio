/*
 * PythonPreferencesPane.java
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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.inject.Inject;

public class PythonPreferencesPane extends PythonPreferencesPaneBase<UserPrefs>
{
   @Inject
   public PythonPreferencesPane(PythonDialogResources res,
                                PythonServerOperations server)
   {
      super(res, server);
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      String pythonPath = prefs.pythonPath().getGlobalValue();
      if (!StringUtil.isNullOrEmpty(pythonPath))
      {
         tbPythonInterpreter_.setText(pythonPath);
         updateDescription();
      }
      
      server_.pythonActiveInterpreter(new ServerRequestCallback<PythonInterpreter>()
      {
         @Override
         public void onResponseReceived(PythonInterpreter response)
         {
            checkForMismatch(response);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement requirement = new RestartRequirement();
      
      String oldValue = prefs.pythonPath().getGlobalValue();
      String newValue = tbPythonInterpreter_.getText();
      
      boolean isSet =
            interpreter_ != null &&
            interpreter_.isValid() &&
            !StringUtil.isNullOrEmpty(newValue) &&
            !StringUtil.equals(newValue, PYTHON_PLACEHOLDER_TEXT);
      
      if (isSet && !StringUtil.equals(oldValue, newValue))
      {
         prefs.pythonType().setGlobalValue(interpreter_.getType());
         prefs.pythonVersion().setGlobalValue(interpreter_.getVersion());
         prefs.pythonPath().setGlobalValue(interpreter_.getPath());
         requirement.setSessionRestartRequired(true);
      }
      
      return requirement;
   }

}
