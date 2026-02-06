/*
 * ConsoleCommandSuggestor.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.console;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantNextCommandSuggestionResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantNextCommandSuggestionSuggestion;
import org.rstudio.studio.client.workbench.assistant.server.AssistantServerOperations;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputEvent;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ConsoleCommandSuggestor implements
   ConsoleInputEvent.Handler,
   ConsoleWriteInputEvent.Handler,
   ConsoleWriteOutputEvent.Handler,
   ConsoleWriteErrorEvent.Handler
{
   @Inject
   public ConsoleCommandSuggestor(EventBus eventBus,
                                  AssistantServerOperations server,
                                  Provider<SourceColumnManager> pSourceColumnManager)
   {
      Debug.log("[NCS] ConsoleCommandSuggestor created");
      server_ = server;
      pSourceColumnManager_ = pSourceColumnManager;

      eventBus.addHandler(ConsoleInputEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteInputEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteOutputEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteErrorEvent.TYPE, this);

      cooldownTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            suggestionInProgress_ = false;
         }
      };
   }

   public void setConsoleDisplay(DocDisplay display)
   {
      consoleDisplay_ = display;
   }

   @Override
   public void onConsoleInput(ConsoleInputEvent event)
   {
      pendingSourceDocumentId_ = event.getConsole();
      Debug.log("[NCS] onConsoleInput: console='" + event.getConsole() + "' input='" + event.getInput() + "'");
   }

   @Override
   public void onConsoleWriteInput(ConsoleWriteInputEvent event)
   {
      String input = event.getInput();
      Debug.log("[NCS] onConsoleWriteInput: " + input);
      if (StringUtil.isNullOrEmpty(input))
         return;

      input = input.trim();
      if (input.isEmpty())
         return;

      lastCommand_ = input;
      lastSourceDocumentId_ = pendingSourceDocumentId_;
      pendingSourceDocumentId_ = null;
      outputBuffer_ = new StringBuilder();
   }

   @Override
   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      String output = event.getOutput();
      if (output != null)
      {
         outputBuffer_.append(output);
      }
   }

   @Override
   public void onConsoleWriteError(ConsoleWriteErrorEvent event)
   {
      String error = event.getError();
      Debug.log("[NCS] onConsoleWriteError: " + error + " lastCommand_=" + (lastCommand_ != null));
      if (error != null)
      {
         outputBuffer_.append(error);
      }

      if (lastCommand_ != null)
      {
         String command = lastCommand_;
         String output = outputBuffer_.toString();
         boolean isError = true;
         String sourceDocumentId = lastSourceDocumentId_;

         if (error != null && error.toLowerCase().contains("warning"))
         {
            isError = false;
         }

         lastCommand_ = null;
         lastSourceDocumentId_ = null;

         if (!suggestionInProgress_)
         {
            requestSuggestion(command, output, isError, sourceDocumentId);
         }
      }
   }

   private void requestSuggestion(String command, String output, boolean isError, String sourceDocumentId)
   {
      Debug.log("[NCS] requestSuggestion called");

      suggestionInProgress_ = true;
      cooldownTimer_.schedule(COOLDOWN_MS);

      String sourceContextType = "console";
      String documentUri = "";
      String documentContent = "";

      Debug.log("[NCS] sourceDocumentId='" + sourceDocumentId + "'");
      if (!StringUtil.isNullOrEmpty(sourceDocumentId))
      {
         sourceContextType = "document";
         SourceColumnManager sourceColumnManager = pSourceColumnManager_.get();
         if (sourceColumnManager != null)
         {
            documentUri = sourceColumnManager.getActiveDocPath();
            String content = sourceColumnManager.getActiveDocContents();
            if (content != null)
            {
               documentContent = content;
            }
         }
      }

      server_.assistantNextCommandSuggestion(
         command,
         output,
         isError,
         sourceContextType,
         documentUri,
         documentContent,
         new ServerRequestCallback<AssistantNextCommandSuggestionResponse>()
         {
            @Override
            public void onResponseReceived(AssistantNextCommandSuggestionResponse response)
            {
               handleSuggestionResponse(response);
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.log("Next command suggestion error: " + error.getMessage());
            }
         }
      );
   }

   private void handleSuggestionResponse(AssistantNextCommandSuggestionResponse response)
   {
      if (response == null || response.result == null)
         return;

      AssistantNextCommandSuggestionSuggestion suggestion = response.result;

      if ("suggest".equals(suggestion.type) && suggestion.nextCommand != null)
      {
         showConsoleSuggestion(suggestion.nextCommand);
      }
      else if ("edit".equals(suggestion.type))
      {
         showEditSuggestion(suggestion);
      }
   }

   private void showConsoleSuggestion(String command)
   {
      Debug.log("[NCS] showConsoleSuggestion: command='" + command + "' hasDisplay=" + (consoleDisplay_ != null));
      if (consoleDisplay_ != null)
      {
         consoleDisplay_.setGhostText(command);
      }
   }

   private void showEditSuggestion(AssistantNextCommandSuggestionSuggestion suggestion)
   {
      if (suggestion.oldString == null || suggestion.newString == null)
         return;

      SourceColumnManager sourceColumnManager = pSourceColumnManager_.get();
      if (sourceColumnManager != null)
      {
         boolean shown = sourceColumnManager.showEditSuggestionOnActiveDoc(
            suggestion.oldString, suggestion.newString);
         if (!shown)
         {
            Debug.log("Failed to show edit suggestion - oldString not found in document");
         }
      }
   }

   private static final int COOLDOWN_MS = 2500;

   private final AssistantServerOperations server_;
   private final Provider<SourceColumnManager> pSourceColumnManager_;
   private final Timer cooldownTimer_;

   private DocDisplay consoleDisplay_;
   private String lastCommand_;
   private String lastSourceDocumentId_;
   private StringBuilder outputBuffer_ = new StringBuilder();
   private boolean suggestionInProgress_ = false;
   private String pendingSourceDocumentId_ = null;
}
