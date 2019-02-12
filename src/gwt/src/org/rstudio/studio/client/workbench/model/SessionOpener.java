/*
 * SessionOpener.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Application;
import org.rstudio.studio.client.application.ApplicationAction;
import org.rstudio.studio.client.application.model.ActiveSession;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.Server;

import com.google.gwt.core.client.GWT;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import java.util.function.Consumer;

public class SessionOpener
{
   /**
    * Prepare for navigation to a session
    * @param session
    * @param editExistingLauncherParams if true, shows launcher settings dialog even if we already
    *                                   have launcher settings for this session; otherwise only
    *                                   prompts if we don't have any launcher settings
    * @param navigate url to load
    */
   public void navigateToActiveSession(ActiveSession session,
                                       boolean editExistingLauncherParams,
                                       Consumer<String> navigate)
   {
        navigateToSession(
            session.getUrl(),
            session.getSessionId(),
            session.getLaunchParameters(),
            "", // verb
            editExistingLauncherParams,
            navigate);
   }
   
   /**
    * Prepare for navigation to a session
    * @param sessionUrl
    * @param sessionId
    * @param launchParams
    * @param verb action shown in dialog titles
    * @param editExistingLauncherParams if true, shows launcher settings dialog even if we already
    *                                   have launcher settings for this session; otherwise only
    *                                   prompts if we don't have any launcher settings
    * @param navigate url to load
    */
   public void navigateToSession(String sessionUrl,
                                 String sessionId,
                                 JavaScriptObject launchParams,
                                 String verb,
                                 boolean editExistingLauncherParams,
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
         
         getApplication().navigateWindowWithDelay(nextSessionUrl);
      }
      else
      {
         getApplication().reloadWindowWithDelay(true);
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
      server().getNewSessionUrl(
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
            display().showErrorMessage(
                  "Create Session",
                  "Could not allocate a new session." +
                  error != null && !StringUtil.isNullOrEmpty(error.getMessage()) ?
                        "\n\n" + error.getMessage() : "");
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

   protected Application getApplication()
   {
      return RStudioGinjector.INSTANCE.getApplication();
   }
   
   protected static Server server()
   {
      return RStudioGinjector.INSTANCE.getServer();
   }
   
   protected static GlobalDisplay display()
   {
      return RStudioGinjector.INSTANCE.getGlobalDisplay();
   }
}
