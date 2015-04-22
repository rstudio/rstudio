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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.ui.RSAccountConnector;
import org.rstudio.studio.client.rsconnect.ui.RSConnectAccountList;
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
                                    RSAccountConnector connector,
                                    UIPrefs prefs,
                                    DependencyManager deps)
   {
      reloadRequired_ = false;
      display_ = globalDisplay;
      uiPrefs_ = prefs;
      server_ = server;
      connector_ = connector;
      deps_ = deps;
      
      VerticalPanel accountPanel = new VerticalPanel();
      Label accountLabel = headerLabel("Publishing Accounts");
      HorizontalPanel hpanel = new HorizontalPanel();
      
      accountPanel.add(accountLabel);
      
      accountList_ = new RSConnectAccountList(server, globalDisplay, true, 
            true);
      accountList_.setHeight("200px");
      accountList_.setWidth("300px");
      accountList_.getElement().getStyle().setMarginBottom(15, Unit.PX);
      accountList_.getElement().getStyle().setMarginLeft(3, Unit.PX);
      hpanel.add(accountList_);
      
      accountList_.setOnRefreshCompleted(new Operation() 
      {
         @Override
         public void execute()
         {
            setDisconnectButtonEnabledState();
         }
      });
      
      VerticalPanel vpanel = new VerticalPanel();
      hpanel.add(vpanel);

      connectButton_ = new ThemedButton("Connect...");
      connectButton_.getElement().getStyle().setMarginBottom(5, Unit.PX);
      connectButton_.setWidth("100%");
      connectButton_.setWrapperWidth("100%");
      connectButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onConnect();
         }
      });
      vpanel.add(connectButton_);
      disconnectButton_ = new ThemedButton("Disconnect");
      disconnectButton_.setWidth("100%");
      disconnectButton_.setWrapperWidth("100%");
      disconnectButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onDisconnect();
         }
      });
      vpanel.add(disconnectButton_);
      setDisconnectButtonEnabledState();

      accountPanel.add(hpanel);
      add(accountPanel);
      
      add(headerLabel("Settings"));
      CheckBox chkEnablePublishing = checkboxPref("Enable publishing apps and documents", 
            uiPrefs_.showPublishUi());
      chkEnablePublishing.addValueChangeHandler(new ValueChangeHandler<Boolean>(){
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            reloadRequired_ = true;
         }
      });
      add(chkEnablePublishing);
   }

   @Override
   protected void initialize(RPrefs rPrefs)
   {
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean reload = super.onApply(rPrefs);

      return reload || reloadRequired_;
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
      final RSConnectAccount account = accountList_.getSelectedAccount();
      if (account == null)
      {
         display_.showErrorMessage("Error Disconnecting Account", 
               "Please select an account to disconnect.");
         return;
      }
      display_.showYesNoMessage(
            GlobalDisplay.MSG_QUESTION, 
            "Confirm Remove Account", 
            "Are you sure you want to disconnect the '" + 
              account.getName() + 
            "' account on '" + 
              account.getServer() + "'" + 
            "? This won't delete the account on the server.", 
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
   
   private void onConfirmDisconnect(final RSConnectAccount account)
   {
      server_.removeRSConnectAccount(account.getName(), 
            account.getServer(), new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            accountList_.refreshAccountList();
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
      // if there's already at least one account connected, the requisite
      // packages must be installed
      if (accountList_.getAccountCount() > 0)
      {
         showAccountWizard();
      }
      else
      {
         deps_.withRSConnect("Connecting a publishing account", null, new Command() 
         {
            @Override
            public void execute()
            {
               // refresh the account list in case there are accounts already on
               // the system (e.g. package was installed at one point and some
               // metadata remains)
               accountList_.refreshAccountList();

               showAccountWizard();
            }
         });
      }
   }
   
   private void showAccountWizard()
   {
      connector_.showAccountWizard(false, true, 
            new OperationWithInput<Boolean>() 
      {
         @Override
         public void execute(Boolean successful)
         {
            if (successful)
            {
               accountList_.refreshAccountList();
            }
         }
      });
   }

   private void setDisconnectButtonEnabledState()
   {
      disconnectButton_.setEnabled(
            accountList_.getSelectedAccount() != null);
   }
   
   private final GlobalDisplay display_;
   private final UIPrefs uiPrefs_;
   private final RSConnectServerOperations server_;
   private final RSAccountConnector connector_;
   private final DependencyManager deps_;

   private RSConnectAccountList accountList_;
   private ThemedButton connectButton_;
   private ThemedButton disconnectButton_;
   private boolean reloadRequired_;
}

