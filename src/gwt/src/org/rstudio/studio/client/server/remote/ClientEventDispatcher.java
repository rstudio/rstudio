/*
 * ClientEventDispatcher.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.compile.CompileError;
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
import org.rstudio.studio.client.common.debugging.events.PackageLoadedEvent;
import org.rstudio.studio.client.common.debugging.events.PackageUnloadedEvent;
import org.rstudio.studio.client.common.rpubs.events.RPubsUploadStatusEvent;
import org.rstudio.studio.client.common.synctex.events.SynctexEditFileEvent;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewCompletedEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewOutputEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewStartedEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.model.OpenProjectError;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.*;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildCompletedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildErrorsEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildOutputEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildStartedEvent;
import org.rstudio.studio.client.workbench.views.choosefile.events.ChooseFileEvent;
import org.rstudio.studio.client.workbench.views.console.events.*;
import org.rstudio.studio.client.workbench.views.console.model.ConsolePrompt;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleResetHistory;
import org.rstudio.studio.client.workbench.views.data.events.ViewDataEvent;
import org.rstudio.studio.client.workbench.views.data.model.DataView;
import org.rstudio.studio.client.workbench.views.edit.events.ShowEditorEvent;
import org.rstudio.studio.client.workbench.views.edit.model.ShowEditorData;
import org.rstudio.studio.client.workbench.views.environment.events.*;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.history.events.HistoryEntriesAddedEvent;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.output.find.events.FindOperationEndedEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindResultEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.events.SourceCppCompletedEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.events.SourceCppStartedEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.model.SourceCppState;
import org.rstudio.studio.client.workbench.views.packages.events.InstalledPackagesChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.LoadedPackageUpdatesEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStatusChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.model.PackageStatus;
import org.rstudio.studio.client.workbench.views.plots.events.LocatorEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsZoomSizeChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsState;
import org.rstudio.studio.client.workbench.views.presentation.events.ShowPresentationPaneEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.source.events.FileEditEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowContentEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowDataEvent;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.vcs.common.events.AskPassEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;

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
            String output = event.getData();
            eventBus_.fireEvent(new ConsoleWriteOutputEvent(output));
         }
         else if (type.equals(ClientEvent.ConsoleError))
         {
            String error = event.getData();
            eventBus_.fireEvent(new ConsoleWriteErrorEvent(error));
         }
         else if (type.equals(ClientEvent.ConsoleWritePrompt))
         {
            String prompt = event.getData();
            eventBus_.fireEvent(new ConsoleWritePromptEvent(prompt));
         }
         else if (type.equals(ClientEvent.ConsoleWriteInput))
         {
            String input = event.getData();
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
         else if (type.equals(ClientEvent.InstalledPackagesChanged))
         {
            eventBus_.fireEvent(new InstalledPackagesChangedEvent());
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
                                                            data.getOutput(),
                                                            data.isError()));
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
            String output = event.getData();
            eventBus_.fireEvent(new CompilePdfOutputEvent(output));
         }
         else if (type.equals(ClientEvent.CompilePdfErrorsEvent))
         {
            JsArray<CompileError> data = event.getData();
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
            ContextDepthChangedEvent.ContextData data = event.getData();
            eventBus_.fireEvent(new ContextDepthChangedEvent(data));
         }
         else if (type.equals(ClientEvent.HandleUnsavedChanges))
         {
            eventBus_.fireEvent(new HandleUnsavedChangesEvent());
         }
         else if (type.equals(ClientEvent.Quit))
         {
            boolean switchProjects = event.<Bool>getData().getValue();
            eventBus_.fireEvent(new QuitEvent(switchProjects));
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