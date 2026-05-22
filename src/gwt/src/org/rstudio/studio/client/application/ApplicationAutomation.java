/*
 * ApplicationAutomation.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application;

import java.util.Map;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.QuitEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.server.model.DocumentCloseAllNoSaveEvent;
import org.rstudio.studio.client.server.model.DocumentResetToUntitledEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.Prefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Exposes a JS-facing automation surface at <code>window.rstudio</code> when
 * rsession is launched with <code>--automation-agent</code>. The surface is
 * read by external test drivers (Playwright tests under <code>e2e/rstudio</code>)
 * to drive commands, read/write preferences, and dispatch a few document
 * actions without going through the R console.
 *
 * <h2>Shape</h2>
 *
 * <pre>
 *   window.rstudio.commands.&lt;commandId&gt;()           // execute
 *   window.rstudio.commands.&lt;commandId&gt;.isChecked()
 *   window.rstudio.commands.&lt;commandId&gt;.isEnabled()
 *   window.rstudio.commands.list                       // string[] of all command ids
 *
 *   window.rstudio.prefs.&lt;camelCaseName&gt;.get()
 *   window.rstudio.prefs.&lt;camelCaseName&gt;.set(value)
 *   window.rstudio.prefs.&lt;camelCaseName&gt;.clear()
 *
 *   window.rstudio.documents.closeAllNoSave()
 *   window.rstudio.documents.resetToUntitled() // close everything but a single untitled tab
 *   window.rstudio.documents.active()          // { id, path, dirty } for the focused doc, or null
 *
 *   window.rstudio.project.path()       // active project file path, or null
 *   window.rstudio.project.name()       // active project display name, or null
 *   window.rstudio.project.isActive()   // boolean
 * </pre>
 *
 * <h2>Why enumerate everything up front</h2>
 *
 * Commands and preferences are both populated lazily in their respective
 * caches (Commands and Prefs.values_). If a caller looks up a pref that no
 * GWT code has accessed yet, the lookup returns null and the old flat-callback
 * bridge would silently no-op. Enumerating <code>commands_.getCommands()</code>
 * and <code>userPrefs_.allPrefs()</code> at startup populates both caches so
 * the bridge surface is complete from the first call.
 */
@Singleton
public class ApplicationAutomation
{
   @Inject
   public ApplicationAutomation(Commands commands,
                                EventBus eventBus,
                                Session session,
                                UserPrefs userPrefs,
                                SourceColumnManager sourceColumnManager)
   {
      commands_ = commands;
      eventBus_ = eventBus;
      session_ = session;
      userPrefs_ = userPrefs;
      sourceColumnManager_ = sourceColumnManager;
   }

   public final boolean isAutomationAgent()
   {
      return isAutomationAgent_;
   }

   public final void initializeAgent()
   {
      isAutomationAgent_ = true;
      initializeRoot();
      registerCommands();
      registerPrefs();
      registerDocuments();
      registerProject();
      registerVersion();
      registerReadinessHandlers();
   }

   // window.rstudio.ready is the canonical "automation can start" signal,
   // flipped to true once R's deferred init has run for the current session.
   // We also reset it back to false synchronously when any session-ending
   // transition starts -- otherwise the prior session's "true" persists across
   // a Restart-R or project open/close, and an automation client polling for
   // the new session's readiness would see the stale value and exit early.
   //
   // Coverage by event:
   //   - QuitEvent: kQuit emitted by SessionMain when R is quitting, including
   //     project open/close/switch (switch_projects flag) and full quit.
   //   - RestartStatusEvent.RESTART_INITIATED: fired by ApplicationQuit before
   //     the actual suspend-for-restart, covering .rs.restartR / restartR
   //     command (kSuspendAndRestart on the server side).
   //   - DeferredInitCompletedEvent: fires once R's deferred init has run for
   //     each session, signalling that R-to-GWT roundtrips are safe.
   //
   // doRestart-style flows (Electron relaunch on PAI uninstall etc.) reload
   // the GWT page entirely, so initializeRoot() handles their reset.
   //
   // Handlers are registered once per GWT page lifetime: initializeAgent runs
   // again on every session restart (via the writeUserPrefs callback that hosts
   // it), so the registered-flag guards against handler leaks across sessions.
   private void registerReadinessHandlers()
   {
      if (readinessHandlersRegistered_)
         return;

      eventBus_.addHandler(DeferredInitCompletedEvent.TYPE, event -> setReadyFlag(true));
      eventBus_.addHandler(QuitEvent.TYPE, event -> setReadyFlag(false));
      eventBus_.addHandler(RestartStatusEvent.TYPE, event -> {
         if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED)
            setReadyFlag(false);
      });

