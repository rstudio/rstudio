/*
 * PublishStaticPage.java
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

import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;

import com.google.gwt.user.client.ui.Widget;

public class PublishStaticPage 
   extends WizardPage<RSConnectPublishInput, RSConnectPublishResult>
{
   // Public methods ---------------------------------------------------------

   public PublishStaticPage(String title, String subTitle, 
         RSConnectPublishInput input, boolean showIcon, boolean asMultiple)
   {
      super(title, subTitle, "Publish", 
            showIcon ? RSConnectAccountResources.INSTANCE.localAccountIcon() :
               null, 
            RSConnectAccountResources.INSTANCE.localAccountIconLarge());

      if (publishWidget_ != null)
      {
         publishWidget_.setContentPath(
               input.getOriginatingEvent().getHtmlFile(), asMultiple);
      }
   }
   
   @Override
   public void focus()
   {
      if (publishWidget_ != null)
         publishWidget_.focus();
   }

   @Override 
   public void onActivate()
   {
      if (publishWidget_ != null)
         publishWidget_.onActivate();
   }

   // Protected methods ------------------------------------------------------

   @Override
   protected Widget createWidget()
   {
      publishWidget_ = new PublishStatic(null);
      return publishWidget_;
   }

   @Override
   protected void initialize(RSConnectPublishInput initData)
   {
   }
   
   @Override
   protected RSConnectPublishResult collectInput()
   {
      return publishWidget_.getResult();
   }

   @Override
   protected boolean validate(RSConnectPublishResult input)
   {
      return publishWidget_.isResultValid();
   }
   
   private PublishStatic publishWidget_;
}
