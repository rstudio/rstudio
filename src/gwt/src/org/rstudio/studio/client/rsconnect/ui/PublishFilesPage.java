/*
 * PublishCodePage.java
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

import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSource;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;

public class PublishFilesPage 
   extends WizardPage<RSConnectPublishInput, RSConnectPublishResult>
{
   public PublishFilesPage(String title, String subTitle, ImageResource icon,
         RSConnectPublishInput input, boolean asMultiple, boolean asStatic)
   {
      super(title, subTitle, "Publish", icon, null);
      
      // createWidget is called by super() above
      if (contents_ != null)
      {
         // publish the HTML file or the original R Markdown doc, as requested
         if (asStatic)
            contents_.setPublishSource(
                  new RSConnectPublishSource(
                        input.getOriginatingEvent().getFromPreview()),
                  asMultiple, true);
         else
            contents_.setPublishSource(
                  new RSConnectPublishSource(input.getSourceRmd().getPath()),
                  asMultiple, false);
      }
   }

   @Override
   public void focus()
   {
      contents_.focus();
   }
   
   @Override
   public void onActivate(ProgressIndicator indicator)
   {
      contents_.onActivate(indicator);
   }
   
   @Override
   protected Widget createWidget()
   {
      contents_ = new RSConnectDeploy(null, null, true);
      return contents_;
   }

   @Override
   protected void initialize(RSConnectPublishInput initData)
   {
   }

   @Override
   protected RSConnectPublishResult collectInput()
   {
      return contents_.getResult();
   }

   @Override
   protected boolean validate(RSConnectPublishResult input)
   {
      return contents_.isResultValid();
   }
   
   private RSConnectDeploy contents_;
}