      readinessHandlersRegistered_ = true;
   }

   private void registerCommands()
   {
      Map<String, AppCommand> commands = commands_.getCommands();
      String[] commandIds = new String[commands.size()];
      int i = 0;
      for (Map.Entry<String, AppCommand> entry : commands.entrySet())
      {
         String id = entry.getKey();
         registerCommand(id, entry.getValue());
         commandIds[i++] = id;
      }
      registerCommandList(commandIds);
   }

   private void registerPrefs()
   {
      // allPrefs() invokes each pref's factory method (defaultProjectLocation(),
      // saveWorkspace(), ...), which constructs the PrefValue and registers it
      // in Prefs.values_ as a side effect. Without this warm-up, prefs that
      // haven't been accessed yet would be undiscoverable by name -- the bug
      // tracked in #17724.
      for (Prefs.PrefValue<?> pref : userPrefs_.allPrefs())
      {
         registerPref(snakeToCamel(pref.getId()), pref);
      }
   }

   private void registerDocuments()
   {
      registerDocumentsObject();
   }

   private void registerProject()
   {
      registerProjectObject();
   }

   private void registerVersion()
   {
      // SessionInfo is set by Application.onResponseReceived before the agent
      // is initialized (see Application.java); the version strings are stable
      // for the life of the session.
      registerVersionObject(
         session_.getSessionInfo().getRstudioVersion(),
         session_.getSessionInfo().getRVersionsInfo().getRVersion()
      );
   }

   /** Convert a snake_case identifier to camelCase. */
   private static String snakeToCamel(String s)
   {
      StringBuilder sb = new StringBuilder(s.length());
      boolean upperNext = false;
      for (int i = 0; i < s.length(); i++)
      {
         char c = s.charAt(i);
         if (c == '_')
         {
            upperNext = true;
         }
         else if (upperNext)
         {
            sb.append(Character.toUpperCase(c));
            upperNext = false;
         }
         else
         {
            sb.append(c);
         }
      }
      return sb.toString();
   }

   // --- Helpers called from JSNI -------------------------------------------
   //
   // JSNI marshals JS primitives into Java boxed types: JS boolean ->
   // java.lang.Boolean, JS number -> java.lang.Double, JS string ->
   // java.lang.String. setPrefValue() dispatches by the pref's declared type,
   // narrowing the Number for int prefs.

   private void executeCommand(AppCommand command)
   {
      command.execute();
   }

   private boolean isCommandChecked(AppCommand command)
   {
      return command.isChecked();
   }

   private boolean isCommandEnabled(AppCommand command)
   {
      return command.isEnabled();
   }

   private Object getPrefValue(Prefs.PrefValue<?> pref)
   {
      return pref.getValue();
   }

   @SuppressWarnings("unchecked")
   private void setPrefValue(Prefs.PrefValue<?> pref, Object value)
   {
      if (pref instanceof Prefs.BooleanValue)
      {
         ((Prefs.PrefValue<Boolean>) pref).setGlobalValue((Boolean) value);
      }
      else if (pref instanceof Prefs.IntValue)
      {
         ((Prefs.PrefValue<Integer>) pref).setGlobalValue(((Number) value).intValue());
      }
      else if (pref instanceof Prefs.DoubleValue)
      {
         ((Prefs.PrefValue<Double>) pref).setGlobalValue(((Number) value).doubleValue());
      }
      else if (pref instanceof Prefs.StringValue || pref instanceof Prefs.EnumValue)
      {
         ((Prefs.PrefValue<String>) pref).setGlobalValue((String) value);
      }
      else
      {
         throw new RuntimeException(
            "Unsupported preference type for " + pref.getId() + ": " + pref.getClass().getSimpleName());
      }
      userPrefs_.writeUserPrefs();
   }

   private void clearPrefValue(Prefs.PrefValue<?> pref)
   {
      pref.removeGlobalValue(true);
      userPrefs_.writeUserPrefs();
   }

   private void dispatchCloseAllNoSave()
   {
      eventBus_.dispatchEvent(new DocumentCloseAllNoSaveEvent());
   }

   private void dispatchResetToUntitled()
   {
      eventBus_.dispatchEvent(new DocumentResetToUntitledEvent());
   }

   // Returns null when no editor is active so callers can distinguish "no doc"
   // from "doc exists but is clean".
   private JavaScriptObject getActiveDocument()
   {
      if (!sourceColumnManager_.hasActiveEditor())
         return null;
      return makeActiveDocumentJso(
         sourceColumnManager_.getActiveDocId(),
         sourceColumnManager_.getActiveDocPath(),
         sourceColumnManager_.isActiveDocDirty());
   }

   private native JavaScriptObject makeActiveDocumentJso(String id, String path, boolean dirty) /*-{
      return { id: id, path: path, dirty: dirty };
   }-*/;

   // Always read through session_.getSessionInfo() rather than capturing a
   // reference at install time -- a session restart (e.g. close-project)
   // replaces the SessionInfo JSO, and we want callers to see the live state.
   private String getActiveProjectPath()
   {
      return session_.getSessionInfo().getActiveProjectFile();
   }

   private String getActiveProjectName()
   {
      return session_.getSessionInfo().getActiveProjectName();
   }

   private boolean isProjectActive()
   {
      return getActiveProjectPath() != null;
   }

   // --- JSNI surface installation ------------------------------------------

   private native final void initializeRoot() /*-{
      $wnd.rstudio = $wnd.rstudio || {};
      $wnd.rstudio.commands = $wnd.rstudio.commands || {};
      $wnd.rstudio.prefs = $wnd.rstudio.prefs || {};
      $wnd.rstudio.documents = $wnd.rstudio.documents || {};
      // Set false on initial bootstrap; flipped by setReadyFlag() in response
      // to lifecycle events (see registerReadinessHandlers).
      $wnd.rstudio.ready = false;
   }-*/;

   // Guarded against the case where the agent was never initialized (an
   // automation session that ends before initializeRoot runs would still get
   // handler-fire attempts on full quit).
   private native final void setReadyFlag(boolean ready) /*-{
      if ($wnd.rstudio) {
         $wnd.rstudio.ready = ready;
      }
   }-*/;

   private native final void registerCommand(String id, AppCommand command) /*-{
      var self = this;
      var fn = $entry(function() {
         self.@org.rstudio.studio.client.application.ApplicationAutomation::executeCommand(*)(command);
      });
      fn.isChecked = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::isCommandChecked(*)(command);
      });
      fn.isEnabled = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::isCommandEnabled(*)(command);
      });
      $wnd.rstudio.commands[id] = fn;
   }-*/;

   private native final void registerCommandList(String[] ids) /*-{
      var list = [];
      for (var i = 0; i < ids.length; i++) {
         list.push(ids[i]);
      }
      $wnd.rstudio.commands.list = list;
   }-*/;

   private native final void registerPref(String name, Prefs.PrefValue<?> pref) /*-{
      var self = this;
      $wnd.rstudio.prefs[name] = {
         get: $entry(function() {
            return self.@org.rstudio.studio.client.application.ApplicationAutomation::getPrefValue(*)(pref);
         }),
         set: $entry(function(value) {
            self.@org.rstudio.studio.client.application.ApplicationAutomation::setPrefValue(*)(pref, value);
         }),
         clear: $entry(function() {
            self.@org.rstudio.studio.client.application.ApplicationAutomation::clearPrefValue(*)(pref);
         })
      };
   }-*/;

   private native final void registerDocumentsObject() /*-{
      var self = this;
      $wnd.rstudio.documents.closeAllNoSave = $entry(function() {
         self.@org.rstudio.studio.client.application.ApplicationAutomation::dispatchCloseAllNoSave()();
      });
      $wnd.rstudio.documents.resetToUntitled = $entry(function() {
         self.@org.rstudio.studio.client.application.ApplicationAutomation::dispatchResetToUntitled()();
      });
      $wnd.rstudio.documents.active = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::getActiveDocument()();
      });
   }-*/;

   private native final void registerProjectObject() /*-{
      var self = this;
      $wnd.rstudio.project = $wnd.rstudio.project || {};
      $wnd.rstudio.project.path = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::getActiveProjectPath()();
      });
      $wnd.rstudio.project.name = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::getActiveProjectName()();
      });
      $wnd.rstudio.project.isActive = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::isProjectActive()();
      });
   }-*/;

   // Versions are stable for the life of the session, so install a plain
   // object rather than getter functions. Read via window.rstudio.version.
   private native final void registerVersionObject(String rstudio, String r) /*-{
      $wnd.rstudio.version = {
         rstudio: rstudio,
         r: r,
      };
   }-*/;

   private final Commands commands_;
   private final EventBus eventBus_;
   private final Session session_;
   private final UserPrefs userPrefs_;
   private final SourceColumnManager sourceColumnManager_;
   private boolean isAutomationAgent_ = false;
   private boolean readinessHandlersRegistered_ = false;
}
