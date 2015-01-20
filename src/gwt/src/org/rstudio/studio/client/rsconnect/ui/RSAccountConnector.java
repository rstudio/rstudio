/*
 * RSAccountConnector.java
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

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult.AccountType;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;

public class RSAccountConnector
{
   public RSAccountConnector(RSConnectServerOperations server,
         GlobalDisplay display,
         Session session)
   {
      server_ = server;
      display_ = display;
      session_ = session;
   }
   
   public void showAccountWizard(
         final OperationWithInput<Boolean> onCompleted)
   {
      RSConnectAccountWizard wizard = new RSConnectAccountWizard(
            session_.getSessionInfo(),
            new ProgressOperationWithInput<NewRSConnectAccountResult>()
      {
         @Override
         public void execute(NewRSConnectAccountResult input,
               final ProgressIndicator indicator)
         {
            connectNewAccount(input, new OperationWithInput<Boolean>()
            {
               @Override
               public void execute(Boolean input)
               {
                  if (input)
                  {
                     indicator.onCompleted();
                     onCompleted.execute(true);
                  }
               }
            });
         }
      });
      wizard.showModal();
   }

   public void connectNewAccount(
         NewRSConnectAccountResult result,
         OperationWithInput<Boolean> onConnected)
   {
      if (result.getAccountType() == AccountType.RSConnectCloudAccount)
      {
         connectCloudAccount(result, onConnected);
      }
      else
      {
         connectLocalAccount(result, onConnected);
      }
   }
   
   public void connectCloudAccount(
         final NewRSConnectAccountResult result,
         final OperationWithInput<Boolean> onConnected)
   {
      // get command and substitute rsconnect for shinyapps
      final String cmd = result.getCloudSecret().replace("shinyapps::", 
                                                         "rsconnect::");
      if (!cmd.startsWith("rsconnect::setAccountInfo"))
      {
         display_.showErrorMessage("Error Connecting Account", 
               "The pasted command should start with " + 
               "rsconnect::setAccountInfo. If you're having trouble, try " + 
               "connecting your account manually; type " +
               "?rsconnect::setAccountInfo at the R console for help.");
         onConnected.execute(false);
      }
      server_.connectRSConnectAccount(cmd, 
            new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            onConnected.execute(true);
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account",  
                  "The command '" + cmd + "' failed. You can set up an " + 
                  "account manually by using rsconnect::setAccountInfo; " +
                  "type ?rsconnect::setAccountInfo at the R console for " +
                  "more information.");
            onConnected.execute(false);
         }
      });
   }

   public void connectLocalAccount(
         final NewRSConnectAccountResult result,
         final OperationWithInput<Boolean> onConnected)
   {
      server_.validateServerUrl(result.getServerUrl(), 
            new ServerRequestCallback<RSConnectServerInfo>()
      {
         @Override
         public void onResponseReceived(RSConnectServerInfo info)
         {
            if (info.isValid()) 
            {
               display_.showMessage(GlobalDisplay.MSG_INFO, 
                                    "Server Validated", info.getAbout());
               onConnected.execute(true);
            }
            else
            {
               display_.showErrorMessage("Server Validation Failed", 
                     "The URL '" + result.getServerUrl() + "' does not " +
                     "appear to belong to a valid server. Please double " +
                     "check the URL, and contact your administrator if " + 
                     "the problem persists.\n\n" +
                     "Error: " + info.getMessage());
               onConnected.execute(false);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account", 
                  "The server couldn't be validated. " + 
                   error.getMessage());
            onConnected.execute(false);
         }
      });
   }
   
   private final GlobalDisplay display_;
   private final RSConnectServerOperations server_;
   private final Session session_;
}
