/*
 * PublishDocServicePage.java
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
 */package org.rstudio.studio.client.rsconnect.ui;

import java.util.ArrayList;

import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;

import com.google.gwt.resources.client.ImageResource;

public class PublishDocServicePage 
   extends WizardNavigationPage<RSConnectPublishInput, RSConnectPublishResult>
{
   public PublishDocServicePage(String title, String subTitle, 
                                ImageResource icon,
                                RSConnectPublishInput input)
   {
      super(title, subTitle, "Publish To", icon, null, createPages(input));
   }
   
   private static ArrayList<WizardPage<RSConnectPublishInput, 
                                       RSConnectPublishResult>> 
           createPages(RSConnectPublishInput input)
   {
      ArrayList<WizardPage<RSConnectPublishInput, 
                           RSConnectPublishResult>> pages =
                           new ArrayList<WizardPage<RSConnectPublishInput, 
                                                    RSConnectPublishResult>>();
      pages.add(new PublishRPubsPage("RPubs", "RPubs is a free service from " + 
         "RStudio for sharing documents on the web."));
      String rscTitle = "RStudio Connect";
      String rscDesc = 
            "A local service running inside your organization. Publish and " +
            "collaborate privately and securely.";
      if (input.isMultiRmd())
      {
         pages.add(new PublishMultiplePage(rscTitle, rscDesc, 
               RSConnectResources.INSTANCE.localAccountIcon(), input));
      }
      else 
      {
         pages.add(new PublishReportSourcePage(rscTitle, rscDesc, 
               RSConnectResources.INSTANCE.localAccountIcon(), input, 
               false));
      }
      return pages;
   }
}
