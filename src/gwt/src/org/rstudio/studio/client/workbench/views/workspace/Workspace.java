/*
 * Workspace.java
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
package org.rstudio.studio.client.workbench.views.workspace;


import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.events.OpenDataFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenDataFileHandler;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.workspace.dataimport.ImportFileSettings;
import org.rstudio.studio.client.workbench.views.workspace.events.*;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceObjectInfo;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceServerOperations;
import org.rstudio.studio.client.workbench.views.workspace.table.WorkspaceObjectTable;

import java.util.ArrayList;

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

   public void onOpenDataFile(OpenDataFileEvent event)
   {
   }

   private void showImportFileDialog(FileSystemItem input, String varname)
   {
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
            // ignore errors when a restart is in progress
            if (!workbenchContext_.isRestartInProgress())
            {
               globalDisplay_.showErrorMessage("Error Listing Objects",
                                               error.getUserMessage());
            }
            
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
