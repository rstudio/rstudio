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
import org.rstudio.studio.client.server.model.DocumentCloseAllNoSaveEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.Prefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

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
                                UserPrefs userPrefs)
   {
      commands_ = commands;
      eventBus_ = eventBus;
      session_ = session;
      userPrefs_ = userPrefs;
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

      // window.rstudio.ready is the canonical "automation can start" signal.
      // initializeAgent runs from a writeUserPrefs callback that fires before
      // the workbench is fully initialized -- the bridge is installed at this
      // point, but show-file events from R's hooked file.edit and other R-to-
      // GWT roundtrips can still race with workbench init. Wait for the
      // DeferredInitCompletedEvent (fired after R's deferred init runs) to
      // flip the flag.
      eventBus_.addHandler(DeferredInitCompletedEvent.TYPE, event -> setReady());
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
      // Flipped to true by setReady() once DeferredInitCompletedEvent fires.
      $wnd.rstudio.ready = false;
   }-*/;

   private native final void setReady() /*-{
      $wnd.rstudio.ready = true;
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
   private boolean isAutomationAgent_ = false;
}
