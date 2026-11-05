/*
 * InstallRDialog.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.renv.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FieldSetPanel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.renv.RenvConstants;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Confirms the destination before any R installation work starts, and where
 * both builds of R run (ARM64 Windows, Apple silicon Macs), which build.
 */
public class InstallRDialog extends ModalDialog<InstallRDialog.Choice>
{
   public static class Choice
   {
      Choice(String directory, String architecture)
      {
         this.directory = directory;
         this.architecture = architecture;
      }

      public final String directory;

      // "arm64" or "x86_64"; "" for rig's default where there's no choice
      public final String architecture;
   }

   /**
    * @param offered the builds this machine runs, native first ("" alone
    *                where there's no choice)
    * @param available those of 'offered' that rig has the version for
    */
   public InstallRDialog(String version, String directory,
                          List<String> offered, List<String> available,
                          ProgressOperationWithInput<Choice> operation)
   {
      super(constants_.installRCaption(version), Roles.getDialogRole(), operation);
      setThemeAware(true);
      setOkButtonCaption(constants_.installRVersion(version));

      directory_ = new DirectoryChooserTextBox(constants_.rInstallDirectory(),
                                              ElementIds.TextBoxButtonId.R_INSTALL_DIR);
      directory_.setReadOnly(false);
      directory_.setText(directory);
      directory_.setWidth("100%");
      directory_.addValueChangeHandler(event -> updateOkButton());
      DomUtils.addEventListener(directory_.getTextBox().getElement(), "input", false,
                                event -> updateOkButton());

      // focus lands on the folder field, so the description (own subfolder,
      // restart afterwards) is attached to the dialog for assistive tech
      Label description = new Label(constants_.rInstallDescription(version));
      setARIADescribedBy(description.getElement());

      contents_ = new VerticalPanel();
      contents_.setWidth("440px");
      contents_.add(description);
      contents_.add(new VerticalSpacer("12px"));
      contents_.add(directory_);

      defaultArchitecture_ = available.isEmpty() ? "" : available.get(0);
      if (offered.size() > 1)
         contents_.add(createBuildChoice(version, offered, available));

      updateOkButton();
   }

   // One radio button per build; a build rig doesn't have is shown but can't
   // be chosen, with a note when that leaves only the emulated x64 build.
   private Widget createBuildChoice(String version, List<String> offered, List<String> available)
   {
      Label label = new Label(constants_.rInstallBuild());
      VerticalPanel choices = new VerticalPanel();
      FieldSetPanel group = new FieldSetPanel(label);
      group.add(choices);

      String name = DOM.createUniqueId();
      for (String architecture : offered)
      {
         RadioButton button = new RadioButton(name, buildLabel(architecture));
         button.setEnabled(available.contains(architecture));
         button.setValue(StringUtil.equals(architecture, defaultArchitecture_));
         architectures_.add(architecture);
         availableBuilds_.add(available.contains(architecture));
         buttons_.add(button);
         choices.add(button);
      }

      if (!available.contains(offered.get(0)) && available.contains("x86_64"))
         choices.add(new Label(constants_.rVersionX64OnlyNote(version)));

      VerticalPanel panel = new VerticalPanel();
      panel.add(new VerticalSpacer("12px"));
      panel.add(label);
      panel.add(group);
      return panel;
   }

   private static String buildLabel(String architecture)
   {
      return StringUtil.equals(architecture, "x86_64")
            ? constants_.x64BuildLabel()
            : constants_.arm64BuildLabel();
   }

   private String selectedArchitecture()
   {
      for (int i = 0; i < buttons_.size(); i++)
      {
         if (buttons_.get(i).getValue())
            return architectures_.get(i);
      }
      return defaultArchitecture_;
   }

   @Override
   protected Choice collectInput()
   {
      return new Choice(directory_.getText().trim(), selectedArchitecture());
   }

   @Override
   protected Widget createMainWidget()
   {
      return contents_;
   }

   @Override
   public void focusFirstControl()
   {
      directory_.focus();
   }

   @Override
   protected void onEnableControls(boolean enabled)
   {
      // while the install request is pending, an edit to the folder would
      // otherwise re-enable OK and allow a second request
      directory_.setEnabled(enabled);

      // a build rig doesn't have stays unavailable
      for (int i = 0; i < buttons_.size(); i++)
         buttons_.get(i).setEnabled(enabled && availableBuilds_.get(i));

      if (enabled)
         updateOkButton();
   }

   private void updateOkButton()
   {
      enableOkButton(!collectInput().directory.isEmpty());
   }

   private final DirectoryChooserTextBox directory_;
   private final VerticalPanel contents_;
   private final String defaultArchitecture_;
   private final List<String> architectures_ = new ArrayList<>();
   private final List<RadioButton> buttons_ = new ArrayList<>();
   private final List<Boolean> availableBuilds_ = new ArrayList<>();
   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
}
