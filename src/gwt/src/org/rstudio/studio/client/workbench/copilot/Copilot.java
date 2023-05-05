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
import org.rstudio.studio.client.workbench.copilot.model.CopilotVerifyResult;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Copilot
{
   @Inject
   public Copilot(GlobalDisplay display,
                  Commands commands,
                  CopilotServerOperations server)
   {
      display_ = display;
      commands_ = commands;
      server_ = server;
      
      // COMMANDS.bind(commands, this);
   }
   
   public void ensureAgentInstalled(CommandWithArg<Boolean> callback)
   {
      String progressLabel = "Verifying copilot installation...";
      server_.copilotVerifyInstalled(
            new DelayedProgressRequestCallback<CopilotVerifyResult>(progressLabel)
      {
         @Override
         protected void onSuccess(CopilotVerifyResult response)
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
      String[] messageLines = {
            "GitHub Copilot is not installed. Would you like to install it?",
            "",
            "Your use of GitHub Copilot is governed by the GitHub Copilot terms of service.",
            "<a href=\"https://docs.github.com/en/site-policy/github-terms/github-terms-for-additional-products-and-features#github-copilot\">Click Here</a> for more details."
      };
      
      String message = StringUtil.join(messageLines, "\n");
      display_.showYesNoMessage(
            MessageDisplay.MSG_INFO,
            "Install Copilot",
            message,
            false,
            () -> installAgent(callback),
            () -> callback.execute(false),
            true);
   }
   
   private void installAgent(CommandWithArg<Boolean> callback)
   {
      String progressLabel = "Installing copilot agent...";
      server_.copilotInstallAgent(
            new DelayedProgressRequestCallback<CopilotVerifyResult>(progressLabel)
            {
               @Override
               protected void onSuccess(CopilotVerifyResult response)
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
   
   private final GlobalDisplay display_;
   private final Commands commands_;
   private final CopilotServerOperations server_;
}
