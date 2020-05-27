/*
 * SessionOpener.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.Command;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.Application;
import org.rstudio.studio.client.application.ApplicationAction;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ActiveSession;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.SuspendOptions;
import org.rstudio.studio.client.common.GlobalDisplay;

import com.google.gwt.core.client.GWT;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleRestartRCompletedEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import javax.inject.Inject;
import java.util.function.Consumer;

@Singleton
public class SessionOpener
{
   @Inject
   public SessionOpener(Provider<Application> pApplication,
                        Provider<GlobalDisplay> pGlobalDisplay,
                        Provider<ApplicationServerOperations> pServer,
                        Provider<EventBus> pEventBus)
   {
      pApplication_ = pApplication;
      pDisplay_ = pGlobalDisplay;
      pServer_ = pServer;
      pEventBus_ = pEventBus;
   }

   /**
    * Prepare for navigation to a session
    * @param session
    * @param navigate url to load
    */
   public void navigateToActiveSession(ActiveSession session,
                                       Consumer<String> navigate)
   {
        navigateToSession(session.getUrl(), navigate);
   }
   
   /**
    * Prepare for navigation to a session
    * @param sessionUrl
    * @param navigate url to load
    */
   protected void navigateToSession(String sessionUrl,
                                    Consumer<String> navigate)
   {
      navigate.accept(sessionUrl);
   }
   
   /**
    * Switch to a session
    * @param nextSessionUrl session to switch to
    */
   public void switchSession(String nextSessionUrl)
   {
      // if we are switching projects then reload after a delay (to allow
      // the R session to fully exit on the server)
      if (!StringUtil.isNullOrEmpty(nextSessionUrl))
      {
         // forward any query string parameters (e.g. the edit_published
         // parameter might follow an action=switch_project)
         String query = ApplicationAction.getQueryStringWithoutAction();
         if (query.length() > 0)
            nextSessionUrl = nextSessionUrl + "?" + query;
         
         pApplication_.get().navigateWindowWithDelay(nextSessionUrl);
      }
      else
      {
         pApplication_.get().reloadWindowWithDelay(true);
      }
   }
   
   /**
    * Create a new session with supplied launchParams and
    * and prepare to navigate to it
    */
   public void navigateToNewSession(boolean isProject,
                                    String directory,
                                    RVersionSpec rVersion,
                                    JavaScriptObject launchParams,
                                    Consumer<String> navigate)
   {
      pServer_.get().getNewSessionUrl(
                    GWT.getHostPageBaseURL(),
                    isProject,
                    directory,
                    rVersion,
                    launchParams,
        new ServerRequestCallback<String>() {

         @Override
         public void onResponseReceived(String url)
         {
            navigate.accept(url);
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            pDisplay_.get().showErrorMessage(
                  "Create Session",
                  "Could not allocate a new session." +
                        (!StringUtil.isNullOrEmpty(error.getMessage()) ?
                        "\n\n" + error.getMessage() : ""));
         }
      });
   }
   
   /**
    * Create a new session and prepare to navigate to it. Prompts for
    * launch params if necessary.
    */
   public void navigateToNewSession(boolean isProject,
                                    String directory,
                                    RVersionSpec rVersion,
                                    Consumer<String> navigate)
   {
      navigateToNewSession(isProject, directory, rVersion, null, /*launchParams*/ navigate);
   }
   
   /**
    * Suspend and restart current session
    */
   public void suspendForRestart(final String afterRestartCommand,
                                 SuspendOptions options,
                                 Command onCompleted,
                                 Command onFailed)
   {
      pServer_.get().suspendForRestart(options,
                                new VoidServerRequestCallback() {
         @Override
         protected void onSuccess()
         {
            waitForSessionJobExit(afterRestartCommand,
                                  () -> waitForSessionRestart(afterRestartCommand, onCompleted),
                                  onFailed);
         }
         @Override
         protected void onFailure()
         {
            onFailed.execute();
         }
      });
   }

   /**
    *  Streams the session job's current connection details
    */
   public void getJobConnectionStatus(final ServerRequestCallback<String> connectionStatusCallback)
   {
   }
   
   protected void waitForSessionJobExit(final String afterRestartCommand,
                                        Command onClosed, Command onFailure)
   {
      // for regular sessions, no job to wait for
      onClosed.execute();
   }
   
   protected void waitForSessionRestart(final String afterRestartCommand, Command onCompleted)
   {
      sendPing(afterRestartCommand, 200, 25, onCompleted);
   }
   
   private void sendPing(final String afterRestartCommand,
                         int delayMs,
                         final int maxRetries,
                         final Command onCompleted)
   {
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

         private int retries_ = 0;
         private boolean pingDelivered_ = false;
         private boolean pingInFlight_ = false;
         
         @Override
         public boolean execute()
         {
            // if we've already delivered the ping or our retry count
            // is exhausted then return false
            if (pingDelivered_ || (++retries_ > maxRetries))
               return false;
            
            if (!pingInFlight_)
            {
               pingInFlight_ = true;
               pServer_.get().ping(new VoidServerRequestCallback() {
                  @Override
                  protected void onSuccess()
                  {
                     pingInFlight_ = false;
                     if (!pingDelivered_)
                     {
                        pingDelivered_ = true;
   
                        // issue after restart command
                        if (!StringUtil.isNullOrEmpty(afterRestartCommand))
                        {
                           pEventBus_.get().fireEvent(
                                 new SendToConsoleEvent(afterRestartCommand,
                                       true, true));
                        }
                        // otherwise make sure the console knows we
                        // restarted (ensure prompt and set focus)
                        else
                        {
                           pEventBus_.get().fireEvent(
                                 new ConsoleRestartRCompletedEvent());
                        }
                     }
                     
                     if (onCompleted != null)
                        onCompleted.execute();
                  }
                  
                  @Override
                  protected void onFailure()
                  {
                     pingInFlight_ = false;
                     
                     if (onCompleted != null)
                        onCompleted.execute();
                  }
               });
            }
            
            // keep trying until the ping is delivered
            return true;
         }
         
      }, delayMs);
   }
   
   // injected
   protected final Provider<Application> pApplication_;
   protected final Provider<GlobalDisplay> pDisplay_;
   protected final Provider<ApplicationServerOperations> pServer_;
   protected final Provider<EventBus> pEventBus_;
}
