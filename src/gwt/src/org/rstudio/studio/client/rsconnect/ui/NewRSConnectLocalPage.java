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

import java.util.ArrayList;

import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

public class NewRSConnectLocalPage 
            extends WizardNavigationPage<NewRSConnectAccountInput,
                                         NewRSConnectAccountResult>
{

   public NewRSConnectLocalPage()
   {
      super("RStudio Connect", 
            "A local service running inside your organization. Publish and " +
            "collaborate privately and securely.",
            "Project Type",
            RSConnectAccountResources.INSTANCE.localAccountIcon(), 
            RSConnectAccountResources.INSTANCE.localAccountIconLarge(), 
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