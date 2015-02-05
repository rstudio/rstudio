/*
 * RSConnectAccountWizard.java
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

import java.util.ArrayList;

import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

import com.google.gwt.user.client.ui.Widget;


public class RSConnectAccountWizard 
   extends Wizard<NewRSConnectAccountInput,NewRSConnectAccountResult>
{
   public RSConnectAccountWizard(
         boolean showCloudPage,
         ProgressOperationWithInput<NewRSConnectAccountResult> operation)
   {
      super("Connect Account", "Select the type of account", "Connect Account",
            new NewRSConnectAccountInput(), operation);
      showCloudPage_ = showCloudPage;
      if (showCloudPage)
      {
         addPage(new NewRSConnectCloudPage());
      }
      addPage(new NewRSConnectLocalPage());
   }
   
   @Override
   protected Widget createFirstPage()
   {
      return new NewRSConnectAccountPage(
            "Create New Account", "Create Account", "New Account", 
            RSConnectAccountResources.INSTANCE.cloudAccountIcon(), 
            RSConnectAccountResources.INSTANCE.cloudAccountIconLarge(), 
            new WizardNavigationPage<
               NewRSConnectAccountInput,
               NewRSConnectAccountResult>(
                     "Connect Account", 
                     "Select the type of account", 
                     "Connect Account", 
                     RSConnectAccountResources.INSTANCE.cloudAccountIcon(), 
                     RSConnectAccountResources.INSTANCE.cloudAccountIconLarge(), 
                     getPages()));
   }
   
   protected ArrayList<WizardPage<NewRSConnectAccountInput, 
                                  NewRSConnectAccountResult>> getPages()
   {
      ArrayList<WizardPage<NewRSConnectAccountInput, 
                           NewRSConnectAccountResult>> pages =
           new ArrayList<WizardPage<NewRSConnectAccountInput, 
                                    NewRSConnectAccountResult>>();

      if (showCloudPage_)
      {
         pages.add(new NewRSConnectCloudPage());
      }
      pages.add(new NewRSConnectLocalPage());
      return pages;
   }
   
   boolean showCloudPage_;
}
