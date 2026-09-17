/*
 * SessionOpener.java
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
package org.rstudio.studio.client.workbench.model;

import java.util.function.Consumer;

import javax.inject.Inject;

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
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleRestartRCompletedEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.Command;
import com.google.inject.Provider;
import com.google.inject.Singleton;

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
                  constants_.createSessionCaption(),
                  constants_.createSessionMessage() +
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
   public void suspendForRestart(SuspendOptions options,
                                 Command onCompleted,
                                 Command onFailed)
   {
      pServer_.get().suspendForRestart(options, new VoidServerRequestCallback()
      {
         @Override
         protected void onSuccess()
         {
            waitForSessionJobExit(
                  () -> waitForSessionRestart(onCompleted),
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
   
   protected void waitForSessionJobExit(Command onClosed, Command onFailure)
   {
      // for regular sessions, no job to wait for
      onClosed.execute();
   }
   
   protected void waitForSessionRestart(Command onCompleted)
   {
      sendPing(200, RESTART_TIMEOUT_MS, onCompleted);
   }
   
   /**
    * Ping the session until it answers, then announce that the restart is done.
    *
    * The budget is wall-clock rather than a tick count: a ping can legitimately
    * sit unanswered for a long time, because rserver holds an RPC aimed at a
    * session it is still relaunching (up to rsession-proxy-max-wait-secs). A
    * ping that fails is likewise not the end of the wait -- an early one lands
    * while the old session is still shutting down, and it is the retry after it
    * that reaches the new session.
    *
    * @param delayMs interval between ping attempts.
    * @param timeoutMs how long to keep trying before giving up.
    * @param onCompleted run exactly once: on an answered ping, or on timeout.
    */
   // package-private so SessionOpenerTests can drive it with its own budget
   void sendPing(int delayMs,
                 final int timeoutMs,
                 final Command onCompleted)
   {
      final long startMs = System.currentTimeMillis();

      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         private boolean completed_ = false;
         private boolean restartAnnounced_ = false;
         private boolean pingInFlight_ = false;
         
         @Override
         public boolean execute()
         {
            // if we've already completed, return false
            if (completed_)
            {
               return false;
            }
            
            // if we're out of time, give up -- but still signal completion
            // so callers (e.g. the restart flow) don't get stuck waiting forever
            if (System.currentTimeMillis() - startMs > timeoutMs)
            {
               Debug.logWarning("Error connecting with session.");
               complete();
               return false;
            }
            
            // a ping we sent is still outstanding; wait for its response rather
            // than piling on another request
            if (pingInFlight_)
            {
               return true;
            }
            
            pingInFlight_ = true;
            pServer_.get().ping(new VoidServerRequestCallback()
            {
               @Override
               protected void onSuccess()
               {
                  pingInFlight_ = false;
                  
                  // the session answered, so the restart really is done --
                  // announce it even if the loop above already timed out
                  announceRestartCompleted();
                  complete();
               }
               
               @Override
               protected void onFailure()
               {
                  pingInFlight_ = false;

                  // keep trying; the next attempt may reach the new session
               }
            });
            
            // keep trying until completion is signaled
            return true;
         }
         
         private void announceRestartCompleted()
         {
            if (restartAnnounced_)
               return;

            restartAnnounced_ = true;
            pEventBus_.get().fireEvent(new ConsoleRestartRCompletedEvent());
         }
         
         private void complete()
         {
            if (completed_)
               return;

            completed_ = true;

            if (onCompleted != null)
               onCompleted.execute();
         }
         
      }, delayMs);
   }
   
   // How long a restart has to produce a session that answers a ping. Generous
   // on purpose: on Server the relaunch runs behind rserver's proxy, and the
   // session still has to restore the workspace and search path before it
   // services RPCs.
   private static final int RESTART_TIMEOUT_MS = 60000;

   // injected
   protected final Provider<Application> pApplication_;
   protected final Provider<GlobalDisplay> pDisplay_;
   protected final Provider<ApplicationServerOperations> pServer_;
   protected final Provider<EventBus> pEventBus_;
   private static final ModelConstants constants_ = GWT.create(ModelConstants.class);
}
