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
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.ui.RSAccountConnector;
import org.rstudio.studio.client.rsconnect.ui.RSConnectAccountList;
import org.rstudio.studio.client.server.Int;
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
            setButtonEnabledState();
         }
      });
      accountList_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent arg0)
         {
            setButtonEnabledState();
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

      reconnectButton_ = new ThemedButton("Reconnect...");
      reconnectButton_.getElement().getStyle().setMarginBottom(5, Unit.PX);
      reconnectButton_.setWidth("100%");
      reconnectButton_.setWrapperWidth("100%");
      reconnectButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onReconnect();
         }
      });
      vpanel.add(reconnectButton_);
      
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
      
      setButtonEnabledState();

      accountPanel.add(hpanel);
      add(accountPanel);
      
      // special UI to show when we detect that there are account records but
      // the RSConnect package isn't installed
      final VerticalPanel missingPkgPanel = new VerticalPanel();
      missingPkgPanel.setVisible(false);
      missingPkgPanel.add(new Label(
            "Account records appear to exist, but cannot be viewed because a " +
            "required package is not installed."));
      ThemedButton installPkgs = new ThemedButton("Install Missing Packages");
      installPkgs.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            deps_.withRSConnect("Viewing publish accounts", false, null, 
                                new CommandWithArg<Boolean>() 
            {
               @Override
               public void execute(Boolean succeeded)
               {
                  if (succeeded)
                  {
                     // refresh the account list to show the accounts
                     accountList_.refreshAccountList();
                     
                     // remove the "missing package" UI
                     missingPkgPanel.setVisible(false);
                  }
               }
            });
         }
      });
      installPkgs.getElement().getStyle().setMarginLeft(0, Unit.PX);
      installPkgs.getElement().getStyle().setMarginTop(10, Unit.PX);
      missingPkgPanel.add(installPkgs);
      missingPkgPanel.getElement().getStyle().setMarginBottom(20, Unit.PX);
      add(missingPkgPanel);
      
      final CheckBox chkEnableRSConnect = checkboxPref("Enable publishing to RStudio Connect",
            uiPrefs_.enableRStudioConnect());
      final HorizontalPanel rsconnectPanel = checkBoxWithHelp(chkEnableRSConnect, 
                                                        "rstudio_connect");
      lessSpaced(rsconnectPanel);
      
      add(headerLabel("Settings"));
      CheckBox chkEnablePublishing = checkboxPref("Enable publishing documents and apps", 
            uiPrefs_.showPublishUi());
      chkEnablePublishing.addValueChangeHandler(new ValueChangeHandler<Boolean>(){
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            reloadRequired_ = true;
            rsconnectPanel.setVisible(
                  RSConnect.showRSConnectUI() && event.getValue());
         }
      });
      add(chkEnablePublishing);
      
      if (RSConnect.showRSConnectUI())
         add(rsconnectPanel);
      
      add(checkboxPref("Show diagnostic information when publishing",
            uiPrefs_.showPublishDiagnostics()));
      
      server_.hasOrphanedAccounts(new ServerRequestCallback<Int>()
      {
         @Override
         public void onResponseReceived(Int numOrphans)
         {
            missingPkgPanel.setVisible(numOrphans.getValue() > 0);
         }

         @Override
         public void onError(ServerError error)
         {
            // if we can't determine whether orphans exist, presume that they
            // don't (this state is recoverable as we'll attempt to install
            // rsconnect if necessary and refresh the account list when the user
            // tries to interact with it)
         }
      });
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
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconPublishing2x());
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
         deps_.withRSConnect("Connecting a publishing account", false, null,
                             new CommandWithArg<Boolean>() 
         {
            @Override
            public void execute(Boolean succeeded)
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
   
   private void onReconnect()
   {
      connector_.showReconnectWizard(accountList_.getSelectedAccount(), 
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

   private void setButtonEnabledState()
   {
      disconnectButton_.setEnabled(
            accountList_.getSelectedAccount() != null);
      
      reconnectButton_.setEnabled(
            accountList_.getSelectedAccount() != null &&
            !accountList_.getSelectedAccount().isCloudAccount());
   }
   
   private final GlobalDisplay display_;
   private final UIPrefs uiPrefs_;
   private final RSConnectServerOperations server_;
   private final RSAccountConnector connector_;
   private final DependencyManager deps_;

   private RSConnectAccountList accountList_;
   private ThemedButton connectButton_;
   private ThemedButton disconnectButton_;
   private ThemedButton reconnectButton_;
   private boolean reloadRequired_;
}

