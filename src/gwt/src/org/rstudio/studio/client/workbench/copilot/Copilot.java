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
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponse.CopilotCodeCompletionResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponse.CopilotInstallAgentResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponse.CopilotVerifyInstalledResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotCompletion;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.copilot.ui.CopilotInstallDialog;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import jsinterop.base.JsArrayLike;

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
   public void onCopilotInstall()
   {
      installAgentWithPrompt(installed -> {});
   }
   
   @Handler
   void onCopilotCodeCompletion()
   {
      SourceColumn column = sourceColumnManager_.get().getActive();
      if (column == null)
         return;
      
      EditingTarget target = column.getActiveEditor();
      if (target == null)
         return;
      
      if (!(target instanceof TextEditingTarget))
         return;
      
      TextEditingTarget textTarget = (TextEditingTarget) target;
      textTarget.withSavedDoc(() ->
      {
         textTarget.withActiveEditor((editor) ->
         {
            ProgressIndicator indicator =
                  globalDisplay_.getProgressIndicator("Error requesting code suggestions");
            
            final Timer indicatorTimer = Timers.singleShot(1000, () -> 
            {
               indicator.onProgress("Waiting for Copilot...");
            });
            
            server_.copilotCodeCompletion(
                  textTarget.getId(),
                  editor.getCursorRow(),
                  editor.getCursorColumn(),
                  new ServerRequestCallback<CopilotCodeCompletionResponse>()
                  {
                     @Override
                     public void onResponseReceived(CopilotCodeCompletionResponse response)
                     {
                        indicatorTimer.cancel();
                        indicator.onCompleted();
                        
                        JsArrayLike<CopilotCompletion> completions = response.result.completions;
                        for (CopilotCompletion completion : completions.asList())
                        {
                           String text = completion.text;
                           if (!StringUtil.isNullOrEmpty(text))
                           {
                              Range insertRange = Range.create(
                                    completion.range.start.line,
                                    completion.range.start.character,
                                    completion.range.end.line,
                                    completion.range.end.character);
                              editor.replaceRange(insertRange, text);
                              break;
                           }
                       }
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        indicatorTimer.cancel();
                        indicator.onError(error.getMessage());
                        Debug.logError(error);
                     }
                     
                  });
         });
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
