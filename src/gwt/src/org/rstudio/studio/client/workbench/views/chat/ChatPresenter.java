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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
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

      interface UpdateObserver
      {
         void onUpdateNow();
         void onRemindLater();
         void onRetryUpdate();
      }

      void setObserver(Observer observer);
      void setUpdateObserver(UpdateObserver observer);
      void setStatus(String status);
      void showError(String errorMessage);
      void loadUrl(String url);
      void showUpdateNotification(String newVersion);
      void showUpdatingStatus();
      void showUpdateComplete();
      void showUpdateError(String errorMessage);
      void showUpdateCheckFailure();
      void hideUpdateNotification();
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
            initializeChat();
         }
      });

      // Set up update observer
      display_.setUpdateObserver(new Display.UpdateObserver()
      {
         @Override
         public void onUpdateNow()
         {
            installUpdate();
         }

         @Override
         public void onRemindLater()
         {
            display_.hideUpdateNotification();
         }

         @Override
         public void onRetryUpdate()
         {
            installUpdate();
         }
      });
   }

   /**
    * Initialize the Chat pane by checking for updates and starting the backend.
    *
    * This method initiates an async update check. Once the check completes
    * (whether successful or failed), the backend will be started automatically
    * via the checkForUpdates() callback.
    *
    * Flow: initializeChat() -> checkForUpdates() -> startBackend() -> pollForBackendUrl() -> loadChatUI()
    */
   public void initializeChat()
   {
      checkForUpdates();
   }

   private void startBackend()
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
               display_.showError("Failed to start Chat backend: " + error);
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

   private void checkForUpdates()
   {
      server_.chatCheckForUpdates(new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject result)
         {
            if (result.getBoolean("updateAvailable"))
            {
               String newVersion = result.getString("newVersion");
               display_.showUpdateNotification(newVersion);
            }
            // Always start backend after update check completes (success path)
            startBackend();
         }

         @Override
         public void onError(ServerError error)
         {
            // Network failure or other error - show message but don't block
            display_.showUpdateCheckFailure();
            Debug.log("Update check failed: " + error.getMessage());
            // Always start backend even if update check fails (error path)
            startBackend();
         }
      });
   }

   private void installUpdate()
   {
      display_.showUpdatingStatus();

      server_.chatInstallUpdate(new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void result)
         {
            // Start polling for update status
            pollUpdateStatus();
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showUpdateError(error.getMessage());
         }
      });
   }

   private void pollUpdateStatus()
   {
      pollUpdateStatus(0);
   }

   private void pollUpdateStatus(final int attemptCount)
   {
      // Reduced timeout: 60 seconds (was 300 = 5 minutes)
      // Most downloads should complete in seconds, not minutes
      if (attemptCount > 60)
      {
         display_.showUpdateError("Update timeout - the update process took too long. Please try again or check your network connection.");
         return;
      }

      server_.chatGetUpdateStatus(new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            String status = response.getString("status");

            if (status.equals("complete"))
            {
               // Update complete - restart backend and reload UI
               display_.showUpdateComplete();

               // Give user a moment to see the success message
               Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
               {
                  @Override
                  public boolean execute()
                  {
                     initializeChat();
                     return false;
                  }
               }, 1000);
            }
            else if (status.equals("error"))
            {
               String message = response.getString("message");
               display_.showUpdateError(message);
            }
            else if (status.equals("downloading") || status.equals("installing"))
            {
               // Keep polling - update in progress
               Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
               {
                  @Override
                  public boolean execute()
                  {
                     pollUpdateStatus(attemptCount + 1);
                     return false;
                  }
               }, 1000);
            }
            else if (status.equals("idle"))
            {
               // Unexpected: update not actually running
               // This shouldn't happen, but handle it to prevent infinite polling
               display_.showUpdateError("Update process did not start. Please try again.");
            }
            else
            {
               // Unknown status - stop polling to prevent infinite loop
               display_.showUpdateError("Unknown update status: " + status);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showUpdateError("Failed to check update status: " + error.getMessage());
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

      // Append WebSocket URL and timestamp as query parameters to bust cache
      long timestamp = System.currentTimeMillis();
      String urlWithWsParam = baseUrl + "?wsUrl=" + URL.encodeQueryString(wsUrl) + "&_t=" + timestamp;

      Debug.log("ChatPresenter: Loading chat UI from: " + urlWithWsParam);
      Debug.log("ChatPresenter: WebSocket URL: " + wsUrl);

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
