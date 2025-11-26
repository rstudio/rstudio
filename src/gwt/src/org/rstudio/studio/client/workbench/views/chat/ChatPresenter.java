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
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.chat.events.ChatBackendExitEvent;
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
         void onRestartBackend();
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
      void showInstallNotification(String newVersion);
      void showUpdatingStatus();
      void showUpdateComplete();
      void showUpdateError(String errorMessage);
      void showUpdateCheckFailure();
      void hideUpdateNotification();
      void showCrashedMessage(int exitCode);
      void showSuspendedMessage();
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

         @Override
         public void onRestartBackend()
         {
            restartBackend();
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
            // User wants to use current version - start backend now
            startBackend();
         }

         @Override
         public void onRetryUpdate()
         {
            installUpdate();
         }
      });

      // Listen for backend exit events (crashes)
      events_.addHandler(ChatBackendExitEvent.TYPE, new ChatBackendExitEvent.Handler()
      {
         @Override
         public void onChatBackendExit(ChatBackendExitEvent event)
         {
            if (event.getCrashed())
            {
               display_.showCrashedMessage(event.getExitCode());
            }
         }
      });

      // Listen for session suspension/resume events
      events_.addHandler(SessionSerializationEvent.TYPE, new SessionSerializationEvent.Handler()
      {
         @Override
         public void onSessionSerialization(SessionSerializationEvent event)
         {
            int action = event.getAction().getType();
            if (action == SessionSerializationAction.SUSPEND_SESSION)
            {
               display_.showSuspendedMessage();
            }
            else if (action == SessionSerializationAction.RESUME_SESSION)
            {
               // Backend will be restarted by onResume() handler in SessionChat.cpp
               // Just need to wait for it to become available
               pollForBackendUrl(0);
            }
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
            boolean updateAvailable = result.getBoolean("updateAvailable");
            boolean isInitialInstall = result.getBoolean("isInitialInstall");

            if (updateAvailable)
            {
               String newVersion = result.getString("newVersion");

               if (isInitialInstall)
               {
                  display_.showInstallNotification(newVersion);
               }
               else
               {
                  display_.showUpdateNotification(newVersion);
               }
               // Do NOT start backend when update/install is available
               // Backend will start after user clicks "Update/Install Now" and it completes,
               // or after user clicks "Ignore"
               return;
            }

            // No update available - start backend normally
            startBackend();
         }

         @Override
         public void onError(ServerError error)
         {
            // Network failure or other error - show message but don't block
            display_.showUpdateCheckFailure();

            // Try to start backend anyway - if nothing is installed, backend will
            // return an error that we handle gracefully. This handles the case where
            // there IS an existing installation but the update check failed due to
            // network issues.
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
                     // Set flag so we know to hide the notification after reload
                     reloadingAfterUpdate_ = true;
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
            else
            {
               // Backend is still starting (could be "starting", "stopped", etc.)
               // Keep retrying until timeout or ready
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
         }

         @Override
         public void onError(ServerError error)
         {
            display_.setStatus("error");
            display_.showError("Failed to check backend status: " + error.getMessage());
         }
      });
   }

   private void restartBackend()
   {
      display_.setStatus("Restarting Posit Assistant...");

      server_.chatStartBackend(new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            if (response.getBoolean("success"))
            {
               // Backend started - begin polling for URL
               pollForBackendUrl(0);
            }
            else
            {
               display_.showError("Failed to restart: " + response.getString("error"));
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showError("Failed to restart: " + error.getMessage());
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

      display_.loadUrl(urlWithWsParam);
      display_.setStatus("ready");

      // Only hide notification if we're reloading after an install/update completion
      // Otherwise, keep any "Update available" notification visible
      if (reloadingAfterUpdate_)
      {
         display_.hideUpdateNotification();
         reloadingAfterUpdate_ = false;  // Reset flag
      }
   }

   private final Display display_;
   @SuppressWarnings("unused")
   private final EventBus events_;
   @SuppressWarnings("unused")
   private final Commands commands_;
   private final ChatServerOperations server_;

   // Track whether we're reloading after an install/update completion
   private boolean reloadingAfterUpdate_ = false;
}
