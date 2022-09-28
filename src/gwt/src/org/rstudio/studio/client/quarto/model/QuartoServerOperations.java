/*
 * QuartoServerOperations.java
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
package org.rstudio.studio.client.quarto.model;

import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.presentation2.model.PresentationEditorLocation;
import org.rstudio.studio.client.rsconnect.model.QmdPublishDetails;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;


public interface QuartoServerOperations 
{
   void quartoCapabilities(ServerRequestCallback<QuartoCapabilities> requestCallback);
   void quartoServe(String format, boolean render, ServerRequestCallback<Void> requestCallback);
   void quartoServeRender(String file, ServerRequestCallback<Boolean> requestCallback);
   void quartoPreview(String file, 
                      String format, 
                      PresentationEditorLocation editorState,
                      ServerRequestCallback<Boolean> requestCallback);
   void quartoCreateProject(String projectFile, 
                            QuartoNewProjectOptions options, 
                            ServerRequestCallback<ConsoleProcess> requestCallback);
   void quartoPublishDetails(String target, ServerRequestCallback<QmdPublishDetails> resultCallback);
}
