/*
 * Workspace.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.workspace;


import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.events.OpenDataFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenDataFileHandler;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.workspace.dataimport.ImportFileSettings;
import org.rstudio.studio.client.workbench.views.workspace.dataimport.ImportFileSettingsDialog;
import org.rstudio.studio.client.workbench.views.workspace.events.*;
import org.rstudio.studio.client.workbench.views.workspace.model.DownloadInfo;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceObjectInfo;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceServerOperations;
import org.rstudio.studio.client.workbench.views.workspace.table.WorkspaceObjectTable;

import java.util.ArrayList;
import java.util.HashMap;

public class Workspace
      extends BasePresenter
   implements WorkspaceObjectAssignedHandler,
              WorkspaceObjectRemovedHandler,
              IsWidget,
              WorkspaceRefreshHandler,
              OpenDataFileHandler,
              WorkspaceObjectTable.Observer
{
   public interface Binder extends CommandBinder<Commands, Workspace> {}

   public interface Display extends WorkbenchView
   {
      WorkspaceObjectTable getWorkspaceObjectTable();
   }

   @Inject
   public Workspace(Workspace.Display view,
                    EventBus eventBus,
                    WorkspaceServerOperations server,
                    GlobalDisplay globalDisplay,
                    FileDialogs fileDialogs,
                    WorkbenchContext workbenchContext,
                    ConsoleDispatcher consoleDispatcher,
                    RemoteFileSystemContext fsContext,
                    Commands commands,
                    Binder binder)
   {
      super(view);
      binder.bind(commands, this);
      fileDialogs_ = fileDialogs;
      view_ = view ;
      eventBus_ = eventBus;
      server_ = server;
      globalDisplay_ = globalDisplay ;
      workbenchContext_ = workbenchContext;
      consoleDispatcher_ = consoleDispatcher;
      fsContext_ = fsContext;
      objects_ = view_.getWorkspaceObjectTable();

      objects_.setObserver(this);
      eventBus_.addHandler(WorkspaceRefreshEvent.TYPE, this);
      eventBus_.addHandler(WorkspaceObjectAssignedEvent.TYPE, this);
      eventBus_.addHandler(WorkspaceObjectRemovedEvent.TYPE, this);
   }
   
   @Override
   public void onBeforeSelected()
   {
      super.onBeforeSelected();
      synchronizeView();
   }

   public void onWorkspaceRefresh(WorkspaceRefreshEvent event)
   {
      synchronizeView();
   }

   public void onWorkspaceObjectAssigned(WorkspaceObjectAssignedEvent event)
   {
      WorkspaceObjectInfo objectInfo = event.getObjectInfo();
      if (!objectInfo.isHidden())
         objects_.updateObject(objectInfo);
   }

   public void onWorkspaceObjectRemoved(WorkspaceObjectRemovedEvent event)
   {
      objects_.removeObject(event.getObjectName());
   }

   public void editObject(String objectName)
   {
      executeFunctionForObject("fix", objectName);
   }

   public void viewObject(String objectName)
   {
      executeFunctionForObject("View", objectName);
   }
 
   private void executeFunctionForObject(String function, String objectName)
   {
      String editCode = function + "(" + StringUtil.toRSymbolName(objectName) + ")";
      SendToConsoleEvent event = new SendToConsoleEvent(editCode, true);
      eventBus_.fireEvent(event);
   }

   @Handler
   void onRefreshWorkspace()
   {
      refreshView();
   }

   void onClearWorkspace()
   {
      view_.bringToFront();
      
      new ClearAllDialog(new ProgressOperationWithInput<Boolean>() {

         @Override
         public void execute(Boolean includeHidden, ProgressIndicator indicator)
         {
            indicator.onProgress("Removing objects...");
            server_.removeAllObjects(
                  includeHidden,
                  new VoidServerRequestCallback(indicator));
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

   private void synchronizeView()
   {
      refreshView(false);
   }
   
   private void refreshView()
   {
      refreshView(true);
   }
   
   private void refreshView(final boolean reset)
   {  
      // show progress if we are doing a full reset
      final boolean showProgress = reset;
      if (showProgress)
         view_.setProgress(true);
      
      // clean out existing if we doing a reseta 
      if (reset)
         objects_.clearObjects();
       
      server_.listObjects(new ServerRequestCallback<RpcObjectList<WorkspaceObjectInfo>>()
      {
         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Error Listing Objects",
                                           error.getUserMessage());
            
            
            if (showProgress)
               view_.setProgress(false);
         }

         @Override
         public void onResponseReceived(RpcObjectList<WorkspaceObjectInfo> response)
         {
            // if this is not a full reset then we need to perform the 
            // deletes manually because we never cleared the existing
            // object table. this state is here so we can implement "silent"
            // refreshes of the workspace that don't flash and reset the
            // user's scroll position
            if (!reset)
            {
               ArrayList<String> objectNames = objects_.getObjectNames();
               for (int i=0; i<objectNames.size(); i++)
               {
                  String objectName = objectNames.get(i);
                  if (!containsObject(response, objectName))
                     objects_.removeObject(objectName);
               }
            }
            
            // perform updates (will add or update as necessary)
            for (int i = 0; i < response.length(); i++)
            {
               WorkspaceObjectInfo objectInfo = response.get(i);
               if (!objectInfo.isHidden())
                  objects_.updateObject(objectInfo);
            } 
            
            if (showProgress)
               view_.setProgress(false);
         }
      });
   }
   
   
   public boolean containsObject(RpcObjectList<WorkspaceObjectInfo> objects,
                                 String name)
   {
      for (int i=0; i<objects.length(); i++)
      {
         if (objects.get(i).getName().equals(name))
            return true;
      }
      
      // didn't find it
      return false;
   }

   private final Workspace.Display view_ ;
   private final WorkspaceServerOperations server_;
   private final GlobalDisplay globalDisplay_ ;
   private final EventBus eventBus_;
   private final WorkspaceObjectTable objects_;
   private final WorkbenchContext workbenchContext_;
   private final RemoteFileSystemContext fsContext_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final FileDialogs fileDialogs_;
}
