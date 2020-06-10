/*
 * RSAccountConnector.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.rsconnect.events.EnableRStudioConnectUIEvent;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult.AccountType;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectAuthUser;
import org.rstudio.studio.client.rsconnect.model.RSConnectPreAuthToken;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerEntry;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.prefs.views.PublishingPreferencesPane;
import org.rstudio.studio.client.workbench.ui.OptionsLoader;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RSAccountConnector implements EnableRStudioConnectUIEvent.Handler
{
   public interface Binder
   extends CommandBinder<Commands, RSAccountConnector> {}

   // possible results of attempting to connect an account
   enum AccountConnectResult
   {
      Incomplete,
      Successful,
      Failed
   }

   @Inject
   public RSAccountConnector(RSConnectServerOperations server,
         GlobalDisplay display,
         Commands commands,
         Binder binder,
         OptionsLoader.Shim optionsLoader,
         EventBus events,
         Session session,
         Provider<UserPrefs> pUiPrefs,
         Provider<UserState> pState,
         Satellite satellite)
   {
      server_ = server;
      display_ = display;
      optionsLoader_ = optionsLoader;
      pUserPrefs_ = pUiPrefs;
      pUserState_ = pState;
      session_ = session;

      events.addHandler(EnableRStudioConnectUIEvent.TYPE, this);

      binder.bind(commands, this);

      // register satellite callback
      if (!Satellite.isCurrentWindowSatellite())
         exportManageAccountsCallback();
   }

   public void showAccountWizard(
         boolean forFirstAccount,
         boolean withCloudOption,
         final OperationWithInput<Boolean> onCompleted)
   {
      if (pUserState_.get().enableRsconnectPublishUi().getGlobalValue())
      {
         showAccountTypeWizard(forFirstAccount, withCloudOption, onCompleted);
      }
      else
      {
         showShinyAppsDialog(onCompleted);
      }
   }

   public void showReconnectWizard(
         final RSConnectAccount account,
         final OperationWithInput<Boolean> onCompleted)
   {
      server_.getServerUrls(new ServerRequestCallback<JsArray<RSConnectServerEntry>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectServerEntry> entries)
         {
            boolean found = false;
            for (int i = 0; i < entries.length(); i++)
            {
               if (entries.get(i).getName().equalsIgnoreCase(
                     account.getServer()))
               {
                  RSConnectReconnectWizard wizard =
                        new RSConnectReconnectWizard(
                        server_,
                        display_,
                        account,
                        entries.get(i).getUrl(),
                        new ProgressOperationWithInput<NewRSConnectAccountResult>()
                  {
                     @Override
                     public void execute(NewRSConnectAccountResult input,
                           final ProgressIndicator indicator)
                     {
                        processDialogResult(input, indicator, onCompleted);
                     }
                  });
                  wizard.showModal();
                  found = true;
                  break;
               }
            }

            if (!found)
            {
               display_.showErrorMessage("Server Information Not Found",
                     "RStudio could not retrieve server information for " +
                     "the selected account.");
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Can't Find Servers",
                  "RStudio could not retrieve server information.");
         }
      });
   }

   @Handler
   public void onRsconnectManageAccounts()
   {
      if (Satellite.isCurrentWindowSatellite())
      {
         callSatelliteManageAccounts();
      }
      else
      {
         optionsLoader_.showOptions(PublishingPreferencesPane.class, true);
      }
   }

   // Event handlers ---------------------------------------------------------

   @Override
   public void onEnableRStudioConnectUI(EnableRStudioConnectUIEvent event)
   {
      pUserState_.get().enableRsconnectPublishUi().setGlobalValue(event.getEnable());
      pUserState_.get().writeState();
   }

   // Private methods --------------------------------------------------------

   private void showShinyAppsDialog(
         final OperationWithInput<Boolean> onCompleted)
   {
      RSConnectCloudDialog dialog = new RSConnectCloudDialog(
      new ProgressOperationWithInput<NewRSConnectAccountResult>()
      {
         @Override
         public void execute(NewRSConnectAccountResult input,
                             ProgressIndicator indicator)
         {
            processDialogResult(input, indicator, onCompleted);
         }
      },
      new Operation()
      {
         @Override
         public void execute()
         {
            onCompleted.execute(false);
         }
      });
      dialog.showModal();
   }

   private void showAccountTypeWizard(
         boolean forFirstAccount,
         boolean withCloudOption,
         final OperationWithInput<Boolean> onCompleted)
   {
      // ignore if wizard is already up
      if (showingWizard_)
         return;

      RSConnectAccountWizard wizard = new RSConnectAccountWizard(
            server_,
            display_,
            forFirstAccount,
            withCloudOption &&
               SessionUtils.showExternalPublishUi(session_, pUserState_.get()),
            new ProgressOperationWithInput<NewRSConnectAccountResult>()
      {
         @Override
         public void execute(NewRSConnectAccountResult input,
               final ProgressIndicator indicator)
         {
            processDialogResult(input, indicator, onCompleted);
         }
      });
      wizard.setGlassEnabled(true);
      wizard.showModal();

      // remember whether wizard is showing
      showingWizard_ = true;
      wizard.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> arg0)
         {
            showingWizard_ = false;
         }
      });
   }

   private void processDialogResult(final NewRSConnectAccountResult input,
         final ProgressIndicator indicator,
         final OperationWithInput<Boolean> onCompleted)
   {
      connectNewAccount(input, indicator,
            new OperationWithInput<AccountConnectResult>()
      {
         @Override
         public void execute(AccountConnectResult input)
         {
            if (input == AccountConnectResult.Failed)
            {
               // the connection failed--take down the dialog entirely
               // (we do this when retrying doesn't make sense)
               onCompleted.execute(false);
               indicator.onCompleted();
            }
            else if (input == AccountConnectResult.Incomplete)
            {
               // the connection didn't finish--take down the progress and
               // allow retry
               indicator.clearProgress();
            }
            else if (input == AccountConnectResult.Successful)
            {
               // successful account connection--mark finished
               onCompleted.execute(true);
               indicator.onCompleted();
            }
         }
      });
   }

   private void connectNewAccount(
         NewRSConnectAccountResult result,
         ProgressIndicator indicator,
         OperationWithInput<AccountConnectResult> onConnected)
   {
      if (result.getAccountType() == AccountType.RSConnectCloudAccount)
      {
         connectCloudAccount(result, indicator, onConnected);
      }
      else
      {
         connectLocalAccount(result, indicator, onConnected);
      }
   }

   private void connectCloudAccount(
         final NewRSConnectAccountResult result,
         final ProgressIndicator indicator,
         final OperationWithInput<AccountConnectResult> onConnected)
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
         onConnected.execute(AccountConnectResult.Incomplete);
      }
      indicator.onProgress("Connecting account...");
      server_.connectRSConnectAccount(cmd,
            new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            onConnected.execute(AccountConnectResult.Successful);
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account",
                  "The command '" + cmd + "' failed. You can set up an " +
                  "account manually by using rsconnect::setAccountInfo; " +
                  "type ?rsconnect::setAccountInfo at the R console for " +
                  "more information.");
            onConnected.execute(AccountConnectResult.Failed);
         }
      });
   }

   private void connectLocalAccount(
         final NewRSConnectAccountResult result,
         final ProgressIndicator indicator,
         final OperationWithInput<AccountConnectResult> onConnected)

   {
      indicator.onProgress("Adding account...");
      final RSConnectAuthUser user = result.getAuthUser();
      final RSConnectServerInfo serverInfo = result.getServerInfo();
      final RSConnectPreAuthToken token = result.getPreAuthToken();

      server_.registerUserToken(serverInfo.getName(),
            result.getAccountNickname(),
            user.getId(), token, new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void result)
         {
            onConnected.execute(AccountConnectResult.Successful);
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Account Connect Failed",
                  "Your account was authenticated successfully, but could " +
                  "not be connected to RStudio. Make sure your installation " +
                  "of the 'rsconnect' package is correct for the server " +
                  "you're connecting to.\n\n" +
                  serverInfo.getInfoString() + "\n" +
                  error.getMessage());
            onConnected.execute(AccountConnectResult.Failed);
         }
      });
   }

   private final native void exportManageAccountsCallback()/*-{
      var rsAccount = this;
      $wnd.rsManageAccountsFromRStudioSatellite = $entry(
         function() {
            rsAccount.@org.rstudio.studio.client.rsconnect.ui.RSAccountConnector::onRsconnectManageAccounts()();
         }
      );
   }-*/;

   private final native void callSatelliteManageAccounts()/*-{
      $wnd.opener.rsManageAccountsFromRStudioSatellite();
   }-*/;

   private final GlobalDisplay display_;
   private final RSConnectServerOperations server_;
   private final OptionsLoader.Shim optionsLoader_;
   private final Provider<UserPrefs> pUserPrefs_;
   private final Provider<UserState> pUserState_;
   private final Session session_;

   private boolean showingWizard_;
}
