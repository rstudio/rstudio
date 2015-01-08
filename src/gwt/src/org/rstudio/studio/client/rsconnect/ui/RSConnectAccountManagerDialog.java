/*
 * RSConnectAccountManagerDialog.java
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;

public class RSConnectAccountManagerDialog 
             extends RSConnectDialog<RSConnectAccountManager>
{
   public RSConnectAccountManagerDialog(RSConnectServerOperations server, 
                                        final GlobalDisplay display)
   {
      super(server, display, new RSConnectAccountManager());
      setText("Configure Accounts");
      setWidth("300px");

      connectButton_ = new ThemedButton("Connect...");
      connectButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onConnect();
         }
      });
      disconnectButton_ = new ThemedButton("Disconnect");
      disconnectButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onDisconnect();
         }
      });
      disconnectButton_.setEnabled(false);
      doneButton_ = new ThemedButton("Done");
      doneButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onDone();
         }
      });
      addLeftButton(disconnectButton_);
      addLeftButton(connectButton_);
      addOkButton(doneButton_);

      contents_.addAccountSelectionChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            setDisconnectButtonEnabledState();
         }
      });
      
      refreshAccountList();
   }

   private void onDisconnect()
   {
      final String account = contents_.getSelectedAccount();
      if (account == null)
      {
         display_.showErrorMessage("Error Disconnection Account", 
               "Please select an account to disconnect.");
         return;
      }
      display_.showYesNoMessage(
            GlobalDisplay.MSG_QUESTION, 
            "Confirm Remove Account", 
            "Are you sure you want to disconnect the '" + 
              account + 
            "' account? This won't delete the account on the server.", 
            false, 
            new Operation()
            {
               @Override
               public void execute()
               {
                  onConfirmDisconnect(account);
               }
            }, null, null, "Disconnect Account", "Cancel", false);
   }
   
   private void onConfirmDisconnect(final String accountName)
   {
      server_.removeRSConnectAccount(accountName, new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            contents_.removeAccount(accountName);
            setDisconnectButtonEnabledState();
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Disconnecting Account", 
                                      error.getMessage());
         }
      });
   }
   
   private void onConnect()
   {
      RSConnectConnectAccountDialog dialog = 
            new RSConnectConnectAccountDialog(server_, display_);
      dialog.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            refreshAccountList();
         }
      });
      dialog.showModal();
   }
   
   private void onDone()
   {
      closeDialog();
   }
   
   private void setDisconnectButtonEnabledState()
   {
      disconnectButton_.setEnabled(
            contents_.getSelectedAccount() != null);
   }
   
   private void refreshAccountList()
   {
      server_.getRSConnectAccountList(new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(JsArrayString accounts)
         {
            contents_.setAccountList(accounts);
            setDisconnectButtonEnabledState();
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error retrieving accounts", 
                                     error.getMessage());
         }
      });
   }
   
   private ThemedButton connectButton_;
   private ThemedButton disconnectButton_;
   private ThemedButton doneButton_;
}
