/*
 * AboutDialogContents.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.application.ui;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Anchor;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.ProductEditionInfo;
import org.rstudio.studio.client.application.model.ProductInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.application.model.ProductNotice;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

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
      versionMajorLabel.setText(info.version_major + "." + info.version_minor + "." + info.version_patch);
      versionBuildLabel.setText(constants_.versionBuildLabel() + info.version_suffix.split("\\+")[1]);

      // a11y
      productInfo.getElement().setId("productinfo");
      gplLinkLabel.getElement().setId("gplLinkLabel");
      Roles.getLinkRole().setAriaDescribedbyProperty(gplLink.getElement(), Id.of(gplLinkLabel.getElement()));

      userAgentLabel.setText(
            Window.Navigator.getUserAgent());
      buildLabel.setText(
           "\"" + info.release_name + "\" " + info.build_type + " (" + info.commit.substring(0, 8) + ", " +
           info.date + constants_.buildLabelForText() + info.os);
      productName.setText(editionInfo.editionName());
      copyrightYearLabel.setText("2009-" + info.copyright_year);

      // Warn that dailies and previews aren't supported
      if (!info.build_type.equals("Release")) {
         supportNotice.setText(
            constants_.buildTypeThisText() +
            info.build_type +
            " " + constants_.buildOfText() +
            editionInfo.editionName() +
            " " + constants_.supportNoticeText()
         );
      }
      else
      {
         preReleaseRibbon.setVisible(false);
      }


      showNoticelink_.setClickHandler(() ->
      {
         RStudioGinjector.INSTANCE.getServer().getProductNotice(new ServerRequestCallback<ProductNotice>()
         {
            @Override
            public void onResponseReceived(ProductNotice notice)
            {
               AboutOpenSourceDialog about = new AboutOpenSourceDialog(notice);
               about.showModal();
            }
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      });

      if (editionInfo.proLicense())
      {
         // no need to show GPL notice in pro edition
         gplNotice.setVisible(false);

         if (Desktop.hasDesktopFrame())
         {
            // load license status in desktop mode
            licenseBox.setVisibleLines(3);
            licenseLabel.setVisible(true);
            licenseBox.setVisible(true);
            licenseBox.setText(constants_.licenseBoxLoadingText());
            Desktop.getFrame().getLicenseStatusMessage(licenseStatus ->
            {
               licenseBox.setText(licenseStatus);
            });
         }
      }
   }

   public Element getDescriptionElement()
   {
      return productInfo.getElement();
   }

   @UiField InlineLabel versionMajorLabel;
   @UiField InlineLabel versionBuildLabel;
   @UiField InlineLabel userAgentLabel;
   @UiField InlineLabel buildLabel;
   @UiField InlineLabel copyrightYearLabel;
   @UiField HyperlinkLabel showNoticelink_;
   @UiField HTMLPanel gplNotice;
   @UiField HTMLPanel licenseLabel;
   @UiField HTMLPanel preReleaseRibbon;
   @UiField TextArea licenseBox;
   @UiField Label productName;
   @UiField HTMLPanel productInfo;
   @UiField InlineLabel supportNotice;
   @UiField Anchor gplLink;
   @UiField Label gplLinkLabel;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
