/*
 * ClientEventDispatcher.java
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


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import org.rstudio.core.client.events.ExecuteAppCommandEvent;
import org.rstudio.core.client.events.HighlightEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.filedialog.events.OpenFileDialogEvent;
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
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.dependencies.events.InstallShinyEvent;
import org.rstudio.studio.client.common.rpubs.events.RPubsUploadStatusEvent;
import org.rstudio.studio.client.common.rstudioapi.events.AskSecretEvent;
import org.rstudio.studio.client.common.rstudioapi.events.RStudioAPIShowDialogEvent;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.common.synctex.events.SynctexEditFileEvent;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.events.EditorCommandDispatchEvent;
import org.rstudio.studio.client.events.EditorCommandEvent;
import org.rstudio.studio.client.events.RStudioApiRequestEvent;
import org.rstudio.studio.client.events.ReticulateEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewCompletedEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewOutputEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewStartedEvent;
import org.rstudio.studio.client.htmlpreview.events.ShowPageViewerEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.packages.events.PackageExtensionIndexingCompletedEvent;
import org.rstudio.studio.client.plumber.events.PlumberAPIStatusEvent;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.projects.events.FollowUserEvent;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.ProjectAccessRevokedEvent;
import org.rstudio.studio.client.projects.events.ProjectTemplateRegistryUpdatedEvent;
import org.rstudio.studio.client.projects.events.ProjectUserChangedEvent;
import org.rstudio.studio.client.projects.events.RequestOpenProjectEvent;
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
import org.rstudio.studio.client.server.model.RequestDocumentCloseEvent;
import org.rstudio.studio.client.server.model.RequestDocumentSaveEvent;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.events.ShinyFrameNavigatedEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.events.AddinRegistryUpdatedEvent;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.*;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UserStateChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.PrefLayer;
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
import org.rstudio.studio.client.workbench.views.jobs.events.JobOutputEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobRefreshEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.JobState;
import org.rstudio.studio.client.workbench.views.jobs.model.JobUpdate;
import org.rstudio.studio.client.workbench.views.output.data.events.DataOutputCompletedEvent;
import org.rstudio.studio.client.workbench.views.output.data.model.DataOutputResult;
import org.rstudio.studio.client.workbench.views.output.find.events.FindOperationEndedEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.ReplaceResultEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.ReplaceProgressEvent;
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
import org.rstudio.studio.client.workbench.views.source.events.AvailablePackagesReadyEvent;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditEndedEvent;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditSavedEvent;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DataViewChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.FileEditEvent;
import org.rstudio.studio.client.workbench.views.source.events.NewDocumentWithCodeEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowContentEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowDataEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceExtendedTypeDetectedEvent;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.terminal.events.ActivateNamedTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.AddTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.ClearTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.RemoveTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.SendToTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalCwdEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSubprocEvent;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialCommandEvent;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialLaunchEvent;
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
         if (type == ClientEvent.Busy)
         {
            boolean busy = event.<Bool>getData().getValue();
            eventBus_.dispatchEvent(new BusyEvent(busy));
         }
         else if (type == ClientEvent.ConsoleOutput)
         {
            ConsoleText output = event.getData();
            eventBus_.dispatchEvent(new ConsoleWriteOutputEvent(output));
         }
         else if (type == ClientEvent.ConsoleError)
         {
            ConsoleText error = event.getData();
            eventBus_.dispatchEvent(new ConsoleWriteErrorEvent(error));
         }
         else if (type == ClientEvent.ConsoleWritePrompt)
         {
            String prompt = event.getData();
            eventBus_.dispatchEvent(new ConsoleWritePromptEvent(prompt));
         }
         else if (type == ClientEvent.ConsoleWriteInput)
         {
            ConsoleText input = event.getData();
            eventBus_.dispatchEvent(new ConsoleWriteInputEvent(input));
         }
         else if (type == ClientEvent.ConsolePrompt)
         {
            ConsolePrompt prompt = event.getData();
            eventBus_.dispatchEvent(new ConsolePromptEvent(prompt));
         }
         else if (type == ClientEvent.ShowEditor)
         {
            ShowEditorData data = event.getData();
            eventBus_.dispatchEvent(new ShowEditorEvent(data));
         }
         else if (type == ClientEvent.FileChanged)
         {
            FileChange fileChange = event.getData();
            eventBus_.dispatchEvent(new FileChangeEvent(fileChange));
         }
         else if (type == ClientEvent.WorkingDirChanged)
         {
            String path = event.getData();
            eventBus_.dispatchEvent(new WorkingDirChangedEvent(path));
         }
         else if (type == ClientEvent.ShowHelp)
         {
            String helpUrl = event.getData();
            eventBus_.dispatchEvent(new ShowHelpEvent(helpUrl));
         }
         else if (type == ClientEvent.ShowErrorMessage)
         {
            ErrorMessage errorMessage = event.getData();
            eventBus_.dispatchEvent(new ShowErrorMessageEvent(errorMessage));
         }
         else if (type == ClientEvent.ChooseFile)
         {
            boolean newFile = event.<Bool>getData().getValue();
            eventBus_.dispatchEvent(new ChooseFileEvent(newFile));
         }
         else if (type == ClientEvent.BrowseUrl)
         {
            BrowseUrlInfo urlInfo = event.getData();
            eventBus_.dispatchEvent(new BrowseUrlEvent(urlInfo));
         }
         else if (type == ClientEvent.PlotsStateChanged)
         {
            PlotsState plotsState = event.getData();
            eventBus_.dispatchEvent(new PlotsChangedEvent(plotsState));
         }
         else if (type == ClientEvent.PackageStateChanged)
         {
            PackageState newState = event.getData();
            eventBus_.dispatchEvent(new PackageStateChangedEvent(newState));
         }
         else if (type == ClientEvent.PackageStatusChanged)
         {
            PackageStatus status = event.getData();
            eventBus_.dispatchEvent(new PackageStatusChangedEvent(status));
         }
         else if (type == ClientEvent.Locator)
         {
            eventBus_.dispatchEvent(new LocatorEvent());
         }
         else if (type == ClientEvent.ConsoleResetHistory)
         {
            ConsoleResetHistory reset = event.getData();
            eventBus_.dispatchEvent(new ConsoleResetHistoryEvent(reset));
         }
         else if (type == ClientEvent.SessionSerialization)
         {
            SessionSerializationAction action = event.getData();
            eventBus_.dispatchEvent(new SessionSerializationEvent(action));
         }
         else if (type == ClientEvent.HistoryEntriesAdded)
         {
            RpcObjectList<HistoryEntry> entries = event.getData();
            eventBus_.dispatchEvent(new HistoryEntriesAddedEvent(entries));
         }
         else if (type == ClientEvent.QuotaStatus)
         {
            QuotaStatus quotaStatus = event.getData();
            eventBus_.dispatchEvent(new QuotaStatusEvent(quotaStatus));
         }
         else if (type == ClientEvent.FileEdit)
         {
            FileSystemItem file = event.getData();
            eventBus_.dispatchEvent(new FileEditEvent(file));
         }
         else if (type == ClientEvent.ShowContent)
         {
            ContentItem content = event.getData();
            eventBus_.dispatchEvent(new ShowContentEvent(content));
         }
         else if (type == ClientEvent.ShowData)
         {
            DataItem data = event.getData();
            eventBus_.dispatchEvent(new ShowDataEvent(data));
         }
         else if (type == ClientEvent.AbendWarning)
         {
            eventBus_.dispatchEvent(new SessionAbendWarningEvent());
         }
         else if (type == ClientEvent.ShowWarningBar)
         {
            eventBus_.dispatchEvent(new ShowWarningBarEvent(event.getData()));
         }
         else if (type == ClientEvent.OpenProjectError)
         {
            OpenProjectError error = event.getData();
            eventBus_.dispatchEvent(new OpenProjectErrorEvent(error));
         }
         else if (type == ClientEvent.VcsRefresh)
         {
            JsObject data = event.getData();
            eventBus_.dispatchEvent(new VcsRefreshEvent(Reason.NA,
                                                    data.getInteger("delay")));
         }
         else if (type == ClientEvent.AskPass)
         {
            AskPassEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new AskPassEvent(data));
         }
         else if (type == ClientEvent.ConsoleProcessOutput)
         {
            ServerConsoleOutputEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ServerConsoleOutputEvent(data.getHandle(),
                                                            data.getOutput()));
         }
         else if (type == ClientEvent.ConsoleProcessPrompt)
         {
            ServerConsolePromptEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ServerConsolePromptEvent(data.getHandle(),
                                                             data.getPrompt()));
         }
         else if (type == ClientEvent.ConsoleProcessCreated)
         {
            ConsoleProcessCreatedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ConsoleProcessCreatedEvent(data));
         }
         else if (type == ClientEvent.ConsoleProcessExit)
         {
            ServerProcessExitEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ServerProcessExitEvent(data.getHandle(),
                                                          data.getExitCode()));
         }
         else if (type == ClientEvent.HTMLPreviewStartedEvent)
         {
            HTMLPreviewStartedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new HTMLPreviewStartedEvent(data));
         }
         else if (type == ClientEvent.HTMLPreviewOutputEvent)
         {
            String output = event.getData();
            eventBus_.dispatchEvent(new HTMLPreviewOutputEvent(output));
         }
         else if (type == ClientEvent.HTMLPreviewCompletedEvent)
         {
            HTMLPreviewResult result = event.getData();
            eventBus_.dispatchEvent(new HTMLPreviewCompletedEvent(result));
         }
         else if (type == ClientEvent.CompilePdfStartedEvent)
         {
            CompilePdfStartedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new CompilePdfStartedEvent(data));
         }
         else if (type == ClientEvent.CompilePdfOutputEvent)
         {
            CompileOutput output = event.getData();
            eventBus_.dispatchEvent(new CompilePdfOutputEvent(output));
         }
         else if (type == ClientEvent.CompilePdfErrorsEvent)
         {
            JsArray<SourceMarker> data = event.getData();
            eventBus_.dispatchEvent(new CompilePdfErrorsEvent(data));
         }
         else if (type == ClientEvent.CompilePdfCompletedEvent)
         {
            CompilePdfResult result = event.getData();
            eventBus_.dispatchEvent(new CompilePdfCompletedEvent(result));
         }
         else if (type == ClientEvent.SynctexEditFile)
         {
            SourceLocation sourceLocation = event.getData();
            eventBus_.dispatchEvent(new SynctexEditFileEvent(sourceLocation));
         }
         else if (type == ClientEvent.FindResult)
         {
            FindResultEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new FindResultEvent(
                  data.getHandle(), data.getResults().toArrayList()));
         }
         else if (type == ClientEvent.FindOperationEnded)
         {
            String data = event.getData();
            eventBus_.dispatchEvent(new FindOperationEndedEvent(data));
         }
         else if (type == ClientEvent.ReplaceResult)
         {
            ReplaceResultEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ReplaceResultEvent(
                   data.getHandle(), data.getResults().toArrayList()));
         }
         else if (type == ClientEvent.ReplaceProgress)
         {
            ReplaceProgressEvent.Data data = event.getData();
            eventBus_.dispatchEvent(
               new ReplaceProgressEvent(data.getTotalReplaceCount(), data.getReplacedCount()));
         }
         else if (type == ClientEvent.RPubsUploadStatus)
         {
            RPubsUploadStatusEvent.Status status = event.getData();
            eventBus_.dispatchEvent(new RPubsUploadStatusEvent(status));
         }
         else if (type == ClientEvent.BuildStarted)
         {
            BuildStartedEvent.Data buildStartedData = event.getData();
            eventBus_.dispatchEvent(new BuildStartedEvent(buildStartedData));
         }
         else if (type == ClientEvent.BuildOutput)
         {
            CompileOutput data = event.getData();
            eventBus_.dispatchEvent(new BuildOutputEvent(data));
         }
         else if (type == ClientEvent.BuildCompleted)
         {
            BuildCompletedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new BuildCompletedEvent(data));
         }
         else if (type == ClientEvent.BuildErrors)
         {
            BuildErrorsEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new BuildErrorsEvent(data));
         }
         else if (type == ClientEvent.DirectoryNavigate)
         {
            DirectoryNavigateEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new DirectoryNavigateEvent(data));
         }
         else if (type == ClientEvent.DeferredInitCompleted)
         {
            eventBus_.dispatchEvent(new DeferredInitCompletedEvent());
         }
         else if (type == ClientEvent.PlotsZoomSizeChanged)
         {
            PlotsZoomSizeChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new PlotsZoomSizeChangedEvent(data));
         }
         else if (type == ClientEvent.SourceCppStarted)
         {
            eventBus_.dispatchEvent(new SourceCppStartedEvent());
         }
         else if (type == ClientEvent.SourceCppCompleted)
         {
            SourceCppState state = event.getData();
            eventBus_.dispatchEvent(new SourceCppCompletedEvent(state));
         }
         else if (type == ClientEvent.LoadedPackageUpdates)
         {
            String installCmd = event.getData();
            eventBus_.dispatchEvent(new LoadedPackageUpdatesEvent(installCmd));
         }
         else if (type == ClientEvent.ActivatePane)
         {
            String pane = event.getData();
            eventBus_.dispatchEvent(new ActivatePaneEvent(pane));
         }
         else if (type == ClientEvent.ShowPresentationPane)
         {
            PresentationState state = event.getData();
            eventBus_.dispatchEvent(new ShowPresentationPaneEvent(state));
         }
         else if (type == ClientEvent.EnvironmentRefresh)
         {
            eventBus_.dispatchEvent(new EnvironmentRefreshEvent());
         }
         else if (type == ClientEvent.ListChanged)
         {
            eventBus_.dispatchEvent(new ListChangedEvent(event.<JsObject>getData()));
         }
         else if (type == ClientEvent.UserPrefsChanged)
         {
            PrefLayer data = event.getData();
            eventBus_.dispatchEvent(new UserPrefsChangedEvent(data));
         }
         else if (type == ClientEvent.UserStateChanged)
         {
            PrefLayer data = event.getData();
            eventBus_.dispatchEvent(new UserStateChangedEvent(data));
         }
         else if (type == ClientEvent.ContextDepthChanged) {
            EnvironmentContextData data = event.getData();
            eventBus_.dispatchEvent(new ContextDepthChangedEvent(data, true));
         }
         else if (type == ClientEvent.HandleUnsavedChanges)
         {
            eventBus_.dispatchEvent(new HandleUnsavedChangesEvent());
         }
         else if (type == ClientEvent.Quit)
         {
            QuitEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new QuitEvent(data));
         }
         else if (type == ClientEvent.Suicide)
         {
            // NOTE: we don't explicitly stop listening for events here
            // for the reasons cited above in ClientEvent.Quit

            // fire event
            String message = event.getData();
            eventBus_.dispatchEvent(new SuicideEvent(message));
         }
         else if (type == ClientEvent.SaveActionChanged)
         {
            SaveAction action = event.getData();
            eventBus_.dispatchEvent(new SaveActionChangedEvent(action));
         }
         else if (type == ClientEvent.EnvironmentAssigned)
         {
            RObject objectInfo = event.getData();
            eventBus_.dispatchEvent(new EnvironmentObjectAssignedEvent(objectInfo));
         }
         else if (type == ClientEvent.EnvironmentRemoved)
         {
            String objectName = event.getData();
            eventBus_.dispatchEvent(new EnvironmentObjectRemovedEvent(objectName));
         }
         else if (type == ClientEvent.EnvironmentChanged)
         {
            EnvironmentChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new EnvironmentChangedEvent(data));
         }
         else if (type == ClientEvent.BrowserLineChanged)
         {
            LineData lineData = event.getData();
            eventBus_.dispatchEvent(new BrowserLineChangedEvent(lineData));
         }
         else if (type == ClientEvent.PackageLoaded)
         {
            eventBus_.dispatchEvent(new PackageLoadedEvent(
                  (String)event.getData()));
         }
         else if (type == ClientEvent.PackageUnloaded)
         {
            eventBus_.dispatchEvent(new PackageUnloadedEvent(
                  (String)event.getData()));
         }
         else if (type == ClientEvent.PresentationPaneRequestCompleted)
         {
            eventBus_.dispatchEvent(new PresentationPaneRequestCompletedEvent());
         }
         else if (type == ClientEvent.UnhandledError)
         {
            UnhandledError err = event.getData();
            eventBus_.dispatchEvent(new UnhandledErrorEvent(err));
         }
         else if (type == ClientEvent.ErrorHandlerChanged)
         {
            ErrorHandlerChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ErrorHandlerChangedEvent(data));
         }
         else if (type == ClientEvent.ViewerNavigate)
         {
            ViewerNavigateEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ViewerNavigateEvent(data));
         }
         else if (type == ClientEvent.SourceExtendedTypeDetected)
         {
            SourceExtendedTypeDetectedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new SourceExtendedTypeDetectedEvent(data));
         }
         else if (type == ClientEvent.ShinyViewer)
         {
            ShinyApplicationParams data = event.getData();
            eventBus_.dispatchEvent(new ShinyApplicationStatusEvent(data, true));
         }
         else if (type == ClientEvent.DebugSourceCompleted)
         {
            DebugSourceResult result = (DebugSourceResult)event.getData();
            eventBus_.dispatchEvent(new DebugSourceCompletedEvent(result));
         }
         else if (type == ClientEvent.RmdRenderStarted)
         {
            RmdRenderStartedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RmdRenderStartedEvent(data));
         }
         else if (type == ClientEvent.RmdRenderOutput)
         {
            CompileOutput data = event.getData();
            eventBus_.dispatchEvent(new RmdRenderOutputEvent(data));
         }
         else if (type == ClientEvent.RmdRenderCompleted)
         {
            RmdRenderResult result = event.getData();
            eventBus_.dispatchEvent(new RmdRenderCompletedEvent(result));
         }
         else if (type == ClientEvent.RmdShinyDocStarted)
         {
            RmdShinyDocInfo docInfo = event.getData();
            eventBus_.dispatchEvent(new RmdShinyDocStartedEvent(docInfo));
         }
         else if (type == ClientEvent.RSConnectDeploymentOutput)
         {
            CompileOutput output = event.getData();
            eventBus_.dispatchEvent(new RSConnectDeploymentOutputEvent(output));
         }
         else if (type == ClientEvent.RSConnectDeploymentCompleted)
         {
            String url = event.getData();
            eventBus_.dispatchEvent(new RSConnectDeploymentCompletedEvent(url));
         }
         else if (type == ClientEvent.RSConnectDeploymentFailed)
         {
            RSConnectDeploymentFailedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RSConnectDeploymentFailedEvent(data));
         }
         else if (type == ClientEvent.UserPrompt)
         {
            UserPrompt prompt = event.getData();
            eventBus_.dispatchEvent(new UserPromptEvent(prompt));
         }
         else if (type == ClientEvent.InstallRtools)
         {
            InstallRtoolsEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new InstallRtoolsEvent(data));
         }
         else if (type == ClientEvent.InstallShiny)
         {
            String userAction = event.getData();
            eventBus_.dispatchEvent(new InstallShinyEvent(userAction));
         }
         else if (type == ClientEvent.SuspendAndRestart)
         {
            SuspendAndRestartEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new SuspendAndRestartEvent(data));
         }
         else if (type == ClientEvent.DataViewChanged)
         {
            DataViewChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new DataViewChangedEvent(data));
         }
         else if (type == ClientEvent.ViewFunction)
         {
            SearchPathFunctionDefinition data = event.getData();
            eventBus_.dispatchEvent(new CodeBrowserNavigationEvent(
                  data, null, false, true));
         }
         else if (type == ClientEvent.MarkersChanged)
         {
            MarkersChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new MarkersChangedEvent(data));
         }
         else if (type == ClientEvent.EnableRStudioConnect)
         {
            EnableRStudioConnectUIEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new EnableRStudioConnectUIEvent(data));
         }
         else if (type == ClientEvent.UpdateGutterMarkers)
         {
            LintEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new LintEvent(data));
         }
         else if (type == ClientEvent.SnippetsChanged)
         {
            SnippetsChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new SnippetsChangedEvent(data));
         }
         else if (type == ClientEvent.JumpToFunction)
         {
            JumpToFunctionEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new JumpToFunctionEvent(data));
         }
         else if (type == ClientEvent.CollabEditStarted)
         {
            CollabEditStartParams params = event.getData();
            eventBus_.dispatchEvent(new CollabEditStartedEvent(params));
         }
         else if (type == ClientEvent.SessionCountChanged)
         {
            SessionCountChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new SessionCountChangedEvent(data));
         }
         else if (type == ClientEvent.SessionLabelChanged)
         {
            SessionLabelChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new SessionLabelChangedEvent(data));
         }
         else if (type == ClientEvent.CollabEditEnded)
         {
            CollabEditEndedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new CollabEditEndedEvent(data));
         }
         else if (type == ClientEvent.ProjectUsersChanged)
         {
            ProjectUserChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ProjectUserChangedEvent(data));
         }
         else if (type == ClientEvent.RVersionsChanged)
         {
            RVersionsInfo versions = event.getData();
            eventBus_.dispatchEvent(new RVersionsChangedEvent(versions));
         }
         else if (type == ClientEvent.ShinyGadgetDialog)
         {
            ShinyGadgetDialogEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ShinyGadgetDialogEvent(data));
         }
         else if (type == ClientEvent.RmdParamsReady)
         {
            String paramsFile = event.getData();
            eventBus_.dispatchEvent(new RmdParamsReadyEvent(paramsFile));
         }
         else if (type == ClientEvent.RegisterUserCommand)
         {
            RegisterUserCommandEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RegisterUserCommandEvent(data));
         }
         else if (type == ClientEvent.SendToConsole)
         {
            SendToConsoleEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new SendToConsoleEvent(data));
         }
         else if (type == ClientEvent.UserFollowStarted)
         {
            ProjectUser user = event.getData();
            eventBus_.dispatchEvent(new FollowUserEvent(user, true));
         }
         else if (type == ClientEvent.UserFollowEnded)
         {
            ProjectUser user = event.getData();
            eventBus_.dispatchEvent(new FollowUserEvent(user, false));
         }
         else if (type == ClientEvent.ProjectAccessRevoked)
         {
            eventBus_.dispatchEvent(new ProjectAccessRevokedEvent());
         }
         else if (type == ClientEvent.CollabEditSaved)
         {
            CollabEditSavedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new CollabEditSavedEvent(data));
         }
         else if (type == ClientEvent.AddinRegistryUpdated)
         {
            RAddins data = event.getData();
            eventBus_.dispatchEvent(new AddinRegistryUpdatedEvent(data));
         }
         else if (type == ClientEvent.ChunkOutput)
         {
            RmdChunkOutput data = event.getData();
            eventBus_.dispatchEvent(new RmdChunkOutputEvent(data));
         }
         else if (type == ClientEvent.ChunkOutputFinished)
         {
            RmdChunkOutputFinishedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RmdChunkOutputFinishedEvent(data));
         }
         else if (type == ClientEvent.RprofStarted)
         {
            eventBus_.dispatchEvent(new RprofEvent(RprofEvent.RprofEventType.START, null));
         }
         else if (type == ClientEvent.RprofStopped)
         {
            eventBus_.dispatchEvent(new RprofEvent(RprofEvent.RprofEventType.STOP, null));
         }
         else if (type == ClientEvent.RprofCreated)
         {
            RprofEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RprofEvent(RprofEvent.RprofEventType.CREATE, data));
         }
         else if (type == ClientEvent.EditorCommand)
         {
            EditorCommandEvent.Data data = event.getData();
            EditorCommandEvent payload = new EditorCommandEvent(data);
            eventBus_.dispatchEvent(new EditorCommandDispatchEvent(payload));
         }
         else if (type == ClientEvent.PreviewRmd)
         {
            PreviewRmdEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new PreviewRmdEvent(data));
         }
         else if (type == ClientEvent.WebsiteFileSaved)
         {
            FileSystemItem fsi = event.getData();
            eventBus_.dispatchEvent(new WebsiteFileSavedEvent(fsi));
         }
         else if (type == ClientEvent.ChunkPlotRefreshed)
         {
            ChunkPlotRefreshedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ChunkPlotRefreshedEvent(data));
         }
         else if (type == ClientEvent.ChunkPlotRefreshFinished)
         {
            ChunkPlotRefreshFinishedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ChunkPlotRefreshFinishedEvent(data));
         }
         else if (type == ClientEvent.ReloadWithLastChanceSave)
         {
            eventBus_.dispatchEvent(new ReloadWithLastChanceSaveEvent());
         }
         else if (type == ClientEvent.ConnectionUpdated)
         {
            ConnectionUpdatedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ConnectionUpdatedEvent(data));
         }
         else if (type == ClientEvent.EnableConnections)
         {
            eventBus_.dispatchEvent(new EnableConnectionsEvent());
         }
         else if (type == ClientEvent.ConnectionListChanged)
         {
            JsArray<Connection> connections = event.getData();
            eventBus_.dispatchEvent(new ConnectionListChangedEvent(connections));
         }
         else if (type == ClientEvent.ActiveConnectionsChanged)
         {
            JsArray<ConnectionId> connections = event.getData();
            eventBus_.dispatchEvent(new ActiveConnectionsChangedEvent(connections));
         }
         else if (type == ClientEvent.ConnectionOpened)
         {
            Connection connection = event.getData();
            eventBus_.dispatchEvent(new ConnectionOpenedEvent(connection));
         }
         else if (type == ClientEvent.NotebookRangeExecuted)
         {
            NotebookRangeExecutedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new NotebookRangeExecutedEvent(data));
         }
         else if (type == ClientEvent.ChunkExecStateChanged)
         {
            ChunkExecStateChangedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ChunkExecStateChangedEvent(data));
         }
         else if (type == ClientEvent.NavigateShinyFrame)
         {
            ShinyFrameNavigatedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ShinyFrameNavigatedEvent(data));
         }
         else if (type == ClientEvent.UpdateNewConnectionDialog)
         {
            NewConnectionDialogUpdatedEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new NewConnectionDialogUpdatedEvent(data));
         }
         else if (type == ClientEvent.ProjectTemplateRegistryUpdated)
         {
            ProjectTemplateRegistry data = event.getData();
            eventBus_.dispatchEvent(new ProjectTemplateRegistryUpdatedEvent(data));
         }
         else if (type == ClientEvent.TerminalSubProcs)
         {
            TerminalSubprocEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new TerminalSubprocEvent(data));
         }
         else if (type == ClientEvent.PackageExtensionIndexingCompleted)
         {
            PackageProvidedExtensions.Data data = event.getData();
            eventBus_.dispatchEvent(new PackageExtensionIndexingCompletedEvent(data));
         }
         else if (type == ClientEvent.RStudioAPIShowDialog)
         {
            RStudioAPIShowDialogEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RStudioAPIShowDialogEvent(data));
         }
         else if (type == ClientEvent.ObjectExplorerEvent)
         {
            ObjectExplorerEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ObjectExplorerEvent(data));
         }
         else if (type == ClientEvent.SendToTerminal)
         {
            SendToTerminalEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new SendToTerminalEvent(data));
         }
         else if (type == ClientEvent.ClearTerminal)
         {
            ClearTerminalEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ClearTerminalEvent(data));
         }
         else if (type == ClientEvent.AddTerminal)
         {
            AddTerminalEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new AddTerminalEvent(data));
         }
         else if (type == ClientEvent.RemoveTerminal)
         {
            RemoveTerminalEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RemoveTerminalEvent(data));
         }
         else if (type == ClientEvent.ActivateTerminal)
         {
            ActivateNamedTerminalEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ActivateNamedTerminalEvent(data));
         }
         else if (type == ClientEvent.TerminalCwd)
         {
            TerminalCwdEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new TerminalCwdEvent(data));
         }
         else if (type == ClientEvent.AdminNotification)
         {
            AdminNotification notification = event.getData();
            eventBus_.dispatchEvent(new AdminNotificationEvent(notification));
         }
         else if (type == ClientEvent.RequestDocumentSave)
         {
            RequestDocumentSaveEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RequestDocumentSaveEvent(data));
         }
         else if (type == ClientEvent.RequestOpenProject)
         {
            RequestOpenProjectEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RequestOpenProjectEvent(data));
         }
         else if (type == ClientEvent.OpenFileDialog)
         {
            OpenFileDialogEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new OpenFileDialogEvent(data));
         }
         else if (type == ClientEvent.ShowPageViewer)
         {
            HTMLPreviewParams params = event.getData();
            eventBus_.dispatchEvent(new ShowPageViewerEvent(params));
         }
         else if (type == ClientEvent.AskSecret)
         {
            AskSecretEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new AskSecretEvent(data));
         }
         else if (type == ClientEvent.JobUpdated)
         {
            JobUpdate data = event.getData();
            eventBus_.dispatchEvent(new JobUpdatedEvent(data));
         }
         else if (type == ClientEvent.JobRefresh)
         {
            JobState data = event.getData();
            eventBus_.dispatchEvent(new JobRefreshEvent(data));
         }
         else if (type == ClientEvent.JobOutput)
         {
            JobOutputEvent.Data output = event.getData();
            eventBus_.dispatchEvent(new JobOutputEvent(output));
         }
         else if (type == ClientEvent.DataOutputCompleted)
         {
            DataOutputResult result = event.getData();
            eventBus_.dispatchEvent(new DataOutputCompletedEvent(result));
         }
         else if (type == ClientEvent.NewDocumentWithCode)
         {
            NewDocumentWithCodeEvent.Data result = event.getData();
            eventBus_.dispatchEvent(new NewDocumentWithCodeEvent(result));
         }
         else if (type == ClientEvent.AvailablePackagesReady)
         {
            AvailablePackagesReadyEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new AvailablePackagesReadyEvent(data));
         }
         else if (type == ClientEvent.PlumberViewer)
         {
            PlumberAPIParams data = event.getData();
            eventBus_.dispatchEvent(new PlumberAPIStatusEvent(data, true));
         }
         else if (type == ClientEvent.ComputeThemeColors)
         {
            eventBus_.dispatchEvent(new ComputeThemeColorsEvent());
         }
         else if (type == ClientEvent.RequestDocumentClose)
         {
            RequestDocumentCloseEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RequestDocumentCloseEvent(data));
         }
         else if (type == ClientEvent.ExecuteAppCommand)
         {
            ExecuteAppCommandEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ExecuteAppCommandEvent(data));
         }
         else if (type == ClientEvent.HighlightUi)
         {
            HighlightEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new HighlightEvent(data));
         }
         else if (type == ClientEvent.TutorialCommand)
         {
            TutorialCommandEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new TutorialCommandEvent(data));
         }
         else if (type == ClientEvent.TutorialLaunch)
         {
            TutorialLaunchEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new TutorialLaunchEvent(data));
         }
         else if (type == ClientEvent.ReticulateEvent)
         {
            ReticulateEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new ReticulateEvent(data));
         }
         else if (type == ClientEvent.RStudioApiRequest)
         {
            RStudioApiRequestEvent.Data data = event.getData();
            eventBus_.dispatchEvent(new RStudioApiRequestEvent(data));
         }
         else
         {
            GWT.log("WARNING: Server event not dispatched: " + type, null);
         }
      }
      catch(Throwable e)
      {
         GWT.log("WARNING: Exception occurred dispatching event: " + type, e);
      }
   }


   private final EventBus eventBus_;

   private final ArrayList<ClientEvent> pendingEvents_ = new ArrayList<ClientEvent>();


}
