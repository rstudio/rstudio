/*
 * ChatPresenter.java
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
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.http.client.URL;
import com.google.inject.Inject;

public class ChatPresenter extends BasePresenter
{
   public interface Binder extends CommandBinder<Commands, ChatPresenter>
   {
   }

   public interface Display extends WorkbenchView
   {
      interface Observer
      {
         void onPaneReady();
      }

      void setObserver(Observer observer);
      void setStatus(String status);
      void showError(String errorMessage);
      void loadUrl(String url);
   }

   public static class Chat
   {
      public Chat()
      {
      }
   }

   @Inject
   protected ChatPresenter(
      Display display,
      EventBus events,
      Commands commands,
      Binder binder,
      ChatServerOperations server)
   {
      super(display);
      binder.bind(commands, this);
      display_ = display;
      events_ = events;
      commands_ = commands;
      server_ = server;

      // Set up observer
      display_.setObserver(new Display.Observer()
      {
         @Override
         public void onPaneReady()
         {
            startBackendAndLoadUI();
         }
      });
   }

   public void startBackendAndLoadUI()
   {
      // Show loading state
      display_.setStatus("starting");

      // Start backend
      server_.chatStartBackend(new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            if (response.getBoolean("success"))
            {
               // Poll for URL
               pollForBackendUrl();
            }
            else
            {
               String error = response.getString("error");
               display_.setStatus("error");
               display_.showError("Failed to start Posit AI backend: " + error);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.setStatus("error");
            display_.showError("Failed to start backend: " + error.getMessage());
         }
      });
   }

   private void pollForBackendUrl()
   {
      pollForBackendUrl(0);
   }

   private void pollForBackendUrl(final int attemptCount)
   {
      if (attemptCount > 30) // 30 seconds timeout
      {
         display_.setStatus("error");
         display_.showError("Timeout waiting for backend to start");
         return;
      }

      server_.chatGetBackendStatus(new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            String status = response.getString("status");

            if (status.equals("ready"))
            {
               String wsUrl = response.getString("url");
               loadChatUI(wsUrl);
            }
            else if (status.equals("starting"))
            {
               // Retry after 1 second
               Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
               {
                  @Override
                  public boolean execute()
                  {
                     pollForBackendUrl(attemptCount + 1);
                     return false;
                  }
               }, 1000);
            }
            else
            {
               display_.setStatus("error");
               display_.showError("Backend failed to start: " + status);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.setStatus("error");
            display_.showError("Failed to check backend status: " + error.getMessage());
         }
      });
   }

   private void loadChatUI(String wsUrl)
   {
      // In dev mode, GWT.getHostPageBaseURL() may not include session prefix
      // Try relative URL first, which should work in both dev and production
      String baseUrl = "ai-chat/index.html";

      // Append WebSocket URL as query parameter
      String urlWithWsParam = baseUrl + "?wsUrl=" + URL.encodeQueryString(wsUrl);

      com.google.gwt.core.client.GWT.log("ChatPresenter: Loading chat UI from: " + urlWithWsParam);
      com.google.gwt.core.client.GWT.log("ChatPresenter: WebSocket URL: " + wsUrl);

      display_.loadUrl(urlWithWsParam);
      display_.setStatus("ready");
   }

   private final Display display_;
   @SuppressWarnings("unused")
   private final EventBus events_;
   @SuppressWarnings("unused")
   private final Commands commands_;
   private final ChatServerOperations server_;
}
