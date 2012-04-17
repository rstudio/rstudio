/*
 * FilesPane.java
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
package org.rstudio.studio.client.workbench.views.files;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.cellview.ColumnSortInfo;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.files.model.PendingFileUpload;
import org.rstudio.studio.client.workbench.views.files.ui.*;

import java.util.ArrayList;

public class FilesPane extends WorkbenchPane implements Files.Display
{
   @Inject
   public FilesPane(GlobalDisplay globalDisplay,
                    FileDialogs fileDialogs,
                    FileTypeRegistry fileTypeRegistry,
                    Provider<FileCommandToolbar> pFileCommandToolbar)
   {
      super("Files");
      globalDisplay_ = globalDisplay ;
      fileDialogs_ = fileDialogs;
      fileTypeRegistry_ = fileTypeRegistry;
      pFileCommandToolbar_ = pFileCommandToolbar;
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
                             ServerDataSource<JsArray<FileSystemItem>> dataSource)
   {
      setProgress(true);
        
      dataSource.requestData(new ServerRequestCallback<JsArray<FileSystemItem>>(){
         public void onResponseReceived(JsArray<FileSystemItem> response)
         {
            setProgress(false);
            filePathToolbar_.setPath(directory.getPath());
            filesList_.displayFiles(directory, response); 
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
   protected Widget createMainWidget()
   {
      filePathToolbar_ = new FilePathToolbar(new DisplayObserverProxy());

      // create file list and file progress
      filesList_ = new FilesList(new DisplayObserverProxy(), fileTypeRegistry_);

      DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
      dockPanel.addNorth(filePathToolbar_, filePathToolbar_.getHeight());
      dockPanel.add(filesList_);
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

   private final FileTypeRegistry fileTypeRegistry_;
   private final Provider<FileCommandToolbar> pFileCommandToolbar_;


}
