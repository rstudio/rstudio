/*
 * AssistantServerOperations.java
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
package org.rstudio.studio.client.workbench.assistant.server;

import java.util.ArrayList;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantDiagnosticsResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantGenerateCompletionsResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantNextEditSuggestionsResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantSignInResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantSignOutResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantStatusResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantCompletion;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantCompletionCommand;

public interface AssistantServerOperations
{
   public void assistantDiagnostics(ServerRequestCallback<AssistantDiagnosticsResponse> requestCallback);

   public void assistantVerifyInstalled(ServerRequestCallback<Boolean> requestCallback);

   public void assistantSignIn(ServerRequestCallback<AssistantSignInResponse> requestCallback);

   public void assistantSignOut(ServerRequestCallback<AssistantSignOutResponse> requestCallback);

   public void assistantStatus(ServerRequestCallback<AssistantStatusResponse> requestCallback);

   public void assistantDocFocused(String documentId,
                                   ServerRequestCallback<Void> requestCallback);

   public void assistantDidShowCompletion(AssistantCompletion command,
                                          ServerRequestCallback<Void> requestCallback);

   public void assistantGenerateCompletions(String documentId,
                                            String documentPath,
                                            boolean isUntitled,
                                            boolean autoInvoked,
                                            int cursorRow,
                                            int cursorColumn,
                                            ServerRequestCallback<AssistantGenerateCompletionsResponse> requestCallback);

   public void assistantNextEditSuggestions(String documentId,
                                            String documentPath,
                                            boolean isUntitled,
                                            int cursorRow,
                                            int cursorColumn,
                                            ServerRequestCallback<AssistantNextEditSuggestionsResponse> requestCallback);

   public void assistantDidAcceptCompletion(AssistantCompletionCommand completionCommand,
                                            ServerRequestCallback<Void> requestCallback);

   public void assistantDidAcceptPartialCompletion(AssistantCompletion completion,
                                                 int acceptedLength,
                                                 ServerRequestCallback<Void> requestCallback);

   public void assistantRegisterOpenFiles(ArrayList<String> filePaths,
                                          ServerRequestCallback<Void> requestCallback);
}
