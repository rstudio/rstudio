/*
 * Copilot.java
 *
 * Copyright (C) 2023 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.copilot;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotInstallAgentResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotSignInResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotSignOutResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotStatusResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotVerifyInstalledResponse;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.copilot.ui.CopilotInstallDialog;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class Copilot
{
   @Inject
   public Copilot(GlobalDisplay display,
                  Commands commands,
                  Provider<SourceColumnManager> sourceColumnManager,
                  GlobalDisplay globalDisplay,
                  CopilotCommandBinder binder,
                  CopilotServerOperations server)
   {
      display_ = display;
      commands_ = commands;
      sourceColumnManager_ = sourceColumnManager;
      globalDisplay_ = globalDisplay;
      server_ = server;
      
      binder.bind(commands, this);
   }
   
   public void ensureAgentInstalled(CommandWithArg<Boolean> callback)
   {
      String progressLabel = "Verifying copilot installation...";
      server_.copilotVerifyInstalled(
            new DelayedProgressRequestCallback<CopilotVerifyInstalledResponse>(progressLabel)
      {
         @Override
         protected void onSuccess(CopilotVerifyInstalledResponse response)
         {
            if (response.installed)
            {
               callback.execute(true);
            }
            else
            {
               installAgentWithPrompt(callback);
            }
         }
      });
   }
   
   private void installAgentWithPrompt(CommandWithArg<Boolean> callback)
   {
      ClickHandler handler = (event) ->
      {
         installAgent(callback);
      };
      
      CopilotInstallDialog dialog = new CopilotInstallDialog(handler);
      dialog.showModal();
   }
   
   private void installAgent(CommandWithArg<Boolean> callback)
   {
      String progressLabel = "Installing copilot agent...";
      server_.copilotInstallAgent(
            new DelayedProgressRequestCallback<CopilotInstallAgentResponse>(progressLabel)
            {
               @Override
               protected void onSuccess(CopilotInstallAgentResponse response)
               {
                  if (response.installed)
                  {
                     display_.showMessage(
                           MessageDisplay.MSG_INFO,
                           "GitHub Copilot",
                           "GitHub Copilot successfully installed.");
                  }
                  else
                  {
                     display_.showErrorMessage("An error occurred while installing GitHub Copilot.");
                  }
                  
                  callback.execute(response.installed);
               }
            });
   }
   
   @Handler
   public void onCopilotInstallAgent()
   {
      installAgentWithPrompt(installed -> {});
   }
   
   @Handler
   public void onCopilotSignIn()
   {
      server_.copilotSignIn(new DelayedProgressRequestCallback<CopilotSignInResponse>("Signing in...")
      {
         @Override
         protected void onSuccess(CopilotSignInResponse response)
         {
            // TODO
            Debug.logObject(response);
         }
      });
   }
   
   @Handler
   public void onCopilotSignOut()
   {
      server_.copilotSignOut(new DelayedProgressRequestCallback<CopilotSignOutResponse>("Signing out...")
      {
         @Override
         protected void onSuccess(CopilotSignOutResponse response)
         {
            // TODO
            Debug.logObject(response);
         }
      });
   }
   
   @Handler
   public void onCopilotStatus()
   {
      server_.copilotStatus(new DelayedProgressRequestCallback<CopilotStatusResponse>("Checking status...")
      {
         @Override
         protected void onSuccess(CopilotStatusResponse response)
         {
            // TODO
            Debug.logObject(response);
         }
      });
   }
   
   interface CopilotCommandBinder
         extends CommandBinder<Commands, Copilot>
   {
   }
   
   private final GlobalDisplay display_;
   private final Commands commands_;
   private final Provider<SourceColumnManager> sourceColumnManager_;
   private final GlobalDisplay globalDisplay_;
   private final CopilotServerOperations server_;
}
