/*
 * NewRSConnectCloudPage.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.RsconnectConstants;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

public class NewPositCloudPage
            extends WizardPage<NewRSConnectAccountInput,
                               NewRSConnectAccountResult>
{
   public NewPositCloudPage()
   {
      super(constants_.cloudTitle(),
            constants_.newPositCloudPageSubTitle(),
            constants_.newPositCloudPageCaption(),
            new ImageResource2x(RSConnectResources.INSTANCE.positCloudAccountIcon2x()),
            new ImageResource2x(RSConnectResources.INSTANCE.positCloudAccountIconLarge2x()));
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
         accountWidget_ = new PositCloudAccount();
      
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
   
   private PositCloudAccount accountWidget_;
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
