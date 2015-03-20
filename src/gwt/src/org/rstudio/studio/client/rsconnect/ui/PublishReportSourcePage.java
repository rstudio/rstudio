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
         RSConnectPublishInput input)
   {
      super(title, subTitle, "Publish Source Code", null, null, 
            createPages(input));
   }

   private static ArrayList<WizardPage<RSConnectPublishInput, 
                                       RSConnectPublishResult>> 
           createPages(RSConnectPublishInput input)
   {
      ArrayList<WizardPage<RSConnectPublishInput, 
                           RSConnectPublishResult>> pages =
                           new ArrayList<WizardPage<RSConnectPublishInput, 
                                                    RSConnectPublishResult>>();
      
      pages.add(new PublishCodePage("Publish document with source code",
            "Choose this option if you want to create a scheduled report or " +
            "execute your document on the server.", input));
      String staticTitle = "Publish finished document only";
      String staticSubtitle = "Choose this option to publish the report as " +
             "it appears in RStudio.";
      if (input.isConnectUIEnabled())
      {
         // if RStudio Connect is enabled, static content could go to either
         // RPubs or Connect
         pages.add(new PublishStaticDestPage(staticTitle, staticSubtitle, 
               input));
      }
      else
      {
         // only RPubs is available for static content
         pages.add(new PublishRPubsPage(staticTitle, staticSubtitle));
      }
      return pages;
   }
}