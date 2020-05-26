/*
 * Source.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.*;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationAction;
import org.rstudio.studio.client.application.ApplicationUtils;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenPresentationSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenPresentationSourceFileHandler;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileHandler;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.common.rnw.RnwWeaveRegistry;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.events.SynctexStatusChangedEvent;
import org.rstudio.studio.client.events.GetEditorContextEvent;
import org.rstudio.studio.client.events.GetEditorContextEvent.DocumentSelection;
import org.rstudio.studio.client.events.ReplaceRangesEvent;
import org.rstudio.studio.client.events.ReplaceRangesEvent.ReplacementData;
import org.rstudio.studio.client.events.SetSelectionRangesEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.model.RequestDocumentCloseEvent;
import org.rstudio.studio.client.server.model.RequestDocumentSaveEvent;
import org.rstudio.studio.client.workbench.ConsoleEditorProvider;
import org.rstudio.studio.client.workbench.MainWindowObject;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.ZoomPaneEvent;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.model.UnsavedChangesItem;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.snippets.model.SnippetsChangedEvent;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.output.find.events.FindInFilesEvent;
import org.rstudio.studio.client.workbench.views.source.NewShinyWebApplication.Result;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager.NavigationResult;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.codebrowser.CodeBrowserEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.events.OpenObjectExplorerEvent;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.OpenProfileEvent;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerContents;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetPresentationHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Selection;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.NewWorkingCopyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.NewRdDialog;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;
import org.rstudio.studio.client.workbench.views.source.model.RdShellResult;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigationHistory;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Singleton
public class Source implements InsertSourceHandler,
                               IsWidget,
                               OpenSourceFileHandler,
                               OpenPresentationSourceFileHandler,
                               FileEditHandler,
                               ShowContentHandler,
                               ShowDataHandler,
                               CodeBrowserNavigationHandler,
                               CodeBrowserFinishedHandler,
                               CodeBrowserHighlightEvent.Handler,
                               SnippetsChangedEvent.Handler,
                               PopoutDocEvent.Handler,
                               DocWindowChangedEvent.Handler,
                               DocTabDragInitiatedEvent.Handler,
                               PopoutDocInitiatedEvent.Handler,
                               DebugModeChangedEvent.Handler,
                               OpenProfileEvent.Handler,
                               OpenObjectExplorerEvent.Handler,
                               ReplaceRangesEvent.Handler,
                               SetSelectionRangesEvent.Handler,
                               GetEditorContextEvent.Handler,
                               RequestDocumentSaveEvent.Handler,
                               RequestDocumentCloseEvent.Handler,
                               EditPresentationSourceEvent.Handler,
                               NewDocumentWithCodeEvent.Handler
{
   interface Binder extends CommandBinder<Commands, Source>
   {
   }

   public interface Display extends IsWidget,
                                    HasTabClosingHandlers,
                                    HasTabCloseHandlers,
                                    HasTabClosedHandlers,
                                    HasTabReorderHandlers,
                                    HasBeforeSelectionHandlers<Integer>,
                                    HasSelectionHandlers<Integer>
                                    
   {
      void addTab(Widget widget,
                  FileIcon icon,
                  String docId,
                  String name,
                  String tooltip,
                  Integer position,
                  boolean switchToTab);

      int getTabCount();
      int getActiveTabIndex();

      void selectTab(int tabIndex);
      void selectTab(Widget widget);
      void moveTab(int index, int delta);
      void renameTab(Widget child,
                     FileIcon icon,
                     String value,
                     String tooltip);

      void setDirty(Widget widget, boolean dirty);

      void closeTab(boolean interactive);
      void closeTab(Widget widget, boolean interactive);
      void closeTab(Widget widget, boolean interactive, Command onClosed);
      void closeTab(int index, boolean interactive);
      void closeTab(int index, boolean interactive, Command onClosed);

      void showUnsavedChangesDialog(
            String title,
            ArrayList<UnsavedChangesTarget> dirtyTargets,
            OperationWithInput<UnsavedChangesDialog.Result> saveOperation,
            Command onCancelled);

      void manageChevronVisibility();
      void showOverflowPopup();
      void cancelTabDrag();
      
      void onBeforeShow();
      void ensureVisible();

      HandlerRegistration addBeforeShowHandler(BeforeShowHandler handler);
   }

   @Inject
   public Source(Commands commands,
                 Binder binder,
                 Display view,
                 SourceColumnManager sourceColumnManager,
                 SourceServerOperations server,
                 EditingTargetSource editingTargetSource,
                 FileTypeRegistry fileTypeRegistry,
                 GlobalDisplay globalDisplay,
                 FileDialogs fileDialogs,
                 RemoteFileSystemContext fileContext,
                 EventBus events,
                 AriaLiveService ariaLive,
                 final Session session,
                 Synctex synctex,
                 WorkbenchContext workbenchContext,
                 UserState userState,
                 Satellite satellite,
                 ConsoleEditorProvider consoleEditorProvider,
                 RnwWeaveRegistry rnwWeaveRegistry,
                 DependencyManager dependencyManager,
                 Provider<SourceWindowManager> pWindowManager)
   {
      commands_ = commands;
      binder.bind(commands, this);
      columnManager_ = sourceColumnManager;
      server_ = server;
      editingTargetSource_ = editingTargetSource;
      fileTypeRegistry_ = fileTypeRegistry;
      globalDisplay_ = globalDisplay;
      fileDialogs_ = fileDialogs;
      fileContext_ = fileContext;
      events_ = events;
      ariaLive_ = ariaLive;
      session_ = session;
      synctex_ = synctex;
      workbenchContext_ = workbenchContext;
      userState_ = userState;
      consoleEditorProvider_ = consoleEditorProvider;
      rnwWeaveRegistry_ = rnwWeaveRegistry;
      dependencyManager_ = dependencyManager;
      pWindowManager_ = pWindowManager;

      commands_.newSourceDoc().setEnabled(true);

      dynamicCommands_ = new HashSet<AppCommand>();
      dynamicCommands_.add(commands.saveSourceDoc());
      dynamicCommands_.add(commands.reopenSourceDocWithEncoding());
      dynamicCommands_.add(commands.saveSourceDocAs());
      dynamicCommands_.add(commands.saveSourceDocWithEncoding());
      dynamicCommands_.add(commands.printSourceDoc());
      dynamicCommands_.add(commands.vcsFileLog());
      dynamicCommands_.add(commands.vcsFileDiff());
      dynamicCommands_.add(commands.vcsFileRevert());
      dynamicCommands_.add(commands.executeCode());
      dynamicCommands_.add(commands.executeCodeWithoutFocus());
      dynamicCommands_.add(commands.executeAllCode());
      dynamicCommands_.add(commands.executeToCurrentLine());
      dynamicCommands_.add(commands.executeFromCurrentLine());
      dynamicCommands_.add(commands.executeCurrentFunction());
      dynamicCommands_.add(commands.executeCurrentSection());
      dynamicCommands_.add(commands.executeLastCode());
      dynamicCommands_.add(commands.insertChunk());
      dynamicCommands_.add(commands.insertSection());
      dynamicCommands_.add(commands.executeSetupChunk());
      dynamicCommands_.add(commands.executePreviousChunks());
      dynamicCommands_.add(commands.executeSubsequentChunks());
      dynamicCommands_.add(commands.executeCurrentChunk());
      dynamicCommands_.add(commands.executeNextChunk());
      dynamicCommands_.add(commands.previewJS());
      dynamicCommands_.add(commands.previewSql());
      dynamicCommands_.add(commands.sourceActiveDocument());
      dynamicCommands_.add(commands.sourceActiveDocumentWithEcho());
      dynamicCommands_.add(commands.knitDocument());
      dynamicCommands_.add(commands.toggleRmdVisualMode());
      dynamicCommands_.add(commands.enableProsemirrorDevTools());
      dynamicCommands_.add(commands.previewHTML());
      dynamicCommands_.add(commands.compilePDF());
      dynamicCommands_.add(commands.compileNotebook());
      dynamicCommands_.add(commands.synctexSearch());
      dynamicCommands_.add(commands.popoutDoc());
      dynamicCommands_.add(commands.returnDocToMain());
      dynamicCommands_.add(commands.findReplace());
      dynamicCommands_.add(commands.findNext());
      dynamicCommands_.add(commands.findPrevious());
      dynamicCommands_.add(commands.findFromSelection());
      dynamicCommands_.add(commands.replaceAndFind());
      dynamicCommands_.add(commands.extractFunction());
      dynamicCommands_.add(commands.extractLocalVariable());
      dynamicCommands_.add(commands.commentUncomment());
      dynamicCommands_.add(commands.reindent());
      dynamicCommands_.add(commands.reflowComment());
      dynamicCommands_.add(commands.jumpTo());
      dynamicCommands_.add(commands.jumpToMatching());
      dynamicCommands_.add(commands.goToHelp());
      dynamicCommands_.add(commands.goToDefinition());
      dynamicCommands_.add(commands.setWorkingDirToActiveDoc());
      dynamicCommands_.add(commands.debugDumpContents());
      dynamicCommands_.add(commands.debugImportDump());
      dynamicCommands_.add(commands.goToLine());
      dynamicCommands_.add(commands.checkSpelling());
      dynamicCommands_.add(commands.wordCount());
      dynamicCommands_.add(commands.codeCompletion());
      dynamicCommands_.add(commands.findUsages());
      dynamicCommands_.add(commands.debugBreakpoint());
      dynamicCommands_.add(commands.vcsViewOnGitHub());
      dynamicCommands_.add(commands.vcsBlameOnGitHub());
      dynamicCommands_.add(commands.editRmdFormatOptions());
      dynamicCommands_.add(commands.reformatCode());
      dynamicCommands_.add(commands.showDiagnosticsActiveDocument());
      dynamicCommands_.add(commands.renameInScope());
      dynamicCommands_.add(commands.insertRoxygenSkeleton());
      dynamicCommands_.add(commands.expandSelection());
      dynamicCommands_.add(commands.shrinkSelection());
      dynamicCommands_.add(commands.toggleDocumentOutline());
      dynamicCommands_.add(commands.knitWithParameters());
      dynamicCommands_.add(commands.clearKnitrCache());
      dynamicCommands_.add(commands.goToNextSection());
      dynamicCommands_.add(commands.goToPrevSection());
      dynamicCommands_.add(commands.goToNextChunk());
      dynamicCommands_.add(commands.goToPrevChunk());
      dynamicCommands_.add(commands.profileCode());
      dynamicCommands_.add(commands.profileCodeWithoutFocus());
      dynamicCommands_.add(commands.saveProfileAs());
      dynamicCommands_.add(commands.restartRClearOutput());
      dynamicCommands_.add(commands.restartRRunAllChunks());
      dynamicCommands_.add(commands.notebookCollapseAllOutput());
      dynamicCommands_.add(commands.notebookExpandAllOutput());
      dynamicCommands_.add(commands.notebookClearOutput());
      dynamicCommands_.add(commands.notebookClearAllOutput());
      dynamicCommands_.add(commands.notebookToggleExpansion());
      dynamicCommands_.add(commands.sendToTerminal());
      dynamicCommands_.add(commands.openNewTerminalAtEditorLocation());
      dynamicCommands_.add(commands.sendFilenameToTerminal());
      dynamicCommands_.add(commands.renameSourceDoc());
      dynamicCommands_.add(commands.sourceAsLauncherJob());
      dynamicCommands_.add(commands.sourceAsJob());
      dynamicCommands_.add(commands.runSelectionAsJob());
      dynamicCommands_.add(commands.runSelectionAsLauncherJob());
      dynamicCommands_.add(commands.toggleSoftWrapMode());
      for (AppCommand command : dynamicCommands_)
      {
         command.setVisible(false);
         command.setEnabled(false);
      }
      
      vimCommands_ = new SourceVimCommands();

      events_.addHandler(EditPresentationSourceEvent.TYPE, this);
      events_.addHandler(FileEditEvent.TYPE, this);
      events_.addHandler(InsertSourceEvent.TYPE, this);
      events_.addHandler(ShowContentEvent.TYPE, this);
      events_.addHandler(ShowDataEvent.TYPE, this);
      events_.addHandler(OpenObjectExplorerEvent.TYPE, this);
      events_.addHandler(OpenPresentationSourceFileEvent.TYPE, this);
      events_.addHandler(OpenSourceFileEvent.TYPE, this);
      events_.addHandler(CodeBrowserNavigationEvent.TYPE, this);
      events_.addHandler(CodeBrowserFinishedEvent.TYPE, this);
      events_.addHandler(CodeBrowserHighlightEvent.TYPE, this);
      events_.addHandler(SnippetsChangedEvent.TYPE, this);
      events_.addHandler(NewDocumentWithCodeEvent.TYPE, this);

      events_.addHandler(FileTypeChangedEvent.TYPE, new FileTypeChangedHandler()
      {
         public void onFileTypeChanged(FileTypeChangedEvent event)
         {
            manageCommands();
         }
      });
      
      events_.addHandler(SourceOnSaveChangedEvent.TYPE, 
                        new SourceOnSaveChangedHandler() {
         @Override
         public void onSourceOnSaveChanged(SourceOnSaveChangedEvent event)
         {
            manageSaveCommands();
         }
      });

      // !!! move to column manager
      events_.addHandler(DocTabActivatedEvent.TYPE, new DocTabActivatedEvent.Handler()
      {
         public void onDocTabActivated(DocTabActivatedEvent event)
         {

            columnManager_.setActiveDocId(event.getId());
         }
      });

      events_.addHandler(SwitchToDocEvent.TYPE, new SwitchToDocHandler()
      {
         public void onSwitchToDoc(SwitchToDocEvent event)
         {
            columnManager_.ensureVisible(false);
            
            // Fire an activation event just to ensure the activated
            // tab gets focus
            commands_.activateSource().execute();
         }
      });

      events_.addHandler(SourcePathChangedEvent.TYPE, 
            new SourcePathChangedEvent.Handler()
      {
         
         @Override
         public void onSourcePathChanged(final SourcePathChangedEvent event)
         {
            
            columnManager_.inEditorForPath(event.getFrom(), 
                            new OperationWithInput<EditingTarget>()
            {
               @Override
               public void execute(EditingTarget input)
               {
                  FileSystemItem toPath = 
                        FileSystemItem.createFile(event.getTo());
                  if (input instanceof TextEditingTarget)
                  {
                     // for text files, notify the editing surface so it can
                     // react to the new file type
                     ((TextEditingTarget)input).setPath(toPath);
                  }
                  else
                  {
                     // for other files, just rename the tab
                     input.getName().setValue(toPath.getName(), true);
                  }
                  events_.fireEvent(new SourceFileSavedEvent(
                        input.getId(), event.getTo()));
               }
            });
         }
      });
           
      events_.addHandler(SourceNavigationEvent.TYPE, 
                        new SourceNavigationHandler() {
         @Override
         public void onSourceNavigation(SourceNavigationEvent event)
         {
            if (!suspendSourceNavigationAdding_)
            {
               sourceNavigationHistory_.add(event.getNavigation());
            }
         }
      });
      
      sourceNavigationHistory_.addChangeHandler(new ChangeHandler()
      {

         @Override
         public void onChange(ChangeEvent event)
         {
            manageSourceNavigationCommands();
         }
      });
      
      events_.addHandler(SynctexStatusChangedEvent.TYPE, 
                        new SynctexStatusChangedEvent.Handler()
      {
         @Override
         public void onSynctexStatusChanged(SynctexStatusChangedEvent event)
         {
            manageSynctexCommands();
         }
      });
      
      events_.addHandler(CollabEditStartedEvent.TYPE, 
            new CollabEditStartedEvent.Handler() 
      {
         @Override
         public void onCollabEditStarted(final CollabEditStartedEvent collab) 
         {
            columnManager_.inEditorForPath(collab.getStartParams().getPath(),
               new OperationWithInput<EditingTarget>()
               {
                  @Override
                  public void execute(EditingTarget editor)
                  {
                     editor.beginCollabSession(collab.getStartParams());
                  }
               });
         }
      });
         
      events_.addHandler(CollabEditEndedEvent.TYPE, 
            new CollabEditEndedEvent.Handler()
      {
         @Override
         public void onCollabEditEnded(final CollabEditEndedEvent collab) 
         {
            columnManager_.inEditorForPath(collab.getPath(), 
               new OperationWithInput<EditingTarget>()
               {
                  @Override
                  public void execute(EditingTarget editor)
                  {
                     editor.endCollabSession();
                  }
               });
         }
      });
         
      events_.addHandler(NewWorkingCopyEvent.TYPE, 
            new NewWorkingCopyEvent.Handler()
      {
         @Override
         public void onNewWorkingCopy(NewWorkingCopyEvent event)
         {
           columnManager_.newDoc(event.getType(), event.getContents(), null);
         }
      });

      events_.addHandler(MaximizeSourceWindowEvent.TYPE, new MaximizeSourceWindowEvent.Handler()
      {
         @Override
         public void onMaximizeSourceWindow(MaximizeSourceWindowEvent e)
         {
            events_.fireEvent(new EnsureVisibleEvent());
            events_.fireEvent(new EnsureHeightEvent(EnsureHeightEvent.MAXIMIZED));
         }
      });
      
      events_.addHandler(EnsureVisibleSourceWindowEvent.TYPE, new EnsureVisibleSourceWindowEvent.Handler()
      {
         @Override
         public void onEnsureVisibleSourceWindow(EnsureVisibleSourceWindowEvent e)
         {
            if (columnManager_.getTabCount() > 0)
            {
               events_.fireEvent(new EnsureVisibleEvent());
               events_.fireEvent(new EnsureHeightEvent(EnsureHeightEvent.NORMAL));
            }
         }
      });

      events_.addHandler(PopoutDocEvent.TYPE, this);
      events_.addHandler(DocWindowChangedEvent.TYPE, this);
      events_.addHandler(DocTabDragInitiatedEvent.TYPE, this);
      events_.addHandler(PopoutDocInitiatedEvent.TYPE, this);
      events_.addHandler(DebugModeChangedEvent.TYPE, this);
      events_.addHandler(ReplaceRangesEvent.TYPE, this);
      events_.addHandler(GetEditorContextEvent.TYPE, this);
      events_.addHandler(SetSelectionRangesEvent.TYPE, this);
      events_.addHandler(OpenProfileEvent.TYPE, this);
      events_.addHandler(RequestDocumentSaveEvent.TYPE, this);
      events_.addHandler(RequestDocumentCloseEvent.TYPE, this);
   }

   public void load()
   {
      AceEditor.load(() -> {
         loadFullSource();
      });
   }

   public void loadDisplay()
   {
      restoreDocuments(session_);
      columnManager_.beforeShow();

      // As tabs were added before, manageCommands() was suppressed due to
      // initialized_ being false, so we need to run it explicitly
      manageCommands();
      // Same with this event
      columnManager_.fireDocTabsChanged();
      
      // open project or edit_published docs (only for main source window)
      if (SourceWindowManager.isMainSourceWindow())
      {
         openProjectDocs(session_);
         openEditPublishedDocs();
      }
      
      // add vim commands
      initVimCommands();
   }

   private void loadFullSource()
   {
      // sync UI prefs with shortcut manager
      userPrefs_ = RStudioGinjector.INSTANCE.getUserPrefs();

      if (userPrefs_.editorKeybindings().getValue() == UserPrefs.EDITOR_KEYBINDINGS_VIM)
         ShortcutManager.INSTANCE.setEditorMode(KeyboardShortcut.MODE_VIM);
      else if (userPrefs_.editorKeybindings().getValue() == UserPrefs.EDITOR_KEYBINDINGS_EMACS)
         ShortcutManager.INSTANCE.setEditorMode(KeyboardShortcut.MODE_EMACS);
      else if (userPrefs_.editorKeybindings().getValue() == UserPrefs.EDITOR_KEYBINDINGS_SUBLIME)
         ShortcutManager.INSTANCE.setEditorMode(KeyboardShortcut.MODE_SUBLIME);
      else
         ShortcutManager.INSTANCE.setEditorMode(KeyboardShortcut.MODE_DEFAULT);

      initialized_ = true;

      // !!! comment why this is needed
      events_.fireEvent(new DocTabsChangedEvent(null,
                                                new String[0],
                                                new FileIcon[0],
                                                new String[0],
                                                new String[0]));

      // fake shortcuts for commands_ which we handle at a lower level
      commands_.goToHelp().setShortcut(new KeyboardShortcut("F1", KeyCodes.KEY_F1, KeyboardShortcut.NONE));
      commands_.goToDefinition().setShortcut(new KeyboardShortcut("F2", KeyCodes.KEY_F2, KeyboardShortcut.NONE));

      // If tab has been disabled for auto complete by the user, set the "shortcut" to ctrl-space instead.
      if (userPrefs_.tabCompletion().getValue() && !userPrefs_.tabKeyMoveFocus().getValue())
         commands_.codeCompletion().setShortcut(new KeyboardShortcut("Tab", KeyCodes.KEY_TAB, KeyboardShortcut.NONE));
      else
      {
         KeySequence sequence = new KeySequence();
         sequence.add(new KeyCombination("Ctrl+Space", KeyCodes.KEY_SPACE, KeyCodes.KEY_CTRL));
         commands_.codeCompletion().setShortcut(new KeyboardShortcut(sequence));
      }
      

      // Suppress 'CTRL + ALT + SHIFT + click' to work around #2483 in Ace
      Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            int type = event.getTypeInt();
            if (type == Event.ONMOUSEDOWN || type == Event.ONMOUSEUP)
            {
               int modifier = KeyboardShortcut.getModifierValue(event.getNativeEvent());
               if (modifier == (KeyboardShortcut.ALT | KeyboardShortcut.CTRL | KeyboardShortcut.SHIFT))
               {
                  event.cancel();
                  return;
               }
            }
         }
      });
      
      // on macOS, we need to aggressively re-sync commands when a new
      // window is selected (since the main menu applies to both main
      // window and satellites)
      if (BrowseCap.isMacintoshDesktop())
      {
         WindowEx.addFocusHandler((FocusEvent event) -> {
            manageCommands(true);
         });
      }
         
      // get the key to use for active tab persistence; use ordinal-based key
      // for source windows rather than their ID to avoid unbounded accumulation
      String activeTabKey = KEY_ACTIVETAB;
      if (!SourceWindowManager.isMainSourceWindow())
         activeTabKey += "SourceWindow" + 
                         pWindowManager_.get().getSourceWindowOrdinal();

      new IntStateValue(MODULE_SOURCE, activeTabKey, 
                        ClientState.PROJECT_PERSISTENT,
                        session_.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(Integer value)
         { 
            if (value == null)
               return;

            columnManager_.initialSelect(value);

            // clear the history manager
            sourceNavigationHistory_.clear();
         }

         @Override
         protected Integer getValue()
         {
            return columnManager_.getPhysicalTabIndex();
         }
      };
      
      AceEditorNative.syncUiPrefs(userPrefs_);
   }
   
   /**
    * Process the save_files_before_build user preference.
    * If false, ask the user how to handle unsaved changes and act accordingly.
    * @param command The command to run after the files are handled.
    * @param cancelCommand The command to run if the user cancels the request.
    * @param commandSource The title to be used by the dialog asking how to handle files.
    */
   public void withSaveFilesBeforeCommand(final Command command,
                                          final Command cancelCommand,
                                          String commandSource)
   {
      if (userPrefs_.saveFilesBeforeBuild().getValue())
      {
         saveUnsavedDocuments(command);
      }
      else
      {
         String alwaysSaveOption = !userPrefs_.saveFilesBeforeBuild().getValue() ?
                                    "Always save files before build" : null;

         ArrayList<UnsavedChangesTarget> unsavedSourceDocs = getUnsavedChanges(TYPE_FILE_BACKED);

         if (unsavedSourceDocs.size() > 0)
         {
            new UnsavedChangesDialog(
                  commandSource,
                  alwaysSaveOption,
                  unsavedSourceDocs,
                  dialogResult ->
                  {
                     if (dialogResult.getAlwaysSave())
                     {
                        userPrefs_.saveFilesBeforeBuild().setGlobalValue(true);
                        userPrefs_.writeUserPrefs();
                     }
                     handleUnsavedChangesBeforeExit(
                                           dialogResult.getSaveTargets(),
                                           command);

                  },
                  cancelCommand
            ).showModal();
         }
         else
         {
            command.execute();
         }
      }
   }

   private boolean consoleEditorHadFocusLast()
   {
      String id = MainWindowObject.lastFocusedEditorId().get();
      return "rstudio_console_input".equals(id);
   }
   
   private void withTarget(String id,
                           CommandWithArg<TextEditingTarget> command,
                           Command onFailure)
   {
      EditingTarget target = StringUtil.isNullOrEmpty(id)
            ? activeEditor_
            : columnManager_.findEditor(id);
      
      if (target == null)
      {
         if (onFailure != null)
            onFailure.execute();
         return;
      }
      
      if (!(target instanceof TextEditingTarget))
      {
         if (onFailure != null)
            onFailure.execute();
         return;
      }
      
      command.execute((TextEditingTarget) target);
   }
   
   private void getEditorContext(String id, String path, DocDisplay docDisplay)
   {
      AceEditor editor = (AceEditor) docDisplay;
      Selection selection = editor.getNativeSelection();
      Range[] ranges = selection.getAllRanges();
      
      // clamp ranges to document boundaries
      for (Range range : ranges)
      {
         Position start = range.getStart();
         start.setRow(MathUtil.clamp(start.getRow(), 0, editor.getRowCount()));
         start.setColumn(MathUtil.clamp(start.getColumn(), 0, editor.getLine(start.getRow()).length()));
         
         Position end = range.getEnd();
         end.setRow(MathUtil.clamp(end.getRow(), 0, editor.getRowCount()));
         end.setColumn(MathUtil.clamp(end.getColumn(), 0, editor.getLine(end.getRow()).length()));
      }

      JsArray<DocumentSelection> docSelections = JavaScriptObject.createArray().cast();
      for (int i = 0; i < ranges.length; i++)
      {
         docSelections.push(DocumentSelection.create(
               ranges[i],
               editor.getTextForRange(ranges[i])));
      }

      id = StringUtil.notNull(id);
      path = StringUtil.notNull(path);
      
      GetEditorContextEvent.SelectionData data =
            GetEditorContextEvent.SelectionData.create(id, path, editor.getCode(), docSelections);
      
      server_.getEditorContextCompleted(data, new VoidServerRequestCallback());
   }
   
   private void withTarget(String id, CommandWithArg<TextEditingTarget> command)
   {
      withTarget(id, command, null);
   }
   
   private void initVimCommands()
   {
      vimCommands_.save(this);
      vimCommands_.selectTabIndex(this);
      vimCommands_.selectNextTab(this);
      vimCommands_.selectPreviousTab(this);
      vimCommands_.closeActiveTab(this);
      vimCommands_.closeAllTabs(this);
      vimCommands_.createNewDocument(this);
      vimCommands_.saveAndCloseActiveTab(this);
      vimCommands_.readFile(this, userPrefs_.defaultEncoding().getValue());
      vimCommands_.runRScript(this);
      vimCommands_.reflowText(this);
      vimCommands_.showVimHelp(
            RStudioGinjector.INSTANCE.getShortcutViewer());
      vimCommands_.showHelpAtCursor(this);
      vimCommands_.reindent(this);
      vimCommands_.expandShrinkSelection(this);
      vimCommands_.openNextFile(this);
      vimCommands_.openPreviousFile(this);
      vimCommands_.addStarRegister();
   }
   
   public Widget asWidget()
   {
     return columnManager_.getActive().asWidget();
   }

   public Widget asWidget(Display display)
   {
      return display.asWidget();
   }

   private void restoreDocuments(final Session session)
   {
      final JsArray<SourceDocument> docs =
            session.getSessionInfo().getSourceDocuments();

      for (int i = 0; i < docs.length(); i++)
      {
         // restore the docs assigned to this source window
         SourceDocument doc = docs.get(i);
         String docWindowId = 
               doc.getProperties().getString(
                     SourceWindowManager.SOURCE_WINDOW_ID);
         if (docWindowId == null)
            docWindowId = "";
         String currentSourceWindowId = SourceWindowManager.getSourceWindowId();
         
         // it belongs in this window if (a) it's assigned to it, or (b) this
         // is the main window, and the window it's assigned to isn't open.
         if (currentSourceWindowId == docWindowId ||
             (SourceWindowManager.isMainSourceWindow() && 
              !pWindowManager_.get().isSourceWindowOpen(docWindowId)))
         {

            // attempt to add a tab for the current doc; try/catch this since
            // we don't want to allow one failure to prevent all docs from
            // opening
            EditingTarget sourceEditor = null;
            try
            {
               // determine the correct display if the doc belongs to this window,
               // otherwise use the active one
               if (currentSourceWindowId == docWindowId &&
                   SourceWindowManager.isMainSourceWindow())
               {
                  String name = doc.getSourceDisplayName();
                  sourceEditor = columnManager_.addTab(doc, true, OPEN_REPLAY,
                                                       columnManager_.findByName(name));
               }
               else
                  sourceEditor = columnManager_.addTab(doc, true, OPEN_REPLAY, null);
            }
            catch (Exception e)
            {
               Debug.logException(e);
            }
            
            // if we couldn't add the tab for this doc, just continue to the
            // next one
            if (sourceEditor == null)
               continue;
         }
      }
   }
   
   private void openEditPublishedDocs()
   {
      // don't do this if we are switching projects (it
      // will be done after the switch)
      if (ApplicationAction.isSwitchProject())
         return;
      
      // check for edit_published url parameter
      final String kEditPublished = "edit_published";
      String editPublished = StringUtil.notNull(
          Window.Location.getParameter(kEditPublished));
      
      // this is an appPath which we can call the server
      // to determine source files to edit 
      if (editPublished.length() > 0)
      {
         // remove it from the url
         ApplicationUtils.removeQueryParam(kEditPublished);
         
         server_.getEditPublishedDocs(
            editPublished, 
            new SimpleRequestCallback<JsArrayString>() {
               @Override
               public void onResponseReceived(JsArrayString docs)
               {
                  new SourceFilesOpener(docs).run();
               }
            }
         );
      }
   }
   
   private void openProjectDocs(final Session session)
   {
      JsArrayString openDocs = session.getSessionInfo().getProjectOpenDocs();
      if (openDocs.length() > 0)
      {
         // set new tab pending for the duration of the continuation
         // !!! UNCOMMENT ON MOVE newTabPending_++;
                 
         // create a continuation for opening the source docs
         SerializedCommandQueue openCommands = new SerializedCommandQueue();
         
         for (int i=0; i<openDocs.length(); i++)
         {
            String doc = openDocs.get(i);
            final FileSystemItem fsi = FileSystemItem.createFile(doc);
              
            openCommands.addCommand(new SerializedCommand() {

               @Override
               public void onExecute(final Command continuation)
               {
                  columnManager_.openFile(fsi, 
                           fileTypeRegistry_.getTextTypeForFile(fsi), 
                           new CommandWithArg<EditingTarget>() {
                              @Override
                              public void execute(EditingTarget arg)
                              {  
                                 continuation.execute();
                              }
                           });
               }
            });
         }
         
         // decrement newTabPending and select first tab when done
         openCommands.addCommand(new SerializedCommand() {

            @Override
            public void onExecute(Command continuation)
            {
               // !!! UNCOMMENT ON MOVE newTabPending_--;
               columnManager_.onFirstTab();
               continuation.execute();
            }
            
         });
         
         // execute the continuation
         openCommands.run();
      }
   }
   
   public void onShowContent(ShowContentEvent event)
   {
      // ignore if we're a satellite
      if (!SourceWindowManager.isMainSourceWindow())
         return;
      
      columnManager_.ensureVisible(true);
      ContentItem content = event.getContent();
      server_.newDocument(
            FileTypeRegistry.URLCONTENT.getTypeId(),
            null,
            (JsObject) content.cast(),
            new SimpleRequestCallback<SourceDocument>("Show")
            {
               @Override
               public void onResponseReceived(SourceDocument response)
               {
                  columnManager_.addTab(response,
                                        OPEN_INTERACTIVE,
                                        columnManager_.findByDocument(content.getTitle()));
               }
            });
   }
   
   @Override
   public void onOpenObjectExplorerEvent(OpenObjectExplorerEvent event)
   {
      // ignore if we're a satellite
      if (!SourceWindowManager.isMainSourceWindow())
         return;
    
      columnManager_.activateObjectExplorer(event.getHandle());
   }

   @Override
   public void onShowData(ShowDataEvent event)
   {
      // ignore if we're a satellite
      if (!SourceWindowManager.isMainSourceWindow())
         return;
      
      columnManager_.showDataItem(event.getData());
   }
   
   public void onShowProfiler(OpenProfileEvent event)
   {
      Debug.logToConsole("onShowProfiler not coded");
      /*
      String profilePath = event.getFilePath();
      String htmlPath = event.getHtmlPath();
      String htmlLocalPath = event.getHtmlLocalPath();
      
      // first try to activate existing
      for (int idx = 0; idx < editors_.size(); idx++)
      {
         String path = editors_.get(idx).getPath();
         if (path != null && path == profilePath)
         {
            columnManager_.ensureVisible(false);
            columnManager_.selectTab(editors_.get(idx));
            return;
         }
      }
      
      // create new profiler 
      columnManager_.ensureVisible(true);

      if (event.getDocId() != null)
      {
         server_.getSourceDocument(event.getDocId(), new ServerRequestCallback<SourceDocument>()
         {
            @Override
            public void onResponseReceived(SourceDocument response)
            {
               columnManager_.addTab(response, OPEN_INTERACTIVE, null);
            }
            
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
               globalDisplay_.showErrorMessage("Source Document Error", error.getUserMessage());
            }
         });
      }
      else
      {
         server_.newDocument(
            FileTypeRegistry.PROFILER.getTypeId(),
            null,
            (JsObject) ProfilerContents.create(
                  profilePath,
                  htmlPath, 
                  htmlLocalPath,
                  event.getCreateProfile()).cast(),
            new SimpleRequestCallback<SourceDocument>("Show Profiler")
            {
               @Override
               public void onResponseReceived(SourceDocument response)
               {
                  columnManager_.addTab(response, OPEN_INTERACTIVE, null);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  globalDisplay_.showErrorMessage("Source Document Error", error.getUserMessage());
               }
            });
      }
      */
   }
   
   @Handler
   public void onNewSourceDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.R, null);
   }

   @Handler
   public void onNewTextDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.TEXT, null);
   }
   
   @Handler
   public void onNewRNotebook()
   {
      dependencyManager_.withRMarkdown("R Notebook",
         "Create R Notebook", new CommandWithArg<Boolean>()
         {
            @Override
            public void execute(Boolean succeeded)
            {
               if (!succeeded)
               {
                  globalDisplay_.showErrorMessage("Notebook Creation Failed", 
                        "One or more packages required for R Notebook " +
                        "creation were not installed.");
                  return;
               }

               columnManager_.newSourceDocWithTemplate(
                     (TextFileType)FileTypeRegistry.RMARKDOWN,
                     "",
                     "notebook.Rmd",
                     Position.create(3, 0));
            }
         });
   }
   
   @Handler
   public void onNewCDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.C, new ResultCallback<EditingTarget, ServerError>()
      {
         @Override
         public void onSuccess(EditingTarget target)
         {
            target.verifyCppPrerequisites();
         }
      });
   }
   
   
   @Handler
   public void onNewCppDoc()
   {
      columnManager_.newSourceDocWithTemplate(
          FileTypeRegistry.CPP, 
          "", 
          userPrefs_.useRcppTemplate().getValue() ? "rcpp.cpp" : "default.cpp",
          Position.create(0, 0),
          new CommandWithArg<EditingTarget> () {
            @Override
            public void execute(EditingTarget target)
            {
               target.verifyCppPrerequisites(); 
            }
          }
      );
   }
   
   @Handler
   public void onNewHeaderDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.H, new ResultCallback<EditingTarget, ServerError>()
      {
         @Override
         public void onSuccess(EditingTarget target)
         {
            target.verifyCppPrerequisites();
         }
      });
   }
   
   @Handler
   public void onNewMarkdownDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.MARKDOWN, null);
   }
   
   
   @Handler
   public void onNewPythonDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.PYTHON, new ResultCallback<EditingTarget, ServerError>()
      {
         @Override
         public void onSuccess(EditingTarget target)
         {
            target.verifyPythonPrerequisites();
         }
      });
   }
   
   @Handler
   public void onNewShellDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.SH, null);
   }
   
   @Handler
   public void onNewHtmlDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.HTML, null);
   }
   
   @Handler
   public void onNewJavaScriptDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.JS, null);
   }
   
   @Handler
   public void onNewCssDoc()
   {
      columnManager_.newDoc(FileTypeRegistry.CSS, null);
   }
   
   @Handler
   public void onNewStanDoc()
   {
      final Command onStanInstalled = () -> {
         columnManager_.newSourceDocWithTemplate(
               FileTypeRegistry.STAN,
               "",
               "stan.stan",
               Position.create(31, 0),
               (EditingTarget target) ->
               {

               });
      };
            
            
      dependencyManager_.withStan(
            "Creating Stan script",
            "Creating Stan scripts",
            onStanInstalled);
   }
   
   @Handler
   public void onNewD3Doc()
   {
      columnManager_.newSourceDocWithTemplate(
         FileTypeRegistry.JS, 
         "", 
         "d3.js",
         Position.create(5, 0),
         new CommandWithArg<EditingTarget> () {
           @Override
           public void execute(EditingTarget target)
           {
              target.verifyD3Prerequisites(); 
              target.setSourceOnSave(true);
           }
         }
      );
   }
   
   
   @Handler
   public void onNewSweaveDoc()
   {
      // set concordance value if we need to
      String concordance = new String();
      if (userPrefs_.alwaysEnableRnwConcordance().getValue())
      {
         RnwWeave activeWeave = rnwWeaveRegistry_.findTypeIgnoreCase(
                                    userPrefs_.defaultSweaveEngine().getValue());
         if (activeWeave.getInjectConcordance())
            concordance = "\\SweaveOpts{concordance=TRUE}\n";
      }
      final String concordanceValue = concordance;
     
      // show progress
      final ProgressIndicator indicator = new GlobalProgressDelayer(
            globalDisplay_, 500, "Creating new document...").getIndicator();

      // get the template
      server_.getSourceTemplate("", 
                                "sweave.Rnw", 
                                new ServerRequestCallback<String>() {
         @Override
         public void onResponseReceived(String templateContents)
         {
            indicator.onCompleted();
            
            // add in concordance if necessary
            final boolean hasConcordance = concordanceValue.length() > 0;
            if (hasConcordance)
            {
               String beginDoc = "\\begin{document}\n";
               templateContents = templateContents.replace(
                     beginDoc,
                     beginDoc + concordanceValue);
            }
            
            columnManager_.newDoc(FileTypeRegistry.SWEAVE, 
                  templateContents, 
                  new ResultCallback<EditingTarget, ServerError> () {
               @Override
               public void onSuccess(EditingTarget target)
               {
                  int startRow = 4 + (hasConcordance ? 1 : 0);
                  target.setCursorPosition(Position.create(startRow, 0));
               }
            });
         }

         @Override
         public void onError(ServerError error)
         {
            indicator.onError(error.getUserMessage());
         }
      });
   }
   
   @Handler
   public void onNewRMarkdownDoc()
   {
      SessionInfo sessionInfo = session_.getSessionInfo();
      boolean useRMarkdownV2 = sessionInfo.getRMarkdownPackageAvailable();
      
      if (useRMarkdownV2)
         columnManager_.newRMarkdownV2Doc();
      else
         columnManager_.newRMarkdownV1Doc();
   }
   
   private void doNewRShinyApp(NewShinyWebApplication.Result result)
   {
      server_.createShinyApp(
            result.getAppName(),
            result.getAppType(),
            result.getAppDir(),
            new SimpleRequestCallback<JsArrayString>("Error Creating Shiny Application", true)
            {
               @Override
               public void onResponseReceived(JsArrayString createdFiles)
               {
                  // Open and focus files that we created
                  new SourceFilesOpener(createdFiles).run();
               }
            });
   }

   @Handler
   public void onNewSqlDoc()
   {
      columnManager_.newSourceDocWithTemplate(
         FileTypeRegistry.SQL, 
         "", 
         "query.sql",
         Position.create(2, 0),
         new CommandWithArg<EditingTarget> () {
           @Override
           public void execute(EditingTarget target)
           {
              target.verifyNewSqlPrerequisites(); 
              target.setSourceOnSave(true);
           }
         }
      );
   }
   
   private void doNewRPlumberAPI(NewPlumberAPI.Result result)
   {
      server_.createPlumberAPI(
            result.getAPIName(),
            result.getAPIDir(),
            new SimpleRequestCallback<JsArrayString>("Error Creating Plumber API", true)
            {
               @Override
               public void onResponseReceived(JsArrayString createdFiles)
               {
                  // Open and focus files that we created
                  new SourceFilesOpener(createdFiles).run();
               }
            });
   }
    
   // open a list of source files then focus the first one within the list
   private class SourceFilesOpener extends SerializedCommandQueue
   {
      public SourceFilesOpener(JsArrayString sourceFiles)
      {
         for (int i=0; i<sourceFiles.length(); i++)
         {
            final String filePath = sourceFiles.get(i);
            addCommand(new SerializedCommand() {

               @Override
               public void onExecute(final Command continuation)
               {
                  FileSystemItem path = FileSystemItem.createFile(filePath);
                  columnManager_.openFile(path, FileTypeRegistry.R,
                        new CommandWithArg<EditingTarget>()
                  {
                     @Override
                     public void execute(EditingTarget target)
                     {
                        // record first target if necessary
                        if (firstTarget_ == null)
                           firstTarget_ = target;
                        
                        continuation.execute();
                     }
                  });  
               }
            });
         }
         
         addCommand(new SerializedCommand() {

            @Override
            public void onExecute(Command continuation)
            {
               if (firstTarget_ != null)
               {
                  columnManager_.selectTab(firstTarget_);
                  firstTarget_.setCursorPosition(Position.create(0, 0));
               }
               
               continuation.execute();
            }
            
         });
      }
      
      private EditingTarget firstTarget_ = null;
   }
   
   @Handler
   public void onNewRShinyApp()
   {
      dependencyManager_.withShiny("Creating Shiny applications", new Command()
      {
         @Override
         public void execute()
         {
            NewShinyWebApplication widget = new NewShinyWebApplication(
                  "New Shiny Web Application",
                  new OperationWithInput<NewShinyWebApplication.Result>()
                  {
                     @Override
                     public void execute(Result input)
                     {
                        doNewRShinyApp(input);
                     }
                  });

            widget.showModal();
         }
      });
   }
   
   @Handler
   public void onNewRHTMLDoc()
   {
      columnManager_.newSourceDocWithTemplate(FileTypeRegistry.RHTML, 
                                              "", 
                                              "default.Rhtml");
   }
   
   @Handler
   public void onNewRDocumentationDoc()
   {
      new NewRdDialog(
         new OperationWithInput<NewRdDialog.Result>() {
           
            @Override
            public void execute(final NewRdDialog.Result result)
            {
               final Command createEmptyDoc = new Command() {
                  @Override
                  public void execute()
                  {
                     columnManager_.newSourceDocWithTemplate(
                           (TextFileType)FileTypeRegistry.RD, 
                           result.name, 
                           "default.Rd",
                           Position.create(3, 7));
                  }  
               };
               
               if (result.type != NewRdDialog.Result.TYPE_NONE)
               {
                  server_.createRdShell(
                     result.name, 
                     result.type,
                     new SimpleRequestCallback<RdShellResult>() {
                        @Override
                        public void onResponseReceived(RdShellResult result)
                        {
                           if (result.getPath() != null)
                           {
                              fileTypeRegistry_.openFile(
                                 FileSystemItem.createFile(result.getPath()));
                           }
                           else if (result.getContents() != null)
                           {
                              columnManager_.newDoc(FileTypeRegistry.RD, 
                                     result.getContents(),
                                     null);
                           }
                           else
                           {
                              createEmptyDoc.execute();
                           }
                        }  
                   });
                 
               }
               else
               {
                  createEmptyDoc.execute();
               }
               
            }
          }).showModal();
   }
   
   @Handler
   public void onNewRPresentationDoc()
   {
      dependencyManager_.withRMarkdown(
         "Authoring R Presentations", new Command() {
            @Override
            public void execute()
            {
               fileDialogs_.saveFile(
                  "New R Presentation", 
                  fileContext_,
                  workbenchContext_.getDefaultFileDialogDir(), 
                  ".Rpres", 
                  true, 
                  new ProgressOperationWithInput<FileSystemItem>() {

                     @Override
                     public void execute(final FileSystemItem input,
                                         final ProgressIndicator indicator)
                     {
                        if (input == null)
                        {
                           indicator.onCompleted();
                           return;
                        }
                        
                        indicator.onProgress("Creating Presentation...");
                        
                        server_.createNewPresentation(
                          input.getPath(),
                          new VoidServerRequestCallback(indicator) {
                             @Override
                             public void onSuccess()
                             { 
                                columnManager_.openFile(input, 
                                   FileTypeRegistry.RPRESENTATION,
                                   new CommandWithArg<EditingTarget>() {

                                    @Override
                                    public void execute(EditingTarget arg)
                                    {
                                       server_.showPresentationPane(
                                           input.getPath(),
                                           new VoidServerRequestCallback());
                                       
                                    }
                                   
                                });
                             }
                          });  
                     }
               });
               
            }
      });
   }
   
   @Handler
   public void onFindInFiles()
   {
      String searchPattern = "";
      if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
      {  
         TextEditingTarget textEditor = (TextEditingTarget) activeEditor_;
         String selection = textEditor.getSelectedText();
         boolean multiLineSelection = selection.indexOf('\n') != -1;
         
         if ((selection.length() != 0) && !multiLineSelection)
            searchPattern = selection; 
      }
      
      events_.fireEvent(new FindInFilesEvent(searchPattern));
   }

   @Handler
   public void onActivateSource()
   {
      onActivateSource(null);
   }
   
   public void onActivateSource(final Command afterActivation)
   {
      // give the window manager a chance to activate the last source pane
      if (pWindowManager_.get().activateLastFocusedSource())
         return;
      columnManager_.activateColumns(afterActivation);
   }
   
   @Handler
   public void onLayoutZoomSource()
   {
      onActivateSource(new Command()
      {
         @Override
         public void execute()
         {
            events_.fireEvent(new ZoomPaneEvent("Source"));
         }
      });
   }
   
   @Override
   public void onPopoutDoc(final PopoutDocEvent e)
   {
      // disowning the doc may cause the entire window to close, so defer it
      // to allow any other popout processing to occur
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            columnManager_.disownDoc(e.getDocId());
         }
      });
   }
   
   @Override
   public void onDebugModeChanged(DebugModeChangedEvent evt)
   {
      // when debugging ends, always disengage any active debug highlights
      if (!evt.debugging() && activeEditor_ != null)
      {
         activeEditor_.endDebugHighlighting();
      }
   }
   
   @Override
   public void onDocWindowChanged(final DocWindowChangedEvent e)
   {
      SourceColumn oldDisplay = null;
      if (e.getNewWindowId() == SourceWindowManager.getSourceWindowId())
      {
         columnManager_.ensureVisible(true);
         
         // look for a collaborative editing session currently running inside 
         // the document being transferred between windows--if we didn't know
         // about one with the event, try to look it up in the local cache of
         // source documents
         final CollabEditStartParams collabParams = 
               e.getCollabParams() == null ? 
                     pWindowManager_.get().getDocCollabParams(e.getDocId()) :
                     e.getCollabParams();
         
         // !!! THIS SHOULD GO IN SOURCECOLUMNMANAGER
         // If we are the main window we need to determine the display columns we are moving to and
         // from. If we can not determine these, cancel the tab change to prevent the tab from
         // moving to the wrong tab or having duplicate instances of the same tab. newDisplay must
         // be final to be used in the onResponseReceived function below.
         final SourceColumn newDisplay = SourceWindowManager.isMainSourceWindow() ?
                                   columnManager_.findByPosition(e.getXPos()) :
                                   null;
         if (SourceWindowManager.isMainSourceWindow())
         {
            if (newDisplay == null)
            {
               Debug.logWarning("Could not determine new source column."); 
               return;
            }
            // when we're moving between columns in the main source window
            // we need to set the oldDisplay before we add the new one
            oldDisplay = e.getOldWindowId() == e.getNewWindowId() ? 
                         columnManager_.findByDocument(e.getDocId()) :
                         columnManager_.getActive();
            // the event doesn't contain the display info, so we add it now
            pWindowManager_.get().assignSourceDocDisplay(e.getDocId(), newDisplay.getName(), true);
         }

         // if we're the adopting window, add the doc
         server_.getSourceDocument(e.getDocId(),
               new ServerRequestCallback<SourceDocument>()
         {
            @Override
            public void onResponseReceived(final SourceDocument doc)
            {
               final EditingTarget target = columnManager_.addTab(doc, e.getPos(), newDisplay);
               
               Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     // if there was a collab session, resume it
                     if (collabParams != null)
                        target.beginCollabSession(e.getCollabParams());
                  }
               });
            }

            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage("Document Tab Move Failed", 
                     "Couldn't move the tab to this window: \n" + 
                      error.getMessage());
            }
         });
      }

      // the new and old window Ids will be the same when moving documents between columns
      // in the main source window
      if (e.getOldWindowId() == SourceWindowManager.getSourceWindowId())
      {
         if (oldDisplay == null)
         {
            oldDisplay = SourceWindowManager.isMainSourceWindow() ?
                         columnManager_.findByDocument(e.getDocId()) :
                         columnManager_.getActive();
         }
         // cancel tab drag if it was occurring
         oldDisplay.cancelTabDrag();
         
         // disown this doc if it was our own
         columnManager_.disownDoc(e.getDocId());
      }
   }
   
   @Override
   public void onDocTabDragInitiated(final DocTabDragInitiatedEvent event)
   {
      columnManager_.inEditorForId(event.getDragParams().getDocId(), 
            new OperationWithInput<EditingTarget>()
      {
         @Override
         public void execute(EditingTarget editor)
         {
            DocTabDragParams params = event.getDragParams();
            params.setSourcePosition(editor.currentPosition());
            params.setDisplayName(columnManager_.findByDocument(editor.getId()).getName());
            events_.fireEvent(new DocTabDragStartedEvent(params));
          }
       });
   }
      
   @Override
   public void onPopoutDocInitiated(final PopoutDocInitiatedEvent event)
   {
      columnManager_.inEditorForId(event.getDocId(), new OperationWithInput<EditingTarget>()
      {
         @Override
         public void execute(EditingTarget editor)
         {
            // if this is a text editor, ensure that its content is 
            // synchronized with the server before we pop it out
            if (editor instanceof TextEditingTarget)
            {
               final TextEditingTarget textEditor = (TextEditingTarget)editor;
               textEditor.withSavedDoc(new Command()
               {
                  @Override
                  public void execute()
                  {
                     textEditor.syncLocalSourceDb();
                     events_.fireEvent(new PopoutDocEvent(event, 
                        textEditor.currentPosition(),
                        columnManager_.findByDocument(textEditor.getId())));
                  }
               });
            }
            else
            {
               events_.fireEvent(new PopoutDocEvent(event, 
                     editor.currentPosition(),
                     columnManager_.findByDocument(editor.getId())));
            }
         }
      });
   }

   @Handler
   public void onCloseSourceDoc()
   {
      closeSourceDoc(true);
   }
   
   void closeSourceDoc(boolean interactive)
   {
      if (columnManager_.getActive().getTabCount() == 0)
         return;
      
      columnManager_.closeTab(interactive);
   }
   
   @Handler
   public void onSaveAllSourceDocs()
   {
      /*
      // Save all documents in the main window
      columnManager_.cpsExecuteForEachEditor(editors_, new CPSEditingTargetCommand()
      {
         @Override
         public void execute(EditingTarget target, Command continuation)
         {
            if (target.dirtyState().getValue())
            {
               target.save(continuation);
            }
            else
            {
               continuation.execute();
            }
         }
      });

      // Save all documents in satellite windows
      pWindowManager_.get().saveUnsavedDocuments(null, null);
      */
      Debug.logToConsole("onSaveAllSourceDocs not coded");
   }
   
   
   private void saveEditingTargetsWithPrompt(
                                       String title,
                                       ArrayList<EditingTarget> editingTargets,
                                       final Command onCompleted,
                                       final Command onCancelled)
   {
      // execute on completed right away if the list is empty
      if (editingTargets.size() ==  0)
      {
         onCompleted.execute();
      }
      
      // if there is just one thing dirty then go straight to the save dialog
      else if (editingTargets.size() == 1)
      {
         editingTargets.get(0).saveWithPrompt(onCompleted, onCancelled);
      }
      
      // otherwise use the multi save changes dialog
      else
      {
         // convert to UnsavedChangesTarget collection
         ArrayList<UnsavedChangesTarget> unsavedTargets = 
                                    new ArrayList<UnsavedChangesTarget>();
         unsavedTargets.addAll(editingTargets);
         
         // show dialog
         columnManager_.showUnsavedChangesDialog(
            title,
            unsavedTargets, 
            new OperationWithInput<UnsavedChangesDialog.Result>() 
            {
               @Override
               public void execute(UnsavedChangesDialog.Result result)
               {
                  saveChanges(result.getSaveTargets(), onCompleted);
               }
            },
            onCancelled); 
      }
   }
   
   private void saveChanges(ArrayList<UnsavedChangesTarget> targets,
                                 Command onCompleted)
   {
      // convert back to editing targets
      ArrayList<EditingTarget> saveTargets = 
                                    new ArrayList<EditingTarget>();
      for (UnsavedChangesTarget target: targets)
      {
         EditingTarget saveTarget = 
                           columnManager_.findEditor(target.getId());
         if (saveTarget != null)
            saveTargets.add(saveTarget);
      }
        
      SourceColumnManager.CPSEditingTargetCommand saveCommand =
         new SourceColumnManager.CPSEditingTargetCommand()
      {
         @Override
         public void execute(EditingTarget saveTarget, 
                             Command continuation)
         {         
            saveTarget.save(continuation); 
         }
      };
         
      // execute the save
      columnManager_.cpsExecuteForEachEditor(
         
         // targets the user chose to save
         saveTargets, 
         
         // save each editor
         saveCommand,

         // onCompleted at the end
         onCompleted
      );          
   }
   
   @Handler
   public void onCloseAllSourceDocs()
   {
      closeAllSourceDocs("Close All",  null, false);
   }
   
   @Handler
   public void onCloseOtherSourceDocs()
   {
      closeAllSourceDocs("Close Other",  null, true);
   }
   
   public void closeAllSourceDocs(final String caption, 
         final Command onCompleted, final boolean excludeActive)
   { 
      if (SourceWindowManager.isMainSourceWindow() && !excludeActive)
      {
         // if this is the main window, close docs in the satellites first 
         pWindowManager_.get().closeAllSatelliteDocs(caption, new Command()
         {
            @Override
            public void execute()
            {
               closeAllLocalSourceDocs(caption, onCompleted, excludeActive);
            }
         });
      }
      else
      {
         // this is a satellite (or we don't need to query satellites)--just
         // close our own tabs
         closeAllLocalSourceDocs(caption, onCompleted, excludeActive);
      }
  }

  private void closeAllLocalSourceDocs(String caption, Command onCompleted,
           final boolean excludeActive)
  {
     /*
      // save active editor for exclusion (it changes as we close tabs)
      final EditingTarget activeEditor = activeEditor_;
      
      // collect up a list of dirty documents
      ArrayList<EditingTarget> dirtyTargets = new ArrayList<EditingTarget>();
      for (EditingTarget target : editors_)
      {
         if (excludeActive && target == activeEditor)
            continue;
         if (target.dirtyState().getValue())
            dirtyTargets.add(target);
      }
      
      // create a command used to close all tabs 
      final Command closeAllTabsCommand = new Command()
      {
         @Override
         public void execute()
         {
            columnManager_.closeAllTabs(excludeActive, false);
         }     
      };
      
      // save targets
      saveEditingTargetsWithPrompt(caption,
                                   dirtyTargets, 
                                   CommandUtil.join(closeAllTabsCommand,
                                                    onCompleted),
                                   null);
      
                                   */
     Debug.logToConsole("close AllLocalSourceDocs not coded");
   }
  
   private boolean isUnsavedTarget(EditingTarget target, int type)
   {
      boolean fileBacked = target.getPath() != null;
      return target.dirtyState().getValue() && 
              ((type == TYPE_FILE_BACKED &&  fileBacked) ||
               (type == TYPE_UNTITLED    && !fileBacked));
   }
   
   public ArrayList<UnsavedChangesTarget> getUnsavedChanges(int type)
   {
      return getUnsavedChanges(type,  null);
   }
   
   public ArrayList<UnsavedChangesTarget> getUnsavedChanges(int type, Set<String> ids)
   {
      ArrayList<UnsavedChangesTarget> targets = 
                                       new ArrayList<UnsavedChangesTarget>();
      /*

      // if this is the main window, collect all unsaved changes from 
      // the satellite windows as well
      if (SourceWindowManager.isMainSourceWindow())
      {
         targets.addAll(pWindowManager_.get().getAllSatelliteUnsavedChanges(type));
      }

      for (EditingTarget target : editors_)
      {
         // no need to save targets which are up-to-date
         if (!isUnsavedTarget(target, type))
            continue;
         
         // if we've requested the save of specific documents, screen
         // out documents not within the requested id set
         if (ids != null && !ids.contains(target.getId()))
            continue;
         
         targets.add(target);
      }
      
      */
      Debug.logToConsole("UnsavedChanges not coded");
      return targets;
   }
   
   public void saveUnsavedDocuments(final Command onCompleted)
   {
      saveUnsavedDocuments(null, onCompleted);
   }
   
   public void saveUnsavedDocuments(final Set<String> ids,
                                    final Command onCompleted)
   {
      Command saveAllLocal = new Command()
      {
         @Override
         public void execute()
         {
            saveChanges(getUnsavedChanges(TYPE_FILE_BACKED, ids), onCompleted);
         }
      };
      
      // if this is the main source window, save all files in satellites first
      if (SourceWindowManager.isMainSourceWindow())
         pWindowManager_.get().saveUnsavedDocuments(ids, saveAllLocal);
      else
         saveAllLocal.execute();
   }
   
   public void saveWithPrompt(UnsavedChangesTarget target, 
                              Command onCompleted,
                              Command onCancelled)
   {
      if (SourceWindowManager.isMainSourceWindow() &&
          !pWindowManager_.get().getWindowIdOfDocId(target.getId()).isEmpty())
      {
         // we are the main window, and we're being asked to save a document
         // that's in a different window; perform the save over there
         pWindowManager_.get().saveWithPrompt(UnsavedChangesItem.create(target), 
               onCompleted);
         return;
      }
      EditingTarget editingTarget = columnManager_.findEditor(target.getId());
      if (editingTarget != null)
         editingTarget.saveWithPrompt(onCompleted, onCancelled);
   }
   
   public Command revertUnsavedChangesBeforeExitCommand(
                                               final Command onCompleted)
   {
      return () -> handleUnsavedChangesBeforeExit(new ArrayList<UnsavedChangesTarget>(),
                                                  onCompleted);
   }
   public void handleUnsavedChangesBeforeExit(
                        final ArrayList<UnsavedChangesTarget> saveTargets,
                        final Command onCompleted)
   {
      // first handle saves, then revert unsaved, then callback on completed
      final Command completed = new Command() {
         @Override
         public void execute()
         {
            // revert unsaved
            revertUnsavedTargets(onCompleted);
         }
      };   

      // if this is the main source window, let satellite windows save any
      // changes first
      if (SourceWindowManager.isMainSourceWindow())
      {
         pWindowManager_.get().handleUnsavedChangesBeforeExit(
               saveTargets, new Command()
         {
            @Override
            public void execute()
            {
               saveChanges(saveTargets, completed);
            }
         });
      }
      else
      {
         saveChanges(saveTargets, completed);
      }
   }
   
   public SourceColumn getActiveView()
   {
      return columnManager_.getActive();
   }

   public void activateView(Display display)
   {
      //display.focus();
   }

   private void revertUnsavedTargets(Command onCompleted)
   {
      Debug.logToConsole("UnsavedChanges not coded");
      /*
      // collect up unsaved targets
      ArrayList<EditingTarget> unsavedTargets =  new ArrayList<EditingTarget>();
      for (EditingTarget target : editors_)
         if (isUnsavedTarget(target, TYPE_FILE_BACKED))
            unsavedTargets.add(target);
      
      // revert all of them
      columnManager_.cpsExecuteForEachEditor(
         
         // targets the user chose not to save
         unsavedTargets, 
         
         // save each editor
         new CPSEditingTargetCommand()
         {
            @Override
            public void execute(EditingTarget saveTarget, 
                                Command continuation)
            {
               if (saveTarget.getPath() != null)
               {
                  // file backed document -- revert it
                  saveTarget.revertChanges(continuation);
               }
               else
               {
                  // untitled document -- just close the tab non-interactively
                  columnManager_.closeTab(saveTarget, false, continuation);
               }
            }
         },
         
         // onCompleted at the end
         onCompleted
      );          
            
      */
   }
   
   @Handler
   public void onOpenSourceDoc()
   {
      fileDialogs_.openFile(
            "Open File",
            fileContext_,
            workbenchContext_.getDefaultFileDialogDir(),
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(final FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  if (input == null)
                     return;

                  workbenchContext_.setDefaultFileDialogDir(
                                                   input.getParentPath());

                  indicator.onCompleted();
                  Scheduler.get().scheduleDeferred(new ScheduledCommand()
                  {
                     public void execute()
                     {
                        fileTypeRegistry_.openFile(input);
                     }
                  });
               }
            });
   }
   
   public void onNewDocumentWithCode(final NewDocumentWithCodeEvent event)
   {
      // determine the type
      final EditableFileType docType;
      if (event.getType() == NewDocumentWithCodeEvent.R_SCRIPT)
         docType = FileTypeRegistry.R;
      else if (event.getType() == NewDocumentWithCodeEvent.SQL)
         docType = FileTypeRegistry.SQL;
      else
         docType = FileTypeRegistry.RMARKDOWN;
      
      // command to create and run the new doc
      Command newDocCommand = new Command() {
         @Override
         public void execute()
         {
            columnManager_.newDoc(docType,
                   event.getCode(),
                   new ResultCallback<EditingTarget, ServerError>() {
               public void onSuccess(EditingTarget arg)
               {
                  TextEditingTarget editingTarget = (TextEditingTarget)arg;
                  
                  if (event.getCursorPosition() != null)
                  {
                     editingTarget.navigateToPosition(event.getCursorPosition(),
                                                      false);
                  }
                  
                  if (event.getExecute())
                  {
                     if (docType.equals(FileTypeRegistry.R))
                     {
                        commands_.executeToCurrentLine().execute();
                        commands_.activateSource().execute();
                     }
                     else if (docType.equals(FileTypeRegistry.SQL))
                     {
                        commands_.previewSql().execute();
                     }
                     else
                     {
                        commands_.executePreviousChunks().execute();
                     }
                  }
               }
            });
         }
      };
     
      // do it
      if (docType.equals(FileTypeRegistry.R))
      {
         newDocCommand.execute();
      }
      else
      {
         dependencyManager_.withRMarkdown("R Notebook",
                                          "Create R Notebook", 
                                          newDocCommand);
      }
   }
   
   @Handler
   public void onNewRPlumberDoc()
   {
      dependencyManager_.withRPlumber("Creating R Plumber API", new Command()
      {
         @Override
         public void execute()
         {
            NewPlumberAPI widget = new NewPlumberAPI(
                  "New Plumber API",
                  new OperationWithInput<NewPlumberAPI.Result>()
                  {
                     @Override
                     public void execute(NewPlumberAPI.Result input)
                     {
                        doNewRPlumberAPI(input);
                     }
                  });
         
            widget.showModal();

         }
      });
   }
    
   public void onOpenSourceFile(final OpenSourceFileEvent event)
   {
      doOpenSourceFile(event.getFile(),
                     event.getFileType(),
                     event.getPosition(),
                     null, 
                     event.getNavigationMethod(),
                     false);
   }
   
   public void onOpenPresentationSourceFile(OpenPresentationSourceFileEvent event)
   {
      // don't do the navigation if the active document is a source
      // file from this presentation module
      
      doOpenSourceFile(event.getFile(),
                       event.getFileType(),
                       event.getPosition(),
                       event.getPattern(),
                       NavigationMethods.HIGHLIGHT_LINE,
                       true);
      
   }
   
   public void onEditPresentationSource(final EditPresentationSourceEvent event)
   { 
      columnManager_.openFile(
            event.getSourceFile(), 
            FileTypeRegistry.RPRESENTATION,
            new CommandWithArg<EditingTarget>() {
               @Override
               public void execute(final EditingTarget editor)
               {   
                  TextEditingTargetPresentationHelper.navigateToSlide(
                                                         editor, 
                                                         event.getSlideIndex());
               }
         });
   }
   
   public void forceLoad()
   {
      AceEditor.preload();
   }

   public String getCurrentDocId()
   {
      if (getActiveEditor() == null)
         return null;
      return getActiveEditor().getId();
   }

   public String getCurrentDocPath()
   {
      if (getActiveEditor() == null)
         return null;
      return getActiveEditor().getPath();
   }
   
   public SourceColumnManager getColumnManager()
   {
      return columnManager_;
   }

   private void doOpenSourceFile(final FileSystemItem file,
                                 final TextFileType fileType,
                                 final FilePosition position,
                                 final String pattern,
                                 final int navMethod, 
                                 final boolean forceHighlightMode)
   {
      // if the navigation should happen in another window, do that instead
      NavigationResult navResult = 
            pWindowManager_.get().navigateToFile(file, position, navMethod);
      
      // we navigated externally, just skip this
      if (navResult.getType() == NavigationResult.RESULT_NAVIGATED)
         return;
      
      // we're about to open in this window--if it's the main window, focus it
      if (SourceWindowManager.isMainSourceWindow() && Desktop.hasDesktopFrame())
         Desktop.getFrame().bringMainFrameToFront();
      
      final boolean isDebugNavigation = 
            navMethod == NavigationMethods.DEBUG_STEP ||
            navMethod == NavigationMethods.DEBUG_END;
      
      final CommandWithArg<EditingTarget> editingTargetAction = 
            new CommandWithArg<EditingTarget>() 
      {
         @Override
         public void execute(EditingTarget target)
         {
            // the rstudioapi package can use the proxy (-1, -1) position to
            // indicate that source navigation should not occur; ie, we should
            // preserve whatever position was used in the document earlier
            boolean navigateToPosition =
                  position != null &&
                  (position.getLine() != -1 || position.getColumn() != -1);
            
            if (navigateToPosition)
            {
               SourcePosition endPosition = null;
               if (isDebugNavigation)
               {
                  DebugFilePosition filePos = 
                        (DebugFilePosition) position.cast();
                  endPosition = SourcePosition.create(
                        filePos.getEndLine() - 1,
                        filePos.getEndColumn() + 1);
                  
                  if (Desktop.hasDesktopFrame() &&
                      navMethod != NavigationMethods.DEBUG_END)
                      Desktop.getFrame().bringMainFrameToFront();
               }
               navigate(target, 
                        SourcePosition.create(position.getLine() - 1,
                                              position.getColumn() - 1),
                        endPosition);
            }
            else if (pattern != null)
            {
               Position pos = target.search(pattern);
               if (pos != null)
               {
                  navigate(target, 
                           SourcePosition.create(pos.getRow(), 0),
                           null);
               }
            }
         }
         
         private void navigate(final EditingTarget target,
                               final SourcePosition srcPosition,
                               final SourcePosition srcEndPosition)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  if (navMethod == NavigationMethods.DEBUG_STEP)
                  {
                     target.highlightDebugLocation(
                           srcPosition, 
                           srcEndPosition, 
                           true);
                  }
                  else if (navMethod == NavigationMethods.DEBUG_END)
                  {
                     target.endDebugHighlighting();
                  }
                  else
                  {
                     // force highlight mode if requested
                     if (forceHighlightMode)
                        target.forceLineHighlighting();
                     
                     // now navigate to the new position
                     boolean highlight = 
                           navMethod == NavigationMethods.HIGHLIGHT_LINE &&
                           !userPrefs_.highlightSelectedLine().getValue();
                     target.navigateToPosition(srcPosition,
                                               false,
                                               highlight);
                  }
               }
            });
         }
      };

      if (navResult.getType() == NavigationResult.RESULT_RELOCATE)
      {
         server_.getSourceDocument(navResult.getDocId(),
               new ServerRequestCallback<SourceDocument>()
         {
            @Override
            public void onResponseReceived(final SourceDocument doc)
            {
               editingTargetAction.execute(columnManager_.addTab(doc, OPEN_REPLAY, null));
            }

            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage("Document Tab Move Failed", 
                     "Couldn't move the tab to this window: \n" + 
                      error.getMessage());
            }
         });
         return;
      }

      final CommandWithArg<FileSystemItem> action = new CommandWithArg<FileSystemItem>()
      {
         @Override
         public void execute(FileSystemItem file)
         {
            // set flag indicating we are opening for a source navigation
            // !!! uncomment on move openingForSourceNavigation_ = position != null || pattern != null;
            
            columnManager_.openFile(file,
                     fileType,
                     (target) -> {
                        // !!! uncomment on move openingForSourceNavigation_ = false;
                        editingTargetAction.execute(target);
                     });      
         }
      };

      // If this is a debug navigation, we only want to treat this as a full
      // file open if the file isn't already open; otherwise, we can just
      // highlight in place.
      if (isDebugNavigation)
      {
         columnManager_.startDebug();
         
         EditingTarget target = columnManager_.findEditorByPath(file.getPath());
         if (target != null)
         {
            // the file's open; just update its highlighting 
            if (navMethod == NavigationMethods.DEBUG_END)
            {
               target.endDebugHighlighting();
            }
            else
            {
               columnManager_.selectTab(target);
               editingTargetAction.execute(target);
            }
            return;
         }
         
         // If we're here, the target file wasn't open in an editor. Don't
         // open a file just to turn off debug highlighting in the file!
         if (navMethod == NavigationMethods.DEBUG_END)
            return;
      }

      // Warning: event.getFile() can be null (e.g. new Sweave document)
      if (file != null && file.getLength() < 0)
      {
         statQueue_.add(new StatFileEntry(file, action));
         if (statQueue_.size() == 1)
            processStatQueue();
      }
      else
      {
         action.execute(file);
      }
   }
   
   private void processStatQueue()
   {
      if (statQueue_.isEmpty())
         return;
      final StatFileEntry entry = statQueue_.peek();
      final Command processNextEntry = new Command()
            {
               @Override
               public void execute()
               {
                  statQueue_.remove();
                  if (!statQueue_.isEmpty())
                     processStatQueue();
               }
            };
       
       server_.stat(entry.file.getPath(), new ServerRequestCallback<FileSystemItem>()
       {
          @Override
          public void onResponseReceived(FileSystemItem response)
          {
             processNextEntry.execute();
             entry.action.execute(response);
          }

          @Override
          public void onError(ServerError error)
          {
             processNextEntry.execute();
             // Couldn't stat the file? Proceed anyway. If the file doesn't
             // exist, we'll let the downstream code be the one to show the
             // error.
             entry.action.execute(entry.file);
          }
        });
   }

   Widget createWidget(EditingTarget target)
   {
      return target.asWidget();
   }
   
   /*
   private EditingTarget addTab(SourceDocument doc, Integer position, 
         int mode, Display display)
   {
      final String defaultNamePrefix = editingTargetSource_.getDefaultNamePrefix(doc);
      final EditingTarget target = editingTargetSource_.getEditingTarget(
            doc, fileContext_, new Provider<String>()
            {
               public String get()
               {
                  return getNextDefaultName(defaultNamePrefix);
               }
            });
      final Display targetView = display != null ? display : columnManager_.getActive();
      
      final Widget widget = createWidget(target);

      if (position == null)
      {
         editors_.add(target);
      }
      else
      {
         // we're inserting into an existing permuted tabset -- push aside
         // any tabs physically to the right of this tab
         editors_.add(position, target);
         for (int i = 0; i < tabOrder_.size(); i++)
         {
            int pos = tabOrder_.get(i);
            if (pos >= position)
               tabOrder_.set(i, pos + 1);
         }

         // add this tab in its "natural" position
         tabOrder_.add(position, position);
      }

      targetView.addEditor(target);
      targetView.addTab(widget,
                        target.getIcon(),
                        target.getId(),
                        target.getName().getValue(),
                        target.getTabTooltip(), // used as tooltip, if non-null
                        position,
                        true);
      fireDocTabsChanged();

      target.getName().addValueChangeHandler(new ValueChangeHandler<String>()
      {
         public void onValueChange(ValueChangeEvent<String> event)
         {
            targetView.renameTab(widget,
                                 target.getIcon(),
                                 event.getValue(),
                                 target.getPath());
            fireDocTabsChanged();
         }
      });

      targetView.setDirty(widget, target.dirtyState().getValue());
      target.dirtyState().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            targetView.setDirty(widget, event.getValue());
            manageCommands();
         }
      });

      target.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            targetView.selectTab(widget);
         }
      });

      target.addCloseHandler(new CloseHandler<Void>()
      {
         public void onClose(CloseEvent<Void> voidCloseEvent)
         {
            editors_.remove(target);
         }
      });
      
      events_.fireEvent(new SourceDocAddedEvent(doc, mode, targetView.getName()));
      
      if (target instanceof TextEditingTarget && doc.isReadOnly())
      {
         ((TextEditingTarget) target).setIntendedAsReadOnly(
               JsUtil.toList(doc.getReadOnlyAlternatives()));
      }
      
      // adding a tab may enable commands that are only available when 
      // multiple documents are open; if this is the second document, go check
      if (editors_.size() == 2)
         manageMultiTabCommands();

      // if the target had an editing session active, attempt to resume it
      if (doc.getCollabParams() != null)
         target.beginCollabSession(doc.getCollabParams());

      return target;
   }
*/

   public void onInsertSource(final InsertSourceEvent event)
   {
      if (activeEditor_ != null
          && activeEditor_ instanceof TextEditingTarget
          && commands_.executeCode().isEnabled())
      {
         TextEditingTarget textEditor = (TextEditingTarget) activeEditor_;
         textEditor.insertCode(event.getCode(), event.isBlock());
      }
      else
      {
         columnManager_.newDoc(FileTypeRegistry.R,
                        new ResultCallback<EditingTarget, ServerError>()
         {
            public void onSuccess(EditingTarget arg)
            {
               ((TextEditingTarget)arg).insertCode(event.getCode(),
                                                   event.isBlock());
            }
         });
      }
   }

   public void closeEditorIfActive(EditingTarget target)
   {
      if (activeEditor_ == target)
      {
         activeEditor_.onDeactivate();
         activeEditor_ = null;
      }
   }

   private void manageCommands()
   {
      manageCommands(false);
   }
   
   // !!! shouldn't be public
   public void manageCommands(boolean forceSync)
   {
     Debug.logToConsole("manageCommands not coded");
      /*
      boolean hasDocs = editors_.size() > 0;

      String name = "Go to to the definition of the currently selected function";

      commands_.newSourceDoc().setEnabled(true);
      commands_.closeSourceDoc().setEnabled(hasDocs);
      commands_.closeAllSourceDocs().setEnabled(hasDocs);
      commands_.nextTab().setEnabled(hasDocs);
      commands_.previousTab().setEnabled(hasDocs);
      commands_.firstTab().setEnabled(hasDocs);
      commands_.lastTab().setEnabled(hasDocs);
      commands_.switchToTab().setEnabled(hasDocs);
      commands_.setWorkingDirToActiveDoc().setEnabled(hasDocs);

      HashSet<AppCommand> newCommands = activeEditor_ != null
            ? activeEditor_.getSupportedCommands()
            : new HashSet<AppCommand>();
      
      if (forceSync)
      {
         for (AppCommand command : activeCommands_)
         {
            if (StringUtil.equals(command.getDesc(), name))
               Debug.logToConsole("disable command");
            command.setEnabled(false);
            command.setVisible(false);
         }
         
         for (AppCommand command : newCommands)
         {
            if (StringUtil.equals(command.getDesc(), name))
               Debug.logToConsole("enable command");
            command.setEnabled(true);
            command.setVisible(true);
         }
      }
      else
      {
         HashSet<AppCommand> commandsToEnable = new HashSet<AppCommand>(newCommands);
         commandsToEnable.removeAll(activeCommands_);

         HashSet<AppCommand> commandsToDisable = new HashSet<AppCommand>(activeCommands_);
         commandsToDisable.removeAll(newCommands);

         for (AppCommand command : commandsToEnable)
         {
            if (StringUtil.equals(command.getDesc(), name))
               Debug.logToConsole("enable command");
            command.setEnabled(true);
            command.setVisible(true);
         }

         for (AppCommand command : commandsToDisable)
         {
            if (StringUtil.equals(command.getDesc(), name))
               Debug.logToConsole("disable command");
            command.setEnabled(false);
            command.setVisible(false);
         }
      }
      
      // commands which should always be visible even when disabled
      commands_.saveSourceDoc().setVisible(true);
      commands_.saveSourceDocAs().setVisible(true);
      commands_.printSourceDoc().setVisible(true);
      commands_.setWorkingDirToActiveDoc().setVisible(true);
      commands_.debugBreakpoint().setVisible(true);
      
      // manage synctex commands
      manageSynctexCommands();
      
      // manage vcs commands
      manageVcsCommands();
      
      // manage save and save all
      manageSaveCommands();
      
      // manage source navigation
      manageSourceNavigationCommands();
      
      // manage RSConnect commands
      manageRSConnectCommands();
      
      // manage R Markdown commands
      manageRMarkdownCommands();
      
      // manage multi-tab commands
      manageMultiTabCommands();
      
      manageTerminalCommands();
      
      activeCommands_ = newCommands;
      
      // give the active editor a chance to manage commands
      if (activeEditor_ != null)
         activeEditor_.manageCommands();

      assert verifyNoUnsupportedCommands(newCommands)
            : "Unsupported commands detected (please add to Source.dynamicCommands_)";
               */
   }
   
   // !! should be private
   public void manageMultiTabCommands()
   {
      Debug.logToConsole("manageMultiTabCommands not coded");
      /*
      boolean hasMultipleDocs = editors_.size() > 1;

      // special case--these editing targets always support popout, but it's
      // nonsensical to show it if it's the only tab in a satellite; hide it in
      // this case
      if (commands_.popoutDoc().isEnabled() &&
          activeEditor_ != null &&
          (activeEditor_ instanceof TextEditingTarget ||
           activeEditor_ instanceof CodeBrowserEditingTarget) &&
          !SourceWindowManager.isMainSourceWindow())
      {
         commands_.popoutDoc().setVisible(hasMultipleDocs);
      }
      
      commands_.closeOtherSourceDocs().setEnabled(hasMultipleDocs);
      */
   }
   
   private void manageSynctexCommands()
   {
      // synctex commands are enabled if we have synctex for the active editor
      boolean synctexAvailable = synctex_.isSynctexAvailable();
      if (synctexAvailable)
      {
         if ((activeEditor_ != null) && 
             (activeEditor_.getPath() != null) &&
             activeEditor_.canCompilePdf())
         {
            synctexAvailable = synctex_.isSynctexAvailable();
         }
         else
         {
            synctexAvailable = false;
         }
      }
     
      synctex_.enableCommands(synctexAvailable);
   }
   
   private void manageVcsCommands()
   {
      // manage availablity of vcs commands
      boolean vcsCommandsEnabled = 
            session_.getSessionInfo().isVcsEnabled() &&
            (activeEditor_ != null) &&
            (activeEditor_.getPath() != null) &&
            activeEditor_.getPath().startsWith(
                  session_.getSessionInfo().getActiveProjectDir().getPath());
      
      commands_.vcsFileLog().setVisible(vcsCommandsEnabled);
      commands_.vcsFileLog().setEnabled(vcsCommandsEnabled);
      commands_.vcsFileDiff().setVisible(vcsCommandsEnabled);
      commands_.vcsFileDiff().setEnabled(vcsCommandsEnabled);
      commands_.vcsFileRevert().setVisible(vcsCommandsEnabled);
      commands_.vcsFileRevert().setEnabled(vcsCommandsEnabled);
          
      if (vcsCommandsEnabled)
      {
         String name = FileSystemItem.getNameFromPath(activeEditor_.getPath());
         commands_.vcsFileDiff().setMenuLabel("_Diff \"" + name + "\"");
         commands_.vcsFileLog().setMenuLabel("_Log of \"" + name +"\"");
         commands_.vcsFileRevert().setMenuLabel("_Revert \"" + name + "\"...");
      }
      
      boolean isGithubRepo = session_.getSessionInfo().isGithubRepository();
      if (vcsCommandsEnabled && isGithubRepo)
      {
         String name = FileSystemItem.getNameFromPath(activeEditor_.getPath());
         
         commands_.vcsViewOnGitHub().setVisible(true);
         commands_.vcsViewOnGitHub().setEnabled(true);
         commands_.vcsViewOnGitHub().setMenuLabel(
                                  "_View \"" + name + "\" on GitHub");
         
         commands_.vcsBlameOnGitHub().setVisible(true);
         commands_.vcsBlameOnGitHub().setEnabled(true);
         commands_.vcsBlameOnGitHub().setMenuLabel(
                                  "_Blame \"" + name + "\" on GitHub");
      }
      else
      {
         commands_.vcsViewOnGitHub().setVisible(false);
         commands_.vcsViewOnGitHub().setEnabled(false);
         commands_.vcsBlameOnGitHub().setVisible(false);
         commands_.vcsBlameOnGitHub().setEnabled(false);
      }
   }
   
   private void manageRSConnectCommands()
   {
      boolean rsCommandsAvailable = 
            SessionUtils.showPublishUi(session_, userState_) &&
            (activeEditor_ != null) &&
            (activeEditor_.getPath() != null) &&
            ((activeEditor_.getExtendedFileType() != null &&
              activeEditor_.getExtendedFileType() .startsWith(SourceDocument.XT_SHINY_PREFIX)) ||
              activeEditor_.getExtendedFileType() == SourceDocument.XT_RMARKDOWN ||
              activeEditor_.getExtendedFileType() == SourceDocument.XT_PLUMBER_API);
      commands_.rsconnectDeploy().setVisible(rsCommandsAvailable);
      if (activeEditor_ != null)
      {
         String deployLabel = null;
         if (activeEditor_.getExtendedFileType() != null)
         {
            if (activeEditor_.getExtendedFileType().startsWith(SourceDocument.XT_SHINY_PREFIX))
            {
               deployLabel = "Publish Application...";
            }
            else if (activeEditor_.getExtendedFileType() == SourceDocument.XT_PLUMBER_API)
            {
               deployLabel = "Publish Plumber API..."; 
            }
         }
         if (deployLabel == null)
            deployLabel = "Publish Document...";
   
         commands_.rsconnectDeploy().setLabel(deployLabel);
      }
      commands_.rsconnectConfigure().setVisible(rsCommandsAvailable);
   }
   
   private void manageRMarkdownCommands()
   {
      boolean rmdCommandsAvailable = 
            session_.getSessionInfo().getRMarkdownPackageAvailable() &&
            (activeEditor_ != null) &&
            activeEditor_.getExtendedFileType() == SourceDocument.XT_RMARKDOWN;
      commands_.editRmdFormatOptions().setVisible(rmdCommandsAvailable);
      commands_.editRmdFormatOptions().setEnabled(rmdCommandsAvailable);
   }
   
   // !!! should be private
   public void manageSaveCommands()
   {
      boolean saveEnabled = (activeEditor_ != null) &&
            activeEditor_.isSaveCommandActive();
      commands_.saveSourceDoc().setEnabled(saveEnabled);
      manageSaveAllCommand();
   }
   
   
   private void manageSaveAllCommand()
   {
      Debug.logToConsole("manageSaveAllCommands not coded");
      /*
      // if source windows are open, managing state of the command becomes
      // complicated, so leave it enabled
      if (pWindowManager_.get().areSourceWindowsOpen())
      {
         commands_.saveAllSourceDocs().setEnabled(true);
         return;
      }

      // if one document is dirty then we are enabled
      for (EditingTarget target : editors_)
      {
         if (target.isSaveCommandActive())
         {
            commands_.saveAllSourceDocs().setEnabled(true);
            return;
         }
      }
      
      // not one was dirty, disabled
      commands_.saveAllSourceDocs().setEnabled(false);
      */
   }
   
   private void manageTerminalCommands()
   {
      if (!session_.getSessionInfo().getAllowShell())
         commands_.sendToTerminal().setVisible(false);
   }
   
   private boolean verifyNoUnsupportedCommands(HashSet<AppCommand> commands)
   {
      HashSet<AppCommand> temp = new HashSet<AppCommand>(commands);
      temp.removeAll(dynamicCommands_);
      return temp.size() == 0;
   }

   public void onFileEdit(FileEditEvent event)
   {
      if (SourceWindowManager.isMainSourceWindow())
      {
         fileTypeRegistry_.editFile(event.getFile());
      }
   }

   @Handler
   public void onSourceNavigateBack()
   {
      if (!sourceNavigationHistory_.isForwardEnabled())
      {
         if (activeEditor_ != null)
            activeEditor_.recordCurrentNavigationPosition();
      }

      SourceNavigation navigation = sourceNavigationHistory_.goBack();
      if (navigation != null)
         attemptSourceNavigation(navigation, commands_.sourceNavigateBack());
   }
   
   @Handler
   public void onSourceNavigateForward()
   {
      SourceNavigation navigation = sourceNavigationHistory_.goForward();
      if (navigation != null)
         attemptSourceNavigation(navigation, commands_.sourceNavigateForward());
   }
   
   @Handler
   public void onOpenNextFileOnFilesystem()
   {
      openAdjacentFile(true);
   }
   
   @Handler
   public void onOpenPreviousFileOnFilesystem()
   {
      openAdjacentFile(false);
   }
   
   @Handler
   public void onSpeakEditorLocation()
   {
      String announcement;
      if (activeEditor_ == null)
         announcement = "No document tabs open";
      else
      {
         announcement = activeEditor_.getCurrentStatus();
      }
      ariaLive_.announce(AriaLiveService.ON_DEMAND, announcement, Timing.IMMEDIATE, Severity.STATUS);
   }
   
   @Handler
   public void onZoomIn()
   {
      if (Desktop.hasDesktopFrame())
      {
         Desktop.getFrame().zoomIn();
      }
   }
   
   @Handler
   public void onZoomOut()
   {
      if (Desktop.hasDesktopFrame())
      {
         Desktop.getFrame().zoomOut();
      }
   }
   
   @Handler
   public void onZoomActualSize()
   {
      if (Desktop.hasDesktopFrame())
      {
         Desktop.getFrame().zoomActualSize();
      }
   }
   
   private void openAdjacentFile(final boolean forward)
   {
      // ensure we have an editor and a titled document is open
      if (activeEditor_ == null || StringUtil.isNullOrEmpty(activeEditor_.getPath()))
         return;
      
      final FileSystemItem activePath =
            FileSystemItem.createFile(activeEditor_.getPath());
      final FileSystemItem activeDir = activePath.getParentPath();
      
      server_.listFiles(
            activeDir,
            false,  // monitor result
            false,  // show hidden
            new ServerRequestCallback<DirectoryListing>()
            {
               @Override
               public void onResponseReceived(DirectoryListing listing)
               {
                  // read file listing (bail if there are no adjacent files)
                  JsArray<FileSystemItem> files = listing.getFiles();
                  int n = files.length();
                  if (n < 2)
                     return;
                  
                  // find the index of the currently open file
                  int index = -1;
                  for (int i = 0; i < n; i++)
                  {
                     FileSystemItem file = files.get(i);
                     if (file.equalTo(activePath))
                     {
                        index = i;
                        break;
                     }
                  }
                  
                  // if this failed for some reason, bail
                  if (index == -1)
                     return;
                  
                  // compute index of file to be opened (with wrap-around)
                  int target = (forward ? index + 1 : index - 1);
                  if (target < 0)
                     target = n - 1;
                  else if (target >= n)
                     target = 0;
                  
                  // extract the file and attempt to open
                  FileSystemItem targetItem = files.get(target);
                  columnManager_.openFile(targetItem);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   
   private void attemptSourceNavigation(final SourceNavigation navigation,
                                        final AppCommand retryCommand)
   {
      // see if we can navigate by id
      String docId = navigation.getDocumentId();
      final EditingTarget target = columnManager_.findEditor(docId);
      if (target != null)
      {
         // check for navigation to the current position -- in this
         // case execute the retry command
         if ( (target == activeEditor_) && 
               target.isAtSourceRow(navigation.getPosition()))
         {
            if (retryCommand.isEnabled())
               retryCommand.execute();
         }
         else
         {
            suspendSourceNavigationAdding_ = true;
            try
            {
               columnManager_.selectTab(target);
               target.restorePosition(navigation.getPosition());
            }
            finally
            {
               suspendSourceNavigationAdding_ = false;
            }
         }
      }
      
      // check for code browser navigation
      else if ((navigation.getPath() != null) &&
               navigation.getPath().startsWith(CodeBrowserEditingTarget.PATH))
      {
         columnManager_.activateCodeBrowser(
            navigation.getPath(),
            false,
            new SourceNavigationResultCallback<CodeBrowserEditingTarget>(
                                                      navigation.getPosition(),
                                                      retryCommand));
      }
      
      // check for file path navigation
      else if ((navigation.getPath() != null) && 
               !navigation.getPath().startsWith(DataItem.URI_PREFIX) &&
               !navigation.getPath().startsWith(ObjectExplorerHandle.URI_PREFIX))
      {
         FileSystemItem file = FileSystemItem.createFile(navigation.getPath());
         TextFileType fileType = fileTypeRegistry_.getTextTypeForFile(file);
         
         // open the file and restore the position
         columnManager_.openFile(file,
                                 fileType,
                                 new SourceNavigationResultCallback<EditingTarget>(
                                                                  navigation.getPosition(),
                                                                  retryCommand));
      } 
      else
      {
         // couldn't navigate to this item, retry
         if (retryCommand.isEnabled())
            retryCommand.execute();
      }
   }
   
   private void manageSourceNavigationCommands()
   {   
      commands_.sourceNavigateBack().setEnabled(
            sourceNavigationHistory_.isBackEnabled());

      commands_.sourceNavigateForward().setEnabled(
            sourceNavigationHistory_.isForwardEnabled());  
   }
   
    
   @Override
   public void onCodeBrowserNavigation(final CodeBrowserNavigationEvent event)
   {
      // if this isn't the main source window, don't handle server-dispatched
      // code browser events
      if (event.serverDispatched() && !SourceWindowManager.isMainSourceWindow())
      {
         return;
      }

      tryExternalCodeBrowser(event.getFunction(), event, new Command()
      {
         @Override
         public void execute()
         {
            if (event.getDebugPosition() != null)
            {
               columnManager_.startDebug();
            }
            
            columnManager_.activateCodeBrowser(
               CodeBrowserEditingTarget.getCodeBrowserPath(event.getFunction()),
               !event.serverDispatched(),
               new ResultCallback<CodeBrowserEditingTarget,ServerError>() {
               @Override
               public void onSuccess(CodeBrowserEditingTarget target)
               {
                  target.showFunction(event.getFunction());
                  if (event.getDebugPosition() != null)
                  {
                     highlightDebugBrowserPosition(target, 
                           event.getDebugPosition(), 
                           event.getExecuting());
                  }
               }
            });
         }
      });
   }
   
   @Override
   public void onCodeBrowserFinished(final CodeBrowserFinishedEvent event)
   {
      tryExternalCodeBrowser(event.getFunction(), event, new Command()
      {
         @Override
         public void execute()
         {
            final String path = CodeBrowserEditingTarget.getCodeBrowserPath(
                  event.getFunction());
            columnManager_.closeTabWithPath(path, false);
         }
      });
   }
   
   @Override
   public void onCodeBrowserHighlight(final CodeBrowserHighlightEvent event)
   {
      tryExternalCodeBrowser(event.getFunction(), event, new Command()
      {
         @Override
         public void execute()
         {
            columnManager_.startDebug();
            columnManager_.activateCodeBrowser(
               CodeBrowserEditingTarget.getCodeBrowserPath(event.getFunction()),
               false,
               new ResultCallback<CodeBrowserEditingTarget,ServerError>() {
               @Override
               public void onSuccess(CodeBrowserEditingTarget target)
               {
                  // if we just stole this code browser from another window,
                  // we may need to repopulate it
                  if (StringUtil.isNullOrEmpty(target.getContext()))
                     target.showFunction(event.getFunction());
                  highlightDebugBrowserPosition(target, event.getDebugPosition(), 
                        true);
               }
            });
         }
      });
   }
   
   private void tryExternalCodeBrowser(SearchPathFunctionDefinition func, 
         CrossWindowEvent<?> event, 
         Command withLocalCodeBrowser)
   {
      final String path = CodeBrowserEditingTarget.getCodeBrowserPath(func);
      NavigationResult result = pWindowManager_.get().navigateToCodeBrowser(
            path, event);
      if (result.getType() != NavigationResult.RESULT_NAVIGATED)
      {
         withLocalCodeBrowser.execute();
      }
   }

   private void highlightDebugBrowserPosition(CodeBrowserEditingTarget target,
                                              DebugFilePosition pos,
                                              boolean executing)
   {
      target.highlightDebugLocation(SourcePosition.create(
               pos.getLine(), 
               pos.getColumn() - 1),
            SourcePosition.create(
               pos.getEndLine(),
               pos.getEndColumn() + 1),
            executing);
   }

   private class SourceNavigationResultCallback<T extends EditingTarget> 
                        extends ResultCallback<T,ServerError>
   {
      public SourceNavigationResultCallback(SourcePosition restorePosition,
                                            AppCommand retryCommand)
      {
         suspendSourceNavigationAdding_ = true;
         restorePosition_ = restorePosition;
         retryCommand_ = retryCommand;
      }
      
      @Override
      public void onSuccess(final T target)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               try
               {
                  target.restorePosition(restorePosition_);
               }
               finally
               {
                  suspendSourceNavigationAdding_ = false;
               }
            }
         });
      }

      @Override
      public void onFailure(ServerError info)
      {
         suspendSourceNavigationAdding_ = false;
         if (retryCommand_.isEnabled())
            retryCommand_.execute();
      }
      
      @Override
      public void onCancelled()
      {
         suspendSourceNavigationAdding_ = false;
      }
      
      private final SourcePosition restorePosition_;
      private final AppCommand retryCommand_;
   }
   
   @Override
   public void onSnippetsChanged(SnippetsChangedEvent event)
   {
      SnippetHelper.onSnippetsChanged(event);
   }
   
   public EditingTarget getActiveEditor()
   {
      return activeEditor_;
   } 

   public SourceServerOperations getServer()
   {
      return server_;
   }

   public RemoteFileSystemContext getFileContext()
   {
      return fileContext_;
   }

   public boolean getInitialized()
   {
      return initialized_;
   }

   public void onOpenProfileEvent(OpenProfileEvent event)
   {
      onShowProfiler(event);
   }
   
   private void saveDocumentIds(JsArrayString ids, final CommandWithArg<Boolean> onSaveCompleted)
   {
      // we use a timer that fires the document save completed event,
      // just to ensure the server receives a response even if something
      // goes wrong during save or some client-side code throws. unfortunately
      // the current save code is wired up in such a way that it's difficult
      // to distinguish a success from failure, so we just make sure the
      // server receives a response after 5s (defaulting to failure)
      final Mutable<Boolean> savedSuccessfully = new Mutable<Boolean>(false);
      final Timer completedTimer = new Timer()
      {
         @Override
         public void run()
         {
            onSaveCompleted.execute(savedSuccessfully.get());
         }
      };
      completedTimer.schedule(5000);
      
      final Command onCompleted = new Command()
      {
         @Override
         public void execute()
         {
            savedSuccessfully.set(true);
            completedTimer.schedule(0);
         }
      };
      
      if (ids == null)
      {
         saveUnsavedDocuments(onCompleted);
      }
      else
      {
         final Set<String> idSet = new HashSet<String>();
         for (String id : JsUtil.asIterable(ids))
            idSet.add(id);
         
         saveUnsavedDocuments(idSet, onCompleted);
      }
   }
   
   @Override
   public void onRequestDocumentSave(RequestDocumentSaveEvent event)
   {
      saveDocumentIds(event.getDocumentIds(), success -> 
      {
         if (SourceWindowManager.isMainSourceWindow())
         {
            server_.requestDocumentSaveCompleted(success,
                  new VoidServerRequestCallback());
         }
      });
   }
   
   @Override
   public void onRequestDocumentClose(RequestDocumentCloseEvent event)
   {
      JsArrayString ids = event.getDocumentIds();
      Command closeEditors = () ->
      {
         // Close each of the requested tabs
         columnManager_.closeTabs(ids);
         
         // Let the server know we've completed the task
         if (SourceWindowManager.isMainSourceWindow())
         {
            server_.requestDocumentCloseCompleted(true,
                  new VoidServerRequestCallback());
         }
      };

      if (event.getSave())
      {
         // Saving, so save unsaved documents before closing the tab(s).
         saveDocumentIds(ids, success ->
         {
            if (success)
            {
               // All unsaved changes saved; OK to close
               closeEditors.execute();
            }
            else
            {
               // We didn't save (or the user cancelled), so let the server know
               if (SourceWindowManager.isMainSourceWindow())
               {
                  server_.requestDocumentCloseCompleted(false,
                        new VoidServerRequestCallback());
               }
            }
         });
      }
      else
      {
         // If not saving, just close the windows immediately
         closeEditors.execute();
      }
   }
   
   private void dispatchEditorEvent(final String id,
                                    final CommandWithArg<DocDisplay> command)
   {
      InputEditorDisplay console = consoleEditorProvider_.getConsoleEditor();

      boolean isConsoleEvent = false;
      if (console != null)
      {
         isConsoleEvent =
               (StringUtil.isNullOrEmpty(id) && console.isFocused()) ||
               "#console".equals(id);
      }
      if (isConsoleEvent)
      {
         command.execute((DocDisplay) console);
      }
      else
      {
         withTarget(id, new CommandWithArg<TextEditingTarget>()
         {
            @Override
            public void execute(TextEditingTarget target)
            {
               command.execute(target.getDocDisplay());
            }
         });
      }
      
   }
   
   @Override
   public void onSetSelectionRanges(final SetSelectionRangesEvent event)
   {
      dispatchEditorEvent(event.getData().getId(), new CommandWithArg<DocDisplay>()
      {
         @Override
         public void execute(DocDisplay docDisplay)
         {
            JsArray<Range> ranges = event.getData().getRanges();
            if (ranges.length() == 0)
               return;
            
            AceEditor editor = (AceEditor) docDisplay;
            editor.setSelectionRanges(ranges);
         }
      });
   }
   
   @Override
   public void onGetEditorContext(GetEditorContextEvent event)
   {
      GetEditorContextEvent.Data data = event.getData();
      int type = data.getType();

      if (type == GetEditorContextEvent.TYPE_ACTIVE_EDITOR)
      {
         if (consoleEditorHadFocusLast() || activeEditor_ == null)
            type = GetEditorContextEvent.TYPE_CONSOLE_EDITOR;
         else
            type = GetEditorContextEvent.TYPE_SOURCE_EDITOR;
      }

      if (type == GetEditorContextEvent.TYPE_CONSOLE_EDITOR)
      {
         InputEditorDisplay editor = consoleEditorProvider_.getConsoleEditor();
         if (editor != null && editor instanceof DocDisplay)
         {
            getEditorContext("#console", "", (DocDisplay) editor);
            return;
         }
      }
      else if (type == GetEditorContextEvent.TYPE_SOURCE_EDITOR)
      {
         EditingTarget target = activeEditor_;
         if (target != null && target instanceof TextEditingTarget)
         {
            TextEditingTarget editingTarget = (TextEditingTarget)target;
            editingTarget.ensureTextEditorActive(() -> {
               getEditorContext(
                  editingTarget.getId(),
                  editingTarget.getPath(),
                  editingTarget.getDocDisplay()
               );
            });
            return;
         }
      }

      // We need to ensure a 'getEditorContext' event is always
      // returned as we have a 'wait-for' event on the server side
      server_.getEditorContextCompleted(
            GetEditorContextEvent.SelectionData.create(),
            new VoidServerRequestCallback());
   }
   
   @Override
   public void onReplaceRanges(final ReplaceRangesEvent event)
   {
      dispatchEditorEvent(event.getData().getId(), new CommandWithArg<DocDisplay>()
      {
         @Override
         public void execute(DocDisplay docDisplay)
         {
            doReplaceRanges(event, docDisplay);
         }
      });
   }
   
   private void pasteFileContentsAtCursor(final String path, final String encoding)
   {
      if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
      {
         final TextEditingTarget target = (TextEditingTarget) activeEditor_;
         server_.getFileContents(path, encoding, new ServerRequestCallback<String>()
         {
            @Override
            public void onResponseReceived(String content)
            {
               target.insertCode(content, false);
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }
   }

   private void pasteRCodeExecutionResult(final String code)
   {
      server_.executeRCode(code, new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String output)
         {
            if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
            {
               TextEditingTarget editor = (TextEditingTarget) activeEditor_;
               editor.insertCode(output, false);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   private void reflowText()
   {
      if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
      {
         TextEditingTarget editor = (TextEditingTarget) activeEditor_;
         editor.reflowText();
      }
   }

   private void reindent()
   {
      if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
      {
         TextEditingTarget editor = (TextEditingTarget) activeEditor_;
         editor.getDocDisplay().reindent();
      }
   }

   // !!! fix this
   private void saveActiveSourceDoc()
   {
      if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
      {
         TextEditingTarget target = (TextEditingTarget) activeEditor_;
         target.save();
      }
   }

   // !!! fix this
   private void saveAndCloseActiveSourceDoc()
   {
      if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
      {
         TextEditingTarget target = (TextEditingTarget) activeEditor_;
         target.save(new Command()
         {
            @Override
            public void execute()
            {
               onCloseSourceDoc();
            }
         });
      }
   }

   // !!! fix this
   private void revertActiveDocument()
   {
      if (activeEditor_ == null)
         return;

      if (activeEditor_.getPath() != null)
         activeEditor_.revertChanges(null);

      // Ensure that the document is in view
      activeEditor_.ensureCursorVisible();
   }

   // !!! fix this
   private void editFile(final String path)
   {
      /*
      server_.ensureFileExists(
            path,
            new ServerRequestCallback<Boolean>()
            {
               @Override
               public void onResponseReceived(Boolean success)
               {
                  if (success)
                  {
                     FileSystemItem file = FileSystemItem.createFile(path);
                     openFile(file);
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
            */
   }

   private void showHelpAtCursor()
   {
      if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
      {
         TextEditingTarget editor = (TextEditingTarget) activeEditor_;
         editor.showHelpAtCursor();
      }
   }

   private void doReplaceRanges(ReplaceRangesEvent event, DocDisplay docDisplay)
   {
      JsArray<ReplacementData> data = event.getData().getReplacementData();
      
      int n = data.length();
      for (int i = 0; i < n; i++)
      {
         ReplacementData el = data.get(n - i - 1);
         Range range = el.getRange();
         String text = el.getText();
         
         // A null range at this point is a proxy to use the current selection
         if (range == null)
            range = docDisplay.getSelectionRange();
         
         docDisplay.replaceRange(range, text);
      }
      docDisplay.focus();
   }
   
   private class StatFileEntry
   {
      public StatFileEntry(FileSystemItem fileIn, 
            CommandWithArg<FileSystemItem> actionIn)
      {
         file = fileIn;
         action = actionIn;
      }
      public final FileSystemItem file;
      public final CommandWithArg<FileSystemItem> action;
   }
   
   final Queue<StatFileEntry> statQueue_ = new LinkedList<StatFileEntry>();
   ArrayList<Integer> tabOrder_ = new ArrayList<Integer>();
   SourceColumnManager columnManager_;
   private EditingTarget activeEditor_;
   private final Commands commands_;
   private final SourceServerOperations server_;
   private final EditingTargetSource editingTargetSource_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final GlobalDisplay globalDisplay_;
   private final WorkbenchContext workbenchContext_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileContext_;
   private final EventBus events_;
   private final AriaLiveService ariaLive_;
   private final Session session_;
   private final Synctex synctex_;
   private UserPrefs userPrefs_;
   private final UserState userState_;
   private final ConsoleEditorProvider consoleEditorProvider_;
   private final RnwWeaveRegistry rnwWeaveRegistry_;
   private HashSet<AppCommand> activeCommands_ = new HashSet<AppCommand>();
   private HashSet<AppCommand> dynamicCommands_;
   private final SourceNavigationHistory sourceNavigationHistory_ = 
                                              new SourceNavigationHistory(30);
   private SourceVimCommands vimCommands_;

   private boolean suspendSourceNavigationAdding_;
  
   private static final String MODULE_SOURCE = "source-pane";
   private static final String KEY_ACTIVETAB = "activeTab";
   private boolean initialized_;
   
   private final Provider<SourceWindowManager> pWindowManager_;
   
   private DependencyManager dependencyManager_;
 
   public final static int TYPE_FILE_BACKED = 0;
   public final static int TYPE_UNTITLED    = 1;
   public final static int OPEN_INTERACTIVE = 0;
   public final static int OPEN_REPLAY      = 1;
   public final static String COLUMN_PREFIX = "Source";
}
