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
import org.rstudio.core.client.Point;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.satellite.model.SatelliteWindowGeometry;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
import org.rstudio.studio.client.common.satellite.events.SatelliteClosedEvent;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.chat.events.ChatBackendExitEvent;
import org.rstudio.studio.client.workbench.views.chat.events.ChatReturnToMainEvent;
import org.rstudio.studio.client.workbench.views.chat.model.ChatSatelliteParams;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations;
import org.rstudio.studio.client.workbench.ui.PaneManager;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.http.client.URL;
import com.google.inject.Inject;

public class ChatPresenter extends BasePresenter
   implements ConsolePromptEvent.Handler,
              SatelliteClosedEvent.Handler,
              ChatReturnToMainEvent.Handler
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
         void onPaneReady(boolean installed, String installedVersion);
         void onRestartBackend();
         void onActivateChat();
         void onReturnChatToMain();
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
      void showNotInstalledWithInstall(String newVersion);
      void showUpdateAvailableWithVersions(String currentVersion, String newVersion);
      void showUpdatingStatus();
      void showUpdateComplete();
      void showUpdateError(String errorMessage);
      void showUpdateCheckFailure();
      void hideUpdateNotification();
      void hideErrorNotification();
      void showCrashedMessage(int exitCode);
      void showSuspendedMessage();
      void showIncompatibleVersion();
      void showUnsupportedVersionUpgradeRequired(String currentVersion, String newVersion);
      void showUnsupportedVersionNoUpdate(String currentVersion);
      void showUnsupportedProtocol();
      void showManifestUnavailable();
      void showReadlineNotification();
      void hideReadlineNotification();
      void updateCachedUrl(String url);
      void showPoppedOutPlaceholder();
      void hidePoppedOutPlaceholder();
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
      PaiUtil paiUtil,
      UserPrefs prefs,
      SatelliteManager satelliteManager,
      PaneManager paneManager,
      Session session)
   {
      super(display);
      binder.bind(commands, this);
      display_ = display;
      events_ = events;
      commands_ = commands;
      server_ = server;
      paiUtil_ = paiUtil;
      prefs_ = prefs;
      installManager_ = new PositAiInstallManager();
      satelliteManager_ = satelliteManager;
      paneManager_ = paneManager;
      session_ = session;

      // Set up observer
      display_.setObserver(new Display.Observer()
      {
         @Override
         public void onPaneReady(boolean installed, String installedVersion)
         {
            if (poppedOut_)
            {
               if (cachedUrl_ != null)
               {
                  // Satellite already open — show placeholder now that
                  // the pane is in the DOM.
                  display_.showPoppedOutPlaceholder();
                  display_.setStatus(Display.Status.READY);
               }
               // else: backend still starting from eager init — let it
               // finish; initializing_ guard prevents re-entry
               return;
            }
            initializeChat(installed, installedVersion);
         }

         @Override
         public void onRestartBackend()
         {
            restartBackend();
         }

         @Override
         public void onActivateChat()
         {
            ChatPresenter.this.onActivateChat();
         }

         @Override
         public void onReturnChatToMain()
         {
            returnChatToMain();
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

      // Listen for console prompt events (to detect when R is waiting for input)
      events_.addHandler(ConsolePromptEvent.TYPE, this);

      // Listen for backend exit events (crashes)
      events_.addHandler(ChatBackendExitEvent.TYPE, new ChatBackendExitEvent.Handler()
      {
         @Override
         public void onChatBackendExit(ChatBackendExitEvent event)
         {
            display_.hideReadlineNotification();
            if (event.getCrashed())
            {
               cachedUrl_ = null;
               if (poppedOut_)
               {
                  updateSavedGeometry();
                  satelliteManager_.closeSatelliteWindow(ChatSatellite.NAME);
               }
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
               display_.hideReadlineNotification();
               if (poppedOut_)
               {
                  // Save geometry; satellite stays open and shows its own
                  // suspend overlay via SessionSerializationEvent forwarding
                  updateSavedGeometry();
               }
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
               if (!paiUtil_.isChatProviderPosit())
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

      // Listen for satellite closed events
      events_.addHandler(SatelliteClosedEvent.TYPE, this);

      // Track when the application is quitting so we don't reset poppedOut_
      // during shutdown. LastChanceSaveEvent fires early in the GWT quit flow,
      // before any windows close, so this flag is set regardless of which
      // window had focus when the user pressed Cmd+Q.
      events_.addHandler(LastChanceSaveEvent.TYPE,
         (LastChanceSaveEvent event) -> windowsClosing_ = true);

      // Listen for chat return-to-main events from satellite
      events_.addHandler(ChatReturnToMainEvent.TYPE, this);

      // Listen for chat provider preference changes (global setting)
      prefs_.chatProvider().addValueChangeHandler((event) ->
      {
         onChatProviderChanged();
      });

      // Listen for project options changes (project-level setting)
      events_.addHandler(ProjectOptionsChangedEvent.TYPE, (event) ->
      {
         onChatProviderChanged();
      });

      // Persist pop-out state and satellite geometry across sessions
      new JSObjectStateValue(
         "chat-window",
         "chatSatelliteState",
         ClientState.PROJECT_PERSISTENT,
         session.getSessionInfo().getClientState(),
         false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (value != null)
            {
               poppedOut_ = Boolean.TRUE.equals(value.getBoolean("poppedOut"));
               if (value.hasKey("geometry"))
                  savedGeometry_ = value.getObject("geometry").cast();

               // Eagerly start the backend when popped out — the normal
               // trigger (ChatPane.onLoad) won't fire if the sidebar is
               // hidden.
               if (poppedOut_)
               {
                  Scheduler.get().scheduleDeferred(() -> initializeChat());
               }
            }
         }

         @Override
         protected JsObject getValue()
         {
            // Capture live geometry from the satellite window if it's open
            if (poppedOut_)
               updateSavedGeometry();

            JsObject state = JsObject.createJsObject();
            state.setBoolean("poppedOut", poppedOut_);
            if (savedGeometry_ != null)
               state.setObject("geometry", savedGeometry_);
            return state;
         }

         @Override
         protected boolean hasChanged()
         {
            return true;
         }
      };
   }

   // When the console prompt fires while R is busy (i.e. executing code rather
   // than at the top-level REPL), it means something mid-execution is requesting
   // user input (e.g. readline, browser, scan). Show a notification so the user
   // knows to respond in the Console.
   //
   // NOTE: This intentionally triggers for all mid-execution prompts, not just
   // AI-initiated ones. A browser() or readline() call is worth surfacing even
   // if it wasn't triggered by the assistant, since the user may need to act.
   //
   // TODO: When Posit Assistant gains support for handling input requests
   // directly, we should signal the assistant here instead of (or in
   // addition to) showing a passive notification.
   // https://github.com/posit-dev/databot/issues/770
   @Override
   public void onConsolePrompt(ConsolePromptEvent event)
   {
      if (event.getPrompt().getBusy())
         display_.showReadlineNotification();
      else
         display_.hideReadlineNotification();
   }

   @Override
   public void onSatelliteClosed(SatelliteClosedEvent event)
   {
      if (!ChatSatellite.NAME.equals(event.getName()) || !poppedOut_)
         return;

      // When Cmd+Q is pressed from the satellite window, SatelliteClosedEvent
      // arrives before LastChanceSaveEvent sets windowsClosing_. Deferring by
      // one tick lets the quit flow's LastChanceSaveEvent fire first.
      Scheduler.get().scheduleDeferred(() ->
      {
         if (!windowsClosing_)
            returnChatToMain();
      });
   }

   @Override
   public void onChatReturnToMain(ChatReturnToMainEvent event)
   {
      returnChatToMain();
   }

   @Handler
   void onPopOutChat()
   {
      if (poppedOut_)
      {
         return;
      }

      poppedOut_ = true;
      display_.showPoppedOutPlaceholder();

      if (cachedUrl_ != null)
      {
         // Backend already running — open satellite immediately
         ChatSatelliteParams params = ChatSatelliteParams.create(
            cachedUrl_, cachedAuthToken_, true);
         Size size = (savedGeometry_ != null)
            ? savedGeometry_.getSize()
            : new Size(500, 700);
         Point position = (savedGeometry_ != null)
            ? savedGeometry_.getPosition()
            : null;
         boolean adjustSize = (savedGeometry_ == null);
         satelliteManager_.openSatellite(
            ChatSatellite.NAME,
            params,
            size,
            adjustSize,
            position);

         paneManager_.hideSidebarIfOnlyChatTab();
      }
      else
      {
         // Backend not initialized (e.g. sidebar was hidden at startup).
         // Start initialization; loadChatUI() will open the satellite
         // because poppedOut_ is already true.
         initializeChat();

         // Hide sidebar only if pop-out wasn't cancelled during init
         if (poppedOut_)
         {
            paneManager_.hideSidebarIfOnlyChatTab();
         }
      }
   }

   @Handler
   void onReturnChatToMain()
   {
      returnChatToMain();
   }

   @Handler
   void onActivateChat()
   {
      if (poppedOut_)
      {
         satelliteManager_.activateSatelliteWindow(ChatSatellite.NAME);
      }
      else
      {
         paneManager_.activateTab(PaneManager.Tab.Chat);
      }
   }

   /**
    * Resets popped-out state when initialization fails before the satellite
    * could be opened (e.g. backend error, provider not selected).
    */
   private void cancelPopOut()
   {
      if (poppedOut_)
      {
         poppedOut_ = false;
         display_.hidePoppedOutPlaceholder();
      }
   }

   private void updateSavedGeometry()
   {
      WindowEx window = satelliteManager_.getSatelliteWindowObject(
         ChatSatellite.NAME);
      if (window != null)
      {
         // In desktop mode, use outer dimensions because Electron's
         // setSize() sets the outer window size including title bar.
         // In web mode, use inner dimensions because window.open()
         // width/height set the content area size.
         int width = Desktop.hasDesktopFrame()
            ? window.getOuterWidth()
            : window.getInnerWidth();
         int height = Desktop.hasDesktopFrame()
            ? window.getOuterHeight()
            : window.getInnerHeight();
         savedGeometry_ = SatelliteWindowGeometry.create(
            0,
            window.getScreenX(),
            window.getScreenY(),
            width,
            height);
      }
   }

   private void returnChatToMain()
   {
      if (!poppedOut_)
      {
         return;
      }

      updateSavedGeometry();
      poppedOut_ = false;
      satelliteManager_.closeSatelliteWindow(ChatSatellite.NAME);
      display_.hidePoppedOutPlaceholder();

      // Reload chat in the main window
      if (cachedUrl_ != null)
      {
         display_.loadUrl(cachedUrl_);
         display_.setStatus(Display.Status.READY);
      }
      else
      {
         // Backend wasn't initialized yet (e.g. pop-out triggered before
         // backend was ready). Re-initialize to load chat in the main pane.
         initializeChat();
      }

      // Ensure the chat pane is visible — handles both sidebar and
      // quadrant configurations correctly
      onActivateChat();
   }

   /**
    * Called when either global chat provider preference or project options change.
    * Re-evaluates whether chat should be enabled based on the effective provider.
    */
   private void onChatProviderChanged()
   {
      if (paiUtil_.isChatProviderPosit())
      {
         // Prevent concurrent initialization
         if (initializing_)
         {
            return;
         }

         // Posit AI is the effective chat provider, initialize chat
         initializing_ = true;
         checkForUpdates();
      }
      else
      {
         // If popped out, close the satellite first
         if (poppedOut_)
         {
            poppedOut_ = false;
            satelliteManager_.closeSatelliteWindow(ChatSatellite.NAME);
            display_.hidePoppedOutPlaceholder();
         }

         // Posit AI is not the effective chat provider, stop backend and show not-selected message
         initializing_ = false;  // Cancel any ongoing initialization
         stopBackend();
         display_.hideReadlineNotification();
         display_.hideUpdateNotification();
         display_.setStatus(Display.Status.ASSISTANT_NOT_SELECTED);
   
      }
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
      initializeChat(false, null);
   }

   /**
    * Initialize the Chat pane with known installation status.
    *
    * @param installed Whether Posit Assistant is currently installed
    * @param installedVersion The currently installed version (null if not installed)
    */
   public void initializeChat(boolean installed, String installedVersion)
   {
      // Prevent concurrent initialization (e.g., from multiple event sources)
      if (initializing_)
      {
         return;
      }

      // Check if Posit AI is selected as chat provider before initializing
      if (!paiUtil_.isChatProviderPosit())
      {
         cancelPopOut();
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
               cancelPopOut();
               display_.setStatus(Display.Status.ERROR);
               display_.showError(constants_.chatBackendStartFailed(error));
            }
         }

         @Override
         public void onError(ServerError error)
         {
            initializing_ = false;
            cancelPopOut();
            display_.setStatus(Display.Status.ERROR);
            display_.showError(constants_.chatBackendStartError(error.getMessage()));
         }
      });
   }

   private void checkForUpdates()
   {
      installManager_.checkForUpdates(new PositAiInstallManager.UpdateCheckCallback()
      {
         @Override
         public void onNoUpdateAvailable()
         {
            // No update available - start backend normally
            startBackend();
         }

         @Override
         public void onUpdateAvailable(String currentVersion, String newVersion, boolean isInitialInstall)
         {
            // Reset initialization flag - we're pausing for user action
            initializing_ = false;
            cancelPopOut();

            if (isInitialInstall)
            {
               // Show "not installed" message with install button in main content area
               display_.showNotInstalledWithInstall(newVersion);
            }
            else
            {
               // Show update available message with version info in main content area
               display_.showUpdateAvailableWithVersions(currentVersion, newVersion);
            }
            // Do NOT start backend when update/install is available
            // Backend will start after user clicks "Update/Install Now" and it completes,
            // or after user clicks "Ignore"
         }

         @Override
         public void onIncompatibleVersion()
         {
            initializing_ = false;
            cancelPopOut();
            display_.showIncompatibleVersion();
         }

         @Override
         public void onUnsupportedVersionUpgradeRequired(
             String currentVersion, String newVersion)
         {
            initializing_ = false;
            cancelPopOut();
            display_.showUnsupportedVersionUpgradeRequired(
                currentVersion, newVersion);
         }

         @Override
         public void onUnsupportedVersionNoUpdate(String currentVersion)
         {
            initializing_ = false;
            cancelPopOut();
            display_.showUnsupportedVersionNoUpdate(currentVersion);
         }

         @Override
         public void onUnsupportedProtocol()
         {
            initializing_ = false;
            cancelPopOut();
            display_.showUnsupportedProtocol();
         }

         @Override
         public void onManifestUnavailable()
         {
            initializing_ = false;
            cancelPopOut();
            display_.showManifestUnavailable();
         }

         @Override
         public void onCheckFailed(String errorMessage)
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
      installManager_.installUpdate(new PositAiInstallManager.InstallCallback()
      {
         @Override
         public void onInstallStarted()
         {
            display_.showUpdatingStatus();
         }

         @Override
         public void onInstallProgress(String status)
         {
            // Status is shown via showUpdatingStatus(), no additional action needed
         }

         @Override
         public void onInstallComplete()
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

         @Override
         public void onInstallFailed(String errorMessage)
         {
            display_.showUpdateError(errorMessage);
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
         cancelPopOut();
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
               String authToken = response.hasKey("auth_token") ? response.getString("auth_token") : "";
               if (authToken.isEmpty())
               {
                  Debug.log("Chat backend reported ready but auth_token is missing");
               }
               boolean resumeChat = response.hasKey("resume_chat") && response.getBoolean("resume_chat");
               loadChatUI(wsUrl, authToken, resumeChat);
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
            cancelPopOut();
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

   private void loadChatUI(String wsUrl, String authToken, boolean resumeChat)
   {
      // Re-check preference before loading (guards against preference change during polling)
      if (!paiUtil_.isChatProviderPosit())
      {
         initializing_ = false;
         cancelPopOut();
         display_.setStatus(Display.Status.ASSISTANT_NOT_SELECTED);
         return;
      }

      // In dev mode, GWT.getHostPageBaseURL() may not include session prefix
      // Try relative URL first, which should work in both dev and production
      String baseUrl = "ai-chat/index.html";

      // Append WebSocket URL, auth token, and timestamp as query parameters to bust cache
      long timestamp = System.currentTimeMillis();
      String params = "?wsUrl=" + URL.encodeQueryString(wsUrl) + "&_t=" + timestamp;
      if (authToken != null && !authToken.isEmpty())
      {
         params += "&authToken=" + URL.encodeQueryString(authToken);
      }

      String loadUrl = baseUrl + params;
      if (resumeChat)
      {
         loadUrl += "&resume";
      }

      // Cache the URL and auth token for satellite use
      cachedUrl_ = loadUrl;
      cachedAuthToken_ = authToken;

      // If popped out, send URL to satellite instead of loading in ChatPane
      if (poppedOut_)
      {
         ChatSatelliteParams satelliteParams = ChatSatelliteParams.create(
            loadUrl, authToken, resumeChat);
         Size size = (savedGeometry_ != null)
            ? savedGeometry_.getSize()
            : new Size(500, 700);
         Point position = (savedGeometry_ != null)
            ? savedGeometry_.getPosition()
            : null;
         boolean adjustSize = (savedGeometry_ == null);
         satelliteManager_.openSatellite(
            ChatSatellite.NAME,
            satelliteParams,
            size,
            adjustSize,
            position);

         display_.setStatus(Display.Status.READY);
         display_.showPoppedOutPlaceholder();
   
      }
      else
      {
         display_.loadUrl(loadUrl);
         display_.setStatus(Display.Status.READY);
   
      }

      // Mark the backend so subsequent status responses include resume_chat=true
      server_.chatNotifyUILoaded(new VoidServerRequestCallback());

      // Always include &resume in the cached URL so that tab-switch reloads
      // (via onSelected) signal resume, even if the initial load was fresh
      String cachedResumeUrl = baseUrl + params + "&resume";
      display_.updateCachedUrl(cachedResumeUrl);
      cachedUrl_ = cachedResumeUrl;

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
   private final Commands commands_;
   private final ChatServerOperations server_;
   private final PaiUtil paiUtil_;
   private final UserPrefs prefs_;
   private final PositAiInstallManager installManager_;
   private final SatelliteManager satelliteManager_;
   private final PaneManager paneManager_;
   private final Session session_;

   // Track whether we're reloading after an install/update completion
   private boolean reloadingAfterUpdate_ = false;

   // Guard against concurrent initialization (multiple polling loops)
   private boolean initializing_ = false;

   // Satellite pop-out state
   private boolean windowsClosing_ = false;
   private boolean poppedOut_ = false;
   private SatelliteWindowGeometry savedGeometry_ = null;
   private String cachedUrl_ = null;
   private String cachedAuthToken_ = null;

   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
