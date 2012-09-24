/*
 * ApplicationClientInit.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class ApplicationClientInit
{
   @Inject
   public ApplicationClientInit(ApplicationServerOperations server,
                                GlobalDisplay globalDisplay)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }
   
   public void execute(final ServerRequestCallback<SessionInfo> requestCallback)
   {
      // reset internal state 
      timedOut_ = false;
      timeoutTimer_ = null;
      
      // send the request
      final ServerRequestCallback<SessionInfo> rpcRequestCallback = 
                                 new ServerRequestCallback<SessionInfo>() {
         @Override
         public void onResponseReceived(SessionInfo sessionInfo)
         {
            if (!timedOut_)
            {
               cancelTimeoutTimer();
               requestCallback.onResponseReceived(sessionInfo);
            }
         }
         @Override
         public void onError(ServerError error)
         {
            if (!timedOut_)
            {
               cancelTimeoutTimer();
               requestCallback.onError(error);
            }
         }                                    
      };
      server_.clientInit(rpcRequestCallback);
                                    
      
      // wait for 60 seconds then ask the user if they want to issue an 
      // interrupt to the server
      int timeoutMs = 60000;
      timeoutTimer_ = new Timer() {
         public void run()
         {  
            // set timed out flag
            timedOut_ = true;
            
            // cancel our request
            rpcRequestCallback.cancel();
            
            // ask the user if they want to attempt to interrupt the server
            globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION, 
                
               // caption
               "Initializing RStudio", 
               
               // message
               "The RStudio server is taking a long time to respond. It is " +
               "possible that your R session has become unresponsive. " +
               "Do you want to terminate the currently running R session?", 
         
               // don't include cancel
               false, 
               
               // Yes operation
               new Operation() { public void execute() {
                  
                  // call interrupt then call this method back on success
                  server_.abort(null, new ServerRequestCallback<Void>() {

                     @Override
                     public void onResponseReceived(Void response)
                     {
                        // reload the application
                        reloadWithDelay(1000);
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        // if we get an error during interrupt then just
                        // forward the error on to the original handler
                        requestCallback.onError(error);     
                     }
                     
                  });
                     
               }}, 
               
               // No operation
               new Operation() { public void execute() {
                  
                  // keep trying (reload to clear out any crufty app
                  // or networking state)
                  reloadWithDelay(1);
               }},
               
               // Cancel operation (none)
               null,

               "Terminate R",
               "Keep Waiting",
               
               // default to No
               false);              
         }
      };
      
      // activate the timer
      timeoutTimer_.schedule(timeoutMs); 
   }
   
   private void reloadWithDelay(int delayMs)
   {
      // need a delay so the server has time to fully process the
      // interrupt and go offline
      Timer delayTimer = new Timer() {
         public void run()
         {
            Window.Location.reload();
         }     
      };
      delayTimer.schedule(delayMs);
   }
   
   private void cancelTimeoutTimer()
   {
      if (timeoutTimer_ != null)
      {
         timeoutTimer_.cancel();
         timeoutTimer_ = null;
      }
   }
   
   private final ApplicationServerOperations server_;
   private final GlobalDisplay globalDisplay_ ;
   private Timer timeoutTimer_ = null;
   private boolean timedOut_ = false;
}
