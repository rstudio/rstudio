/*
 * PublishMultiplePage.java
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

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;

import com.google.gwt.resources.client.ImageResource;

public class PublishMultiplePage 
   extends WizardNavigationPage<RSConnectPublishInput, RSConnectPublishResult>
{
   public PublishMultiplePage(String title, String subTitle, ImageResource icon,
         RSConnectPublishInput input)
   {
      super(title, subTitle, "What do you want to publish?", 
            icon, null, createPages(input));
   }
   
   private static ArrayList<WizardPage<RSConnectPublishInput, 
                                       RSConnectPublishResult>> 
           createPages(RSConnectPublishInput input)
   {
      ArrayList<WizardPage<RSConnectPublishInput, 
                           RSConnectPublishResult>> pages =
                           new ArrayList<WizardPage<RSConnectPublishInput, 
                                                    RSConnectPublishResult>>();
      String singleTitle = "Publish just this document";
      String singleSubtitle = "Only the document " + 
                              input.getSourceRmd().getName() + 
                              " will be published.";
      String multipleTitle = "Publish all documents in the directory";
      String multipleSubtitle = "All of the documents in the directory " + 
                                input.getSourceRmd().getParentPathString() + 
                                " will be published.";
      if (input.isShiny() || !input.hasDocOutput())
      {
         pages.add(new PublishFilesPage(singleTitle, singleSubtitle, 
               new ImageResource2x(RSConnectResources.INSTANCE.publishSingleRmd2x()), 
               input, false, false));
         pages.add(new PublishFilesPage(multipleTitle, multipleSubtitle,
               new ImageResource2x(RSConnectResources.INSTANCE.publishMultipleRmd2x()), 
               input, true, false));
      }
      else if (input.isStaticDocInput())
      {
         pages.add(new PublishFilesPage(singleTitle, singleSubtitle, 
               new ImageResource2x(RSConnectResources.INSTANCE.publishSingleRmd2x()), 
               input, false, true));
         pages.add(new PublishFilesPage(multipleTitle, multipleSubtitle,
               new ImageResource2x(RSConnectResources.INSTANCE.publishMultipleRmd2x()), 
               input, true, true));
      }
      else
      {
         pages.add(new PublishReportSourcePage(singleTitle, singleSubtitle,
               new ImageResource2x(
                  RSConnectResources.INSTANCE.publishSingleRmd2x()), input, false));
         pages.add(new PublishReportSourcePage(multipleTitle, multipleSubtitle,
               new ImageResource2x(
                  RSConnectResources.INSTANCE.publishMultipleRmd2x()), input, true));
      }
      return pages;
   }
}
