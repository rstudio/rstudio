/*
 * PublishReportSourcePage.java
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
package org.rstudio.studio.client.rsconnect.ui;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.RsconnectConstants;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;

import com.google.gwt.resources.client.ImageResource;

public class PublishReportSourcePage 
   extends WizardNavigationPage<RSConnectPublishInput, RSConnectPublishResult>
{
   
   public PublishReportSourcePage(
         String title,
         String subTitle,
         ImageResource icon,
         RSConnectPublishInput input,
         boolean asMultiple)
   {
      super(title, subTitle, constants_.publishToRstudioConnect(), icon, null,
            createPages(input, asMultiple));
   }

   private static ArrayList<WizardPage<RSConnectPublishInput, 
                                       RSConnectPublishResult>> 
           createPages(RSConnectPublishInput input, boolean asMultiple)
   {
      ArrayList<WizardPage<RSConnectPublishInput, 
                           RSConnectPublishResult>> pages = new ArrayList<>();
      
      String descriptor = constants_.documentLowercase();
      if (asMultiple)
         descriptor = constants_.documentsLowercasePlural();
      if (input.isWebsiteRmd())
         descriptor = constants_.websiteLowercase();
      
      pages.add(new PublishFilesPage(constants_.publishFilesPageTitle(descriptor),
            constants_.publishReportSourcePageSubTitle(
                    asMultiple ? constants_.scheduledReportsPlural() : constants_.scheduledReportsSingular()
                    ,descriptor),
            new ImageResource2x(RSConnectResources.INSTANCE.publishDocWithSource2x()), 
            input, asMultiple, false));
      String staticTitle = constants_.publishReportSourcePageStaticTitle(descriptor);
      String staticSubtitle = constants_.publishReportSourcePageStaticSubtitle();
      pages.add(new PublishFilesPage(staticTitle, staticSubtitle, 
            new ImageResource2x(RSConnectResources.INSTANCE.publishDocWithoutSource2x()), 
            input, asMultiple, true));
      return pages;
   }
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
