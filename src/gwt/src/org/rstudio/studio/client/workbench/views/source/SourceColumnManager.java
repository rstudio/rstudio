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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
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
import org.rstudio.core.client.theme.DocTabSelectionEvent;
import org.rstudio.core.client.widget.Operation;
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
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.ObjectExplorerFileType;
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
import org.rstudio.studio.client.rmarkdown.model.RmdChosenTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.model.RequestDocumentCloseEvent;
import org.rstudio.studio.client.server.model.RequestDocumentSaveEvent;
import org.rstudio.studio.client.workbench.ConsoleEditorProvider;
import org.rstudio.studio.client.workbench.MainWindowObject;
import org.rstudio.studio.client.workbench.FileMRUList;
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
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager.NavigationResult;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.codebrowser.CodeBrowserEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.ObjectExplorerEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.events.OpenObjectExplorerEvent;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.OpenProfileEvent;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerContents;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetPresentationHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Selection;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.NewWorkingCopyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.NewRMarkdownDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.NewRdDialog;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;
import org.rstudio.studio.client.workbench.views.source.model.RdShellResult;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocumentResult;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigationHistory;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SourceColumnManager implements BeforeShowHandler
{
   public SourceColumnManager(Source.Display display)
   {
      String name = display.setName(Source.COLUMN_PREFIX);
      columnMap.put(name, display);
      activeDisplay_ = display;

      display.addBeforeShowHandler(this);
   }

   public void add(Source.Display display)
   {
      add(display, false);
   }
   
   public void add(Source.Display display, boolean activate)
   {
      String name = display.setName(Source.COLUMN_PREFIX + StringUtil.makeRandomId(12));
      columnMap_.put(name, display);
      display.ensureVisible();

      if (activate || activeDisplay_ == null)
         activeDisplay_ = display;

      ensureVisible(false);
   }

   public void initialSelect(int index)
   {
      activeDisplay_.initialSelect();
   }

   public void setActive(String name)
   {
      activeDisplay_ = columnMap_.get(name);
   }

   public void setActive(EditingTarget target)
   {
      activeEditor_ = target;
      activeDisplay_ = findByDocument(target.getId());
      activeDisplay_.setActiveEditor(target);
   }

   public void activateColumns(final Command afterActivation)
   {
      if (activeEditor_ == null)
      {
         if (activeDisplay_ == null)
            activeDisplay_ = columnMap_.get(Source.COLUMN_PREFIX);
         newDoc(activeDisplay_, FileTypeRegistry.R, new ResultCallback<EditingTarget, ServerError>()
         {
            @Override
            public void onSuccess(EditingTarget target)
            {
               activeEditor_ = target;
               activeDisplay_.setActive(target);
               doActivateSource(afterActivation);
            }
         });
      }
      else
      {
         doActivateSource(afterActivation);
      }
   }

   public Source.Display getActive()
   {
      if (activeDisplay_ != null)
         return activeDisplay_;

      if (activeEditor_ != null)
      {
         activeDisplay_ = findByDocument(activeEditor_.getId());
         return activeDisplay_;
      }
      else
         activeDisplay_ = this.get(0);
      return activeDisplay_;
   }

   public ArrayList<Widget> getWidgets()
   {
      ArrayList<Widget> result = new ArrayList<Widget>();
      columnMap_.forEach((name, column) -> {
         result.add(column.asWidget());
      });
      return result;
   }

   public Source.Display findByDocument(String docId)
   {
      for (Map.Entry entry : columnMap_.entrySet())
      {
         Source.Display column = (Source.Display)entry.getValue();
         if (column.hasDoc(docId))
            return column;
      }
      return null;
   }

   public Source.Display findByPath(String path)
   {
      for (Map.Entry entry : columnMap_.entrySet())
      {
         Source.Display column = (Source.Display)entry.getValue();
         if (column.hasDocWithPath(docId))
            return column;
      }
      return null;
   }

   public Source.Display findByName(String name)
   {
      return columnMap_.get(name);
   }

   public Source.Display findByPosition(int x)
   {
      for (Map.Entry entry : columnMap_.entrySet())
      {
         Source.Display column = (Source.Display)entry.getValue();

         Widget w = column.asWidget();
         int left = w.getAbsoluteLeft();
         int right = w.getAbsoluteLeft() + w.getOffsetWidth();

         if (x > left && x < right)
            return column;
      }
      return null;
   }

   public boolean isEmpty(String name)
   {
      return columnMap_.get(name).getTabCount() == 0;
   }

   public void activateCodeBrowser(
         final String codeBrowserPath,
         boolean replaceIfActive,
         final ResultCallback<CodeBrowserEditingTarget,ServerError> callback)
   {
      // first check to see if this request can be fulfilled with an existing
      // code browser tab
      Display column = findByPath(codeBrowserPath);
      if (column != null)
      {
         EditingTarget target = column.getEditorWithPath(codeBrowserPath);
         column.selectTab(target.asWidget());

         // callback
         callback.onSuccess((CodeBrowserEditingTarget) target);

         // satisfied request
         return;
      }

      // then check to see if the active editor is a code browser -- if it is,
      // we'll use it as is, replacing its contents
      if (replaceIfActive &&
          activeEditor_ != null &&
          activeEditor_ instanceof CodeBrowserEditingTarget)
      {
         events_.fireEvent(new CodeBrowserCreatedEvent(activeEditor_.getId(),
               codeBrowserPath));
         callback.onSuccess((CodeBrowserEditingTarget) activeEditor_);
         return;
      }

      // create a new one
      newDoc(activeDisplay_,
            FileTypeRegistry.CODEBROWSER,
             new ResultCallback<EditingTarget, ServerError>()
             {
               @Override
               public void onSuccess(EditingTarget arg)
               {
                  events_.fireEvent(new CodeBrowserCreatedEvent(
                        arg.getId(), codeBrowserPath));
                  callback.onSuccess( (CodeBrowserEditingTarget)arg);
               }

               @Override
               public void onFailure(ServerError error)
               {
                  callback.onFailure(error);
               }

               @Override
               public void onCancelled()
               {
                  callback.onCancelled();
               }

            });
   }

   public void activateObjectExplorer(ObjectExplorerHandle handle)
   {
      columnMap_.forEach((name, column) -> {
         for (EditingTarget target : column.getEditors())
         {
            // bail if this isn't an object explorer filetype
            FileType fileType = target.getFileType();
            if (!(fileType instanceof ObjectExplorerFileType))
               continue;
   
            // check for identical titles
            if (handle.getTitle() == target.getTitle())
            {
               ((ObjectExplorerEditingTarget) target).update(handle);
               ensureVisible(false);
               column.selectTab(i);
               return;
            }
         }
      });

      ensureVisible(true);
      server_.newDocument(
            FileTypeRegistry.OBJECT_EXPLORER.getTypeId(),
            null,
            (JsObject) handle.cast(),
            new SimpleRequestCallback<SourceDocument>("Show Object Explorer")
            {
               @Override
               public void onResponseReceived(SourceDocument response)
               {
                  activeDisplay_.addTab(response, Source.OPEN_INTERACTIVE);
               }
            });
   }

   public void showOverflowPopout()
   {
      ensureVisible(false);
      activeDisplay_.showOverflowPopout();
   }

   public void showDataItem(DataItem data)
   {
      columnMap_.forEach((name, column) -> {
         for (EditingTarget target : column.getEditors())
         {
            String path = target.getPath();
            if (path != null && path.equals(data.getURI()))
            {
               ((DataEditingTarget)target).updateData(data);
   
               ensureVisible(false);
               column.selectTab(target);
               return;
            }
         }
      });

      ensureVisible(true);
      server_.newDocument(
            FileTypeRegistry.DATAFRAME.getTypeId(),
            null,
            (JsObject) data.cast(),
            new SimpleRequestCallback<SourceDocument>("Show Data Frame")
            {
               @Override
               public void onResponseReceived(SourceDocument response)
               {
                  activeDisplay_.addTab(response, Source.OPEN_INTERACTIVE);
               }
            });
   }

   private void doActivateSource(final Command afterActivation)
   {
      ensureVisible(false);
      if (activeEditor_ != null)
      {
         activeEditor_.focus();
         activeEditor_.ensureCursorVisible();
      }

      if (afterActivation != null)
         afterActivation.execute();
   }

   private void newDoc(Source.Display column,
                       EditableFileType fileType,
                       final String contents,
                       final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      column.ensureVisible(true);
      server_.newDocument(
            fileType.getTypeId(),
            contents,
            JsObject.createJsObject(),
            new SimpleRequestCallback<SourceDocument>(
                  "Error Creating New Document")
            {
               @Override
               public void onResponseReceived(SourceDocument newDoc)
               {
                  EditingTarget target = column.addTab(newDoc, Source.OPEN_INTERACTIVE);

                  if (contents != null)
                  {
                     target.forceSaveCommandActive();
                     manageSaveCommands();
                  }

                  if (resultCallback != null)
                     resultCallback.onSuccess(target);
               }

               @Override
               public void onError(ServerError error)
               {
                  if (resultCallback != null)
                     resultCallback.onFailure(error);
               }
            });
   }

   public void selectTab(EditingTarget target)
   {
      findByDocument(target.getId()).selectTab(target.asWidget());
   }

   public void closeTab(EditingTarget target, boolean interactive)
   {
      findByDocument(target.getId()).closeTab(target.asWidget(), interactive, null);
   }

   public void closeAllTabs(boolean excludeActive)
   {
      columnMap_.forEach((name, column) -> {
         cpsExecuteForEachEditor(column.getEditors(),
               new SPCEditingTargetCommand()
          {
             @Override
             public void execute(EditingTarget editingTarget, Command continuation)
             {
                if (excludeActive && target == activeEditor_)
                {
                   continuation.execute();
                   return;
                }
                else
                {
                   column.closeTab(editingTarget, false, continuation);
                }
             }
          });
      });
   }

   public void closeColumn(String name)
   {
      Source.Display display = findByName(name);
      if (display.getTabCount() > 0)
         return;
      if (display == activeDisplay_)
         activeDisplay_ = null;

      columnMap_.remove(name);
   }

   public void manageChevronVisibility()
   {
      columnMap_.forEach((name, column) -> {
         column.manageChevronVisibility();
      });
   }

   public void onBeforeShow(BeforeShowEvent event)
   {
      columnMap_.forEach((name, column) -> {
         if (column.getTabCount() == 0 && column.getNewTabPending() == 0)
         {
            // Avoid scenarios where the Source tab comes up but no tabs are
            // in it. (But also avoid creating an extra source tab when there
            // were already new tabs about to be created!)
            onNewSourceDoc();
         }
      });
   }
   
   /**
    * @param isNewTabPending True if a new tab is about to be created. (If
    *    false and there are no tabs already, then a new source doc might
    *    be created to make sure we don't end up with a source pane showing
    *    with no tabs in it.)
    */
   public void ensureVisible(boolean isNewTabPending)
   {
      columnMap_.forEach((name, column) -> {
         newTabPending_++;
         try
         {
            column.ensureVisible();
         }
         finally
         {
            newTabPending_--;
         }
      });
   }

   public void onNewSourceDoc()
   {
      EditableFileType fileType = FileTypeRegistry.R;

      TextFileType textType = (TextFileType)fileType;
      server_.getSourceTemplate("",
            "default" + textType.getDefaultExtension(),
            new ServerRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String template)
               {
                  // Create a new document with the supplied template
                  newDoc(fileType, template, null);
               }

               @Override
               public void onError(ServerError error)
               {
                  // Ignore errors; there's just not a template for this type
                  newDoc(fileType, null, null);
               }
            });
   }

   private void vimSetTabIndex(int index)
   {
      int tabCount = activeDisplay_.getTabCount();
      if (index >= tabCount)
         return;
      activeDisplay_.setPhysicalTabIndex(index);
   }

   Source.Display activeDisplay_;
   TextEditingTarget activeEditor_;

   // If positive, a new tab is about to be created
   private int newTabPending_;

   private HashMap<String,Source.Display> columnMap_ = new HashMap<String,Source.Display>();

   private final SourceServerOperations server_;
}
