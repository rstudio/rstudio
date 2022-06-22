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
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class PythonPreferencesPane extends PythonPreferencesPaneBase<UserPrefs>
{
   @Inject
   public PythonPreferencesPane(PythonDialogResources res,
                                PythonServerOperations server,
                                Commands commands)
   {

      super("420px", constants_.pythonPreferencesText(), false);

      projectPrefsPanel_ = new HorizontalPanel();
      projectPrefsPanel_.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
      overrideLabel_ = new Label();
      projectPrefsPanel_.add(overrideLabel_);

      SmallButton editProjectSettings = new SmallButton(constants_.editProjectPreferencesButtonLabel());
      editProjectSettings.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
      editProjectSettings.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            // open the project options pane for editing
            // this will open to the General tab, but it would be ideal if we could open directly to Editing tab
            commands.projectOptions().execute();
         }
      });
      projectPrefsPanel_.add(editProjectSettings);
      add(projectPrefsPanel_);
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
         overrideLabel_.addStyleName(RES.styles().overrideLabel());
         projectPrefsPanel_.setVisible(true);
      }
      else
      {
         overrideLabel_.removeStyleName(RES.styles().overrideLabel());
         projectPrefsPanel_.setVisible(false);
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
   private final HorizontalPanel projectPrefsPanel_;
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);

}
