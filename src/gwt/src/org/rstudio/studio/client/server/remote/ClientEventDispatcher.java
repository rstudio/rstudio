/*
 * ClientEventDispatcher.java
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


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.application.events.*;
import org.rstudio.studio.client.application.model.RVersionsInfo;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfErrorsEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfOutputEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfStartedEvent;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfResult;
import org.rstudio.studio.client.common.console.ConsoleProcessCreatedEvent;
import org.rstudio.studio.client.common.console.ServerConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ServerConsolePromptEvent;
import org.rstudio.studio.client.common.console.ServerProcessExitEvent;
import org.rstudio.studio.client.common.debugging.events.ErrorHandlerChangedEvent;
import org.rstudio.studio.client.common.debugging.events.PackageLoadedEvent;
import org.rstudio.studio.client.common.debugging.events.PackageUnloadedEvent;
import org.rstudio.studio.client.common.debugging.events.UnhandledErrorEvent;
import org.rstudio.studio.client.common.debugging.model.ErrorHandlerType;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.dependencies.events.InstallShinyEvent;
import org.rstudio.studio.client.common.rpubs.events.RPubsUploadStatusEvent;
import org.rstudio.studio.client.common.rstudioapi.events.RStudioAPIShowDialogEvent;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.common.synctex.events.SynctexEditFileEvent;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.events.EditorCommandDispatchEvent;
import org.rstudio.studio.client.events.EditorCommandEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewCompletedEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewOutputEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewStartedEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.packages.events.PackageExtensionIndexingCompletedEvent;
import org.rstudio.studio.client.projects.events.FollowUserEvent;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.ProjectAccessRevokedEvent;
import org.rstudio.studio.client.projects.events.ProjectTemplateRegistryUpdatedEvent;
import org.rstudio.studio.client.projects.events.ProjectUserChangedEvent;
import org.rstudio.studio.client.projects.model.OpenProjectError;
import org.rstudio.studio.client.projects.model.ProjectTemplateRegistry;
import org.rstudio.studio.client.projects.model.ProjectUser;
import org.rstudio.studio.client.rmarkdown.events.ChunkExecStateChangedEvent;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshedEvent;
import org.rstudio.studio.client.rmarkdown.events.NotebookRangeExecutedEvent;
import org.rstudio.studio.client.rmarkdown.events.PreviewRmdEvent;
import org.rstudio.studio.client.rmarkdown.events.ShinyGadgetDialogEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdParamsReadyEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderCompletedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderStartedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdShinyDocStartedEvent;
import org.rstudio.studio.client.rmarkdown.events.WebsiteFileSavedEvent;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutput;
import org.rstudio.studio.client.rmarkdown.model.RmdRenderResult;
import org.rstudio.studio.client.rmarkdown.model.RmdShinyDocInfo;
import org.rstudio.studio.client.rsconnect.events.EnableRStudioConnectUIEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentFailedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentOutputEvent;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.events.ShinyFrameNavigatedEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.events.AddinRegistryUpdatedEvent;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.*;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedEvent;
import org.rstudio.studio.client.workbench.snippets.model.SnippetsChangedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildCompletedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildErrorsEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildOutputEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildStartedEvent;
import org.rstudio.studio.client.workbench.views.choosefile.events.ChooseFileEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ActiveConnectionsChangedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionListChangedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionOpenedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionUpdatedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.EnableConnectionsEvent;
import org.rstudio.studio.client.workbench.views.connections.events.NewConnectionDialogUpdatedEvent;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;
import org.rstudio.studio.client.workbench.views.console.events.*;
import org.rstudio.studio.client.workbench.views.console.model.ConsolePrompt;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleResetHistory;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleText;
import org.rstudio.studio.client.workbench.views.data.events.ViewDataEvent;
import org.rstudio.studio.client.workbench.views.data.model.DataView;
import org.rstudio.studio.client.workbench.views.edit.events.ShowEditorEvent;
import org.rstudio.studio.client.workbench.views.edit.model.ShowEditorData;
import org.rstudio.studio.client.workbench.views.environment.events.*;
import org.rstudio.studio.client.workbench.views.environment.model.DebugSourceResult;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentContextData;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.history.events.HistoryEntriesAddedEvent;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.output.find.events.FindOperationEndedEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindResultEvent;
import org.rstudio.studio.client.workbench.views.output.lint.events.LintEvent;
import org.rstudio.studio.client.workbench.views.output.markers.events.MarkersChangedEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.events.SourceCppCompletedEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.events.SourceCppStartedEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.model.SourceCppState;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.LoadedPackageUpdatesEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStatusChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.model.PackageProvidedExtensions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageState;
import org.rstudio.studio.client.workbench.views.packages.model.PackageStatus;
import org.rstudio.studio.client.workbench.views.plots.events.LocatorEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsZoomSizeChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsState;
import org.rstudio.studio.client.workbench.views.presentation.events.PresentationPaneRequestCompletedEvent;
import org.rstudio.studio.client.workbench.views.presentation.events.ShowPresentationPaneEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.events.ObjectExplorerEvent;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.RprofEvent;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditEndedEvent;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditSavedEvent;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DataViewChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.FileEditEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowContentEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowDataEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceExtendedTypeDetectedEvent;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSubprocEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.AskPassEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigateEvent;

import java.util.ArrayList;

public class ClientEventDispatcher 
{
   public ClientEventDispatcher(EventBus eventBus)
   {
      eventBus_ = eventBus;
   }
   
   public void enqueEventAsJso(JavaScriptObject event)
   {
      ClientEvent clientEvent = event.<ClientEvent>cast();
      enqueEvent(clientEvent);
   }

   public void enqueEvent(ClientEvent event)
   {
      pendingEvents_.add(event);
      if (pendingEvents_.size() == 1)
      {
         Scheduler.get().scheduleIncremental(new RepeatingCommand()
         {
            public boolean execute()
            {
               final int MAX_EVENTS_AT_ONCE = 200;
               for (int i = 0;
                    i < MAX_EVENTS_AT_ONCE && pendingEvents_.size() > 0;
                    i++)
               {
                  ClientEvent currentEvent = pendingEvents_.remove(0);
                  dispatchEvent(currentEvent);
               }
               return pendingEvents_.size() > 0;
            }
         });
      }
   }
   
   private void dispatchEvent(ClientEvent event) 
   { 
      String type = event.getType();
      try
      {
         if (type.equals(ClientEvent.Busy))
         {
            boolean busy = event.<Bool>getData().getValue();
            eventBus_.fireEvent(new BusyEvent(busy));
         }
         else if (type.equals(ClientEvent.ConsoleOutput))
         {
            ConsoleText output = event.getData();
            eventBus_.fireEvent(new ConsoleWriteOutputEvent(output));
         }
         else if (type.equals(ClientEvent.ConsoleError))
         {
            ConsoleText error = event.getData();
            eventBus_.fireEvent(new ConsoleWriteErrorEvent(error));
         }
         else if (type.equals(ClientEvent.ConsoleWritePrompt))
         {
            String prompt = event.getData();
            eventBus_.fireEvent(new ConsoleWritePromptEvent(prompt));
         }
         else if (type.equals(ClientEvent.ConsoleWriteInput))
         {
            ConsoleText input = event.getData();
            eventBus_.fireEvent(new ConsoleWriteInputEvent(input));
         }
         else if (type.equals(ClientEvent.ConsolePrompt))
         {
            ConsolePrompt prompt = event.getData();
            eventBus_.fireEvent(new ConsolePromptEvent(prompt));
         }
         else if (type.equals(ClientEvent.ShowEditor))
         {
            ShowEditorData data = event.getData();
            eventBus_.fireEvent(new ShowEditorEvent(data));
         }
         else if (type.equals(ClientEvent.FileChanged))
         {
            FileChange fileChange = event.getData();
            eventBus_.fireEvent(new FileChangeEvent(fileChange));
         }
         else if (type.equals(ClientEvent.WorkingDirChanged))
         {
            String path = event.getData();
            eventBus_.fireEvent(new WorkingDirChangedEvent(path));
         }
         else if (type.equals(ClientEvent.ShowHelp))
         {
            String helpUrl = event.getData();
            eventBus_.fireEvent(new ShowHelpEvent(helpUrl));
         }
         else if (type.equals(ClientEvent.ShowErrorMessage))
         {
            ErrorMessage errorMessage = event.getData();
            eventBus_.fireEvent(new ShowErrorMessageEvent(errorMessage));
         }
         else if (type.equals(ClientEvent.ChooseFile))
         {
            boolean newFile = event.<Bool>getData().getValue();
            eventBus_.fireEvent(new ChooseFileEvent(newFile));
         }
         else if (type.equals(ClientEvent.BrowseUrl))
         {
            BrowseUrlInfo urlInfo = event.getData();
            eventBus_.fireEvent(new BrowseUrlEvent(urlInfo));
         }
         else if (type.equals(ClientEvent.PlotsStateChanged))
         {
            PlotsState plotsState = event.getData();
            eventBus_.fireEvent(new PlotsChangedEvent(plotsState));
         }
         else if (type.equals(ClientEvent.ViewData))
         {
            DataView dataView = event.getData();
            eventBus_.fireEvent(new ViewDataEvent(dataView));
         }
         else if (type.equals(ClientEvent.PackageStateChanged))
         {
            PackageState newState = event.getData();
            eventBus_.fireEvent(new PackageStateChangedEvent(newState));
         }
         else if (type.equals(ClientEvent.PackageStatusChanged))
         {
            PackageStatus status = event.getData();
            eventBus_.fireEvent(new PackageStatusChangedEvent(status));
         }
         else if (type.equals(ClientEvent.Locator))
         {
            eventBus_.fireEvent(new LocatorEvent());
         }
         else if (type.equals(ClientEvent.ConsoleResetHistory))
         {
            ConsoleResetHistory reset = event.getData();
            eventBus_.fireEvent(new ConsoleResetHistoryEvent(reset));
         }
         else if (type.equals(ClientEvent.SessionSerialization))
         {
            SessionSerializationAction action = event.getData();
            eventBus_.fireEvent(new SessionSerializationEvent(action));
         }
         else if (type.equals(ClientEvent.HistoryEntriesAdded))
         {
            RpcObjectList<HistoryEntry> entries = event.getData();
            eventBus_.fireEvent(new HistoryEntriesAddedEvent(entries));
         }
         else if (type.equals(ClientEvent.QuotaStatus))
         {
            QuotaStatus quotaStatus = event.getData();
            eventBus_.fireEvent(new QuotaStatusEvent(quotaStatus));
         }
         else if (type.equals(ClientEvent.FileEdit))
         {
            FileSystemItem file = event.getData();
            eventBus_.fireEvent(new FileEditEvent(file));
         }
         else if (type.equals(ClientEvent.ShowContent))
         {
            ContentItem content = event.getData();
            eventBus_.fireEvent(new ShowContentEvent(content));
         }
         else if (type.equals(ClientEvent.ShowData))
         {
            DataItem data = event.getData();
            eventBus_.fireEvent(new ShowDataEvent(data));
         }
         else if (type.equals(ClientEvent.AbendWarning))
         {            
            eventBus_.fireEvent(new SessionAbendWarningEvent());
         }
         else if (type.equals(ClientEvent.ShowWarningBar))
         {
            WarningBarMessage message = event.getData();
            eventBus_.fireEvent(new ShowWarningBarEvent(message));
         }
         else if (type.equals(ClientEvent.OpenProjectError))
         {
            OpenProjectError error = event.getData();
            eventBus_.fireEvent(new OpenProjectErrorEvent(error));
         }
         else if (type.equals(ClientEvent.VcsRefresh))
         {
            JsObject data = event.getData();
            eventBus_.fireEvent(new VcsRefreshEvent(Reason.NA,
                                                    data.getInteger("delay")));
         }
         else if (type.equals(ClientEvent.AskPass))
         {
            AskPassEvent.Data data = event.getData();
            eventBus_.fireEvent(new AskPassEvent(data));
         }
         else if (type.equals(ClientEvent.ConsoleProcessOutput))
         {
            ServerConsoleOutputEvent.Data data = event.getData();
            eventBus_.fireEvent(new ServerConsoleOutputEvent(data.getHandle(),
                                                            data.getOutput()));
         }
         else if (type.equals(ClientEvent.ConsoleProcessPrompt))
         {
            ServerConsolePromptEvent.Data data = event.getData();
            eventBus_.fireEvent(new ServerConsolePromptEvent(data.getHandle(),
                                                             data.getPrompt()));
         }
         else if (type.equals(ClientEvent.ConsoleProcessCreated))
         {
            ConsoleProcessCreatedEvent.Data data = event.getData();
            eventBus_.fireEvent(new ConsoleProcessCreatedEvent(data));
         }
         else if (type.equals(ClientEvent.ConsoleProcessExit))
         {
            ServerProcessExitEvent.Data data = event.getData();
            eventBus_.fireEvent(new ServerProcessExitEvent(data.getHandle(),
                                                          data.getExitCode()));
         }
         else if (type.equals(ClientEvent.HTMLPreviewStartedEvent))
         {
            HTMLPreviewStartedEvent.Data data = event.getData();
            eventBus_.fireEvent(new HTMLPreviewStartedEvent(data));
         }
         else if (type.equals(ClientEvent.HTMLPreviewOutputEvent))
         {
            String output = event.getData();
            eventBus_.fireEvent(new HTMLPreviewOutputEvent(output));
         }
         else if (type.equals(ClientEvent.HTMLPreviewCompletedEvent))
         {
            HTMLPreviewResult result = event.getData();
            eventBus_.fireEvent(new HTMLPreviewCompletedEvent(result));
         }
         else if (type.equals(ClientEvent.CompilePdfStartedEvent))
         {
            CompilePdfStartedEvent.Data data = event.getData();
            eventBus_.fireEvent(new CompilePdfStartedEvent(data));
         }
         else if (type.equals(ClientEvent.CompilePdfOutputEvent))
         {
            CompileOutput output = event.getData();
            eventBus_.fireEvent(new CompilePdfOutputEvent(output));
         }
         else if (type.equals(ClientEvent.CompilePdfErrorsEvent))
         {
            JsArray<SourceMarker> data = event.getData();
            eventBus_.fireEvent(new CompilePdfErrorsEvent(data));
         }
         else if (type.equals(ClientEvent.CompilePdfCompletedEvent))
         {
            CompilePdfResult result = event.getData();
            eventBus_.fireEvent(new CompilePdfCompletedEvent(result));
         }
         else if (type.equals(ClientEvent.SynctexEditFile))
         {
            SourceLocation sourceLocation = event.getData();
            eventBus_.fireEvent(new SynctexEditFileEvent(sourceLocation));
         }
         else if (type.equals(ClientEvent.FindResult))
         {
            FindResultEvent.Data data = event.getData();
            eventBus_.fireEvent(new FindResultEvent(
                  data.getHandle(), data.getResults().toArrayList()));
         }
         else if (type.equals(ClientEvent.FindOperationEnded))
         {
            String data = event.getData();
            eventBus_.fireEvent(new FindOperationEndedEvent(data));
         }
         else if (type.equals(ClientEvent.RPubsUploadStatus))
         {
            RPubsUploadStatusEvent.Status status = event.getData();
            eventBus_.fireEvent(new RPubsUploadStatusEvent(status));
         }
         else if (type.equals(ClientEvent.BuildStarted))
         {
            eventBus_.fireEvent(new BuildStartedEvent());
         }
         else if (type.equals(ClientEvent.BuildOutput))
         {
            CompileOutput data = event.getData();
            eventBus_.fireEvent(new BuildOutputEvent(data));
         }
         else if (type.equals(ClientEvent.BuildCompleted))
         {
            BuildCompletedEvent.Data data = event.getData();
            eventBus_.fireEvent(new BuildCompletedEvent(data));
         }
         else if (type.equals(ClientEvent.BuildErrors))
         {
            BuildErrorsEvent.Data data = event.getData();
            eventBus_.fireEvent(new BuildErrorsEvent(data));
         }
         else if (type.equals(ClientEvent.DirectoryNavigate))
         {
            DirectoryNavigateEvent.Data data = event.getData();
            eventBus_.fireEvent(new DirectoryNavigateEvent(data));
         }
         else if (type.equals(ClientEvent.DeferredInitCompleted))
         {
            eventBus_.fireEvent(new DeferredInitCompletedEvent());
         }
         else if (type.equals(ClientEvent.PlotsZoomSizeChanged))
         {
            PlotsZoomSizeChangedEvent.Data data = event.getData();
            eventBus_.fireEvent(new PlotsZoomSizeChangedEvent(data));
         }
         else if (type.equals(ClientEvent.SourceCppStarted))
         {
            eventBus_.fireEvent(new SourceCppStartedEvent());
         }
         else if (type.equals(ClientEvent.SourceCppCompleted))
         {
            SourceCppState state = event.getData();
            eventBus_.fireEvent(new SourceCppCompletedEvent(state));
         }
         else if (type.equals(ClientEvent.LoadedPackageUpdates))
         {
            String installCmd = event.getData();
            eventBus_.fireEvent(new LoadedPackageUpdatesEvent(installCmd));
         }
         else if (type.equals(ClientEvent.ActivatePane))
         {
            String pane = event.getData();
            eventBus_.fireEvent(new ActivatePaneEvent(pane));
         }
         else if (type.equals(ClientEvent.ShowPresentationPane))
         {
            PresentationState state = event.getData();
            eventBus_.fireEvent(new ShowPresentationPaneEvent(state));
         }
         else if (type.equals(ClientEvent.EnvironmentRefresh))
         {
            eventBus_.fireEvent(new EnvironmentRefreshEvent());
         }
         else if (type.equals(ClientEvent.ListChanged))
         {
            eventBus_.fireEvent(new ListChangedEvent(event.<JsObject>getData()));
         }
         else if (type.equals(ClientEvent.UiPrefsChanged))
         {
            UiPrefsChangedEvent.Data data = event.getData();
            eventBus_.fireEvent(new UiPrefsChangedEvent(data));
         }
         else if (type.equals(ClientEvent.ContextDepthChanged)) {
            EnvironmentContextData data = event.getData();
            eventBus_.fireEvent(new ContextDepthChangedEvent(data, true));
         }
         else if (type.equals(ClientEvent.HandleUnsavedChanges))
         {
            eventBus_.fireEvent(new HandleUnsavedChangesEvent());
         }
         else if (type.equals(ClientEvent.Quit))
         {
            QuitEvent.Data data = event.getData();
            eventBus_.fireEvent(new QuitEvent(data));
         }
         else if (type.equals(ClientEvent.Suicide))
         {
            // NOTE: we don't explicitly stop listening for events here
            // for the reasons cited above in ClientEvent.Quit
            
            // fire event
            String message = event.getData();
            eventBus_.fireEvent(new SuicideEvent(message));
         }
         else if (type.equals(ClientEvent.SaveActionChanged))
         {
            SaveAction action = event.getData();
            eventBus_.fireEvent(new SaveActionChangedEvent(action));
         }
         else if (type.equals(ClientEvent.EnvironmentAssigned))
         {
            RObject objectInfo = event.getData();
            eventBus_.fireEvent(new EnvironmentObjectAssignedEvent(objectInfo));
         }
         else if (type.equals(ClientEvent.EnvironmentRemoved))
         {
            String objectName = event.getData();
            eventBus_.fireEvent(new EnvironmentObjectRemovedEvent(objectName));
         }
         else if (type.equals(ClientEvent.BrowserLineChanged))
         {
            LineData lineData = event.getData();
            eventBus_.fireEvent(new BrowserLineChangedEvent(lineData));
         }
         else if (type.equals(ClientEvent.PackageLoaded))
         {
            eventBus_.fireEvent(new PackageLoadedEvent(
                  (String)event.getData()));
         }
         else if (type.equals(ClientEvent.PackageUnloaded))
         {
            eventBus_.fireEvent(new PackageUnloadedEvent(
                  (String)event.getData()));
         }
         else if (type.equals(ClientEvent.PresentationPaneRequestCompleted))
         {
            eventBus_.fireEvent(new PresentationPaneRequestCompletedEvent());
         }
         else if (type.equals(ClientEvent.UnhandledError))
         {
            UnhandledError err = event.getData();
            eventBus_.fireEvent(new UnhandledErrorEvent(err));
         }
         else if (type.equals(ClientEvent.ErrorHandlerChanged))
         {
            ErrorHandlerType handlerType = event.getData();
            eventBus_.fireEvent(new ErrorHandlerChangedEvent(handlerType));
         }
         else if (type.equals(ClientEvent.ViewerNavigate))
         {
            ViewerNavigateEvent.Data data = event.getData();
            eventBus_.fireEvent(new ViewerNavigateEvent(data));
         }
         else if (type.equals(ClientEvent.SourceExtendedTypeDetected))
         {
            SourceExtendedTypeDetectedEvent.Data data = event.getData();
            eventBus_.fireEvent(new SourceExtendedTypeDetectedEvent(data));
         }
         else if (type.equals(ClientEvent.ShinyViewer))
         {
            ShinyApplicationParams data = event.getData();
            eventBus_.fireEvent(new ShinyApplicationStatusEvent(data, true));
         }
         else if (type.equals(ClientEvent.DebugSourceCompleted))
         {
            DebugSourceResult result = (DebugSourceResult)event.getData();
            eventBus_.fireEvent(new DebugSourceCompletedEvent(result));
         }
         else if (type.equals(ClientEvent.RmdRenderStarted))
         {
            RmdRenderStartedEvent.Data data = event.getData();
            eventBus_.fireEvent(new RmdRenderStartedEvent(data));
         }
         else if (type.equals(ClientEvent.RmdRenderOutput))
         {
            CompileOutput data = event.getData();
            eventBus_.fireEvent(new RmdRenderOutputEvent(data));
         }
         else if (type.equals(ClientEvent.RmdRenderCompleted))
         {
            RmdRenderResult result = event.getData();
            eventBus_.fireEvent(new RmdRenderCompletedEvent(result));
         }
         else if (type.equals(ClientEvent.RmdShinyDocStarted))
         {
            RmdShinyDocInfo docInfo = event.getData();
            eventBus_.fireEvent(new RmdShinyDocStartedEvent(docInfo));
         }
         else if (type.equals(ClientEvent.RSConnectDeploymentOutput))
         {
            CompileOutput output = event.getData();
            eventBus_.fireEvent(new RSConnectDeploymentOutputEvent(output));
         }
         else if (type.equals(ClientEvent.RSConnectDeploymentCompleted))
         {
            String url = event.getData();
            eventBus_.fireEvent(new RSConnectDeploymentCompletedEvent(url));
         }
         else if (type.equals(ClientEvent.RSConnectDeploymentFailed))
         {
            RSConnectDeploymentFailedEvent.Data data = event.getData();
            eventBus_.fireEvent(new RSConnectDeploymentFailedEvent(data));
         }
         else if (type.equals(ClientEvent.UserPrompt))
         {
            UserPrompt prompt = event.getData();
            eventBus_.fireEvent(new UserPromptEvent(prompt));
         }
         else if (type.equals(ClientEvent.InstallRtools))
         {
            InstallRtoolsEvent.Data data = event.getData();
            eventBus_.fireEvent(new InstallRtoolsEvent(data));
         }
         else if (type.equals(ClientEvent.InstallShiny))
         {
            String userAction = event.getData();
            eventBus_.fireEvent(new InstallShinyEvent(userAction));
         }
         else if (type.equals(ClientEvent.SuspendAndRestart))
         {
            SuspendAndRestartEvent.Data data = event.getData();
            eventBus_.fireEvent(new SuspendAndRestartEvent(data));
         }
         else if (type.equals(ClientEvent.DataViewChanged))
         {
            DataViewChangedEvent.Data data = event.getData();
            eventBus_.fireEvent(new DataViewChangedEvent(data));
         }
         else if (type.equals(ClientEvent.ViewFunction))
         {
            SearchPathFunctionDefinition data = event.getData();
            eventBus_.fireEvent(new CodeBrowserNavigationEvent(
                  data, null, false, true));
         }
         else if (type.equals(ClientEvent.MarkersChanged))
         {
            MarkersChangedEvent.Data data = event.getData();
            eventBus_.fireEvent(new MarkersChangedEvent(data));
         }
         else if (type.equals(ClientEvent.EnableRStudioConnect))
         {
            EnableRStudioConnectUIEvent.Data data = event.getData();
            eventBus_.fireEvent(new EnableRStudioConnectUIEvent(data));
         }
         else if (type.equals(ClientEvent.UpdateGutterMarkers))
         {
            LintEvent.Data data = event.getData();
            eventBus_.fireEvent(new LintEvent(data));
         }
         else if (type.equals(ClientEvent.SnippetsChanged))
         {
            SnippetsChangedEvent.Data data = event.getData();
            eventBus_.fireEvent(new SnippetsChangedEvent(data));
         }
         else if (type.equals(ClientEvent.JumpToFunction))
         {
            JumpToFunctionEvent.Data data = event.getData();
            eventBus_.fireEvent(new JumpToFunctionEvent(data));
         }
         else if (type.equals(ClientEvent.CollabEditStarted))
         {
            CollabEditStartParams params = event.getData();
            eventBus_.fireEvent(new CollabEditStartedEvent(params));
         }
         else if (type.equals(ClientEvent.SessionCountChanged))
         {
            SessionCountChangedEvent.Data data = event.getData();
            eventBus_.fireEvent(new SessionCountChangedEvent(data));
         }
         else if (type.equals(ClientEvent.CollabEditEnded))
         {
            CollabEditEndedEvent.Data data = event.getData();
            eventBus_.fireEvent(new CollabEditEndedEvent(data));
         }
         else if (type.equals(ClientEvent.ProjectUsersChanged))
         {
            ProjectUserChangedEvent.Data data = event.getData();
            eventBus_.fireEvent(new ProjectUserChangedEvent(data));
         }
         else if (type.equals(ClientEvent.RVersionsChanged))
         {
            RVersionsInfo versions = event.getData();
            eventBus_.fireEvent(new RVersionsChangedEvent(versions));
         }
         else if (type.equals(ClientEvent.ShinyGadgetDialog))
         {
            ShinyGadgetDialogEvent.Data data = event.getData();
            eventBus_.fireEvent(new ShinyGadgetDialogEvent(data));
         }
         else if (type.equals(ClientEvent.RmdParamsReady))
         {
            String paramsFile = event.getData();
            eventBus_.fireEvent(new RmdParamsReadyEvent(paramsFile));
         }
         else if (type.equals(ClientEvent.RegisterUserCommand))
         {
            RegisterUserCommandEvent.Data data = event.getData();
            eventBus_.fireEvent(new RegisterUserCommandEvent(data));
         }
         else if (type.equals(ClientEvent.SendToConsole))
         {
            SendToConsoleEvent.Data data = event.getData();
            eventBus_.fireEvent(new SendToConsoleEvent(data));
         }
         else if (type.equals(ClientEvent.UserFollowStarted))
         {
            ProjectUser user = event.getData();
            eventBus_.fireEvent(new FollowUserEvent(user, true));
         }
         else if (type.equals(ClientEvent.UserFollowEnded))
         {
            ProjectUser user = event.getData();
            eventBus_.fireEvent(new FollowUserEvent(user, false));
         }
         else if (type.equals(ClientEvent.ProjectAccessRevoked))
         {
            eventBus_.fireEvent(new ProjectAccessRevokedEvent());
         }
         else if (type.equals(ClientEvent.CollabEditSaved))
         {
            CollabEditSavedEvent.Data data = event.getData();
            eventBus_.fireEvent(new CollabEditSavedEvent(data));
         }
         else if (type.equals(ClientEvent.AddinRegistryUpdated))
         {
            RAddins data = event.getData();
            eventBus_.fireEvent(new AddinRegistryUpdatedEvent(data));
         }
         else if (type.equals(ClientEvent.ChunkOutput))
         {
            RmdChunkOutput data = event.getData();
            eventBus_.fireEvent(new RmdChunkOutputEvent(data));
         }
         else if (type.equals(ClientEvent.ChunkOutputFinished))
         {
            RmdChunkOutputFinishedEvent.Data data = event.getData();
            eventBus_.fireEvent(new RmdChunkOutputFinishedEvent(data));
         }
         else if (type.equals(ClientEvent.RprofStarted))
         {
            eventBus_.fireEvent(new RprofEvent(RprofEvent.RprofEventType.START, null));
         }
         else if (type.equals(ClientEvent.RprofStopped))
         {
            eventBus_.fireEvent(new RprofEvent(RprofEvent.RprofEventType.STOP, null));
         }
         else if (type.equals(ClientEvent.RprofCreated))
         {
            RprofEvent.Data data = event.getData();
            eventBus_.fireEvent(new RprofEvent(RprofEvent.RprofEventType.CREATE, data));
         }
         else if (type.equals(ClientEvent.EditorCommand))
         {
            EditorCommandEvent.Data data = event.getData();
            EditorCommandEvent payload = new EditorCommandEvent(data);
            eventBus_.fireEvent(new EditorCommandDispatchEvent(payload));
         }
         else if (type.equals(ClientEvent.PreviewRmd))
         {
            PreviewRmdEvent.Data data = event.getData();
            eventBus_.fireEvent(new PreviewRmdEvent(data));
         }
         else if (type.equals(ClientEvent.WebsiteFileSaved))
         {
            FileSystemItem fsi = event.getData();
            eventBus_.fireEvent(new WebsiteFileSavedEvent(fsi));
         }
         else if (type.equals(ClientEvent.ChunkPlotRefreshed))
         {
            ChunkPlotRefreshedEvent.Data data = event.getData();
            eventBus_.fireEvent(new ChunkPlotRefreshedEvent(data));
         }
         else if (type.equals(ClientEvent.ChunkPlotRefreshFinished))
         {
            ChunkPlotRefreshFinishedEvent.Data data = event.getData();
            eventBus_.fireEvent(new ChunkPlotRefreshFinishedEvent(data));
         }
         else if (type.equals(ClientEvent.ReloadWithLastChanceSave))
         {
            eventBus_.fireEvent(new ReloadWithLastChanceSaveEvent());
         }
         else if (type.equals(ClientEvent.ConnectionUpdated))
         {
            ConnectionUpdatedEvent.Data data = event.getData();
            eventBus_.fireEvent(new ConnectionUpdatedEvent(data));
         }
         else if (type.equals(ClientEvent.EnableConnections))
         {
            eventBus_.fireEvent(new EnableConnectionsEvent());
         }
         else if (type.equals(ClientEvent.ConnectionListChanged))
         {
            JsArray<Connection> connections = event.getData();
            eventBus_.fireEvent(new ConnectionListChangedEvent(connections));
         }
         else if (type.equals(ClientEvent.ActiveConnectionsChanged))
         {
            JsArray<ConnectionId> connections = event.getData();
            eventBus_.fireEvent(new ActiveConnectionsChangedEvent(connections));
         }
         else if (type.equals(ClientEvent.ConnectionOpened))
         {
            Connection connection = event.getData();
            eventBus_.fireEvent(new ConnectionOpenedEvent(connection));
         }
         else if (type.equals(ClientEvent.NotebookRangeExecuted))
         {
            NotebookRangeExecutedEvent.Data data = event.getData();
            eventBus_.fireEvent(new NotebookRangeExecutedEvent(data));
         }
         else if (type.equals(ClientEvent.ChunkExecStateChanged))
         {
            ChunkExecStateChangedEvent.Data data = event.getData();
            eventBus_.fireEvent(new ChunkExecStateChangedEvent(data));
         }
         else if (type.equals(ClientEvent.NavigateShinyFrame))
         {
            ShinyFrameNavigatedEvent.Data data = event.getData();
            eventBus_.fireEvent(new ShinyFrameNavigatedEvent(data));
         }
         else if (type.equals(ClientEvent.UpdateNewConnectionDialog))
         {
            NewConnectionDialogUpdatedEvent.Data data = event.getData();
            eventBus_.fireEvent(new NewConnectionDialogUpdatedEvent(data));
         }
         else if (type.equals(ClientEvent.ProjectTemplateRegistryUpdated))
         {
            ProjectTemplateRegistry data = event.getData();
            eventBus_.fireEvent(new ProjectTemplateRegistryUpdatedEvent(data));
         }
         else if (type.equals(ClientEvent.TerminalSubProcs))
         {
            TerminalSubprocEvent.Data data = event.getData();
            eventBus_.fireEvent(new TerminalSubprocEvent(data));
         }
         else if (type.equals(ClientEvent.PackageExtensionIndexingCompleted))
         {
            PackageProvidedExtensions.Data data = event.getData();
            eventBus_.fireEvent(new PackageExtensionIndexingCompletedEvent(data));
         }
         else if (type.equals(ClientEvent.RStudioAPIShowDialog))
         {
            RStudioAPIShowDialogEvent.Data data = event.getData();
            eventBus_.fireEvent(new RStudioAPIShowDialogEvent(data));
         }
         else if (type.equals(ClientEvent.ObjectExplorerEvent))
         {
            ObjectExplorerEvent.Data data = event.getData();
            eventBus_.fireEvent(new ObjectExplorerEvent(data));
         }
         else
         {
            GWT.log("WARNING: Server event not dispatched: " + type, null);
         }
      }
      catch(Throwable e)
      {
         GWT.log("WARNING: Exception occured dispatching event: " + type, e);
      }
   }
   

   private final EventBus eventBus_;

   private final ArrayList<ClientEvent> pendingEvents_ = new ArrayList<ClientEvent>();
   

}
