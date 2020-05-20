/*
 * NewRSConnectAccountPage.java
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

import org.rstudio.core.client.widget.WizardIntermediatePage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;

public class NewRSConnectAccountPage 
   extends WizardIntermediatePage<NewRSConnectAccountInput, 
                                  NewRSConnectAccountResult>
{
   public NewRSConnectAccountPage(
         String title,
         String subTitle,
         String pageCaption,
         ImageResource image,
         ImageResource largeImage,
         WizardPage<NewRSConnectAccountInput, NewRSConnectAccountResult> nextPage)
   {
      super(title, subTitle, pageCaption, image, largeImage, nextPage);
   }

   @Override
   public void focus()
   {
      // no focusable widgets in this dialog
   }

   @Override
   protected Widget createWidget()
   {
      contents_ = new RSConnectNewAccount();
      return contents_;
   }

   @Override
   protected void initialize(NewRSConnectAccountInput initData)
   {
   }

   @Override
   protected NewRSConnectAccountResult collectInput()
   {
      return null;
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return true;
   }
   
   private RSConnectNewAccount contents_; 
}
