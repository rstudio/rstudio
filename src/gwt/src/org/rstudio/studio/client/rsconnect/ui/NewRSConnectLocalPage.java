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

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardIntermediatePage;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectPreAuthToken;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.user.client.ui.Widget;

public class NewRSConnectLocalPage 
            extends WizardIntermediatePage<NewRSConnectAccountInput,
                                           NewRSConnectAccountResult>
{

   public NewRSConnectLocalPage()
   {
      super("RStudio Connect", 
            "A local service running inside your organization. Publish and " +
            "collaborate privately and securely.",
            "RStudio Connect Account",
            RSConnectResources.INSTANCE.localAccountIcon(), 
            RSConnectResources.INSTANCE.localAccountIconLarge(),
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
      NewRSConnectAccountResult result = collectInput();
      if (!validate(result))
      {
         onResult.execute(result);
         return;
      }

      setIntermediateResult(result);
      indicator.onProgress("Checking server connection...");
      server_.validateServerUrl(getIntermediateResult().getServerUrl(), 
            new ServerRequestCallback<RSConnectServerInfo>()
      {
         @Override
         public void onResponseReceived(RSConnectServerInfo info)
         {
            if (info.isValid()) 
            {
               getPreAuthToken(getIntermediateResult(), info, indicator, 
                     onResult);
            }
            else
            {
               display_.showErrorMessage("Server Validation Failed", 
                     "The URL '" + getIntermediateResult().getServerUrl() + 
                     "' does not appear to belong to a valid server. Please " +
                     "double check the URL, and contact your administrator " +
                     "if the problem persists.\n\n" +
                     info.getMessage());
               onResult.execute(null);
               indicator.clearProgress();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account", 
                  "The server couldn't be validated. " + 
                   error.getMessage());
            onResult.execute(null);
            indicator.clearProgress();
         }
      });
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
      server_ = initData.getServer();
      display_ = initData.getDisplay();
   }

   @Override
   protected NewRSConnectAccountResult collectInput()
   {
      return new NewRSConnectAccountResult("", local_.getServerUrl().trim(), 
            local_.getAccountName().trim());
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return input != null &&
            input.getServerUrl() != null && 
            !input.getServerUrl().isEmpty();
   }

   private void getPreAuthToken(
         final NewRSConnectAccountResult result,
         final RSConnectServerInfo serverInfo,
         final ProgressIndicator indicator,
         final OperationWithInput<NewRSConnectAccountResult> onResult)
   {
      indicator.onProgress("Setting up an account...");
      server_.getPreAuthToken(serverInfo.getName(), 
            new ServerRequestCallback<RSConnectPreAuthToken>()
      {
         @Override
         public void onResponseReceived(final RSConnectPreAuthToken token)
         {
            NewRSConnectAccountResult newResult = result;
            newResult.setPreAuthToken(token);
            newResult.setServerInfo(serverInfo);
            onResult.execute(newResult);
            indicator.clearProgress();
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account", 
                  "The server appears to be valid, but rejected the " + 
                  "request to authorize an account.\n\n"+
                  serverInfo.getInfoString() + "\n" +
                  error.getMessage());
            indicator.clearProgress();
            onResult.execute(null);
         }
      });
   }

   private RSConnectServerOperations server_;
   private GlobalDisplay display_;
   private RSConnectLocalAccount local_;
}