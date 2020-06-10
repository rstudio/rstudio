/*
 * ApplicationClientInit.java
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
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;

import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.SessionInitOptions;
import org.rstudio.studio.client.application.ui.RTimeoutOptions;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class ApplicationClientInit implements RTimeoutOptions.RTimeoutObserver
{
   @Inject
   public ApplicationClientInit(ApplicationServerOperations server,
                                GlobalDisplay globalDisplay)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }
   
   public void execute(final ServerRequestCallback<SessionInfo> requestCallback,
                       final SessionInitOptions options,
                       final boolean retryOnTransmissionError)
   {
      // reset internal state 
      timedOut_ = false;
      cancelTimeoutTimer();
      parentRequest_ = requestCallback;
      
      // send the request
      rpcRequestCallback_ = new ServerRequestCallback<SessionInfo>()
      {
         @Override
         public void cancel()
         {
            super.cancel();
            requestCallback.cancel();
         }

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
            if (!timedOut_ && !cancelled())
            {
               cancelTimeoutTimer();
               
               if ((error.getCode() == ServerError.TRANSMISSION) &&
                    retryOnTransmissionError)
               {
                  // transmission error can occur due to a race 
                  // condition when switching projects or versions, for
                  // this case wait 1000ms then retry
                  new Timer() {
                     @Override
                     public void run()
                     {
                        // retry (specify flag to ensure we only retry once)
                        execute(requestCallback, options, false);
                     }
                  }.schedule(1000);
               }
               else
               {
                  requestCallback.onError(error);
               }
            }
         }
      };

      server_.clientInit(GWT.getHostPageBaseURL(), options, rpcRequestCallback_);
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
   
   @Override
   public void onReload()
   {
      // keep trying (reload to clear out any crufty app or networking state)
      reloadWithDelay(1);
   }

   @Override
   public void onTerminate()
   {
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
            parentRequest_.onError(error);
         }
      });
   }

   @Override
   public void onSafeMode()
   {
      // cancel the in-flight request, if any
      if (rpcRequestCallback_ != null)
      {
         rpcRequestCallback_.cancel();
         rpcRequestCallback_ = null;
      }

      // abort the pending launch
      server_.abort(null, new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void response)
         {
            // re-attempt the launch with the new options
            final SessionInitOptions options = SessionInitOptions.create(
                  SessionInitOptions.RESTORE_WORKSPACE_NO,
                  SessionInitOptions.RUN_RPROFILE_NO);
            execute(parentRequest_, options, true);
         }
         
         @Override
         public void onError(ServerError error)
         {
            parentRequest_.onError(error);
         }
      });
   }

   private final ApplicationServerOperations server_;
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   private Timer timeoutTimer_ = null;
   private boolean timedOut_ = false;
   private ServerRequestCallback<SessionInfo> rpcRequestCallback_;
   private ServerRequestCallback<SessionInfo> parentRequest_;
}
