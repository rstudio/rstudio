/*
 * NewRSConnectCloudConnectPage.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.RsconnectConstants;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult.AccountType;

import com.google.gwt.user.client.ui.Widget;

public class NewRSConnectCloudConnectPage
            extends WizardPage<NewRSConnectAccountInput,
                               NewRSConnectAccountResult>
{
   public NewRSConnectCloudConnectPage()
   {
      super(constants_.positConnectCloud(),
            constants_.positConnectCloudSubTitle(),
            constants_.positConnectCloudCaption(),
            new ImageResource2x(RSConnectResources.INSTANCE.cloudAccountIcon2x()),
            new ImageResource2x(RSConnectResources.INSTANCE.cloudAccountIconLarge2x()));
   }

   @Override
   public void focus()
   {
   }

   @Override
   protected Widget createWidget()
   {
      if (widget_ == null)
         widget_ = new RSConnectCloudConnectAccount();

      return widget_;
   }

   @Override
   protected void initialize(NewRSConnectAccountInput initData)
   {
   }

   @Override
   protected NewRSConnectAccountResult collectInput()
   {
      return new NewRSConnectAccountResult(
            AccountType.RSConnectCloudConnectAccount);
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return true;
   }

   private RSConnectCloudConnectAccount widget_;
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
