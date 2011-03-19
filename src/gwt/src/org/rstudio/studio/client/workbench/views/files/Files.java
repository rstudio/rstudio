/*
 * Files.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.WorkbenchEventHelper;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserHandler;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.files.events.ShowFolderEvent;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.files.model.FileSystemItemAction;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.files.model.PendingFileUpload;

import java.util.ArrayList;

public class Files
      extends BasePresenter
      implements FileChangeHandler, OpenFileInBrowserHandler
{
   interface Binder extends CommandBinder<Commands, Files> {}

 
   public interface Display extends WorkbenchView
   {   
      public interface NavigationObserver
      {
         void onFileNavigation(FileSystemItem file);
         void onSelectAllValueChanged(boolean value);
      }
      
      public interface Observer extends NavigationObserver
      {
         void onFileSelectionChanged();
      }
      
      void setObserver(Observer observer);
      
      void listDirectory(FileSystemItem directory, 
                         ServerDataSource<JsArray<FileSystemItem>> filesDS);
      
      void updateDirectoryListing(FileChange action);
      
      void renameFile(FileSystemItem from, FileSystemItem to);
      
      void selectAll();
      void selectNone();
      
      ArrayList<FileSystemItem> getSelectedFiles();
       
      void showFolderPicker(
            String title,
            RemoteFileSystemContext context,
            FileSystemItem initialDir,
            ProgressOperationWithInput<FileSystemItem> operation);
      
      void showFileUpload(
                     String targetURL,
                     FileSystemItem targetDirectory, 
                     RemoteFileSystemContext fileSystemContext,
                     OperationWithInput<PendingFileUpload> completedOperation);
      
      void showFileExport(String defaultName,
                          String defaultExtension,
                          ProgressOperationWithInput<String> operation);
      
      void scrollToBottom();
   }

   @Inject
   public Files(Display view, 
                EventBus eventBus,
                FilesServerOperations server,
                RemoteFileSystemContext fileSystemContext,
                GlobalDisplay globalDisplay,
                Session session,
                Commands commands,
                Provider<FilesCopy> pFilesCopy,
                Provider<FilesUpload> pFilesUpload,
                FileTypeRegistry fileTypeRegistry)
   {
      super(view);
      view_ = view ;
      view_.setObserver(new DisplayObserver());
      fileTypeRegistry_ = fileTypeRegistry;
      
      eventBus_ = eventBus;
      server_ = server;
      fileSystemContext_ = fileSystemContext;
      globalDisplay_ = globalDisplay ;
      session_ = session;
      pFilesCopy_ = pFilesCopy;
      pFilesUpload_ = pFilesUpload;

      ((Binder)GWT.create(Binder.class)).bind(commands, this);

      
      eventBus_.addHandler(FileChangeEvent.TYPE, this);

      initSession();
   }

   private void initSession()
   {
      final SessionInfo sessionInfo = session_.getSessionInfo();
      ClientInitState state = sessionInfo.getClientState();

      // navigate to previous directory (works for resumed case)
      new StringStateValue(MODULE_FILES, KEY_PATH, false, state) {
         @Override
         protected void onInit(final String value)
         {
            DeferredCommand.addCommand(new Command()
            {
               public void execute()
               {
                  FileSystemItem start = value != null
                                    ? FileSystemItem.createDir(value)
                                    : FileSystemItem.createDir(
                                          sessionInfo.getInitialWorkingDir());
                  navigateToDirectory(start);
               }
            });
         }

         @Override
         protected String getValue()
         {
            return currentPath_.getPath();
         }
      };
   }

   public Display getDisplay()
   {
      return view_ ;
   }
   
   // observer for display
   public class DisplayObserver implements Display.Observer {
      
      public void onFileSelectionChanged()
      {    
      }
      
      public void onFileNavigation(FileSystemItem file)
      {
         if (file.isDirectory())
         {
            navigateToDirectory(file);
         }
         else
         {
            fileTypeRegistry_.openFile(file);
         }
      }
      
      public void onSelectAllValueChanged(boolean value)
      {
         if (value)
            view_.selectAll();
         else
            view_.selectNone();
      }
   };
    

   @Handler
   void onRefreshFiles()
   {
      view_.listDirectory(currentPath_, currentPathFilesDS_);
   }

   @Handler
   void onNewTextFile()
   {  
      onNewFile("New Text File", null, null);
   }
   
   @Handler
   void onNewRSourceFile()
   {
      onNewFile("New R Source File", "R", new FileSystemItemAction() {
         public void execute(FileSystemItem file)
         {
            eventBus_.fireEvent(new OpenSourceFileEvent(file, FileTypeRegistry.R));
         }  
      });
   }

   @Handler
   void onNewFolder()
   {
      globalDisplay_.promptForText(
            "New Folder",
            "Please enter the new folder name",
            null,
            new ProgressOperationWithInput<String>()
            {
               public void execute(String input,
                                   final ProgressIndicator progress)
               {
                  progress.onProgress("Creating folder...");

                  String folderPath = currentPath_.completePath(input);
                  FileSystemItem folder = FileSystemItem.createDir(folderPath);

                  server_.createFolder(
                        folder,
                        new VoidServerRequestCallback(progress));
               }
            });
   }

   void onUploadFile()
   {
      pFilesUpload_.get().execute(currentPath_, fileSystemContext_);
   }
   
   @Handler
   void onCopyFile()
   {
      // copy selected files then clear the selection
      pFilesCopy_.get().execute(view_.getSelectedFiles(),
                                currentPath_,
                                new Command() {
                                    public void execute()
                                    {
                                       view_.selectNone();
                                    }});
   }
   
   
   @Handler
   void onMoveFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();
      
      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;

      view_.showFolderPicker(
                        "Choose Folder", 
                        fileSystemContext_,
                        currentPath_,
                        new ProgressOperationWithInput<FileSystemItem>() {

         public void execute(final FileSystemItem targetDir,
                             final ProgressIndicator progress)
         {
            if (targetDir == null)
                  return;
            
            // check to make sure that we aren't moving any folders
            // onto themselves or moving any files into the directory
            // where they currently reside
            for (int i=0; i<selectedFiles.size(); i++)
            {
               FileSystemItem file = selectedFiles.get(i);
               FileSystemItem fileParent = file.getParentPath();
               
               if (file.getPath().equals(targetDir.getPath()) ||
                   fileParent.getPath().equals(targetDir.getPath()))
               {
                  progress.onError("Invalid target folder");
                  return ;
               } 
            }
            
            progress.onProgress("Moving files...");
      
            server_.moveFiles(selectedFiles, 
                              targetDir, 
                              new VoidServerRequestCallback(progress)); 
         }
      });
   }
   

   @Handler
   void onExportFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();
      
      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;
         
      // case: single file which is not a folder 
      if ((selectedFiles.size()) == 1 && !selectedFiles.get(0).isDirectory())
      {
         final FileSystemItem file = selectedFiles.get(0);
         
         view_.showFileExport(file.getStem(),
                              file.getExtension(),
                              new ProgressOperationWithInput<String>(){
            public void execute(String name, ProgressIndicator progress)
            {
               // execute the download (open in a new window)
               globalDisplay_.openWindow(server_.getFileExportUrl(name, file));
               
            }
         });
      }
      
      // case: folder or multiple files
      else
      {
         // determine the default zip file name based on the selection
         String defaultArchiveName;
         if (selectedFiles.size() == 1)
            defaultArchiveName = selectedFiles.get(0).getStem();
         else
            defaultArchiveName = "rstudio-export";
         
         // prompt user
         final String ZIP = ".zip";
         view_.showFileExport(defaultArchiveName,
                              ZIP,
                              new ProgressOperationWithInput<String>(){
            
            public void execute(String archiveName, ProgressIndicator progress)
            {
               // force zip extension in case the user deleted it
               if (!archiveName.endsWith(ZIP))
                  archiveName += ZIP;
               
               // build list of filenames for current selection
               ArrayList<String> filenames = new ArrayList<String>();
               for (FileSystemItem file : selectedFiles)
                  filenames.add(file.getName());
               
               // execute the download (open in a new window)
               globalDisplay_.openWindow(server_.getFileExportUrl(archiveName, 
                                                                  currentPath_, 
                                                                  filenames));
            }
         });
      }
   }

   @Handler
   void onRenameFile()
   {
      // get currently selected files
      ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();
      
      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;
      
      // validation: no more than one file selected
      if  (selectedFiles.size() > 1)
      {
         globalDisplay_.showErrorMessage(
                           "Invalid Selection", 
                           "Please select only one file to rename");
         return ;
      }
      
      // prompt for new file name then execute the rename
      final FileSystemItem file = selectedFiles.get(0);
      globalDisplay_.promptForText("Rename File",
                                   "Please enter the new file name:",
                                   file.getName(),
                                   0,
                                   file.getStem().length(),
                                   null,
                                   new ProgressOperationWithInput<String>() {

                                      public void execute(String input,
                             final ProgressIndicator progress)
         {
            progress.onProgress("Renaming file...");

            String path = file.getParentPath().completePath(input);
            FileSystemItem target ;
            if (file.isDirectory())
               target = FileSystemItem.createDir(path);
            else
               target = FileSystemItem.createFile(path);
              
            // premptively rename in the UI then fallback to refreshing
            // the view if there is an error
            view_.renameFile(file, target);
            
            // execute on the server
            server_.renameFile(file, 
                               target, 
                               new VoidServerRequestCallback(progress) {
                                 @Override
                                 protected void onFailure()
                                 {
                                    onRefreshFiles();
                                 }
                              });        
         }                                
      }); 
   }
   
   @Handler
   void onDeleteFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();
      
      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return ;
      
      // confirm delete then execute it
      globalDisplay_.showYesNoMessage(
                        GlobalDisplay.MSG_QUESTION,
                        "Confirm Delete", 
                        "Are you sure you want to delete the selected files?", 
                        new ProgressOperation() {
                           public void execute(final ProgressIndicator progress)
                           {
                              progress.onProgress("Deleting files...");
                              
                              server_.deleteFiles(
                                    selectedFiles, 
                                    new VoidServerRequestCallback(progress));
                               
                           }
                        },
                       true);
   }

   @Handler
   void onSyncWorkingDir()
   {
      WorkbenchEventHelper.sendSetWdToConsole(currentPath_, eventBus_);
   }

   @Handler
   void onShowFolder()
   {
      eventBus_.fireEvent(new ShowFolderEvent(currentPath_));
   }
   
   public void onFileChange(FileChangeEvent event)
   {
      view_.updateDirectoryListing(event.getFileChange());
   }

   public void onOpenFileInBrowser(OpenFileInBrowserEvent event)
   {
      FileSystemItem file = event.getFile();
      // show the file in a new window if we can get a file url for it
      String fileURL = server_.getFileUrl(file);
      if (fileURL !=  null)
      {
         globalDisplay_.openWindow(fileURL);
      }
   }
  
   private void navigateToDirectory(FileSystemItem directoryEntry)
   {
      currentPath_ = directoryEntry;
      view_.listDirectory(currentPath_, currentPathFilesDS_);
      session_.persistClientState();
   }
   
   private void addFileAndScrollToBottom(FileSystemItem file)
   {
     view_.updateDirectoryListing(FileChange.createAdd(file));
     view_.scrollToBottom();
   }
   
   private void onNewFile(String title, 
                          final String defaultExtension,
                          final FileSystemItemAction successAction)
   {
      globalDisplay_.promptForText(
         title,
         "Please enter the new file name:",
         null,
         new ProgressOperationWithInput<String>() {

            public void execute(String input, final ProgressIndicator progress)
            {
               // append extension if necessary
               if (defaultExtension != null)
               {
                  if (!input.contains("."))
                     input = input.concat("." + defaultExtension);
               }

               // set progress
               progress.onProgress("Creating file...");

               // create file entry
               final FileSystemItem newFile = FileSystemItem.createFile(
                                          currentPath_.completePath(input));

               // call server.
               server_.createFile(
                     newFile,
                     new VoidServerRequestCallback(progress) {

                        // HACK: manually add file entry on success so we can
                        // scroll to the bottom and have the file appear. we will
                        // later also get an add file event from the server but
                        // this will be a no-op
                        @Override
                        protected void onSuccess()
                        {
                           addFileAndScrollToBottom(newFile);

                           if (successAction != null)
                              successAction.execute(newFile);
                        }
                     });
             }
         });
   }

   // data source for listing files on the current path which can 
   // be passed to the files view
   ServerDataSource<JsArray<FileSystemItem>> currentPathFilesDS_ = 
      new ServerDataSource<JsArray<FileSystemItem>>()
      {
         public void requestData(
               ServerRequestCallback<JsArray<FileSystemItem>> requestCallback)
         {
            // pass true to enable monitoring for all calls to list_files
            server_.listFiles(currentPath_, true, requestCallback);
         }
      };

   private final Display view_ ;
   private final FileTypeRegistry fileTypeRegistry_;
   private final FilesServerOperations server_;
   private final EventBus eventBus_;
   private final GlobalDisplay globalDisplay_ ;
   private final RemoteFileSystemContext fileSystemContext_;
   private final Session session_;
   private FileSystemItem currentPath_ = FileSystemItem.home();
   private final Provider<FilesCopy> pFilesCopy_;
   private final Provider<FilesUpload> pFilesUpload_;
   private static final String MODULE_FILES = "module_files";
   private static final String KEY_PATH = "path";
  
}
