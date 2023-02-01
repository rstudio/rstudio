/*
 * Files.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.files;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.ParallelCommandList;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ColumnSortInfo;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.Clipboard;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.fileexport.FileExport;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserEvent;
import org.rstudio.studio.client.common.filetypes.events.RenameSourceFileEvent;
import org.rstudio.studio.client.events.RStudioApiRequestEvent;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.PaneManager;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.environment.dataimport.DataImportPresenter;
import org.rstudio.studio.client.workbench.views.files.events.*;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.files.model.PendingFileUpload;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.events.SourcePathChangedEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocumentResult;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateNewTerminalEvent;

import java.util.ArrayList;
import java.util.List;

public class Files
      extends BasePresenter
      implements FileChangeEvent.Handler,
                 OpenFileInBrowserEvent.Handler,
                 DirectoryNavigateEvent.Handler,
                 RenameSourceFileEvent.Handler,
                 RStudioApiRequestEvent.Handler,
                 WorkingDirChangedEvent.Handler
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
         void onColumnSortOrderChanaged(JsArray<ColumnSortInfo> sortOrder);
      }

      void setObserver(Observer observer);

      void resetColumnWidths();
      void setColumnSortOrder(JsArray<ColumnSortInfo> sortOrder);

      void listDirectory(FileSystemItem directory,
                         ServerDataSource<DirectoryListing> filesDS);

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

      void showFilePicker(
            String title,
            RemoteFileSystemContext context,
            FileSystemItem initialFile,
            ProgressOperationWithInput<FileSystemItem> operation);

      void showFileUpload(
                     String targetURL,
                     FileSystemItem targetDirectory,
                     RemoteFileSystemContext fileSystemContext,
                     Operation beginOperation,
                     OperationWithInput<PendingFileUpload> completedOperation,
                     Operation failedOperation);


      void showHtmlFileChoice(FileSystemItem file,
                              Command onEdit,
                              Command onBrowse);

      void showDataImportFileChoice(FileSystemItem file,
                                    Command onView,
                                    Command onImport);

      void bringToFront();
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
                Provider<FileExport> pFileExport,
                Provider<UserPrefs> pPrefs,
                FileTypeRegistry fileTypeRegistry,
                ConsoleDispatcher consoleDispatcher,
                WorkbenchContext workbenchContext,
                PaneManager paneManager,
                DataImportPresenter dataImportPresenter)
   {
      super(view);
      view_ = view;
      view_.setObserver(new DisplayObserver());
      fileTypeRegistry_ = fileTypeRegistry;
      consoleDispatcher_ = consoleDispatcher;
      workbenchContext_ = workbenchContext;

      eventBus_ = eventBus;
      server_ = server;
      fileSystemContext_ = fileSystemContext;
      globalDisplay_ = globalDisplay;
      session_ = session;
      pFilesCopy_ = pFilesCopy;
      pFilesUpload_ = pFilesUpload;
      pFileExport_ = pFileExport;
      pPrefs_ = pPrefs;
      paneManager_ = paneManager;
      dataImportPresenter_ = dataImportPresenter;

      ((Binder)GWT.create(Binder.class)).bind(commands, this);


      eventBus_.addHandler(FileChangeEvent.TYPE, this);
      eventBus_.addHandler(RenameSourceFileEvent.TYPE, this);
      eventBus_.addHandler(RStudioApiRequestEvent.TYPE, this);
      eventBus_.addHandler(WorkingDirChangedEvent.TYPE, this);

      initSession();
   }

   private void initSession()
   {
      final SessionInfo sessionInfo = session_.getSessionInfo();
      ClientInitState state = sessionInfo.getClientState();

      // make the column sort order persistent
      new JSObjectStateValue(MODULE_FILES, KEY_SORT_ORDER, ClientState.PROJECT_PERSISTENT, state, false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (value != null)
               columnSortOrder_ = value.cast();
            else
               columnSortOrder_ = null;

            lastKnownState_ = columnSortOrder_;

            view_.setColumnSortOrder(columnSortOrder_);
         }

         @Override
         protected JsObject getValue()
         {
            if (columnSortOrder_ != null)
               return columnSortOrder_.cast();
            else
               return null;
         }

         @Override
         protected boolean hasChanged()
         {
            if (lastKnownState_ != columnSortOrder_)
            {
               lastKnownState_ = columnSortOrder_;
               return true;
            }
            else
            {
               return false;
            }
         }

         private JsArray<ColumnSortInfo> lastKnownState_ = null;
      };


      // navigate to previous directory (works for resumed case)
      new StringStateValue(MODULE_FILES, KEY_PATH, ClientState.PROJECT_PERSISTENT, state) {
         @Override
         protected void onInit(final String value)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  // if we've already navigated to a directory then
                  // we don't need to initialize from client state
                  if (hasNavigatedToDirectory_)
                     return;

                  // compute start dir
                  String path = transformPathStateValue(value);
                  FileSystemItem start = path != null
                                    ? FileSystemItem.createDir(path)
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

         private String transformPathStateValue(String value)
         {
            // if the value is null then return null
            if (value == null)
               return null;

            // only respect the value for projects
            String projectFile = session_.getSessionInfo().getActiveProjectFile();
            if (projectFile == null)
               return null;

            // ensure that the value is within the project dir (it wouldn't
            // be if the project directory has been moved or renamed)
            String projectDirPath =
               FileSystemItem.createFile(projectFile).getParentPathString();
            if (value.startsWith(projectDirPath))
               return value;
            else
               return null;
         }
      };

      // register handler for show hidden file state change
      pPrefs_.get().showHiddenFiles().addValueChangeHandler(show ->
      {
         // refresh when pref value changes to show/hide hidden files
         onRefreshFiles();
      });

      // register handler for sync pane pref, so that when the user enables the pref we can sync immediately
      // to show it's working
      pPrefs_.get().syncFilesPaneWorkingDir().addValueChangeHandler(sync ->
      {
         // ignore at startup/haven't navigated yet
         if (!hasNavigatedToDirectory_)
            return;

         // if we are now syncing and we know the current working path, go there
         if (sync.getValue() && workingPath_ != null)
         {
            if (!currentPath_.equalTo(workingPath_))
            {
               eventBus_.fireEvent(new DirectoryNavigateEvent(workingPath_, false));
            }
         }
      });
   }


   public Display getDisplay()
   {
      return view_;
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
            navigateToFile(file);
         }
      }

      public void onSelectAllValueChanged(boolean value)
      {
         if (value)
            view_.selectAll();
         else
            view_.selectNone();
      }

      public void onColumnSortOrderChanaged(
                                    JsArray<ColumnSortInfo> sortOrder)
      {
         columnSortOrder_ = sortOrder;
      }
   }


   @Handler
   void onRefreshFiles()
   {
      view_.resetColumnWidths();
      view_.listDirectory(currentPath_, currentPathFilesDS_);
   }

   @Handler
   void onNewFolder()
   {
      globalDisplay_.promptForText(
              constants_.newFolderTitle(),
              constants_.newFolderNameLabel(),
            null,
            new ProgressOperationWithInput<String>()
            {
               public void execute(String input,
                                   final ProgressIndicator progress)
               {
                  progress.onProgress(constants_.creatingFolderProgressLabel());

                  String folderPath = currentPath_.completePath(input);
                  FileSystemItem folder = FileSystemItem.createDir(folderPath);

                  server_.createFolder(
                        folder,
                        new VoidServerRequestCallback(progress));
               }
            });
   }
   
   @Handler
   void onTouchSourceDoc() {
      touchFile(FileTypeRegistry.R);
   }

   @Handler
   void onTouchRMarkdownDoc() {
      touchFile(FileTypeRegistry.RMARKDOWN);
   }

   @Handler
   void onTouchQuartoDoc() {
      touchFile(FileTypeRegistry.QUARTO);
   }

   @Handler
   void onTouchTextDoc() {
      touchFile(FileTypeRegistry.TEXT);
   }

   @Handler
   void onTouchCppDoc() {
      touchFile(FileTypeRegistry.CPP);
   }

   @Handler
   void onTouchPythonDoc() {
      touchFile(FileTypeRegistry.PYTHON);
   }

   @Handler
   void onTouchSqlDoc() {
      touchFile(FileTypeRegistry.SQL);
   }

   @Handler
   void onTouchStanDoc() {
      touchFile(FileTypeRegistry.STAN);
   }

   @Handler
   void onTouchD3Doc() {
      touchFile(FileTypeRegistry.JS);
   }

   @Handler
   void onTouchSweaveDoc() {
      touchFile(FileTypeRegistry.SWEAVE);
   }

   @Handler
   void onTouchRHTMLDoc() {
      touchFile(FileTypeRegistry.RHTML);
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
   void onCopyFileTo()
   {
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();

      // validate selection size
      if (selectedFiles.size() == 0)
         return;

      if (selectedFiles.size() > 1)
      {
         globalDisplay_.showErrorMessage(
                           constants_.multipleItemsSelectedCaption(),
                           constants_.multipleItemsSelectedMessage());
         return;
      }

      FileSystemItem initialFile = selectedFiles.get(0);
      if (initialFile.isDirectory())
         initialFile = initialFile.getParentPath();

      view_.showFilePicker(
              constants_.chooseDestinationTitle(),
                        fileSystemContext_,
                        initialFile,
                        new ProgressOperationWithInput<FileSystemItem>() {

         public void execute(FileSystemItem targetFile,
                             final ProgressIndicator progress)
         {
            if (targetFile == null)
               return;

            if (targetFile.getPath() == "~")
            {
               globalDisplay_.showErrorMessage(
                     constants_.invalidDestinationCaption(),
                     constants_.invalidDestinationErrorMessage());
               return;
            }

            if (StringUtil.isNullOrEmpty(targetFile.getExtension()))
            {
               targetFile = FileSystemItem.createFile(
                     targetFile.getPath() + selectedFiles.get(0).getExtension());
            }

            server_.copyFile(selectedFiles.get(0),
                 targetFile,
                 true,
                 new VoidServerRequestCallback(progress) {
                     @Override
                     protected void onSuccess()
                     {
                        view_.selectNone();
                     }
                  });
         }
      });
   }


   @Handler
   void onMoveFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();

      // validation: some selection exists
      if  (selectedFiles.size() == 0)
         return;

      // validation -- not prohibited move of public folder
      if (!validateNotRestrictedFolder(selectedFiles, "moved"))
         return;

      view_.showFolderPicker(
                        constants_.chooseFolderTitle(),
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

               if (file.getPath() == targetDir.getPath() ||
                   fileParent.getPath() == targetDir.getPath())
               {
                  progress.onError(constants_.invalidTargetFolderErrorMessage());
                  return;
               }
            }

            progress.onProgress(constants_.movingFilesLabel());

            view_.selectNone();

            server_.moveFiles(selectedFiles,
                              targetDir,
                              new VoidServerRequestCallback(progress));
         }
      });
   }


   @Handler
   void onExportFiles()
   {
      pFileExport_.get().export(constants_.exportFilesCaption(),
                                constants_.selectedFilesCaption(),
                                currentPath_,
                                view_.getSelectedFiles());
   }

   @Handler
   void onRenameFile()
   {
      // get currently selected files
      ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();

      // validation: some selection exists
      if (selectedFiles.size() != 1)
      {
         globalDisplay_.showErrorMessage(
               constants_.invalidSelectionCaption(),
               constants_.invalidSelectionMessage());
         return;
      }

      // validation -- not prohibited move of public folder
      if (!validateNotRestrictedFolder(selectedFiles, "renamed"))
         return;

      // perform the rename
      final FileSystemItem file = selectedFiles.get(0);
      renameFile(file);
   }

   @Handler
   void onDeleteFiles()
   {
      // get currently selected files
      final ArrayList<FileSystemItem> selectedFiles = view_.getSelectedFiles();

      // validation: some selection exists
      String message = constants_.permanentDeleteMessage();
      if (selectedFiles.size() == 0)
      {
         return;
      }
      else if (selectedFiles.size() == 1)
      {
         message += selectedFiles.get(0).getName();
      }
      else
      {
         message += constants_.selectedFilesMessage(selectedFiles.size());
      }
      message += constants_.cannotBeUndoneMessage();


      // validation -- not prohibited move of public folder
      if (!validateNotRestrictedFolder(selectedFiles, "deleted"))
         return;

      // confirm delete then execute it
      globalDisplay_.showYesNoMessage(
                        GlobalDisplay.MSG_QUESTION,
                        constants_.confirmDeleteCaption(),
                        message,
                        new ProgressOperation() {
                           public void execute(final ProgressIndicator progress)
                           {
                              progress.onProgress(constants_.deletingFilesLabel());

                              view_.selectNone();

                              server_.deleteFiles(
                                    selectedFiles,
                                    new VoidServerRequestCallback(progress));
                           }
                        },
                       true);
   }

   private boolean validateNotRestrictedFolder(ArrayList<FileSystemItem> files,
                                               String verb)
   {
      if (!session_.getSessionInfo().getAllowRemovePublicFolder())
      {
         for (FileSystemItem file : files)
         {
            if (file.isPublicFolder())
            {
               globalDisplay_.showErrorMessage(
                     constants_.errorCaption(),
                     constants_.publicFolderMessage(verb));
               return false;
            }
         }
      }

      return true;
   }

   void onGoToWorkingDir()
   {
      view_.bringToFront();
      navigateToDirectory(workbenchContext_.getCurrentWorkingDir());
   }

   void onCopyFilesPaneCurrentDirectory()
   {
      Clipboard.setText(currentPath_.getPath());
   }

   /**
   * the purpose of this function is specifically to pre-screen selected files for the following:
   *  - the file must NOT be a directory
   *  - the file must NOT already be open in a source editor 
   *  - if the file is an RNotebook (.nb[.html]), then make sure that only ONE .RMarkdown (.Rmd) 
   *    file is selected
   *  - aggregate any errors encountered opening RNotebooks and display them at once
   *
   * @param onCompleted a callback passed {@code <List<FileSystemItem>>}, which is the screened 
   *                    list of selected files
   */
   private void getUnopenedSelectedFiles(final CommandWithArg<List<FileSystemItem>> onCompleted)
   {
      final SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();
      final ArrayList<String> selectedFilePaths = new ArrayList<>(); 

      final List<String> errors = new ArrayList<>();

      // Use parallelCommandList to execute these requests as fast as possible in parallel, asynchronously
      ParallelCommandList commandList = new ParallelCommandList(new Command()
      {
         @Override
         public void execute() 
         {
            if (errors.size() > 0)
            {
               String caption = constants_.errorOpeningFilesCaption();
               String errorMsg = constants_.fileErrorMessage(errors.size());
               errorMsg += constants_.errorMessage();
               for (String err : errors) 
               {
                  errorMsg += "\n" + err;
               }
               globalDisplay_.showErrorMessage(caption, errorMsg);
               return;
            }
            final ArrayList<FileSystemItem> selectedFiles = new ArrayList<>(); 
            selectedFilePaths.forEach((String path) -> {
               selectedFiles.add(FileSystemItem.createFile(path));
            });
            onCompleted.execute(selectedFiles);
         }
      });


      final List<FileSystemItem> notebooks = new ArrayList<>();

      // pre-process to make sure there aren't any directories, and that already-open files do not count
      // towards the column limit. Take notebooks out for additional processing.
      for (FileSystemItem item : view_.getSelectedFiles()) 
      {
         if (!item.isDirectory() && !mgr.openFileAlreadyOpen(item, null))
         {
            TextFileType fileType = fileTypeRegistry_.getTextTypeForFile(item);
            if (fileType.isRNotebook())
               notebooks.add(item);
            else
               selectedFilePaths.add(item.getPath());
         }
      }

      for (FileSystemItem notebook : notebooks)
      {
         commandList.addCommand(new SerializedCommand()
         {
            @Override
            public void onExecute(final Command continuation)
            {
               final String rnbPath = notebook.getPath();
               final String rmdPath = FilePathUtils.filePathSansExtension(rnbPath) + ".Rmd";

               mgr.extractRmdFile(
                     notebook,
                     new ResultCallback<SourceDocumentResult, ServerError>()
                     {
                        @Override
                        public void onSuccess(SourceDocumentResult doc)
                        {
                           // this means the operation succeeded; add ONLY the Rmd file to the list
                           // if it is not already open in a source editor
                           final FileSystemItem rmdFile = FileSystemItem.createFile(rmdPath);
                           if (!selectedFilePaths.contains(rmdPath)  &&
                               !mgr.openFileAlreadyOpen(rmdFile, null))
                              selectedFilePaths.add(rmdPath);

                           continuation.execute();
                        }

                        @Override
                        public void onFailure(ServerError error)
                        {
                           String message = constants_.failedToOpenMessage(notebook.getName(), error.getUserMessage());
                           errors.add(message);
                           continuation.execute();
                        }
                     });
            }
         });
      }

      commandList.run();
   }

   @Handler
   void onOpenFilesInSinglePane()
   {
      // getUnopenedSelectedFiles aggregates RNotebook errors together, so use
      // it to get the selected files instead. Otherwise any RNotebook-related
      // opening errors stack over each other unpleasantly
      getUnopenedSelectedFiles(new CommandWithArg<List<FileSystemItem>>()
      {
         @Override
         public void execute(List<FileSystemItem> selectedFiles)
         {
            final SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();

            for (FileSystemItem item : selectedFiles) 
            {
               if (!item.isDirectory())
               {
                  mgr.openFile(item);
               }
            }

            view_.selectNone();
         }
      });
   }


   @Handler
   void onOpenEachFileInColumns()
   {
      getUnopenedSelectedFiles(new CommandWithArg<List<FileSystemItem>>()
      {
         @Override
         public void execute(List<FileSystemItem> selectedFiles)
         {
            final SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();

            if (selectedFiles.size() == 0)
            {
               view_.selectNone();
               return;
            }

            // only open available:
            // get the current number of remaining columns available
            // all of the remaining files that cannot fit will be dumped into the the last-opened column
            // there is a +1 here because the columnList includes the "non-additional" source pane
            final int columnsRemaining = PaneManager.MAX_COLUMN_COUNT - mgr.getColumnList().size() + 1; 

            // if there aren't any remaining cols then just open the files anyway and see what happens
            if (columnsRemaining <= 0) 
            {
               for (FileSystemItem item : selectedFiles) 
               {
                  mgr.openFile(item);
               }
               return;
            }

            final List<FileSystemItem> openInColumns = 
               selectedFiles.subList(0, Math.min(selectedFiles.size(), columnsRemaining));

            // open these asynchronously IN-ORDER
            SerializedCommandQueue openCommands = new SerializedCommandQueue();

            for (FileSystemItem item : openInColumns)
            {
               openCommands.addCommand(new SerializedCommand()
               {
                  @Override
                  public void onExecute(final Command continuation)
                  {
                     paneManager_.openFileInNewColumn(item, continuation);
                  }
               });
            }

            // open the remaining selected files in whichever column happens to be active at that point
            openCommands.addCommand(new SerializedCommand()
            {
               @Override
               public void onExecute(final Command continuation)
               {
                  if (columnsRemaining < selectedFiles.size()) 
                  {
                     final List<FileSystemItem> openRegular = selectedFiles.subList(columnsRemaining, selectedFiles.size());

                     for (FileSystemItem item : openRegular) 
                     {
                        mgr.openFile(item);
                     }
                  }
                  continuation.execute();
               }
            });

            openCommands.run();
            view_.selectNone();
         }
      });
   }

   @Handler
   void onSetAsWorkingDir()
   {
      consoleDispatcher_.executeSetWd(currentPath_, true);
   }

   @Handler
   void onOpenNewTerminalAtFilePaneLocation()
   {
      eventBus_.fireEvent(new CreateNewTerminalEvent(currentPath_));
   }

   void onSetWorkingDirToFilesPane()
   {
      onSetAsWorkingDir();
   }

   @Handler
   void onShowFolder()
   {
      String path = server_.resolveAliasedPath(currentPath_);
      FileSystemItem resolvedCurrentDir = FileSystemItem.createDir(path);
      eventBus_.fireEvent(new ShowFolderEvent(resolvedCurrentDir));
   }

   public void onFileChange(FileChangeEvent event)
   {
      view_.updateDirectoryListing(event.getFileChange());
   }

   public void onOpenFileInBrowser(OpenFileInBrowserEvent event)
   {
      showFileInBrowser(event.getFile());
   }

   public void onDirectoryNavigate(DirectoryNavigateEvent event)
   {
      navigateToDirectory(event.getDirectory());
      if (event.getActivate())
         view_.bringToFront();
   }

   @Override
   public void onWorkingDirChanged(WorkingDirChangedEvent event)
   {
      // save the current working directory
      workingPath_ = FileSystemItem.createDir(event.getPath());

      // don't listen to these until we've actually loaded
      if (!hasNavigatedToDirectory_)
      {
         return;
      }

      // if enabled via pref, navigate to directories in the Files pane when the working directory changes
      if (pPrefs_.get().syncFilesPaneWorkingDir().getValue())
      {
         if (!currentPath_.equalTo(workingPath_))
         {
            eventBus_.fireEvent(new DirectoryNavigateEvent(workingPath_, false));
         }
      }
   }

   @Override
   public void onRenameSourceFile(RenameSourceFileEvent event)
   {
      renameFile(FileSystemItem.createFile(event.getPath()));
   }

   @Override
   public void onRStudioApiRequest(RStudioApiRequestEvent requestEvent)
   {
      RStudioApiRequestEvent.Data requestData = requestEvent.getData();

      if (requestData.getType() == RStudioApiRequestEvent.TYPE_FILES_PANE_NAVIGATE)
      {
         RStudioApiRequestEvent.FilesPaneNavigateData data = requestData.getPayload().cast();
         String path = data.getPath();
         navigateToDirectory(FileSystemItem.createDir(path));
      }

   }

   private void navigateToDirectory(FileSystemItem directoryEntry)
   {
      hasNavigatedToDirectory_ = true;
      currentPath_ = directoryEntry;
      view_.listDirectory(currentPath_, currentPathFilesDS_);
      session_.persistClientState();
   }

   private void navigateToFile(final FileSystemItem file)
   {
      final String ext = file.getExtension().toLowerCase();
      if (ext.equals(".htm") || ext.equals(".html") || ext.equals(".nb.html"))
      {
         view_.showHtmlFileChoice(
            file,
            new Command() {

               @Override
               public void execute()
               {
                  fileTypeRegistry_.openFile(file);
               }
            },
            new Command() {

               @Override
               public void execute()
               {
                  showFileInBrowser(file);
               }
            });
      }
      else if (ext.equals(".csv") ||
               ext.equals(".xls") || ext.equals(".xlsx") ||
               ext.equals(".sav") || ext.equals(".dta") || ext.equals("por") ||
               ext.equals(".sas") || ext.equals(".stata"))
      {
         view_.showDataImportFileChoice(
            file,
            new Command() {

               @Override
               public void execute()
               {
                  fileTypeRegistry_.openFile(file);
               }
            },
            new Command() {

               @Override
               public void execute()
               {
                  if (ext.equals(".csv")) {
                     dataImportPresenter_.openImportDatasetFromCSV(file.getPath());
                  }
                  else if (ext.equals(".xls") || ext.equals(".xlsx")) {
                     dataImportPresenter_.openImportDatasetFromXLS(file.getPath());
                  }
                  else {
                     dataImportPresenter_.openImportDatasetFromSAV(file.getPath());
                  }
               }
            });
      }
      else
      {
         fileTypeRegistry_.openFile(file);
      }

   }

   private void showFileInBrowser(FileSystemItem file)
   {
      // show the file in a new window if we can get a file url for it
      String fileURL = server_.getFileUrl(file);
      if (fileURL != null)
      {
         if (!Desktop.isRemoteDesktop())
         {
            globalDisplay_.openWindow(fileURL);
         }
         else
         {
            Desktop.getFrame().browseUrl(fileURL);
         }
      }
   }

   // generate a filename based on the file type and currentPath_ variable
   private FileSystemItem getDefaultFileName(TextFileType fileType) 
   {
      String defaultExt = fileType.getDefaultExtension();

      // extension has a '.' at the start, so remove that in the default name
      String newFileDefaultName = "Untitled" + defaultExt.toUpperCase().substring(1) + defaultExt;
      String path = currentPath_.completePath(newFileDefaultName);
      FileSystemItem newTempFile = FileSystemItem.createFile(path);
      return newTempFile;
   }
   
   private void touchFile(final TextFileType fileType)
   {
      // prepare default information about the new file
      FileSystemItem newTempFile = getDefaultFileName(fileType);
      String formattedExt = fileType.getDefaultExtension().toUpperCase().substring(1);
      
      // guard for reentrancy
      if (inputPending_)
         return;
      
      inputPending_ = true;

      // prompt for new file name then execute the operation
      globalDisplay_.promptForText(constants_.createNewFileTitle(formattedExt),
                                   constants_.enterFileNameLabel(),
                                   newTempFile.getName(),
                                   0,
                                   newTempFile.getStem().length(),
                                   null,
                                   new ProgressOperationWithInput<String>()
      {
         public void execute(final String input, final ProgressIndicator progress)
         {
            // no longer waiting for user to input
            inputPending_ = false;

            progress.onProgress(constants_.creatingFileLabel());

            String path = currentPath_.completePath(input);
            final FileSystemItem newFile = FileSystemItem.createFile(path);

            // execute on the server
            server_.touchFile(newFile, new VoidServerRequestCallback(progress)
            {
               @Override
               protected void onSuccess()
               {
                  // if we were successful, refresh list and open in source editor
                  onRefreshFiles();
                  fileTypeRegistry_.openFile(newFile);
               }

               @Override
               public void onError(ServerError error)
               {
                  String errCaption = constants_.blankFileFailedCaption();
                  String errMsg = constants_.blankFileFailedMessage(fileType.getDefaultExtension(), input, error.getUserMessage());
                  globalDisplay_.showErrorMessage(errCaption, errMsg);
                  progress.onCompleted();
               }
            });
         }
      },
      () ->
      {
         // clear pending input flag when operation is canceled
         inputPending_ = false;
      });
   }

   private void renameFile(FileSystemItem file)
   {
      // guard for reentrancy
      if (inputPending_)
         return;
      
      inputPending_ = true;

      // prompt for new file name then execute the rename
      globalDisplay_.promptForText(constants_.renameFileTitle(),
                                   constants_.renameFileCaption(),
                                   file.getName(),
                                   0,
                                   file.getStem().length(),
                                   null,
                                   new ProgressOperationWithInput<String>() {
        public void execute(String input,
                            final ProgressIndicator progress)
        {
            // no longer waiting for user to rename
            inputPending_ = false;

            progress.onProgress(constants_.renamingFileProgressMessage());

            String path = file.getParentPath().completePath(input);
            final FileSystemItem target =
               file.isDirectory() ?
                  FileSystemItem.createDir(path) :
                  FileSystemItem.create(path, false, file.getLength(), file.getLastModifiedNative());

            // clear selection
            view_.selectNone();

            // preemptively rename in the UI then fallback to refreshing
            // the view if there is an error
            view_.renameFile(file, target);

            // execute on the server
            server_.renameFile(file,
                               target,
                               new VoidServerRequestCallback(progress) {
                                 @Override
                                 protected void onSuccess()
                                 {
                                    // if we were successful, let editor know
                                    if (!file.isDirectory())
                                    {
                                       eventBus_.fireEvent(
                                             new SourcePathChangedEvent(
                                                   file.getPath(),
                                                   target.getPath()));
                                    }
                                 }
                                 @Override
                                 protected void onFailure()
                                 {
                                    onRefreshFiles();
                                 }
                              });
         }
      },
      () ->
      {
         // clear rename flag when operation is canceled
         inputPending_ = false;
      });
   }

   // data source for listing files on the current path which can
   // be passed to the files view
   ServerDataSource<DirectoryListing> currentPathFilesDS_ =
      new ServerDataSource<DirectoryListing>()
      {
         public void requestData(
               ServerRequestCallback<DirectoryListing> requestCallback)
         {

            server_.listFiles(currentPath_,
                  true, // pass true to enable monitoring for all calls to list_files
                  pPrefs_.get().showHiddenFiles().getValue(), // respect user pref for showing hidden
                  requestCallback);
         }
      };

   private final Display view_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final WorkbenchContext workbenchContext_;
   private final FilesServerOperations server_;
   private final EventBus eventBus_;
   private final GlobalDisplay globalDisplay_;
   private final RemoteFileSystemContext fileSystemContext_;
   private final Session session_;
   private FileSystemItem currentPath_ = FileSystemItem.home();
   private FileSystemItem workingPath_;
   private boolean hasNavigatedToDirectory_ = false;
   private final Provider<FilesCopy> pFilesCopy_;
   private final Provider<FilesUpload> pFilesUpload_;
   private final Provider<FileExport> pFileExport_;
   private final Provider<UserPrefs> pPrefs_;
   private static final String MODULE_FILES = "files-pane";
   private static final String KEY_PATH = "path";
   private static final String KEY_SORT_ORDER = "sortOrder";
   private JsArray<ColumnSortInfo> columnSortOrder_ = null;
   private DataImportPresenter dataImportPresenter_;
   private boolean inputPending_ = false;

   private final PaneManager paneManager_;
   private static final FilesConstants constants_ = GWT.create(FilesConstants.class);
}
