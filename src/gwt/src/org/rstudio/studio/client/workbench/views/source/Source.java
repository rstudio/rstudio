/*
 * Source.java
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
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
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.DebugFilePosition;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.BeforeShowEvent;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasEnsureHeightHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.events.HasTabCloseHandlers;
import org.rstudio.core.client.events.HasTabClosedHandlers;
import org.rstudio.core.client.events.HasTabClosingHandlers;
import org.rstudio.core.client.events.HasTabReorderHandlers;
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
import org.rstudio.studio.client.application.events.MouseNavigateSourceHistoryEvent;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenPresentationSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenPresentationSourceFileHandler;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileHandler;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.common.rnw.RnwWeaveRegistry;
import org.rstudio.studio.client.events.GetEditorContextEvent;
import org.rstudio.studio.client.events.RStudioApiRequestEvent;
import org.rstudio.studio.client.events.ReplaceRangesEvent;
import org.rstudio.studio.client.events.ReplaceRangesEvent.ReplacementData;
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
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
import org.rstudio.studio.client.workbench.model.UnsavedChangesItem;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.snippets.model.SnippetsChangedEvent;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.events.SuppressNextShellFocusEvent;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.source.NewShinyWebApplication.Result;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager.NavigationResult;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
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
import org.rstudio.studio.client.workbench.views.source.editors.text.events.NewWorkingCopyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.NewRdDialog;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserFinishedHandler;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserHighlightEvent;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationHandler;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditEndedEvent;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragInitiatedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragStartedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocWindowChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.EditPresentationSourceEvent;
import org.rstudio.studio.client.workbench.views.source.events.ScrollToPositionEvent;
import org.rstudio.studio.client.workbench.views.source.events.XRefNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.events.EnsureVisibleSourceWindowEvent;
import org.rstudio.studio.client.workbench.views.source.events.FileEditEvent;
import org.rstudio.studio.client.workbench.views.source.events.FileEditHandler;
import org.rstudio.studio.client.workbench.views.source.events.InsertSourceEvent;
import org.rstudio.studio.client.workbench.views.source.events.InsertSourceHandler;
import org.rstudio.studio.client.workbench.views.source.events.MaximizeSourceWindowEvent;
import org.rstudio.studio.client.workbench.views.source.events.NewDocumentWithCodeEvent;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocInitiatedEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowContentEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowContentHandler;
import org.rstudio.studio.client.workbench.views.source.events.ShowDataEvent;
import org.rstudio.studio.client.workbench.views.source.events.ShowDataHandler;
import org.rstudio.studio.client.workbench.views.source.events.SourceFileSavedEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationHandler;
import org.rstudio.studio.client.workbench.views.source.events.SourcePathChangedEvent;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;
import org.rstudio.studio.client.workbench.views.source.model.RdShellResult;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.Set;

@Singleton
public class Source implements InsertSourceHandler,
                               IsWidget,
                               OpenSourceFileHandler,
                               OpenPresentationSourceFileHandler,
                               CommandPaletteEntrySource,
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
                               OpenProfileEvent.Handler,
                               OpenObjectExplorerEvent.Handler,
                               ReplaceRangesEvent.Handler,
                               SetSelectionRangesEvent.Handler,
                               GetEditorContextEvent.Handler,
                               RequestDocumentSaveEvent.Handler,
                               RequestDocumentCloseEvent.Handler,
                               ScrollToPositionEvent.Handler,
                               EditPresentationSourceEvent.Handler,
                               XRefNavigationEvent.Handler,
                               NewDocumentWithCodeEvent.Handler,
                               MouseNavigateSourceHistoryEvent.Handler,
                               RStudioApiRequestEvent.Handler
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
                                    HasSelectionHandlers<Integer>,
                                    HasEnsureVisibleHandlers,
                                    HasEnsureHeightHandlers
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
      void resetDocTabs(String activeId,
                         String[] ids,
                         FileIcon[] icons,
                         String[] names,
                         String[] paths);

      void setDirty(Widget widget, boolean dirty);

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

      void ensureVisible();
      HandlerRegistration addBeforeShowHandler(BeforeShowEvent.Handler handler);
      HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler);
   }

   @Inject
   public Source(Commands commands,
                 Binder binder,
                 SourceColumnManager sourceColumnManager,
                 SourceServerOperations server,
                 FileTypeRegistry fileTypeRegistry,
                 GlobalDisplay globalDisplay,
                 FileDialogs fileDialogs,
                 RemoteFileSystemContext fileContext,
                 EventBus events,
                 AriaLiveService ariaLive,
                 final Session session,
                 WorkbenchContext workbenchContext,
                 ConsoleEditorProvider consoleEditorProvider,
                 RnwWeaveRegistry rnwWeaveRegistry,
                 DependencyManager dependencyManager,
                 Provider<SourceWindowManager> pWindowManager)
   {
      commands_ = commands;
      binder.bind(commands, this);
      columnManager_ = sourceColumnManager;
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
      globalDisplay_ = globalDisplay;
      fileDialogs_ = fileDialogs;
      fileContext_ = fileContext;
      events_ = events;
      ariaLive_ = ariaLive;
      session_ = session;
      workbenchContext_ = workbenchContext;
      consoleEditorProvider_ = consoleEditorProvider;
      rnwWeaveRegistry_ = rnwWeaveRegistry;
      dependencyManager_ = dependencyManager;
      pWindowManager_ = pWindowManager;

      commands_.newSourceDoc().setEnabled(true);

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
      events_.addHandler(ScrollToPositionEvent.TYPE, this);
      events_.addHandler(NewDocumentWithCodeEvent.TYPE, this);
      events_.addHandler(XRefNavigationEvent.TYPE, this);
      if (Desktop.hasDesktopFrame())
         events_.addHandler(MouseNavigateSourceHistoryEvent.TYPE, this);

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
               columnManager_.getSourceNavigationHistory().add(event.getNavigation());
            }
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
      events_.addHandler(ReplaceRangesEvent.TYPE, this);
      events_.addHandler(GetEditorContextEvent.TYPE, this);
      events_.addHandler(SetSelectionRangesEvent.TYPE, this);
      events_.addHandler(OpenProfileEvent.TYPE, this);
      events_.addHandler(RequestDocumentSaveEvent.TYPE, this);
      events_.addHandler(RequestDocumentCloseEvent.TYPE, this);
      events_.addHandler(RStudioApiRequestEvent.TYPE, this);
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
            columnManager_.clearSourceNavigationHistory();
         }

         @Override
         protected Integer getValue()
         {
            return columnManager_.getPhysicalTabIndex();
         }
      };

      AceEditorNative.syncUiPrefs(userPrefs_);
      // As tabs were added before, manageCommands() was suppressed due to
      // initialized_ being false, so we need to run it explicitly
      columnManager_.manageCommands(false);
      // Same with this event
      columnManager_.fireDocTabsChanged();

      // open project or edit_published docs (only for main source window)
      if (SourceWindowManager.isMainSourceWindow())
      {
         columnManager_.openProjectDocs(session_, true);
         openEditPublishedDocs();
      }

      // add vim commands
      columnManager_.initVimCommands();
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

      //  handle mouse button navigations
      if (!Desktop.hasDesktopFrame())
         handleMouseButtonNavigations();

      // on macOS, we need to aggressively re-sync commands when a new
      // window is selected (since the main menu applies to both main
      // window and satellites)
      if (BrowseCap.isMacintoshDesktop())
      {
         WindowEx.addFocusHandler(
             (FocusEvent event) -> columnManager_.manageCommands(true));
      }
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

   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      return columnManager_.getCommandPaletteItems();
   }

   private boolean consoleEditorHadFocusLast()
   {
      String id = MainWindowObject.lastFocusedEditorId().get();
      return "rstudio_console_input".equals(id);
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
                                                       columnManager_.getByName(name));
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
      columnManager_.setDocsRestored();
      columnManager_.beforeShow(true);
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
      String profilePath = event.getFilePath();
      String htmlPath = event.getHtmlPath();
      String htmlLocalPath = event.getHtmlLocalPath();

      // first try to activate existing
      EditingTarget target = columnManager_.findEditorByPath(profilePath);
      if (target != null)
      {
         columnManager_.selectTab(target);
         return;
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
                     FileTypeRegistry.RMARKDOWN,
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
   public void onActivateSource()
   {
      onActivateSource(SourceColumnManager.MAIN_SOURCE_NAME, null);
   }

   public void onActivateSource(String columnName, final Command afterActivation)
   {
      // give the window manager a chance to activate the last source pane
      if (pWindowManager_.get().activateLastFocusedSource())
         return;
      columnManager_.activateColumn(columnName, afterActivation);
   }

   @Handler
   public void onLayoutZoomSource()
   {
      onActivateSource(columnManager_.getActive().getName(),
         new Command()
         {
            @Override
            public void execute()
            {
               events_.fireEvent(new ZoomPaneEvent(columnManager_.getActive().getName(),
                  "SourceColumn"));
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
   public void onDocWindowChanged(final DocWindowChangedEvent e)
   {
      // Determine the old column before making any changes
      final SourceColumn oldDisplay = columnManager_.findByDocument(e.getDocId());

      if (e.getNewWindowId().equals(SourceWindowManager.getSourceWindowId()))
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

         // If we can not determine the new column and the window has more than one column,
         // log a warning but continue to prevent the tab from being lost.
         final SourceColumn newDisplay = columnManager_.findByPosition(e.getXPos());
         if (newDisplay == null &&
             e.getNewWindowId().equals(e.getOldWindowId()) &&
             columnManager_.getSize() > 1)
            Debug.logWarning("Couldn't determine new window column for dragged document.");

         // the event doesn't contain the display info, so we add it now
         pWindowManager_.get().assignSourceDocDisplay(
             e.getDocId(),
             (newDisplay == null) ?
                 columnManager_.getActive().getName() :
                 (newDisplay).getName(),
             true);

         // if we're the adopting window, add the doc
         server_.getSourceDocument(e.getDocId(),
             new ServerRequestCallback<SourceDocument>()
             {
                @Override
                public void onResponseReceived(final SourceDocument doc)
                {
                   final EditingTarget target = columnManager_.addTab(doc, e.getPos(),
                       newDisplay);

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
      if (e.getOldWindowId().equals(SourceWindowManager.getSourceWindowId()))
      {
         columnManager_.disownDocOnDrag(e.getDocId(), oldDisplay);
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
   public void onSaveAllSourceDocs()
   {
      // Save all documents in the main window
       columnManager_.saveAllSourceDocs();

      // Save all documents in satellite windows
      pWindowManager_.get().saveUnsavedDocuments(null, null);
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
               columnManager_.closeAllLocalSourceDocs(caption, null, onCompleted, excludeActive);
            }
         });
      }
      else
      {
         // this is a satellite (or we don't need to query satellites)--just
         // close our own tabs
         columnManager_.closeAllLocalSourceDocs(caption, null, onCompleted, excludeActive);
      }
   }

   public ArrayList<UnsavedChangesTarget> getUnsavedChanges(int type)
   {
      return getUnsavedChanges(type,  null);
   }

   public ArrayList<UnsavedChangesTarget> getUnsavedChanges(int type, Set<String> ids)
   {
      ArrayList<UnsavedChangesTarget> targets =
          new ArrayList<UnsavedChangesTarget>();

      // if this is the main window, collect all unsaved changes from
      // the satellite windows as well
      if (SourceWindowManager.isMainSourceWindow())
      {
         targets.addAll(pWindowManager_.get().getAllSatelliteUnsavedChanges(type));
      }
      targets.addAll(columnManager_.getUnsavedChanges(type, ids));

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
            columnManager_.saveChanges(getUnsavedChanges(TYPE_FILE_BACKED, ids), onCompleted);
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
            columnManager_.revertUnsavedTargets(onCompleted);
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
               columnManager_.saveChanges(saveTargets, completed);
            }
         });
      }
      else
      {
         columnManager_.saveChanges(saveTargets, completed);
      }
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

   public void onScrollToPosition(final ScrollToPositionEvent event)
   {
      if (!isLastFocusedSourceWindow())
         return;
      FilePosition pos = FilePosition.create(event.getLine(),
         event.getColumn());
      columnManager_.scrollToPosition(pos, event.getMoveCursor());
   }

   public void onNewDocumentWithCode(final NewDocumentWithCodeEvent event)
   {
      // The document should only be opened in the last focused window, unless this window is a
      // satellite that has already been closed. When this is the case, open the new doc in the
      // main source window.
      if (!isLastFocusedSourceWindow())
         return;

      // determine the type
      final EditableFileType docType;
      if (event.getType() == NewDocumentWithCodeEvent.R_SCRIPT)
         docType = FileTypeRegistry.R;
      else if (event.getType() == NewDocumentWithCodeEvent.SQL)
         docType = FileTypeRegistry.SQL;
      else
         docType = FileTypeRegistry.RMARKDOWN;

      final ResultCallback<EditingTarget, ServerError> callback = event.getCallback();
      
      // command to create and run the new doc
      Command newDocCommand = new Command()
      {
         @Override
         public void execute()
         {
            columnManager_.newDoc(docType, event.getCode(), new ResultCallback<EditingTarget, ServerError>()
            {
               @Override
               public void onSuccess(EditingTarget arg)
               {
                  TextEditingTarget editingTarget = (TextEditingTarget)arg;

                  SourcePosition position = event.getCursorPosition();
                  if (position != null &&
                      position.getRow() > 0 ||
                      position.getColumn() > 0)
                  {
                     editingTarget.navigateToPosition(position, false);
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
                  
                  if (callback != null)
                     callback.onSuccess(arg);
               }
               
               @Override
               public void onFailure(ServerError info)
               {
                  super.onFailure(info);
                  if (callback != null)
                     callback.onFailure(info);
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

   @Override
   public void onMouseNavigateSourceHistory(MouseNavigateSourceHistoryEvent event)
   {
      if (isPointInSourcePane(event.getMouseX(), event.getMouseY()))
      {
         if (event.getForward())
            onSourceNavigateForward();
         else
            onSourceNavigateBack();
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
      doOpenSourceFile(
            event.getFile(),
            event.getFileType(),
            event.getPosition(),
            null,
            event.getNavigationMethod(),
            false,
            event.getMoveCursor());
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
                       true,
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

   @Override
   public void onXRefNavigation(XRefNavigationEvent event)
   {
      TextFileType fileType = fileTypeRegistry_.getTextTypeForFile(event.getSourceFile());

      columnManager_.openFile(
            event.getSourceFile(),
            fileType,
            (final EditingTarget editor) ->
            {
               // NOTE: we defer execution here as otherwise we might attempt navigation
               // before the underlying Ace editor has been fully initialized
               Scheduler.get().scheduleDeferred(() ->
               {
                  TextEditingTarget target = (TextEditingTarget) editor;
                  target.navigateToXRef(event.getXRef(), event.getForceVisualMode());
               });
            });

   }

   public void forceLoad()
   {
      AceEditor.preload();
   }

   public String getCurrentDocId()
   {
      return columnManager_.getActiveDocId();
   }

   public String getCurrentDocPath()
   {
      return columnManager_.getActiveDocPath();
   }

   private void doOpenSourceFile(final FileSystemItem file,
                                 final TextFileType fileType,
                                 final FilePosition position,
                                 final String pattern,
                                 final int navMethod,
                                 final boolean forceHighlightMode,
                                 final boolean moveCursor)
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
            // helper command to focus after navigation has completed
            final Command onNavigationCompleted = () ->
            {
               if (file.focusOnNavigate())
               {
                  Scheduler.get().scheduleDeferred(() ->
                  {
                     target.focus();
                  });
               }
            };


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
                  {
                      Desktop.getFrame().bringMainFrameToFront();
                  }
               }

               SourcePosition startPosition = SourcePosition.create(
                     position.getLine() - 1,
                     position.getColumn() - 1);

               navigate(target,
                        startPosition,
                        endPosition,
                        onNavigationCompleted);
            }
            else if (pattern != null)
            {
               Position pos = target.search(pattern);
               if (pos != null)
               {
                  navigate(target,
                           SourcePosition.create(pos.getRow(), 0),
                           null,
                           onNavigationCompleted);
               }
            }
            else
            {
               onNavigationCompleted.execute();
            }
         }

         private void navigate(final EditingTarget target,
                               final SourcePosition srcPosition,
                               final SourcePosition srcEndPosition,
                               final Command onNavigationCompleted)
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

                     target.navigateToPosition(
                           srcPosition,
                           false,
                           highlight,
                           moveCursor,
                           onNavigationCompleted);
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
            columnManager_.setOpeningForSourceNavigation(position != null || pattern != null);

            columnManager_.openFile(file,
                     fileType,
                     (target) -> {
                        columnManager_.setOpeningForSourceNavigation(false);
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

   public void onInsertSource(final InsertSourceEvent event)
   {

      if (!columnManager_.insertSource(event.getCode(), event.isBlock()))
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
      if (!columnManager_.getSourceNavigationHistory().isForwardEnabled())
         columnManager_.recordCurrentNavigationHistoryPosition();

      SourceNavigation navigation = columnManager_.getSourceNavigationHistory().goBack();
      if (navigation != null)
         attemptSourceNavigation(navigation, commands_.sourceNavigateBack());
   }

   @Handler
   public void onSourceNavigateForward()
   {
      SourceNavigation navigation = columnManager_.getSourceNavigationHistory().goForward();
      if (navigation != null)
         attemptSourceNavigation(navigation, commands_.sourceNavigateForward());
   }

   // handle mouse forward and back buttons if the mouse is within a source pane
   private native final void handleMouseButtonNavigations() /*-{
   try {
      if ($wnd.addEventListener) {
         var self = this;
         function handler(nav) {
            return $entry(function(evt) {
               if ((evt.button === 3 || evt.button === 4) &&
                   self.@org.rstudio.studio.client.workbench.views.source.Source::isMouseEventInSourcePane(Lcom/google/gwt/dom/client/NativeEvent;)(evt)) {

                  // perform navigation
                  if (nav) {
                     if (evt.button === 3) {
                        self.@org.rstudio.studio.client.workbench.views.source.Source::onSourceNavigateBack()();
                     } else if (evt.button === 4) {
                        self.@org.rstudio.studio.client.workbench.views.source.Source::onSourceNavigateForward()();
                     }
                  }

                  // prevent other handling
                  evt.preventDefault();
                  evt.stopPropagation();
                  evt.stopImmediatePropagation()
                  return false;
               }
            });
         }

         // mask mousedown from ace to prevent selection, mask mouseup from chrome
         // to prevent navigation of the entire browser
         $wnd.addEventListener('mousedown', handler(false), false);
         $wnd.addEventListener('mouseup', handler(true), false);
      }
   } catch(err) {
      console.log(err);
   }
   }-*/;

   private boolean isPointInSourcePane(int x, int y)
   {
      ArrayList<Widget> sourceWidgets = columnManager_.getWidgets(false);
      for (Widget sourceWidget : sourceWidgets)
      {
         Element sourceEl = sourceWidget.getElement();
         boolean inPane = x > sourceEl.getAbsoluteLeft() &&
                          x < sourceEl.getAbsoluteRight() &&
                          y > sourceEl.getAbsoluteTop() &&
                          y < sourceEl.getAbsoluteBottom();
         if (inPane)
            return true;
      }
      return false;
   }

   private boolean isLastFocusedSourceWindow()
   {
      String lastFocusedWindow = pWindowManager_.get().getLastFocusedSourceWindowId();
      if (!SourceWindowManager.getSourceWindowId().equals(lastFocusedWindow) &&
         (!SourceWindowManager.isMainSourceWindow() ||
            pWindowManager_.get().isSourceWindowOpen(lastFocusedWindow)))
         return false;
      return true;
   }

   private boolean isMouseEventInSourcePane(NativeEvent event)
   {
      return isPointInSourcePane(event.getClientX(), event.getClientY());
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
      ariaLive_.announce(AriaLiveService.ON_DEMAND,
          columnManager_.getEditorPositionString(),
          Timing.IMMEDIATE,
          Severity.STATUS);
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
      if (!columnManager_.hasActiveEditor() ||
          StringUtil.isNullOrEmpty(columnManager_.getActiveDocPath()))
         return;

      final FileSystemItem activePath =
            FileSystemItem.createFile(columnManager_.getActiveDocPath());
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
         if (columnManager_.isActiveEditor(target) &&
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
         columnManager_.withTarget(id, new CommandWithArg<TextEditingTarget>()
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
         if (consoleEditorHadFocusLast() || !columnManager_.hasActiveEditor())
            type = GetEditorContextEvent.TYPE_CONSOLE_EDITOR;
         else
            type = GetEditorContextEvent.TYPE_SOURCE_EDITOR;
      }

      if (type == GetEditorContextEvent.TYPE_CONSOLE_EDITOR)
      {
         InputEditorDisplay editor = consoleEditorProvider_.getConsoleEditor();
         if (editor != null && editor instanceof DocDisplay)
         {
            SourceColumnManager.getEditorContext("#console", "", (DocDisplay) editor, server_);
            return;
         }
      }
      else if (type == GetEditorContextEvent.TYPE_SOURCE_EDITOR)
      {
         if (columnManager_.requestActiveEditorContext())
            return;
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
   
   private boolean apiEventTargetIsConsole(String id)
   {
      return
            StringUtil.equals(id, "#console") ||
            StringUtil.isNullOrEmpty(id) &&
            consoleEditorHadFocusLast();
   }
   
   private void invokeEditorApiAction(String docId,
                                      CommandWithArg<TextEditingTarget> callback)
   {
      columnManager_.withTarget(docId, callback, () ->
      {
         server_.rstudioApiResponse(
               JavaScriptObject.createObject(),
               new VoidServerRequestCallback());
      });
   }
   
   @Override
   public void onRStudioApiRequest(RStudioApiRequestEvent requestEvent)
   {
      try
      {
         onRStudioApiRequestImpl(requestEvent);
      }
      catch (Exception e)
      {
         Debug.logException(e);
         
         // ensure that a response if made if something goes wrong
         if (requestEvent.getData().isSynchronous())
         {
            server_.rstudioApiResponse(
                  JavaScriptObject.createObject(),
                  new VoidServerRequestCallback());
         }
      }
   }
   
   private void onRStudioApiRequestImpl(RStudioApiRequestEvent requestEvent)
   {
      // retrieve request data
      RStudioApiRequestEvent.Data requestData = requestEvent.getData();
      
      // if this event is only for the active source window,
      // then ignore if if we're not the active window
      boolean ignore =
            requestData.getTarget() == RStudioApiRequestEvent.TARGET_ACTIVE_WINDOW &&
            !isLastFocusedSourceWindow();
      
      if (ignore)
         return;
      
      int type = requestData.getType();
      if (type == RStudioApiRequestEvent.TYPE_GET_EDITOR_SELECTION)
      {
         RStudioApiRequestEvent.GetEditorSelectionData data = requestEvent.getPayload().cast();
         
         if (apiEventTargetIsConsole(data.getDocId()))
         {
            String selection = consoleEditorProvider_.getConsoleEditor().getSelectionValue();
            JsObject response = JsObject.createJsObject();
            response.setString("value", selection);
            server_.rstudioApiResponse(response, new VoidServerRequestCallback());
         }
         else
         {
            invokeEditorApiAction(data.getDocId(), (TextEditingTarget target) ->
            {
               target.withEditorSelection((String selection) ->
               {
                  JsObject response = JsObject.createJsObject();
                  response.setString("value", selection);
                  server_.rstudioApiResponse(response, new VoidServerRequestCallback());
               });
            });
         }
      }
      else if (type == RStudioApiRequestEvent.TYPE_SET_EDITOR_SELECTION)
      {
         RStudioApiRequestEvent.SetEditorSelectionData data = requestEvent.getPayload().cast();
         
         if (apiEventTargetIsConsole(data.getDocId()))
         {
            InputEditorDisplay console = consoleEditorProvider_.getConsoleEditor();
            console.replaceSelection(data.getValue(), true);
            
            server_.rstudioApiResponse(
                  JavaScriptObject.createObject(),
                  new VoidServerRequestCallback());
         }
         else
         {
            invokeEditorApiAction(data.getDocId(), (TextEditingTarget target) ->
            {
               target.replaceSelection(data.getValue(), () ->
               {
                  server_.rstudioApiResponse(
                        JavaScriptObject.createObject(),
                        new VoidServerRequestCallback());
               });
            });
         }
      }
      else if (type == RStudioApiRequestEvent.TYPE_DOCUMENT_ID)
      {
         RStudioApiRequestEvent.DocumentIdData data = requestEvent.getPayload().cast();
         
         if (data.getAllowConsole() && consoleEditorHadFocusLast())
         {
            JsObject response = JsObject.createJsObject();
            response.setString("id", "#console");
            server_.rstudioApiResponse(response, new VoidServerRequestCallback());
         }
         else
         {
            invokeEditorApiAction(null, (TextEditingTarget target) ->
            {
               JsObject response = JsObject.createJsObject();
               response.setString("id", target.getId());
               server_.rstudioApiResponse(response, new VoidServerRequestCallback());
            });
         }
      }
      else if (type == RStudioApiRequestEvent.TYPE_DOCUMENT_OPEN)
      {
         RStudioApiRequestEvent.DocumentOpenData data = requestEvent.getPayload().cast();
         final String path = data.getPath();
         columnManager_.editFile(path, new ResultCallback<EditingTarget, ServerError>()
         {
            @Override
            public void onSuccess(final EditingTarget result)
            {
               // focus opened document (note that we may need to suppress
               // attempts by the shell widget to steal focus here)
               events_.fireEvent(new SuppressNextShellFocusEvent());
               result.focus();
               
               JsObject response = JsObject.createJsObject();
               response.setString("id", result.getId());
               server_.rstudioApiResponse(response, new VoidServerRequestCallback());
            }

            @Override
            public void onFailure(ServerError info)
            {
               super.onFailure(info);
               server_.rstudioApiResponse(
                     JavaScriptObject.createObject(),
                     new VoidServerRequestCallback());
            }
         });
         
      }
      else if (type == RStudioApiRequestEvent.TYPE_DOCUMENT_NEW)
      {
         RStudioApiRequestEvent.DocumentNewData data = requestEvent.getPayload().cast();
         
         SourcePosition position = SourcePosition.create(
               data.getRow(),
               data.getColumn());
         
         events_.fireEvent(new NewDocumentWithCodeEvent(
               data.getType(),
               data.getCode(),
               position,
               data.getExecute(),
               new ResultCallback<EditingTarget, ServerError>()
               {
                  @Override
                  public void onSuccess(final EditingTarget result)
                  {
                     // focus opened document (note that we may need to suppress
                     // attempts by the shell widget to steal focus here)
                     events_.fireEvent(new SuppressNextShellFocusEvent());
                     result.focus();

                     JsObject response = JsObject.createJsObject();
                     response.setString("id", result.getId());
                     server_.rstudioApiResponse(response, new VoidServerRequestCallback());
                  }

                  @Override
                  public void onFailure(ServerError info)
                  {
                     super.onFailure(info);
                     server_.rstudioApiResponse(
                           JavaScriptObject.createObject(),
                           new VoidServerRequestCallback());
                  }
               }));
      }
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

   private final Commands commands_;
   final Queue<StatFileEntry> statQueue_ = new LinkedList<StatFileEntry>();
   SourceColumnManager columnManager_;
   private final SourceServerOperations server_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final GlobalDisplay globalDisplay_;
   private final WorkbenchContext workbenchContext_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileContext_;
   private final EventBus events_;
   private final AriaLiveService ariaLive_;
   private final Session session_;
   private UserPrefs userPrefs_;
   private final ConsoleEditorProvider consoleEditorProvider_;
   private final RnwWeaveRegistry rnwWeaveRegistry_;

   private boolean suspendSourceNavigationAdding_;

   private static final String MODULE_SOURCE = "source-pane";
   private static final String KEY_ACTIVETAB = "activeTab";
   private boolean initialized_;

   private final Provider<SourceWindowManager> pWindowManager_;

   private final DependencyManager dependencyManager_;

   public final static int TYPE_FILE_BACKED = 0;
   public final static int TYPE_UNTITLED    = 1;
   public final static int OPEN_INTERACTIVE = 0;
   public final static int OPEN_REPLAY      = 1;
}
