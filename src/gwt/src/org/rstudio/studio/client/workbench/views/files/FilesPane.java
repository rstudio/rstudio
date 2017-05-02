/*
 * FilesPane.java
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
package org.rstudio.studio.client.workbench.views.files;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.cellview.ColumnSortInfo;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.files.model.PendingFileUpload;
import org.rstudio.studio.client.workbench.views.files.ui.*;

import java.util.ArrayList;

public class FilesPane extends WorkbenchPane implements Files.Display
{
   @Inject
   public FilesPane(GlobalDisplay globalDisplay,
                    FileDialogs fileDialogs,
                    Commands commands,
                    FileTypeRegistry fileTypeRegistry,
                    Session session,
                    Provider<FileCommandToolbar> pFileCommandToolbar)
   {
      super("Files");
      globalDisplay_ = globalDisplay ;
      commands_ = commands;
      fileDialogs_ = fileDialogs;
      fileTypeRegistry_ = fileTypeRegistry;
      pFileCommandToolbar_ = pFileCommandToolbar;
      session_ = session;
      ensureWidget();
   }
   
   public void setObserver(Files.Display.Observer observer)
   {
      observer_ = observer;
   }
   
   // implement forwarding (and null-safe) observer for sub-components
   private class DisplayObserverProxy implements Files.Display.Observer 
   {
      public void onFileSelectionChanged()
      {
         if (observer_ != null)
            observer_.onFileSelectionChanged();
         
      }
      public void onFileNavigation(FileSystemItem file)
      {
         if (observer_ != null)
            observer_.onFileNavigation(file);
      }
      
      public void onSelectAllValueChanged(boolean value)
      {
         if (observer_ != null)
            observer_.onSelectAllValueChanged(value);
      }
      
      public void onColumnSortOrderChanaged(
                                    JsArray<ColumnSortInfo> sortOrder)
      {
         if (observer_ != null)
            observer_.onColumnSortOrderChanaged(sortOrder);
      }
   }
   
   @Override
   public void setColumnSortOrder(JsArray<ColumnSortInfo> sortOrder)
   {
      filesList_.setColumnSortOrder(sortOrder);
   }
    
   public void listDirectory(final FileSystemItem directory, 
                             ServerDataSource<DirectoryListing> dataSource)
   {
      setProgress(true);
        
      dataSource.requestData(new ServerRequestCallback<DirectoryListing>(){
         public void onResponseReceived(DirectoryListing response)
         {
            setProgress(false);
            String lastBrowseable = null;
            if (!response.isParentBrowseable())
            {
               // if we can't go up, disable everything up to the current path
               lastBrowseable = directory.getPath();
            }
            else
            {
               // if we're in someone else's project, disable paths above
               // the project
               SessionInfo si = session_.getSessionInfo();
               if (si.getActiveProjectDir() != null && !si.projectParentBrowseable())
                  lastBrowseable = si.getActiveProjectDir().getPath();
            }
               
            filePathToolbar_.setPath(directory.getPath(), lastBrowseable);
            filesList_.displayFiles(directory, response.getFiles()); 
         }
         public void onError(ServerError error)
         {
            setProgress(false);
            globalDisplay_.showErrorMessage("File Listing Error",
                                            "Error navigating to " +
                                            directory.getPath() + ":\n\n" +
                                            error.getUserMessage());

            if (!directory.equalTo(FileSystemItem.home()))
            {
               observer_.onFileNavigation(FileSystemItem.home());
            }
         } 
      });
   }
   
   public void updateDirectoryListing(FileChange fileAction)
   {
      if (filesList_ != null) // can be called by file_changed event
                             // prior to widget creation
      {
         filesList_.updateWithAction(fileAction);
      }
   }
   
   public void renameFile(FileSystemItem from, FileSystemItem to)
   {
      filesList_.renameFile(from, to);
   }
    
   public void showFolderPicker(
         String caption,
         RemoteFileSystemContext fileSystemContext,
         FileSystemItem initialDir,
         ProgressOperationWithInput<FileSystemItem> operation)
   {
      fileDialogs_.chooseFolder(caption,
                                fileSystemContext,
                                initialDir,
                                operation);
   }
   
   public void showFilePicker(
         String caption,
         RemoteFileSystemContext fileSystemContext,
         FileSystemItem initialFile,
         ProgressOperationWithInput<FileSystemItem> operation)
   {
      fileDialogs_.saveFile(caption, 
            fileSystemContext, 
            initialFile, 
            initialFile.getExtension(), false, 
            operation);
   }
   
   public void showFileUpload(
                     String targetURL,
                     FileSystemItem targetDirectory, 
                     RemoteFileSystemContext fileSystemContext,
                     OperationWithInput<PendingFileUpload> completedOperation)
   {
      FileUploadDialog dlg = new FileUploadDialog(targetURL, 
                                                  targetDirectory,
                                                  fileDialogs_,
                                                  fileSystemContext,
                                                  completedOperation);
      dlg.showModal();
   } 
   
   public void selectAll()
   {
      filesList_.selectAll();
   }
   
   public void selectNone()
   {
      filesList_.selectNone();
   }
   
   public ArrayList<FileSystemItem> getSelectedFiles()
   {
      return filesList_.getSelectedFiles();
   } 
   
   @Override
   public void showHtmlFileChoice(FileSystemItem file, 
                                  Command onEdit,
                                  Command onBrowse)
   {
       final ToolbarPopupMenu menu = new ToolbarPopupMenu();
       
       String editLabel = AppCommand.formatMenuLabel(
          commands_.renameFile().getImageResource(), "Open in Editor", null);
       String openLabel = AppCommand.formatMenuLabel(
          commands_.openHtmlExternal().getImageResource(), 
          "View in Web Browser", 
          null);
       
       menu.addItem(new MenuItem(editLabel, true, onEdit));
       menu.addItem(new MenuItem(openLabel, true, onBrowse));
       
       menu.setPopupPositionAndShow(new PositionCallback() {
          @Override
          public void setPosition(int offsetWidth, int offsetHeight)
          {
             Event event = Event.getCurrentEvent();
             PopupPositioner.setPopupPosition(menu, event.getClientX(), event.getClientY());
          }
       });
   }

   @Override
   public void showDataImportFileChoice(FileSystemItem file, 
                                  Command onView,
                                  Command onImport)
   {
       final ToolbarPopupMenu menu = new ToolbarPopupMenu();
       
       String editLabel = AppCommand.formatMenuLabel(
          commands_.renameFile().getImageResource(), "View File", null);
       String importLabel = AppCommand.formatMenuLabel(
          new ImageResource2x(StandardIcons.INSTANCE.import_dataset2x()), 
          "Import Dataset...", 
          null);
       
       menu.addItem(new MenuItem(editLabel, true, onView));
       menu.addItem(new MenuItem(importLabel, true, onImport));
       
       menu.setPopupPositionAndShow(new PositionCallback() {
          @Override
          public void setPosition(int offsetWidth, int offsetHeight)
          {
             Event event = Event.getCurrentEvent();
             PopupPositioner.setPopupPosition(menu, event.getClientX(), event.getClientY());
          }
       });
   }
   
   @Override 
   protected Widget createMainWidget()
   {
      filePathToolbar_ = new FilePathToolbar(new DisplayObserverProxy());

      // create file list and file progress
      filesList_ = new FilesList(new DisplayObserverProxy(), fileTypeRegistry_);

      DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
      dockPanel.addNorth(filePathToolbar_, filePathToolbar_.getHeight());
      dockPanel.add(filesList_);
      dockPanel.addStyleName("ace_editor_theme");
      
      // return container
      return dockPanel;
   }

   @Override
   public void onBeforeSelected()
   {
      if (needsInit)
      {
         needsInit = false;
         FileSystemItem home = FileSystemItem.home();
         observer_.onFileNavigation(home);
      }
      else
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               filesList_.redraw();
            }
         });
      }
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      return pFileCommandToolbar_.get();
   }

   private boolean needsInit = false;
   private FilesList filesList_ ;
   private FilePathToolbar filePathToolbar_;
   private final GlobalDisplay globalDisplay_ ;
   private final FileDialogs fileDialogs_;
   private Files.Display.Observer observer_;
   private final Session session_;

   private final FileTypeRegistry fileTypeRegistry_;
   private final Commands commands_;
   private final Provider<FileCommandToolbar> pFileCommandToolbar_;
}
