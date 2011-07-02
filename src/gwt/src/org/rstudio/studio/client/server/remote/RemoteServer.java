/*
 * RemoteServer.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.server.remote;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Random;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.jsonrpc.*;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.*;
import org.rstudio.studio.client.application.model.HttpLogEntry;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.projects.model.CreateProjectResult;
import org.rstudio.studio.client.projects.model.OpenProjectResult;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.remote.RemoteServerEventListener.ClientEvent;
import org.rstudio.studio.client.workbench.model.Agreement;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.WorkbenchMetrics;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.views.files.model.FileUploadToken;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;
import org.rstudio.studio.client.workbench.views.help.model.Link;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.views.plots.model.Point;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.model.CheckForExternalEditResult;
import org.rstudio.studio.client.workbench.views.source.model.PublishPdfResult;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.workspace.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class RemoteServer implements Server
{ 
   @Inject
   public RemoteServer(Session session, EventBus eventBus)
   {
      clientId_ = null;
      disconnected_ = false;
      workbenchReady_ = false;
      session_ = session;
      eventBus_ = eventBus;
      serverAuth_ = new RemoteServerAuth(this);
      serverEventListener_ = new RemoteServerEventListener(this);
   }
   
   // complete initialization now that the workbench is ready
   public void onWorkbenchReady()
   {
      // update state
      workbenchReady_ = true;
      
      // only check credentials if we are in server mode
      if (session_.getSessionInfo().getMode().equals(SessionInfo.SERVER_MODE))
         serverAuth_.schedulePeriodicCredentialsUpdate();
      
      // start event listener
      serverEventListener_.start();
   }
   
   public void ensureListeningForEvents()
   {
      // if the workbench is ready then make sure we are listening for
      // events (retry events up to 10 times). 
      
      // we need to check for workbenchReady_ because we don't want to cause
      // events to flow prior to the workbench being instantiated and fully 
      // initialized. since this method can be called at any time we need to
      // protect ourselves against this "pre-workbench initialization" state
      
      // the retries are there to work around the fact that when we execute a
      // network request which causes us to resume from a suspended session
      // the first query for events often returns ServiceUnavailable because 
      // the process isn't alive yet. by retrying we make certain that if
      // the first attempts to listen fail we eventually get synced up
      
      if (workbenchReady_)
         serverEventListener_.ensureListening(10);
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
   
   // accept application agreement
   public void acceptAgreement(Agreement agreement, 
                               ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, 
                  ACCEPT_AGREEMENT, 
                  agreement.getHash(),
                  requestCallback);
   }
   
   
   public void suspendSession(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, SUSPEND_SESSION, requestCallback);
   }
   
   public void quitSession(boolean saveWorkspace, 
                           String switchToProject,
                           ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, JSONBoolean.getInstance(saveWorkspace));
      params.set(1, new JSONString(StringUtil.notNull(switchToProject)));
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
                                 ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONObject(temporary));
      params.set(1, new JSONObject(persistent));
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

   public void consoleInput(String consoleInput,
                            ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, CONSOLE_INPUT, consoleInput, requestCallback);
   }
   
   public void resetConsoleActions(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, RESET_CONSOLE_ACTIONS, requestCallback);
   }
   
   public void interrupt(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, INTERRUPT, requestCallback);
   }
   
   public void abort(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, ABORT, requestCallback);
   }
   
   public void httpLog(
         ServerRequestCallback<JsArray<HttpLogEntry>> requestCallback)
   {
      sendRequest(RPC_SCOPE, HTTP_LOG, requestCallback);
   }

   public void getCompletions(String line, int cursorPos,
                          ServerRequestCallback<Completions> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(line));
      params.set(1, new JSONNumber(cursorPos));
      sendRequest(RPC_SCOPE, 
                  GET_COMPLETIONS, 
                  params, 
                  requestCallback) ;
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
   
   public void listObjects(
         ServerRequestCallback<RpcObjectList<WorkspaceObjectInfo>> requestCallback)
   {
      sendRequest(RPC_SCOPE, LIST_OBJECTS, requestCallback);
   }

  
   public void removeAllObjects(ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  REMOVE_ALL_OBJECTS,
                  requestCallback);
   }

   
   public void setObjectValue(String objectName,
                              String value,
                              ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(objectName));
      params.set(1, new JSONString(value));
      sendRequest(RPC_SCOPE,
                  SET_OBJECT_VALUE,
                  params,
                  requestCallback);
   }

   public void getObjectValue(String objectName,
                              ServerRequestCallback<RpcObjectList<WorkspaceObjectInfo>> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_OBJECT_VALUE,
                  objectName,
                  requestCallback);
   }

 
   public void listGoogleSpreadsheets(
         String titlePattern,             // null for all spreadsheets
         int maxResults,
         ServerRequestCallback<JsArray<GoogleSpreadsheetInfo>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, titlePattern != null ? new JSONString(titlePattern) :
                                           JSONNull.getInstance());
      params.set(1, new JSONNumber(maxResults));
      sendRequest(RPC_SCOPE, 
                  LIST_GOOGLE_SPREADSHEETS, 
                  params, 
                  requestCallback) ;
   }
   
   public void importGoogleSpreadsheet(
                                GoogleSpreadsheetImportSpec importSpec,
                                ServerRequestCallback<Void> requestCallback)
   {
      sendRequest(RPC_SCOPE, 
                  IMPORT_GOOGLE_SPREADSHEET, 
                  importSpec, 
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
                                boolean heading,
                                String separator,
                                String decimal,
                                String quote,
                                ServerRequestCallback<DataPreviewResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(dataFilePath));
      params.set(1, JSONBoolean.getInstance(heading));
      params.set(2, new JSONString(separator));
      params.set(3, new JSONString(decimal));
      params.set(4, new JSONString(quote));

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


   public void listPackages(
         ServerRequestCallback<JsArray<PackageInfo>> requestCallback)
   {
      sendRequest(RPC_SCOPE, LIST_PACKAGES, requestCallback);
   }
   
   public void getPackageInstallContext(
               ServerRequestCallback<PackageInstallContext> requestCallback)
   {
      sendRequest(RPC_SCOPE, GET_PACKAGE_INSTALL_CONTEXT, requestCallback);
   }
   
   public void isPackageLoaded(String packageName,
                               ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE, IS_PACKAGE_LOADED, packageName, requestCallback);
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
   
   public void showHelpTopic(String topic, String pkgName)
   {
      JSONArray params = new JSONArray() ;
      params.set(0, new JSONString(topic)) ;
      params.set(1, pkgName != null 
                       ? new JSONString(pkgName)
                       : JSONNull.getInstance()) ;
      
      sendRequest(RPC_SCOPE,
                  SHOW_HELP_TOPIC,
                  params,
                  null) ;
   }
   
   public void search(String query, 
                      ServerRequestCallback<JsArrayString> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  SEARCH,
                  query,
                  requestCallback) ;
   }
   
   public void getHelpLinks(String setName, 
                            ServerRequestCallback<LinksList> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  GET_HELP_LINKS,
                  setName,
                  requestCallback) ;
   }
   
   public void setHelpLinks(String setName, ArrayList<Link> links)
   {
      JSONArray urls = new JSONArray() ;
      JSONArray titles = new JSONArray() ;
      for (int i = 0; i < links.size(); i++)
      {
         urls.set(i, new JSONString(links.get(i).getUrl())) ;
         titles.set(i, new JSONString(links.get(i).getTitle())) ;
      }
      
      JSONArray params = new JSONArray() ;
      params.set(0, new JSONString(setName)) ;
      params.set(1, urls) ;
      params.set(2, titles) ;
      
      sendRequest(RPC_SCOPE,
                  SET_HELP_LINKS,
                  params,
                  null) ;
   }

   public void listFiles(
                  FileSystemItem directory,
                  boolean monitor,
                  ServerRequestCallback<JsArray<FileSystemItem>> requestCallback)
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
                        ServerRequestCallback<Void> requestCallback)
   {
      JSONArray paramArray = new JSONArray();
      paramArray.set(0, new JSONString(sourceFile.getPath()));
      paramArray.set(1, new JSONString(targetFile.getPath()));
      
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
      return getApplicationURL(EXPORT_SCOPE) + "?" +
         "name=" + URL.encodeQueryString(name) + "&" +
         "file=" + URL.encodeQueryString(file.getPath());
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
      return getApplicationURL(EXPORT_SCOPE) + "?" +
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
                             boolean overwrite,
                             ServerRequestCallback<Bool> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(file.getPath()));
      params.set(1, new JSONNumber(widthInches));
      params.set(2, new JSONNumber(heightInches));
      params.set(3, JSONBoolean.getInstance(overwrite));
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
   
   public void createProject(
         String projectDirectory,
         ServerRequestCallback<CreateProjectResult> requestCallback)
   {
      sendRequest(RPC_SCOPE, CREATE_PROJECT, projectDirectory, requestCallback);
   }
   
   public void openProject(
         String projectDirectory,
         ServerRequestCallback<OpenProjectResult> requestCallback)
   {
      sendRequest(RPC_SCOPE, OPEN_PROJECT, projectDirectory, requestCallback);
   }

   public void newDocument(String filetype,
                           JsObject properties,
                           ServerRequestCallback<SourceDocument> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(filetype));
      params.set(1, new JSONObject(properties));
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

   public void listDocuments(
         ServerRequestCallback<JsArray<SourceDocument>> requestCallback)
   {
      sendRequest(RPC_SCOPE, LIST_DOCUMENTS, requestCallback);
   }

   public void saveDocument(String id,
                            String path,
                            String fileType,
                            String encoding,
                            String contents,
                            ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, path == null ? JSONNull.getInstance() : new JSONString(path));
      params.set(2, fileType == null ? JSONNull.getInstance() : new JSONString(fileType));
      params.set(3, encoding == null ? JSONNull.getInstance() : new JSONString(encoding));
      params.set(4, new JSONString(contents));
      sendRequest(RPC_SCOPE, SAVE_DOCUMENT, params, requestCallback);
   }

   public void saveDocumentDiff(String id,
                                String path,
                                String fileType,
                                String encoding,
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
      params.set(4, new JSONString(replacement));
      params.set(5, new JSONNumber(offset));
      params.set(6, new JSONNumber(length));
      params.set(7, new JSONString(hash));
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
   
   public void publishPdf(String id,
                          String title,
                          boolean update,
                          ServerRequestCallback<PublishPdfResult> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(id));
      params.set(1, new JSONString(title));
      params.set(2, JSONBoolean.getInstance(update));
      
      sendRequest(RPC_SCOPE, PUBLISH_PDF, params, requestCallback);
   }

   public void isTexInstalled(ServerRequestCallback<Boolean> requestCallback)
   {
      sendRequest(RPC_SCOPE,
                  IS_TEX_INSTALLED,
                  requestCallback);
   }
   
   public String getProgressUrl(String message)
   {
      String url = getApplicationURL(SOURCE_SCOPE + "/" + "progress");
      url += "?message=" + URL.encodeQueryString(message);
      return url;
   }
   
  
   public void saveActiveDocument(String contents,
                                  boolean sweave,
                                  ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(contents));
      params.set(1, JSONBoolean.getInstance(sweave));

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

   public void detectFreeVars(String code,
                              ServerRequestCallback<JsArrayString> requestCallback)
   {
      sendRequest(RPC_SCOPE, DETECT_FREE_VARS, code, requestCallback);
   }

   public void iconvlist(ServerRequestCallback<IconvListResult> requestCallback)
   {
      sendRequest(RPC_SCOPE, ICONVLIST, requestCallback);      
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
         ServerRequestCallback<RpcObjectList<HistoryEntry>> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(prefix));
      params.set(1, new JSONNumber(maxEntries));
      sendRequest(RPC_SCOPE, SEARCH_HISTORY_ARCHIVE_BY_PREFIX, params, requestCallback);
   }
   
   public void vcsAdd(ArrayList<String> paths,
                      ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = new JSONArray();
      for (int i = 0; i < paths.size(); i++)
         jsonPaths.set(i, new JSONString(paths.get(i)));

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, VCS_ADD, params, requestCallback);
   }

   public void vcsRemove(ArrayList<String> paths,
                         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = new JSONArray();
      for (int i = 0; i < paths.size(); i++)
         jsonPaths.set(i, new JSONString(paths.get(i)));

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, VCS_REMOVE, params, requestCallback);
   }

   public void vcsRevert(ArrayList<String> paths,
                         ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = new JSONArray();
      for (int i = 0; i < paths.size(); i++)
         jsonPaths.set(i, new JSONString(paths.get(i)));

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, VCS_REVERT, params, requestCallback);
   }

   public void vcsUnstage(ArrayList<String> paths,
                          ServerRequestCallback<Void> requestCallback)
   {
      JSONArray jsonPaths = new JSONArray();
      for (int i = 0; i < paths.size(); i++)
         jsonPaths.set(i, new JSONString(paths.get(i)));

      JSONArray params = new JSONArray();
      params.set(0, jsonPaths);
      sendRequest(RPC_SCOPE, VCS_UNSTAGE, params, requestCallback);
   }

   @Override
   public void vcsFullStatus(ServerRequestCallback<JsArray<StatusAndPath>> requestCallback)
   {
      sendRequest(RPC_SCOPE, VCS_FULL_STATUS, requestCallback);
   }

   public void vcsCommitGit(String message,
                            boolean amend,
                            boolean signOff,
                            ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(message));
      params.set(1, JSONBoolean.getInstance(amend));
      params.set(2, JSONBoolean.getInstance(signOff));
      sendRequest(RPC_SCOPE, VCS_COMMIT_GIT, params, requestCallback);
   }

   @Override
   public void vcsDiffFile(String path,
                           PatchMode mode,
                           int contextLines,
                           ServerRequestCallback<String> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(path));
      params.set(1, new JSONNumber(mode.getValue()));
      params.set(2, new JSONNumber(contextLines));
      sendRequest(RPC_SCOPE, VCS_DIFF_FILE, params, requestCallback);
   }

   @Override
   public void vcsApplyPatch(String patch,
                             PatchMode mode,
                             ServerRequestCallback<Void> requestCallback)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONString(patch));
      params.set(1, new JSONNumber(mode.getValue()));
      sendRequest(RPC_SCOPE, VCS_APPLY_PATCH, params, requestCallback);
   }

   // package-visible methods for peer classes RemoteServerAuth and
   // RemoveServerEventListener

   
   EventBus getEventBus()
   {
      return eventBus_;
   }

   RpcRequest getEvents(
                  int lastEventId, 
                  ServerRequestCallback<JsArray<ClientEvent>> requestCallback,
                  RetryHandler retryHandler)
   {
      JSONArray params = new JSONArray();
      params.set(0, new JSONNumber(lastEventId));
      return sendRequest(EVENTS_SCOPE, 
                         "get_events", 
                         params, 
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

   @SuppressWarnings("unused")
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
      // retry handler (make the same call with the same params. ensure that
      // only one retry occurs by passing null as the retryHandler)
      RetryHandler retryHandler = new RetryHandler() {
        
         public void onRetry()
         {
            // retry one time (passing null as last param ensures there
            // is no retry handler installed)
            sendRequest(scope, method, params, requestCallback, null);    
         }   

         public void onError(ServerError error)
         {
            // propagate error which caused the retry to the caller
            requestCallback.onError(error);
         }
      };
      
      // submit request (retry same request up to one time)
      sendRequest(scope, method, params, requestCallback, retryHandler);
   }
    
   
   private <T> RpcRequest sendRequest(
                              String scope, 
                              String method, 
                              JSONArray params,
                              final ServerRequestCallback<T> requestCallback,
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
                                             clientId_,
                                             clientVersion_);
      
      // send the request
      rpcRequest.send(new RpcRequestCallback() {
         public void onError(RpcRequest request, RpcError error)
         {
            // ignore errors if:
            //   - we are disconnected;
            //   - no response handler; or 
            //   - handler was cancelled
            if ( disconnected_                || 
                 (requestCallback == null)    || 
                 requestCallback.cancelled() )
            {
               return;
            }
            
            // if we have a retry handler then see if we can resolve the
            // error and then retry
            if ( resolveRpcErrorAndRetry(error, retryHandler) ) 
               return ;

            // first crack goes to globally registered rpc error handlers
            if (!handleRpcErrorInternally(error))
            {
               // no global handlers processed it, send on to caller
               requestCallback.onError(new RemoteServerError(error));
            }
         }

         public void onResponseReceived(final RpcRequest request,
                                        RpcResponse response)
         {
            // ignore response if:
            //   - we are disconnected;
            //   - no response handler; or 
            //   - handler was cancelled
            if ( disconnected_                 || 
                 (requestCallback == null)     || 
                 requestCallback.cancelled() )
            {
                 return;
            }
                   
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
                  requestCallback.onError(new RemoteServerError(error));
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
               // no error, get the result
               T result = response.<T> getResult();
               requestCallback.onResponseReceived(result);
               
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
      else
      {
         return false;
      } 
   }
  
   private void disconnect()
   {
      disconnected_ = true;
      serverEventListener_.stop();
   }

   private String clientId_;
   private double clientVersion_ = 0;
   private boolean workbenchReady_;
   private boolean disconnected_;
   
   private final RemoteServerAuth serverAuth_;
   private final RemoteServerEventListener serverEventListener_ ;
  
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
   private static final String FILE_SHOW = "file_show";
   
   // session methods
   private static final String CLIENT_INIT = "client_init";
   private static final String ACCEPT_AGREEMENT = "accept_agreement";
   private static final String SUSPEND_SESSION = "suspend_session";
   private static final String QUIT_SESSION = "quit_session";
   
   private static final String SET_WORKBENCH_METRICS = "set_workbench_metrics";
   private static final String SET_PREFS = "set_prefs";
   private static final String SET_UI_PREFS = "set_ui_prefs";
   private static final String GET_R_PREFS = "get_r_prefs";
   private static final String SET_CLIENT_STATE = "set_client_state";
   private static final String USER_PROMPT_COMPLETED = "user_prompt_completed";
   
   private static final String CONSOLE_INPUT = "console_input";
   private static final String RESET_CONSOLE_ACTIONS = "reset_console_actions";
   private static final String INTERRUPT = "interrupt";
   private static final String ABORT = "abort";
   private static final String HTTP_LOG = "http_log";
   private static final String GET_COMPLETIONS = "get_completions";
   private static final String GET_HELP_AT_CURSOR = "get_help_at_cursor";

   private static final String LIST_OBJECTS = "list_objects";
   private static final String REMOVE_ALL_OBJECTS = "remove_all_objects";
   private static final String SET_OBJECT_VALUE = "set_object_value";
   private static final String GET_OBJECT_VALUE = "get_object_value";
   private static final String LIST_GOOGLE_SPREADSHEETS = "list_google_spreadsheets";
   private static final String IMPORT_GOOGLE_SPREADSHEET = "import_google_spreadsheet";
   private static final String DOWNLOAD_DATA_FILE = "download_data_file";
   private static final String GET_DATA_PREVIEW = "get_data_preview";
   private static final String GET_OUTPUT_PREVIEW = "get_output_preview";

   private static final String EDIT_COMPLETED = "edit_completed";
   private static final String CHOOSE_FILE_COMPLETED = "choose_file_completed";
   
   private static final String LIST_PACKAGES = "list_packages";
   private static final String AVAILABLE_PACKAGES = "available_packages";
   private static final String CHECK_FOR_PACKAGE_UPDATES = "check_for_package_updates";
   private static final String INIT_DEFAULT_USER_LIBRARY = "init_default_user_library";
   private static final String GET_PACKAGE_INSTALL_CONTEXT = "get_package_install_context";
   private static final String IS_PACKAGE_LOADED = "is_package_loaded";
   private static final String SET_CRAN_MIRROR = "set_cran_mirror";
   private static final String GET_CRAN_MIRRORS = "get_cran_mirrors";

   private static final String GET_HELP = "get_help";
   private static final String SHOW_HELP_TOPIC = "show_help_topic" ;
   private static final String SEARCH = "search" ;
   private static final String GET_HELP_LINKS = "get_help_links" ;
   private static final String SET_HELP_LINKS = "set_help_links" ;

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
   private static final String GET_UNIQUE_SAVE_PLOT_STEM = "get_unique_save_plot_stem";
   private static final String GET_SAVE_PLOT_CONTEXT = "get_save_plot_context";
   private static final String LOCATOR_COMPLETED = "locator_completed";
   private static final String SET_MANIPULATOR_VALUES = "set_manipulator_values";

   private static final String CREATE_PROJECT = "create_project";
   private static final String OPEN_PROJECT = "open_project";
   
   private static final String NEW_DOCUMENT = "new_document";
   private static final String OPEN_DOCUMENT = "open_document";
   private static final String LIST_DOCUMENTS = "list_documents";
   private static final String SAVE_DOCUMENT = "save_document";
   private static final String SAVE_DOCUMENT_DIFF = "save_document_diff";
   private static final String CHECK_FOR_EXTERNAL_EDIT = "check_for_external_edit";
   private static final String IGNORE_EXTERNAL_EDIT = "ignore_external_edit";
   private static final String CLOSE_DOCUMENT = "close_document";
   private static final String CLOSE_ALL_DOCUMENTS = "close_all_documents";
   private static final String SET_SOURCE_DOCUMENT_ON_SAVE
         = "set_source_document_on_save";
   private static final String SAVE_ACTIVE_DOCUMENT = "save_active_document";
   private static final String MODIFY_DOCUMENT_PROPERTIES = "modify_document_properties";
   private static final String REVERT_DOCUMENT = "revert_document";
   private static final String REOPEN_WITH_ENCODING = "reopen_with_encoding";
   private static final String REMOVE_CONTENT_URL = "remove_content_url";
   private static final String DETECT_FREE_VARS = "detect_free_vars";
   private static final String ICONVLIST = "iconvlist";
   private static final String PUBLISH_PDF = "publish_pdf";
   private static final String IS_TEX_INSTALLED = "is_tex_installed";

   private static final String GET_RECENT_HISTORY = "get_recent_history";
   private static final String GET_HISTORY_ITEMS = "get_history_items";
   private static final String REMOVE_HISTORY_ITEMS = "remove_history_items";
   private static final String CLEAR_HISTORY = "clear_history";
   private static final String GET_HISTORY_ARCHIVE_ITEMS = "get_history_archive_items";
   private static final String SEARCH_HISTORY_ARCHIVE = "search_history_archive";
   private static final String SEARCH_HISTORY_ARCHIVE_BY_PREFIX = "search_history_archive_by_prefix";

   private static final String VCS_ADD = "vcs_add";
   private static final String VCS_REMOVE = "vcs_remove";
   private static final String VCS_REVERT = "vcs_revert";
   private static final String VCS_UNSTAGE = "vcs_unstage";
   private static final String VCS_FULL_STATUS = "vcs_full_status";
   private static final String VCS_COMMIT_GIT = "vcs_commit_git";
   private static final String VCS_DIFF_FILE = "vcs_diff_file";
   private static final String VCS_APPLY_PATCH = "vcs_apply_patch";

   private static final String LOG = "log";


}
