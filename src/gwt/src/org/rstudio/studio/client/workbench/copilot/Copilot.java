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
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponse.CopilotInstallAgentResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponse.CopilotVerifyInstalledResponse;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.copilot.ui.CopilotInstallDialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Copilot
{
   @Inject
   public Copilot(GlobalDisplay display,
                  Commands commands,
                  CopilotCommandBinder binder,
                  CopilotServerOperations server)
   {
      display_ = display;
      commands_ = commands;
      server_ = server;
      
      binder.bind(commands, this);
      commands.copilotInstall().setEnabled(true, true);
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
      ClickHandler handler = (event) -> {
         
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
   public void onCopilotInstall()
   {
      installAgentWithPrompt(new CommandWithArg<Boolean>()
      {
         @Override
         public void execute(Boolean isInstalled)
         {
            // nothing to do
         }
      });
   }
 
   interface CopilotCommandBinder
         extends CommandBinder<Commands, Copilot>
   {
   }
   
   private final GlobalDisplay display_;
   private final Commands commands_;
   private final CopilotServerOperations server_;
}
