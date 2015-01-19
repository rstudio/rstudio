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


import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class NewRSConnectLocalPage 
            extends WizardPage<NewRSConnectAccountInput,
                               NewRSConnectAccountResult>
{

   public NewRSConnectLocalPage()
   {
      super("RStudio Connect", 
            "A local service running inside your organization. Publish and " +
            "collaborate privately and securely.",
            "RStudio Connect Account",
            RSConnectAccountResources.INSTANCE.localAccountIcon(), 
            RSConnectAccountResources.INSTANCE.localAccountIconLarge());
   }

   @Override
   public void focus()
   {
   }

   @Override
   protected Widget createWidget()
   {
      return new Label("NYI");
   }

   @Override
   protected void initialize(NewRSConnectAccountInput initData)
   {
   }

   @Override
   protected NewRSConnectAccountResult collectInput()
   {
      return new NewRSConnectAccountResult("", "", "");
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return true;
   }
}