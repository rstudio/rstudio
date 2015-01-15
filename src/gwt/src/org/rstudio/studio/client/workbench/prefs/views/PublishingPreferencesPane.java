/*
 * PublishingPreferencesPane.java
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

package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.ui.RSConnectConnectAccountDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class PublishingPreferencesPane extends PreferencesPane
{
   @Inject
   public PublishingPreferencesPane(GlobalDisplay globalDisplay,
                                    RSConnectServerOperations server,
                                    UIPrefs prefs)
   {
      display_ = globalDisplay;
      uiPrefs_ = prefs;
      server_ = server;
      
      Label accountLabel = new Label("Connected Accounts");
      accountLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
      add(accountLabel);
      
      accountList_ = new WidgetListBox<Label>();
      accountList_.setHeight("200px");
      add(accountList_);
      
      HorizontalPanel panel = new HorizontalPanel();
      connectButton_ = new ThemedButton("Connect...");
      connectButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onConnect();
         }
      });
      panel.add(connectButton_);
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
      panel.add(disconnectButton_);
      add(panel);
   }

   @Override
   protected void initialize(RPrefs rPrefs)
   {
      refreshAccountList();
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      return super.onApply(rPrefs);
   }

   
   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconPublishing();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Publishing";
   }

   private void onDisconnect()
   {
      // TODO: read selected account
      final String account = "";
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
            // TODO: remove selected account from UI
            // contents_.removeAccount(accountName);
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

   private void refreshAccountList()
   {
      server_.getRSConnectAccountList(new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(JsArrayString accounts)
         {
            accountList_.clearItems();
            for (int i = 0; i < accounts.length(); i++)
            {
               accountList_.addItem(new Label(accounts.get(i)), true);
            }
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
   
   private void setDisconnectButtonEnabledState()
   {
      disconnectButton_.setEnabled(
            accountList_.getSelectedIndex() >= 0);
   }
   
   private final GlobalDisplay display_;
   private final UIPrefs uiPrefs_;
   private final RSConnectServerOperations server_;
   private WidgetListBox<Label> accountList_;
   private ThemedButton connectButton_;
   private ThemedButton disconnectButton_;
}

