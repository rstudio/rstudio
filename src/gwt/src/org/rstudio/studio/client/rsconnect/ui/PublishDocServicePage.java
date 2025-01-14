/*
 * PublishDocServicePage.java
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
 */package org.rstudio.studio.client.rsconnect.ui;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.RsconnectConstants;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy.ServerType;

import com.google.gwt.resources.client.ImageResource;

public class PublishDocServicePage 
   extends WizardNavigationPage<RSConnectPublishInput, RSConnectPublishResult>
{
   public PublishDocServicePage(String title, String subTitle, 
                                ImageResource icon,
                                RSConnectPublishInput input)
   {
      super(title, subTitle, constants_.publishTo(), icon, null, createPages(input));
   }
   
   private static ArrayList<WizardPage<RSConnectPublishInput, 
                                       RSConnectPublishResult>> 
           createPages(RSConnectPublishInput input)
   {
      ArrayList<WizardPage<RSConnectPublishInput, 
                           RSConnectPublishResult>> pages = new ArrayList<>();
      String rscTitle = RSConnectAccountWizard.SERVICE_NAME;
      String rscDesc = RSConnectAccountWizard.SERVICE_DESCRIPTION;
           
      WizardPage<RSConnectPublishInput, RSConnectPublishResult> connectPage;
      if (input.isMultiRmd() && !input.isWebsiteRmd())
      {
         connectPage = new PublishMultiplePage(rscTitle, rscDesc,
               new ImageResource2x(RSConnectResources.INSTANCE.localAccountIcon2x()), input,
               ServerType.RSCONNECT);
      }
      else 
      {
         if (input.isStaticDocInput())
         {
            // static input implies static output
            connectPage = new PublishFilesPage(rscTitle, rscDesc,
                  new ImageResource2x(RSConnectResources.INSTANCE.localAccountIcon2x()), input,
                  false, true, ServerType.RSCONNECT);
         }
         else
         {
            connectPage = new PublishReportSourcePage(rscTitle, rscDesc,
                  constants_.publishToRstudioConnect(),
                  new ImageResource2x(RSConnectResources.INSTANCE.localAccountIcon2x()), input, 
                  false, true, ServerType.RSCONNECT);
         }
      }
      WizardPage<RSConnectPublishInput, RSConnectPublishResult> rpubsPage  =
            new PublishRPubsPage("RPubs", constants_.rPubsSubtitle());

      String cloudTitle = constants_.cloudTitle();
      String cloudSubtitle = constants_.cloudSubtitle();

      WizardPage<RSConnectPublishInput, RSConnectPublishResult> cloudPage = null;

      if (input.isMultiRmd() && !input.isWebsiteRmd())
      {
         cloudPage = new PublishMultiplePage(cloudTitle, cloudSubtitle,
            new ImageResource2x(RSConnectResources.INSTANCE.positCloudAccountIcon2x()), input,
            ServerType.POSITCLOUD);
      } else
      {
         if (input.isStaticDocInput())
         {
            // static input implies static output
            cloudPage = new PublishFilesPage(cloudTitle, cloudSubtitle,
               new ImageResource2x(RSConnectResources.INSTANCE.positCloudAccountIcon2x()), input,
               false, true, ServerType.POSITCLOUD);
         }
         else
         {
            cloudPage = new PublishReportSourcePage(cloudTitle, cloudSubtitle,
               constants_.publishToPositCloud(),
               new ImageResource2x(RSConnectResources.INSTANCE.positCloudAccountIcon2x()), input,
               false, false, ServerType.POSITCLOUD);
         }
      }

      if (input.isExternalUIEnabled() && !input.isWebsiteRmd())
         pages.add(rpubsPage);
      if (input.isConnectUIEnabled())
         pages.add(connectPage);
      if (input.isCloudUIEnabled() && cloudPage != null)
         pages.add(cloudPage);
      
      return pages;
   }
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
