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

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.QuitEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.common.shiny.model.ShinyServerOperations;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.server.model.DocumentCloseAllNoSaveEvent;
import org.rstudio.studio.client.server.model.DocumentResetToUntitledEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.Prefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.PaneManager;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
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
 *   window.rstudio.commands.&lt;commandId&gt;.isVisible()
 *   window.rstudio.commands.list                       // string[] of all command ids
 *
 *   window.rstudio.prefs.&lt;camelCaseName&gt;.get()
 *   window.rstudio.prefs.&lt;camelCaseName&gt;.set(value)
 *   window.rstudio.prefs.&lt;camelCaseName&gt;.clear()
 *
 *   window.rstudio.documents.closeAllNoSave()
 *   window.rstudio.documents.resetToUntitled() // close everything but a single untitled tab
 *   window.rstudio.documents.active()          // { id, path, dirty } for the focused doc, or null
 *   window.rstudio.documents.activeEditor()    // native Ace editor for the focused doc, or null
 *   window.rstudio.documents.open(path, opts?) // open file at path; opts: { line?, col?, moveCursor? }
 *
 *   window.rstudio.project.path()       // active project file path, or null
 *   window.rstudio.project.name()       // active project display name, or null
 *   window.rstudio.project.isActive()   // boolean
 *   window.rstudio.project.airTomlPath() // cached project-root air.toml path, or null
 *   window.rstudio.project.open(path)   // fire SwitchToProjectEvent; resets ready
 *
 *   window.rstudio.dialogs.numShowing()  // count of open modal dialogs
 *   window.rstudio.dialogs.dismissAll()  // hide every open modal dialog
 *
 *   window.rstudio.layout.reset()        // end any active pane/column zoom or
 *                                        // pane maximize; Promise resolves once
 *                                        // layout settles
 *
 *   window.rstudio.errors.list()         // uncaught client exceptions recorded
 *                                        // since the last clear(): message,
 *                                        // stack, time
 *   window.rstudio.errors.clear()        // empty the record
 *   window.rstudio.errors.simulate(msg)  // raise a real uncaught exception
 *                                        // (harness self-test only)
 *
 *   window.rstudio.shiny.stopForegroundApp() // stop the running foreground
 *                                            // shiny app via shiny::stopApp();
 *                                            // resolves once R exits runApp
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
                                SourceColumnManager sourceColumnManager,
                                ChatServerOperations chatServer,
                                ShinyServerOperations shinyServer,
                                Provider<PaneManager> pPaneManager)
   {
      commands_ = commands;
      eventBus_ = eventBus;
      session_ = session;
      userPrefs_ = userPrefs;
      sourceColumnManager_ = sourceColumnManager;
      chatServer_ = chatServer;
      shinyServer_ = shinyServer;
      // Provider, not a direct injection: PaneManager is constructed later in
      // the workbench lifecycle than this early-initialized agent, so resolve
      // it lazily (mirrors WorkbenchScreen / PaneLayoutPreferencesPane).
      pPaneManager_ = pPaneManager;
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
      registerChat();
      registerShiny();
      registerDialogs();
      registerLayout();
      registerErrors();
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
   //     each session, signalling that R-to-GWT roundtrips are safe. NOTE this
   //     is enqueued once per R *session* (SessionMain::rSessionInitHook), not
   //     per client connect, so it does NOT re-fire on a re-join -- a page
   //     reload that reconnects to an R session which already finished deferred
   //     init (e.g. restoreDefaultPaneAndTabLayoutNoPrompt, which saves prefs
   //     then WindowEx.reload()s). The re-join is covered separately below by
   //     reading sessionInfo.deferred_init_completed (mirroring Packages.java).
   //   - OpenProjectErrorEvent: server-emitted client event when a project
   //     open/switch fails before reaching quit. Bridge callers preemptively
   //     reset ready in project.open() to close the kQuit-arrival window;
   //     without this handler that flag would stay false forever on the
   //     failure path and openProject() would time out instead of surfacing
   //     the real error.
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
      eventBus_.addHandler(OpenProjectErrorEvent.TYPE, event -> setReadyFlag(true));

      // Re-join: a reload that reconnects to a still-running R session which has
      // already completed its deferred init. kDeferredInitCompleted fired once
      // for that session and won't fire again (see the event note above), so the
      // handler is registered for the fresh-start / suspend-resume case while we
      // recognize the re-join here from sessionInfo. Without this, ready would
      // stay false forever after such a reload and any test gating on it in a
      // beforeEach would time out. On a fresh start / suspend-resume the flag is
      // still false at this point, so the event handler does the work as before.
      if (session_.getSessionInfo().getDeferredInitCompleted())
         setReadyFlag(true);

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

   private void registerChat()
   {
      registerChatObject();
   }

   private void registerShiny()
   {
      registerShinyObject();
   }

   private void registerDialogs()
   {
      registerDialogsObject();
   }

   private void registerLayout()
   {
      registerLayoutObject();
   }

   // Record uncaught client exceptions where the automation harness can see
   // them. The default ApplicationUncaughtExceptionHandler shows an "Error"
   // dialog (message only -- no stack) and best-effort logs to the server,
   // neither of which a test run can reliably observe or attribute. Wrap the
   // current handler with one that first records {message, stack, time} into
   // window.rstudio.errors, then delegates so the existing dialog/server
   // behavior is unchanged. The harness drains the buffer after every test
   // and fails the test that produced an exception, with the stack in the
   // failure output.
   private void registerErrors()
   {
      registerErrorsObject();

      final GWT.UncaughtExceptionHandler previousHandler =
            GWT.getUncaughtExceptionHandler();

      GWT.setUncaughtExceptionHandler((e) ->
      {
         try
         {
            recordClientException(StringUtil.notNull(e.toString()), stackString(e));
         }
         catch (Throwable ignored)
         {
            // never let recording break the real handler
         }

         if (previousHandler != null)
            previousHandler.onUncaughtException(e);
      });
   }

   // Render the throwable (and its cause chain) as a readable stack string.
   // In draft / super-dev builds the frames carry Java names; in optimized
   // builds they are best-effort (obfuscated JS identifiers), but the
   // message and cause chain still identify the failure.
   private static String stackString(Throwable e)
   {
      StringBuilder sb = new StringBuilder();
      for (Throwable t = e; t != null; t = t.getCause())
      {
         if (t != e)
            sb.append("Caused by: ");
         sb.append(t.toString()).append("\n");

         StackTraceElement[] stack = t.getStackTrace();
         int limit = Math.min(stack.length, 50);
         for (int i = 0; i < limit; i++)
            sb.append("    at ").append(stack[i]).append("\n");
      }
      return sb.toString();
   }

   // Throw from a scheduled (i.e. $entry-wrapped) context so the exception
   // takes the real uncaught-handler path. Exists so the harness can
   // regression-test the capture chain end-to-end; not for product use.
   private void simulateUncaughtException(String message)
   {
      Scheduler.get().scheduleDeferred(() ->
      {
         throw new RuntimeException(message);
      });
   }

   // End any pane/column zoom or WindowFrame-level pane maximize a prior test
   // left active. No-op when nothing is zoomed or maximized, so a normal layout
   // keeps its current column widths. onCompleted fires once the relayout has
   // settled (see PaneManager.endZoomIfActive) so the JS side can await it.
   private void resetLayoutZoom(JavaScriptObject onCompleted)
   {
      pPaneManager_.get().endZoomIfActive(() -> invokeCallback(onCompleted));
   }

   private native void invokeCallback(JavaScriptObject cb) /*-{
      if (cb) cb();
   }-*/;

   private int numModalsShowing()
   {
      return ModalDialogTracker.numModalsShowing();
   }

   private void dismissAllModals()
   {
      ModalDialogTracker.dismissAllModalDialogs();
   }

   // Stop the running foreground shiny app via the same `stop_shiny_app` RPC
   // the IDE invokes on satellite-window close. Cleaner than driving the
   // interrupt button from a test: interrupt relies on a signal landing while
   // R is in a check-for-interrupts state inside runApp(), which is unreliable
   // on Windows where the OS-level signal path differs from Unix. The RPC
   // calls shiny::stopApp() on the rsession side, which returns from runApp()
   // through the normal shutdown path on every platform.
   private void stopForegroundShinyApp(JavaScriptObject onCompleted)
   {
      shinyServer_.stopShinyApp(ShinyApplicationParams.ID_FOREGROUND,
         new org.rstudio.studio.client.server.ServerRequestCallback<org.rstudio.studio.client.server.VoidResponse>()
         {
            @Override
            public void onResponseReceived(org.rstudio.studio.client.server.VoidResponse response)
            {
               invokeBoolCallback(onCompleted, true);
            }

            @Override
            public void onError(org.rstudio.studio.client.server.ServerError error)
            {
               invokeBoolCallback(onCompleted, false);
            }
         });
   }

   private void setChatUpdateCheckOverride(JavaScriptObject override,
                                           JavaScriptObject onCompleted)
   {
      chatServer_.chatSetUpdateCheckOverride(override,
         new org.rstudio.studio.client.server.ServerRequestCallback<JavaScriptObject>()
         {
            @Override
            public void onResponseReceived(JavaScriptObject response)
            {
               invokeBoolCallback(onCompleted, true);
            }

            @Override
            public void onError(org.rstudio.studio.client.server.ServerError error)
            {
               invokeBoolCallback(onCompleted, false);
            }
         });
   }

   private native final void invokeBoolCallback(JavaScriptObject cb, boolean ok) /*-{
      cb(ok);
   }-*/;

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

   private boolean isCommandVisible(AppCommand command)
   {
      return command.isVisible();
   }

   private Object getPrefValue(Prefs.PrefValue<?> pref)
   {
      return pref.getValue();
   }

   @SuppressWarnings("unchecked")
   private void setPrefValue(Prefs.PrefValue<?> pref, Object value, JavaScriptObject onCompleted)
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
      userPrefs_.writeUserPrefs(succeeded -> invokeWriteCallback(onCompleted, succeeded));
   }

   private void clearPrefValue(Prefs.PrefValue<?> pref, JavaScriptObject onCompleted)
   {
      pref.removeGlobalValue(true);
      userPrefs_.writeUserPrefs(succeeded -> invokeWriteCallback(onCompleted, succeeded));
   }

   private native void invokeWriteCallback(JavaScriptObject cb, boolean succeeded) /*-{
      if (cb) cb(succeeded);
   }-*/;

   private void dispatchCloseAllNoSave()
   {
      eventBus_.dispatchEvent(new DocumentCloseAllNoSaveEvent());
   }

   private void dispatchResetToUntitled()
   {
      eventBus_.dispatchEvent(new DocumentResetToUntitledEvent());
   }

   // Equivalent to .rs.api.documentOpen(path, line, col, moveCursor) but skips
   // the ensureFileExists RPC that columnManager_.editFile would do -- tests
   // pass known-existing paths, and OpenSourceFileEvent is the same event the
   // GWT side ultimately fires from the R-API path. line is 1-indexed; pass
   // line < 0 to open without navigating.
   private void dispatchDocumentOpen(String path, int line, int col, boolean moveCursor)
   {
      FileSystemItem file = FileSystemItem.createFile(path);
      FileTypeRegistry registry = RStudioGinjector.INSTANCE.getFileTypeRegistry();
      TextFileType fileType = registry.getTextTypeForFile(file);
      FilePosition position = line >= 0
         ? FilePosition.create(line, Math.max(col, 1))
         : null;
      eventBus_.dispatchEvent(new OpenSourceFileEvent(
         file, position, fileType, moveCursor, NavigationMethods.DEFAULT));
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

   // Returns the native Ace editor JS instance for the active document, or
   // null when no active editor is present (or the active editor isn't
   // Ace-backed). Callers invoke Ace API methods on the returned object
   // directly -- this is the same JS object exposed via element.env.editor,
   // but reached deterministically through the GWT-tracked active doc rather
   // than by scanning the DOM.
   private JavaScriptObject getActiveNativeEditor()
   {
      return sourceColumnManager_.getActiveNativeEditor();
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

   // The cached project-root air.toml path maintained by Projects via
   // FileChangeEvent. Exposed so tests that create or delete an air.toml on
   // disk can wait for the file monitor to surface the change before
   // exercising formatter paths that consult this cache.
   private String getAirTomlPath()
   {
      return RStudioGinjector.INSTANCE.getProjects().getAirTomlPath();
   }

   // forceSaveAll=true matches what tests want: the bridge caller owns the
   // document state going in, and a modal "save changes?" prompt would
   // deadlock automation. Going through SwitchToProjectEvent directly (rather
   // than OpenProjectFileEvent) also skips the confirm-open and "this is the
   // current project, open options instead" branches in Projects.java.
   private void switchToProject(String projectFilePath)
   {
      eventBus_.dispatchEvent(new SwitchToProjectEvent(projectFilePath, true));
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
      fn.isVisible = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::isCommandVisible(*)(command);
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
         // set / clear return a Promise that resolves once the setUserPrefs
         // RPC has completed, so callers (especially tests that immediately
         // follow with another server-affecting action) can be sure the
         // pref change is fully landed before proceeding.
         set: $entry(function(value) {
            return new $wnd.Promise(function(resolve, reject) {
               var cb = $entry(function(succeeded) {
                  if (succeeded) resolve();
                  else reject(new Error('writeUserPrefs failed for pref: ' + name));
               });
               self.@org.rstudio.studio.client.application.ApplicationAutomation::setPrefValue(*)(pref, value, cb);
            });
         }),
         clear: $entry(function() {
            return new $wnd.Promise(function(resolve, reject) {
               var cb = $entry(function(succeeded) {
                  if (succeeded) resolve();
                  else reject(new Error('writeUserPrefs failed for pref: ' + name));
               });
               self.@org.rstudio.studio.client.application.ApplicationAutomation::clearPrefValue(*)(pref, cb);
            });
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
      // Native Ace editor for the active doc, or null. Lets tests call
      // Ace API directly (getValue, setValue, getSession, ...) without
      // walking `.ace_editor` DOM nodes -- which can collide with the
      // console scroll panel, dialog editors, and stale source editors
      // left in the DOM after a tab close.
      $wnd.rstudio.documents.activeEditor = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::getActiveNativeEditor()();
      });
      // opts: { line?: number (1-indexed; omit/<0 = don't navigate),
      //         col?: number (1-indexed; defaults to 1),
      //         moveCursor?: boolean (defaults to true) }.
      $wnd.rstudio.documents.open = $entry(function(path, opts) {
         opts = opts || {};
         var line = (typeof opts.line === 'number') ? opts.line : -1;
         var col = (typeof opts.col === 'number') ? opts.col : 1;
         var moveCursor = (opts.moveCursor !== false);
         self.@org.rstudio.studio.client.application.ApplicationAutomation::dispatchDocumentOpen(*)(
            path, line, col, moveCursor);
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
      // Cached project-root air.toml path (or null); tracks the file monitor,
      // so tests can poll it after creating/deleting an air.toml on disk.
      $wnd.rstudio.project.airTomlPath = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::getAirTomlPath()();
      });
      // Reset ready synchronously before dispatching. The QuitEvent handler
      // will also reset it once kQuit lands, but resetting here closes the
      // gap between this call returning and the server-emitted kQuit -- a
      // caller polling immediately would otherwise see the prior session's
      // stale true.
      $wnd.rstudio.project.open = $entry(function(path) {
         $wnd.rstudio.ready = false;
         self.@org.rstudio.studio.client.application.ApplicationAutomation::switchToProject(*)(path);
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

   // Open modal dialogs block the Electron close path: the renderer's quit
   // confirmation prompts queue behind the existing modal and the window
   // ends up in a half-shut-down state until a human dismisses things. Tests
   // can call dialogs.dismissAll() from afterEach / teardown to clear the
   // stack before the harness closes the app. numShowing() lets tests assert
   // that earlier steps left the dialog stack clean.
   private native final void registerDialogsObject() /*-{
      var self = this;
      $wnd.rstudio.dialogs = $wnd.rstudio.dialogs || {};
      $wnd.rstudio.dialogs.numShowing = $entry(function() {
         return self.@org.rstudio.studio.client.application.ApplicationAutomation::numModalsShowing()();
      });
      $wnd.rstudio.dialogs.dismissAll = $entry(function() {
         self.@org.rstudio.studio.client.application.ApplicationAutomation::dismissAllModals()();
      });
   }-*/;

   // End any pane/column zoom or pane maximize left active by a prior test
   // (no-op otherwise). Lets a per-test reset clear leaked zoom/maximize state
   // without reverse-engineering it from command checked-states -- PaneManager
   // decides based on the live layout state it owns. Returns a Promise that
   // resolves once the relayout has settled (the restore flushes on a later
   // animation frame even under reduced_motion), so callers can await a stable
   // layout before measuring or clicking panes.
   // window.rstudio.errors: a drainable record of uncaught client exceptions.
   //   list()           -> [{ message, stack, time }] (copy)
   //   clear()          -> empty the record
   //   simulate(message)-> throw an uncaught exception from a scheduled
   //                       context (harness self-test only)
   private native final void registerErrorsObject() /*-{
      var self = this;
      $wnd.rstudio.errors = $wnd.rstudio.errors || {};
      $wnd.rstudio.errors._items = [];
      $wnd.rstudio.errors.list = $entry(function() {
         return $wnd.rstudio.errors._items.slice();
      });
      $wnd.rstudio.errors.clear = $entry(function() {
         $wnd.rstudio.errors._items = [];
      });
      $wnd.rstudio.errors.simulate = $entry(function(message) {
         self.@org.rstudio.studio.client.application.ApplicationAutomation::simulateUncaughtException(Ljava/lang/String;)(message);
      });
   }-*/;

   private native void recordClientException(String message, String stack) /*-{
      var errors = $wnd.rstudio && $wnd.rstudio.errors;
      if (errors && errors._items)
         errors._items.push({ message: message, stack: stack, time: Date.now() });
   }-*/;

   private native final void registerLayoutObject() /*-{
      var self = this;
      $wnd.rstudio.layout = $wnd.rstudio.layout || {};
      $wnd.rstudio.layout.reset = $entry(function() {
         return new $wnd.Promise(function(resolve) {
            var cb = $entry(function() { resolve(); });
            self.@org.rstudio.studio.client.application.ApplicationAutomation::resetLayoutZoom(*)(cb);
         });
      });
   }-*/;

   private native final void registerChatObject() /*-{
      var self = this;
      $wnd.rstudio.chat = $wnd.rstudio.chat || {};
      // Install/clear an automation-only override for the next
      // chat_check_for_updates response. Pass an object to install (any
      // shape the real response can take); pass null to clear. Returns a
      // Promise that resolves once the RPC has landed in rsession.
      $wnd.rstudio.chat.setUpdateCheckOverride = $entry(function(override) {
         return new $wnd.Promise(function(resolve, reject) {
            var cb = $entry(function(succeeded) {
               if (succeeded) resolve();
               else reject(new Error('chat_set_update_check_override RPC failed'));
            });
            self.@org.rstudio.studio.client.application.ApplicationAutomation::setChatUpdateCheckOverride(*)(
               override || null, cb);
         });
      });
   }-*/;

   private native final void registerShinyObject() /*-{
      var self = this;
      $wnd.rstudio = $wnd.rstudio || {};
      $wnd.rstudio.shiny = $wnd.rstudio.shiny || {};
      // Stop the running foreground shiny app. Returns a Promise that
      // resolves once the rsession-side `stop_shiny_app` RPC completes (and
      // shiny::stopApp() has unblocked runApp on the R side). Tests use this
      // instead of clicking the interrupt button to get a clean shutdown on
      // every platform -- the interrupt path depends on signal handling that
      // is unreliable on Windows.
      $wnd.rstudio.shiny.stopForegroundApp = $entry(function() {
         return new $wnd.Promise(function(resolve, reject) {
            var cb = $entry(function(succeeded) {
               if (succeeded) resolve();
               else reject(new Error('stop_shiny_app RPC failed'));
            });
            self.@org.rstudio.studio.client.application.ApplicationAutomation::stopForegroundShinyApp(*)(cb);
         });
      });
   }-*/;

   private final Commands commands_;
   private final EventBus eventBus_;
   private final Session session_;
   private final UserPrefs userPrefs_;
   private final SourceColumnManager sourceColumnManager_;
   private final ChatServerOperations chatServer_;
   private final ShinyServerOperations shinyServer_;
   private final Provider<PaneManager> pPaneManager_;
   private boolean isAutomationAgent_ = false;
   private boolean readinessHandlersRegistered_ = false;
}
