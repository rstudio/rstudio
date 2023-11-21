/*
 * PublishMultiplePage.java
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
package org.rstudio.studio.client.rsconnect.ui;

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

public class PublishMultiplePage 
   extends WizardNavigationPage<RSConnectPublishInput, RSConnectPublishResult>
{
   public PublishMultiplePage(String title, String subTitle, ImageResource icon,
         RSConnectPublishInput input, ServerType serverType)
   {
      super(title, subTitle, constants_.publishMultiplePageCaption(),
            icon, null, createPages(input, serverType));
   }
   
   private static ArrayList<WizardPage<RSConnectPublishInput, 
                                       RSConnectPublishResult>> 
           createPages(RSConnectPublishInput input, ServerType serverType)
   {
      ArrayList<WizardPage<RSConnectPublishInput, 
                           RSConnectPublishResult>> pages = new ArrayList<>();
      String singleTitle = constants_.publishMultiplePagSingleTitle();
      String singleSubtitle = constants_.publishMultiplePagSingleSubtitle(input.getSourceRmd().getName());
      String multipleTitle = constants_.publishMultiplePageTitle();
      String multipleSubtitle = constants_.publishMultiplePageSubtitle(input.getSourceRmd().getParentPathString());
      if (input.isShiny() || !input.hasDocOutput())
      {
         pages.add(new PublishFilesPage(singleTitle, singleSubtitle, 
               new ImageResource2x(RSConnectResources.INSTANCE.publishSingleRmd2x()), 
               input, false, false, serverType));
         pages.add(new PublishFilesPage(multipleTitle, multipleSubtitle,
               new ImageResource2x(RSConnectResources.INSTANCE.publishMultipleRmd2x()), 
               input, true, false, serverType));
      }
      else if (input.isStaticDocInput())
      {
         pages.add(new PublishFilesPage(singleTitle, singleSubtitle, 
               new ImageResource2x(RSConnectResources.INSTANCE.publishSingleRmd2x()), 
               input, false, true, serverType));
         pages.add(new PublishFilesPage(multipleTitle, multipleSubtitle,
               new ImageResource2x(RSConnectResources.INSTANCE.publishMultipleRmd2x()), 
               input, true, true, serverType));
      }
      else
      {
         String pageCaption = constants_.publish();
         boolean allowScheduling = true;
         if (serverType == ServerType.RSCONNECT)
         {
            pageCaption = constants_.publishToRstudioConnect();
         }
         else if (serverType == ServerType.POSITCLOUD)
         {
            pageCaption = constants_.publishToPositCloud();
            allowScheduling = false;
         }
         pages.add(
            new PublishReportSourcePage(singleTitle, singleSubtitle, pageCaption,
                  new ImageResource2x(RSConnectResources.INSTANCE.publishSingleRmd2x()),
                  input, false, allowScheduling, serverType));
         pages.add(
            new PublishReportSourcePage(multipleTitle, multipleSubtitle, pageCaption,
                  new ImageResource2x(RSConnectResources.INSTANCE.publishMultipleRmd2x()),
                  input, true, allowScheduling, serverType));
      }
      return pages;
   }
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
