/*
 * ChatSatelliteParams.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ChatSatelliteParams extends JavaScriptObject
{
   protected ChatSatelliteParams()
   {
   }

   public static native ChatSatelliteParams create(
      String chatUrl,
      String authToken,
      boolean resumeChat) /*-{
      var params = new Object();
      params.chat_url = chatUrl;
      params.auth_token = authToken;
      params.resume_chat = resumeChat;
      return params;
   }-*/;

   public final native String getChatUrl() /*-{
      return this.chat_url;
   }-*/;

   public final native String getAuthToken() /*-{
      return this.auth_token;
   }-*/;

   public final native boolean getResumeChat() /*-{
      return this.resume_chat;
   }-*/;
}
