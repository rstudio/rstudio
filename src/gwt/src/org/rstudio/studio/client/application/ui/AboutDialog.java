/*
 * AboutDialog.java
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

package org.rstudio.studio.client.application.ui;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.Clipboard;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.ProductEditionInfo;
import org.rstudio.studio.client.application.model.ProductInfo;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.studio.client.common.GlobalDisplay;

public class AboutDialog extends ModalDialogBase
{
   public AboutDialog(ProductInfo info)
   {
      super(Roles.getDialogRole());
      RStudioGinjector.INSTANCE.injectMembers(this);

      setText(constants_.title(editionInfo_.editionName()));

      ThemedButton copyVersionButton = new ThemedButton(constants_.copyVersionButtonTitle(), (ClickEvent) ->
      {
         Clipboard.setText("RStudio " + info.version + " " +
            "\"" + info.release_name + "\" " + info.build_type +
            " (" + info.commit + ", " + info.date + ") " +
            constants_.forText() + info.os + "\n" +
            Window.Navigator.getUserAgent());
         RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(GlobalDisplay.MSG_INFO, constants_.versionCopiedText(),
                 constants_.versionInformationCopiedText());
      });
      addButton(copyVersionButton, constants_.copyVersionButton());

      ThemedButton OKButton = new ThemedButton(constants_.okBtn(), (ClickEvent) -> closeDialog());
      addOkButton(OKButton);

      if (editionInfo_.proLicense() && Desktop.hasDesktopFrame())
      {
         ThemedButton licenseButton = new ThemedButton(constants_.manageLicenseBtn(), (ClickEvent) ->
         {
            closeDialog();
            editionInfo_.showLicense();
         });
         addLeftButton(licenseButton, ElementIds.ABOUT_MANAGE_LICENSE_BUTTON);
      }
      contents_ = new AboutDialogContents(info, editionInfo_);
      setARIADescribedBy(contents_.getDescriptionElement());
      setWidth("600px"); //$NON-NLS-1$
   }

   @Override
   protected Widget createMainWidget()
   {
      return contents_;
   }

   @Override
   protected void focusInitialControl()
   {
      focusOkButton();
   }

   @Inject
   private void initialize(ProductEditionInfo editionInfo)
   {
      editionInfo_ = editionInfo;
   }

   private AboutDialogContents contents_;
   private ProductEditionInfo editionInfo_;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
