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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public interface ChatServerOperations
{
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class ChatVerifyInstalledResponse
   {
      public boolean installed;
      public String version;
   }

   public void chatVerifyInstalled(ServerRequestCallback<ChatVerifyInstalledResponse> requestCallback);
   public void chatStartBackend(ServerRequestCallback<JsObject> requestCallback);
   public void chatStopBackend(ServerRequestCallback<JsObject> requestCallback);
   public void chatGetBackendUrl(ServerRequestCallback<JsObject> requestCallback);
   public void chatGetBackendStatus(ServerRequestCallback<JsObject> requestCallback);
   public void chatGetVersion(ServerRequestCallback<String> requestCallback);

   public void chatCheckForUpdates(ServerRequestCallback<JsObject> requestCallback);
   public void chatInstallUpdate(ServerRequestCallback<Void> requestCallback);
   public void chatGetUpdateStatus(ServerRequestCallback<JsObject> requestCallback);

   public void chatDocFocused(String documentId, ServerRequestCallback<Void> requestCallback);
   public void chatDocFocused(String documentId, JsArray<JavaScriptObject> selections,
                              ServerRequestCallback<Void> requestCallback);
}
