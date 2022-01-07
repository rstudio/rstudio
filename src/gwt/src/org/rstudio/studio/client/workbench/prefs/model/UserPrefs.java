/*
 * UserPrefs.java
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.gwt.core.client.GWT;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ReloadEvent;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;

@Singleton
public class UserPrefs extends UserPrefsComputed
   implements UserPrefsChangedEvent.Handler, DeferredInitCompletedEvent.Handler
{
   public interface Binder
           extends CommandBinder<Commands, UserPrefs> {}

   @Inject
   public UserPrefs(Session session,
                    EventBus eventBus,
                    PrefsServerOperations server,
                    SatelliteManager satelliteManager,
                    Commands commands,
                    Binder binder,
                    GlobalDisplay display,
                    AriaLiveService ariaLive,
                    ApplicationQuit quit)
   {
      super(session.getSessionInfo(),
            (session.getSessionInfo() == null ?
               JsArray.createArray().cast() :
               session.getSessionInfo().getPrefs()));

      session_ = session;
      eventBus_ = eventBus;
      server_ = server;
      satelliteManager_ = satelliteManager;
      display_ = display;
      commands_ = commands;
      reloadAfterInit_ = false;
      ariaLive_ = ariaLive;
      quit_ = quit;

      binder.bind(commands_, this);

      eventBus.addHandler(UserPrefsChangedEvent.TYPE, this);
      eventBus.addHandler(DeferredInitCompletedEvent.TYPE, this);
      Scheduler.get().scheduleDeferred(() ->
      {
         origScreenReaderLabel_ = commands_.toggleScreenReaderSupport().getMenuLabel(false);
         announceScreenReaderState();
         syncToggleTabKeyMovesFocusState();
      });
   }

   public void writeUserPrefs()
   {
      writeUserPrefs(null);
   }

   public void writeUserPrefs(CommandWithArg<Boolean> onCompleted)
   {
      updatePrefs(session_.getSessionInfo().getPrefs());
      server_.setUserPrefs(
         session_.getSessionInfo().getUserPrefs(),
         new ServerRequestCallback<Void>()
         {
            @Override
            public void onResponseReceived(Void v)
            {
               UserPrefsChangedEvent event = new UserPrefsChangedEvent(
                     session_.getSessionInfo().getUserPrefLayer());

               if (Satellite.isCurrentWindowSatellite())
               {
                  RStudioGinjector.INSTANCE.getEventBus()
                     .fireEventToMainWindow(event);
               }
               else
               {
                  // let satellites know prefs have changed
                  satelliteManager_.dispatchCrossWindowEvent(event);
               }

               if (onCompleted != null)
               {
                  onCompleted.execute(true);
               }
            }
            @Override
            public void onError(ServerError error)
            {
               if (onCompleted != null)
               {
                  onCompleted.execute(false);
               }
               Debug.logError(error);
            }
         });
   }

   /**
    * Indicates whether autosave is enabled, via any pref that turns it on.
    *
    * @return Whether auto save is enabled.
    */
   public boolean autoSaveEnabled()
   {
      return autoSaveOnBlur().getValue() ||
             StringUtil.equals(autoSaveOnIdle().getValue(), AUTO_SAVE_ON_IDLE_COMMIT);
   }

   /**
    * Indicates the number of milliseconds after which to autosave. Won't return
    * a value less than 500 since autosaves typically require a network call and
    * other synchronization.
    *
    * @return The number of milliseconds.
    */
   public int autoSaveMs()
   {
      Integer ms = autoSaveIdleMs().getValue();
      if (ms < 500)
         return 500;
      return ms;
   }

   @Override
   public void onUserPrefsChanged(UserPrefsChangedEvent e)
   {
      syncPrefs(e.getName(), e.getValues());
   }

   @Handler
   public void onEditUserPrefs()
   {
      server_.editPreferences(new VoidServerRequestCallback());
   }

   @Handler
   public void onClearUserPrefs()
   {
      display_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
         constants_.onClearUserPrefsCaption(),
         constants_.onClearUserPrefsMessage(),
         false,
         (indicator) ->
         {
            server_.clearPreferences(new ServerRequestCallback<String>()
            {
               public void onResponseReceived(String path)
               {
                  indicator.onCompleted();
                  display_.showMessage(
                        GlobalDisplay.MSG_INFO,
                        constants_.onClearUserPrefsResponseCaption(),
                        constants_.onClearUserPrefsResponseMessage(path),
                        () ->
                        {
                           // Restart R, then reload the UI when done
                           reloadAfterInit_ = true;
                           commands_.restartR().execute();
                        },
                          constants_.onClearUserPrefsRestartR(),
                        false);
               }

               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getMessage());
               }
            });
         },
         null,
         constants_.onClearUserPrefsYesLabel(),
         constants_.cancel(),
         false);
   }

   @Override
   public void onDeferredInitCompleted(DeferredInitCompletedEvent event)
   {
      // Called when R is finished initializing; if we have just cleared prefs,
      // we also reload the UI when R's done restarting
      if (reloadAfterInit_)
      {
         reloadAfterInit_ = false;
         WindowEx.get().reload();
      }
   }

   @Handler
   public void onViewAllPrefs()
   {
      server_.viewPreferences(new VoidServerRequestCallback());
   }

   private void setScreenReaderMenuState(boolean checked)
   {
      commands_.toggleScreenReaderSupport().setChecked(checked);
      commands_.toggleScreenReaderSupport().setMenuLabel(checked ?
            constants_.screenReaderStateEnabled(origScreenReaderLabel_) :
            constants_.screenReaderStateDisabled(origScreenReaderLabel_));
   }

   private void announceScreenReaderState()
   {
      // announce if screen reader is not enabled; most things work without enabling it, but for
      // best experience user should turn it on
      if (!enableScreenReader().getValue())
      {
         Timers.singleShot(AriaLiveService.STARTUP_ANNOUNCEMENT_DELAY, () ->
         {
            String shortcut = commands_.toggleScreenReaderSupport().getShortcutRaw();
            ariaLive_.announce(AriaLiveService.SCREEN_READER_NOT_ENABLED,
                  constants_.announceScreenReaderStateMessage(shortcut),
                  Timing.IMMEDIATE, Severity.ALERT);
         });
      }
      setScreenReaderMenuState(enableScreenReader().getValue());
   }

   public void setScreenReaderEnabled(boolean enabled)
   {
      if (Desktop.hasDesktopFrame())
         Desktop.getFrame().setEnableAccessibility(enabled);
      enableScreenReader().setGlobalValue(enabled);

      // When screen-reader is enabled, reduce UI animations as they serve no purpose
      // other than to potentially confuse the screen reader; turn animations back
      // on when screen-reader support is disabled as that is the normal default and most
      // users will never touch it.
      RStudioGinjector.INSTANCE.getUserPrefs().reducedMotion().setGlobalValue(enabled);

      // Disable virtual scrolling when screen reader is enabled
      if (enabled)
         RStudioGinjector.INSTANCE.getUserPrefs().limitVisibleConsole().setGlobalValue(false);
   }

   @Handler
   void onToggleScreenReaderSupport()
   {
      display_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
            constants_.toggleScreenReaderConfirmCaption(),
            constants_.toggleScreenReaderMessageConfirmDialog(
                    enableScreenReader().getValue() ? constants_.disable() : constants_.enable()),
            false,
            () ->
            {
               setScreenReaderEnabled(!enableScreenReader().getValue());
               writeUserPrefs(succeeded -> {
                  if (succeeded)
                  {
                     if (Desktop.isDesktop())
                        quit_.doRestart(session_);
                     else
                        eventBus_.fireEvent(new ReloadEvent());
                  }
                  else
                  {
                     display_.showErrorMessage(constants_.errorChangingSettingCaption(),
                           constants_.toggleScreenReaderErrorMessage());
                  }
               });
            },
            () -> {
               setScreenReaderMenuState(enableScreenReader().getValue());
            },
            false);
   }

   public void syncToggleTabKeyMovesFocusState(boolean checked)
   {
      commands_.toggleTabKeyMovesFocus().setChecked(checked);
   }

   private void syncToggleTabKeyMovesFocusState()
   {
      syncToggleTabKeyMovesFocusState(
            RStudioGinjector.INSTANCE.getUserPrefs().tabKeyMoveFocus().getValue());
   }

   @Handler
   void onToggleTabKeyMovesFocus()
   {
      boolean newMode = !RStudioGinjector.INSTANCE.getUserPrefs().tabKeyMoveFocus().getValue();
      RStudioGinjector.INSTANCE.getUserPrefs().tabKeyMoveFocus().setGlobalValue(newMode);
      writeUserPrefs(succeeded ->
      {
         if (succeeded)
         {
            syncToggleTabKeyMovesFocusState();
            ariaLive_.announce(AriaLiveService.TAB_KEY_MODE,
                  newMode ? constants_.tabKeyFocusOnMessage() : constants_.tabKeyFocusOffMessage(),
                  Timing.IMMEDIATE, Severity.STATUS);
         }
         else
         {
            display_.showErrorMessage(constants_.errorChangingSettingCaption(),
                  constants_.tabKeyErrorMessage());
         }
      });
   }

   public static final int LAYER_DEFAULT  = 0;
   public static final int LAYER_SYSTEM   = 1;
   public static final int LAYER_COMPUTED = 2;
   public static final int LAYER_USER     = 3;
   public static final int LAYER_PROJECT  = 4;

   public static final int MAX_TAB_WIDTH = 64;
   public static final int MAX_WRAP_COLUMN = 256;
   public static final int MAX_SCREEN_READER_CONSOLE_OUTPUT = 999;

   private final Session session_;
   private final PrefsServerOperations server_;
   private final SatelliteManager satelliteManager_;
   private final GlobalDisplay display_;
   private final Commands commands_;
   private final EventBus eventBus_;
   private final AriaLiveService ariaLive_;
   private final ApplicationQuit quit_;

   private boolean reloadAfterInit_;
   private String origScreenReaderLabel_;

   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
