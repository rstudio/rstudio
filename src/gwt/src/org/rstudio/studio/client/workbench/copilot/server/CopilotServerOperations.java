/*
 * CopilotServerOperations.java
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
package org.rstudio.studio.client.workbench.copilot.server;

import java.util.ArrayList;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotDiagnosticsResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotGenerateCompletionsResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotSignInResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotSignOutResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotStatusResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotCompletion;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotCompletionCommand;

public interface CopilotServerOperations
{
   public void copilotDiagnostics(ServerRequestCallback<CopilotDiagnosticsResponse> requestCallback);
   
   public void copilotVerifyInstalled(ServerRequestCallback<Boolean> requestCallback);
   
   public void copilotSignIn(ServerRequestCallback<CopilotSignInResponse> requestCallback);
   
   public void copilotSignOut(ServerRequestCallback<CopilotSignOutResponse> requestCallback);
   
   public void copilotStatus(ServerRequestCallback<CopilotStatusResponse> requestCallback);
   
   public void copilotDocFocused(String documentId,
                                 ServerRequestCallback<Void> requestCallback);
   
   public void copilotDidShowCompletion(CopilotCompletion command,
                                        ServerRequestCallback<Void> requestCallback);

   public void copilotGenerateCompletions(String documentId,
                                          String documentPath,
                                          boolean isUntitled,
                                          boolean autoInvoked,
                                          int cursorRow,
                                          int cursorColumn,
                                          ServerRequestCallback<CopilotGenerateCompletionsResponse> requestCallback);

   public void copilotDidAcceptCompletion(CopilotCompletionCommand completionCommand,
                                          ServerRequestCallback<Void> requestCallback);

   public void copilotDidAcceptPartialCompletion(CopilotCompletion completion,
                                                 int acceptedLength,
                                                 ServerRequestCallback<Void> requestCallback);

   public void copilotRegisterOpenFiles(ArrayList<String> filePaths,
                                        ServerRequestCallback<Void> requestCallback);
}
