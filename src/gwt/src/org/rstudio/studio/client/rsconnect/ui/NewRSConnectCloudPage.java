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

import java.util.ArrayList;

import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

public class NewRSConnectCloudPage 
            extends WizardNavigationPage<NewRSConnectAccountInput,
                                         NewRSConnectAccountResult>
{

   public NewRSConnectCloudPage()
   {
      super("ShinyApps.io", 
            "A cloud service run by RStudio. Publish Shiny applications " +
            "and interactive documents to the Internet.",
            "Account Type",
            RSConnectAccountResources.INSTANCE.cloudAccountIcon(), 
            RSConnectAccountResources.INSTANCE.cloudAccountIconLarge(), 
            createPages());
   }

  
   private static ArrayList<WizardPage<NewRSConnectAccountInput, 
                                       NewRSConnectAccountResult>>
                                createPages()
   {   
      ArrayList<WizardPage<NewRSConnectAccountInput, 
                           NewRSConnectAccountResult>> pages = 
            new ArrayList<WizardPage<NewRSConnectAccountInput, 
                                     NewRSConnectAccountResult>>();
      
      return pages;
   }
}
