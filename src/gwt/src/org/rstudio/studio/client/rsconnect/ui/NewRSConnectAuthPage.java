package org.rstudio.studio.client.rsconnect.ui;

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
      super("", "", "Verifying Account", 
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

      indicator.onProgress("Checking server connection...");
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
               contents_.showError("Server Validation Failed", 
                     "The URL '" + result_.getServerUrl() + 
                     "' does not appear to belong to a valid server. Please " +
                     "double check the URL, and contact your administrator " +
                     "if the problem persists.\n\n" +
                     info.getMessage());
               indicator.clearProgress();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            contents_.showError("Error Connecting Account", 
                  "The server couldn't be validated. " + 
                   error.getMessage());
            indicator.clearProgress();
         }
      });
   }


   @Override
   public void onWindowClosed(WindowClosedEvent event)
   {
      if (event.getName().equals(AUTH_WINDOW_NAME))
      {
         waitingForAuth_.setValue(false, true);
         
         // check to see if the user successfully authenticated
         onAuthCompleted();
      }
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
                        
                        if (Desktop.isDesktop())
                        {
                           // on the desktop, we can close the window by name
                           Desktop.getFrame().closeNamedWindow(
                                 AUTH_WINDOW_NAME);
                        }
                       
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
               contents_.showError("Account Not Connected", 
                     "Authentication failed. If you did not cancel " +
                     "authentication, try again, or contact your server " +
                     "administrator for assistance.");
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
            contents_.showError("Account Validation Failed", 
                  "RStudio failed to determine whether the account was " +
                  "valid. Try again; if the error persists, contact your " +
                  "server administrator.\n\n" +
                  result_.getServerInfo().getInfoString() + "\n" +
                  error.getMessage());
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
                  result_.getAuthUser().getFirstName().substring(0, 1) + 
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
                  if (canSpawnAuthenticationWindow())
                  {
                     NewWindowOptions options = new NewWindowOptions();
                     options.setName(AUTH_WINDOW_NAME);
                     options.setAllowExternalNavigation(true);
                     options.setShowDesktopToolbar(false);
                     display_.openWebMinimalWindow(
                           result_.getPreAuthToken().getClaimUrl(),
                           false, 
                           700, 800, options);
                  }
                  else
                  {
                     Desktop.getFrame().browseUrl(result_.getPreAuthToken().getClaimUrl());
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
   
   private boolean canSpawnAuthenticationWindow()
   {
      if (!Desktop.isDesktop())
         return true;
      
      if (Desktop.getFrame().isCentOS())
         return false;
      
      return true;
   }
   
   private OperationWithInput<Boolean> setOkButtonVisible_;
   
   private NewRSConnectAccountResult result_;
   private RSConnectServerOperations server_;
   private GlobalDisplay display_;
   private RSConnectAuthWait contents_;
   private Value<Boolean> waitingForAuth_ = new Value<Boolean>(false);
   private boolean runningAuthCompleteCheck_ = false;
   private ProgressIndicator wizardIndicator_;

   public final static String AUTH_WINDOW_NAME = "rstudio_rsconnect_auth";
}
