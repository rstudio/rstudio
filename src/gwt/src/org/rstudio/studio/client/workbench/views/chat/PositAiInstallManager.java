/*
 * PositAiInstallManager.java
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
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.assistant.server.AssistantServerOperations;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.inject.Inject;

/**
 * Manages Posit AI installation and updates.
 *
 * This class encapsulates the logic for checking for updates, installing/updating
 * Posit AI, and polling for installation status. It can be used by both the Chat
 * pane and the Preferences pane.
 */
public class PositAiInstallManager
{
   /**
    * Callback interface for update check results.
    */
   public interface UpdateCheckCallback
   {
      /**
       * Called when no update is available (Posit AI is already installed and up-to-date).
       */
      void onNoUpdateAvailable();

      /**
       * Called when an update or initial install is available.
       *
       * @param currentVersion The currently installed version (empty string if not installed)
       * @param newVersion The version that can be installed
       * @param isInitialInstall True if this is a fresh install, false if it's an update
       */
      void onUpdateAvailable(String currentVersion, String newVersion, boolean isInitialInstall);

      /**
       * Called when no compatible version of Posit AI is available for this RStudio version.
       */
      void onIncompatibleVersion();

      /**
       * Called when the update check failed (e.g., network error).
       *
       * @param errorMessage The error message
       */
      void onCheckFailed(String errorMessage);
   }

   /**
    * Callback interface for installation progress and results.
    */
   public interface InstallCallback
   {
      /**
       * Called when installation has started.
       */
      void onInstallStarted();

      /**
       * Called periodically during installation with status updates.
       *
       * @param status The current status (e.g., "downloading", "installing")
       */
      void onInstallProgress(String status);

      /**
       * Called when installation completes successfully.
       */
      void onInstallComplete();

      /**
       * Called when installation fails.
       *
       * @param errorMessage The error message
       */
      void onInstallFailed(String errorMessage);
   }

   /**
    * Creates a new PositAiInstallManager.
    */
   public PositAiInstallManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   private void initialize(ChatServerOperations chatServer,
                           AssistantServerOperations assistantServer)
   {
      chatServer_ = chatServer;
      assistantServer_ = assistantServer;
   }

   /**
    * Checks if an update or initial install is available for Posit AI.
    *
    * @param callback The callback to receive the result
    */
   public void checkForUpdates(UpdateCheckCallback callback)
   {
      chatServer_.chatCheckForUpdates(new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject result)
         {
            // Check for incompatible protocol version first
            boolean noCompatibleVersion = result.getBoolean("noCompatibleVersion");
            if (noCompatibleVersion)
            {
               callback.onIncompatibleVersion();
               return;
            }

            boolean updateAvailable = result.getBoolean("updateAvailable");
            boolean isInitialInstall = result.getBoolean("isInitialInstall");

            if (updateAvailable)
            {
               String currentVersion = result.getString("currentVersion");
               String newVersion = result.getString("newVersion");
               callback.onUpdateAvailable(currentVersion, newVersion, isInitialInstall);
            }
            else
            {
               callback.onNoUpdateAvailable();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            callback.onCheckFailed(error.getMessage());
         }
      });
   }

   /**
    * Starts the installation or update of Posit AI.
    *
    * @param callback The callback to receive progress and completion status
    */
   public void installUpdate(InstallCallback callback)
   {
      callback.onInstallStarted();

      chatServer_.chatInstallUpdate(new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void result)
         {
            // Start polling for update status
            pollUpdateStatus(callback, 0);
         }

         @Override
         public void onError(ServerError error)
         {
            callback.onInstallFailed(error.getMessage());
         }
      });
   }

   /**
    * Polls for installation status until completion, error, or timeout.
    */
   private void pollUpdateStatus(InstallCallback callback, int attemptCount)
   {
      if (attemptCount > MAX_POLL_ATTEMPTS)
      {
         callback.onInstallFailed(constants_.chatUpdateTimeout());
         return;
      }

      chatServer_.chatGetUpdateStatus(new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            String status = response.getString("status");

            if (status.equals("complete"))
            {
               // Notify the assistant module that installation is complete
               // so it can reset its cached state and start the agent
               assistantServer_.assistantNotifyInstalled(new VoidServerRequestCallback()
               {
                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                  }
               });

               callback.onInstallComplete();
            }
            else if (status.equals("error"))
            {
               String message = response.getString("message");
               callback.onInstallFailed(message);
            }
            else if (status.equals("downloading") || status.equals("installing"))
            {
               // Notify progress
               callback.onInstallProgress(status);

               // Keep polling
               Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
               {
                  @Override
                  public boolean execute()
                  {
                     pollUpdateStatus(callback, attemptCount + 1);
                     return false;
                  }
               }, POLL_INTERVAL_MS);
            }
            else if (status.equals("idle"))
            {
               // Unexpected: update not actually running
               callback.onInstallFailed(constants_.chatUpdateNotStarted());
            }
            else
            {
               // Unknown status - stop polling to prevent infinite loop
               callback.onInstallFailed(constants_.chatUpdateStatusUnknown(status));
            }
         }

         @Override
         public void onError(ServerError error)
         {
            callback.onInstallFailed(constants_.chatUpdateStatusCheckFailed(error.getMessage()));
         }
      });
   }

   private ChatServerOperations chatServer_;
   private AssistantServerOperations assistantServer_;

   // Polling configuration
   private static final int MAX_POLL_ATTEMPTS = 60; // 60 seconds timeout
   private static final int POLL_INTERVAL_MS = 1000; // 1 second between polls

   private static final ChatConstants constants_ = GWT.create(ChatConstants.class);
}
