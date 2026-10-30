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

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.renv.RenvConstants;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/** Confirms the destination before any R installation work starts. */
public class InstallRDialog extends ModalDialog<String>
{
   public InstallRDialog(String version, String directory,
                          ProgressOperationWithInput<String> operation)
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

      contents_ = new VerticalPanel();
      contents_.setWidth("440px");
      contents_.add(new Label(constants_.rInstallDescription(version)));
      contents_.add(new VerticalSpacer("12px"));
      contents_.add(directory_);
      updateOkButton();
   }

   @Override
   protected String collectInput()
   {
      return directory_.getText().trim();
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
      if (enabled)
         updateOkButton();
   }

   private void updateOkButton()
   {
      enableOkButton(!collectInput().isEmpty());
   }

   private final DirectoryChooserTextBox directory_;
   private final VerticalPanel contents_;
   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
}
