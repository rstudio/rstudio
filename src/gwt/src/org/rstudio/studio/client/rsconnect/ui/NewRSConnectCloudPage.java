/*
 * NewRSConnectCloudPage.java
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
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

import com.google.gwt.user.client.ui.Widget;

public class NewRSConnectCloudPage 
            extends WizardPage<NewRSConnectAccountInput,
                               NewRSConnectAccountResult>
{
   public NewRSConnectCloudPage()
   {
      super(RSConnect.CLOUD_SERVICE_NAME,
            "A cloud service run by RStudio. Publish Shiny applications " +
            "and interactive documents to the Internet.",
            "Connect ShinyApps.io Account",
            new ImageResource2x(RSConnectResources.INSTANCE.cloudAccountIcon2x()), 
            new ImageResource2x(RSConnectResources.INSTANCE.cloudAccountIconLarge2x()));
   }

   @Override
   public void focus()
   {
      if (accountWidget_ != null)
         accountWidget_.focus();
   }

   @Override
   protected Widget createWidget()
   {
      if (accountWidget_ == null)
         accountWidget_ = new RSConnectCloudAccount();
      
      return accountWidget_;
   }

   @Override
   protected void initialize(NewRSConnectAccountInput initData)
   {
   }

   @Override
   protected NewRSConnectAccountResult collectInput()
   {
      return new NewRSConnectAccountResult(
            accountWidget_.getAccountInfo());
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return input.getCloudSecret().length() > 0;
   }
   
   private RSConnectCloudAccount accountWidget_;
}
