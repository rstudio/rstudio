/*
 * NewRSConnectLocalPage.java
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
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardIntermediatePage;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

import com.google.gwt.user.client.ui.Widget;

public class NewRSConnectLocalPage 
            extends WizardIntermediatePage<NewRSConnectAccountInput,
                                           NewRSConnectAccountResult>
{

   public NewRSConnectLocalPage()
   {
      super(RSConnectAccountWizard.SERVICE_NAME, 
            RSConnectAccountWizard.SERVICE_DESCRIPTION,
            "RStudio Connect Account",
            new ImageResource2x(RSConnectResources.INSTANCE.localAccountIcon2x()), 
            new ImageResource2x(RSConnectResources.INSTANCE.localAccountIconLarge2x()),
            new NewRSConnectAuthPage());
   }

   @Override
   public void focus()
   {
      if (local_ != null)
         local_.focus();
   }

   @Override
   public void collectIntermediateInput(
         final ProgressIndicator indicator, 
         final OperationWithInput<NewRSConnectAccountResult> onResult) 
   {
      // get the current configuration and abort if it's not valid
      final NewRSConnectAccountResult result = collectInput();
      if (!validate(result))
      {
         onResult.execute(result);
         return;
      }

      setIntermediateResult(result);
      onResult.execute(result);
   }
   
   @Override
   protected Widget createWidget()
   {
      if (local_ == null)
         local_ = new RSConnectLocalAccount();
      return local_;
   }

   @Override
   protected void initialize(NewRSConnectAccountInput initData)
   {
   }

   @Override
   protected NewRSConnectAccountResult collectInput()
   {
      return new NewRSConnectAccountResult("", local_.getServerUrl().trim(), 
            "");
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return input != null &&
            input.getServerUrl() != null && 
            !input.getServerUrl().isEmpty();
   }


   private RSConnectLocalAccount local_;
}