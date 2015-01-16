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

import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class RSConnectAccountWizard 
   extends Wizard<NewRSConnectAccountInput,NewRSConnectAccountResult>
{

   public RSConnectAccountWizard(SessionInfo session, 
         ProgressOperationWithInput<NewRSConnectAccountResult> operation)
   {
      super("Connect Account", "Select the type of account", 
            new NewRSConnectAccountInput(), operation);
      setOkButtonCaption("Connect Account");
      /*
      if (session.getEnableRStudioConnect())
      {
         addPage(new NewRSConnectCloudPage());
         addPage(new NewRSConnectLocalPage());
      }
      */
      addPage(new NewRSConnectCloudPage());
      addPage(new NewRSConnectLocalPage());
   }
}
