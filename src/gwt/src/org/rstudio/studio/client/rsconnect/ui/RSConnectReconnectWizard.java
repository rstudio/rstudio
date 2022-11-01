/*
 * RSConnectReconnectWizard.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.RsconnectConstants;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;

public class RSConnectReconnectWizard
   extends Wizard<NewRSConnectAccountInput,NewRSConnectAccountResult>
{
   public RSConnectReconnectWizard(
         RSConnectServerOperations server,
         GlobalDisplay display,
         RSConnectAccount account,
         String serverUrl,
         ProgressOperationWithInput<NewRSConnectAccountResult> onCompleted) 
   {
      super(constants_.reconnectAccount(), constants_.connectAccount(), Roles.getDialogRole(),
            new NewRSConnectAccountInput(server, display), 
            new NewRSConnectAuthPage(),
            onCompleted);

      // hook up ok button visibility toggle
      NewRSConnectAuthPage firstPage = 
            (NewRSConnectAuthPage) super.getFirstPage();
      firstPage.setOkButtonVisible(new OperationWithInput<Boolean>()
            {
               @Override
               public void execute(Boolean input)
               {
                  setOkButtonVisible(input);
               }

            });
      NewRSConnectAuthPage authPage = (NewRSConnectAuthPage)getFirstPage();
      NewRSConnectAccountResult result = new NewRSConnectAccountResult(
            account.getServer(), serverUrl, account.getName());
      authPage.setIntermediateResult(result);
   }
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}

