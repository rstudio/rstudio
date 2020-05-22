/*
 * PublishCodePage.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.RSConnect;
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
         {
            RSConnectPublishSource source = null;
            if (input.getOriginatingEvent().getFromPreview() != null)
            {
               source = new RSConnectPublishSource(
                              input.getOriginatingEvent().getFromPreview(),
                              input.getWebsiteDir(),
                              input.isSelfContained(),
                              asStatic,
                              input.isShiny(),
                              input.getDescription());
            }
            else
            {
               source = new RSConnectPublishSource(
                              input.getOriginatingEvent().getPath(),
                              input.getOriginatingEvent().getHtmlFile(),
                              input.getWebsiteDir(),
                              input.getWebsiteOutputDir(),
                              input.isSelfContained(),
                              asStatic,
                              input.isShiny(),
                              input.getDescription(),
                              input.getContentType());
            }
            contents_.setPublishSource(source, input.getContentType(), 
                  asMultiple, true);
         }
         else
            contents_.setPublishSource(
                  new RSConnectPublishSource(input.getSourceRmd().getPath(),
                        input.getWebsiteDir(),
                        input.isSelfContained(),
                        asStatic, 
                        input.isShiny(),
                        input.getDescription(),
                        input.getContentType()),
                  input.getContentType(),
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
      contents_ = new RSConnectDeploy(null, RSConnect.CONTENT_TYPE_NONE, 
            null, true);
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
   protected void validateAsync(RSConnectPublishResult input,
         OperationWithInput<Boolean> onValidated)
   {
      contents_.validateResult(onValidated);
   }
   
   private RSConnectDeploy contents_;
}
