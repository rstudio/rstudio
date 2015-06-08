/*
 * RSConnectAuthenticator.java
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
package org.rstudio.studio.client.rsconnect;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.satellite.events.WindowClosedEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectAuthUser;
import org.rstudio.studio.client.rsconnect.model.RSConnectPreAuthToken;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.ui.RSConnectAuthWait;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

public class RSConnectAuthenticator implements WindowClosedEvent.Handler
{
   public RSConnectAuthenticator(RSConnectServerOperations server,
                                 RSConnectServerInfo serverInfo,
                                 String accountNickname,
                                 RSConnectAuthWait contents)
   {
      contents_ = contents;
      server_ = server;
      serverInfo_ = serverInfo;
      accountNickname_ = accountNickname;
      waitingForAuth_ = false;
      runningAuthCompleteCheck_ = false;
      display_ = RStudioGinjector.INSTANCE.getGlobalDisplay();
      RStudioGinjector.INSTANCE.getEventBus().addHandler(
            WindowClosedEvent.TYPE, this);
   }
   
   public void authenticate(
         final OperationWithInput<Boolean> onCompleted)
   {
      onCompleted_ = onCompleted;
      server_.getPreAuthToken(serverInfo_.getName(), 
            new ServerRequestCallback<RSConnectPreAuthToken>()
      {
         @Override
         public void onResponseReceived(final RSConnectPreAuthToken token)
         {
            contents_.setClaimLink(serverInfo_.getName(),
                  token.getClaimUrl());
            token_ = token;

            // begin waiting for user to complete authentication
            waitingForAuth_ = true;
            contents_.showWaiting();
            
            // prepare a new window with the auth URL loaded
            NewWindowOptions options = new NewWindowOptions();
            options.setName(AUTH_WINDOW_NAME);
            options.setAllowExternalNavigation(true);
            options.setShowDesktopToolbar(false);
            display_.openWebMinimalWindow(
                  token.getClaimUrl(),
                  false, 
                  700, 800, options);
            
            // close the window automatically when authentication finishes
            pollForAuthCompleted();
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Connecting Account", 
                  "The server appears to be valid, but rejected the " + 
                  "request to authorize an account.\n\n"+
                  serverInfo_.getInfoString() + "\n" +
                  error.getMessage());
            onCompleted.execute(false);
         }
      });
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
   
   public void stopPolling()
   {
      waitingForAuth_ = false;
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
            server_.getUserFromToken(serverInfo_.getUrl(), token_, 
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
                        waitingForAuth_ = false;
                        
                        if (Desktop.isDesktop())
                        {
                           // on the desktop, we can close the window by name
                           Desktop.getFrame().closeNamedWindow(
                                 AUTH_WINDOW_NAME);
                        }
                       
                        user_ = user;
                        registerAccount();
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
      server_.getUserFromToken(serverInfo_.getUrl(), token_, 
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
               onCompleted_.execute(false);
            }
            else
            {
               user_ = user;
               registerAccount();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            contents_.showError("Account Validation Failed", 
                  "RStudio failed to determine whether the account was " +
                  "valid. Try again; if the error persists, contact your " +
                  "server administrator.\n\n" +
                  serverInfo_.getInfoString() + "\n" +
                  error.getMessage());
         }
      });
   }
   
   private void registerAccount()
   {
      server_.registerUserToken(serverInfo_.getName(), 
            accountNickname_, 
            user_.getId(), token_, new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void result)
         {
            onCompleted_.execute(true);
         }

         @Override
         public void onError(ServerError error)
         {
            contents_.showError("Account Connect Failed", 
                  "Your account was authenticated successfully, but could " +
                  "not be connected to RStudio. Make sure your installation " +
                  "of the 'rsconnect' package is correct for the server " + 
                  "you're connecting to.\n\n" +
                  serverInfo_.getInfoString() + "\n" +
                  error.getMessage());
            onCompleted_.execute(false);
         }
      });
   }

   private final RSConnectServerOperations server_;
   private final RSConnectAuthWait contents_;
   private final RSConnectServerInfo serverInfo_;
   private final GlobalDisplay display_;
   private final String accountNickname_;

   private RSConnectPreAuthToken token_;
   private RSConnectAuthUser user_;

   private boolean waitingForAuth_;
   private boolean runningAuthCompleteCheck_;
   private final static String AUTH_WINDOW_NAME = "rstudio_rsconnect_auth";
   private OperationWithInput<Boolean> onCompleted_;
}
