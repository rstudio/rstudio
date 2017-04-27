/*
 * RemoteServer.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.ClientDisconnectedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InvalidClientVersionEvent;
import org.rstudio.studio.client.application.events.InvalidSessionEvent;
import org.rstudio.studio.client.application.events.ServerOfflineEvent;
import org.rstudio.studio.client.application.events.UnauthorizedEvent;
import org.rstudio.studio.client.application.model.ActiveSession;
import org.rstudio.studio.client.application.model.InvalidSessionInfo;
import org.rstudio.studio.client.application.model.ProductInfo;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.SuspendOptions;
import org.rstudio.studio.client.application.model.UpdateCheckResult;
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
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectContext;
import org.rstudio.studio.client.projects.model.NewShinyAppOptions;
import org.rstudio.studio.client.projects.model.ProjectTemplateOptions;
import org.rstudio.studio.client.projects.model.ProjectTemplateRegistry;
import org.rstudio.studio.client.projects.model.ProjectUser;
import org.rstudio.studio.client.projects.model.ProjectUserRole;
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
import org.rstudio.studio.client.server.Int;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.shiny.model.ShinyRunCmd;
import org.rstudio.studio.client.shiny.model.ShinyViewerType;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchResults;
import org.rstudio.studio.client.workbench.codesearch.model.ObjectDefinition;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.model.Agreement;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.TerminalOptions;
import org.rstudio.studio.client.workbench.model.TexCapabilities;
import org.rstudio.studio.client.workbench.model.WorkbenchMetrics;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;
import org.rstudio.studio.client.workbench.snippets.model.SnippetData;
import org.rstudio.studio.client.workbench.views.buildtools.model.BookdownFormats;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionObjectSpecifier;
import org.rstudio.studio.client.workbench.views.connections.model.DatabaseObject;
import org.rstudio.studio.client.workbench.views.connections.model.Field;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.console.model.ProcessBufferChunk;
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
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageState;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;
import org.rstudio.studio.client.workbench.views.plots.model.Point;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationRPubsSource;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerInspectionResult;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationRequest;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationResponse;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
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
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
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
      if (session_.getSessionInfo().getMode().equals(SessionInfo.SERVER_MODE))
         serverAuth_.schedulePeriodicCredentialsUpdate();
      
      // start event listener
      serverEventListener_.start();
      
      // register satallite callback
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

   public void clientInit(
                     final ServerRequestCallback<SessionInfo> requestCallback)
   {      
      // send init request (record clientId and version contained in response)
      sendRequest(RPC_SCOPE, 
                  CLIENT_INIT, 
                  new ServerRequestCallback<SessionInfo>() {

         public void onResponseReceived(SessionInfo sessionInfo)
         {
            clientId_ = sessionInfo.getClientId();
            clientVersion_ = sessionInfo.getClientVersion();
            requestCallback.onResponseReceived(sessionInfo);
         }
   
         public void onError(ServerError error)
         {
            requestCallback.onError(error);
         }
      });
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
   
   // accept application agreement
   public void acceptAgreement(Agreement agreement, 
                               ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, 
                  ACCEPT_AGREEMENT, 
                  agreement.getHash(),
                  requestCallback);
   }
   
   
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
      sendRequest(RPC_SCOPE, PING, requestCallback);
   }
  
   
   public void setWorkbenchMetrics(WorkbenchMetrics metrics,
                                   ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, 
                  SET_WORKBENCH_METRICS, 
                  metrics, 
                  requestCallback);
   }

   public void setPrefs(RPrefs rPrefs,
                        JavaScriptObject uiPrefs,
                        ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(rPrefs));
      params.set(1, new JSONObject(uiPrefs));
      sendRequest(RPC_SCOPE, SET_PREFS, params, requestCallback);
}
   
   public void setUiPrefs(JavaScriptObject uiPrefs,
                          ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  SET_UI_PREFS,
                  uiPrefs,
                  requestCallback);
   }

   public void getRPrefs(ServerRequestCallback<RPrefs> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_R_PREFS,
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
      sendRequest(RPC_SCOPE,
                  SET_CLIENT_STATE,
                  params,
                  requestCallback);
   }
   
   public void userPromptCompleted(int response, 
                                  ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, USER_PROMPT_COMPLETED, response, requestCallback);
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
   public void startTerminal(
                     int shellType,
                     int cols, int rows,
                     String terminalHandle,
                     String caption,
                     String title,
                     boolean websocket,
                     int sequence,
                     ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(shellType));
      params.set(1, new JSONNumber(cols));
      params.set(2, new JSONNumber(rows));
      params.set(3, new JSONString(StringUtil.notNull(terminalHandle)));
      params.set(4, new JSONString(StringUtil.notNull(caption)));
      params.set(5, new JSONString(StringUtil.notNull(title)));
      params.set(6, JSONBoolean.getInstance(websocket));
      params.set(7, new JSONNumber(sequence));

      sendRequest(RPC_SCOPE,
                  START_TERMINAL,
                  params,
                  new ConsoleProcessCallbackAdapter(requestCallback));
   }
   
   @Override
   public void executeCode(String code,
                           ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0,  new JSONString(code));
      sendRequest(RPC_SCOPE, "execute_code", params, requestCallback);
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
                                 ServerRequestCallback<Void> requestCallback)
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
                                  ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
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
   public void processUseRpc(String handle,
                             ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(handle)));
      sendRequest(RPC_SCOPE, PROCESS_USE_RPC, params, requestCallback);   
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
      
      sendRequest(RPC_SCOPE,
                  GET_COMPLETIONS,
                  params,
                  requestCallback);
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
                  requestCallback) ;
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
                             ServerRequestCallback<HelpInfo.Custom> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(helpHandler));
      params.set(1, new JSONString(topic));
      params.set(2, new JSONString(source));
      sendRequest(RPC_SCOPE, GET_CUSTOM_HELP, params, requestCallback);
   }
   
   public void getCustomParameterHelp(String helpHandler,
                                      String source,
                                      ServerRequestCallback<HelpInfo.Custom> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(helpHandler));
      params.set(1, new JSONString(source));
      sendRequest(RPC_SCOPE, GET_CUSTOM_PARAMETER_HELP, params, requestCallback);
   }

   
   public void showHelpTopic(String what, String from, int type)
   {
      JSONArray params = new JSONArray() ;
      params.set(0, new JSONString(what)) ;
      params.set(1, from != null 
                       ? new JSONString(from)
                       : JSONNull.getInstance()) ;
      params.set(2, new JSONNumber(type));
      
      sendRequest(RPC_SCOPE,
                  SHOW_HELP_TOPIC,
                  params,
                  null) ;
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
                  requestCallback) ;
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
                  ServerRequestCallback<DirectoryListing> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      paramArray.set(0, new JSONString(directory.getPath()));
      paramArray.set(1, JSONBoolean.getInstance(monitor));
      
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
                        boolean ovewrite,
                        ServerRequestCallback<Void> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      paramArray.set(0, new JSONString(sourceFile.getPath()));
      paramArray.set(1, new JSONString(targetFile.getPath()));
      paramArray.set(2, JSONBoolean.getInstance(ovewrite));
      
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

   public String getFileUrl(FileSystemItem file)
   {
      if (Desktop.isDesktop())
      {
         return Desktop.getFrame().getUriForPath(file.getPath());
      }
      
      if (!file.isDirectory())
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
      return getApplicationURL(UPLOAD_SCOPE);
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
   
   public void writeJSON(String path,
                         JavaScriptObject object,
                         ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONObject(object));
      sendRequest(RPC_SCOPE, "write_json", params, requestCallback);
   }
   
   public void readJSON(String path,
                        boolean logErrorIfNotFound,
                        ServerRequestCallback<JavaScriptObject> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, JSONBoolean.getInstance(logErrorIfNotFound));
      sendRequest(RPC_SCOPE, "read_json", params, requestCallback);
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
                                ServerRequestCallback<String> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(hostPageUrl));
      params.set(1, JSONBoolean.getInstance(isProject));
      params.set(2, new JSONString(directory));
      params.set(3, rVersion != null ? new JSONObject(rVersion) :
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
   public void executeRCode(String code,
                            ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0,  new JSONString(code));
      sendRequest(RPC_SCOPE, EXECUTE_R_CODE, params, requestCallback);
   }
   
   public void createProject(String projectFile,
                             NewPackageOptions newPackageOptions,
                             NewShinyAppOptions newShinyAppOptions,
                             ProjectTemplateOptions projectTemplateOptions,
                             ServerRequestCallback<Void> requestCallback)
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
   public void createProjectFile(String projectFile,
                                 ServerRequestCallback<Boolean> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(StringUtil.notNull(projectFile)));
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
                            ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, path == null ? JSONNull.getInstance() : new JSONString(path));
      params.set(2, fileType == null ? JSONNull.getInstance() : new JSONString(fileType));
      params.set(3, encoding == null ? JSONNull.getInstance() : new JSONString(encoding));
      params.set(4, new JSONString(StringUtil.notNull(foldSpec)));
      params.set(5, chunkDefs == null ? JSONNull.getInstance() : new JSONObject(chunkDefs));
      params.set(6, new JSONString(contents));
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
                                String hash,
                                ServerRequestCallback<String> requestCallback)
   {
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
      params.set(9, new JSONString(hash));
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
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(contents));
      params.set(1, JSONBoolean.getInstance(sweave));
      params.set(2, new JSONString(rnwWeave));

      sendRequest(RPC_SCOPE,
                  SAVE_ACTIVE_DOCUMENT,
                  params,
                  requestCallback);
   }

   public void modifyDocumentProperties(
         String id,
         HashMap<String, String> properties,
         ServerRequestCallback<Void> requestCallback)
   {
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
                           ServerRequestCallback<DiffResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONNumber(mode.getValue()));
      params.set(2, new JSONNumber(contextLines));
      params.set(3, JSONBoolean.getInstance(noSizeWarning));
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
         Debug.log("Satellite window shoudl not call getEvents!");
         assert false;
      }
      
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(lastEventId));
      return sendRequest(EVENTS_SCOPE,
                         "get_events",
                         params,
                         false,
                         requestCallback,
                         retryHandler);
   }

   void handleUnauthorizedError()
   {
      // disconnect
      disconnect();

      // fire event
      UnauthorizedEvent event = new UnauthorizedEvent();
      eventBus_.fireEvent(event);
   }

   private <T> void sendRequest(String scope,
                                String method,
                                ServerRequestCallback<T> requestCallback)
   {
      sendRequest(scope, method, new JSONArray(), requestCallback);
   }

   private <T> void sendRequest(String scope,
                                String method,
                                boolean param,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(param));
      sendRequest(scope, method, params, requestCallback);
   }

   private <T> void sendRequest(String scope,
                                String method,
                                long param,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(param));
      sendRequest(scope, method, params, requestCallback);
   }

   private <T> void sendRequest(String scope,
                                String method,
                                String param,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();

      // pass JSONNull if the string is null
      params.set(0, param != null ?
                     new JSONString(param) :
                     JSONNull.getInstance());

      sendRequest(scope, method, params, requestCallback);
   }

   private <T> void sendRequest(String scope,
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


      sendRequest(scope, method, params, requestCallback);
   }


   private <T> void sendRequest(String scope,
                                String method,
                                JavaScriptObject param,
                                ServerRequestCallback<T> requestCallback)
   {
      JSONArray params = new JSONArray();

      // pass JSONNull if the object is null
      params.set(0, param != null ? new JSONObject(param) :
                                    JSONNull.getInstance());

      sendRequest(scope, method, params, requestCallback);
   }


   private <T> void sendRequest(final String scope,
                                final String method,
                                final JSONArray params,
                                final ServerRequestCallback<T> requestCallback)
   {
      sendRequest(scope, method, params, false, requestCallback);
   }

   private <T> void sendRequest(final String scope,
                                final String method,
                                final JSONArray params,
                                final boolean redactLog,
                                final ServerRequestCallback<T> cb)
   {
      // if this is a satellite window then we handle this by proxying
      // back through the main workbench window
      if (Satellite.isCurrentWindowSatellite())
      {
         sendRequestViaMainWorkbench(scope, method, params, redactLog, cb);

      }
      // otherwise just a standard request with single retry
      else
      {
         sendRequestWithRetry(scope, method, params, redactLog, cb); 
      }
      
   }
   
   private <T> void sendRequestWithRetry(
                                 final String scope,
                                 final String method,
                                 final JSONArray params,
                                 final boolean redactLog,
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
                        redactLog, 
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
                  redactLog, 
                  requestCallback, 
                  retryHandler);
   }

   // sendRequest method called for internal calls from main workbench
   // (as opposed to proxied calls from satellites)
   private <T> RpcRequest sendRequest(
                              String scope,
                              String method,
                              JSONArray params,
                              boolean redactLog,
                              final ServerRequestCallback<T> requestCallback,
                              RetryHandler retryHandler)
   { 
      return sendRequest(
            null,
            scope,
            method,
            params,
            redactLog,
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
                     T result = response.<T> getResult();
                     requestCallback.onResponseReceived(result);
                  }
               }
             },
             retryHandler);

   }
      
   // lowest level sendRequest method -- called from the main workbench
   // in two scenarios: direct internal call and servicing a proxied
   // request from a satellite window
   private RpcRequest sendRequest(String sourceWindow,
                                  String scope, 
                                  String method, 
                                  JSONArray params,
                                  boolean redactLog,
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
                                             null,
                                             redactLog,
                                             sourceWindow,
                                             clientId_,
                                             clientVersion_);
      
      if (isDisconnected())
         return rpcRequest;

      // send the request
      rpcRequest.send(new RpcRequestCallback() {
         public void onError(RpcRequest request, RpcError error)
         {
            // ignore errors if we are disconnected
            if (isDisconnected())           
               return;
            
            // if we have a retry handler then see if we can resolve the
            // error and then retry
            if ( resolveRpcErrorAndRetry(error, retryHandler) )
               return ;

            // first crack goes to globally registered rpc error handlers
            if (!handleRpcErrorInternally(error))
            {
               // no global handlers processed it, send on to caller
               responseHandler.onResponseReceived(RpcResponse.create(error));
            }
         }

         public void onResponseReceived(final RpcRequest request,
                                        RpcResponse response)
         {
            // ignore response if we are disconnected
            //   - handler was cancelled
            if (isDisconnected()) 
                 return;
                   
            // check for error
            if (response.getError() != null)
            {
               // ERROR: explicit error returned by server
               RpcError error = response.getError();

               // if we have a retry handler then see if we can resolve the
               // error and then retry
               if ( resolveRpcErrorAndRetry(error, retryHandler) )
                  return ;

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
         return true ; // default to true for json-rpc compactness
      else
         return Boolean.parseBoolean(eventsPending);
   }

   private boolean resolveRpcErrorAndRetry(final RpcError error,
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
               if (response.intValue() ==
                                 RemoteServerAuth.CREDENTIALS_UPDATE_SUCCESS)
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
      else
      {
         return false;
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

   
   // the following sequence of calls enables marsahlling of remote server
   // requests from satellite windows back into the main workbench window
   
   // this code sets up the sendRemoteServerRequest global callback within
   // the main workbench
   private native void registerSatelliteCallback() /*-{
      var server = this;     
      $wnd.sendRemoteServerRequest = $entry(
         function(sourceWindow, scope, method, params, redactLog, responseCallback) {
            server.@org.rstudio.studio.client.server.remote.RemoteServer::sendRemoteServerRequest(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;ZLcom/google/gwt/core/client/JavaScriptObject;)(sourceWindow, scope, method, params, redactLog, responseCallback);
         }
      ); 
   }-*/;
   
   // this code runs in the main workbench and implements the server request
   // and then calls back the satellite on the provided js responseCallback
   private void sendRemoteServerRequest(final JavaScriptObject sourceWindow,
                                        final String scope,
                                        final String method,
                                        final JavaScriptObject params,
                                        final boolean redactLog,
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
      };
      final ResponseHandler responseHandler = new ResponseHandler();
      
      // setup a retry handler which will call back the second time with
      // the same args (but no retryHandler, ensurin at most 1 retry)
      RetryHandler retryHandler = new RetryHandler() {
        
         public void onRetry()
         {
            // retry one time (passing null as last param ensures there
            // is no retry handler installed)
            sendRequest(getSourceWindowName(sourceWindow),
                        scope, 
                        method, 
                        jsonParams, 
                        redactLog, 
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
      sendRequest(getSourceWindowName(sourceWindow),
                  scope, 
                  method, 
                  jsonParams, 
                  redactLog, 
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
                               boolean redactLog,
                               final ServerRequestCallback<T> requestCallback)
   {
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
            redactLog, 
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
                                    boolean redactLog,
                                    RpcResponseHandler handler) /*-{
      
      var responseCallback = new Object();
      responseCallback.onResponse = $entry(function(response) {
        handler.@org.rstudio.core.client.jsonrpc.RpcResponseHandler::onResponseReceived(Lorg/rstudio/core/client/jsonrpc/RpcResponse;)(response);
      });

      $wnd.opener.sendRemoteServerRequest($wnd,
                                          scope, 
                                          method, 
                                          params, 
                                          redactLog,
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
   public void tutorialFeedback(String feedback, 
                                ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, TUTORIAL_FEEDBACK, feedback, requestCallback);
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
                         JsArrayString filePatterns,
                         ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(searchString));
      params.set(1, JSONBoolean.getInstance(regex));
      params.set(2, JSONBoolean.getInstance(ignoreCase));
      params.set(3, new JSONString(directory == null ? ""
                                                     : directory.getPath()));
      params.set(4, new JSONArray(filePatterns));
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
         ServerRequestCallback<JsArray<EnvironmentFrame>> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_ENVIRONMENT_NAMES,
                  requestCallback);
   }
   
   @Override
   public void getEnvironmentState(
         ServerRequestCallback<EnvironmentContextData> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_ENVIRONMENT_STATE,
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
         int type,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(type));
      
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
   public void getRAddins(boolean reindex, 
                          ServerRequestCallback<RAddins> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_R_ADDINS, reindex, requestCallback);
   }
   
   @Override
   public void executeRAddinNonInteractively(String commandId, ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(commandId));
      sendRequest(RPC_SCOPE, EXECUTE_R_ADDIN, params, requestCallback);
   }

   @Override
   public void getShinyViewerType(ServerRequestCallback<ShinyViewerType> requestCallback)
   {
      sendRequest(RPC_SCOPE,
            GET_SHINY_VIEWER_TYPE,
            requestCallback);
   }

   @Override
   public void setShinyViewerType(int viewerType,
         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(viewerType));
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
   public void getRSConnectApp(String appId, String accountName, String server,
         ServerRequestCallback<RSConnectApplicationInfo> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(appId));
      params.set(1, new JSONString(accountName));
      params.set(2, new JSONString(server));
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
   public void publishContent(
         RSConnectPublishSource source, String account, 
         String server, String appName, String appTitle,
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
      sendRequest(RPC_SCOPE,
            RSCONNECT_PUBLISH,
            params,
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
         ServerRequestCallback<Int> requestCallback)
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
      JsArray<Dependency> dependencies,
      ServerRequestCallback<ConsoleProcess> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  "install_dependencies",
                  dependencies,
                  new ConsoleProcessCallbackAdapter(requestCallback));
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
      sendRequest(RPC_SCOPE, GET_NEW_SPARK_CONNECTION_CONTEXT, callback);
   }
   
   public void installSpark(String sparkVersion,
                            String hadoopVersion,
                            ServerRequestCallback<ConsoleProcess> callback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(sparkVersion));
      params.set(1, new JSONString(hadoopVersion));
      sendRequest(RPC_SCOPE,
                  INSTALL_SPARK,
                  params,
                  new ConsoleProcessCallbackAdapter(callback));
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
   public void stopShinyApp(ServerRequestCallback<Void> callback)
   {
      JSONArray params = new JSONArray();
      sendRequest(RPC_SCOPE, STOP_SHINY_APP, params, true, callback);
   }

   private String clientId_;
   private String clientVersion_ = "";
   private boolean listeningForEvents_;
   private boolean disconnected_;

   private final RemoteServerAuth serverAuth_;
   private final RemoteServerEventListener serverEventListener_ ;

   private final Provider<ConsoleProcessFactory> pConsoleProcessFactory_;

   private final Session session_;
   private final EventBus eventBus_;

   // url scopes
   private static final String RPC_SCOPE = "rpc";
   private static final String FILES_SCOPE = "files";
   private static final String EVENTS_SCOPE = "events";
   private static final String UPLOAD_SCOPE = "upload";
   private static final String EXPORT_SCOPE = "export";
   private static final String GRAPHICS_SCOPE = "graphics";
   private static final String SOURCE_SCOPE = "source";
   private static final String LOG_SCOPE = "log";
   private static final String META_SCOPE = "meta";
   private static final String FILE_SHOW = "file_show";

   // session methods
   private static final String CLIENT_INIT = "client_init";
   private static final String ACCEPT_AGREEMENT = "accept_agreement";
   private static final String SUSPEND_SESSION = "suspend_session";
   private static final String HANDLE_UNSAVED_CHANGES_COMPLETED = "handle_unsaved_changes_completed";
   private static final String QUIT_SESSION = "quit_session";
   private static final String SUSPEND_FOR_RESTART = "suspend_for_restart";
   private static final String PING="ping";

   private static final String SET_WORKBENCH_METRICS = "set_workbench_metrics";
   private static final String SET_PREFS = "set_prefs";
   private static final String SET_UI_PREFS = "set_ui_prefs";
   private static final String GET_R_PREFS = "get_r_prefs";
   private static final String SET_CLIENT_STATE = "set_client_state";
   private static final String USER_PROMPT_COMPLETED = "user_prompt_completed";
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
   private static final String PROCESS_USE_RPC = "process_use_rpc";

   private static final String REMOVE_ALL_OBJECTS = "remove_all_objects";
   private static final String REMOVE_OBJECTS = "remove_objects";
   private static final String DOWNLOAD_DATA_FILE = "download_data_file";
   private static final String GET_DATA_PREVIEW = "get_data_preview";
   private static final String GET_OUTPUT_PREVIEW = "get_output_preview";

   private static final String EDIT_COMPLETED = "edit_completed";
   private static final String CHOOSE_FILE_COMPLETED = "choose_file_completed";

   private static final String GET_PACKAGE_STATE = "get_package_state";
   private static final String AVAILABLE_PACKAGES = "available_packages";
   private static final String CHECK_FOR_PACKAGE_UPDATES = "check_for_package_updates";
   private static final String INIT_DEFAULT_USER_LIBRARY = "init_default_user_library";
   private static final String LOADED_PACKAGE_UPDATES_REQUIRED = "loaded_package_updates_required";
   private static final String IGNORE_NEXT_LOADED_PACKAGE_CHECK = "ignore_next_loaded_package_check";
   private static final String GET_PACKAGE_INSTALL_CONTEXT = "get_package_install_context";
   private static final String IS_PACKAGE_LOADED = "is_package_loaded";
   private static final String SET_CRAN_MIRROR = "set_cran_mirror";
   private static final String GET_CRAN_MIRRORS = "get_cran_mirrors";
   private static final String PACKAGE_SKELETON = "package_skeleton";

   private static final String GET_HELP = "get_help";
   private static final String SHOW_HELP_TOPIC = "show_help_topic" ;
   private static final String SEARCH = "search" ;
   private static final String GET_CUSTOM_HELP = "get_custom_help";
   private static final String GET_CUSTOM_PARAMETER_HELP = "get_custom_parameter_help";
   private static final String SHOW_CUSTOM_HELP_TOPIC = "show_custom_help_topic" ;

   private static final String STAT = "stat";
   private static final String IS_TEXT_FILE = "is_text_file";
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
   private static final String GET_AVAILABLE_R_VERSIONS = "get_available_r_versions";
   private static final String CREATE_PROJECT = "create_project";
   private static final String CREATE_PROJECT_FILE = "create_project_file";
   private static final String GET_PROJECT_TEMPLATE_REGISTRY = "get_project_template_registry";
   private static final String EXECUTE_PROJECT_TEMPLATE = "execute_project_template";
   private static final String READ_PROJECT_OPTIONS = "read_project_options";
   private static final String WRITE_PROJECT_OPTIONS = "write_project_options";
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
   
   private static final String GET_EDITOR_CONTEXT_COMPLETED = "get_editor_context_completed";

   private static final String GET_RECENT_HISTORY = "get_recent_history";
   private static final String GET_HISTORY_ITEMS = "get_history_items";
   private static final String REMOVE_HISTORY_ITEMS = "remove_history_items";
   private static final String CLEAR_HISTORY = "clear_history";
   private static final String GET_HISTORY_ARCHIVE_ITEMS = "get_history_archive_items";
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
   
   private static final String TUTORIAL_FEEDBACK = "tutorial_feedback";
   private static final String TUTORIAL_QUIZ_RESPONSE = "tutorial_quiz_response";
   
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
   
   private static final String GET_CPP_COMPLETIONS = "get_cpp_completions";
   private static final String GET_CPP_DIAGNOSTICS = "get_cpp_diagnostics";
   
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
   
   private static final String GET_R_ADDINS = "get_r_addins";
   private static final String EXECUTE_R_ADDIN = "execute_r_addin";

   private static final String CREATE_SHINY_APP = "create_shiny_app";
   private static final String GET_SHINY_VIEWER_TYPE = "get_shiny_viewer_type";
   private static final String GET_SHINY_RUN_CMD = "get_shiny_run_cmd";
   private static final String SET_SHINY_VIEWER_TYPE = "set_shiny_viewer_type";
   
   private static final String GET_RSCONNECT_ACCOUNT_LIST = "get_rsconnect_account_list";
   private static final String REMOVE_RSCONNECT_ACCOUNT = "remove_rsconnect_account";
   private static final String CONNECT_RSCONNECT_ACCOUNT = "connect_rsconnect_account";
   private static final String GET_RSCONNECT_APP_LIST = "get_rsconnect_app_list";
   private static final String GET_RSCONNECT_APP = "get_rsconnect_app";
   private static final String GET_RSCONNECT_DEPLOYMENTS = "get_rsconnect_deployments";
   private static final String RSCONNECT_PUBLISH = "rsconnect_publish";
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
   
   private static final String GET_PACKRAT_PREREQUISITES = "get_packrat_prerequisites";
   private static final String INSTALL_PACKRAT = "install_packrat";
   private static final String GET_PACKRAT_CONTEXT = "get_packrat_context";
   private static final String GET_PACKRAT_STATUS = "get_packrat_status";
   private static final String PACKRAT_BOOTSTRAP = "packrat_bootstrap";
   private static final String GET_PENDING_ACTIONS = "get_pending_actions";
   
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
   private static final String GET_NEW_SPARK_CONNECTION_CONTEXT = "get_new_connection_context";
   private static final String INSTALL_SPARK = "install_spark";

   private static final String SQL_CHUNK_DEFAULT_CONNECTION = "default_sql_connection_name";

   private static final String LAUNCH_EMBEDDED_SHINY_CONNECTION_UI = "launch_embedded_shiny_connection_ui";
   private static final String RSTUDIOAPI_SHOW_DIALOG_COMPLETED = "rstudioapi_show_dialog_completed";

   private static final String STOP_SHINY_APP = "stop_shiny_app";
}
