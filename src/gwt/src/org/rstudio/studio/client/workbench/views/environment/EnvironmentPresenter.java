/*
 * EnvironmentPresenter.java
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
package org.rstudio.studio.client.workbench.views.environment;

import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.DebugFilePosition;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.debugging.DebugCommander;
import org.rstudio.studio.client.common.debugging.DebugCommander.DebugMode;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenDataFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenDataFileHandler;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent.NavigationMethod;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.ActivatePaneEvent;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;


import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.views.environment.dataimport.ImportFileSettings;
import org.rstudio.studio.client.workbench.views.environment.dataimport.ImportFileSettingsDialog;
import org.rstudio.studio.client.workbench.views.environment.events.BrowserLineChangedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.ContextDepthChangedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.EnvironmentObjectAssignedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.EnvironmentObjectRemovedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.EnvironmentRefreshEvent;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;
import org.rstudio.studio.client.workbench.views.environment.model.DownloadInfo;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentContextData;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentServerOperations;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentClientState;
import org.rstudio.studio.client.workbench.views.source.SourceShim;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EnvironmentPresenter extends BasePresenter
        implements OpenDataFileHandler

{
   public interface Binder
           extends CommandBinder<Commands, EnvironmentPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void addObject(RObject object);
      void addObjects(JsArray<RObject> objects);
      void clearObjects();
      void clearSelection();
      void setContextDepth(int contextDepth);
      void removeObject(String object);
      void setEnvironmentName(String name);
      void setCallFrames(JsArray<CallFrame> frames);
      int getScrollPosition();
      void setScrollPosition(int scrollPosition);
      void setObjectDisplayType(int type);
      int getObjectDisplayType();
      int getSortColumn();
      boolean getAscendingSort();
      void setSort(int sortColumn, boolean sortAscending);
      void setExpandedObjects(JsArrayString objects);
      String[] getExpandedObjects();
      boolean clientStateDirty();
      void setClientStateClean();
      void resize();
      void setBrowserRange(DebugFilePosition filePosition);
      List<String> getSelectedObjects();
   }
   
   @Inject
   public EnvironmentPresenter(Display view,
                               EnvironmentServerOperations server,
                               Binder binder,
                               Commands commands,
                               GlobalDisplay globalDisplay,
                               EventBus eventBus,
                               FileDialogs fileDialogs,
                               WorkbenchContext workbenchContext,
                               ConsoleDispatcher consoleDispatcher,
                               RemoteFileSystemContext fsContext,
                               Session session,
                               SourceShim sourceShim,
                               DebugCommander debugCommander)
   {
      super(view);
      binder.bind(commands, this);
      
      view_ = view;
      server_ = server;
      globalDisplay_ = globalDisplay;
      consoleDispatcher_ = consoleDispatcher;
      fsContext_ = fsContext;
      fileDialogs_ = fileDialogs;
      workbenchContext_ = workbenchContext;
      eventBus_ = eventBus;
      refreshingView_ = false;
      initialized_ = false;
      currentBrowseFile_ = "";
      currentBrowsePosition_ = null;
      sourceShim_ = sourceShim;
      debugCommander_ = debugCommander;
      session_ = session;

      eventBus.addHandler(EnvironmentRefreshEvent.TYPE,
                          new EnvironmentRefreshEvent.Handler()
      {
         @Override
         public void onEnvironmentRefresh(EnvironmentRefreshEvent event)
         {
            refreshView();
         }
      });
      
      eventBus.addHandler(ContextDepthChangedEvent.TYPE, 
                          new ContextDepthChangedEvent.Handler()
      {
         @Override
         public void onContextDepthChanged(ContextDepthChangedEvent event)
         {
            loadNewContextState(event.getContextDepth(), 
                  event.getEnvironmentName(),
                  event.getCallFrames(),
                  event.useProvidedSource(),
                  event.getFunctionCode());
            setViewFromEnvironmentList(event.getEnvironmentList());
         }
      });
      
      eventBus.addHandler(EnvironmentObjectAssignedEvent.TYPE,
                          new EnvironmentObjectAssignedEvent.Handler() 
      {
         @Override
         public void onEnvironmentObjectAssigned(EnvironmentObjectAssignedEvent event)
         {
            view_.addObject(event.getObjectInfo());
         }
      });

      eventBus.addHandler(EnvironmentObjectRemovedEvent.TYPE,
            new EnvironmentObjectRemovedEvent.Handler() 
      {
         @Override
         public void onEnvironmentObjectRemoved(EnvironmentObjectRemovedEvent event)
         {
            view_.removeObject(event.getObjectName());
         }
      });

      eventBus.addHandler(BrowserLineChangedEvent.TYPE,
            new BrowserLineChangedEvent.Handler()
      {
         @Override
         public void onBrowserLineChanged(BrowserLineChangedEvent event)
         {
            if (currentBrowsePosition_.compareTo(event.getRange()) != 0)
            {
               currentBrowsePosition_ = event.getRange();
               view_.setBrowserRange(currentBrowsePosition_);
               openOrUpdateFileBrowsePoint(true);
            }
         }
      });
      
      new JSObjectStateValue(
              "environment-panel",
              "environmentPanelSettings",
              ClientState.TEMPORARY,
              session.getSessionInfo().getClientState(),
              false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (value != null)
            {
               EnvironmentClientState clientState = value.cast();
               view_.setScrollPosition(clientState.getScrollPosition());
               view_.setExpandedObjects(clientState.getExpandedObjects());
               view_.setSort(clientState.getSortColumn(), 
                             clientState.getAscendingSort());
            }
         }

         @Override
         protected JsObject getValue()
         {
            // the state object we're about to create will be persisted, so
            // our state is clean until the user makes more changes.
            view_.setClientStateClean();
            return EnvironmentClientState.create(view_.getScrollPosition(),
                                                 view_.getExpandedObjects(),
                                                 view_.getSortColumn(),
                                                 view_.getAscendingSort())
                                         .cast();
         }

         @Override
         protected boolean hasChanged()
         {
            return view_.clientStateDirty();
         }
      };
      
      // Store the object display type more permanently than the other 
      // client state settings; it's likely to be a user preference. 
      new IntStateValue(
              "environment-grid",
              "objectDisplayType",
              ClientState.PERSISTENT,
              session.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(Integer value)
         {
            if (value != null)
               view_.setObjectDisplayType(value);
         }

         @Override
         protected Integer getValue()
         {
            return view_.getObjectDisplayType();
         }
      };
   }

   @Handler
   void onRefreshEnvironment()
   {
      refreshView();
   }
   
   void onClearWorkspace()
   {
      view_.bringToFront();
      final List<String> objectNames = view_.getSelectedObjects();

      new ClearAllDialog(objectNames.size(), 
                         new ProgressOperationWithInput<Boolean>() {

         @Override
         public void execute(Boolean includeHidden, ProgressIndicator indicator)
         {
            indicator.onProgress("Removing objects...");
            if (objectNames.size() == 0)
            {
               server_.removeAllObjects(
                       includeHidden,
                       new VoidServerRequestCallback(indicator) {
                           @Override
                           public void onSuccess()
                           {
                              view_.clearSelection();
                              view_.clearObjects();
                           }
                       });
            }
            else
            {
               server_.removeObjects(
                       objectNames, 
                       new VoidServerRequestCallback(indicator) {
                           @Override
                           public void onSuccess()
                           {
                              view_.clearSelection();
                              for (String obj: objectNames)
                              {
                                 view_.removeObject(obj);
                              }
                           }
                       });
            }
         }
      }).showModal();
   }

   void onSaveWorkspace()
   {
      view_.bringToFront();

      consoleDispatcher_.saveFileAsThenExecuteCommand("Save Workspace As",
                                                      ".RData",
                                                      true,
                                                      "save.image");
   }

   void onLoadWorkspace()
   {
      view_.bringToFront();
      consoleDispatcher_.chooseFileThenExecuteCommand("Load Workspace", "load");
   }

   void onImportDatasetFromFile()
   {
      view_.bringToFront();
      fileDialogs_.openFile(
              "Select File to Import",
              fsContext_,
              workbenchContext_.getCurrentWorkingDir(),
              new ProgressOperationWithInput<FileSystemItem>()
              {
                 public void execute(
                         FileSystemItem input,
                         ProgressIndicator indicator)
                 {
                    if (input == null)
                       return;

                    indicator.onCompleted();

                    showImportFileDialog(input, null);
                 }
              });
   }

   void onImportDatasetFromURL()
   {
      view_.bringToFront();
      globalDisplay_.promptForText(
              "Import from Web URL" ,
              "Please enter the URL to import data from:",
              "",
              new ProgressOperationWithInput<String>(){
                 public void execute(String input, final ProgressIndicator indicator)
                 {
                    indicator.onProgress("Downloading data...");
                    server_.downloadDataFile(input.trim(),
                        new ServerRequestCallback<DownloadInfo>(){

                           @Override
                           public void onResponseReceived(DownloadInfo downloadInfo)
                           {
                              indicator.onCompleted();
                              showImportFileDialog(
                                FileSystemItem.createFile(downloadInfo.getPath()),
                                downloadInfo.getVarname());
                           }

                           @Override
                           public void onError(ServerError error)
                           {
                              indicator.onError(error.getUserMessage());
                           }

                        });
                 }
              });
   }

   public void onOpenDataFile(OpenDataFileEvent event)
   {
      final String dataFilePath = event.getFile().getPath();
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
           "Confirm Load Workspace",

           "Do you want to load the R data file \"" + dataFilePath + "\" " +
           "into your workspace?",

           new ProgressOperation() {
              public void execute(ProgressIndicator indicator)
              {
                 consoleDispatcher_.executeCommand(
                         "load",
                         FileSystemItem.createFile(dataFilePath));

                 indicator.onCompleted();
              }
           },

           true);
   }

   @Override
   public void onBeforeSelected()
   {
      super.onBeforeSelected();

      // if the view isn't yet initialized, initialize it with the list of 
      // objects in the environment
      if (!initialized_)
      {
         setViewFromEnvironmentList(
            session_.getSessionInfo().getEnvironmentState().environmentList());
         initialized_ = true;
      }
   }

   @Override
   public void onSelected()
   {
      super.onSelected();

      // GWT sometimes gets a 0-height layout cached on tab switch; resize the
      // tab after selection to ensure it's filling its space.
      if (initialized_)
      {
         view_.resize();
      }
   }

   public void initialize(EnvironmentContextData environmentState)
   {
      loadNewContextState(environmentState.contextDepth(),
            environmentState.environmentName(),
            environmentState.callFrames(),
            environmentState.useProvidedSource(),
            environmentState.functionCode());
      setViewFromEnvironmentList(environmentState.environmentList());
      initialized_ = true;
   }
   
   public void setContextDepth(int contextDepth)
   {
      // if entering debug state, activate this tab 
      if (contextDepth > 0 &&
          contextDepth_ == 0)
      {
         eventBus_.fireEvent(new ActivatePaneEvent("Environment"));
         debugCommander_.enterDebugMode(DebugMode.Function);
      }
      // if leaving debug mode, let everyone know
      else if (contextDepth == 0 &&
               contextDepth_ > 0)
      {
         debugCommander_.leaveDebugMode();
      }
      contextDepth_ = contextDepth;
      view_.setContextDepth(contextDepth_);
   }

   // Private methods ---------------------------------------------------------

   private void loadNewContextState(int contextDepth, 
         String environmentName,
         JsArray<CallFrame> callFrames,
         boolean useBrowseSources,
         String functionCode)
   {
      setContextDepth(contextDepth);
      environmentName_ = environmentName;
      view_.setEnvironmentName(environmentName_);
      if (callFrames != null && 
          callFrames.length() > 0 &&
          contextDepth > 0)
      {
         view_.setCallFrames(callFrames);
         CallFrame browseFrame = callFrames.get(
                 contextDepth_ - 1);
         String newBrowseFile = browseFrame.getFileName().trim();
         
         // check to see if the file we're about to switch to contains unsaved
         // changes. if it does, use the source supplied by the server, even if
         // the server thinks the document is clean.
         if (fileContainsUnsavedChanges(newBrowseFile))
         {
            useBrowseSources = true;
         }
         
         // if the file is different or we're swapping into or out of the source
         // viewer, turn off highlighting in the old file before turning it on
         // in the new one. avoid this in the case where the file is different
         // but both frames are viewed from source, since in this case an
         // unnecessary close and reopen of the source viewer would be 
         // triggered.
         if ((!newBrowseFile.equals(currentBrowseFile_) ||
                  useBrowseSources != useCurrentBrowseSource_) &&
             !(useBrowseSources && useCurrentBrowseSource_))
         {
            openOrUpdateFileBrowsePoint(false);
         }

         useCurrentBrowseSource_ = useBrowseSources;
         currentBrowseSource_ = functionCode;
         
         // highlight the active line in the file now being debugged
         currentBrowseFile_ = newBrowseFile;
         currentBrowsePosition_ = browseFrame.getRange();
         currentFunctionLineNumber_ = browseFrame.getFunctionLineNumber();
         openOrUpdateFileBrowsePoint(true);
      }   
      else
      {
         openOrUpdateFileBrowsePoint(false);
         useCurrentBrowseSource_ = false;
         currentBrowseSource_ = "";
         currentBrowseFile_ = "";
         currentBrowsePosition_ = null;
         currentFunctionLineNumber_ = 0;
      }
   }
   
   // given a path, indicate whether it corresponds to a file that currently
   // contains unsaved changes.
   private boolean fileContainsUnsavedChanges(String path)
   {
      ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
         sourceShim_.getUnsavedChanges();
      
      for (UnsavedChangesTarget target: unsavedSourceDocs)
      {
         if (target.getPath().equals(path))
         {
            return true;
         }
      }     
      
      return false;
   }
   
   private void openOrUpdateFileBrowsePoint(boolean debugging)
   {
      String file = currentBrowseFile_;
      
      // if we have no file and no source code, we can do no navigation 
      if (!CallFrame.isNavigableFilename(file) &&
          !useCurrentBrowseSource_)
      {
         return;
      }
      
      // if we have a real filename and sign from the server that the file 
      // is in sync with the actual copy of the function, navigate to the
      // file itself
      if (currentBrowsePosition_ != null &&
          !useCurrentBrowseSource_)
      {
         // If we're finished debugging this file and it has an active top-level
         // debug session, let that session take over line highlighting instead
         // of turning it off ourselves. 
         if (!debugging &&
             debugCommander_.hasTopLevelDebugSession(currentBrowseFile_))
         {
            return;
         }
         FileSystemItem sourceFile = FileSystemItem.createFile(file);
         eventBus_.fireEvent(new OpenSourceFileEvent(sourceFile,
                                (FilePosition) currentBrowsePosition_.cast(),
                                FileTypeRegistry.R,
                                debugging ? 
                                      (contextDepth_ == 1 ?
                                         NavigationMethod.DebugStep :
                                         NavigationMethod.DebugFrame)
                                      :
                                      NavigationMethod.DebugEnd));

      }
      // otherwise, if we have a copy of the source from the server, load
      // the copy from the server into the code browser window
      else if (useCurrentBrowseSource_ &&
               currentBrowseSource_.length() > 0)
      {
         if (debugging)
         {
            eventBus_.fireEvent(new CodeBrowserNavigationEvent(
                  SearchPathFunctionDefinition.create(
                        environmentName_, 
                        "source unavailable or out of sync", 
                        currentBrowseSource_,
                        true),
                  currentBrowsePosition_.functionRelativePosition(
                        currentFunctionLineNumber_),
                  contextDepth_ == 1));
         }
         else
         {
            eventBus_.fireEvent(new CodeBrowserFinishedEvent());
         }
      }
   }

   private void setViewFromEnvironmentList(JsArray<RObject> objects)
   {
      view_.clearObjects();
      view_.addObjects(objects);
   }
    
   private void refreshView()
   {
      // if we're currently waiting for a view refresh to come back, don't
      // queue another server request
      if (refreshingView_)
      {
         return;
      }
      // start showing the progress spinner and initiate the request
      view_.setProgress(true);
      refreshingView_ = true;
      server_.getEnvironmentState(
            new ServerRequestCallback<EnvironmentContextData>()
      {

         @Override
         public void onResponseReceived(EnvironmentContextData data)
         {
            view_.setProgress(false);
            refreshingView_ = false;
            initialized_ = true;
            eventBus_.fireEvent(new ContextDepthChangedEvent(data));
         }

         @Override
         public void onError(ServerError error)
         {
            if (!workbenchContext_.isRestartInProgress())
            {
               globalDisplay_.showErrorMessage("Error Listing Objects",
                                               error.getUserMessage());
            }
            view_.setProgress(false);
            refreshingView_ = false;
         }
      });
   }

   private void showImportFileDialog(FileSystemItem input, String varname)
   {
      ImportFileSettingsDialog dialog = new ImportFileSettingsDialog(
              server_,
              input,
              varname,
              "Import Dataset",
              new OperationWithInput<ImportFileSettings>()
              {
                 public void execute(
                         ImportFileSettings input)
                 {
                    String var = StringUtil.toRSymbolName(input.getVarname());
                    String code =
                            var +
                            " <- " +
                            makeCommand(input) +
                            "\n  View(" + var + ")";
                    eventBus_.fireEvent(new SendToConsoleEvent(code, true));
                 }
              },
              globalDisplay_);
      dialog.showModal();
   }

   private String makeCommand(ImportFileSettings input)
   {
      HashMap<String, ImportFileSettings> commandDefaults_ =
              new HashMap<String, ImportFileSettings>();

      commandDefaults_.put("read.table", new ImportFileSettings(
              null, null, false, "", ".", "\"'"));
      commandDefaults_.put("read.csv", new ImportFileSettings(
              null, null, true, ",", ".", "\""));
      commandDefaults_.put("read.delim", new ImportFileSettings(
              null, null, true, "\t", ".", "\""));
      commandDefaults_.put("read.csv2", new ImportFileSettings(
              null, null, true, ";", ",", "\""));
      commandDefaults_.put("read.delim2", new ImportFileSettings(
              null, null, true, "\t", ",", "\""));

      String command = "read.table";
      ImportFileSettings settings = commandDefaults_.get("read.table");
      int score = settings.calculateSimilarity(input);
      for (String cmd : new String[] {"read.csv", "read.delim"})
      {
         ImportFileSettings theseSettings = commandDefaults_.get(cmd);
         int thisScore = theseSettings.calculateSimilarity(input);
         if (thisScore > score)
         {
            score = thisScore;
            command = cmd;
            settings = theseSettings;
         }
      }

      StringBuilder code = new StringBuilder(command);
      code.append("(");
      code.append(StringUtil.textToRLiteral(input.getFile().getPath()));
      if (input.isHeader() != settings.isHeader())
         code.append(", header=" + (input.isHeader() ? "T" : "F"));
      if (!input.getSep().equals(settings.getSep()))
         code.append(", sep=" + StringUtil.textToRLiteral(input.getSep()));
      if (!input.getDec().equals(settings.getDec()))
         code.append(", dec=" + StringUtil.textToRLiteral(input.getDec()));
      if (!input.getQuote().equals(settings.getQuote()))
         code.append(", quote=" + StringUtil.textToRLiteral(input.getQuote()));
      code.append(")");

      return code.toString();
   }

   private final Display view_;
   private final EnvironmentServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final RemoteFileSystemContext fsContext_;
   private final WorkbenchContext workbenchContext_;
   private final FileDialogs fileDialogs_;
   private final EventBus eventBus_;
   private final SourceShim sourceShim_;
   private final DebugCommander debugCommander_;
   private final Session session_;
   
   private int contextDepth_;
   private boolean refreshingView_;
   private boolean initialized_;
   private DebugFilePosition currentBrowsePosition_;
   private int currentFunctionLineNumber_;
   private String currentBrowseFile_;
   private boolean useCurrentBrowseSource_;
   private String currentBrowseSource_;
   private String environmentName_;
}
