package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.satellite.events.WindowClosedEvent;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectAuthUser;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
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
            RSConnectResources.INSTANCE.localAccountIcon(), 
            RSConnectResources.INSTANCE.localAccountIconLarge());

      // listen for window close events (this page needs to know when the user
      // closes the auth dialog
      RStudioGinjector.INSTANCE.getEventBus().addHandler(
            WindowClosedEvent.TYPE, 
            this);
   }

   @Override
   public void focus()
   {
   }

   @Override
   public void setIntermediateResult(NewRSConnectAccountResult result) 
   {
      result_ = result;
      contents_.setClaimLink(result.getServerInfo().getName(),
            result.getPreAuthToken().getClaimUrl());
   }
   
   @Override
   public void onActivate(ProgressIndicator indicator) 
   {
      if (waitingForAuth_ || result_ == null)
         return;

      // begin waiting for user to complete authentication
      waitingForAuth_ = true;
      contents_.showWaiting();
      
      // prepare a new window with the auth URL loaded
      NewWindowOptions options = new NewWindowOptions();
      options.setName(AUTH_WINDOW_NAME);
      options.setAllowExternalNavigation(true);
      options.setShowDesktopToolbar(false);
      display_.openWebMinimalWindow(
            result_.getPreAuthToken().getClaimUrl(),
            false, 
            700, 800, options);
      
      // close the window automatically when authentication finishes
      pollForAuthCompleted();
   }

   @Override
   public void onWindowClosed(WindowClosedEvent event)
   {
      if (event.getName().equals(AUTH_WINDOW_NAME))
      {
         waitingForAuth_ = false;
         
         // check to see if the user successfully authenticated
         onAuthCompleted();
      }
   }
   
   @Override
   public void onWizardClosing()
   {
      // this will cause us to stop polling for auth (if we haven't already)
      waitingForAuth_ = false;
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
            onActivate(null);
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
            if (!waitingForAuth_)
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
                        waitingForAuth_ = false;
                        
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

   private NewRSConnectAccountResult result_;
   private RSConnectServerOperations server_;
   private GlobalDisplay display_;
   private RSConnectAuthWait contents_;
   private boolean waitingForAuth_ = false;
   private boolean runningAuthCompleteCheck_ = false;
   private final static String AUTH_WINDOW_NAME = "rstudio_rsconnect_auth";
}
