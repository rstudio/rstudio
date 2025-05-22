/*
 * NewRSConnectAuthPage.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.rsconnect.ui;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.satellite.events.WindowClosedEvent;
import org.rstudio.studio.client.rsconnect.RsconnectConstants;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectAuthUser;
import org.rstudio.studio.client.rsconnect.model.RSConnectPreAuthToken;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

public class NewRSConnectAuthPage 
   extends WizardPage<NewRSConnectAccountInput, 
                      NewRSConnectAccountResult>
   implements WindowClosedEvent.Handler
{
   public NewRSConnectAuthPage()
   {
      super("", "", constants_.verifyingAccount(),
            new ImageResource2x(RSConnectResources.INSTANCE.localAccountIcon2x()), 
            new ImageResource2x(RSConnectResources.INSTANCE.localAccountIconLarge2x()));

      // listen for window close events (this page needs to know when the user
      // closes the auth dialog
      RStudioGinjector.INSTANCE.getEventBus().addHandler(
            WindowClosedEvent.TYPE, 
            this);

      waitingForAuth_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> waiting)
         {
            if (setOkButtonVisible_ != null)
               setOkButtonVisible_.execute(!waiting.getValue());
         }
      });
   }

   @Override
   public void focus()
   {
   }

   @Override
   public void setIntermediateResult(NewRSConnectAccountResult result) 
   {
      result_ = result;
   }
   
   @Override
   public void onActivate(final ProgressIndicator indicator) 
   {
      if (waitingForAuth_.getValue() || result_ == null)
         return;
      
      // save reference to parent wizard's progress indicator for retries
      wizardIndicator_ = indicator;

      indicator.onProgress(constants_.checkingServerConnection());
      server_.validateServerUrl(result_.getServerUrl(), 
            new ServerRequestCallback<RSConnectServerInfo>()
      {
         @Override
         public void onResponseReceived(RSConnectServerInfo info)
         {
            if (info.isValid()) 
            {
               result_.setServerInfo(info);
               getPreAuthToken(indicator);
            }
            else
            {
               contents_.showError(constants_.serverValidationFailed(),
                     constants_.serverValidationFailedMessage(result_.getServerUrl(),
                     info.getMessage()));
               indicator.clearProgress();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            contents_.showError(constants_.errorConnectingAccount(),
                  constants_.serverCouldntBeValidated(
                   error.getMessage()));
            indicator.clearProgress();
         }
      });
   }

   @Override
   public void onWindowClosed(WindowClosedEvent event)
   {
   }
   
   @Override
   public void onWizardClosing()
   {
      // this will cause us to stop polling for auth (if we haven't already)
      waitingForAuth_.setValue(false, true);
   }
   
   public void setOkButtonVisible(OperationWithInput<Boolean> okButtonVisible)
   {
      setOkButtonVisible_ = okButtonVisible;
   }

   @Override
   protected Widget createWidget()
   {
      contents_ = new RSConnectAuthWait();
      contents_.setOnTryAgain(new Command()
      {
         @Override
         public void execute()
         {
            onActivate(wizardIndicator_);
         }
      });
      return contents_;
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
      return result_;
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return input != null && input.getAuthUser() != null;
   }

   private void pollForAuthCompleted()
   {
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            // don't keep polling once auth is complete or window is closed
            if (!waitingForAuth_.getValue())
               return false;
            
            // avoid re-entrancy--if we're already running a check but it hasn't
            // returned for some reason, just wait for it to finish
            if (runningAuthCompleteCheck_)
               return true;
            
            runningAuthCompleteCheck_ = true;
            server_.getUserFromToken(result_.getServerInfo().getUrl(), 
                  result_.getPreAuthToken(), 
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
                        result_.setAuthUser(user);
                        waitingForAuth_.setValue(false, true);
                        
                        onUserAuthVerified();
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

   @SuppressWarnings("unused")
   private void onAuthCompleted()
   {
      server_.getUserFromToken(result_.getServerInfo().getUrl(), 
            result_.getPreAuthToken(), 
            new ServerRequestCallback<RSConnectAuthUser>()
      {
         @Override 
         public void onResponseReceived(RSConnectAuthUser user)
         {
            if (!user.isValidUser())
            {
               contents_.showError(constants_.accountNotConnected(),
                     constants_.accountNotConnectedMessage());
            }
            else
            {
               result_.setAuthUser(user);
               onUserAuthVerified();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            contents_.showError(constants_.accountValidationFailed(),
                  constants_.accountValidationFailedMessage(result_.getServerInfo().getInfoString(),
                  error.getMessage()));
         }
      });
   }
   
   private void onUserAuthVerified()
   {
      // set the account nickname if we didn't already have one
      if (result_.getAccountNickname().length() == 0)
      {
         if (result_.getAuthUser().getUsername().length() > 0) 
         {
            // if we have a username already, just use it 
            result_.setAccountNickname(
                  result_.getAuthUser().getUsername());
         }
         else
         {
            // if we don't have any username, guess one based on user's given name
            // on the server
            result_.setAccountNickname(
                  StringUtil.substring(result_.getAuthUser().getFirstName(), 0, 1) + 
                  result_.getAuthUser().getLastName().toLowerCase());
         }
      }
      
      contents_.showSuccess(result_.getServerName(), 
            result_.getAccountNickname());
   }

   private void getPreAuthToken(ProgressIndicator indicator)
   {
      getPreAuthToken(result_, result_.getServerInfo(), indicator, 
            new OperationWithInput<NewRSConnectAccountResult>()
            {
               @Override
               public void execute(NewRSConnectAccountResult input)
               {
                  // do nothing if no result returned
                  if (input == null)
                     return;
                  
                  // save intermediate result
                  result_ = input;

                  contents_.setClaimLink(result_.getServerInfo().getName(),
                        result_.getPreAuthToken().getClaimUrl());

                  // begin waiting for user to complete authentication
                  waitingForAuth_.setValue(true, true);
                  contents_.showWaiting();
                  
                  // prepare a new window with the auth URL loaded
                  if (Desktop.hasDesktopFrame())
                  {
                     Desktop.getFrame().browseUrl(StringUtil.notNull(result_.getPreAuthToken().getClaimUrl()));
                  }
                  else
                  {
                     NewWindowOptions options = new NewWindowOptions();
                     options.setAllowExternalNavigation(true);
                     options.setShowDesktopToolbar(false);
                     display_.openWebMinimalWindow(
                           result_.getPreAuthToken().getClaimUrl(),
                           false, 
                           700, 800, options);
                  }
                  
                  // close the window automatically when authentication finishes
                  pollForAuthCompleted();
               }
            });
   }

   private void getPreAuthToken(
         final NewRSConnectAccountResult result,
         final RSConnectServerInfo serverInfo,
         final ProgressIndicator indicator,
         final OperationWithInput<NewRSConnectAccountResult> onResult)
   {
      indicator.onProgress(constants_.settingUpAccount());
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
            display_.showErrorMessage(constants_.errorConnectingAccount(),
                  constants_.errorConnectingAccountMessage(serverInfo.getInfoString(),
                          error.getMessage()));
            indicator.clearProgress();
            onResult.execute(null);
         }
      });
   }
   
   private OperationWithInput<Boolean> setOkButtonVisible_;
   
   private NewRSConnectAccountResult result_;
   private RSConnectServerOperations server_;
   private GlobalDisplay display_;
   private RSConnectAuthWait contents_;
   private Value<Boolean> waitingForAuth_ = new Value<>(false);
   private boolean runningAuthCompleteCheck_ = false;
   private ProgressIndicator wizardIndicator_;
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
