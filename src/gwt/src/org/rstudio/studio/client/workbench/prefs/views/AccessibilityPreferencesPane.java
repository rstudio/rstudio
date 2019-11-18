/*
 * AccessibilityPreferencesPane.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.dom.client.Style;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

public class AccessibilityPreferencesPane extends PreferencesPane
{
   @Inject
   public AccessibilityPreferencesPane(UserPrefs prefs,
                                       PreferencesDialogResources res)
   {
      res_ = res;

      add(headerLabel("Assistive Tools"));
      chkScreenReaderEnabled_ = new CheckBox("Screen reader support (requires restart)");
      add(chkScreenReaderEnabled_);

      initialAriaApplicationRole_ = prefs.ariaApplicationRole().getValue();
      add(chkApplicationRole_ = checkboxPref("Entire page has application role (requires restart)",
            prefs.ariaApplicationRole()));

      typingStatusDelay_ = numericPref("Milliseconds after typing before speaking results",
            prefs.typingStatusDelayMs());
      add(indent(typingStatusDelay_));

      Label displayLabel = headerLabel("Other");
      add(displayLabel);
      displayLabel.getElement().getStyle().setMarginTop(8, Style.Unit.PX);
      add(checkboxPref("Reduce user interface animations", prefs.reducedMotion()));
      chkTabMovesFocus_ = new CheckBox("Tab key always moves focus");
      add(chkTabMovesFocus_);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconAccessibility2x());
   }

   @Override
   public String getName()
   {
      return "Accessibility";
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      initialScreenReaderEnabled_ = prefs.getScreenReaderEnabled();
      chkScreenReaderEnabled_.setValue(initialScreenReaderEnabled_);
      chkTabMovesFocus_.setValue(prefs.tabKeyMoveFocus().getValue());
   }

   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement restartRequirement = super.onApply(prefs);

      boolean screenReaderEnabledSetting = chkScreenReaderEnabled_.getValue();
      if (screenReaderEnabledSetting != initialScreenReaderEnabled_)
      {
         initialScreenReaderEnabled_ = screenReaderEnabledSetting;
         prefs.setScreenReaderEnabled(screenReaderEnabledSetting);
         if (Desktop.isDesktop())
            restartRequirement.setDesktopRestartRequired(true);
         else
            restartRequirement.setUiReloadRequired(true);
      }

      boolean applicationRoleSetting = chkApplicationRole_.getValue();
      if (applicationRoleSetting != initialAriaApplicationRole_)
      {
         initialAriaApplicationRole_ = applicationRoleSetting;
         restartRequirement.setUiReloadRequired(true);
      }

      prefs.tabKeyMoveFocus().setGlobalValue(chkTabMovesFocus_.getValue());
      prefs.syncToggleTabKeyMovesFocusState(chkTabMovesFocus_.getValue());
      return restartRequirement;
   }

   @Override
   public boolean validate()
   {
      return (!chkScreenReaderEnabled_.getValue() || typingStatusDelay_.validate("Speak results after typing delay"));
   }

   private final CheckBox chkScreenReaderEnabled_;
   private final NumericValueWidget typingStatusDelay_;
   private final CheckBox chkApplicationRole_;
   private final CheckBox chkTabMovesFocus_;

   // initial values of prefs that can trigger reloads (to avoid unnecessary reloads)
   private boolean initialScreenReaderEnabled_;
   private boolean initialAriaApplicationRole_;

   private final PreferencesDialogResources res_;
}
