/*
 * PublishRPubsPage.java
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

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSource;

import com.google.gwt.user.client.ui.Widget;

public class PublishRPubsPage 
   extends WizardPage<RSConnectPublishInput, RSConnectPublishResult>
{
   public PublishRPubsPage(String title, String subTitle)
   {
      super(title, subTitle, "Publish to RPubs", 
            new ImageResource2x(RSConnectResources.INSTANCE.rpubsPublish2x()), 
            new ImageResource2x(RSConnectResources.INSTANCE.rpubsPublishLarge2x()));
   }

   @Override
   public void focus()
   {
      
   }

   @Override
   protected Widget createWidget()
   {
      return new PublishRPubs();
   }

   @Override
   protected void initialize(RSConnectPublishInput initData)
   {
      initialData_ = initData;
   }

   @Override
   protected RSConnectPublishResult collectInput()
   {
      return new RSConnectPublishResult(
            new RSConnectPublishSource(
                  initialData_.getOriginatingEvent().getPath(), 
                  initialData_.getOriginatingEvent().getHtmlFile(),
                  null, // website directory
                  initialData_.isSelfContained(),
                  true, // as static
                  initialData_.isShiny(),
                  initialData_.getDescription(),
                  initialData_.getContentType()));
   }

   @Override
   protected boolean validate(RSConnectPublishResult input)
   {
      return true;
   }

   private RSConnectPublishInput initialData_;
}
