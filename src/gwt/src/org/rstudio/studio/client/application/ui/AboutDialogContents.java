/*
 * AboutDialogContents.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.ProductEditionInfo;
import org.rstudio.studio.client.application.model.ProductInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

public class AboutDialogContents extends Composite
{
   public static void ensureStylesInjected()
   {
      new AboutDialogContents();
   }
   
   private static AboutDialogContentsUiBinder uiBinder = GWT
         .create(AboutDialogContentsUiBinder.class);

   interface AboutDialogContentsUiBinder extends
         UiBinder<Widget, AboutDialogContents>
   {
   }
   
   private AboutDialogContents()
   {
      uiBinder.createAndBindUi(this);
   }

   public AboutDialogContents(ProductInfo info, ProductEditionInfo editionInfo)
   {
      initWidget(uiBinder.createAndBindUi(this));
      versionLabel.setText(info.version);
      userAgentLabel.setText(
            Window.Navigator.getUserAgent());
      buildLabel.setText(
           "Build " + info.build + " (" + info.commit.substring(0, 8) + ")");
      noticeBox.setValue(info.notice);
      productName.setText(editionInfo.editionName());
      
      if (editionInfo.proLicense() && Desktop.isDesktop())
      {
         noticeBox.setVisibleLines(9);
         licenseBox.setVisibleLines(3);
         licenseLabel.setVisible(true);
         licenseBox.setVisible(true);
         licenseBox.setText("Loading...");
      }
   }
   
   public void refresh()
   {
      if (licenseBox.isVisible())
      {
         licenseBox.setText("Loading...");
         Desktop.getFrame().getLicenseStatusMessage(licenseStatus ->
         {
            licenseBox.setText(licenseStatus);
         });
      }
   }

   @UiField InlineLabel versionLabel;
   @UiField InlineLabel userAgentLabel;
   @UiField InlineLabel buildLabel;
   @UiField TextArea noticeBox;
   @UiField Label licenseLabel;
   @UiField TextArea licenseBox;
   @UiField Label productName;
}
