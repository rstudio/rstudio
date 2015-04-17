/*
 * PublishReportSourcePage.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import java.util.ArrayList;

import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;

public class PublishReportSourcePage 
   extends WizardNavigationPage<RSConnectPublishInput, RSConnectPublishResult>
{
   
   public PublishReportSourcePage(
         String title,
         String subTitle,
         RSConnectPublishInput input,
         boolean asMultiple)
   {
      super(title, subTitle, "Publish Source Code", asMultiple ? 
            RSConnectResources.INSTANCE.publishMultipleRmd() :
            RSConnectResources.INSTANCE.publishSingleRmd(),
            null, 
            createPages(input, asMultiple));
   }

   private static ArrayList<WizardPage<RSConnectPublishInput, 
                                       RSConnectPublishResult>> 
           createPages(RSConnectPublishInput input, boolean asMultiple)
   {
      ArrayList<WizardPage<RSConnectPublishInput, 
                           RSConnectPublishResult>> pages =
                           new ArrayList<WizardPage<RSConnectPublishInput, 
                                                    RSConnectPublishResult>>();
      
      pages.add(new PublishFilesPage("Publish " +
            (asMultiple? "documents" : "document") + " with source code",
            "Choose this option if you want to create " + 
            (asMultiple ? "scheduled reports" : "a scheduled report") + " or " +
            "execute your " + 
            (asMultiple ? "documents" : "document") + " on the server.", 
            RSConnectResources.INSTANCE.publishDocWithSource(), 
            input, asMultiple, false));
      String staticTitle = "Publish finished " + 
            (asMultiple ? "documents" : "document") + " only";
      String staticSubtitle = "Choose this option to publish the content as " +
             "it appears in RStudio.";
      if (input.isConnectUIEnabled() && input.isExternalUIEnabled() && 
          input.isSelfContained() && !asMultiple)
      {
         // if RStudio Connect and external accounts are both enabled, static 
         // content could go to either RPubs or Connect
         pages.add(new PublishStaticDestPage(staticTitle, staticSubtitle, 
               RSConnectResources.INSTANCE.publishDocWithoutSource(),
               input, asMultiple));
      }
      else if (input.isConnectUIEnabled())
      {
         // only RStudio Connect is available for static content
         pages.add(new PublishFilesPage(staticTitle, staticSubtitle, 
               RSConnectResources.INSTANCE.publishDocWithoutSource(), 
               input, asMultiple, true));
      }
      else if (input.isSelfContained())
      {
         // only RPubs is available for static content
         pages.add(new PublishRPubsPage(staticTitle, staticSubtitle));
      }
      return pages;
   }
}