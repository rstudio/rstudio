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

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.satellite.events.WindowClosedEvent;
import org.rstudio.studio.client.rsconnect.events.EnableRStudioConnectUIEvent;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult.AccountType;
import org.rstudio.studio.client.rsconnect.model.RSConnectAuthUser;
import org.rstudio.studio.client.rsconnect.model.RSConnectPreAuthToken;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.views.PublishingPreferencesPane;
import org.rstudio.studio.client.workbench.ui.OptionsLoader;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RSAccountConnector implements WindowClosedEvent.Handler, 
   EnableRStudioConnectUIEvent.Handler
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
         Provider<UIPrefs> pUiPrefs)
   {
      server_ = server;
      display_ = display;
      optionsLoader_ = optionsLoader;
      pUiPrefs_ = pUiPrefs;
      session_ = session;

      events.addHandler(WindowClosedEvent.TYPE, this);
      events.addHandler(EnableRStudioConnectUIEvent.TYPE, this);

      binder.bind(commands, this);
   }
   
   public void showAccountWizard(
         boolean forFirstAccount,
         final OperationWithInput<Boolean> onCompleted)
   {
      if (pUiPrefs_.get().enableRStudioConnect().getGlobalValue())
      {
         showAccountTypeWizard(forFirstAccount, onCompleted);
      }
      else
      {
         showShinyAppsDialog(onCompleted);
      }
   }
   
   @Handler
   public void onRsconnectManageAccounts()
   {
      optionsLoader_.showOptions(PublishingPreferencesPane.class);
   }
   
   // Event handlers ---------------------------------------------------------

   @Override
   public void onWindowClosed(WindowClosedEvent event)
   {
      if (event.getName() == AUTH_WINDOW_NAME)
      {
         notifyAuthClosed();
      }
   }

   @Override
   public void onEnableRStudioConnectUI(EnableRStudioConnectUIEvent event)
   {
      pUiPrefs_.get().enableRStudioConnect().setGlobalValue(event.getEnable());
      pUiPrefs_.get().writeUIPrefs();
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
         final OperationWithInput<Boolean> onCompleted)
   {
      RSConnectAccountWizard wizard = new RSConnectAccountWizard(
            forFirstAccount,
            SessionUtils.showExternalPublishUi(session_, pUiPrefs_.get()),
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
      indicator.onProgress("Checking server connection...");
      server_.validateServerUrl(result.getServerUrl(), 
            new ServerRequestCallback<RSConnectServerInfo>()
      {
         @Override
         public void onResponseReceived(RSConnectServerInfo info)
         {
            if (info.isValid()) 
            {
               getPreAuthToken(result, info, indicator, onConnected);
            }
            else
            {
               display_.showErrorMessage("Server Validation Failed", 
                     "The URL '" + result.getServerUrl() + "' does not " +
                     "appear to belong to a valid server. Please double " +
                     "check the URL, and contact your administrator if " + 
                     "the problem persists.\n\n" +
                     info.getMessage());
               onConnected.execute(AccountConnectResult.Incomplete);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account", 
                  "The server couldn't be validated. " + 
                   error.getMessage());
            onConnected.execute(AccountConnectResult.Incomplete);
         }
      });
   }
   
   private void getPreAuthToken(
         final NewRSConnectAccountResult result,
         final RSConnectServerInfo serverInfo,
         final ProgressIndicator indicator,
         final OperationWithInput<AccountConnectResult> onConnected)
   {
      indicator.onProgress("Setting up an account...");
      server_.getPreAuthToken(serverInfo.getName(), 
            new ServerRequestCallback<RSConnectPreAuthToken>()
      {
         @Override
         public void onResponseReceived(final RSConnectPreAuthToken token)
         {
            indicator.onProgress("Waiting for authentication...");

            // set up pending state -- these will be used to complete the wizard
            // once the user finishes using the wizard to authenticate
            pendingAuthToken_ = token;
            pendingServerInfo_ = serverInfo;
            pendingAuthIndicator_ = indicator;
            pendingOnConnected_ = onConnected;
            pendingWizardResult_ = result;
            
            // prepare a new window with the auth URL loaded
            NewWindowOptions options = new NewWindowOptions();
            options.setName(AUTH_WINDOW_NAME);
            options.setAllowExternalNavigation(true);
            options.setShowDesktopToolbar(false);
            display_.openWebMinimalWindow(
                  pendingAuthToken_.getClaimUrl(), 
                  false, 
                  700, 800, options);
            
            if (!Desktop.isDesktop())
            {
               // in the browser we have no control over the window, so show
               // a dialog to help guide the user
               waitDialog_ = new RSConnectAuthWaitDialog(
                     serverInfo, token);
               waitDialog_.addCloseHandler(new CloseHandler<PopupPanel>()
               {
                  @Override
                  public void onClose(CloseEvent<PopupPanel> arg0)
                  {
                     waitDialog_ = null;
                     notifyAuthClosed();
                  }
               });
               waitDialog_.showModal();
            }
            
            // we'll finish auth automatically when the satellite closes, but we
            // also want to close it ourselves when we detect that auth has
            // completed
            pollForAuthCompleted();
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account", 
                  "The server appears to be valid, but rejected the " + 
                  "request to authorize an account.\n\n"+
                  serverInfo.getInfoString() + "\n" +
                  error.getMessage());
            onConnected.execute(AccountConnectResult.Incomplete);
         }
      });
   }
   
   private void pollForAuthCompleted()
   {
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            // don't keep polling once auth is complete or window is closed
            if (pendingAuthToken_ == null ||
                pendingServerInfo_ == null)
               return false;
            
            // if we're already running a check but it hasn't returned for some
            // reason, just wait for it to finish
            if (runningAuthCompleteCheck_)
               return true;
            
            runningAuthCompleteCheck_ = true;
            server_.getUserFromToken(pendingServerInfo_.getUrl(), 
                  pendingAuthToken_, 
                  new ServerRequestCallback<RSConnectAuthUser>()
                  {
                     @Override
                     public void onResponseReceived(RSConnectAuthUser user)
                     {
                        runningAuthCompleteCheck_ = false;
                        
                        // expected if user hasn't finished authenticating yet,
                        // just wait and try again
                        if (!user.isValidUser())
                           return;
                        
                        // user is valid--cache account info and close the
                        // window
                        pendingAuthUser_ = user;
                        
                        if (Desktop.isDesktop())
                        {
                           // on the desktop, we can close the window by name
                           Desktop.getFrame().closeNamedWindow(
                                 AUTH_WINDOW_NAME);
                        }
                        
                        // on the server, we open a waiting dialog; if that
                        // exists, close it
                        if (waitDialog_ != null)
                        {
                           waitDialog_.closeDialog();
                           waitDialog_ = null;
                        }
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        // ignore this error
                        runningAuthCompleteCheck_ = false;
                     }
                  });
            return true;
         }
      }, 1000);
   }
   
   private void notifyAuthClosed()
   {
      if (pendingAuthToken_ != null &&
          pendingServerInfo_ != null &&
          pendingAuthIndicator_ != null &&
          pendingOnConnected_ != null &&
          pendingWizardResult_ != null)
      {
         if (pendingAuthUser_ == null)
         {
            // the window closed because the user closed it manually--check to
            // see if the token is now valid
            onAuthCompleted(pendingWizardResult_,
                  pendingServerInfo_, 
                  pendingAuthToken_, 
                  pendingAuthIndicator_, 
                  pendingOnConnected_);
         }
         else
         {
            // the window closed because we detected that the token became
            // valid--add the user directly
            onUserAuthVerified(pendingWizardResult_,
                  pendingServerInfo_, 
                  pendingAuthToken_, 
                  pendingAuthUser_, 
                  pendingAuthIndicator_, 
                  pendingOnConnected_);
            
         }
      }
      pendingWizardResult_ = null;
      pendingAuthToken_ = null;
      pendingServerInfo_ = null;
      pendingAuthIndicator_ = null;
      pendingOnConnected_ = null;
   }

   private void onAuthCompleted(
         final NewRSConnectAccountResult result,
         final RSConnectServerInfo serverInfo,
         final RSConnectPreAuthToken token,
         final ProgressIndicator indicator,
         final OperationWithInput<AccountConnectResult> onConnected)
   {
      indicator.onProgress("Validating account...");
      server_.getUserFromToken(serverInfo.getUrl(), token, 
            new ServerRequestCallback<RSConnectAuthUser>()
      {
         @Override 
         public void onResponseReceived(RSConnectAuthUser user)
         {
            if (!user.isValidUser())
            {
               display_.showErrorMessage("Account Not Connected", 
                     "Authentication failed. If you did not cancel " +
                     "authentication, try again, or contact your server " +
                     "administrator for assistance.");
               onConnected.execute(AccountConnectResult.Incomplete);
            }
            else
            {
               onUserAuthVerified(result, serverInfo, token, user, 
                                  indicator, onConnected);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Account Validation Failed", 
                  "RStudio failed to determine whether the account was " +
                  "valid. Try again; if the error persists, contact your " +
                  "server administrator.\n\n" +
                  serverInfo.getInfoString() + "\n" +
                  error.getMessage());
            onConnected.execute(AccountConnectResult.Incomplete);
         }
      });
   }
   
   private void onUserAuthVerified(
         final NewRSConnectAccountResult result,
         final RSConnectServerInfo serverInfo,
         final RSConnectPreAuthToken token,
         final RSConnectAuthUser user,
         final ProgressIndicator indicator,
         final OperationWithInput<AccountConnectResult> onConnected)
   {
      indicator.onProgress("Adding account...");
      String accountName;
      if (result.getAccountNickname().length() > 0)
      {
         // if the user specified a nickname, that trumps everything else
         accountName = result.getAccountNickname();
      }
      else if (user.getUsername().length() > 0) 
      {
         // if we have a username already, just use it 
         accountName = user.getUsername();
      }
      else
      {
         // if we don't have any username, guess one based on user's given name
         // on the server
         accountName = (user.getFirstName().substring(0, 1) + 
               user.getLastName()).toLowerCase();
      }
       
      server_.registerUserToken(serverInfo.getName(), accountName, 
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

   private final GlobalDisplay display_;
   private final RSConnectServerOperations server_;
   private final OptionsLoader.Shim optionsLoader_;
   private final Provider<UIPrefs> pUiPrefs_;
   private final Session session_;
   
   private RSConnectPreAuthToken pendingAuthToken_;
   private RSConnectServerInfo pendingServerInfo_;
   private ProgressIndicator pendingAuthIndicator_;
   private OperationWithInput<AccountConnectResult> pendingOnConnected_;
   private RSConnectAuthUser pendingAuthUser_;
   private NewRSConnectAccountResult pendingWizardResult_;
   private boolean runningAuthCompleteCheck_ = false;
   private RSConnectAuthWaitDialog waitDialog_;
   
   private final static String AUTH_WINDOW_NAME = "rstudio_rsconnect_auth";

}
