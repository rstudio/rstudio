/*
 * ShinyAppsDeployDialog.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.shiny.ui;

import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.shiny.model.ShinyAppsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;

public class ShinyAppsDeployDialog 
             extends ShinyAppsDialog<ShinyAppsDeploy>
{
   public ShinyAppsDeployDialog(ShinyAppsServerOperations server, 
                                final GlobalDisplay display, 
                                String sourceDir)
   {
      super(server, display, new ShinyAppsDeploy());
      setText("Deploy to ShinyApps");
      setWidth("400px");
      deployButton_ = new ThemedButton("Deploy");
      addCancelButton();
      addOkButton(deployButton_);
      contents_.setSourceDir(sourceDir);
      server_.getShinyAppsAccountList(new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(JsArrayString accounts)
         {
            if (accounts.length() == 0)
            {
               // The user has no accounts connected--hide ourselves and 
               // ask the user to connect an account before we continue.
               hide();
               ShinyAppsConnectAccountDialog dialog = 
                     new ShinyAppsConnectAccountDialog(server_, display_);
               dialog.addCloseHandler(new CloseHandler<PopupPanel>()
               {
                  @Override
                  public void onClose(CloseEvent<PopupPanel> event)
                  {
                     onConnectAccountFinished();
                  }
               });
               dialog.showModal();
            }
            else
            {
               contents_.setAccountList(accounts);
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error retrieving ShinyApps accounts", 
                                     error.getMessage());
            closeDialog();
         }
      });
   }
   
   // Runs when we've finished doing a just-in-time account connection
   private void onConnectAccountFinished()
   {
      server_.getShinyAppsAccountList(new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(JsArrayString accounts)
         {
            if (accounts.length() == 0)
            {
               // The user didn't successfully connect an account--just close 
               // ourselves
               closeDialog();
            }
            else
            {
               // We have an account, show it and re-display oursleves
               contents_.setAccountList(accounts);
               showModal();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error retrieving ShinyApps accounts", 
                                     error.getMessage());
            closeDialog();
         }
      });
   }
   
   private ThemedButton deployButton_;
}