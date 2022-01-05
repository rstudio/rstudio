/*
 * PythonPreferencesPane.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class PythonPreferencesPane extends PythonPreferencesPaneBase<UserPrefs>
{
   @Inject
   public PythonPreferencesPane(PythonDialogResources res,
                                PythonServerOperations server)
   {

      super("420px", constants_.pythonPreferencesText(), false);

      
      overrideLabel_ = new Label();
      add(overrideLabel_);
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      String pythonPath = prefs.pythonPath().getGlobalValue();
      initialize(pythonPath);
      
      // notify user if project pref is overriding global pref
      String projectPythonPath = prefs.pythonPath().getProjectValue();
      boolean hasProjectOverride = !StringUtil.isNullOrEmpty(projectPythonPath);
      if (hasProjectOverride)
      {
         String text =
                 constants_.overrideText();
         
         overrideLabel_.setText(text);
         overrideLabel_.setVisible(true);
         overrideLabel_.addStyleName(RES.styles().overrideLabel());
      }
      else
      {
         overrideLabel_.setVisible(false);
         overrideLabel_.removeStyleName(RES.styles().overrideLabel());
      }
   }
   
   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      return onApply(false, (PythonInterpreter interpreter) ->
      {
         if (interpreter.isValid())
         {
            prefs.pythonType().setGlobalValue(interpreter.getType());
            prefs.pythonVersion().setGlobalValue(interpreter.getVersion());
            prefs.pythonPath().setGlobalValue(interpreter.getPath());
         }
         else
         {
            prefs.pythonType().removeGlobalValue(true);
            prefs.pythonVersion().removeGlobalValue(true);
            prefs.pythonPath().removeGlobalValue(true);
         }
      });
   }
   
   private final Label overrideLabel_;
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);

}
