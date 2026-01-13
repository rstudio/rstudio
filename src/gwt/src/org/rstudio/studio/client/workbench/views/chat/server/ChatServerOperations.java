/*
 * ChatServerOperations.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat.server;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface ChatServerOperations
{
   public void chatVerifyInstalled(ServerRequestCallback<Boolean> requestCallback);
   public void chatStartBackend(ServerRequestCallback<JsObject> requestCallback);
   public void chatGetBackendUrl(ServerRequestCallback<JsObject> requestCallback);
   public void chatGetBackendStatus(ServerRequestCallback<JsObject> requestCallback);
   public void chatGetVersion(ServerRequestCallback<String> requestCallback);

   public void chatCheckForUpdates(ServerRequestCallback<JsObject> requestCallback);
   public void chatInstallUpdate(ServerRequestCallback<Void> requestCallback);
   public void chatGetUpdateStatus(ServerRequestCallback<JsObject> requestCallback);

   public void chatDocFocused(String documentId, ServerRequestCallback<Void> requestCallback);
}
