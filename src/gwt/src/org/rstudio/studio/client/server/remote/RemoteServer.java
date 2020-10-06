/*
 * RemoteServer.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

package org.rstudio.studio.client.server.remote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.json.client.*;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.jsonrpc.RequestLog;
import org.rstudio.core.client.jsonrpc.RequestLogEntry;
import org.rstudio.core.client.jsonrpc.RequestLogEntry.ResponseType;
import org.rstudio.core.client.jsonrpc.RpcError;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.jsonrpc.RpcRequest;
import org.rstudio.core.client.jsonrpc.RpcRequestCallback;
import org.rstudio.core.client.jsonrpc.RpcResponse;
import org.rstudio.core.client.jsonrpc.RpcResponseHandler;
import org.rstudio.studio.client.application.ApplicationTutorialEvent;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.*;
import org.rstudio.studio.client.application.model.*;
import org.rstudio.studio.client.common.JSONArrayBuilder;
import org.rstudio.studio.client.common.JSONUtils;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcess.ConsoleProcessFactory;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.crypto.PublicKeyInfo;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.debugging.model.FunctionState;
import org.rstudio.studio.client.common.debugging.model.FunctionSteps;
import org.rstudio.studio.client.common.debugging.model.TopLevelLineData;
import org.rstudio.studio.client.common.dependencies.model.Dependency;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
import org.rstudio.studio.client.common.presentation.model.SlideNavigation;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper.SetClassCall;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper.SetGenericCall;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper.SetMethodCall;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper.SetRefClassCall;
import org.rstudio.studio.client.common.repos.model.SecondaryReposResult;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.common.shiny.model.ShinyCapabilities;
import org.rstudio.studio.client.common.synctex.model.PdfLocation;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.common.vcs.AllStatus;
import org.rstudio.studio.client.common.vcs.BranchesInfo;
import org.rstudio.studio.client.common.vcs.CreateKeyOptions;
import org.rstudio.studio.client.common.vcs.CreateKeyResult;
import org.rstudio.studio.client.common.vcs.DiffResult;
import org.rstudio.studio.client.common.vcs.ProcessResult;
import org.rstudio.studio.client.common.vcs.RemotesInfo;
import org.rstudio.studio.client.common.vcs.StatusAndPathInfo;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;
import org.rstudio.studio.client.events.GetEditorContextEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.notebook.CompileNotebookOptions;
import org.rstudio.studio.client.notebook.CompileNotebookResult;
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.packrat.model.PackratPackageAction;
import org.rstudio.studio.client.packrat.model.PackratPrerequisites;
import org.rstudio.studio.client.packrat.model.PackratStatus;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroCollectionSpec;
import org.rstudio.studio.client.plumber.model.PlumberRunCmd;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectContext;
import org.rstudio.studio.client.projects.model.NewShinyAppOptions;
import org.rstudio.studio.client.projects.model.ProjectTemplateOptions;
import org.rstudio.studio.client.projects.model.ProjectTemplateRegistry;
import org.rstudio.studio.client.projects.model.ProjectUser;
import org.rstudio.studio.client.projects.model.ProjectUserRole;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectVcsOptions;
import org.rstudio.studio.client.projects.model.SharedProjectDetails;
import org.rstudio.studio.client.projects.model.SharingConfigResult;
import org.rstudio.studio.client.projects.model.SharingResult;
import org.rstudio.studio.client.rmarkdown.model.NotebookCreateResult;
import org.rstudio.studio.client.rmarkdown.model.NotebookDocQueue;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdCreatedTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdDocumentTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdExecutionState;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputInfo;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateContent;
import org.rstudio.studio.client.rmarkdown.model.RmdYamlData;
import org.rstudio.studio.client.rmarkdown.model.RmdYamlResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectAppName;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectAuthUser;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentFiles;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectLintResults;
import org.rstudio.studio.client.rsconnect.model.RSConnectPreAuthToken;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSource;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerEntry;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerInfo;
import org.rstudio.studio.client.rsconnect.model.RmdPublishDetails;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.ClientException;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.model.ShinyRunCmd;
import org.rstudio.studio.client.shiny.model.ShinyTestResults;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchResults;
import org.rstudio.studio.client.workbench.codesearch.model.ObjectDefinition;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.TerminalOptions;
import org.rstudio.studio.client.workbench.model.TexCapabilities;
import org.rstudio.studio.client.workbench.model.WorkbenchMetrics;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreters;
import org.rstudio.studio.client.workbench.projects.RenvAction;
import org.rstudio.studio.client.workbench.snippets.model.SnippetData;
import org.rstudio.studio.client.workbench.views.buildtools.model.BookdownFormats;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionObjectSpecifier;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionUninstallResult;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionUpdateResult;
import org.rstudio.studio.client.workbench.views.connections.model.DatabaseObject;
import org.rstudio.studio.client.workbench.views.connections.model.Field;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionInfo;
import org.rstudio.studio.client.workbench.views.console.model.ProcessBufferChunk;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PythonCompletionContext;
import org.rstudio.studio.client.workbench.views.console.shell.assist.SqlCompletionParseContext;
import org.rstudio.studio.client.workbench.views.environment.dataimport.DataImportOptions;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportAssembleResponse;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportPreviewResponse;
import org.rstudio.studio.client.workbench.views.environment.model.DataPreviewResult;
import org.rstudio.studio.client.workbench.views.environment.model.DownloadInfo;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentContextData;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentFrame;
import org.rstudio.studio.client.workbench.views.environment.model.ObjectContents;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.files.model.FileUploadToken;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.jobs.model.JobLaunchSpec;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageState;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;
import org.rstudio.studio.client.workbench.views.packages.model.PackratActions;
import org.rstudio.studio.client.workbench.views.plots.model.Point;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationRPubsSource;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerInspectionResult;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationRequest;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationResponse;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;
import org.rstudio.studio.client.workbench.views.source.events.AvailablePackagesReadyEvent;
import org.rstudio.studio.client.workbench.views.source.model.CheckForExternalEditResult;
import org.rstudio.studio.client.workbench.views.source.model.CppCapabilities;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletionResult;
import org.rstudio.studio.client.workbench.views.source.model.CppDiagnostic;
import org.rstudio.studio.client.workbench.views.source.model.CppSourceLocation;
import org.rstudio.studio.client.workbench.views.source.model.RdShellResult;
import org.rstudio.studio.client.workbench.views.source.model.RnwChunkOptions;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocumentResult;
import org.rstudio.studio.client.workbench.views.terminal.TerminalShellInfo;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitCount;
import org.rstudio.studio.client.workbench.views.vcs.dialog.CommitInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Random;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RemoteServer implements Server
{
   @Inject
   public RemoteServer(Session session,
                       EventBus eventBus,
                       final SatelliteManager satelliteManager,
                       Provider<ConsoleProcessFactory> pConsoleProcessFactory)
   {
      pConsoleProcessFactory_ = pConsoleProcessFactory;
      clientId_ = null;
      disconnected_ = false;
      listeningForEvents_ = false;
      sessionRelaunchPending_ = false;
      session_ = session;
      eventBus_ = eventBus;
      serverAuth_ = new RemoteServerAuth(this);

      // define external event listener if we are the main window
      // (so we can forward to the satellites)
      ClientEventHandler externalListener = null;
      if (!Satellite.isCurrentWindowSatellite())
      {
         externalListener = new ClientEventHandler() {
            @Override
            public void onClientEvent(JavaScriptObject clientEvent)
            {
               satelliteManager.dispatchClientEvent(clientEvent);
            }
         };
      }

      // initialize user home path on init
      eventBus_.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         userHomePath_ = getUserHomePath(session_.getSessionInfo());
      });

      // create server event listener
      serverEventListener_ = new RemoteServerEventListener(this,
                                                           externalListener);
   }

   // complete initialization now that the workbench is ready
   public void initializeForMainWorkbench()
   {
      // satellite windows should never call onWorkbenchReady
      if (Satellite.isCurrentWindowSatellite())
      {
         Debug.log("Satellite window cannot call onWorkbenchReady!");
         assert false;
      }

      // update state
      listeningForEvents_ = true;

      // only check credentials if we are in server mode
      if (session_.getSessionInfo().getMode() == SessionInfo.SERVER_MODE)
         serverAuth_.schedulePeriodicCredentialsUpdate();

      // start event listener
      serverEventListener_.start();

      // register satellite callback
      registerSatelliteCallback();
   }

   public void stopEventListener()
   {
      serverEventListener_.stop();
   }

   public void ensureEventListener()
   {
      ensureListeningForEvents();
   }

   public void disconnect()
   {
      disconnected_ = true;
      serverEventListener_.stop();
      eventBus_.fireEvent(new ApplicationTutorialEvent(ApplicationTutorialEvent.SESSION_DISCONNECT));
   }

   public void log(int logEntryType,
                   String logEntry,
                   ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(logEntryType));
      params.set(1, new JSONString(logEntry));
      sendRequest(LOG_SCOPE , LOG, params, requestCallback);
   }

   public void logException(ClientException e,
                            ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(LOG_SCOPE, LOG_EXCEPTION, e, requestCallback);
   }

   public void clientInit(String baseURL,
                     SessionInitOptions options,
                     final ServerRequestCallback<SessionInfo> requestCallback)
   {
      // generate a unique id to represent this client init request
      // this allows us to request the current status of routing for this request
      // for launcher jobs
      if (clientInitId_.isEmpty())
         clientInitId_ = StringUtil.makeRandomId(32);

      // send init request (record clientId and version contained in response)
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(baseURL));
      params.set(1, new JSONString(clientInitId_));
      sendRequest(RPC_SCOPE,
                  CLIENT_INIT,
                  params,
                  options == null ? null : new JSONObject(options),
                  new ServerRequestCallback<SessionInfo>()
      {
         @Override
         public void cancel()
         {
            super.cancel();
            requestCallback.cancel();
         }

         public void onResponseReceived(SessionInfo sessionInfo)
         {
            clientId_ = sessionInfo.getClientId();
            clientVersion_ = sessionInfo.getClientVersion();
            launchParameters_ = sessionInfo.getLaunchParameters();
            requestCallback.onResponseReceived(sessionInfo);
         }

         public void onError(ServerError error)
         {
            requestCallback.onError(error);
         }
      });
   }

   @Override
   public void getJobConnectionStatus(final ServerRequestCallback<String> requestCallback)
   {
   }

   private void setArrayString(JSONArray params, int index, List<String> what) {
      JSONArray array = new JSONArray();
      for (int i = 0; i < what.size(); i++)
         array.set(i, new JSONString(what.get(i)));
      params.set(index, array);
   }

   private void setArrayString(JSONArray params, int index, JsArrayString what) {
      JSONArray array = new JSONArray();
      for (int i = 0; i < what.length(); i++)
         array.set(i, new JSONString(what.get(i)));
      params.set(index, array);
   }

   private void setArrayNumber(JSONArray params, int index, List<Integer> what) {
      JSONArray array = new JSONArray();
      for (int i = 0; i < what.size(); i++)
         array.set(i, new JSONNumber(what.get(i)));
      params.set(index, array);
   }


   // extract user home path from session info (we don't expose this as a
   // helper function on SessionInfo just because paths on the client are
   // typically aliased, and we want to avoid potentially mixing aliased
   // and unaliased paths in other contexts)
   private static final native String getUserHomePath(SessionInfo info) /*-{
      return info.user_home_path;
   }-*/;

   public void suspendSession(boolean force,
                              ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, SUSPEND_SESSION, force, requestCallback);
   }


   public void handleUnsavedChangesCompleted(
                            boolean handled,
                            ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  HANDLE_UNSAVED_CHANGES_COMPLETED,
                  handled,
                  requestCallback);
   }

   public void quitSession(boolean saveWorkspace,
                           String switchToProject,
                           RVersionSpec switchToRVersion,
                           String hostPageUrl,
                           ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(saveWorkspace));
      params.set(1, new JSONString(StringUtil.notNull(switchToProject)));
      if (switchToRVersion != null)
         params.set(2, new JSONObject(switchToRVersion));
      else
         params.set(2, JSONNull.getInstance());
      params.set(3, new JSONString(StringUtil.notNull(hostPageUrl)));
      sendRequest(RPC_SCOPE, QUIT_SESSION, params, requestCallback);
   }

   public void updateCredentials()
   {
      serverAuth_.attemptToUpdateCredentials();
   }

   public String getApplicationURL(String pathName)
   {
      // if accessing a URL is the first thing we do after being
      // suspended ensure that events flow right away
      ensureListeningForEvents();

      // return the url
      return GWT.getHostPageBaseURL() + pathName;
   }

   public void suspendForRestart(SuspendOptions options,
                                 ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, SUSPEND_FOR_RESTART, options, requestCallback);
   }

   public void ping(ServerRequestCallback<Void> requestCallback)
   {
      if (launchParameters_ == null)
      {
         sendRequest(RPC_SCOPE, PING, requestCallback);
      }
      else
      {
         // include the launch params received earlier via client_init so the ping
         // can restart the session
         JSONArray params = new JSONArray();
         JSONObject kwParams = new JSONObject();
         kwParams.put("launch_parameters", new JSONObject(launchParameters_));
         sendRequest(RPC_SCOPE, PING, params, kwParams, requestCallback);
      }
   }


   public void setWorkbenchMetrics(WorkbenchMetrics metrics,
                                   ServerRequestCallback<Void> requestCallback)
   {
      sendRequestNoCredRefresh(RPC_SCOPE,
                               SET_WORKBENCH_METRICS,
                               metrics,
                               requestCallback);
   }

   @Override
   public void setUserPrefs(JavaScriptObject userPrefs,
                            ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  SET_USER_PREFS,
                  userPrefs,
                  requestCallback);
   }

   @Override
   public void setUserState(JavaScriptObject userState,
                            ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  SET_USER_STATE,
                  userState,
                  requestCallback);
   }

   @Override
   public void editPreferences(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  "edit_user_prefs",
                  requestCallback);
   }

   @Override
   public void viewPreferences(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  "view_all_prefs",
                  requestCallback);
   }

   @Override
   public void clearPreferences(ServerRequestCallback<String> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  "clear_user_prefs",
                  requestCallback);
   }

   public void updateClientState(JavaScriptObject temporary,
                                 JavaScriptObject persistent,
                                 JavaScriptObject projectPersistent,
                                 ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(temporary));
      params.set(1, new JSONObject(persistent));
      params.set(2, new JSONObject(projectPersistent));
      sendRequestNoCredRefresh(RPC_SCOPE,
                               SET_CLIENT_STATE,
                               params,
                               requestCallback);
   }

   public void userPromptCompleted(int response,
                                  ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, USER_PROMPT_COMPLETED, response, requestCallback);
   }

   public void adminNotificationAcknowledged(String id, ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, ADMIN_NOTIFICATION_ACKNOWLEDGED, id, requestCallback);
   }

   public void setUserCrashHandlerPrompted(boolean enableCrashHandling,
                                           ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, SET_USER_CRASH_HANDLER_PROMPTED, enableCrashHandling, requestCallback);
   }
   
   @Override
   public void rstudioApiResponse(JavaScriptObject response,
                                  ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, RSTUDIOAPI_RESPONSE, response, requestCallback);
   }

   @Override
   public void getTerminalOptions(
                     ServerRequestCallback<TerminalOptions> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_TERMINAL_OPTIONS, requestCallback);
   }

   @Override
   public void getTerminalShells(
         ServerRequestCallback<JsArray<TerminalShellInfo>> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_TERMINAL_SHELLS, requestCallback);
   }

   @Override
   public void startTerminal(ConsoleProcessInfo cpi,
                             ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  START_TERMINAL,
                  cpi,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   @Override
   public void adaptToLanguage(String language,
                               ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(language));
      sendRequest(RPC_SCOPE, ADAPT_TO_LANGUAGE, params, requestCallback);
   }

   @Override
   public void executeCode(String code,
                           ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0,  new JSONString(code));
      sendRequest(RPC_SCOPE, EXECUTE_CODE, params, requestCallback);
   }

   public void getInitMessages(ServerRequestCallback<String> requestCallback)
   {
      sendRequest(META_SCOPE,
                  GET_INIT_MESSAGES,
                  requestCallback);
   }

   public void searchCode(
         String term,
         int maxResults,
         ServerRequestCallback<CodeSearchResults> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(term));
      params.set(1, new JSONNumber(maxResults));
      sendRequest(RPC_SCOPE, SEARCH_CODE, params, requestCallback);
   }

   public void getObjectDefinition(
         String line,
         int pos,
         ServerRequestCallback<ObjectDefinition> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(line));
      params.set(1, new JSONNumber(pos));
      sendRequest(RPC_SCOPE,
                  GET_FUNCTION_DEFINITION,
                  params,
                  requestCallback);
   }

   public void findFunctionInSearchPath(
         String line,
         int pos,
         String fromWhere,
         ServerRequestCallback<SearchPathFunctionDefinition> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(line));
      params.set(1, new JSONNumber(pos));
      params.set(2, fromWhere != null ? new JSONString(fromWhere) :
                                        JSONNull.getInstance());
      sendRequest(RPC_SCOPE,
                  FIND_FUNCTION_IN_SEARCH_PATH,
                  params,
                  requestCallback);
   }


   public void getSearchPathFunctionDefinition(
         String name,
         String namespace,
         ServerRequestCallback<SearchPathFunctionDefinition> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(name));
      params.set(1, new JSONString(namespace));
      sendRequest(RPC_SCOPE,
                  GET_SEARCH_PATH_FUNCTION_DEFINITION,
                  params,
                  requestCallback);
   }

   public void getMethodDefinition(
         String name,
         ServerRequestCallback<SearchPathFunctionDefinition> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_METHOD_DEFINITION, name, requestCallback);
   }

   public void consoleInput(String consoleInput,
                            String consoleId,
                            ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, consoleInput == null ? JSONNull.getInstance() :
         new JSONString(consoleInput));
      params.set(1, consoleId == null? JSONNull.getInstance() :
         new JSONString(consoleId));
      sendRequest(RPC_SCOPE, CONSOLE_INPUT, params, requestCallback);
   }

   public void resetConsoleActions(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, RESET_CONSOLE_ACTIONS, requestCallback);
   }

   public void processStart(String handle,
                            ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, PROCESS_START, handle, requestCallback);
   }

   @Override
   public void processInterrupt(String handle,
                                ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, PROCESS_INTERRUPT, handle, requestCallback);
   }

   @Override
   public void processReap(String handle,
                           ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, PROCESS_REAP, handle, requestCallback);
   }

   @Override
   public void processWriteStdin(String handle,
                                 ShellInput input,
                                 ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(handle));
      params.set(1, new JSONObject(input));
      sendRequest(RPC_SCOPE, PROCESS_WRITE_STDIN, params, requestCallback);
   }

   @Override
   public void processSetShellSize(String handle,
                                   int width,
                                   int height,
                                   ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(handle));
      params.set(1, new JSONNumber(width));
      params.set(2,  new JSONNumber(height));
      sendRequest(RPC_SCOPE, PROCESS_SET_SIZE, params, requestCallback);
   }

   @Override
   public void processSetCaption(String handle,
                                 String caption,
                                 ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(handle));
      params.set(1, new JSONString(caption));
      sendRequest(RPC_SCOPE, PROCESS_SET_CAPTION, params, requestCallback);
   }

   @Override
   public void processSetTitle(String handle,
                               String title,
                               ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(handle));
      params.set(1, new JSONString(title));
      sendRequest(RPC_SCOPE, PROCESS_SET_TITLE, params, requestCallback);
   }

   @Override
   public void processEraseBuffer(String handle,
                                  boolean lastLineOnly,
                                  ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
      params.set(1, JSONBoolean.getInstance(lastLineOnly));
      sendRequest(RPC_SCOPE, PROCESS_ERASE_BUFFER, params, requestCallback);
   }

   @Override
   public void processGetBufferChunk(String handle,
                                     int chunk,
                                     ServerRequestCallback<ProcessBufferChunk> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
      params.set(1, new JSONNumber(chunk));
      sendRequest(RPC_SCOPE, PROCESS_GET_BUFFER_CHUNK, params, requestCallback);
   }

   @Override
   public void processGetBuffer(String handle,
                                boolean stripAnsiCodes,
                                ServerRequestCallback<ProcessBufferChunk> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
      params.set(1,  JSONBoolean.getInstance(stripAnsiCodes));
      sendRequest(RPC_SCOPE, PROCESS_GET_BUFFER, params, requestCallback);
   }

   @Override
   public void processUseRpc(String handle,
                             ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
      sendRequest(RPC_SCOPE, PROCESS_USE_RPC, params, requestCallback);
   }

   @Override
   public void processInterruptChild(String handle,
                                     ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
      sendRequest(RPC_SCOPE, PROCESS_INTERRUPT_CHILD, params, requestCallback);
   }

   @Override
   public void processTestExists(String handle,
                                 ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
      sendRequest(RPC_SCOPE, PROCESS_TEST_EXISTS, params, requestCallback);
   }

   @Override
   public void processNotifyVisible(String handle,
                                    ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
      sendRequest(RPC_SCOPE, PROCESS_NOTIFY_VISIBLE, params, requestCallback);
   }

   public void interrupt(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, INTERRUPT, requestCallback);
   }

   public void abort(String nextProj,
                     ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(nextProj)));
      sendRequest(RPC_SCOPE, ABORT, params, requestCallback);
   }

   public void goToCppDefinition(
                  String docPath,
                  int line,
                  int column,
                  ServerRequestCallback<CppSourceLocation> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docPath));
      params.set(1, new JSONNumber(line));
      params.set(2, new JSONNumber(column));
      sendRequest(RPC_SCOPE, "go_to_cpp_definition", params, requestCallback);
   }

   public void findCppUsages(
                  String docPath,
                  int line,
                  int column,
                  ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docPath));
      params.set(1, new JSONNumber(line));
      params.set(2, new JSONNumber(column));
      sendRequest(RPC_SCOPE, "find_cpp_usages", params, requestCallback);
   }

   public void getCppCompletions(
                  String line,
                  String docPath,
                  String docId,
                  int row,
                  int column,
                  String userText,
                  ServerRequestCallback<CppCompletionResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(line));
      params.set(1, new JSONString(docPath));
      params.set(2, new JSONString(docId));
      params.set(3, new JSONNumber(row));
      params.set(4, new JSONNumber(column));
      params.set(5,  new JSONString(userText));
      sendRequest(RPC_SCOPE, GET_CPP_COMPLETIONS, params, requestCallback);
   }

   public void getCppDiagnostics(
                 String docPath,
                 ServerRequestCallback<JsArray<CppDiagnostic>> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_CPP_DIAGNOSTICS, docPath, requestCallback);
   }

   public void printCppCompletions(String docId,
                                   String docPath,
                                   String docContents,
                                   boolean docDirty,
                                   int line,
                                   int column,
                                   ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      params.set(1, new JSONString(docPath));
      params.set(2, new JSONString(docContents));
      params.set(3,  JSONBoolean.getInstance(docDirty));
      params.set(4, new JSONNumber(line));
      params.set(5, new JSONNumber(column));
      sendRequest(RPC_SCOPE, "print_cpp_completions", params, requestCallback);
   }

   public void isFunction(
         String functionString,
         String envString,
         ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(functionString));
      params.set(1, new JSONString(envString));
      sendRequest(RPC_SCOPE, IS_FUNCTION, params, requestCallback);
   }

   public void getDplyrJoinCompletionsString(
         String token,
         String string,
         String cursorPos,
         ServerRequestCallback<Completions> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(token));
      params.set(1, new JSONString(string));
      params.set(2, new JSONString(cursorPos));
      sendRequest(
            RPC_SCOPE,
            GET_DPLYR_JOIN_COMPLETIONS_STRING,
            params,
            requestCallback);
   }

   public void getDplyrJoinCompletions(
         String token,
         String leftDataName,
         String rightDataName,
         String verb,
         String cursorPos,
         ServerRequestCallback<Completions> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(token));
      params.set(1, new JSONString(leftDataName));
      params.set(2, new JSONString(rightDataName));
      params.set(3, new JSONString(verb));
      params.set(4, new JSONString(cursorPos));
      sendRequest(
            RPC_SCOPE,
            GET_DPLYR_JOIN_COMPLETIONS,
            params,
            requestCallback);
   }

   public void getArgs(String name,
                       String source,
                       String helpHandler,
                       ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(name));
      params.set(1, new JSONString(source));
      params.set(2,  new JSONString(StringUtil.notNull(helpHandler)));
      sendRequest(
            RPC_SCOPE,
            GET_ARGS,
            params,
            requestCallback);
   }

   public void extractChunkOptions(
         String chunkText,
         ServerRequestCallback<JsObject> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(chunkText));
      sendRequest(RPC_SCOPE,
                  EXTRACT_CHUNK_OPTIONS,
                  params,
                  requestCallback);
   }

   public void executeUserCommand(String functionName,
                                  ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();

      params.set(0, new JSONString(functionName));

      sendRequest(RPC_SCOPE,
                  EXECUTE_USER_COMMAND,
                  params,
                  requestCallback);
   }

   public void saveSnippets(JsArray<SnippetData> snippets,
                            ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, "save_snippets", snippets, callback);
   }

   public void getCompletions(
         String token,
         List<String> assocData,
         List<Integer> dataType,
         List<Integer> numCommas,
         String chainObjectName,
         String functionCallString,
         JsArrayString additionalArgs,
         JsArrayString excludeArgs,
         boolean excludeArgsFromObject,
         String filePath,
         String documentId,
         String line,
         boolean isConsole,
         ServerRequestCallback<Completions> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(token));
      setArrayString(params, 1, assocData);
      setArrayNumber(params, 2, dataType);
      setArrayNumber(params, 3, numCommas);
      params.set(4, new JSONString(chainObjectName));
      params.set(5, new JSONString(functionCallString));
      setArrayString(params, 6, additionalArgs);
      setArrayString(params, 7, excludeArgs);
      params.set(8, JSONBoolean.getInstance(excludeArgsFromObject));
      params.set(9, new JSONString(filePath));
      params.set(10, new JSONString(documentId));
      params.set(11, new JSONString(line));
      params.set(12, JSONBoolean.getInstance(isConsole));

      sendRequest(RPC_SCOPE,
                  GET_COMPLETIONS,
                  params,
                  requestCallback);
   }

   public void markdownGetCompletions(int completionType,
                                      JavaScriptObject completionData,
                                      ServerRequestCallback<Completions> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(completionType)
            .add(completionData)
            .get();

      sendRequest(RPC_SCOPE, MARKDOWN_GET_COMPLETIONS, params, requestCallback);
   }

   public void pythonActiveInterpreter(ServerRequestCallback<PythonInterpreter> requestCallback)
   {
      sendRequest(RPC_SCOPE, PYTHON_ACTIVE_INTERPRETER, requestCallback);
   }

   public void pythonFindInterpreters(ServerRequestCallback<PythonInterpreters> requestCallback)
   {
      sendRequest(RPC_SCOPE, PYTHON_FIND_INTERPRETERS, requestCallback);
   }

   public void pythonInterpreterInfo(String interpreterPath,
                                     ServerRequestCallback<PythonInterpreter> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(interpreterPath)
            .get();

      sendRequest(RPC_SCOPE, PYTHON_INTERPRETER_INFO, params, requestCallback);
   }

   public void pythonGetCompletions(String line,
                                    PythonCompletionContext context,
                                    ServerRequestCallback<Completions> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(line)
            .add(context)
            .get();

      sendRequest(RPC_SCOPE, PYTHON_GET_COMPLETIONS, params, requestCallback);
   }

   public void pythonGoToDefinition(String line,
                                    int column,
                                    ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(line)
            .add(column)
            .get();

      sendRequest(RPC_SCOPE, PYTHON_GO_TO_DEFINITION, params, requestCallback);
   }

   public void pythonGoToHelp(String line,
                              int column,
                              ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(line)
            .add(column)
            .get();

      sendRequest(RPC_SCOPE, PYTHON_GO_TO_HELP, params, requestCallback);
   }

   public void stanGetCompletions(String line,
                                  ServerRequestCallback<Completions> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(line)
            .get();

      sendRequest(RPC_SCOPE, STAN_GET_COMPLETIONS, params, requestCallback);
   }

   public void stanGetArguments(String function,
                                ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(function)
            .get();

      sendRequest(RPC_SCOPE, STAN_GET_ARGUMENTS, params, requestCallback);
   }

   public void stanRunDiagnostics(String filename,
                                  boolean useSourceDatabase,
                                  ServerRequestCallback<JsArray<AceAnnotation>> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(filename)
            .add(useSourceDatabase)
            .get();

      sendRequest(RPC_SCOPE, STAN_RUN_DIAGNOSTICS, params, requestCallback);
   }

   public void sqlGetCompletions(String line,
                                 String connection,
                                 SqlCompletionParseContext context,
                                 ServerRequestCallback<Completions> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(line)
            .add(connection)
            .add(context)
            .get();

      sendRequest(RPC_SCOPE, SQL_GET_COMPLETIONS, params, requestCallback);
   }

   public void getHelpAtCursor(String line, int cursorPos,
                               ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(line));
      params.set(1, new JSONNumber(cursorPos));
      sendRequest(RPC_SCOPE,
                  GET_HELP_AT_CURSOR,
                  params,
                  requestCallback);
   }

   public void removeAllObjects(boolean includeHidden,
                                ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  REMOVE_ALL_OBJECTS,
                  includeHidden,
                  requestCallback);
   }

   @Override
   public void removeObjects(List<String> objectNames,
                             ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONUtils.toJSONStringArray(objectNames));
      sendRequest(RPC_SCOPE,
                  REMOVE_OBJECTS,
                  params,
                  requestCallback);
   }

   public void downloadDataFile(
                  String dataFileUrl,
                  ServerRequestCallback<DownloadInfo> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  DOWNLOAD_DATA_FILE,
                  dataFileUrl,
                  requestCallback);
   }

   public void getDataPreview(String dataFilePath,
                              ServerRequestCallback<DataPreviewResult> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_DATA_PREVIEW,
                  dataFilePath,
                  requestCallback);
   }

   public void getOutputPreview(String dataFilePath,
                                String encoding,
                                boolean heading,
                                String separator,
                                String decimal,
                                String quote,
                                String comment,
                                ServerRequestCallback<DataPreviewResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(dataFilePath));
      params.set(1, new JSONString(encoding));
      params.set(2, JSONBoolean.getInstance(heading));
      params.set(3, new JSONString(separator));
      params.set(4, new JSONString(decimal));
      params.set(5, new JSONString(quote));
      params.set(6, new JSONString(comment));

      sendRequest(RPC_SCOPE,
                  GET_OUTPUT_PREVIEW,
                  params,
                  requestCallback);
   }

   @Override
   public void previewSql(String command,
                          ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(command));
      sendRequest(RPC_SCOPE, PREVIEW_SQL, params, requestCallback);
   }

   public void editCompleted(String text,
                             ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, EDIT_COMPLETED, text, requestCallback);
   }

   public void chooseFileCompleted(String file,
                                   ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CHOOSE_FILE_COMPLETED, file, requestCallback);
   }

   public void openFileDialogCompleted(String selectedPath,
                                       ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(selectedPath)
            .get();

      sendRequest(RPC_SCOPE, OPEN_FILE_DIALOG_COMPLETED, params, requestCallback);
   }

   public void getPackageState(
         boolean manual,
         ServerRequestCallback<PackageState> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(manual));
      sendRequest(RPC_SCOPE, GET_PACKAGE_STATE, params, requestCallback);
   }

   public void getPackageInstallContext(
               ServerRequestCallback<PackageInstallContext> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_PACKAGE_INSTALL_CONTEXT, requestCallback);
   }

   public void isPackageLoaded(
                       String packageName,
                       String libName,
                       ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(packageName));
      params.set(1, new JSONString(libName));
      sendRequest(RPC_SCOPE, IS_PACKAGE_LOADED, params, requestCallback);
   }

   public void isPackageInstalled(String packageName,
                                  String version,
                                  ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(packageName));
      params.set(1, (version == null) ? JSONNull.getInstance() : new JSONString(version));
      sendRequest(RPC_SCOPE, IS_PACKAGE_INSTALLED, params, requestCallback);
   }


   public void availablePackages(
         String repository,
         ServerRequestCallback<JsArrayString> requestCallback)
   {
      sendRequest(RPC_SCOPE, AVAILABLE_PACKAGES, repository, requestCallback);
   }

   public void checkForPackageUpdates(
         ServerRequestCallback<JsArray<PackageUpdate>> requestCallback)
   {
      sendRequest(RPC_SCOPE, CHECK_FOR_PACKAGE_UPDATES, requestCallback);
   }

   public void initDefaultUserLibrary(
                              ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, INIT_DEFAULT_USER_LIBRARY, requestCallback);
   }

   public void loadedPackageUpdatesRequired(
                              List<String> packages,
                              ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONArray(JsUtil.toJsArrayString(packages)));
      sendRequest(RPC_SCOPE,
                  LOADED_PACKAGE_UPDATES_REQUIRED,
                  params,
                  requestCallback);
   }

   public void ignoreNextLoadedPackageCheck(
                              ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, IGNORE_NEXT_LOADED_PACKAGE_CHECK, requestCallback);
   }

   public void getPackageNewsUrl(String packageName,
                                 String libraryPath,
                                 ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(packageName)
            .add(libraryPath)
            .get();

      sendRequest(RPC_SCOPE, GET_PACKAGE_NEWS_URL, params, requestCallback);
   }

   public void setCRANMirror(CRANMirror mirror,
                             ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, SET_CRAN_MIRROR, mirror, requestCallback);
   }

   public void getCRANMirrors(
                  ServerRequestCallback<JsArray<CRANMirror>> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_CRAN_MIRRORS, requestCallback);
   }

   public void getCRANActives(
                  ServerRequestCallback<JsArray<CRANMirror>> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_CRAN_ACTIVES, requestCallback);
   }

   public void suggestTopics(String prefix,
                             ServerRequestCallback<JsArrayString> requestCallback)
   {
      sendRequest(RPC_SCOPE, "suggest_topics", prefix, requestCallback);
   }

   public void getHelp(String topic,
                       String packageName,
                       int options,
                       ServerRequestCallback<HelpInfo> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(topic));
      if (packageName != null)
         params.set(1, new JSONString(packageName));
      else
         params.set(1, JSONNull.getInstance());
      params.set(2, new JSONNumber(options));

      sendRequest(RPC_SCOPE, GET_HELP, params, requestCallback);
   }

   public void getCustomHelp(String helpHandler,
                             String topic,
                             String source,
                             String language,
                             ServerRequestCallback<HelpInfo.Custom> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(helpHandler));
      params.set(1, new JSONString(topic));
      params.set(2, new JSONString(source));
      params.set(3, new JSONString(language));
      sendRequest(RPC_SCOPE, GET_CUSTOM_HELP, params, requestCallback);
   }

   public void getCustomParameterHelp(String helpHandler,
                                      String source,
                                      String language,
                                      ServerRequestCallback<HelpInfo.Custom> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(helpHandler));
      params.set(1, new JSONString(source));
      params.set(2, new JSONString(language));
      sendRequest(RPC_SCOPE, GET_CUSTOM_PARAMETER_HELP, params, requestCallback);
   }


   public void showHelpTopic(String what, String from, int type)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(what));
      params.set(1, from != null
                       ? new JSONString(from)
                       : JSONNull.getInstance());
      params.set(2, new JSONNumber(type));

      sendRequest(RPC_SCOPE,
                  SHOW_HELP_TOPIC,
                  params,
                  null);
   }

   public void showCustomHelpTopic(String helpHandler,
                                   String topic,
                                   String source)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(helpHandler));
      params.set(1, new JSONString(topic));
      params.set(2, new JSONString(source));
      sendRequest(RPC_SCOPE,
                  SHOW_CUSTOM_HELP_TOPIC,
                  params,
                  null);
   }

   public void search(String query,
                      ServerRequestCallback<JsArrayString> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  SEARCH,
                  query,
                  requestCallback);
   }

   @Override
   public void stat(String path,
                    ServerRequestCallback<FileSystemItem> requestCallback)
   {
      sendRequest(RPC_SCOPE, STAT, path, requestCallback);
   }

   @Override
   public void isTextFile(String path,
                          ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, IS_TEXT_FILE, path, requestCallback);
   }

   @Override
   public void isGitDirectory(String path,
                              ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, IS_GIT_DIRECTORY, path, requestCallback);
   }

   @Override
   public void isPackageDirectory(String path,
                                  ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, IS_PACKAGE_DIRECTORY, path, requestCallback);
   }

   @Override
   public void getFileContents(String path,
                               String encoding,
                               ServerRequestCallback<String> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      paramArray.set(0, new JSONString(path));
      paramArray.set(1, new JSONString(encoding));

      sendRequest(RPC_SCOPE, "get_file_contents", paramArray, requestCallback);
   }

   @Override
   public void listFiles(
                  FileSystemItem directory,
                  boolean monitor,
                  boolean showHidden,
                  ServerRequestCallback<DirectoryListing> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      paramArray.set(0, new JSONString(directory.getPath()));
      paramArray.set(1, JSONBoolean.getInstance(monitor));
      paramArray.set(2, JSONBoolean.getInstance(showHidden));

      sendRequest(RPC_SCOPE,
                  LIST_FILES,
                  paramArray,
                  requestCallback);
   }

   public void listAllFiles(String path,
                            String pattern,
                            ServerRequestCallback<JsArrayString> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONString(pattern));
      sendRequest(RPC_SCOPE,
                  LIST_ALL_FILES,
                  params,
                  requestCallback);
   }

   public void createFolder(FileSystemItem folder,
                            ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  CREATE_FOLDER,
                  folder.getPath(),
                  requestCallback);
   }

   public void deleteFiles(ArrayList<FileSystemItem> files,
                           ServerRequestCallback<Void> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      JSONArray pathArray = new JSONArray();
      for (int i=0; i<files.size(); i++)
         pathArray.set(i, new JSONString(files.get(i).getPath()));
      paramArray.set(0, pathArray);

      sendRequest(RPC_SCOPE, DELETE_FILES, paramArray, requestCallback);
   }

   public void copyFile(FileSystemItem sourceFile,
                        FileSystemItem targetFile,
                        boolean overwrite,
                        ServerRequestCallback<Void> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      paramArray.set(0, new JSONString(sourceFile.getPath()));
      paramArray.set(1, new JSONString(targetFile.getPath()));
      paramArray.set(2, JSONBoolean.getInstance(overwrite));

      sendRequest(RPC_SCOPE, COPY_FILE, paramArray, requestCallback);
   }


   public void moveFiles(ArrayList<FileSystemItem> files,
                         FileSystemItem targetDirectory,
                         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray paramArray = new JSONArray();

      JSONArray pathArray = new JSONArray();
      for (int i=0; i<files.size(); i++)
         pathArray.set(i, new JSONString(files.get(i).getPath()));

      paramArray.set(0, pathArray);
      paramArray.set(1, new JSONString(targetDirectory.getPath()));

      sendRequest(RPC_SCOPE, MOVE_FILES, paramArray, requestCallback);
   }

   public void renameFile(FileSystemItem file,
                          FileSystemItem targetFile,
                          ServerRequestCallback<Void> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      paramArray.set(0, new JSONString(file.getPath()));
      paramArray.set(1, new JSONString(targetFile.getPath()));

      sendRequest(RPC_SCOPE, RENAME_FILE, paramArray, requestCallback);
   }

   // this method is private as we generally don't want to expose
   // non-aliased paths to other parts of the client codebase
   // (most client-side APIs assume paths are aliased)
   private final String resolveAliasedPath(FileSystemItem file)
   {
      String path = file.getPath();
      if (path.startsWith("~"))
         path = userHomePath_ + path.substring(1);
      return path;
   }

   public String getFileUrl(FileSystemItem file)
   {
      if (Desktop.isDesktop())
      {
         String prefix = BrowseCap.isWindowsDesktop()
               ? "file:///"
               : "file://";

         return prefix + resolveAliasedPath(file);
      }
      else if (!file.isDirectory())
      {
         if (file.isWithinHome())
         {
            return getApplicationURL(FILES_SCOPE) + "/" + file.homeRelativePath();
         }
         else
         {
            String url = getApplicationURL(FILE_SHOW);
            url += "?path=" + URL.encodeQueryString(file.getPath());
            return url;
         }
      }
      else
      {
         return null;
      }
   }

   // get file upload base url
   public String getFileUploadUrl()
   {
      String url = getApplicationURL(UPLOAD_SCOPE);

      // if we are in a load balanced session, we need to send the upload to the correct node
      String sessionNode = session_.getSessionInfo().getSessionNode();
      if (!sessionNode.isEmpty())
      {
         url += "?host_node=" + sessionNode;
      }

      return url;
   }

   public void completeUpload(FileUploadToken token,
                              boolean commit,
                              ServerRequestCallback<Void> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      paramArray.set(0, new JSONObject(token));
      paramArray.set(1, JSONBoolean.getInstance(commit));
      sendRequest(RPC_SCOPE, COMPLETE_UPLOAD, paramArray, requestCallback);
   }

   public String getFileExportUrl(String name, FileSystemItem file)
   {
      return getApplicationURL(EXPORT_SCOPE) + "/" + URL.encodePathSegment(name) + "?" +
         "name=" + URL.encodeQueryString(name) + "&" +
         "file=" + URL.encodeQueryString(file.getPath());
   }

   public void writeConfigJSON(String path,
                               JavaScriptObject object,
                               ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONObject(object));
      sendRequest(RPC_SCOPE, "write_config_json", params, requestCallback);
   }

   public void readConfigJSON(String path,
                              boolean logErrorIfNotFound,
                              ServerRequestCallback<JavaScriptObject> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, JSONBoolean.getInstance(logErrorIfNotFound));
      sendRequest(RPC_SCOPE, "read_config_json", params, requestCallback);
   }

   public String getFileExportUrl(String name,
                                  FileSystemItem parentDirectory,
                                  ArrayList<String> filenames)
   {
      // build url params for files
      StringBuilder files = new StringBuilder();
      for (int i = 0; i<filenames.size(); i++)
      {
         files.append("file").append(i).append("=");
         files.append(URL.encodeQueryString(filenames.get(i)));
         files.append("&");
      }

      // return url
      return getApplicationURL(EXPORT_SCOPE) + "/" + URL.encodePathSegment(name) + "?" +
        "name=" + URL.encodeQueryString(name) + "&" +
        "parent=" + URL.encodeQueryString(parentDirectory.getPath()) + "&" +
         files.toString();
   }


   // get graphics url
   public String getGraphicsUrl(String filename)
   {
      return getApplicationURL(GRAPHICS_SCOPE) + "/" + filename;
   }

   public String getPlotExportUrl(String type,
                                  int width,
                                  int height,
                                  boolean attachment)
   {
      // build preview URL
      String previewURL = getGraphicsUrl("plot." + type);
      previewURL += "?";
      previewURL += "width=" + width;
      previewURL += "&";
      previewURL += "height=" + height;
      // append random number to default over-aggressive image caching
      // by browsers
      previewURL += "&randomizer=" + Random.nextInt();
      if (attachment)
         previewURL += "&attachment=1";

      return previewURL;
   }

   public void nextPlot(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, NEXT_PLOT, requestCallback);
   }

   public void previousPlot(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, PREVIOUS_PLOT, requestCallback);
   }

   public void removePlot(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, REMOVE_PLOT, requestCallback);
   }

   public void clearPlots(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CLEAR_PLOTS, requestCallback);
   }

   public void refreshPlot(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, REFRESH_PLOT, requestCallback);
   }

   public void savePlotAs(FileSystemItem file,
                          String format,
                          int width,
                          int height,
                          boolean overwrite,
                          ServerRequestCallback<Bool> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(file.getPath()));
      params.set(1, new JSONString(format));
      params.set(2, new JSONNumber(width));
      params.set(3, new JSONNumber(height));
      params.set(4, JSONBoolean.getInstance(overwrite));
      sendRequest(RPC_SCOPE, SAVE_PLOT_AS, params, requestCallback);
   }

   public void savePlotAsPdf(FileSystemItem file,
                             double widthInches,
                             double heightInches,
                             boolean useCairoPdf,
                             boolean overwrite,
                             ServerRequestCallback<Bool> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(file.getPath()));
      params.set(1, new JSONNumber(widthInches));
      params.set(2, new JSONNumber(heightInches));
      params.set(3, JSONBoolean.getInstance(useCairoPdf));
      params.set(4, JSONBoolean.getInstance(overwrite));
      sendRequest(RPC_SCOPE, SAVE_PLOT_AS_PDF, params, requestCallback);
   }

   public void copyPlotToClipboardMetafile(
                              int width,
                              int height,
                              ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(width));
      params.set(1, new JSONNumber(height));
      sendRequest(RPC_SCOPE,
                  COPY_PLOT_TO_CLIPBOARD_METAFILE,
                  params,
                  requestCallback);
   }

   @Override
   public void copyPlotToCocoaPasteboard(
                                 int width,
                                 int height,
                                 ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(width));
      params.set(1, new JSONNumber(height));
      sendRequest(RPC_SCOPE,
                  COPY_PLOT_TO_COCOA_PASTEBOARD,
                  params,
                  requestCallback);
   }

   public void getUniqueSavePlotStem(String directory,
                                  ServerRequestCallback<String> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_UNIQUE_SAVE_PLOT_STEM, directory, requestCallback);
   }

   public void getSavePlotContext(
                  String directory,
                  ServerRequestCallback<SavePlotAsImageContext> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_SAVE_PLOT_CONTEXT,
                  directory,
                  requestCallback);
   }

   public void locatorCompleted(Point point,
                                ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, LOCATOR_COMPLETED, point, requestCallback);
   }

   public void setManipulatorValues(JSONObject values,
                                    ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, values);
      sendRequest(RPC_SCOPE, SET_MANIPULATOR_VALUES, params, requestCallback);
   }

   public void manipulatorPlotClicked(
                                 int x,
                                 int y,
                                 ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(x));
      params.set(1, new JSONNumber(y));
      sendRequest(RPC_SCOPE, MANIPULATOR_PLOT_CLICKED, params, requestCallback);
   }

   public void validateProjectPath(String projectPath,
                                   ServerRequestCallback<Boolean> callback)
   {
      sendRequest(RPC_SCOPE, "validate_project_path", projectPath, callback);
   }

   public void createShinyApp(String appName,
                              String appType,
                              String appDir,
                              ServerRequestCallback<JsArrayString> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(appName));
      params.set(1, new JSONString(appType));
      params.set(2, new JSONString(appDir));
      sendRequest(RPC_SCOPE, CREATE_SHINY_APP, params, requestCallback);
   }

   public void createPlumberAPI(String apiName,
                                String apiDir,
                                ServerRequestCallback<JsArrayString> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(apiName));
      params.set(1, new JSONString(apiDir));
      sendRequest(RPC_SCOPE, CREATE_PLUMBER_API, params, requestCallback);
   }

   public void discoverPackageDependencies(String docId,
                                           String fileType,
                                           ServerRequestCallback<AvailablePackagesReadyEvent.Data> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(docId)
            .add(fileType)
            .get();

      sendRequest(RPC_SCOPE, DISCOVER_PACKAGE_DEPENDENCIES, params, requestCallback);

   }

   public void getEditorContextCompleted(GetEditorContextEvent.SelectionData data,
                                         ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_EDITOR_CONTEXT_COMPLETED, data, requestCallback);
   }

   public void setSourceDocumentDirty(String docId,
         boolean dirty,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      params.set(1, JSONBoolean.getInstance(dirty));
      sendRequest(RPC_SCOPE, "set_source_document_dirty", params,
                  requestCallback);
   }

   public void getNewProjectContext(
                        ServerRequestCallback<NewProjectContext> callback)
   {
      sendRequest(RPC_SCOPE, GET_NEW_PROJECT_CONTEXT, callback);
   }

   @Override
   public void getNewSessionUrl(String hostPageUrl,
                                boolean isProject,
                                String directory,
                                RVersionSpec rVersion,
                                JavaScriptObject launchSpec,
                                ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(hostPageUrl));
      params.set(1, JSONBoolean.getInstance(isProject));
      params.set(2, new JSONString(directory));
      params.set(3, rVersion != null ? new JSONObject(rVersion) :
                                       JSONNull.getInstance());
      params.set(4, launchSpec != null ? new JSONObject(launchSpec) :
            JSONNull.getInstance());
      sendRequest(RPC_SCOPE, GET_NEW_SESSION_URL, params, callback);
   }

   @Override
   public void getActiveSessions(
             String hostPageUrl,
             ServerRequestCallback<JsArray<ActiveSession>> callback)
   {
      sendRequest(RPC_SCOPE, GET_ACTIVE_SESSIONS, hostPageUrl, callback);
   }

   @Override
   public void setSessionLabel(
             String hostPageUrl,
             ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, SET_SESSION_LABEL, hostPageUrl, callback);
   }

   @Override
   public void deleteSessionDir(
             String sessionId,
             ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, DELETE_SESSION_DIR, sessionId, callback);
   }

   @Override
   public void getAvailableRVersions(
         ServerRequestCallback<JsArray<RVersionSpec>> callback)
   {
      sendRequest(RPC_SCOPE, GET_AVAILABLE_R_VERSIONS, callback);
   }

   public void getProjectRVersion(
         String projectDir,
         ServerRequestCallback<RVersionSpec> callback)
   {
      sendRequest(RPC_SCOPE, "get_project_r_version", projectDir, callback);
   }

   public void getProjectFilePath(
         String projectId,
         ServerRequestCallback<String> callback)
   {
      sendRequest(RPC_SCOPE, "get_project_file_path", projectId, callback);
   }

   @Override
   public void findProjectInFolder(
         String folder,
         ServerRequestCallback<String> callback)
   {
      sendRequest(RPC_SCOPE, "find_project_in_folder", folder, callback);
   }

   @Override
   public void executeRCode(String code,
                            ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0,  new JSONString(code));
      sendRequest(RPC_SCOPE, EXECUTE_R_CODE, params, requestCallback);
   }

   @Override
   public void createProject(String projectFile,
                             NewPackageOptions newPackageOptions,
                             NewShinyAppOptions newShinyAppOptions,
                             ProjectTemplateOptions projectTemplateOptions,
                             ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(projectFile));
      params.set(1, newPackageOptions != null ?
               new JSONObject(newPackageOptions) : JSONNull.getInstance());
      params.set(2, newShinyAppOptions != null ?
            new JSONObject(newShinyAppOptions) : JSONNull.getInstance());
      params.set(3, projectTemplateOptions != null ?
            new JSONObject(projectTemplateOptions) : JSONNull.getInstance());
      sendRequest(RPC_SCOPE, CREATE_PROJECT, params, requestCallback);
   }

   @Override
   public void createProjectFile(String projectDir,
                                 ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(projectDir)));
      sendRequest(RPC_SCOPE, CREATE_PROJECT_FILE, params, requestCallback);
   }

   public void getProjectTemplateRegistry(
         ServerRequestCallback<ProjectTemplateRegistry> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_PROJECT_TEMPLATE_REGISTRY, requestCallback);
   }

   public void executeProjectTemplate(String pkgName,
                                      String pkgBinding,
                                      ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(pkgName));
      params.set(1, new JSONString(pkgBinding));
      sendRequest(RPC_SCOPE, EXECUTE_PROJECT_TEMPLATE, params, requestCallback);
   }

   public void packageSkeleton(String packageName,
                               String packageDirectory,
                               JsArrayString sourceFiles,
                               boolean usingRcpp,
                               ServerRequestCallback<RResult<Void>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(packageName));
      params.set(1, new JSONString(packageDirectory));
      setArrayString(params, 2, sourceFiles);
      params.set(3, JSONBoolean.getInstance(usingRcpp));

      sendRequest(RPC_SCOPE, PACKAGE_SKELETON, params, requestCallback);
   }

   public void readProjectOptions(ServerRequestCallback<RProjectOptions> callback)
   {
      sendRequest(RPC_SCOPE, READ_PROJECT_OPTIONS, callback);
   }

   public void writeProjectOptions(RProjectOptions options,
                                  ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, WRITE_PROJECT_OPTIONS, options, callback);
   }
   
   public void writeProjectConfig(RProjectConfig config, ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, WRITE_PROJECT_CONFIG, config, callback);
   }


   public void writeProjectVcsOptions(RProjectVcsOptions options,
                                      ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, WRITE_PROJECT_VCS_OPTIONS, options, callback);
   }

   public void newDocument(String filetype,
                           String contents,
                           JsObject properties,
                           ServerRequestCallback<SourceDocument> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(filetype));
      params.set(1, contents != null ? new JSONString(contents) :
                                       JSONNull.getInstance());
      params.set(2, new JSONObject(properties));
      sendRequest(RPC_SCOPE, NEW_DOCUMENT, params, requestCallback);
   }

   public void openDocument(String path,
                            String filetype,
                            String encoding,
                            ServerRequestCallback<SourceDocument> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONString(filetype));
      params.set(2, encoding != null ? new JSONString(encoding)
                                     : JSONNull.getInstance());
      sendRequest(RPC_SCOPE, OPEN_DOCUMENT, params, requestCallback);
   }

   public void saveDocument(String id,
                            String path,
                            String fileType,
                            String encoding,
                            String foldSpec,
                            JsArray<ChunkDefinition> chunkDefs,
                            String contents,
                            boolean retryWrite,
                            ServerRequestCallback<String> requestCallback)
   {
      eventBus_.fireEvent(new ApplicationTutorialEvent(ApplicationTutorialEvent.FILE_SAVE));

      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, path == null ? JSONNull.getInstance() : new JSONString(path));
      params.set(2, fileType == null ? JSONNull.getInstance() : new JSONString(fileType));
      params.set(3, encoding == null ? JSONNull.getInstance() : new JSONString(encoding));
      params.set(4, new JSONString(StringUtil.notNull(foldSpec)));
      params.set(5, chunkDefs == null ? JSONNull.getInstance() : new JSONObject(chunkDefs));
      params.set(6, new JSONString(contents));
      params.set(7, JSONBoolean.getInstance(retryWrite));
      sendRequest(RPC_SCOPE, SAVE_DOCUMENT, params, requestCallback);
   }

   public void saveDocumentDiff(String id,
                                String path,
                                String fileType,
                                String encoding,
                                String foldSpec,
                                JsArray<ChunkDefinition> chunkDefs,
                                String replacement,
                                int offset,
                                int length,
                                boolean valid,
                                String hash,
                                boolean retryWrite,
                                ServerRequestCallback<String> requestCallback)
   {
      eventBus_.fireEvent(new ApplicationTutorialEvent(ApplicationTutorialEvent.FILE_SAVE));

      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, path == null ? JSONNull.getInstance() : new JSONString(path));
      params.set(2, fileType == null ? JSONNull.getInstance() : new JSONString(fileType));
      params.set(3, encoding == null ? JSONNull.getInstance() : new JSONString(encoding));
      params.set(4, new JSONString(StringUtil.notNull(foldSpec)));
      params.set(5, chunkDefs == null ? JSONNull.getInstance() : new JSONObject(chunkDefs));
      params.set(6, new JSONString(replacement));
      params.set(7, new JSONNumber(offset));
      params.set(8, new JSONNumber(length));
      params.set(9, JSONBoolean.getInstance(valid));
      params.set(10, new JSONString(hash));
      params.set(11, JSONBoolean.getInstance(retryWrite));
      sendRequest(RPC_SCOPE, SAVE_DOCUMENT_DIFF, params, requestCallback);
   }

   public void checkForExternalEdit(
         String id,
         ServerRequestCallback<CheckForExternalEditResult> requestCallback)
   {
      sendRequest(RPC_SCOPE, CHECK_FOR_EXTERNAL_EDIT, id, requestCallback);
   }

   public void ignoreExternalEdit(String id,
                                  ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, IGNORE_EXTERNAL_EDIT, id, requestCallback);
   }

   public void closeDocument(String id,
                             ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CLOSE_DOCUMENT, id, requestCallback);
   }

   public void closeAllDocuments(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CLOSE_ALL_DOCUMENTS, requestCallback);
   }

   public void getSourceTemplate(String name,
                                 String template,
                                 ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(name));
      params.set(1, new JSONString(template));
      sendRequest(RPC_SCOPE, GET_SOURCE_TEMPLATE, params, requestCallback);
   }

   public void getSourceDocument(String docId,
                        ServerRequestCallback<SourceDocument> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      sendRequest(RPC_SCOPE, GET_SOURCE_DOCUMENT, params, requestCallback);
   }

   public void explorerInspectObject(String handleId,
                                     String extractingCode,
                                     String objectName,
                                     String objectAccess,
                                     JsArrayString tags,
                                     int fromIndex,
                                     ServerRequestCallback<ObjectExplorerInspectionResult> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(handleId)
            .add(extractingCode)
            .add(objectName)
            .add(objectAccess)
            .add(tags)
            .add(fromIndex)
            .get();

      sendRequest(RPC_SCOPE, EXPLORER_INSPECT_OBJECT, params, requestCallback);
   }

   public void explorerBeginInspect(String handleId,
                                    String objectName,
                                    ServerRequestCallback<ObjectExplorerInspectionResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(handleId));
      params.set(1, new JSONString(objectName));
      sendRequest(RPC_SCOPE, EXPLORER_BEGIN_INSPECT, params, requestCallback);
   }

   public void explorerEndInspect(String handleId,
                                  ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(handleId));
      sendRequest(RPC_SCOPE, EXPLORER_END_INSPECT, params, requestCallback);
   }

   public void createRdShell(
                        String name,
                        String type,
                        ServerRequestCallback<RdShellResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(name));
      params.set(1, new JSONString(type));
      sendRequest(RPC_SCOPE, CREATE_RD_SHELL, params, requestCallback);
   }

   public void setSourceDocumentOnSave(String id,
                                       boolean shouldSourceOnSave,
                                       ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, JSONBoolean.getInstance(shouldSourceOnSave));
      sendRequest(RPC_SCOPE,
                  SET_SOURCE_DOCUMENT_ON_SAVE,
                  params,
                  requestCallback);
   }

   public void setDocOrder(List<String> ids,
                           ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();

      params.set(0, JSONUtils.toJSONStringArray(ids));
      sendRequest(RPC_SCOPE,
                  SET_DOC_ORDER,
                  params,
                  requestCallback);
   }

   public void getTexCapabilities(
                  ServerRequestCallback<TexCapabilities> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_TEX_CAPABILITIES,
                  requestCallback);
   }

   public void getChunkOptions(
                       String weaveType,
                       ServerRequestCallback<RnwChunkOptions> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_CHUNK_OPTIONS, weaveType, requestCallback);
   }

   public String getProgressUrl(String message)
   {
      String url = getApplicationURL(SOURCE_SCOPE + "/" + "progress");
      url += "?message=" + URL.encodeQueryString(message);
      return url;
   }


   public void saveActiveDocument(String contents,
                                  boolean sweave,
                                  String rnwWeave,
                                  ServerRequestCallback<Void> requestCallback)
   {
      eventBus_.fireEvent(new ApplicationTutorialEvent(ApplicationTutorialEvent.FILE_SAVE));
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(contents));
      params.set(1, JSONBoolean.getInstance(sweave));
      params.set(2, new JSONString(rnwWeave));

      sendRequest(RPC_SCOPE,
                  SAVE_ACTIVE_DOCUMENT,
                  params,
                  requestCallback);
   }

   public void requestDocumentSaveCompleted(boolean isSuccessfulSave,
                                            ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(isSuccessfulSave));
      sendRequest(RPC_SCOPE,
                  REQUEST_DOCUMENT_SAVE_COMPLETED,
                  params,
                  requestCallback);
   }

   public void requestDocumentCloseCompleted(boolean isSuccessfulClose,
                                            ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(isSuccessfulClose));
      sendRequest(RPC_SCOPE,
                  REQUEST_DOCUMENT_CLOSE_COMPLETED,
                  params,
                  requestCallback);
   }

   public void modifyDocumentProperties(
         String id,
         HashMap<String, String> properties,
         ServerRequestCallback<Void> requestCallback)
   {
      eventBus_.fireEvent(new ApplicationTutorialEvent(ApplicationTutorialEvent.FILE_SAVE));

      JSONObject obj = new JSONObject();
      for (Map.Entry<String, String> entry : properties.entrySet())
      {
         obj.put(entry.getKey(), entry.getValue() == null
                                 ? JSONNull.getInstance()
                                 : new JSONString(entry.getValue()));
      }

      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, obj);

      sendRequest(RPC_SCOPE, MODIFY_DOCUMENT_PROPERTIES, params, requestCallback);
   }

   public void getDocumentProperties(
         String path,
         ServerRequestCallback<JsObject> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));

      sendRequest(RPC_SCOPE, GET_DOCUMENT_PROPERTIES, params, requestCallback);
   }

   public void revertDocument(String id,
                              String fileType,
                              ServerRequestCallback<SourceDocument> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, new JSONString(fileType));
      sendRequest(RPC_SCOPE, REVERT_DOCUMENT, params, requestCallback);
   }

   public void reopenWithEncoding(String id,
                              String encoding,
                              ServerRequestCallback<SourceDocument> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, new JSONString(encoding));
      sendRequest(RPC_SCOPE, REOPEN_WITH_ENCODING, params, requestCallback);
   }

   public void removeContentUrl(String contentUrl,
                                ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, REMOVE_CONTENT_URL, contentUrl, requestCallback);
   }

   public void removeCachedData(String cacheKey,
                                ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, REMOVE_CACHED_DATA, cacheKey, requestCallback);
   }

   public void ensureFileExists(String path,
                                ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      sendRequest(RPC_SCOPE, ENSURE_FILE_EXISTS, params, requestCallback);
   }

   public void detectFreeVars(String code,
                              ServerRequestCallback<JsArrayString> requestCallback)
   {
      sendRequest(RPC_SCOPE, DETECT_FREE_VARS, code, requestCallback);
   }

   public void iconvlist(ServerRequestCallback<IconvListResult> requestCallback)
   {
      sendRequest(RPC_SCOPE, ICONVLIST, requestCallback);
   }

   @Override
   public void extractRmdFromNotebook(String inputPath,
              ServerRequestCallback<SourceDocumentResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(inputPath));
      sendRequest(RPC_SCOPE, "extract_rmd_from_notebook", params,
            requestCallback);
   }

   @Override
   public void createNotebook(
                 CompileNotebookOptions options,
                 ServerRequestCallback<CompileNotebookResult> requestCallback)
   {
      sendRequest(RPC_SCOPE, "create_notebook", options, requestCallback);
   }

   @Override
   public void isReadOnlyFile(String path,
                              ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, "is_read_only_file", path, requestCallback);
   }

   @Override
   public void getScriptRunCommand(String interpreter,
                                   String path,
                                   ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(interpreter)));
      params.set(1, new JSONString(path));
      sendRequest(RPC_SCOPE, "get_script_run_command", params, callback);
   }

   @Override
   public void getMinimalSourcePath(String path,
                                    ServerRequestCallback<String> callback)
   {
      sendRequest(RPC_SCOPE, "get_minimal_source_path", path, callback);
   }

   @Override
   public void getShinyCapabilities(
         ServerRequestCallback<ShinyCapabilities> requestCallback)
   {
      sendRequest(RPC_SCOPE, "get_shiny_capabilities", requestCallback);
   }

   public void getRecentHistory(
         long maxItems,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_RECENT_HISTORY, maxItems, requestCallback);
   }

   public void getHistoryItems(
         long startIndex, // inclusive
         long endIndex, // exclusive
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(startIndex));
      params.set(1, new JSONNumber(endIndex));
      sendRequest(RPC_SCOPE, GET_HISTORY_ITEMS, params, requestCallback);
   }


   public void removeHistoryItems(JsArrayNumber itemIndexes,
                                  ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  REMOVE_HISTORY_ITEMS,
                  itemIndexes,
                  requestCallback);
   }


   public void clearHistory(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CLEAR_HISTORY, requestCallback);
   }


   public void getHistoryArchiveItems(
         long startIndex, // inclusive
         long endIndex,   // exclusive
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(startIndex));
      params.set(1, new JSONNumber(endIndex));
      sendRequest(RPC_SCOPE, GET_HISTORY_ARCHIVE_ITEMS, params, requestCallback);
   }

   public void searchHistory(
         String query,
         long maxEntries,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(query));
      params.set(1, new JSONNumber(maxEntries));
      sendRequest(RPC_SCOPE, SEARCH_HISTORY, params, requestCallback);
   }

   public void searchHistoryArchive(
         String query,
         long maxEntries,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(query));
      params.set(1, new JSONNumber(maxEntries));
      sendRequest(RPC_SCOPE, SEARCH_HISTORY_ARCHIVE, params, requestCallback);
   }

   public void searchHistoryArchiveByPrefix(
         String prefix,
         long maxEntries,
         boolean uniqueOnly,
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(prefix));
      params.set(1, new JSONNumber(maxEntries));
      params.set(2, JSONBoolean.getInstance(uniqueOnly));
      sendRequest(RPC_SCOPE, SEARCH_HISTORY_ARCHIVE_BY_PREFIX, params, requestCallback);
   }

   public void gitAdd(ArrayList<String> paths,
                      ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = JSONUtils.toJSONStringArray(paths);

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, GIT_ADD, params, requestCallback);
   }

   public void gitRemove(ArrayList<String> paths,
                         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = JSONUtils.toJSONStringArray(paths);

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, GIT_REMOVE, params, requestCallback);
   }

   public void gitDiscard(ArrayList<String> paths,
                          ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = JSONUtils.toJSONStringArray(paths);

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, GIT_DISCARD, params, requestCallback);
   }

   public void gitRevert(ArrayList<String> paths,
                         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = JSONUtils.toJSONStringArray(paths);

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, GIT_REVERT, params, requestCallback);
   }

   public void gitStage(ArrayList<String> paths,
                        ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = JSONUtils.toJSONStringArray(paths);

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, GIT_STAGE, params, requestCallback);
   }

   public void gitUnstage(ArrayList<String> paths,
                          ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = JSONUtils.toJSONStringArray(paths);

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, GIT_UNSTAGE, params, requestCallback);
   }

   @Override
   public void gitAllStatus(ServerRequestCallback<AllStatus> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_ALL_STATUS, requestCallback);
   }

   @Override
   public void gitFullStatus(ServerRequestCallback<JsArray<StatusAndPathInfo>> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_FULL_STATUS, requestCallback);
   }

   @Override
   public void gitCreateBranch(String branch,
                               ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(branch));
      sendRequest(RPC_SCOPE, GIT_CREATE_BRANCH, params,
            new ConsoleProcessCallbackAdapter(requestCallback));
   }

   @Override
   public void gitListBranches(ServerRequestCallback<BranchesInfo> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_LIST_BRANCHES, requestCallback);
   }

   @Override
   public void gitListRemotes(ServerRequestCallback<JsArray<RemotesInfo>> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_LIST_REMOTES, requestCallback);
   }

   @Override
   public void gitAddRemote(String name,
                            String url,
                            ServerRequestCallback<JsArray<RemotesInfo>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(name));
      params.set(1, new JSONString(url));
      sendRequest(RPC_SCOPE, GIT_ADD_REMOTE, params, requestCallback);
   }

   @Override
   public void gitCheckout(String id,
                           ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_CHECKOUT, id,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   @Override
   public void gitCheckoutRemote(String branch,
                                 String remote,
                                 ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(branch));
      params.set(1, new JSONString(remote));
      sendRequest(RPC_SCOPE, GIT_CHECKOUT_REMOTE, params,
            new ConsoleProcessCallbackAdapter(requestCallback));
   }

   public void gitCommit(String message,
                         boolean amend,
                         boolean signOff,
                         ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(message));
      params.set(1, JSONBoolean.getInstance(amend));
      params.set(2, JSONBoolean.getInstance(signOff));
      sendRequest(RPC_SCOPE, GIT_COMMIT, params,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   private class ConsoleProcessCallbackAdapter
         extends ServerRequestCallback<ConsoleProcessInfo>
   {
      private ConsoleProcessCallbackAdapter(
            ServerRequestCallback<ConsoleProcess> callback)
      {
         callback_ = callback;
      }

      @Override
      public void onResponseReceived(ConsoleProcessInfo response)
      {
         pConsoleProcessFactory_.get().connectToProcess(response,
                                                        callback_);
      }

      @Override
      public void onError(ServerError error)
      {
         callback_.onError(error);
      }

      private final ServerRequestCallback<ConsoleProcess> callback_;
   }

   public void gitPush(ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_PUSH,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   public void gitPushBranch(String branch,
                             String remote,
                             ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(branch));
      params.set(1, new JSONString(remote));
      sendRequest(RPC_SCOPE, GIT_PUSH_BRANCH, params,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }


   @Override
   public void vcsClone(VcsCloneOptions options,
                        ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  VCS_CLONE,
                  options,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   public void gitPull(ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_PULL,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   public void gitPullRebase(ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_PULL_REBASE,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   @Override
   public void askpassCompleted(String value, boolean remember,
                                ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, value == null ? JSONNull.getInstance()
                                  : new JSONString(value));
      params.set(1, JSONBoolean.getInstance(remember));
      sendRequest(RPC_SCOPE, ASKPASS_COMPLETED, params, true, requestCallback);
   }

   @Override
   public void createSshKey(CreateKeyOptions options,
                            ServerRequestCallback<CreateKeyResult> request)
   {
      sendRequest(RPC_SCOPE, CREATE_SSH_KEY, options, request);
   }


   @Override
   public void gitSshPublicKey(String privateKeyPath,
                               ServerRequestCallback<String> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GIT_SSH_PUBLIC_KEY,
                  privateKeyPath,
                  requestCallback);
   }

   @Override
   public void gitHasRepo(String directory,
                          ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_HAS_REPO, directory, requestCallback);
   }

   @Override
   public void gitInitRepo(String directory,
                           ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, GIT_INIT_REPO, directory, requestCallback);
   }

   @Override
   public void gitGetIgnores(String path,
                             ServerRequestCallback<ProcessResult> callback)
   {
      sendRequest(RPC_SCOPE, GIT_GET_IGNORES, path, callback);
   }

   @Override
   public void gitSetIgnores(String path,
                             String ignores,
                             ServerRequestCallback<ProcessResult> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONString(ignores));
      sendRequest(RPC_SCOPE, GIT_SET_IGNORES, params, callback);
   }

   @Override
   public void gitGithubRemoteUrl(String view,
                                  String path,
                                  ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(view));
      params.set(1, new JSONString(path));
      sendRequest(RPC_SCOPE, GIT_GITHUB_REMOTE_URL, params, callback);
   }

   @Override
   public void gitDiffFile(String path,
                           PatchMode mode,
                           int contextLines,
                           boolean noSizeWarning,
                           boolean ignoreWhitespace,
                           ServerRequestCallback<DiffResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONNumber(mode.getValue()));
      params.set(2, new JSONNumber(contextLines));
      params.set(3, JSONBoolean.getInstance(noSizeWarning));
      params.set(4, JSONBoolean.getInstance(ignoreWhitespace));
      sendRequest(RPC_SCOPE, GIT_DIFF_FILE, params, requestCallback);
   }

   @Override
   public void gitApplyPatch(String patch,
                             PatchMode mode,
                             String sourceEncoding,
                             ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(patch));
      params.set(1, new JSONNumber(mode.getValue()));
      params.set(2, new JSONString(sourceEncoding));
      sendRequest(RPC_SCOPE, GIT_APPLY_PATCH, params, requestCallback);
   }

   public void gitHistoryCount(String spec,
                               FileSystemItem fileFilter,
                               String searchText,
                               ServerRequestCallback<CommitCount> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(spec));
      params.set(1, fileFilter != null ?
                new JSONString(fileFilter.getPath()) : JSONNull.getInstance());
      params.set(2, new JSONString(searchText));
      sendRequest(RPC_SCOPE, GIT_HISTORY_COUNT, params, requestCallback);
   }

   @Override
   public void gitHistory(String spec,
                          FileSystemItem fileFilter,
                          int skip,
                          int maxentries,
                          String searchText,
                          ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(spec));
      params.set(1, fileFilter != null ?
            new JSONString(fileFilter.getPath()) : JSONNull.getInstance());
      params.set(2, new JSONNumber(skip));
      params.set(3, new JSONNumber(maxentries));
      params.set(4, new JSONString(StringUtil.notNull(searchText)));
      sendRequest(RPC_SCOPE, GIT_HISTORY, params, requestCallback);
   }

   @Override
   public void gitShow(String rev,
                       boolean noSizeWarning,
                       ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(rev));
      params.set(1, JSONBoolean.getInstance(noSizeWarning));

      sendRequest(RPC_SCOPE, GIT_SHOW, params, requestCallback);
   }

   @Override
   public void gitShowFile(String rev,
                           String filename,
                           ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(rev));
      params.set(1, new JSONString(filename));
      sendRequest(RPC_SCOPE, GIT_SHOW_FILE, params, requestCallback);
   }

   @Override
   public void gitExportFile(String rev,
                             String filename,
                             String targetPath,
                             ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(rev));
      params.set(1, new JSONString(filename));
      params.set(2, new JSONString(targetPath));
      sendRequest(RPC_SCOPE, GIT_EXPORT_FILE, params, requestCallback);
   }


   @Override
   public void getPublicKey(ServerRequestCallback<PublicKeyInfo> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_PUBLIC_KEY, requestCallback);
   }

   @Override
   public void listGet(String listName,
                       ServerRequestCallback<JsArrayString> requestCallback)
   {
      sendRequest(RPC_SCOPE, LIST_GET, listName, requestCallback);
   }

   @Override
   public void listSetContents(String listName,
                               ArrayList<String> list,
                               ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(listName));
      params.set(1, new JSONArray(JsUtil.toJsArrayString(list)));

      sendRequest(RPC_SCOPE, LIST_SET_CONTENTS, params, requestCallback);
   }

   @Override
   public void listPrependItem(String listName,
                               String value,
                               ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  LIST_PREPEND_ITEM,
                  listName,
                  value,
                  requestCallback);
   }

   @Override
   public void listAppendItem(String listName,
                              String value,
                              ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  LIST_APPEND_ITEM,
                  listName,
                  value,
                  requestCallback);
   }

   @Override
   public void listRemoveItem(String listName,
                              String value,
                              ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  LIST_REMOVE_ITEM,
                  listName,
                  value,
                  requestCallback);
   }

   @Override
   public void listClear(String listName,
                         ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, LIST_CLEAR, listName, requestCallback);
   }

   // package-visible methods for peer classes RemoteServerAuth and
   // RemoveServerEventListener


   boolean isDisconnected(String scope)
   {
      if (scope == JOB_LAUNCHER_RPC_SCOPE)
      {
         // no concept of being disconnected for launcher scope
         return false;
      }
      else
      {
         return isDisconnected();
      }
   }

   boolean isDisconnected()
   {
      return disconnected_;
   }

   EventBus getEventBus()
   {
      return eventBus_;
   }

   RpcRequest getEvents(
                  int lastEventId,
                  ServerRequestCallback<JsArray<ClientEvent>> requestCallback,
                  RetryHandler retryHandler)
   {
      // satellite windows should never call getEvents directly!
      if (Satellite.isCurrentWindowSatellite())
      {
         Debug.log("Satellite window should not call getEvents!");
         assert false;
      }

      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(lastEventId));
      return sendRequest(EVENTS_SCOPE,
                         "get_events",
                         params,
                         null, // kwParams
                         false, // redactLog
                         false, // refreshCreds
                         null, // resultFieldName
                         requestCallback,
                         retryHandler);
   }

   void handleUnauthorizedError()
   {
      UnauthorizedEvent event = new UnauthorizedEvent();
      eventBus_.fireEvent(event);
   }

   protected <T> void sendRequest(String scope,
                                String method,
                                ServerRequestCallback<T> requestCallback)
   {
      sendRequest(scope, method, new JSONArray(), null, requestCallback);
   }

   protected <T> void sendRequest(String scope,
                                String method,
                                boolean param,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(param));
      sendRequest(scope, method, params, null, requestCallback);
   }

   protected <T> void sendRequest(String scope,
                                String method,
                                long param,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(param));
      sendRequest(scope, method, params, null, requestCallback);
   }

   protected <T> void sendRequest(String scope,
                                String method,
                                String param,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();

      // pass JSONNull if the string is null
      params.set(0, param != null ?
                     new JSONString(param) :
                     JSONNull.getInstance());

      sendRequest(scope, method, params, null, requestCallback);
   }

   protected <T> void sendRequest(String scope,
                                String method,
                                String param1,
                                String param2,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();

      // pass JSONNull if the string is null
      params.set(0, param1 != null ? new JSONString(param1) :
                                    JSONNull.getInstance());
      params.set(1, param2 != null ? new JSONString(param2) :
                                    JSONNull.getInstance());


      sendRequest(scope, method, params, null, requestCallback);
   }


   protected <T> void sendRequest(String scope,
                                String method,
                                JavaScriptObject param,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();

      // pass JSONNull if the object is null
      params.set(0, param != null ? new JSONObject(param) :
                                    JSONNull.getInstance());

      sendRequest(scope, method, params, null, requestCallback);
   }

   protected <T> void sendRequestNoCredRefresh(String scope,
                                               String method,
                                               JavaScriptObject param,
                                               ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();

      // pass JSONNull if the object is null
      params.set(0, param != null ? new JSONObject(param) :
            JSONNull.getInstance());

      sendRequest(scope, method, params, null, false, false, null, requestCallback);
   }

   protected <T> void sendRequest(final String scope,
                                final String method,
                                final JSONArray params,
                                final ServerRequestCallback<T> requestCallback)
   {
      sendRequest(scope, method, params, null, false, requestCallback);
   }

   protected <T> void sendRequestNoCredRefresh(final String scope,
                                               final String method,
                                               final JSONArray params,
                                               final ServerRequestCallback<T> requestCallback)
   {
      sendRequest(scope, method, params, null, false, false, null, requestCallback);
   }

   protected <T> void sendRequest(final String scope,
                                final String method,
                                final JSONArray params,
                                final JSONObject kwparams,
                                final ServerRequestCallback<T> requestCallback)
   {
      sendRequest(scope, method, params, kwparams, false, requestCallback);
   }

   protected <T> void sendRequest(final String scope,
                                final String method,
                                final JSONArray params,
                                final JSONObject kwparams,
                                final String resultFieldName,
                                final ServerRequestCallback<T> requestCallback)
   {
      sendRequest(scope, method, params, kwparams, false, true, resultFieldName, requestCallback);
   }

   protected <T> void sendRequest(final String scope,
                                final String method,
                                final JSONArray params,
                                final boolean redactLog,
                                final ServerRequestCallback<T> cb)
   {
      sendRequest(scope, method, params, null, redactLog, cb);

   }

   protected <T> void sendRequest(final String scope,
                                final String method,
                                final JSONArray params,
                                final JSONObject kwparams,
                                final boolean redactLog,
                                final ServerRequestCallback<T> cb)
   {
      sendRequest(scope, method, params, kwparams, redactLog, true, null, cb);
   }



   protected <T> void sendRequest(final String scope,
                                final String method,
                                final JSONArray params,
                                final JSONObject kwparams,
                                final boolean redactLog,
                                final boolean refreshCreds,
                                final String resultFieldName,
                                final ServerRequestCallback<T> cb)
   {
      // if this is a satellite window then we handle this by proxying
      // back through the main workbench window
      if (Satellite.isCurrentWindowSatellite())
      {
         sendRequestViaMainWorkbench(scope, method, params, kwparams, redactLog, refreshCreds, resultFieldName, cb);

      }
      // otherwise just a standard request with single retry
      else
      {
         sendRequestWithRetry(scope, method, params, kwparams, redactLog, refreshCreds, resultFieldName, cb);
      }

   }

   private <T> void sendRequestWithRetry(
                                 final String scope,
                                 final String method,
                                 final JSONArray params,
                                 final JSONObject kwparams,
                                 final boolean redactLog,
                                 final boolean refreshCreds,
                                 final String resultFieldName,
                                 final ServerRequestCallback<T> requestCallback)
   {
      // retry handler (make the same call with the same params. ensure that
      // only one retry occurs by passing null as the retryHandler)
      RetryHandler retryHandler = new RetryHandler() {

         public void onRetry()
         {
            // retry one time (passing null as last param ensures there
            // is no retry handler installed)
            sendRequest(scope,
                        method,
                        params,
                        kwparams,
                        redactLog,
                        refreshCreds,
                        resultFieldName,
                        requestCallback,
                        null);
         }

         public void onModifiedRetry(RpcRequest modifiedRequest)
         {
            // retry this modified request once
            sendRequest(scope,
                        modifiedRequest.getMethod(),
                        modifiedRequest.getParams(),
                        modifiedRequest.getKwparams(),
                        modifiedRequest.getRedactLog(),
                        modifiedRequest.getRefreshCreds(),
                        modifiedRequest.getResultFieldName(),
                        requestCallback,
                        null);
         }

         public void onError(RpcError error)
         {
            // propagate error which caused the retry to the caller
            requestCallback.onError(new RemoteServerError(error));
         }
      };

      // submit request (retry same request up to one time)
      sendRequest(scope,
                  method,
                  params,
                  kwparams,
                  redactLog,
                  refreshCreds,
                  resultFieldName,
                  requestCallback,
                  retryHandler);
   }

   // sendRequest method called for internal calls from main workbench
   // (as opposed to proxied calls from satellites)
   protected <T> RpcRequest sendRequest(
                              String scope,
                              String method,
                              JSONArray params,
                              JSONObject kwparams,
                              boolean redactLog,
                              boolean refreshCreds,
                              String resultFieldName,
                              final ServerRequestCallback<T> requestCallback,
                              RetryHandler retryHandler)
   {
      final RpcRequest request = sendRequest(
            null,
            scope,
            method,
            params,
            kwparams,
            redactLog,
            refreshCreds,
            resultFieldName,
            new RpcResponseHandler()
            {
               @Override
               public void onResponseReceived(RpcResponse response)
               {
                  // ignore response if no request callback or
                  // if it was cancelled
                  if (requestCallback == null ||
                      requestCallback.cancelled())
                     return;

                  if (response.getError() != null)
                  {
                     requestCallback.onError(
                      new RemoteServerError(response.getError()));
                  }
                  else
                  {
                     clearSessionRelaunchPending();

                     T result;
                     if (resultFieldName == null)
                        result = response.getResult();
                     else
                        result = response.getField(resultFieldName);
                     requestCallback.onResponseReceived(result);
                  }
               }
             },
             retryHandler);

      if (requestCallback != null)
      {
         requestCallback.onRequestInitiated(request);
      }
      return request;
   }

   // lowest level sendRequest method -- called from the main workbench
   // in two scenarios: direct internal call and servicing a proxied
   // request from a satellite window
   protected RpcRequest sendRequest(String sourceWindow,
                                  String scope,
                                  String method,
                                  JSONArray params,
                                  JSONObject kwparams,
                                  boolean redactLog,
                                  boolean refreshCreds,
                                  String resultFieldName,
                                  final RpcResponseHandler responseHandler,
                                  final RetryHandler retryHandler)
   {
      // ensure we are listening for events. note that we do this here
      // because we are no longer so aggressive about retrying on failed
      // get_events calls. therefore, if we retry and fail a few times
      // we may need to restart event listening.
      ensureListeningForEvents();

      // create request
      String rserverURL = getApplicationURL(scope) + "/" + method;
      RpcRequest rpcRequest = new RpcRequest(rserverURL,
                                             method,
                                             params,
                                             kwparams,
                                             redactLog,
                                             resultFieldName,
                                             sourceWindow,
                                             clientId_,
                                             clientVersion_,
                                             refreshCreds);

      if (isDisconnected(scope))
         return rpcRequest;

      // send the request
      rpcRequest.send(new RpcRequestCallback() {
         public void onError(RpcRequest request, RpcError error)
         {
            // ignore errors if we are disconnected
            if (isDisconnected(scope))
               return;

            // if we have a retry handler then see if we can resolve the
            // error and then retry
            if ( resolveRpcErrorAndRetry(rpcRequest, error, retryHandler) )
               return;

            // first crack goes to globally registered rpc error handlers
            if (!handleRpcErrorInternally(error))
            {
               eventBus_.fireEvent(new ApplicationTutorialEvent(
                     ApplicationTutorialEvent.API_ERROR,
                     error.getEndUserMessage(),
                     new TutorialApiCallContext("rpc", null)));

               // no global handlers processed it, send on to caller
               responseHandler.onResponseReceived(RpcResponse.create(error));
            }
         }

         public void onResponseReceived(final RpcRequest request,
                                        RpcResponse response)
         {
            // ignore response if we are disconnected
            //   - handler was cancelled
            if (isDisconnected(scope))
                 return;

            // check for error
            if (response.getError() != null)
            {
               // ERROR: explicit error returned by server
               RpcError error = response.getError();

               // if we have a retry handler then see if we can resolve the
               // error and then retry
               if ( resolveRpcErrorAndRetry(request, error, retryHandler) )
                  return;

               // give first crack to internal handlers, then forward to caller
               if (!handleRpcErrorInternally(error))
                  responseHandler.onResponseReceived(response);
            }
            else if (response.getAsyncHandle() != null)
            {
               serverEventListener_.registerAsyncHandle(
                     response.getAsyncHandle(),
                     request,
                     this);
            }
            // no error, process the result
            else
            {
               clearSessionRelaunchPending();

               // no error, forward to caller
               responseHandler.onResponseReceived(response);

               // always ensure that the event source receives events unless
               // the server specifically flags us that no events are likely
               // to be pending (e.g. an rpc call where no events were added
               // to the queue by the call)
               if (eventsPending(response))
                  serverEventListener_.ensureEvents();
            }
         }
      });

      // return the request
      return rpcRequest;
   }

   private void ensureListeningForEvents()
   {
      // don't do this if we are disconnected
      if (isDisconnected())
         return;

      // if we are in a mode where we are listening for events (running
      // as the main workbench) then ensure we are listening

      // we need the listeningForEvents_ flag because we don't want to cause
      // events to flow prior to the workbench being instantiated and fully
      // initialized. since this method can be called at any time we need to
      // protect ourselves against this "pre-workbench initialization" state

      // the retries are there to work around the fact that when we execute a
      // network request which causes us to resume from a suspended session
      // the first query for events often returns ServiceUnavailable because
      // the process isn't alive yet. by retrying we make certain that if
      // the first attempts to listen fail we eventually get synced up

      if (listeningForEvents_)
         serverEventListener_.ensureListening(10);
   }

   private boolean eventsPending(RpcResponse response)
   {
      String eventsPending = response.getField("ep");
      if (eventsPending == null)
         return true; // default to true for json-rpc compactness
      else
         return Boolean.parseBoolean(eventsPending);
   }

   private boolean resolveRpcErrorAndRetry(final RpcRequest request,
                                           final RpcError error,
                                           final RetryHandler retryHandler)
   {
      // won't even attempt resolve if we don't have a retryHandler
      if (retryHandler == null)
         return false;

      // can attempt to resolve UNAUTHORIZED by updating credentials
      if (error.getCode() == RpcError.UNAUTHORIZED)
      {
         // check credentials
         serverAuth_.updateCredentials(new ServerRequestCallback<Integer>() {

            @Override
            public void onResponseReceived(Integer response)
            {
               // allow retry on success, otherwise handle unauthorized error
               if (response == RemoteServerAuth.CREDENTIALS_UPDATE_SUCCESS)
               {
                  retryHandler.onRetry();
               }
               else
               {
                  handleUnauthorizedError();
               }
            }

            @Override
            public void onError(ServerError serverError)
            {
               // log the auth sequence error
               Debug.logError(serverError);

               // unable to resolve unauthorized error through a
               // credentials check -- treat as unauthorized
               handleUnauthorizedError();
            }
         });

         // attempting to resolve
         return true;
      }
      // launch params missing means we are in a launcher session
      else if (error.getCode() == RpcError.LAUNCH_PARAMETERS_MISSING)
      {
         setSessionRelaunchPending(error.getRedirectUrl());
         return true;
      }
      else
      {
         return false;
      }
   }

   private void setSessionRelaunchPending(String redirectUrl)
   {
      if (!sessionRelaunchPending_)
      {
         sessionRelaunchPending_ = true;

         // fire event to inform UI that we are attempting to relaunch the session
         eventBus_.dispatchEvent(new SessionRelaunchEvent(SessionRelaunchEvent.Type.RELAUNCH_INITIATED, redirectUrl));
      }
   }

   private void clearSessionRelaunchPending()
   {
      if (sessionRelaunchPending_)
      {
         sessionRelaunchPending_ = false;

         // fire event to inform UI that we are done relaunching the session
         eventBus_.dispatchEvent(new SessionRelaunchEvent(SessionRelaunchEvent.Type.RELAUNCH_COMPLETE));
      }
   }

   private boolean handleRpcErrorInternally(RpcError error)
   {
      if (error.getCode() == RpcError.UNAUTHORIZED)
      {
         handleUnauthorizedError();
         return true;
      }
      else if (error.getCode() == RpcError.INVALID_CLIENT_ID)
      {
         // disconnect
         disconnect();

         // fire event
         ClientDisconnectedEvent event = new ClientDisconnectedEvent();
         eventBus_.fireEvent(event);

         // handled
         return true;
      }
      else if (error.getCode() == RpcError.INVALID_CLIENT_VERSION)
      {
         // disconnect
         disconnect();

         // fire event
         InvalidClientVersionEvent event = new InvalidClientVersionEvent();
         eventBus_.fireEvent(event);

         // handled
         return true;
      }
      else if (error.getCode() == RpcError.SERVER_OFFLINE)
      {
         // disconnect
         disconnect();

         // fire event
         ServerOfflineEvent event = new ServerOfflineEvent();
         eventBus_.fireEvent(event);

         // handled
         return true;

      }
      else if (error.getCode() == RpcError.INVALID_SESSION)
      {
         // disconnect
         disconnect();

         // fire event
         InvalidSessionInfo info = error.getClientInfo().isObject()
                                             .getJavaScriptObject().cast();
         InvalidSessionEvent event = new InvalidSessionEvent(info);
         eventBus_.fireEvent(event);

         // handled
         return true;
      }
      else
      {
         return false;
      }
   }


   // the following sequence of calls enables marshalling of remote server
   // requests from satellite windows back into the main workbench window

   // this code sets up the sendRemoteServerRequest global callback within
   // the main workbench
   private native void registerSatelliteCallback() /*-{
      var server = this;
      $wnd.sendRemoteServerRequest = $entry(
         function(sourceWindow, scope, method, params, redactLog, refreshCreds, resultFieldName, responseCallback) {
            server.@org.rstudio.studio.client.server.remote.RemoteServer::sendRemoteServerRequest(*)(sourceWindow, scope, method, params, redactLog, refreshCreds, resultFieldName, responseCallback);
         }
      );
   }-*/;

   // this code runs in the main workbench and implements the server request
   // and then calls back the satellite on the provided js responseCallback
   private RpcRequest sendRemoteServerRequest(final JavaScriptObject sourceWindow,
                                        final String scope,
                                        final String method,
                                        final JavaScriptObject params,
                                        final boolean redactLog,
                                        final boolean refreshCreds,
                                        final String resultFieldName,
                                        final JavaScriptObject responseCallback)
   {
      // get the WindowEx from the sourceWindow
      final WindowEx srcWnd = sourceWindow.<WindowEx>cast();

      // unwrap the parameter array
      JsArrayEx array = params.cast();
      final JSONArray jsonParams = array.toJSONArray();

      // setup an rpc response handler that proxies back to the js object
      class ResponseHandler extends RpcResponseHandler
      {
         @Override
         public void onResponseReceived(RpcResponse response)
         {
            if (!srcWnd.isClosed())
               performCallback(responseCallback, response);
         }

         public void onError(RpcError error)
         {
            RpcResponse errorResponse = RpcResponse.create(error);
            if (!srcWnd.isClosed())
               performCallback(responseCallback, errorResponse);
         }

         private native void performCallback(JavaScriptObject responseCallback,
                                             RpcResponse response) /*-{
            responseCallback.onResponse(response);
         }-*/;
      }
      final ResponseHandler responseHandler = new ResponseHandler();

      // setup a retry handler which will call back the second time with
      // the same args (but no retryHandler, ensuring at most 1 retry)
      RetryHandler retryHandler = new RetryHandler() {

         public void onRetry()
         {
            // retry one time (passing null as last param ensures there
            // is no retry handler installed)
            sendRequest(getSourceWindowName(sourceWindow),
                        scope,
                        method,
                        jsonParams,
                        null,
                        redactLog,
                        refreshCreds,
                        resultFieldName,
                        responseHandler,
                        null);
         }

         public void onModifiedRetry(RpcRequest modifiedRequest)
         {
            // retry this modified request once
            sendRequest(getSourceWindowName(sourceWindow),
                        scope,
                        modifiedRequest.getMethod(),
                        modifiedRequest.getParams(),
                        modifiedRequest.getKwparams(),
                        modifiedRequest.getRedactLog(),
                        modifiedRequest.getRefreshCreds(),
                        modifiedRequest.getResultFieldName(),
                        responseHandler,
                        null);
         }

         public void onError(RpcError error)
         {
            // propagate error which caused the retry to the caller
            responseHandler.onError(error);
         }
      };

      // submit request (retry same request up to one time)
      return sendRequest(getSourceWindowName(sourceWindow),
                  scope,
                  method,
                  jsonParams,
                  null,
                  redactLog,
                  refreshCreds,
                  resultFieldName,
                  responseHandler,
                  retryHandler);
   }

   private native String getSourceWindowName(JavaScriptObject sourceWindow) /*-{
      return sourceWindow.RStudioSatelliteName;
   }-*/;

   // call made from satellite -- this delegates to a native method which
   // sets up a javascript callback and then calls the main workbench
   private <T> void sendRequestViaMainWorkbench(
                               String scope,
                               String method,
                               JSONArray params,
                               JSONObject kwparams,
                               boolean redactLog,
                               boolean refreshCreds,
                               String resultFieldName,
                               final ServerRequestCallback<T> requestCallback)
   {
      assert kwparams == null : "kwparams was not null for sendRequestViaMainWorkbench - not currently supported";
      assert resultFieldName == null : "resultFieldName was not null for sendRequestViaMainWorkbench - not currently supported";

      JSONObject request = new JSONObject();
      request.put("method", new JSONString(method));
      if (params != null)
         request.put("params", params);

      final RequestLogEntry requestLogEntry = RequestLog.log(
         Integer.toString(Random.nextInt()),
         redactLog ? "[REDACTED]": request.toString());

      sendRequestViaMainWorkbench(
            scope,
            method,
            params.getJavaScriptObject(),
            kwparams == null ? JavaScriptObject.createObject() : kwparams.getJavaScriptObject(),
            redactLog,
            refreshCreds,
            resultFieldName,
            new RpcResponseHandler() {
               @Override
               public void onResponseReceived(RpcResponse response)
               {
                  String responseText = response.toString();
                  requestLogEntry.logResponse(ResponseType.Normal,
                                              responseText);

                  if (response.getError() != null)
                  {
                     RpcError error = response.getError();
                     requestCallback.onError(new RemoteServerError(error));
                  }
                  else
                  {
                     clearSessionRelaunchPending();

                     T result = response.<T> getResult();
                     requestCallback.onResponseReceived(result);
                  }

               }
      });
   }

   // call from satellite to sendRemoteServerRequest method made available
   // by main workbench
   private native void sendRequestViaMainWorkbench(
                                    String scope,
                                    String method,
                                    JavaScriptObject params,
                                    JavaScriptObject kwparams,
                                    boolean redactLog,
                                    boolean refreshCreds,
                                    String resultFieldName,
                                    RpcResponseHandler handler) /*-{

      var responseCallback = new Object();
      responseCallback.onResponse = $entry(function(response) {
        handler.@org.rstudio.core.client.jsonrpc.RpcResponseHandler::onResponseReceived(*)(response);
      });

      $wnd.opener.sendRemoteServerRequest($wnd,
                                          scope,
                                          method,
                                          params,
                                          redactLog,
                                          refreshCreds,
                                          resultFieldName,
                                          responseCallback);
   }-*/;

   @Override
   public void svnAdd(ArrayList<String> paths,
                      ServerRequestCallback<ProcessResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONUtils.toJSONStringArray(paths));
      sendRequest(RPC_SCOPE, SVN_ADD, params, requestCallback);
   }

   @Override
   public void svnDelete(ArrayList<String> paths,
                         ServerRequestCallback<ProcessResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONUtils.toJSONStringArray(paths));
      sendRequest(RPC_SCOPE, SVN_DELETE, params, requestCallback);
   }

   @Override
   public void svnRevert(ArrayList<String> paths,
                         ServerRequestCallback<ProcessResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONUtils.toJSONStringArray(paths));
      sendRequest(RPC_SCOPE, SVN_REVERT, params, requestCallback);
   }

   @Override
   public void svnResolve(String accept,
                          ArrayList<String> paths,
                          ServerRequestCallback<ProcessResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(accept));
      params.set(1, JSONUtils.toJSONStringArray(paths));
      sendRequest(RPC_SCOPE, SVN_RESOLVE, params, requestCallback);
   }

   @Override
   public void svnStatus(ServerRequestCallback<JsArray<StatusAndPathInfo>> requestCallback)
   {
      sendRequest(RPC_SCOPE, SVN_STATUS, requestCallback);
   }

   @Override
   public void svnUpdate(ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE, SVN_UPDATE,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   @Override
   public void svnCleanup( ServerRequestCallback<ProcessResult> requestCallback)
   {
      sendRequest(RPC_SCOPE, SVN_CLEANUP, requestCallback);
   }


   @Override
   public void svnCommit(ArrayList<String> paths,
                         String message,
                         ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONUtils.toJSONStringArray(paths));
      params.set(1, new JSONString(message));

      sendRequest(RPC_SCOPE, SVN_COMMIT, params,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   @Override
   public void svnDiffFile(String path,
                           Integer contextLines,
                           boolean noSizeWarning,
                           ServerRequestCallback<DiffResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONNumber(contextLines));
      params.set(2, JSONBoolean.getInstance(noSizeWarning));
      sendRequest(RPC_SCOPE, SVN_DIFF_FILE, params, requestCallback);
   }

   @Override
   public void svnApplyPatch(String path,
                             String patch,
                             String sourceEncoding,
                             ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONString(patch));
      params.set(2, new JSONString(sourceEncoding));
      sendRequest(RPC_SCOPE, SVN_APPLY_PATCH, params, requestCallback);
   }

   @Override
   public void svnHistoryCount(int revision,
                               FileSystemItem path,
                               String searchText,
                               ServerRequestCallback<CommitCount> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(revision));
      params.set(1, path == null ? JSONNull.getInstance()
                                 : new JSONString(path.getPath()));
      params.set(2, new JSONString(StringUtil.notNull(searchText)));

      sendRequest(RPC_SCOPE, SVN_HISTORY_COUNT, params, requestCallback);
   }

   @Override
   public void svnHistory(int revision,
                          FileSystemItem path,
                          int skip,
                          int maxentries,
                          String searchText,
                          ServerRequestCallback<RpcObjectList<CommitInfo>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(revision));
      params.set(1, path == null ? JSONNull.getInstance()
                                 : new JSONString(path.getPath()));
      params.set(2, new JSONNumber(skip));
      params.set(3, new JSONNumber(maxentries));
      params.set(4, new JSONString(StringUtil.notNull(searchText)));

      sendRequest(RPC_SCOPE, SVN_HISTORY, params, requestCallback);
   }

   @Override
   public void svnShow(int rev,
                       boolean noSizeWarning,
                       ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(rev));
      params.set(1, JSONBoolean.getInstance(noSizeWarning));

      sendRequest(RPC_SCOPE, SVN_SHOW, params, requestCallback);
   }

   @Override
   public void svnShowFile(int rev,
                           String filename,
                           ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(rev));
      params.set(1, new JSONString(filename));
      sendRequest(RPC_SCOPE, SVN_SHOW_FILE, params, requestCallback);
   }

   public void svnGetIgnores(
         String path,
         ServerRequestCallback<ProcessResult> requestCallback)
   {
      sendRequest(RPC_SCOPE, SVN_GET_IGNORES, path, requestCallback);
   }

   public void svnSetIgnores(String path,
                             String ignores,
                             ServerRequestCallback<ProcessResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONString(ignores));
      sendRequest(RPC_SCOPE, SVN_SET_IGNORES, params, requestCallback);
   }

   @Override
   public void viewerStopped(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "viewer_stopped", requestCallback);
   }

   @Override
   public void viewerBack(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "viewer_back", requestCallback);
   }

   @Override
   public void viewerForward(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "viewer_forward", requestCallback);
   }

   @Override
   public void viewerCurrent(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "viewer_current", requestCallback);
   }

   @Override
   public void viewerClearCurrent(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "viewer_clear_current", requestCallback);
   }

   @Override
   public void viewerClearAll(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "viewer_clear_all", requestCallback);
   }

   @Override
   public void getViewerExportContext(
            String directory,
            ServerRequestCallback<SavePlotAsImageContext> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  "get_viewer_export_context",
                  directory,
                  requestCallback);
   }

   @Override
   public void viewerSaveAsWebPage(String targetPath,
                                   ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            "viewer_save_as_web_page",
            targetPath,
            requestCallback);
   }

   @Override
   public void viewerCreateRPubsHtml(
            String title,
            String comment,
            ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(title));
      params.set(1, new JSONString(comment));
      sendRequest(RPC_SCOPE, "viewer_create_rpubs_html", params, callback);
   }

   @Override
   public void plotsCreateRPubsHtml(
            String title,
            String comment,
            int width,
            int height,
            ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(title));
      params.set(1, new JSONString(comment));
      params.set(2,  new JSONNumber(width));
      params.set(3,  new JSONNumber(height));
      sendRequest(RPC_SCOPE, "plots_create_rpubs_html", params, callback);
   }



   public void previewHTML(HTMLPreviewParams params,
                           ServerRequestCallback<Boolean> callback)
   {
      sendRequest(RPC_SCOPE, PREVIEW_HTML, params, callback);
   }

   public void terminatePreviewHTML(ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, TERMINATE_PREVIEW_HTML, callback);
   }

   public void getHTMLCapabilities(
                        ServerRequestCallback<HTMLCapabilities> callback)
   {
      sendRequest(RPC_SCOPE, GET_HTML_CAPABILITIES, callback);
   }

   public void rpubsIsPublished(String htmlFile,
                                ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, "rpubs_is_published", htmlFile, requestCallback);
   }

   public void rpubsUpload(String contextId,
                           String title,
                           String rmdFile,
                           String htmlFile,
                           String uploadId,
                           boolean isUpdate,
                           ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(contextId));
      params.set(1, new JSONString(title));
      params.set(2, new JSONString(rmdFile));
      params.set(3, new JSONString(htmlFile));
      params.set(4, new JSONString(uploadId));
      params.set(5, JSONBoolean.getInstance(isUpdate));
      sendRequest(RPC_SCOPE, RPUBS_UPLOAD, params, requestCallback);
   }

   public void rpubsTerminateUpload(String contextId,
                                    ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  RPUBS_TERMINATE_UPLOAD,
                  contextId,
                  requestCallback);
   }

   @Override
   public void setPresentationSlideIndex(
                                 int index,
                                 ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, SET_PRESENTATION_SLIDE_INDEX, index, requestCallback);
   }

   @Override
   public void setWorkingDirectory(String path,
                                   ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  SET_WORKING_DIRECTORY,
                  path,
                  requestCallback);
   }

   @Override
   public void createStandalonePresentation(
                              String targetFile,
                              ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  CREATE_STANDALONE_PRESENTATION,
                  StringUtil.notNull(targetFile),
                  requestCallback);
   }

   @Override
   public void createDesktopViewInBrowserPresentation(
                              ServerRequestCallback<String> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  CREATE_DESKTOP_VIEW_IN_BROWSER_PRESENTATION,
                  requestCallback);
   }


   @Override
   public void createPresentationRPubsSource(
             ServerRequestCallback<PresentationRPubsSource> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  CREATE_PRESENTATION_RPUBS_SOURCE,
                  requestCallback);
   }

   @Override
   public void presentationExecuteCode(
                                 String code,
                                 ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, PRESENTATION_EXECUTE_CODE, code, requestCallback);
   }

   @Override
   public void createNewPresentation(
                        String filePath,
                        ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CREATE_NEW_PRESENTATION, filePath, requestCallback);
   }

   @Override
   public void showPresentationPane(String filePath,
                                    ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, SHOW_PRESENTATION_PANE, filePath, requestCallback);
   }

   @Override
   public void closePresentationPane(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CLOSE_PRESENTATION_PANE, requestCallback);
   }

   @Override
   public void tutorialQuizResponse(
                           int slideIndex, int answer, boolean correct,
                           ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(slideIndex));
      params.set(1, new JSONNumber(answer));
      params.set(2, JSONBoolean.getInstance(correct));
      sendRequest(RPC_SCOPE, TUTORIAL_QUIZ_RESPONSE, params, requestCallback);
   }

   @Override
   public void tutorialStarted(String tutorialName,
                               String tutorialPackage,
                               String tutorialUrl,
                               ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(tutorialName)
            .add(tutorialPackage)
            .add(tutorialUrl)
            .get();

      sendRequest(RPC_SCOPE, TUTORIAL_STARTED, params, requestCallback);
   }

   @Override
   public void tutorialStop(String tutorialUrl,
                            ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(tutorialUrl)
            .get();

      sendRequest(RPC_SCOPE, TUTORIAL_STOP, params, requestCallback);
   }

   @Override
   public void tutorialMetadata(String tutorialUrl,
                                ServerRequestCallback<JsObject> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(tutorialUrl)
            .get();

      sendRequest(RPC_SCOPE, TUTORIAL_METADATA, params, requestCallback);
   }


   @Override
   public void getSlideNavigationForFile(
                     String filePath,
                     ServerRequestCallback<SlideNavigation> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_SLIDE_NAVIGATION_FOR_FILE,
                  filePath,
                  requestCallback);
   }

   @Override
   public void getSlideNavigationForCode(
                     String code,
                     String baseDir,
                     ServerRequestCallback<SlideNavigation> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(code));
      params.set(1, new JSONString(baseDir));
      sendRequest(RPC_SCOPE,
                  GET_SLIDE_NAVIGATION_FOR_CODE,
                  params,
                  requestCallback);
   }

   @Override
   public void clearPresentationCache(
                                  ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CLEAR_PRESENTATION_CACHE, requestCallback);
   }


   public void compilePdf(FileSystemItem targetFile,
                          String encoding,
                          SourceLocation sourceLocation,
                          String completedAction,
                          ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(targetFile.getPath()));
      params.set(1, new JSONString(encoding));
      params.set(2, new JSONObject(sourceLocation));
      params.set(3, new JSONString(completedAction));
      sendRequest(RPC_SCOPE, COMPILE_PDF, params, requestCallback);
   }

   public void isCompilePdfRunning(ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, IS_COMPILE_PDF_RUNNING, requestCallback);
   }

   public void terminateCompilePdf(
                           ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, TERMINATE_COMPILE_PDF, requestCallback);
   }

   public void compilePdfClosed(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, COMPILE_PDF_CLOSED, requestCallback);
   }

   @Override
   public void synctexForwardSearch(String rootDocument,
                                    SourceLocation sourceLocation,
                                    ServerRequestCallback<PdfLocation> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(rootDocument));
      params.set(1, new JSONObject(sourceLocation));
      sendRequest(RPC_SCOPE, SYNCTEX_FORWARD_SEARCH, params, callback);
   }

   @Override
   public void applyForwardConcordance(
                                String rootDocument,
                                SourceLocation sourceLocation,
                                ServerRequestCallback<SourceLocation> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(rootDocument));
      params.set(1, new JSONObject(sourceLocation));
      sendRequest(RPC_SCOPE, APPLY_FORWARD_CONCORDANCE, params, callback);
   }

   @Override
   public void synctexInverseSearch(PdfLocation pdfLocation,
                                    ServerRequestCallback<SourceLocation> callback)
   {
      sendRequest(RPC_SCOPE, SYNCTEX_INVERSE_SEARCH, pdfLocation, callback);
   }

   @Override
   public void applyInverseConcordance(
                               SourceLocation sourceLocation,
                               ServerRequestCallback<SourceLocation> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(sourceLocation));
      sendRequest(RPC_SCOPE, APPLY_INVERSE_CONCORDANCE, params, callback);
   }


   public void checkSpelling(
                         JsArrayString words,
                         ServerRequestCallback<JsArrayInteger> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONArray(words));
      sendRequest(RPC_SCOPE, CHECK_SPELLING, params, requestCallback);
   }

   public void suggestionList(
                     String word,
                     ServerRequestCallback<JsArrayString> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(word));
      sendRequest(RPC_SCOPE, SUGGESTION_LIST, params, requestCallback);
   }

   @Override
   public void getWordChars(ServerRequestCallback<String> requestCallback)
   {
      sendRequest(RPC_SCOPE, "get_word_chars", requestCallback);
   }

   public void addCustomDictionary(
                              String dictPath,
                              ServerRequestCallback<JsArrayString> callback)
   {
      sendRequest(RPC_SCOPE, ADD_CUSTOM_DICTIONARY, dictPath, callback);
   }

   public void removeCustomDictionary(
                              String name,
                              ServerRequestCallback<JsArrayString> callback)
   {
      sendRequest(RPC_SCOPE, REMOVE_CUSTOM_DICTIONARY, name, callback);
   }


   public void installAllDictionaries(
               ServerRequestCallback<SpellingPrefsContext> requestCallback)
   {
      sendRequest(RPC_SCOPE, INSTALL_ALL_DICTIONARIES, requestCallback);
   }

   @Override
   public void beginFind(String searchString,
                         boolean regex,
                         boolean ignoreCase,
                         FileSystemItem directory,
                         JsArrayString includeFilePatterns,
                         JsArrayString excludeFilePatterns,
                         ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(searchString));
      params.set(1, JSONBoolean.getInstance(regex));
      params.set(2, JSONBoolean.getInstance(ignoreCase));
      params.set(3, new JSONString(directory == null ? ""
                                                     : directory.getPath()));
      params.set(4, new JSONArray(includeFilePatterns));
      params.set(5, new JSONArray(excludeFilePatterns));
      sendRequest(RPC_SCOPE, BEGIN_FIND, params, requestCallback);
   }

   @Override
   public void stopFind(String findOperationHandle,
                        ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, STOP_FIND, findOperationHandle, requestCallback);
   }

   @Override
   public void clearFindResults(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "clear_find_results", requestCallback);
   }

   @Override
   public void previewReplace(String searchString,
                              boolean regex,
                              boolean searchIgnoreCase,
                              FileSystemItem directory,
                              JsArrayString includeFilePatterns,
                              JsArrayString excludeFilePatterns,
                              String replaceString,
                              ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(searchString));
      params.set(1, JSONBoolean.getInstance(regex));
      params.set(2, JSONBoolean.getInstance(searchIgnoreCase));
      params.set(3, new JSONString(directory == null ? ""
                                                     : directory.getPath()));
      params.set(4, new JSONArray(includeFilePatterns));
      params.set(5, new JSONArray(excludeFilePatterns));
      params.set(6, new JSONString(replaceString));

      sendRequest(RPC_SCOPE, PREVIEW_REPLACE, params, requestCallback);
   }

   @Override
   public void completeReplace(String searchString,
                               boolean regex,
                               boolean searchIgnoreCase,
                               FileSystemItem directory,
                               JsArrayString includeFilePatterns,
                               JsArrayString excludeFilePatterns,
                               int searchResults,
                               String replaceString,
                               ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(searchString));
      params.set(1, JSONBoolean.getInstance(regex));
      params.set(2, JSONBoolean.getInstance(searchIgnoreCase));
      params.set(3, new JSONString(directory == null ? ""
                                                     : directory.getPath()));
      params.set(4, new JSONArray(includeFilePatterns));
      params.set(5, new JSONArray(excludeFilePatterns));
      params.set(6, new JSONNumber(searchResults));
      params.set(7, new JSONString(replaceString));

      sendRequest(RPC_SCOPE, COMPLETE_REPLACE, params, requestCallback);
   }

   @Override
   public void stopReplace(String findOperationHandle,
                           ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, STOP_REPLACE, findOperationHandle, requestCallback);
   }

   @Override
   public void getCppCapabilities(
                     ServerRequestCallback<CppCapabilities> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_CPP_CAPABILITIES, requestCallback);
   }

   @Override
   public void installBuildTools(String action,
                                 ServerRequestCallback<Boolean> callback)
   {
      sendRequest(RPC_SCOPE, INSTALL_BUILD_TOOLS, action, callback);
   }


   @Override
   public void startBuild(String type,
                          String subType,
                          ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(type));
      params.set(1, new JSONString(subType));
      sendRequest(RPC_SCOPE, START_BUILD, params, requestCallback);
   }

   @Override
   public void terminateBuild(ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, TERMINATE_BUILD, requestCallback);
   }

   @Override
   public void devtoolsLoadAllPath(
                              ServerRequestCallback<String> requestCallback)
   {
      sendRequest(RPC_SCOPE, DEVTOOLS_LOAD_ALL_PATH, requestCallback);
   }

   @Override
   public void getBookdownFormats(
                  ServerRequestCallback<BookdownFormats> requestCallback)
   {
      sendRequest(RPC_SCOPE, "get_bookdown_formats", requestCallback);
   }

   @Override
   public void listEnvironment(ServerRequestCallback<JsArray<RObject>> callback)
   {
      sendRequest(RPC_SCOPE, LIST_ENVIRONMENT, callback);
   }

   @Override
   public void setContextDepth(int newContextDepth,
                               ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  SET_CONTEXT_DEPTH,
                  newContextDepth,
                  requestCallback);
   }

   @Override
   public void setEnvironment(String environmentName,
                              ServerRequestCallback<Void> requestCallback)
   {

      JSONArray params = new JSONArray();
      params.set(0, new JSONString(environmentName));
      sendRequest(RPC_SCOPE,
                  SET_ENVIRONMENT,
                  params,
                  requestCallback);
   }

   @Override
   public void setEnvironmentFrame(int frame,
                                   ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(frame));
      sendRequest(RPC_SCOPE,
                  SET_ENVIRONMENT_FRAME,
                  params,
                  requestCallback);
   }

   @Override
   public void getEnvironmentNames(
         String language,
         ServerRequestCallback<JsArray<EnvironmentFrame>> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(language)
            .get();

      sendRequest(RPC_SCOPE,
                  GET_ENVIRONMENT_NAMES,
                  params,
                  requestCallback);
   }

   @Override
   public void getEnvironmentState(
         String language,
         String environment,
         ServerRequestCallback<EnvironmentContextData> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(language)
            .add(environment)
            .get();

      sendRequest(RPC_SCOPE,
                  GET_ENVIRONMENT_STATE,
                  params,
                  requestCallback);
   }

   @Override
   public void requeryContext(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  REQUERY_CONTEXT,
                  requestCallback);
   }
   
   @Override
   public void isFunctionMasked(String functionName,
                                String packageName,
                                ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(functionName)
            .add(packageName)
            .get();
      
      sendRequest(RPC_SCOPE, IS_FUNCTION_MASKED, params, requestCallback);
   }

   @Override
   public void environmentSetLanguage(String language,
                                      ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(language)
            .get();

      sendRequest(RPC_SCOPE,
                  ENVIRONMENT_SET_LANGUAGE,
                  params,
                  requestCallback);
   }

   @Override
   public void setEnvironmentMonitoring(boolean monitoring,
                                        ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(monitoring));
      sendRequest(RPC_SCOPE,
                  SET_ENVIRONMENT_MONITORING,
                  params,
                  requestCallback);
   }

   @Override
   public void getObjectContents(
                 String objectName,
                 ServerRequestCallback<ObjectContents> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(objectName));
      sendRequest(RPC_SCOPE,
                  GET_OBJECT_CONTENTS,
                  params,
                  requestCallback);
   }

   @Override
   public void getFunctionSteps(
                 String functionName,
                 String fileName,
                 String packageName,
                 int[] lineNumbers,
                 ServerRequestCallback<JsArray<FunctionSteps>> requestCallback)
   {
      JSONArray lineNums = new JSONArray();
      for (int idx = 0; idx < lineNumbers.length; idx++)
      {
         lineNums.set(idx, new JSONNumber(lineNumbers[idx]));
      }
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(functionName));
      params.set(1, new JSONString(fileName));
      params.set(2, new JSONString(packageName));
      params.set(3, lineNums);
      sendRequest(RPC_SCOPE,
                  GET_FUNCTION_STEPS,
                  params,
                  requestCallback);
   }

   @Override
   public void setFunctionBreakpoints(
         String functionName,
         String fileName,
         String packageName,
         ArrayList<String> steps,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray breakSteps = new JSONArray();
      for (int idx = 0; idx < steps.size(); idx++)
      {
         breakSteps.set(idx, new JSONString(steps.get(idx)));
      }
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(functionName));
      params.set(1, new JSONString(fileName));
      params.set(2, new JSONString(packageName));
      params.set(3, breakSteps);
      sendRequest(RPC_SCOPE,
                  SET_FUNCTION_BREAKPOINTS,
                  params,
                  requestCallback);
   }

   @Override
   public void getFunctionState(
         String functionName,
         String fileName,
         int lineNumber,
         ServerRequestCallback<FunctionState> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(functionName));
      params.set(1, new JSONString(fileName));
      params.set(2, new JSONNumber(lineNumber));
      sendRequest(RPC_SCOPE,
                  GET_FUNCTION_STATE,
                  params,
                  requestCallback);
   }

   public void executeDebugSource(
         String fileName,
         ArrayList<Integer> topBreakLines,
         ArrayList<Integer> debugBreakLines,
         int step,
         int mode,
         ServerRequestCallback<TopLevelLineData> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(fileName));
      params.set(1, JSONUtils.toJSONNumberArray(topBreakLines));
      params.set(2, JSONUtils.toJSONNumberArray(debugBreakLines));
      params.set(3, new JSONNumber(step));
      params.set(4, new JSONNumber(mode));

      sendRequest(RPC_SCOPE,
            EXECUTE_DEBUG_SOURCE,
            params,
            requestCallback);
   }

   public void setErrorManagementType(
         String type,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(type));

      sendRequest(RPC_SCOPE,
            SET_ERROR_MANAGEMENT_TYPE,
            params,
            requestCallback);
   }

   @Override
   public void updateBreakpoints(ArrayList<Breakpoint> breakpoints,
         boolean set, boolean arm, ServerRequestCallback<Void> requestCallback)
   {
      JSONArray bps = new JSONArray();
      for (int i = 0; i < breakpoints.size(); i++)
      {
         bps.set(i, new JSONObject(breakpoints.get(i)));
      }

      JSONArray params = new JSONArray();
      params.set(0, bps);
      params.set(1, JSONBoolean.getInstance(set));
      params.set(2, JSONBoolean.getInstance(arm));
      sendRequest(RPC_SCOPE,
            UPDATE_BREAKPOINTS,
            params,
            requestCallback);
   }

   @Override
   public void removeAllBreakpoints(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            REMOVE_ALL_BREAKPOINTS,
            requestCallback);
   }

   @Override
   public void checkForUpdates(
         boolean manual,
         ServerRequestCallback<UpdateCheckResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(manual));
      sendRequest(RPC_SCOPE,
            CHECK_FOR_UPDATES,
            params,
            requestCallback);
   }

   @Override
   public void getProductInfo(ServerRequestCallback<ProductInfo> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            GET_PRODUCT_INFO,
            requestCallback);
   }

   @Override
   public void getProductNotice(ServerRequestCallback<ProductNotice> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_PRODUCT_NOTICE,
                  requestCallback);
   }

   @Override
   public void getRAddins(boolean reindex,
                          ServerRequestCallback<RAddins> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_R_ADDINS, reindex, requestCallback);
   }

   @Override
   public void prepareForAddin(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, PREPARE_FOR_ADDIN, requestCallback);
   }

   @Override
   public void executeRAddinNonInteractively(String commandId, ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(commandId));
      sendRequest(RPC_SCOPE, EXECUTE_R_ADDIN, params, requestCallback);
   }

   @Override
   public void getShinyViewerType(ServerRequestCallback<String> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            GET_SHINY_VIEWER_TYPE,
            requestCallback);
   }

   @Override
   public void setShinyViewerType(String viewerType,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(viewerType));
      sendRequest(RPC_SCOPE,
            SET_SHINY_VIEWER_TYPE,
            params,
            requestCallback);
   }

   @Override
   public void getShinyRunCmd(String shinyFile,
                              String extendedType,
                              ServerRequestCallback<ShinyRunCmd> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(shinyFile));
      params.set(1, new JSONString(extendedType));
      sendRequest(RPC_SCOPE,
            GET_SHINY_RUN_CMD,
            params,
            requestCallback);
   }

   @Override
   public void runShinyBackgroundApp(String shinyFile, String extendedType,
                                     ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(shinyFile));
      params.set(1, new JSONString(extendedType));
      sendRequest(RPC_SCOPE,
            "run_shiny_background_app",
            params,
            requestCallback);
   }

   @Override
   public void getPlumberViewerType(ServerRequestCallback<String> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            GET_PLUMBER_VIEWER_TYPE,
            requestCallback);
   }

   @Override
   public void getPlumberRunCmd(String plumberFile,
                                ServerRequestCallback<PlumberRunCmd> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(plumberFile));
      sendRequest(RPC_SCOPE,
            GET_PLUMBER_RUN_CMD,
            params,
            requestCallback);
   }

   @Override
   public void getRSConnectAccountList(
         ServerRequestCallback<JsArray<RSConnectAccount>> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            GET_RSCONNECT_ACCOUNT_LIST,
            requestCallback);
   }

   @Override
   public void removeRSConnectAccount(String accountName, String server,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(accountName));
      params.set(1, new JSONString(server));
      sendRequest(RPC_SCOPE,
            REMOVE_RSCONNECT_ACCOUNT,
            params,
            requestCallback);
   }

   @Override
   public void connectRSConnectAccount(String command,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(command));
      sendRequest(RPC_SCOPE,
            CONNECT_RSCONNECT_ACCOUNT,
            params,
            requestCallback);
   }

   @Override
   public void getRSConnectAppList(
         String accountName,
         String server,
         ServerRequestCallback<JsArray<RSConnectApplicationInfo>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(accountName));
      params.set(1, new JSONString(server));
      sendRequest(RPC_SCOPE,
            GET_RSCONNECT_APP_LIST,
            params,
            requestCallback);
   }

   @Override
   public void getRSConnectApp(String appId, String accountName, String server, String hostUrl,
         ServerRequestCallback<RSConnectApplicationResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(appId)));
      params.set(1, new JSONString(StringUtil.notNull(accountName)));
      params.set(2, new JSONString(StringUtil.notNull(server)));
      params.set(3, new JSONString(StringUtil.notNull(hostUrl)));
      sendRequest(RPC_SCOPE,
            GET_RSCONNECT_APP,
            params,
            requestCallback);
   }

   @Override
   public void getRSConnectDeployments(
         String sourcePath,
         String outputPath,
         ServerRequestCallback<JsArray<RSConnectDeploymentRecord>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(sourcePath));
      params.set(1, new JSONString(outputPath));
      sendRequest(RPC_SCOPE,
            GET_RSCONNECT_DEPLOYMENTS,
            params,
            requestCallback);
   }

   @Override
   public void forgetRSConnectDeployments(String sourceFile,
                                          String outputFile,
                                          ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(sourceFile)
            .add(outputFile)
            .get();

      sendRequest(RPC_SCOPE, FORGET_RSCONNECT_DEPLOYMENTS, params, requestCallback);
   }

   @Override
   public void publishContent(
         RSConnectPublishSource source, String account,
         String server, String appName, String appTitle, String appId,
         RSConnectPublishSettings settings,
         ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(source.toJso()));
      params.set(1, new JSONObject(settings.toJso()));
      params.set(2, new JSONString(account));
      params.set(3, new JSONString(server));
      params.set(4, new JSONString(appName));
      params.set(5, new JSONString(StringUtil.notNull(appTitle)));
      params.set(6, new JSONString(StringUtil.notNull(appId)));
      sendRequest(RPC_SCOPE,
            RSCONNECT_PUBLISH,
            params,
            requestCallback);
   }

   @Override
   public void cancelPublish(ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            CANCEL_PUBLISH,
            requestCallback);
   }

   @Override
   public void validateServerUrl(String url,
         ServerRequestCallback<RSConnectServerInfo> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(url));
      sendRequest(RPC_SCOPE,
            VALIDATE_SERVER_URL,
            params,
            requestCallback);
   }

   @Override
   public void getServerUrls(
         ServerRequestCallback<JsArray<RSConnectServerEntry>> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            GET_SERVER_URLS,
            new JSONArray(),
            requestCallback);
   }

   @Override
   public void getPreAuthToken(String serverName,
         ServerRequestCallback<RSConnectPreAuthToken> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(serverName));
      sendRequest(RPC_SCOPE,
            GET_AUTH_TOKEN,
            params,
            requestCallback);
   }

   @Override
   public void getUserFromToken(String url,
         RSConnectPreAuthToken token,
         ServerRequestCallback<RSConnectAuthUser> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(url));
      params.set(1, new JSONString(token.getToken()));
      params.set(2, new JSONString(token.getPrivateKey()));
      sendRequest(RPC_SCOPE,
            GET_USER_FROM_TOKEN,
            params,
            requestCallback);
   }

   @Override
   public void registerUserToken(String serverName, String accountName, int userId,
                RSConnectPreAuthToken token,
                ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(serverName));
      params.set(1, new JSONString(accountName));
      params.set(2, new JSONNumber(userId));
      params.set(3, new JSONString(token.getToken()));
      params.set(4, new JSONString(token.getPrivateKey()));
      sendRequest(RPC_SCOPE,
            REGISTER_USER_TOKEN,
            params,
            requestCallback);
   }

   @Override
   public void getDeploymentFiles(String dir,
         boolean asMultipleRmd,
         ServerRequestCallback<RSConnectDeploymentFiles> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(dir));
      params.set(1, JSONBoolean.getInstance(asMultipleRmd));
      sendRequest(RPC_SCOPE,
            GET_DEPLOYMENT_FILES,
            params,
            requestCallback);
   }

   @Override
   public void getLintResults(String target,
         ServerRequestCallback<RSConnectLintResults> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(target));
      sendRequest(RPC_SCOPE,
            GET_RSCONNECT_LINT_RESULTS,
            params,
            requestCallback);
   }

   @Override
   public void getRmdPublishDetails(String target,
         ServerRequestCallback<RmdPublishDetails> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(target));
      sendRequest(RPC_SCOPE,
            GET_RMD_PUBLISH_DETAILS,
            params,
            requestCallback);
   }

   @Override
   public void hasOrphanedAccounts(
         ServerRequestCallback<Double> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            HAS_ORPHANED_ACCOUNTS,
            new JSONArray(),
            requestCallback);
   }

   @Override
   public void generateAppName(String title, String appPath, String accountName,
         ServerRequestCallback<RSConnectAppName> resultCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(title));
      params.set(1, new JSONString(StringUtil.isNullOrEmpty(appPath) ?
            "" : appPath));
      params.set(2, new JSONString(accountName));
      sendRequest(RPC_SCOPE, "generate_app_name", params, resultCallback);
   }

   @Override
   public void getEditPublishedDocs(String appPath,
         ServerRequestCallback<JsArrayString> resultCallback)
   {
      sendRequest(RPC_SCOPE,
                 "get_edit_published_docs",
                 appPath,
                 resultCallback);
   }

   @Override
   public void getRMarkdownContext(
                  ServerRequestCallback<RMarkdownContext> requestCallback)
   {
      sendRequest(RPC_SCOPE, "get_rmarkdown_context", requestCallback);
   }


   @Override
   public void renderRmd(String file, int line, String format, String encoding,
                         String paramsFile, boolean asTempfile, int type,
                         String existingOutputFile, String workingDir,
                         String viewerType,
         ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(file));
      params.set(1, new JSONNumber(line));
      params.set(2, new JSONString(StringUtil.notNull(format)));
      params.set(3, new JSONString(encoding));
      params.set(4, new JSONString(StringUtil.notNull(paramsFile)));
      params.set(5, JSONBoolean.getInstance(asTempfile));
      params.set(6, new JSONNumber(type));
      params.set(7, new JSONString(StringUtil.notNull(existingOutputFile)));
      params.set(8, new JSONString(StringUtil.notNull(workingDir)));
      params.set(9, new JSONString(StringUtil.notNull(viewerType)));
      sendRequest(RPC_SCOPE,
            RENDER_RMD,
            params,
            requestCallback);
   }

   @Override
   public void renderRmdSource(String source,
         ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, RENDER_RMD_SOURCE, source, requestCallback);
   }


   @Override
   public void maybeCopyWebsiteAsset(String file,
                                ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, "maybe_copy_website_asset", file, requestCallback);
   }

   @Override
   public void terminateRenderRmd(boolean normal,
                                  ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(normal));
      sendRequest(RPC_SCOPE,
            TERMINATE_RENDER_RMD,
            params,
            requestCallback);
   }

   @Override
   public void rmdOutputFormat(String file,
                               String encoding,
                               ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(file));
      params.set(1, new JSONString(encoding));
      sendRequest(RPC_SCOPE, "rmd_output_format", params, requestCallback);
   }

   @Override
   public void convertToYAML(JavaScriptObject input,
         ServerRequestCallback<RmdYamlResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(input));
      sendRequest(RPC_SCOPE,
            CONVERT_TO_YAML,
            params,
            requestCallback);
   }

   @Override
   public void convertFromYAML(String yaml,
         ServerRequestCallback<RmdYamlData> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(yaml));
      sendRequest(RPC_SCOPE,
            CONVERT_FROM_YAML,
            params,
            requestCallback);
   }

   @Override
   public void getRmdTemplates(
         ServerRequestCallback<JsArray<RmdDocumentTemplate>> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            GET_RMD_TEMPLATES,
            requestCallback);
   }

   @Override
   public void createRmdFromTemplate(String filePath, String templatePath,
         boolean createDirectory,
         ServerRequestCallback<RmdCreatedTemplate> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(filePath));
      params.set(1, new JSONString(templatePath));
      params.set(2, JSONBoolean.getInstance(createDirectory));
      sendRequest(RPC_SCOPE,
            CREATE_RMD_FROM_TEMPLATE,
            params,
            requestCallback);
   }

   @Override
   public void getRmdTemplate(String templatePath,
         ServerRequestCallback<RmdTemplateContent> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(templatePath));
      sendRequest(RPC_SCOPE,
            GET_RMD_TEMPLATE,
            params,
            requestCallback);
   }

   @Override
   public void prepareForRmdChunkExecution(
         String id,
         ServerRequestCallback<RmdExecutionState> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  "prepare_for_rmd_chunk_execution",
                  id,
                  requestCallback);
   }

   @Override
   public void refreshChunkOutput(String docPath, String docId,
         String contextId, String requestId, String chunkId,
         ServerRequestCallback<NotebookDocQueue> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docPath == null ? "" : docPath));
      params.set(1, new JSONString(docId));
      params.set(2, new JSONString(contextId));
      params.set(3, new JSONString(requestId));
      params.set(4, new JSONString(chunkId));
      sendRequest(RPC_SCOPE,
            "refresh_chunk_output",
            params,
            requestCallback);
   }

   @Override
   public void setChunkConsole(String docId, String chunkId, int commitMode,
         int execMode, int execScope, String options, int pixelWidth,
         int charWidth,
         ServerRequestCallback<RmdChunkOptions> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      params.set(1, new JSONString(chunkId));
      params.set(2, new JSONNumber(commitMode));
      params.set(3, new JSONNumber(execMode));
      params.set(4, new JSONNumber(execScope));
      params.set(5, new JSONString(options));
      params.set(6, new JSONNumber(pixelWidth));
      params.set(7, new JSONNumber(charWidth));
      sendRequest(RPC_SCOPE,
            "set_chunk_console",
            params,
            requestCallback);
   }

   @Override
   public void createNotebookFromCache(String rmdPath,
               String outputPath,
               ServerRequestCallback<NotebookCreateResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(rmdPath));
      params.set(1, new JSONString(outputPath));
      sendRequest(RPC_SCOPE, "create_notebook_from_cache", params, requestCallback);
   }

   @Override
   public void replayNotebookPlots(String docId, String initialChunkId,
         int pixelWidth, int pixelHeight, ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      params.set(1, new JSONString(initialChunkId));
      params.set(2, new JSONNumber(pixelWidth));
      params.set(3, new JSONNumber(pixelHeight));
      sendRequest(RPC_SCOPE, "replay_notebook_plots", params, requestCallback);
   }

   @Override
   public void replayNotebookChunkPlots(String docId, String chunkId,
         int pixelWidth, int pixelHeight, ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      params.set(1, new JSONString(chunkId));
      params.set(2, new JSONNumber(pixelWidth));
      params.set(3, new JSONNumber(pixelHeight));
      sendRequest(RPC_SCOPE, "replay_notebook_chunk_plots", params, requestCallback);
   }

   @Override
   public void cleanReplayNotebookChunkPlots(String docId, String chunkId,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      params.set(1, new JSONString(chunkId));
      sendRequest(RPC_SCOPE, "clean_replay_notebook_chunk_plots", params, requestCallback);
   }

   @Override
   public void executeAlternateEngineChunk(String docId,
                                           String chunkId,
                                           int commitMode,
                                           String engine,
                                           String code,
                                           JsObject options,
                                           ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      params.set(1, new JSONString(chunkId));
      params.set(2, new JSONNumber(commitMode));
      params.set(3, new JSONString(engine));
      params.set(4, new JSONString(code));
      params.set(5, new JSONObject(options));
      sendRequest(RPC_SCOPE, "execute_alternate_engine_chunk", params, requestCallback);
   }

   public void executeNotebookChunks(NotebookDocQueue queue,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(queue));
      sendRequest(RPC_SCOPE, "execute_notebook_chunks", params,
            requestCallback);
   }

   public void updateNotebookExecQueue(NotebookQueueUnit unit, int op,
         String beforeChunkId, ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(unit));
      params.set(1, new JSONNumber(op));
      params.set(2, new JSONString(beforeChunkId));
      sendRequest(RPC_SCOPE, "update_notebook_exec_queue", params,
            requestCallback);
   }

   @Override
   public void interruptChunk(String docId,
                              String chunkId,
                              ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(docId));
      params.set(1, new JSONString(chunkId));
      sendRequest(RPC_SCOPE, "interrupt_chunk", params, requestCallback);
   }

   @Override
   public void getRmdOutputInfo(String input,
         ServerRequestCallback<RmdOutputInfo> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(input));
      sendRequest(RPC_SCOPE, GET_RMD_OUTPUT_INFO, params, requestCallback);
   }
   
   @Override
   public void rmdImportImages(JsArrayString images, String imagesDir,
                               ServerRequestCallback<JsArrayString> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONArray(images));
      params.set(1,  new JSONString(imagesDir));
      sendRequest(RPC_SCOPE, RMD_IMPORT_IMAGES, params, requestCallback);
   }

   @Override
   public void unsatisfiedDependencies(
      JsArray<Dependency> dependencies,
      boolean silentUpdate,
      ServerRequestCallback<JsArray<Dependency>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONArray(dependencies));
      params.set(1, JSONBoolean.getInstance(silentUpdate));
      sendRequest(RPC_SCOPE,
                  "unsatisfied_dependencies",
                  params,
                  requestCallback);
   }

   @Override
   public void installDependencies(
      String context,
      JsArray<Dependency> dependencies,
      ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(context)));
      params.set(1, new JSONArray(dependencies));
      sendRequest(RPC_SCOPE,
                  "install_dependencies",
                  params,
                  requestCallback);
   }

   @Override
   public  void getPackratPrerequisites(
         ServerRequestCallback<PackratPrerequisites> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_PACKRAT_PREREQUISITES, requestCallback);
   }

   @Override
   public void installPackrat(
                   ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, INSTALL_PACKRAT, requestCallback);
   }

   @Override
   public void getPackratContext(
                     ServerRequestCallback<PackratContext> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_PACKRAT_CONTEXT, requestCallback);
   }


   @Override
   public void getPackratStatus(String dir,
         ServerRequestCallback<JsArray<PackratStatus>> requestCallback)
   {

      JSONArray params = new JSONArray();
      params.set(0, new JSONString(dir));
      sendRequest(RPC_SCOPE,
                  GET_PACKRAT_STATUS,
                  params,
                  requestCallback);
   }

   public void getPackratActions(ServerRequestCallback<PackratActions> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_PACKRAT_ACTIONS, requestCallback);
   }

   @Override
   public void packratBootstrap(String dir,
                                boolean enter,
                                ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(dir));
      params.set(1, JSONBoolean.getInstance(enter));
      sendRequest(RPC_SCOPE,
                  PACKRAT_BOOTSTRAP,
                  params,
                  requestCallback);
   }

   @Override
   public void getPendingActions(
         String action,
         String dir,
         ServerRequestCallback<JsArray<PackratPackageAction>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(action));
      params.set(1, new JSONString(dir));
      sendRequest(RPC_SCOPE,
                  GET_PENDING_ACTIONS,
                  params,
                  requestCallback);
   }

   @Override
   public void renvInit(String projDir,
                        ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(projDir)
            .get();

      sendRequest(RPC_SCOPE, RENV_INIT, params, requestCallback);
   }

   @Override
   public void renvActions(String action,
                           ServerRequestCallback<JsArray<RenvAction>> requestCallback)
   {
      JSONArray params = new JSONArrayBuilder()
            .add(action)
            .get();

      sendRequest(RPC_SCOPE, RENV_ACTIONS, params, requestCallback);
   }

   @Override
   public void markersTabClosed(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "markers_tab_closed", requestCallback);
   }

   @Override
   public void updateActiveMarkerSet(String set,
                                     ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, "update_active_marker_set", set, callback);
   }

   @Override
   public void clearActiveMarkerSet(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, "clear_active_marker_set", requestCallback);
   }

   @Override
   public void lintRSourceDocument(String documentId,
                                   String documentPath,
                                   boolean showMarkersPane,
                                   boolean explicit,
                                   ServerRequestCallback<JsArray<LintItem>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(documentId));
      params.set(1, new JSONString(documentPath));
      params.set(2, JSONBoolean.getInstance(showMarkersPane));
      params.set(3, JSONBoolean.getInstance(explicit));
      sendRequest(RPC_SCOPE, LINT_R_SOURCE_DOCUMENT, params, requestCallback);
   }

   @Override
   public void analyzeProject(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, ANALYZE_PROJECT, requestCallback);
   }

   @Override
   public void getSetClassCall(String call,
                               ServerRequestCallback<SetClassCall> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(call));
      sendRequest(RPC_SCOPE, GET_SET_CLASS_CALL, params, requestCallback);
   }

   @Override
   public void getSetGenericCall(String call,
                                 ServerRequestCallback<SetGenericCall> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(call));
      sendRequest(RPC_SCOPE, GET_SET_GENERIC_CALL, params, requestCallback);
   }

   @Override
   public void getSetMethodCall(String call,
                                ServerRequestCallback<SetMethodCall> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(call));
      sendRequest(RPC_SCOPE, GET_SET_METHOD_CALL, params, requestCallback);
   }

   @Override
   public void getSetRefClassCall(String call,
                                  ServerRequestCallback<SetRefClassCall> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(call));
      sendRequest(RPC_SCOPE, GET_SET_REF_CLASS_CALL, params, requestCallback);
   }

   @Override
   public void transformSnippet(String snippetContent,
                                ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(snippetContent));
      sendRequest(RPC_SCOPE, TRANSFORM_SNIPPET, params, requestCallback);
   }

   @Override
   public void getSnippets(ServerRequestCallback<JsArray<SnippetData>> callback)
   {
      sendRequest(RPC_SCOPE, GET_SNIPPETS, callback);
   }

   @Override
   public void getProjectSharedUsers(
         ServerRequestCallback<JsArray<ProjectUserRole>> callback)
   {
      sendRequest(RPC_SCOPE, "get_shared_users", callback);
   }

   @Override
   public void setProjectSharedUsers(JsArrayString users,
         ServerRequestCallback<SharingResult> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONArray(users));
      sendRequest(RPC_SCOPE, "set_shared_users", params, callback);
   }

   @Override
   public void getAllServerUsers(ServerRequestCallback<JsArrayString> callback)
   {
      sendRequest(RPC_SCOPE, "get_all_users", callback);
   }

   @Override
   public void validateSharingConfig(
         ServerRequestCallback<SharingConfigResult> callback)
   {
      sendRequest(RPC_SCOPE, "validate_sharing_config", callback);
   }

   @Override
   public void getSharedProjects(int maxProjects,
         ServerRequestCallback<JsArray<SharedProjectDetails>> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(maxProjects));
      sendRequest(RPC_SCOPE, "get_shared_projects", params, callback);
   }

   @Override
   public void setCurrentlyEditing(String path,
         String id,
         ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONString(id));
      sendRequest(RPC_SCOPE, "set_currently_editing", params, callback);
   }

   @Override
   public void reportCollabDisconnected(String path, String id,
         ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONString(id));
      sendRequest(RPC_SCOPE, "report_collab_disconnected", params, callback);
   }

   @Override
   public void getProjectUser(String sessionId,
         ServerRequestCallback<ProjectUser> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(sessionId));
      sendRequest(RPC_SCOPE, "get_project_user", params, callback);
   }

   @Override
   public void setFollowingUser(String sessionId,
         ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(sessionId));
      sendRequest(RPC_SCOPE, "set_following_user", params, callback);
   }

   @Override
   public void previewDataImport(DataImportOptions dataImportOptions,
                                 int maxCols,
                                 int maxFactors,
                                 ServerRequestCallback<DataImportPreviewResponse> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(dataImportOptions));
      params.set(1, new JSONNumber(maxCols));
      params.set(2, new JSONNumber(maxFactors));
      sendRequest(RPC_SCOPE, PREVIEW_DATA_IMPORT, params, requestCallback);
   }

   @Override
   public void assembleDataImport(DataImportOptions dataImportOptions,
                                  ServerRequestCallback<DataImportAssembleResponse> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(dataImportOptions));
      sendRequest(RPC_SCOPE, ASSEMBLE_DATA_IMPORT, params, requestCallback);
   }

   @Override
   public void startProfiling(ProfileOperationRequest profilerRequest,
                              ServerRequestCallback<ProfileOperationResponse> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(profilerRequest));
      sendRequest(RPC_SCOPE, START_PROFILING, params, requestCallback);
   }

   @Override
   public void stopProfiling(ProfileOperationRequest profilerRequest,
                             ServerRequestCallback<ProfileOperationResponse> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(profilerRequest));
      sendRequest(RPC_SCOPE, STOP_PROFILING, params, requestCallback);
   }

   @Override
   public void previewDataImportAsync(DataImportOptions dataImportOptions,
                                      int maxCols,
                                      int maxFactors,
                                      ServerRequestCallback<DataImportPreviewResponse> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(dataImportOptions));
      params.set(1, new JSONNumber(maxCols));
      params.set(2, new JSONNumber(maxFactors));
      sendRequest(RPC_SCOPE, PREVIEW_DATA_IMPORT_ASYNC, params, requestCallback);
   }

   @Override
   public void previewDataImportClean(DataImportOptions dataImportOptions,
                                      ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(dataImportOptions));
      sendRequest(RPC_SCOPE, PREVIEW_DATA_IMPORT_CLEAN, params, requestCallback);
   }

   @Override
   public void previewDataImportAsyncAbort(ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      sendRequest(RPC_SCOPE, PREVIEW_DATA_IMPORT_ASYNC_ABORT, params, requestCallback);
   }

   public void openProfile(ProfileOperationRequest profilerRequest,
                           ServerRequestCallback<ProfileOperationResponse> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(profilerRequest));
      sendRequest(RPC_SCOPE, OPEN_PROFILE, params, requestCallback);
   }

   public void copyProfile(String fromPath, String toPath,
                           ServerRequestCallback<JavaScriptObject> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(fromPath));
      params.set(1, new JSONString(toPath));
      sendRequest(RPC_SCOPE, COPY_PROFILE, params, requestCallback);
   }

   public void clearProfile(String path,
                           ServerRequestCallback<JavaScriptObject> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      sendRequest(RPC_SCOPE, CLEAR_PROFILE, params, requestCallback);
   }

   public void profileSources(String path, String normPath,
                              ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONString(normPath));
      sendRequest(RPC_SCOPE, PROFILE_SOURCES, params, requestCallback);
   }

   public void removeConnection(ConnectionId id,
                                ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, REMOVE_CONNECTION, id, callback);
   }

   public void connectionDisconnect(ConnectionId connectionId,
                                    ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, CONNECTION_DISCONNECT, connectionId, callback);
   }

   @Override
   public void connectionExecuteAction(ConnectionId connection,
                            String action,
                            ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(connection));
      params.set(1, new JSONString(action));
      sendRequest(RPC_SCOPE, CONNECTION_EXECUTE_ACTION, params, callback);
   }

   @Override
   public void connectionListObjects(
                              ConnectionId connectionId,
                              ConnectionObjectSpecifier container,
                              ServerRequestCallback<JsArray<DatabaseObject>> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(connectionId));
      params.set(1, new JSONArray(container.asJsArray()));
      sendRequest(RPC_SCOPE, CONNECTION_LIST_OBJECTS, params, callback);
   }

   @Override
   public void connectionListFields(
                              ConnectionId connectionId,
                              ConnectionObjectSpecifier object,
                              ServerRequestCallback<JsArray<Field>> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(connectionId));
      params.set(1, new JSONArray(object.asJsArray()));
      sendRequest(RPC_SCOPE, CONNECTION_LIST_FIELDS, params, callback);
   }

   @Override
   public void connectionPreviewObject(ConnectionId connectionId,
                                       ConnectionObjectSpecifier object,
                                       ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(connectionId));
      params.set(1, new JSONArray(object.asJsArray()));
      sendRequest(RPC_SCOPE, CONNECTION_PREVIEW_OBJECT, params, callback);
   }

   public void getNewConnectionContext(
         ServerRequestCallback<NewConnectionContext> callback)
   {
      sendRequest(RPC_SCOPE, GET_NEW_CONNECTION_CONTEXT, callback);
   }

   @Override
   public void defaultSqlConnectionName(ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      sendRequest(RPC_SCOPE, SQL_CHUNK_DEFAULT_CONNECTION, params, requestCallback);
   }

   @Override
   public void connectionTest(String code,
                              ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(code));
      sendRequest(RPC_SCOPE, CONNECTION_TEST, params, callback);
   }



   @Override
   public void launchEmbeddedShinyConnectionUI(String packageName,
                                               String connectionName,
                                               ServerRequestCallback<RResult<Void>> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(packageName));
      params.set(1, new JSONString(connectionName));
      sendRequest(RPC_SCOPE, LAUNCH_EMBEDDED_SHINY_CONNECTION_UI, params, callback);
   }

   @Override
   public void showDialogCompleted(String prompt,
                                   boolean ok,
                                   ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, prompt == null ? JSONNull.getInstance() : new JSONString(prompt));
      params.set(1, JSONBoolean.getInstance(ok));
      sendRequest(RPC_SCOPE, RSTUDIOAPI_SHOW_DIALOG_COMPLETED, params, true, callback);
   }

   @Override
   public void stopShinyApp(String id, ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      sendRequest(RPC_SCOPE, STOP_SHINY_APP, params, true, callback);
   }

   @Override
   public void connectionAddPackage(String packageName,
                                    ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(packageName));
      sendRequest(RPC_SCOPE, CONNECTION_ADD_PACKAGE, params, callback);
   }

   @Override
   public void askSecretCompleted(String value,
                                  boolean remember,
                                  boolean changed,
                                  ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, value == null ? JSONNull.getInstance()
                                  : new JSONString(value));
      params.set(1, JSONBoolean.getInstance(remember));
      params.set(2, JSONBoolean.getInstance(changed));
      sendRequest(RPC_SCOPE, ASKSECRET_COMPLETED, params, true, requestCallback);
   }

   @Override
   public void installOdbcDriver(String name,
                                 String installationPath,
                                 ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  INSTALL_ODBC_DRIVER,
                  name,
                  installationPath,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }

   public void getOdbcConnectionContext(String name,
                                        ServerRequestCallback<NewConnectionInfo> callback)
   {
      sendRequest(RPC_SCOPE,
                  GET_NEW_ODBC_CONNECTION_CONTEXT,
                  name,
                  callback);
   }

   @Override
   public void uninstallOdbcDriver(String name,
                                   ServerRequestCallback<ConnectionUninstallResult> callback)
   {
      sendRequest(RPC_SCOPE,
                  UNINSTALL_ODBC_DRIVER,
                  name,
                  callback);
   }

   @Override
   public void updateOdbcInstallers(ServerRequestCallback<ConnectionUpdateResult> callback)
   {
      sendRequest(RPC_SCOPE,
                  UPDATE_ODBC_INSTALLERS,
                  callback);
   }

   @Override
   public void setJobListening(String id, boolean listening, boolean bypassLauncherCall,
                               ServerRequestCallback<JsArray<JobOutput>> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, JSONBoolean.getInstance(listening));
      params.set(2, JSONBoolean.getInstance(bypassLauncherCall));
      sendRequest(RPC_SCOPE, "set_job_listening", params, callback);
   }

   @Override
   public void executeJobAction(String id, String action,
                                ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, new JSONString(action));
      sendRequest(RPC_SCOPE, "execute_job_action", params, callback);
   }

   @Override
   public void startJob(JobLaunchSpec spec,
                        ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(spec));
      sendRequest(RPC_SCOPE, "run_script_job", params, callback);
   }

   @Override
   public void clearJobs(ServerRequestCallback<Void> callback)
   {
      sendRequest(RPC_SCOPE, "clear_jobs", callback);
   }

   @Override
   public void hasShinyTestDependenciesInstalled(ServerRequestCallback<Boolean> callback)
   {
      sendRequest(RPC_SCOPE,
                  HAS_SHINYTEST_HAS_DEPENDENCIES,
                  callback);
   }

   @Override
   public void installShinyTestDependencies(ServerRequestCallback<ConsoleProcess> callback)
   {
      sendRequest(RPC_SCOPE,
                  INSTALL_SHINYTEST_DEPENDENCIES,
                  new ConsoleProcessCallbackAdapter(callback));
   }

   @Override
   public void hasShinyTestResults(String testFile, ServerRequestCallback<ShinyTestResults> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(testFile));

      sendRequest(RPC_SCOPE,
                  HAS_SHINYTEST_RESULTS,
                  params,
                  true,
                  callback);
   }

   @Override
   public void getSecondaryRepos(ServerRequestCallback<SecondaryReposResult> callback,
                                 String cranRepoUrl,
                                 boolean cranIsCustom)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(cranRepoUrl));
      params.set(1, JSONBoolean.getInstance(cranIsCustom));

      sendRequest(RPC_SCOPE,
                  GET_SECONDARY_REPOS,
                  params,
                  true,
                  callback);
   }

   @Override
   public void validateCranRepo(ServerRequestCallback<Boolean> callback,
                                String cranRepoUrl)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(cranRepoUrl));

      sendRequest(RPC_SCOPE,
                  VALIDATE_CRAN_REPO,
                  params,
                  true,
                  callback);
   }

   @Override
   public void getThemes(ServerRequestCallback<JsArray<AceTheme>> callback)
   {
      sendRequest(RPC_SCOPE, GET_THEMES, new JSONArray(), callback);
   }

   @Override
   public void addTheme(ServerRequestCallback<String> callback, String themeLocation)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(themeLocation));
      sendRequest(RPC_SCOPE, ADD_THEME, params, callback);
   }

   @Override
   public void removeTheme(ServerRequestCallback<Void> callback, String themeName)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(themeName));
      sendRequest(RPC_SCOPE, REMOVE_THEME, params, callback);
   }

   @Override
   public void getThemeName(ServerRequestCallback<String> callback, String themeLocation)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(themeLocation));
      sendRequest(RPC_SCOPE, GET_THEME_NAME, params, callback);
   }

   @Override
   public void setComputedThemeColors(String foreground, String background, VoidServerRequestCallback callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(foreground));
      params.set(1, new JSONString(background));
      sendRequest(RPC_SCOPE, SET_COMPUTED_THEME_COLORS, params, callback);
   }

   @Override
   public void replaceCommentHeader(String command,
                                    String path,
                                    String code,
                                    ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(command));
      params.set(1, new JSONString(path));
      params.set(2, new JSONString(code));
      sendRequest(RPC_SCOPE, REPLACE_COMMENT_HEADER, params, callback);
   }

   @Override
   public void pandocGetCapabilities(ServerRequestCallback<JavaScriptObject> callback)
   {
      sendRequest(RPC_SCOPE, PANDOC_GET_CAPABILITIES, callback);
   }

   @Override
   public void pandocMarkdownToAst(String markdown, String format, JsArrayString options,
                                   ServerRequestCallback<JavaScriptObject> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(markdown));
      params.set(1, new JSONString(format));
      setArrayString(params, 2, options);
      sendRequest(RPC_SCOPE, PANDOC_MARKDOWN_TO_AST, params, callback);
   }

   @Override
   public void pandocAstToMarkdown(JavaScriptObject ast, String format, JsArrayString options,
                                   ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(ast));
      params.set(1, new JSONString(format));
      setArrayString(params, 2, options);
      sendRequest(RPC_SCOPE, PANDOC_AST_TO_MARKDOWN, params, callback);
   }

   @Override
   public void pandocListExtensions(String format, ServerRequestCallback<String> callback)
   {
      sendRequest(RPC_SCOPE, PANDOC_LIST_EXTENSIONS, format, callback);
   }

   @Override
   public void pandocGetBibliography(String file, JsArrayString bibliographies, String refBlock, String etag, ServerRequestCallback<JavaScriptObject> callback) {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(file)));
      params.set(1, new JSONArray(bibliographies));
      params.set(2, new JSONString(StringUtil.notNull(refBlock)));
      params.set(3, new JSONString(StringUtil.notNull(etag)));
      sendRequest(RPC_SCOPE, PANDOC_GET_BIBLIOGRAPHY, params, callback);
   }

   @Override
   public void pandocAddToBibliography(String bibliography, boolean project, String id, String sourceAsJson, String sourceAsBibTeX,
                                       ServerRequestCallback<Boolean> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(bibliography)));
      params.set(1, JSONBoolean.getInstance(project));
      params.set(2, new JSONString(id));
      params.set(3, new JSONString(sourceAsJson));
      params.set(4, new JSONString(sourceAsBibTeX));
      sendRequest(RPC_SCOPE, PANDOC_ADD_TO_BIBLIOGRAPHY, params, callback);
   }

   @Override
   public void pandocCitationHTML(String file, String sourceAsJson, String csl, ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(file)));
      params.set(1, new JSONString(sourceAsJson));
      params.set(2, new JSONString(StringUtil.notNull(csl)));
      sendRequest(RPC_SCOPE, PANDOC_CITATION_HTML, params, callback);
   }


   @Override
   public void crossrefWorks(String query, ServerRequestCallback<JavaScriptObject> callback)
   {
      sendRequest(RPC_SCOPE, CROSSREF_WORKS, query, callback);
   }
   
   @Override
   public void dataciteSearch(String query, ServerRequestCallback<JavaScriptObject> callback)
   {
      sendRequest(RPC_SCOPE, DATACITE_SEARCH, query, callback);  
   }
   
   @Override
   public void pubmedSearch(String query, ServerRequestCallback<JavaScriptObject> callback)
   {
      sendRequest(RPC_SCOPE, PUBMED_SEARCH, query, callback);  
   }

   @Override
   public void zoteroGetCollections(String file,
                                    JsArrayString collections,
                                    JsArray<PanmirrorZoteroCollectionSpec> cached,
                                    boolean useCache,
                                    ServerRequestCallback<JavaScriptObject> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(file)));
      params.set(1, new JSONArray(collections));
      params.set(2, new JSONArray(cached));
      params.set(3,  JSONBoolean.getInstance(useCache));
      sendRequest(RPC_SCOPE, ZOTERO_GET_COLLECTIONS, params, callback);
   }

   @Override
   public void zoteroGetLibraryNames(ServerRequestCallback<JavaScriptObject> callback)
   {
      sendRequest(RPC_SCOPE, ZOTERO_GET_LIBRARY_NAMES, callback);
   }
   
   @Override
   public void zoteroGetActiveCollectionSpecs(String file, 
                                              JsArrayString collections,
                                              ServerRequestCallback<JavaScriptObject> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(file)));
      params.set(1, new JSONArray(collections));
      sendRequest(RPC_SCOPE, ZOTERO_GET_ACTIVE_COLLECTIONSPECS, params, callback);
      
   }
   
   @Override
   public void zoteroValidateWebAPIKey(String key, ServerRequestCallback<Boolean> callback)
   {
      sendRequest(RPC_SCOPE, ZOTERO_VALIDATE_WEB_API_KEY, key, callback);
   }

   @Override
   public void zoteroDetectLocalConfig(ServerRequestCallback<elemental2.core.JsObject> callback)
   {
      sendRequest(RPC_SCOPE, ZOTERO_DETECT_LOCAL_CONFIG, callback);
   }

   @Override
   public void zoteroBetterBibtexExport(JsArrayString itemKeys, String translatorId, int libraryID,
                                        ServerRequestCallback<JavaScriptObject> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONArray(itemKeys));
      params.set(1, new JSONString(translatorId));
      params.set(2, new JSONNumber(libraryID));
      sendRequest(RPC_SCOPE, ZOTERO_BETTER_BIBTEX_EXPORT, params, callback);
   }

   @Override
   public void doiFetchCSL(String doi, ServerRequestCallback<JavaScriptObject> callback)
   {
      sendRequest(RPC_SCOPE, DOI_FETCH_CSL, doi, callback);
   }

   @Override
   public void xrefIndexForFile(String file, ServerRequestCallback<JavaScriptObject> callback)
   {
      sendRequest(RPC_SCOPE, XREF_INDEX_FOR_FILE, file, callback);
   }

   @Override
   public void xrefForId(String file, String id, ServerRequestCallback<JavaScriptObject> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(file));
      params.set(1, new JSONString(id));
      sendRequest(RPC_SCOPE, XREF_FOR_ID, params, callback);
   }

   @Override
   public void getInstalledFonts(ServerRequestCallback<JsArrayString> callback)
   {
      sendRequest(RPC_SCOPE, "get_installed_fonts", callback);
   }

   protected String clientInitId_ = "";
   private String clientId_;
   private String clientVersion_ = "";
   private JsObject launchParameters_;
   private String userHomePath_;
   private boolean listeningForEvents_;
   private boolean disconnected_;
   private boolean sessionRelaunchPending_;

   private final RemoteServerAuth serverAuth_;
   private final RemoteServerEventListener serverEventListener_;

   private final Provider<ConsoleProcessFactory> pConsoleProcessFactory_;

   protected final Session session_;
   protected final EventBus eventBus_;

   // url scopes
   protected static final String RPC_SCOPE = "rpc";
   private static final String FILES_SCOPE = "files";
   private static final String EVENTS_SCOPE = "events";
   private static final String UPLOAD_SCOPE = "upload";
   private static final String EXPORT_SCOPE = "export";
   private static final String GRAPHICS_SCOPE = "graphics";
   private static final String SOURCE_SCOPE = "source";
   private static final String LOG_SCOPE = "log";
   private static final String META_SCOPE = "meta";
   private static final String FILE_SHOW = "file_show";
   protected static final String JOB_LAUNCHER_RPC_SCOPE = "job_launcher_rpc";

   // session methods
   private static final String CLIENT_INIT = "client_init";
   private static final String SUSPEND_SESSION = "suspend_session";
   private static final String HANDLE_UNSAVED_CHANGES_COMPLETED = "handle_unsaved_changes_completed";
   private static final String QUIT_SESSION = "quit_session";
   private static final String SUSPEND_FOR_RESTART = "suspend_for_restart";
   private static final String PING = "ping";
   private static final String RSTUDIOAPI_RESPONSE = "rstudioapi_response";

   private static final String SET_WORKBENCH_METRICS = "set_workbench_metrics";
   private static final String SET_PREFS = "set_prefs";
   private static final String SET_USER_PREFS = "set_user_prefs";
   private static final String SET_USER_STATE = "set_user_state";
   private static final String GET_R_PREFS = "get_r_prefs";
   private static final String SET_CLIENT_STATE = "set_client_state";
   private static final String USER_PROMPT_COMPLETED = "user_prompt_completed";
   private static final String ADMIN_NOTIFICATION_ACKNOWLEDGED = "admin_notification_acknowledged";
   private static final String GET_TERMINAL_OPTIONS = "get_terminal_options";
   private static final String GET_TERMINAL_SHELLS = "get_terminal_shells";
   private static final String START_TERMINAL = "start_terminal";
   private static final String SEARCH_CODE = "search_code";
   private static final String GET_SEARCH_PATH_FUNCTION_DEFINITION = "get_search_path_function_definition";
   private static final String GET_METHOD_DEFINITION = "get_method_definition";
   private static final String GET_FUNCTION_DEFINITION = "get_function_definition";
   private static final String FIND_FUNCTION_IN_SEARCH_PATH = "find_function_in_search_path";

   private static final String CONSOLE_INPUT = "console_input";
   private static final String RESET_CONSOLE_ACTIONS = "reset_console_actions";
   private static final String INTERRUPT = "interrupt";
   private static final String ABORT = "abort";
   private static final String ADAPT_TO_LANGUAGE = "adapt_to_language";
   private static final String EXECUTE_CODE = "execute_code";
   private static final String GET_DPLYR_JOIN_COMPLETIONS_STRING =
         "get_dplyr_join_completions_string";
   private static final String GET_DPLYR_JOIN_COMPLETIONS = "get_dplyr_join_completions";
   private static final String GET_ARGS = "get_args";
   private static final String EXTRACT_CHUNK_OPTIONS = "extract_chunk_options";
   private static final String EXECUTE_USER_COMMAND = "execute_user_command";
   private static final String GET_COMPLETIONS = "get_completions";
   private static final String IS_FUNCTION = "is_function";
   private static final String GET_HELP_AT_CURSOR = "get_help_at_cursor";

   private static final String PROCESS_START = "process_start";
   private static final String PROCESS_INTERRUPT = "process_interrupt";
   private static final String PROCESS_REAP = "process_reap";
   private static final String PROCESS_WRITE_STDIN = "process_write_stdin";
   private static final String PROCESS_SET_SIZE = "process_set_size";
   private static final String PROCESS_SET_CAPTION = "process_set_caption";
   private static final String PROCESS_SET_TITLE = "process_set_title";
   private static final String PROCESS_ERASE_BUFFER = "process_erase_buffer";
   private static final String PROCESS_GET_BUFFER_CHUNK = "process_get_buffer_chunk";
   private static final String PROCESS_GET_BUFFER = "process_get_buffer";
   private static final String PROCESS_USE_RPC = "process_use_rpc";
   private static final String PROCESS_TEST_EXISTS = "process_test_exists";
   private static final String PROCESS_NOTIFY_VISIBLE = "process_notify_visible";
   private static final String PROCESS_INTERRUPT_CHILD = "process_interrupt_child";

   private static final String REMOVE_ALL_OBJECTS = "remove_all_objects";
   private static final String REMOVE_OBJECTS = "remove_objects";
   private static final String DOWNLOAD_DATA_FILE = "download_data_file";
   private static final String GET_DATA_PREVIEW = "get_data_preview";
   private static final String GET_OUTPUT_PREVIEW = "get_output_preview";

   private static final String PREVIEW_SQL = "preview_sql";

   private static final String EDIT_COMPLETED = "edit_completed";
   private static final String CHOOSE_FILE_COMPLETED = "choose_file_completed";
   private static final String OPEN_FILE_DIALOG_COMPLETED = "open_file_dialog_completed";

   private static final String GET_PACKAGE_STATE = "get_package_state";
   private static final String AVAILABLE_PACKAGES = "available_packages";
   private static final String CHECK_FOR_PACKAGE_UPDATES = "check_for_package_updates";
   private static final String INIT_DEFAULT_USER_LIBRARY = "init_default_user_library";
   private static final String LOADED_PACKAGE_UPDATES_REQUIRED = "loaded_package_updates_required";
   private static final String IGNORE_NEXT_LOADED_PACKAGE_CHECK = "ignore_next_loaded_package_check";
   private static final String GET_PACKAGE_NEWS_URL = "get_package_news_url";
   private static final String GET_PACKAGE_INSTALL_CONTEXT = "get_package_install_context";
   private static final String IS_PACKAGE_LOADED = "is_package_loaded";
   private static final String IS_PACKAGE_INSTALLED = "is_package_installed";
   private static final String SET_CRAN_MIRROR = "set_cran_mirror";
   private static final String GET_CRAN_MIRRORS = "get_cran_mirrors";
   private static final String GET_CRAN_ACTIVES = "get_cran_actives";
   private static final String PACKAGE_SKELETON = "package_skeleton";
   private static final String DISCOVER_PACKAGE_DEPENDENCIES = "discover_package_dependencies";

   private static final String GET_HELP = "get_help";
   private static final String SHOW_HELP_TOPIC = "show_help_topic";
   private static final String SEARCH = "search";
   private static final String GET_CUSTOM_HELP = "get_custom_help";
   private static final String GET_CUSTOM_PARAMETER_HELP = "get_custom_parameter_help";
   private static final String SHOW_CUSTOM_HELP_TOPIC = "show_custom_help_topic";

   private static final String STAT = "stat";
   private static final String IS_TEXT_FILE = "is_text_file";
   private static final String IS_GIT_DIRECTORY = "is_git_directory";
   private static final String IS_PACKAGE_DIRECTORY = "is_package_directory";
   private static final String LIST_FILES = "list_files";
   private static final String LIST_ALL_FILES = "list_all_files";
   private static final String CREATE_FOLDER = "create_folder";
   private static final String DELETE_FILES = "delete_files";
   private static final String COPY_FILE = "copy_file";
   private static final String MOVE_FILES = "move_files";
   private static final String RENAME_FILE = "rename_file";
   private static final String COMPLETE_UPLOAD = "complete_upload";

   private static final String NEXT_PLOT = "next_plot";
   private static final String PREVIOUS_PLOT = "previous_plot";
   private static final String REMOVE_PLOT = "remove_plot";
   private static final String CLEAR_PLOTS = "clear_plots";
   private static final String REFRESH_PLOT = "refresh_plot";
   private static final String SAVE_PLOT_AS = "save_plot_as";
   private static final String SAVE_PLOT_AS_PDF = "save_plot_as_pdf";
   private static final String COPY_PLOT_TO_CLIPBOARD_METAFILE = "copy_plot_to_clipboard_metafile";
   private static final String COPY_PLOT_TO_COCOA_PASTEBOARD = "copy_plot_to_cocoa_pasteboard";
   private static final String GET_UNIQUE_SAVE_PLOT_STEM = "get_unique_save_plot_stem";
   private static final String GET_SAVE_PLOT_CONTEXT = "get_save_plot_context";
   private static final String LOCATOR_COMPLETED = "locator_completed";
   private static final String SET_MANIPULATOR_VALUES = "set_manipulator_values";
   private static final String MANIPULATOR_PLOT_CLICKED = "manipulator_plot_clicked";

   private static final String EXECUTE_R_CODE = "execute_r_code";

   private static final String GET_NEW_PROJECT_CONTEXT = "get_new_project_context";
   private static final String GET_NEW_SESSION_URL = "get_new_session_url";
   private static final String GET_ACTIVE_SESSIONS = "get_active_sessions";
   private static final String SET_SESSION_LABEL = "set_session_label";
   private static final String DELETE_SESSION_DIR = "delete_session_dir";
   private static final String GET_AVAILABLE_R_VERSIONS = "get_available_r_versions";
   private static final String CREATE_PROJECT = "create_project";
   private static final String CREATE_PROJECT_FILE = "create_project_file";
   private static final String GET_PROJECT_TEMPLATE_REGISTRY = "get_project_template_registry";
   private static final String EXECUTE_PROJECT_TEMPLATE = "execute_project_template";
   private static final String READ_PROJECT_OPTIONS = "read_project_options";
   private static final String WRITE_PROJECT_OPTIONS = "write_project_options";
   private static final String WRITE_PROJECT_CONFIG = "write_project_config";
   private static final String WRITE_PROJECT_VCS_OPTIONS = "write_project_vcs_options";

   private static final String NEW_DOCUMENT = "new_document";
   private static final String OPEN_DOCUMENT = "open_document";
   private static final String SAVE_DOCUMENT = "save_document";
   private static final String SAVE_DOCUMENT_DIFF = "save_document_diff";
   private static final String CHECK_FOR_EXTERNAL_EDIT = "check_for_external_edit";
   private static final String IGNORE_EXTERNAL_EDIT = "ignore_external_edit";
   private static final String CLOSE_DOCUMENT = "close_document";
   private static final String CLOSE_ALL_DOCUMENTS = "close_all_documents";
   private static final String GET_SOURCE_TEMPLATE = "get_source_template";
   private static final String CREATE_RD_SHELL = "create_rd_shell";
   private static final String SET_SOURCE_DOCUMENT_ON_SAVE = "set_source_document_on_save";
   private static final String SAVE_ACTIVE_DOCUMENT = "save_active_document";
   private static final String REQUEST_DOCUMENT_SAVE_COMPLETED = "request_document_save_completed";
   private static final String REQUEST_DOCUMENT_CLOSE_COMPLETED = "request_document_close_completed";
   private static final String MODIFY_DOCUMENT_PROPERTIES = "modify_document_properties";
   private static final String GET_DOCUMENT_PROPERTIES = "get_document_properties";
   private static final String REVERT_DOCUMENT = "revert_document";
   private static final String REOPEN_WITH_ENCODING = "reopen_with_encoding";
   private static final String REMOVE_CONTENT_URL = "remove_content_url";
   private static final String DETECT_FREE_VARS = "detect_free_vars";
   private static final String ICONVLIST = "iconvlist";
   private static final String GET_TEX_CAPABILITIES = "get_tex_capabilities";
   private static final String GET_CHUNK_OPTIONS = "get_chunk_options";
   private static final String SET_DOC_ORDER = "set_doc_order";
   private static final String REMOVE_CACHED_DATA = "remove_cached_data";
   private static final String ENSURE_FILE_EXISTS = "ensure_file_exists";
   private static final String GET_SOURCE_DOCUMENT = "get_source_document";

   private static final String EXPLORER_INSPECT_OBJECT = "explorer_inspect_object";
   private static final String EXPLORER_BEGIN_INSPECT = "explorer_begin_inspect";
   private static final String EXPLORER_END_INSPECT = "explorer_end_inspect";

   private static final String GET_EDITOR_CONTEXT_COMPLETED = "get_editor_context_completed";

   private static final String GET_RECENT_HISTORY = "get_recent_history";
   private static final String GET_HISTORY_ITEMS = "get_history_items";
   private static final String REMOVE_HISTORY_ITEMS = "remove_history_items";
   private static final String CLEAR_HISTORY = "clear_history";
   private static final String GET_HISTORY_ARCHIVE_ITEMS = "get_history_archive_items";
   private static final String SEARCH_HISTORY = "search_history";
   private static final String SEARCH_HISTORY_ARCHIVE = "search_history_archive";
   private static final String SEARCH_HISTORY_ARCHIVE_BY_PREFIX = "search_history_archive_by_prefix";

   private static final String VCS_CLONE = "vcs_clone";

   private static final String GIT_ADD = "git_add";
   private static final String GIT_REMOVE = "git_remove";
   private static final String GIT_DISCARD = "git_discard";
   private static final String GIT_REVERT = "git_revert";
   private static final String GIT_STAGE = "git_stage";
   private static final String GIT_UNSTAGE = "git_unstage";
   private static final String GIT_ALL_STATUS = "git_all_status";
   private static final String GIT_FULL_STATUS = "git_full_status";
   private static final String GIT_CREATE_BRANCH = "git_create_branch";
   private static final String GIT_LIST_BRANCHES = "git_list_branches";
   private static final String GIT_LIST_REMOTES = "git_list_remotes";
   private static final String GIT_ADD_REMOTE = "git_add_remote";
   private static final String GIT_CHECKOUT = "git_checkout";
   private static final String GIT_CHECKOUT_REMOTE = "git_checkout_remote";
   private static final String GIT_COMMIT = "git_commit";
   private static final String GIT_PUSH = "git_push";
   private static final String GIT_PUSH_BRANCH = "git_push_branch";
   private static final String GIT_PULL = "git_pull";
   private static final String GIT_PULL_REBASE = "git_pull_rebase";
   private static final String ASKPASS_COMPLETED = "askpass_completed";
   private static final String CREATE_SSH_KEY = "create_ssh_key";
   private static final String GIT_SSH_PUBLIC_KEY = "git_ssh_public_key";
   private static final String GIT_HAS_REPO = "git_has_repo";
   private static final String GIT_INIT_REPO = "git_init_repo";
   private static final String GIT_GET_IGNORES = "git_get_ignores";
   private static final String GIT_SET_IGNORES = "git_set_ignores";
   private static final String GIT_GITHUB_REMOTE_URL = "git_github_remote_url";
   private static final String GIT_DIFF_FILE = "git_diff_file";
   private static final String GIT_APPLY_PATCH = "git_apply_patch";
   private static final String GIT_HISTORY_COUNT = "git_history_count";
   private static final String GIT_HISTORY = "git_history";
   private static final String GIT_SHOW = "git_show";
   private static final String GIT_SHOW_FILE = "git_show_file";
   private static final String GIT_EXPORT_FILE = "git_export_file";

   private static final String SVN_ADD = "svn_add";
   private static final String SVN_DELETE = "svn_delete";
   private static final String SVN_REVERT = "svn_revert";
   private static final String SVN_RESOLVE = "svn_resolve";
   private static final String SVN_STATUS = "svn_status";
   private static final String SVN_UPDATE = "svn_update";
   private static final String SVN_CLEANUP = "svn_cleanup";
   private static final String SVN_COMMIT = "svn_commit";
   private static final String SVN_DIFF_FILE = "svn_diff_file";
   private static final String SVN_APPLY_PATCH = "svn_apply_patch";
   private static final String SVN_HISTORY_COUNT = "svn_history_count";
   private static final String SVN_HISTORY = "svn_history";
   private static final String SVN_SHOW = "svn_show";
   private static final String SVN_SHOW_FILE = "svn_show_file";
   private static final String SVN_GET_IGNORES = "svn_get_ignores";
   private static final String SVN_SET_IGNORES = "svn_set_ignores";

   private static final String GET_PUBLIC_KEY = "get_public_key";

   private static final String LIST_GET = "list_get";
   private static final String LIST_SET_CONTENTS = "list_set_contents";
   private static final String LIST_PREPEND_ITEM = "list_prepend_item";
   private static final String LIST_APPEND_ITEM = "list_append_item";
   private static final String LIST_REMOVE_ITEM = "list_remove_item";
   private static final String LIST_CLEAR = "list_clear";

   private static final String PREVIEW_HTML = "preview_html";
   private static final String TERMINATE_PREVIEW_HTML = "terminate_preview_html";
   private static final String GET_HTML_CAPABILITIES = "get_html_capabilities";
   private static final String RPUBS_UPLOAD = "rpubs_upload";
   private static final String RPUBS_TERMINATE_UPLOAD = "terminate_rpubs_upload";

   private static final String SET_WORKING_DIRECTORY = "set_working_directory";
   private static final String CREATE_STANDALONE_PRESENTATION = "create_standalone_presentation";
   private static final String CREATE_DESKTOP_VIEW_IN_BROWSER_PRESENTATION = "create_desktop_view_in_browser_presentation";
   private static final String CREATE_PRESENTATION_RPUBS_SOURCE = "create_presentation_rpubs_source";
   private static final String SET_PRESENTATION_SLIDE_INDEX = "set_presentation_slide_index";
   private static final String PRESENTATION_EXECUTE_CODE = "presentation_execute_code";
   private static final String CREATE_NEW_PRESENTATION = "create_new_presentation";
   private static final String SHOW_PRESENTATION_PANE = "show_presentation_pane";
   private static final String CLOSE_PRESENTATION_PANE = "close_presentation_pane";

   private static final String TUTORIAL_QUIZ_RESPONSE = "tutorial_quiz_response";

   private static final String TUTORIAL_STARTED = "tutorial_started";
   private static final String TUTORIAL_STOP = "tutorial_stop";
   private static final String TUTORIAL_METADATA = "tutorial_metadata";

   private static final String GET_SLIDE_NAVIGATION_FOR_FILE = "get_slide_navigation_for_file";
   private static final String GET_SLIDE_NAVIGATION_FOR_CODE = "get_slide_navigation_for_code";
   private static final String CLEAR_PRESENTATION_CACHE = "clear_presentation_cache";

   private static final String COMPILE_PDF = "compile_pdf";
   private static final String IS_COMPILE_PDF_RUNNING = "is_compile_pdf_running";
   private static final String TERMINATE_COMPILE_PDF = "terminate_compile_pdf";
   private static final String COMPILE_PDF_CLOSED = "compile_pdf_closed";

   private static final String SYNCTEX_FORWARD_SEARCH = "synctex_forward_search";
   private static final String SYNCTEX_INVERSE_SEARCH = "synctex_inverse_search";
   private static final String APPLY_FORWARD_CONCORDANCE = "apply_forward_concordance";
   private static final String APPLY_INVERSE_CONCORDANCE = "apply_inverse_concordance";

   private static final String CHECK_SPELLING = "check_spelling";
   private static final String SUGGESTION_LIST = "suggestion_list";
   private static final String ADD_CUSTOM_DICTIONARY = "add_custom_dictionary";
   private static final String REMOVE_CUSTOM_DICTIONARY = "remove_custom_dictionary";
   private static final String INSTALL_ALL_DICTIONARIES = "install_all_dictionaries";

   private static final String BEGIN_FIND = "begin_find";
   private static final String STOP_FIND = "stop_find";

   private static final String PREVIEW_REPLACE = "preview_replace";
   private static final String COMPLETE_REPLACE = "complete_replace";
   private static final String STOP_REPLACE = "stop_replace";

   private static final String GET_CPP_COMPLETIONS = "get_cpp_completions";
   private static final String GET_CPP_DIAGNOSTICS = "get_cpp_diagnostics";

   private static final String MARKDOWN_GET_COMPLETIONS = "markdown_get_completions";

   private static final String PYTHON_ACTIVE_INTERPRETER = "python_active_interpreter";
   private static final String PYTHON_FIND_INTERPRETERS = "python_find_interpreters";
   private static final String PYTHON_INTERPRETER_INFO = "python_interpreter_info";
   private static final String PYTHON_GET_COMPLETIONS = "python_get_completions";
   private static final String PYTHON_GO_TO_DEFINITION = "python_go_to_definition";
   private static final String PYTHON_GO_TO_HELP = "python_go_to_help";

   private static final String STAN_GET_COMPLETIONS = "stan_get_completions";
   private static final String STAN_GET_ARGUMENTS = "stan_get_arguments";
   private static final String STAN_RUN_DIAGNOSTICS = "stan_run_diagnostics";

   private static final String SQL_GET_COMPLETIONS = "sql_get_completions";

   private static final String GET_CPP_CAPABILITIES = "get_cpp_capabilities";
   private static final String INSTALL_BUILD_TOOLS = "install_build_tools";
   private static final String START_BUILD = "start_build";
   private static final String TERMINATE_BUILD = "terminate_build";
   private static final String DEVTOOLS_LOAD_ALL_PATH = "devtools_load_all_path";

   private static final String LIST_ENVIRONMENT = "list_environment";
   private static final String SET_CONTEXT_DEPTH = "set_context_depth";
   private static final String SET_ENVIRONMENT = "set_environment";
   private static final String SET_ENVIRONMENT_FRAME = "set_environment_frame";
   private static final String GET_ENVIRONMENT_NAMES = "get_environment_names";
   private static final String GET_ENVIRONMENT_STATE = "get_environment_state";
   private static final String GET_OBJECT_CONTENTS = "get_object_contents";
   private static final String REQUERY_CONTEXT = "requery_context";
   private static final String ENVIRONMENT_SET_LANGUAGE = "environment_set_language";
   private static final String SET_ENVIRONMENT_MONITORING = "set_environment_monitoring";
   private static final String IS_FUNCTION_MASKED = "is_function_masked";

   private static final String GET_FUNCTION_STEPS = "get_function_steps";
   private static final String SET_FUNCTION_BREAKPOINTS = "set_function_breakpoints";
   private static final String GET_FUNCTION_STATE = "get_function_state";
   private static final String EXECUTE_DEBUG_SOURCE = "execute_debug_source";
   private static final String SET_ERROR_MANAGEMENT_TYPE = "set_error_management_type";
   private static final String UPDATE_BREAKPOINTS = "update_breakpoints";
   private static final String REMOVE_ALL_BREAKPOINTS = "remove_all_breakpoints";

   private static final String LOG = "log";
   private static final String LOG_EXCEPTION = "log_exception";

   private static final String GET_INIT_MESSAGES = "get_init_messages";

   private static final String CHECK_FOR_UPDATES = "check_for_updates";
   private static final String GET_PRODUCT_INFO = "get_product_info";
   private static final String GET_PRODUCT_NOTICE = "get_product_notice";

   private static final String GET_R_ADDINS = "get_r_addins";
   private static final String PREPARE_FOR_ADDIN = "prepare_for_addin";
   private static final String EXECUTE_R_ADDIN = "execute_r_addin";

   private static final String CREATE_SHINY_APP = "create_shiny_app";
   private static final String GET_SHINY_VIEWER_TYPE = "get_shiny_viewer_type";
   private static final String GET_SHINY_RUN_CMD = "get_shiny_run_cmd";
   private static final String SET_SHINY_VIEWER_TYPE = "set_shiny_viewer_type";

   private static final String CREATE_PLUMBER_API = "create_plumber_api";
   private static final String GET_PLUMBER_VIEWER_TYPE = "get_plumber_viewer_type";
   private static final String GET_PLUMBER_RUN_CMD = "get_plumber_run_cmd";

   private static final String GET_RSCONNECT_ACCOUNT_LIST = "get_rsconnect_account_list";
   private static final String REMOVE_RSCONNECT_ACCOUNT = "remove_rsconnect_account";
   private static final String CONNECT_RSCONNECT_ACCOUNT = "connect_rsconnect_account";
   private static final String GET_RSCONNECT_APP_LIST = "get_rsconnect_app_list";
   private static final String GET_RSCONNECT_APP = "get_rsconnect_app";
   private static final String GET_RSCONNECT_DEPLOYMENTS = "get_rsconnect_deployments";
   private static final String FORGET_RSCONNECT_DEPLOYMENTS = "forget_rsconnect_deployments";
   private static final String RSCONNECT_PUBLISH = "rsconnect_publish";
   private static final String CANCEL_PUBLISH = "cancel_publish";
   private static final String GET_DEPLOYMENT_FILES = "get_deployment_files";
   private static final String VALIDATE_SERVER_URL = "validate_server_url";
   private static final String GET_SERVER_URLS = "get_server_urls";
   private static final String GET_AUTH_TOKEN = "get_auth_token";
   private static final String GET_USER_FROM_TOKEN = "get_user_from_token";
   private static final String REGISTER_USER_TOKEN = "register_user_token";
   private static final String GET_RSCONNECT_LINT_RESULTS = "get_rsconnect_lint_results";
   private static final String GET_RMD_PUBLISH_DETAILS = "get_rmd_publish_details";
   private static final String HAS_ORPHANED_ACCOUNTS = "has_orphaned_accounts";

   private static final String RENDER_RMD = "render_rmd";
   private static final String RENDER_RMD_SOURCE = "render_rmd_source";
   private static final String TERMINATE_RENDER_RMD = "terminate_render_rmd";
   private static final String CONVERT_TO_YAML = "convert_to_yaml";
   private static final String CONVERT_FROM_YAML = "convert_from_yaml";
   private static final String CREATE_RMD_FROM_TEMPLATE = "create_rmd_from_template";
   private static final String GET_RMD_TEMPLATE = "get_rmd_template";
   private static final String GET_RMD_TEMPLATES = "get_rmd_templates";
   private static final String GET_RMD_OUTPUT_INFO = "get_rmd_output_info";
   private static final String RMD_IMPORT_IMAGES = "rmd_import_images";

   private static final String GET_PACKRAT_PREREQUISITES = "get_packrat_prerequisites";
   private static final String INSTALL_PACKRAT = "install_packrat";
   private static final String GET_PACKRAT_CONTEXT = "get_packrat_context";
   private static final String GET_PACKRAT_STATUS = "get_packrat_status";
   private static final String PACKRAT_BOOTSTRAP = "packrat_bootstrap";
   private static final String GET_PENDING_ACTIONS = "get_pending_actions";
   private static final String GET_PACKRAT_ACTIONS = "get_packrat_actions";

   private static final String RENV_INIT = "renv_init";
   private static final String RENV_ACTIONS = "renv_actions";

   private static final String LINT_R_SOURCE_DOCUMENT = "lint_r_source_document";
   private static final String ANALYZE_PROJECT = "analyze_project";

   private static final String GET_SET_CLASS_CALL = "get_set_class_slots";
   private static final String GET_SET_GENERIC_CALL = "get_set_generic_call";
   private static final String GET_SET_METHOD_CALL = "get_set_method_call";
   private static final String GET_SET_REF_CLASS_CALL = "get_set_ref_class_call";
   private static final String TRANSFORM_SNIPPET = "transform_snippet";
   private static final String GET_SNIPPETS = "get_snippets";

   private static final String PREVIEW_DATA_IMPORT = "preview_data_import";
   private static final String ASSEMBLE_DATA_IMPORT = "assemble_data_import";
   private static final String PREVIEW_DATA_IMPORT_ASYNC = "preview_data_import_async";
   private static final String PREVIEW_DATA_IMPORT_ASYNC_ABORT = "preview_data_import_async_abort";
   private static final String PREVIEW_DATA_IMPORT_CLEAN = "preview_data_import_clean";

   private static final String START_PROFILING = "start_profiling";
   private static final String STOP_PROFILING = "stop_profiling";
   private static final String OPEN_PROFILE = "open_profile";
   private static final String COPY_PROFILE = "copy_profile";
   private static final String CLEAR_PROFILE = "clear_profile";
   private static final String PROFILE_SOURCES = "profile_sources";

   private static final String REMOVE_CONNECTION = "remove_connection";
   private static final String CONNECTION_DISCONNECT = "connection_disconnect";
   private static final String CONNECTION_EXECUTE_ACTION = "connection_execute_action";
   private static final String CONNECTION_LIST_OBJECTS = "connection_list_objects";
   private static final String CONNECTION_LIST_FIELDS = "connection_list_fields";
   private static final String CONNECTION_PREVIEW_OBJECT = "connection_preview_object";
   private static final String CONNECTION_TEST = "connection_test";
   private static final String GET_NEW_CONNECTION_CONTEXT = "get_new_connection_context";
   private static final String INSTALL_SPARK = "install_spark";

   private static final String SQL_CHUNK_DEFAULT_CONNECTION = "default_sql_connection_name";

   private static final String LAUNCH_EMBEDDED_SHINY_CONNECTION_UI = "launch_embedded_shiny_connection_ui";
   private static final String RSTUDIOAPI_SHOW_DIALOG_COMPLETED = "rstudioapi_show_dialog_completed";

   private static final String STOP_SHINY_APP = "stop_shiny_app";

   private static final String CONNECTION_ADD_PACKAGE = "connection_add_package";

   private static final String ASKSECRET_COMPLETED = "asksecret_completed";
   private static final String INSTALL_ODBC_DRIVER = "install_odbc_driver";
   private static final String GET_NEW_ODBC_CONNECTION_CONTEXT = "get_new_odbc_connection_context";
   private static final String UNINSTALL_ODBC_DRIVER = "uninstall_odbc_driver";
   private static final String UPDATE_ODBC_INSTALLERS = "update_odbc_installers";

   private static final String HAS_SHINYTEST_HAS_DEPENDENCIES = "has_shinytest_dependencies";
   private static final String INSTALL_SHINYTEST_DEPENDENCIES = "install_shinytest_dependencies";
   private static final String HAS_SHINYTEST_RESULTS = "has_shinytest_results";

   private static final String GET_SECONDARY_REPOS = "get_secondary_repos";
   private static final String VALIDATE_CRAN_REPO = "validate_cran_repo";

   private static final String GET_THEMES = "get_themes";
   private static final String ADD_THEME = "add_theme";
   private static final String REMOVE_THEME = "remove_theme";
   private static final String GET_THEME_NAME = "get_theme_name";
   private static final String SET_COMPUTED_THEME_COLORS = "set_computed_theme_colors";

   private static final String REPLACE_COMMENT_HEADER = "replace_comment_header";
   private static final String SET_USER_CRASH_HANDLER_PROMPTED = "set_user_crash_handler_prompted";

   private static final String PANDOC_GET_CAPABILITIES = "pandoc_get_capabilities";
   private static final String PANDOC_AST_TO_MARKDOWN = "pandoc_ast_to_markdown";
   private static final String PANDOC_MARKDOWN_TO_AST = "pandoc_markdown_to_ast";
   private static final String PANDOC_LIST_EXTENSIONS = "pandoc_list_extensions";
   private static final String PANDOC_GET_BIBLIOGRAPHY = "pandoc_get_bibliography";
   private static final String PANDOC_ADD_TO_BIBLIOGRAPHY = "pandoc_add_to_bibliography";
   private static final String PANDOC_CITATION_HTML = "pandoc_citation_html";

   private static final String CROSSREF_WORKS = "crossref_works";
   
   private static final String PUBMED_SEARCH = "pubmed_search";
   
   private static final String DATACITE_SEARCH = "datacite_search";
  
   private static final String ZOTERO_GET_COLLECTIONS = "zotero_get_collections";
   private static final String ZOTERO_GET_LIBRARY_NAMES = "zotero_get_library_names";
   private static final String ZOTERO_GET_ACTIVE_COLLECTIONSPECS = "zotero_get_active_collection_specs";
   private static final String ZOTERO_VALIDATE_WEB_API_KEY = "zotero_validate_web_api_key";
   private static final String ZOTERO_DETECT_LOCAL_CONFIG = "zotero_detect_local_config";
   private static final String ZOTERO_BETTER_BIBTEX_EXPORT = "zotero_better_bibtex_export";

   private static final String DOI_FETCH_CSL = "doi_fetch_csl";

   private static final String XREF_INDEX_FOR_FILE = "xref_index_for_file";
   private static final String XREF_FOR_ID = "xref_for_id";
  
}
