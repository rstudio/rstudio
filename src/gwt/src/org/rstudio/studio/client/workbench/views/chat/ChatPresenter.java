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
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
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
      enum Status
      {
         STARTING,
         RESTARTING,
         NOT_INSTALLED,
         ERROR,
         READY,
         ASSISTANT_NOT_SELECTED
      }

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
      void setStatus(Status status);
      void showError(String errorMessage);
      void loadUrl(String url);
      void showUpdateNotification(String newVersion);
      void showInstallNotification(String newVersion);
      void showUpdatingStatus();
      void showUpdateComplete();
      void showUpdateError(String errorMessage);
      void showUpdateCheckFailure();
      void hideUpdateNotification();
      void hideErrorNotification();
      void showCrashedMessage(int exitCode);
      void showSuspendedMessage();
      void showIncompatibleVersion();
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
      ChatServerOperations server,
      UserPrefs prefs)
   {
      super(display);
      binder.bind(commands, this);
      display_ = display;
      events_ = events;
      commands_ = commands;
      server_ = server;
      prefs_ = prefs;

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
               // Prevent concurrent initialization
               if (initializing_)
               {
                  return;
               }

               // Don't poll for backend if Posit AI isn't selected as chat provider
               if (!PaiUtil.isChatProviderPosit(prefs_))
               {
                  display_.setStatus(Display.Status.ASSISTANT_NOT_SELECTED);
                  return;
               }

               // Backend will be restarted by onResume() handler in SessionChat.cpp
               // Just need to wait for it to become available
               initializing_ = true;
               pollForBackendUrl(0);
            }
         }
      });

      // Listen for chat provider preference changes
      prefs_.chatProvider().addValueChangeHandler((event) ->
      {
         if (StringUtil.equals(event.getValue(), UserPrefs.CHAT_PROVIDER_POSIT))
         {
            // Prevent concurrent initialization
            if (initializing_)
            {
               return;
            }

            // Posit AI was just selected as chat provider, initialize chat
            initializing_ = true;
            checkForUpdates();
         }
         else
         {
            // Posit AI was deselected as chat provider, stop backend and show not-selected message
            initializing_ = false;  // Cancel any ongoing initialization
            stopBackend();
            display_.hideUpdateNotification();  // Clean up all notifications
            display_.setStatus(Display.Status.ASSISTANT_NOT_SELECTED);
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
      // Prevent concurrent initialization (e.g., from multiple event sources)
      if (initializing_)
      {
         return;
      }

      // Check if Posit AI is selected as chat provider before initializing
      if (!PaiUtil.isChatProviderPosit(prefs_))
      {
         display_.setStatus(Display.Status.ASSISTANT_NOT_SELECTED);
         return;
      }

      initializing_ = true;
      checkForUpdates();
   }

   private void startBackend()
   {
      // Show loading state
      display_.setStatus(Display.Status.STARTING);

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
               initializing_ = false;
               display_.setStatus(Display.Status.ERROR);
               display_.showError(constants_.chatBackendStartFailed(error));
            }
         }

         @Override
         public void onError(ServerError error)
         {
            initializing_ = false;
            display_.setStatus(Display.Status.ERROR);
            display_.showError(constants_.chatBackendStartError(error.getMessage()));
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
            // Check for incompatible protocol version first
            boolean noCompatibleVersion = result.getBoolean("noCompatibleVersion");
            if (noCompatibleVersion)
            {
               initializing_ = false;
               display_.showIncompatibleVersion();
               return;  // Don't try to start backend
            }

            boolean updateAvailable = result.getBoolean("updateAvailable");
            boolean isInitialInstall = result.getBoolean("isInitialInstall");

            if (updateAvailable)
            {
               String newVersion = result.getString("newVersion");

               // Reset initialization flag - we're pausing for user action
               initializing_ = false;

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
         display_.showUpdateError(constants_.chatUpdateTimeout());
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
               display_.showUpdateError(constants_.chatUpdateNotStarted());
            }
            else
            {
               // Unknown status - stop polling to prevent infinite loop
               display_.showUpdateError(constants_.chatUpdateStatusUnknown(status));
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showUpdateError(constants_.chatUpdateStatusCheckFailed(error.getMessage()));
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
         initializing_ = false;
         display_.setStatus(Display.Status.ERROR);
         display_.showError(constants_.chatBackendStartTimeout());
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
            initializing_ = false;
            display_.setStatus(Display.Status.ERROR);
            display_.showError(constants_.chatBackendStatusCheckFailed(error.getMessage()));
         }
      });
   }

   private void restartBackend()
   {
      // Prevent concurrent initialization
      if (initializing_)
      {
         return;
      }

      initializing_ = true;
      display_.setStatus(Display.Status.RESTARTING);

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
               initializing_ = false;
               display_.showError(constants_.chatRestartFailed(response.getString("error")));
            }
         }

         @Override
         public void onError(ServerError error)
         {
            initializing_ = false;
            display_.showError(constants_.chatRestartFailed(error.getMessage()));
         }
      });
   }

   private void stopBackend()
   {
      server_.chatStopBackend(new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            // Backend stopped (or wasn't running) - nothing to do
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   private void loadChatUI(String wsUrl)
   {
      // Re-check preference before loading (guards against preference change during polling)
      if (!PaiUtil.isChatProviderPosit(prefs_))
      {
         initializing_ = false;
         display_.setStatus(Display.Status.ASSISTANT_NOT_SELECTED);
         return;
      }

      // In dev mode, GWT.getHostPageBaseURL() may not include session prefix
      // Try relative URL first, which should work in both dev and production
      String baseUrl = "ai-chat/index.html";

      // Append WebSocket URL and timestamp as query parameters to bust cache
      long timestamp = System.currentTimeMillis();
      String urlWithWsParam = baseUrl + "?wsUrl=" + URL.encodeQueryString(wsUrl) + "&_t=" + timestamp;

      display_.loadUrl(urlWithWsParam);
      display_.setStatus(Display.Status.READY);

      // Reset initialization flag - we're done
      initializing_ = false;

      // Handle notifications after successful load
      if (reloadingAfterUpdate_)
      {
         // After update/install completion, hide all notifications
         display_.hideUpdateNotification();
         reloadingAfterUpdate_ = false;
      }
      else
      {
         // Hide error notifications since backend started successfully
         // Keep "Update available" notifications visible so user can update later
         display_.hideErrorNotification();
      }
   }

   private final Display display_;
   private final EventBus events_;
   @SuppressWarnings("unused")
   private final Commands commands_;
   private final ChatServerOperations server_;
   private final UserPrefs prefs_;

   // Track whether we're reloading after an install/update completion
   private boolean reloadingAfterUpdate_ = false;

   // Guard against concurrent initialization (multiple polling loops)
   private boolean initializing_ = false;

   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
