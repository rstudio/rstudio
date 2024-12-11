/*
 * TextEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandUtil;
import org.rstudio.core.client.CommandWith2Args;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.IntervalTracker;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.WordWrap;
import org.rstudio.core.client.XRef;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasEnsureHeightHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsMap;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ResetEditorCommandsEvent;
import org.rstudio.studio.client.application.events.SetEditorCommandBindingsEvent;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.debugging.BreakpointManager;
import org.rstudio.studio.client.common.debugging.events.BreakpointsSavedEvent;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.SweaveFileType;
import org.rstudio.studio.client.common.filetypes.TexFileType;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.CopySourcePathEvent;
import org.rstudio.studio.client.common.filetypes.events.RenameFileInitiatedEvent;
import org.rstudio.studio.client.common.filetypes.events.RenameSourceFileEvent;
import org.rstudio.studio.client.common.mathjax.MathJax;
import org.rstudio.studio.client.common.presentation2.model.PresentationEditorLocation;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.SynctexUtils;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.events.GetEditorContextEvent;
import org.rstudio.studio.client.htmlpreview.events.ShowHTMLPreviewEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.notebook.CompileNotebookOptions;
import org.rstudio.studio.client.notebook.CompileNotebookOptionsDialog;
import org.rstudio.studio.client.notebook.CompileNotebookPrefs;
import org.rstudio.studio.client.notebook.CompileNotebookResult;
import org.rstudio.studio.client.palette.model.CommandPaletteEntryProvider;
import org.rstudio.studio.client.plumber.events.LaunchPlumberAPIEvent;
import org.rstudio.studio.client.plumber.events.PlumberAPIStatusEvent;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.quarto.QuartoHelper;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.events.ConvertToShinyDocEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdOutputFormatChangedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderPendingEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RmdEditorOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatterOutputOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdYamlData;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.YamlTree;
import org.rstudio.studio.client.rmarkdown.ui.RmdTemplateOptionsDialog;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.ShinyApplication;
import org.rstudio.studio.client.shiny.events.LaunchShinyApplicationEvent;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.model.ShinyTestResults;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.model.CopilotEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.shell.ConsoleLanguageTracker;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobRunScriptEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.LauncherJobRunScriptEvent;
import org.rstudio.studio.client.workbench.views.output.compilepdf.events.CompilePdfEvent;
import org.rstudio.studio.client.workbench.views.output.lint.LintManager;
import org.rstudio.studio.client.workbench.views.presentation.events.SourceFileSaveCompletedEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetCodeExecution;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource.EditingTargetNameProvider;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper.RmdSelectedTemplate;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceAfterCommandExecutedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.VimMarks;
import org.rstudio.studio.client.workbench.views.source.editors.text.assist.RChunkHeaderParser;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionOperation;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.BreakpointMoveEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.BreakpointSetEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CommandClickEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditingTargetSelectedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FindRequestedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.NewWorkingCopyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkExecUnit;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.events.InterruptChunkEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar.HideMessageHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar.StatusBarIconType;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupMenu;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupRequest;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.RMarkdownNoParamsDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualMode;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualMode.SyncType;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualModeChunk;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualModeUtil;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.CollabExternalEditEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocFocusedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragStateChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocWindowChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceFileSavedEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.CheckForExternalEditResult;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.ProjectConfig;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.terminal.events.SendToTerminalEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsDiffEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsHistoryEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRevertFileEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsViewOnGitHubEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.model.GitHubViewRequest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class TextEditingTarget implements
                                  EditingTarget,
                                  EditingTargetCodeExecution.CodeExtractor,
                                  HasAttachHandlers
{
   interface MyCommandBinder
         extends CommandBinder<Commands, TextEditingTarget>
   {
   }
   
   public static final String REFORMAT_ON_SAVE = "reformatOnSave";

   private static final String NOTEBOOK_TITLE = "notebook_title";
   private static final String NOTEBOOK_AUTHOR = "notebook_author";
   private static final String NOTEBOOK_TYPE = "notebook_type";

   public final static String DOC_OUTLINE_SIZE    = "docOutlineSize";
   public final static String DOC_OUTLINE_VISIBLE = "docOutlineVisible";

   public static final String RMD_VISUAL_MODE = "rmdVisualMode";
   public static final String RMD_VISUAL_MODE_WRAP_CONFIGURED = "rmdVisualWrapConfigured";
   public static final String RMD_VISUAL_MODE_COLLAPSED_CHUNKS = "rmdVisualCollapsedChunks";

   public static final String SOFT_WRAP_LINES = "softWrapLines";
   public static final String USE_RAINBOW_PARENS = "useRainbowParens";
   public static final String USE_RAINBOW_FENCED_DIVS = "useRainbowFencedDivs";
   
   public static final String QUARTO_PREVIEW_FORMAT = "quartoPreviewFormat";

   private static final MyCommandBinder commandBinder =
         GWT.create(MyCommandBinder.class);

   public interface Display extends TextDisplay,
                                    WarningBarDisplay,
                                    HasFindReplace,
                                    HasEnsureVisibleHandlers,
                                    HasEnsureHeightHandlers,
                                    HasResizeHandlers
   {
      HasValue<Boolean> getSourceOnSave();
      void ensureVisible();

      void findSelectAll();
      void findFromSelection();
      void findFromSelection(String selectionValue);

      StatusBar getStatusBar();

      boolean isAttached();

      void adaptToExtendedFileType(String extendedType);
      void onShinyApplicationStateChanged(String state);
      void onPlumberAPIStateChanged(String state);

      void debug_dumpContents();
      void debug_importDump();

      void setIsShinyFormat(boolean showOutputOptions,
                            boolean isPresentation,
                            boolean isShinyPrerendered);
      void setIsNotShinyFormat();
      void setIsNotebookFormat();
      void setFormatOptions(TextFileType fileType,
                            boolean showRmdFormatMenu,
                            boolean canEditFormatOptions,
                            List<String> options,
                            List<String> values,
                            List<String> extensions,
                            String selected);
      void setQuartoFormatOptions(TextFileType fileType, boolean showRmdFormatMenu, List<String> formats);
      HandlerRegistration addRmdFormatChangedHandler(RmdOutputFormatChangedEvent.Handler handler);

      void setPublishPath(String type, String publishPath);
      void invokePublish();

      void initWidgetSize();

      void toggleDocumentOutline();
      void toggleRmdVisualMode();
      void toggleSoftWrapMode();
      void toggleRainbowParens();
      void toggleRainbowFencedDivs();

      void setNotebookUIVisible(boolean visible);

      void setAccessibleName(String name);

      TextEditorContainer editorContainer();
      
      MarkdownToolbar getMarkdownToolbar();

      void manageCommandUI();

      void addVisualModeFindReplaceButton(ToolbarButton findReplaceButton);
      void showVisualModeFindReplaceButton(boolean show);
      
      SourceColumn getSourceColumn();
      
      HandlerRegistration addAttachHandler(AttachEvent.Handler handler);
   }

   private class SaveProgressIndicator implements ProgressIndicator
   {
      public SaveProgressIndicator(FileSystemItem file,
                                   TextFileType fileType,
                                   boolean suppressFileLockError,
                                   Command executeOnSuccess)
      {
         this(file, fileType, suppressFileLockError, executeOnSuccess, null);
      }

      public SaveProgressIndicator(FileSystemItem file,
                                   TextFileType fileType,
                                   boolean suppressFileLockError,
                                   Command executeOnSuccess,
                                   Command executeOnSilentFailure)
      {
         file_ = file;
         newFileType_ = fileType;
         suppressFileLockError_ = suppressFileLockError;
         executeOnSuccess_ = executeOnSuccess;
         executeOnSilentFailure_ = executeOnSilentFailure;
      }

      public void onProgress(String message)
      {
         onProgress(message, null);
      }

      public void onProgress(String message, Operation onCancel)
      {
      }

      public void clearProgress()
      {
      }

      public void onCompleted()
      {
         isSaving_ = false;

         // don't need to check again soon because we just saved
         // (without this and when file monitoring is active we'd
         // end up immediately checking for external edits)
         externalEditCheckInterval_.reset(250);
         boolean fileTypeChanged = true;

         if (newFileType_ != null)
         {
            // if we already had a file type, see if the underlying type has changed
            if (fileType_ != null)
            {
               fileTypeChanged = !StringUtil.equals(newFileType_.getTypeId(), fileType_.getTypeId());
            }
            
            fileType_ = newFileType_;
            if (fileTypeChanged)
               copilotHelper_.onFileTypeChanged();
         }

         if (file_ != null)
         {
            ignoreDeletes_ = false;
            forceSaveCommandActive_ = false;
            commands_.reopenSourceDocWithEncoding().setEnabled(true);
            name_.setValue(file_.getName(), true);
            // Make sure tooltip gets updated, even if name hasn't changed
            name_.fireChangeEvent();
            dirtyState_.markClean();
         }

         if (newFileType_ != null && fileTypeChanged)
         {
            // Make sure the icon gets updated, even if name hasn't changed
            name_.fireChangeEvent();
            updateStatusBarLanguage();
            view_.adaptToFileType(newFileType_);

            // turn R Markdown behavior (inline execution, previews, etc.)
            // based on whether we just became an R Markdown type
            setRMarkdownBehaviorEnabled(newFileType_.isRmd());

            events_.fireEvent(new FileTypeChangedEvent());
            if (!isSourceOnSaveEnabled() && docUpdateSentinel_.sourceOnSave())
            {
               view_.getSourceOnSave().setValue(false, true);
            }
         }

         if (executeOnSuccess_ != null)
            executeOnSuccess_.execute();
      }

      public void onError(final String message)
      {
         isSaving_ = false;

         // in case the error occurred saving a document that wasn't
         // in the foreground
         view_.ensureVisible();

         // command to show the error
         final Command showErrorCommand = new Command() {
            @Override
            public void execute()
            {
               // do not show the error if it is a transient autosave related issue - this can occur fairly frequently
               // when attempting to save files that are being backed up by external software
               if (message.contains(constants_.onErrorMessage()) && suppressFileLockError_)
               {
                  if (executeOnSilentFailure_ != null)
                     executeOnSilentFailure_.execute();

                  return;
               }

               globalDisplay_.showErrorMessage(constants_.errorSavingFile(), message);
            }
         };

         // check whether the file exists and isn't writeable
         if (file_ != null)
         {
            server_.isReadOnlyFile(file_.getPath(),
                                   new ServerRequestCallback<Boolean>() {

               @Override
               public void onResponseReceived(Boolean isReadOnly)
               {
                  if (isReadOnly)
                  {
                     String message = constants_.onResponseReceivedMessage();
                     view_.showWarningBar(message);

                     String saveAsPath = file_.getParentPath().completePath(constants_.saveAsPathName(
                             file_.getStem(), file_.getExtension()));
                     saveNewFile(
                           saveAsPath,
                           null,
                           CommandUtil.join(postSaveCommand(false), new Command() {

                              @Override
                              public void execute()
                              {
                                 view_.hideWarningBar();
                              }
                           }));

                  }
                  else
                  {
                     showErrorCommand.execute();
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  showErrorCommand.execute();
               }
            });
         }
         else
         {
            showErrorCommand.execute();
         }


      }

      private final FileSystemItem file_;
      private final TextFileType newFileType_;
      private final boolean suppressFileLockError_;
      private final Command executeOnSuccess_;
      private final Command executeOnSilentFailure_;
   }

   @Inject
   public TextEditingTarget(Commands commands,
                            SourceServerOperations server,
                            EventBus events,
                            GlobalDisplay globalDisplay,
                            FileDialogs fileDialogs,
                            FileTypeRegistry fileTypeRegistry,
                            FileTypeCommands fileTypeCommands,
                            ConsoleDispatcher consoleDispatcher,
                            WorkbenchContext workbenchContext,
                            Session session,
                            Synctex synctex,
                            FontSizeManager fontSizeManager,
                            DocDisplay docDisplay,
                            UserPrefs prefs,
                            UserState state,
                            BreakpointManager breakpointManager,
                            Source source,
                            DependencyManager dependencyManager)
   {
      commands_ = commands;
      server_ = server;
      events_ = events;
      globalDisplay_ = globalDisplay;
      fileDialogs_ = fileDialogs;
      fileTypeRegistry_ = fileTypeRegistry;
      fileTypeCommands_ = fileTypeCommands;
      consoleDispatcher_ = consoleDispatcher;
      workbenchContext_ = workbenchContext;
      session_ = session;
      synctex_ = synctex;
      fontSizeManager_ = fontSizeManager;
      breakpointManager_ = breakpointManager;
      source_ = source;
      dependencyManager_ = dependencyManager;

      docDisplay_ = docDisplay;
      dirtyState_ = new DirtyState(docDisplay_, false);
      lintManager_ = new LintManager(new TextEditingTargetLintSource(this), releaseOnDismiss_);
      prefs_ = prefs;
      state_ = state;
      compilePdfHelper_ = new TextEditingTargetCompilePdfHelper(docDisplay_);
      rmarkdownHelper_ = new TextEditingTargetRMarkdownHelper();
      cppHelper_ = new TextEditingTargetCppHelper(cppCompletionContext_,
                                                  docDisplay_);
      jsHelper_ = new TextEditingTargetJSHelper(docDisplay_);
      sqlHelper_ = new TextEditingTargetSqlHelper(docDisplay_);
      presentationHelper_ = new TextEditingTargetPresentationHelper(docDisplay_);
      presentation2Helper_ = new TextEditingTargetPresentation2Helper(docDisplay_);
      rHelper_ = new TextEditingTargetRHelper(docDisplay_);
      quartoHelper_ = new TextEditingTargetQuartoHelper(this, docDisplay_);

      docDisplay_.setRnwCompletionContext(compilePdfHelper_);
      docDisplay_.setCppCompletionContext(cppCompletionContext_);
      docDisplay_.setRCompletionContext(rContext_);
      scopeHelper_ = new TextEditingTargetScopeHelper(docDisplay_);

      addRecordNavigationPositionHandler(releaseOnDismiss_,
                                         docDisplay_,
                                         events_,
                                         this);

      copilotHelper_ = new TextEditingTargetCopilotHelper(this);
      
      EditingTarget target = this;
      docDisplay_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            NativeEvent ne = event.getNativeEvent();
            int mod = KeyboardShortcut.getModifierValue(ne);

            if ((mod == KeyboardShortcut.META || (
                  mod == KeyboardShortcut.CTRL &&
                  !BrowseCap.hasMetaKey() &&
                  !docDisplay_.isEmacsModeOn() &&
                  (!docDisplay_.isVimModeOn() || docDisplay_.isVimInInsertMode())))
                && ne.getKeyCode() == 'F')
            {
               event.preventDefault();
               event.stopPropagation();
               commands_.findReplace().execute();
            }
            else if (BrowseCap.hasMetaKey() &&
                     (mod == KeyboardShortcut.META) &&
                     (ne.getKeyCode() == 'E'))
            {
               event.preventDefault();
               event.stopPropagation();
               commands_.findFromSelection().execute();
            }
            else if (mod == KeyboardShortcut.CTRL
                     && ne.getKeyCode() == KeyCodes.KEY_UP
                     && fileType_ == FileTypeRegistry.R)
            {
               event.preventDefault();
               event.stopPropagation();
               jumpToPreviousFunction();
            }
            else if (mod == KeyboardShortcut.CTRL
                     && ne.getKeyCode() == KeyCodes.KEY_DOWN
                     && fileType_ == FileTypeRegistry.R)
            {
               event.preventDefault();
               event.stopPropagation();
               jumpToNextFunction();
            }
            else if ((ne.getKeyCode() == KeyCodes.KEY_ESCAPE) &&
                     prefs_.editorKeybindings().getValue() != UserPrefs.EDITOR_KEYBINDINGS_VIM)
            {
               event.preventDefault();
               event.stopPropagation();

               // Don't send an interrupt if a popup is visible
               if (docDisplay_.isPopupVisible())
                  return;

               // Don't send an interrupt if we're in a source window
               if (!SourceWindowManager.isMainSourceWindow())
                  return;

               if (commands_.interruptR().isEnabled())
                  commands_.interruptR().execute();
            }
            else if (TextEditingTargetQuartoHelper.continueSpecialCommentOnNewline(docDisplay_, ne))
            {
               // nothing to do; continueSpecialCommentOnNewline() does all the magic
            }
            else if (
                  prefs_.continueCommentsOnNewline().getValue() &&
                  !docDisplay_.isPopupVisible() &&
                  ne.getKeyCode() == KeyCodes.KEY_ENTER && mod == 0 &&
                    (fileType_.isC() || isCursorInRMode(docDisplay_) || isCursorInTexMode(docDisplay_)))
            {
               String line = docDisplay_.getCurrentLineUpToCursor();
               
               // validate that this line is composed of only comments and whitespace
               // (necessary to check token type for e.g. Markdown documents)
               // https://github.com/rstudio/rstudio/issues/6421
               JsArray<Token> tokens =
                     docDisplay_.getTokens(docDisplay_.getCursorPosition().getRow());
               
               boolean isCommentLine = true;
               for (int i = 0, n = tokens.length(); i < n; i++)
               {
                  Token token = tokens.get(i);
                  
                  // allow for empty whitespace tokens
                  String value = token.getValue();
                  if (value.trim().isEmpty())
                     continue;
                  
                  // allow tokens explicitly declared as comments
                  if (token.hasType("comment"))
                     continue;
                  
                  // if we got here, then we got a non-whitespace, non-comment token,
                  // so we cannot continue the comment
                  isCommentLine = false;
                  break;
               }
               
               Pattern pattern = null;
               
               if (!isCommentLine)
               {
                  pattern = null;
               }
               else if (isCursorInRMode(docDisplay_))
               {
                  pattern = Pattern.create("^(\\s*#+'?\\s*)");
               }
               else if (isCursorInTexMode(docDisplay_))
               {
                  pattern = Pattern.create("^(\\s*%+'?\\s*)");
               }
               else if (fileType_.isC())
               {
                  // bail on attributes
                  if (!line.matches("^\\s*//\\s*\\[\\[.*\\]\\].*"))
                     pattern = Pattern.create("^(\\s*//'?\\s*)");
               }

               if (pattern != null)
               {
                  Match match = pattern.match(line, 0);
                  if (match != null)
                  {
                     event.preventDefault();
                     event.stopPropagation();
                     docDisplay_.insertCode("\n" + match.getGroup(1));
                     docDisplay_.ensureCursorVisible();
                  }
               }
            }
            else if (
                  prefs_.continueCommentsOnNewline().getValue() &&
                  !docDisplay_.isPopupVisible() &&
                  ne.getKeyCode() == KeyCodes.KEY_ENTER &&
                  mod == KeyboardShortcut.SHIFT)
            {
               event.preventDefault();
               event.stopPropagation();
               String indent = docDisplay_.getNextLineIndent();
               docDisplay_.insertCode("\n" + indent);
            }
            events_.fireEvent(new EditingTargetSelectedEvent(target));
         }
      });

      docDisplay_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            events_.fireEvent(new EditingTargetSelectedEvent(target));
         }
      });

      docDisplay_.addCommandClickHandler(new CommandClickEvent.Handler()
      {
         @Override
         public void onCommandClick(CommandClickEvent event)
         {
            // bail if the target is a link marker (implies already handled)
            NativeEvent nativeEvent = event.getNativeEvent();
            Element target = nativeEvent.getEventTarget().cast();
            if (target != null && target.hasClassName("ace_marker"))
            {
               nativeEvent.stopPropagation();
               nativeEvent.preventDefault();
               return;
            }

            // force cursor position
            Position position = event.getEvent().getDocumentPosition();
            docDisplay_.setCursorPosition(position);

            // delegate to handlers
            if (fileType_.canCompilePDF() &&
                commands_.synctexSearch().isEnabled())
            {
               // warn firefox users that this doesn't really work in Firefox
               if (BrowseCap.isFirefox() && !BrowseCap.isMacintosh())
                  SynctexUtils.maybeShowFirefoxWarning("PDF preview");

               doSynctexSearch(true);
            }
            else
            {
               docDisplay_.goToDefinition();
            }
         }
      });

      docDisplay_.addFindRequestedHandler(new FindRequestedEvent.Handler() {
         @Override
         public void onFindRequested(FindRequestedEvent event)
         {
            view_.showFindReplace(event.getDefaultForward());
         }
      });

      docDisplay_.addScopeTreeReadyHandler(new ScopeTreeReadyEvent.Handler()
      {
         @Override
         public void onScopeTreeReady(ScopeTreeReadyEvent event)
         {
            updateCurrentScope();
         }
      });

      docDisplay_.addEditorBlurHandler((BlurEvent evt) ->
      {
         maybeAutoSaveOnBlur();
      });

      releaseOnDismiss_.add(
         prefs.autoSaveOnBlur().addValueChangeHandler((ValueChangeEvent<Boolean> val) ->
         {
            // When the user turns on autosave, disable Source on Save if it was
            // previously enabled; otherwise documents which were open with this
            // setting enabled will start sourcing themselves on blur.
            if (val.getValue())
            {
               setSourceOnSave(false);
            }
         }));
      releaseOnDismiss_.add(
         prefs.autoSaveOnIdle().bind((String behavior) ->
         {
            if (behavior == UserPrefs.AUTO_SAVE_ON_IDLE_COMMIT)
            {
               // When switching into autosave on idle mode, start the timer
               setSourceOnSave(false);
               nudgeAutosave();
            }
            else
            {
               // When leaving it, stop the timer
               autoSaveTimer_.cancel();
            }
         }));
      
      releaseOnDismiss_.add(
            events_.addHandler(RenameFileInitiatedEvent.TYPE, new RenameFileInitiatedEvent.Handler()
            {
               @Override
               public void onRenameFileInitiated(RenameFileInitiatedEvent event)
               {
                  if (StringUtil.equals(event.getPath(), docUpdateSentinel_.getPath()))
                  {
                     save();
                  }
               }
            }));
   }

   static {
      initializeIncrementalSearch();
   }

   private static final native String initializeIncrementalSearch() /*-{
      var IncrementalSearch = $wnd.require("ace/incremental_search").IncrementalSearch;
      (function() {
         this.message = $entry(function(msg) {
            @org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget::setIncrementalSearchMessage(Ljava/lang/String;)(msg);
         });

      }).call(IncrementalSearch.prototype);
   }-*/;

   private static final native boolean isIncrementalSearchCommand(JavaScriptObject data) /*-{
      var command = data.command;
      if (command == null)
         return false;

      var result =
         command.name === "iSearch" ||
         command.name === "iSearchBackwards" ||
         command.isIncrementalSearchCommand === true;

      return result;
   }-*/;

   private static String sIncrementalSearchMessage_ = null;
   private static final void setIncrementalSearchMessage(String message)
   {
      sIncrementalSearchMessage_ = message;
   }

   private static final String getIncrementalSearchMessage()
   {
      return sIncrementalSearchMessage_;
   }

   private boolean moveCursorToNextSectionOrChunk(boolean includeSections)
   {
      Scope current = docDisplay_.getCurrentScope();
      ScopeList scopes = new ScopeList(docDisplay_);
      Position cursorPos = docDisplay_.getCursorPosition();

      int n = scopes.size();
      for (int i = 0; i < n; i++)
      {
         Scope scope = scopes.get(i);
         if (!(scope.isChunk() || (scope.isSection() && includeSections)))
            continue;

         if (scope.equals(current))
            continue;

         if (scope.getPreamble().isAfter(cursorPos))
         {
            moveCursorToNextPrevSection(scope.getPreamble());
            return true;
         }
      }

      return false;
   }

   private boolean moveCursorToPreviousSectionOrChunk(boolean includeSections)
   {
      ScopeList scopes = new ScopeList(docDisplay_);
      Position cursorPos = docDisplay_.getCursorPosition();

      int n = scopes.size();
      for (int i = n - 1; i >= 0; i--)
      {
         Scope scope = scopes.get(i);
         if (!(scope.isChunk() || (includeSections && scope.isSection())))
            continue;

         if (scope.getPreamble().isBefore(cursorPos))
         {
            moveCursorToNextPrevSection(scope.getPreamble());
            return true;
         }
      }

      return false;
   }

   private void moveCursorToNextPrevSection(Position pos)
   {
      docDisplay_.setCursorPosition(pos);
      docDisplay_.moveCursorNearTop(5);
   }

   @Handler
   void onSwitchFocusSourceConsole()
   {
      if (docDisplay_.isFocused())
         commands_.activateConsole().execute();
      else
         commands_.activateSource().execute();
   }

   @Handler
   void onGoToStartOfCurrentScope()
   {
      docDisplay_.focus();
      Scope scope = docDisplay_.getCurrentScope();
      if (scope != null)
      {
         Position position = Position.create(
               scope.getBodyStart().getRow(),
               scope.getBodyStart().getColumn() + 1);
         docDisplay_.setCursorPosition(position);
      }
   }

   @Handler
   void onGoToEndOfCurrentScope()
   {
      docDisplay_.focus();
      Scope scope = docDisplay_.getCurrentScope();
      if (scope != null)
      {
         Position end = scope.getEnd();
         if (end != null)
         {
            Position position = Position.create(
                  end.getRow(),
                  Math.max(0, end.getColumn() - 1));
            docDisplay_.setCursorPosition(position);
         }
      }
   }

   @Handler
   void onGoToNextSection()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.goToNextSection();
      }
      else
      {
         if (docDisplay_.getFileType().canGoNextPrevSection())
         {
            if (!moveCursorToNextSectionOrChunk(true))
               docDisplay_.gotoPageDown();
         }
         else
         {
            docDisplay_.gotoPageDown();
         }
      }

   }

   @Handler
   void onGoToPrevSection()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.goToPreviousSection();
      }
      else
      {
         if (docDisplay_.getFileType().canGoNextPrevSection())
         {
            if (!moveCursorToPreviousSectionOrChunk(true))
               docDisplay_.gotoPageUp();
         }
         else
         {
            docDisplay_.gotoPageUp();
         }
      }
   }

   @Handler
   void onGoToNextChunk()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.goToNextChunk();
      }
      else
      {
         moveCursorToNextSectionOrChunk(false);
      }
   }

   @Handler
   void onGoToPrevChunk()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.goToPreviousChunk();
      }
      else
      {
         moveCursorToPreviousSectionOrChunk(false);
      }
   }


   public void ensureTextEditorActive(Command command)
   {
      visualMode_.deactivate(command);
   }

   public void ensureVisualModeActive(Command command)
   {
      visualMode_.activate(command);
   }

   public void onVisualEditorBlur()
   {
      maybeAutoSaveOnBlur();
   }

   public void navigateToXRef(String xref)
   {
      ensureVisualModeActive(() -> {
         Scheduler.get().scheduleDeferred(() -> {
            visualMode_.navigateToXRef(xref, false);
         });

      });
   }

   public void navigateToXRef(XRef xref, boolean forceVisualMode)
   {
      if (isVisualModeActivated() || forceVisualMode)
      {
         ensureVisualModeActive(() -> {
            Scheduler.get().scheduleDeferred(() -> {
               visualMode_.navigateToXRef(xref.getXRefString(), false);
            });
         });
      }
      else
      {
         String title = xref.getTitle();
         for (int i = 0, n = docDisplay_.getRowCount(); i < n; i++)
         {
            String line = docDisplay_.getLine(i);
            int index = line.indexOf(title);
            if (index == -1)
               continue;

            navigateToPosition(
                  SourcePosition.create(i, index),
                  false);
         }
      }

   }
   
   public void navigateToPresentationEditorLocation(PresentationEditorLocation location)
   {
      if (isVisualModeActivated())
      {
         ensureVisualModeActive(() -> {
            Scheduler.get().scheduleDeferred(() -> {
               visualMode_.navigateToPresentationEditorLocation(location);
            });
         });
      }
      else
      {
         presentation2Helper_.navigateToPresentationEditorLocation(location);
      }
   }

   // the navigateToPosition methods are called by modules that explicitly
   // want the text editor active (e.g. debugging, find in files, etc.) so they
   // don't chec for visual mode

   @Override
   public void navigateToPosition(SourcePosition position,
                                  boolean recordCurrent)
   {
      navigateToVisualPosition(position, (disp, pos) ->
      {
         disp.navigateToPosition(pos, recordCurrent);
      });
   }

   @Override
   public void navigateToPosition(SourcePosition position,
                                  boolean recordCurrent,
                                  boolean highlightLine)
   {
      navigateToVisualPosition(position, (disp, pos) ->
      {
         disp.navigateToPosition(pos, recordCurrent, highlightLine, false);
      });
   }

   @Override
   public void navigateToPosition(SourcePosition position,
                                  boolean recordCurrent,
                                  boolean highlightLine,
                                  boolean moveCursor,
                                  Command onNavigationCompleted)
   {
      navigateToVisualPosition(position, (disp, pos) ->
      {
         disp.navigateToPosition(pos, recordCurrent, highlightLine, !moveCursor);
         if (onNavigationCompleted != null)
            onNavigationCompleted.execute();
      });
   }

   /**
    * Navigate to a source position, possibly in the visual editor.
    *
    * @param pos The position to navigate to
    * @param navCommand The command that actually performs the navigation
    */
   private void navigateToVisualPosition(SourcePosition pos,
                                         CommandWith2Args<DocDisplay, SourcePosition> navCommand)
   {
      if (isVisualEditorActive())
      {
         VisualModeChunk chunk = visualMode_.getChunkAtRow(pos.getRow());
         if (chunk == null)
         {
            // No editor chunk at this position, so we need to switch to text
            // editor mode.
            ensureTextEditorActive(() ->
            {
               navCommand.execute(docDisplay_, pos);
            });
         }
         else
         {
            // Adjust the position based on the chunk's offset and navigate
            // there.
            SourcePosition newPos = SourcePosition.create(
                  pos.getRow() - chunk.getScope().getPreamble().getRow(),
                  pos.getColumn());
            navCommand.execute(chunk.getAceInstance(), newPos);
            chunk.focus();

            // Scroll the cursor into view; we have to do this after a layout
            // pass so that Ace has time to render the cursor.
            Scheduler.get().scheduleDeferred(() ->
            {
               chunk.scrollCursorIntoView();
            });
         }
      }
      else
      {
         // No visual editor active, so navigate directly
         navCommand.execute(docDisplay_, pos);
      }
   }

   // These methods are called by SourceNavigationHistory and source pane management
   // features (e.g. external source window and source columns) so need to check for
   // and dispatch to visual mode

   @Override
   public void recordCurrentNavigationPosition()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.recordCurrentNavigationPosition();
      }
      else
      {
         docDisplay_.recordCurrentNavigationPosition();
      }
   }


   @Override
   public void restorePosition(SourcePosition position)
   {
      if (visualMode_.isVisualModePosition(position))
      {
         ensureVisualModeActive(() -> {
            visualMode_.navigate(position, false);
         });
      }
      else
      {
         ensureTextEditorActive(() -> {
            docDisplay_.restorePosition(position);
         });
      }
   }

   @Override
   public SourcePosition currentPosition()
   {
      if (visualMode_.isActivated())
      {
         return visualMode_.getSourcePosition();
      }
      else
      {
         Position cursor = docDisplay_.getCursorPosition();
         if (docDisplay_.hasLineWidgets())
         {
            // if we have line widgets, they create an non-reproducible scroll
            // position, so use the cursor position only
            return SourcePosition.create(cursor.getRow(), cursor.getColumn());
         }
         return SourcePosition.create(getContext(), cursor.getRow(),
               cursor.getColumn(), docDisplay_.getScrollTop());
      }

   }

   @Override
   public boolean isAtSourceRow(SourcePosition position)
   {
      if (visualMode_.isActivated())
      {
         return visualMode_.isAtRow(position);
      }
      else
      {
         return docDisplay_.isAtSourceRow(position);
      }

   }

   @Override
   public void setCursorPosition(Position position)
   {
      docDisplay_.setCursorPosition(position);
   }

   @Override
   public void ensureCursorVisible()
   {
      docDisplay_.ensureCursorVisible();
   }

   @Override
   public void forceLineHighlighting()
   {
      docDisplay_.setHighlightSelectedLine(true);
   }

   @Override
   public void setSourceOnSave(boolean sourceOnSave)
   {
      if (view_ != null)
      {
         view_.getSourceOnSave().setValue(sourceOnSave, true);
      }
   }

   @Override
   public void highlightDebugLocation(
         SourcePosition startPos,
         SourcePosition endPos,
         boolean executing)
   {
      if (documentDirtyHandler_ == null)
      {
         documentDirtyHandler_ = dirtyState_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
         {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event)
            {
               if (documentDirtyHandler_ != null)
               {
                  documentDirtyHandler_.removeHandler();
                  documentDirtyHandler_ = null;
               }
               
               documentChangedDuringDebugSession_ = true;
               updateDebugWarningBar();
            }
         });
      }
      
      debugStartPos_ = startPos;
      debugEndPos_ = endPos;
      docDisplay_.highlightDebugLocation(startPos, endPos, executing);
      updateDebugWarningBar();
   }

   @Override
   public void endDebugHighlighting()
   {
      if (documentDirtyHandler_ != null)
      {
         documentDirtyHandler_.removeHandler();
         documentDirtyHandler_ = null;
      }
      
      docDisplay_.endDebugHighlighting();
      documentChangedDuringDebugSession_ = false;
      debugStartPos_ = null;
      debugEndPos_ = null;
      hideDebugWarningBar();
   }

   @Override
   public void beginCollabSession(CollabEditStartParams params)
   {
      visualMode_.deactivate(() -> {
         // the server may notify us of a collab session we're already
         // participating in; this is okay
         if (docDisplay_.hasActiveCollabSession())
         {
            return;
         }

         // were we waiting to process another set of params when these arrived?
         boolean paramQueueClear = queuedCollabParams_ == null;

         // save params
         queuedCollabParams_ = params;

         // if we're not waiting for another set of params to resolve, and we're
         // the active doc, process these params immediately
         if (paramQueueClear && isActiveDocument())
         {
            beginQueuedCollabSession();
         }
      });
   }

   @Override
   public void endCollabSession()
   {
      if (docDisplay_.hasActiveCollabSession())
         docDisplay_.endCollabSession();

      // a collaboration session may have come and gone while the tab was not
      // focused
      queuedCollabParams_ = null;
   }

   private void beginQueuedCollabSession()
   {
      // do nothing if we don't have an active path
      if (docUpdateSentinel_ == null || docUpdateSentinel_.getPath() == null)
         return;

      // do nothing if we don't have queued params
      final CollabEditStartParams params = queuedCollabParams_;
      if (params == null)
         return;

      // if we have local changes, and we're not the master copy nor rejoining a
      // previous edit session, we need to prompt the user
      if (dirtyState().getValue() && !params.isMaster() &&
          !params.isRejoining())
      {
         String filename =
               FilePathUtils.friendlyFileName(docUpdateSentinel_.getPath());
         globalDisplay_.showYesNoMessage(
               GlobalDisplay.MSG_QUESTION,
               constants_.beginQueuedCollabSessionCaption(),
               constants_.beginQueuedCollabSessionMessage(filename),
               false, // includeCancel
               new Operation()
               {
                  @Override
                  public void execute()
                  {
                     docDisplay_.beginCollabSession(params, dirtyState_);
                     queuedCollabParams_ = null;
                  }
               },
               new Operation()
               {
                  @Override
                  public void execute()
                  {
                     // open a new tab for the user's local changes
                     events_.fireEvent(new NewWorkingCopyEvent(fileType_,
                           docUpdateSentinel_.getPath(),
                           docUpdateSentinel_.getContents()));

                     // let the collab session initiate in this tab
                     docDisplay_.beginCollabSession(params, dirtyState_);
                     queuedCollabParams_ = null;
                  }
               },
               null, // cancelOperation,
               constants_.beginQueuedCollabSessionYesLabel(),
               constants_.beginQueuedCollabSessionNoLabel(),
               true  // yesIsDefault
               );
      }
      else
      {
         // just begin the session right away
         docDisplay_.beginCollabSession(params, dirtyState_);
         queuedCollabParams_ = null;
      }
   }
   
   private void hideDebugWarningBar()
   {
      if (isDebugWarningVisible_)
      {
         isDebugWarningVisible_ = false;
         view_.hideWarningBar();
      }
   }

   private void updateDebugWarningBar(String message, boolean force)
   {
      if (force || documentChangedDuringDebugSession_)
      {
         isDebugWarningVisible_ = true;
         view_.hideWarningBar();
         view_.showWarningBar(message);
         return;
      }
      
      // show the warning bar if we're debugging and the document is dirty
      if (debugStartPos_ != null &&
          dirtyState().getValue() &&
          !isDebugWarningVisible_)
      {
         view_.showWarningBar(message);
         isDebugWarningVisible_ = true;
      }
      
      // hide the warning bar if the dirty state or debug state change
      else if (isDebugWarningVisible_ &&
               (debugStartPos_ == null || dirtyState().getValue() == false))
      {
         view_.hideWarningBar();
         // if we're still debugging, start highlighting the line again
         if (debugStartPos_ != null)
         {
            docDisplay_.highlightDebugLocation(
                  debugStartPos_,
                  debugEndPos_, false);
         }
         isDebugWarningVisible_ = false;
      }
   }
 
   private void updateDebugWarningBar(String message)
   {
      updateDebugWarningBar(message, true);
   }
   
   private void updateDebugWarningBar()
   {
      updateDebugWarningBar(constants_.updateDebugWarningBarMessage(), false);
   }

   public void showWarningMessage(String message)
   {
      view_.showWarningBar(message);
   }

   public void showRequiredPackagesMissingWarning(List<String> packages)
   {
      view_.showRequiredPackagesMissingWarning(packages);
   }

   public void showTexInstallationMissingWarning(String message)
   {
      view_.showTexInstallationMissingWarning(message);
   }

   public void installTinyTeX()
   {
      Command onInstall = () -> {
         String code = "tinytex::install_tinytex()";
         events_.fireEvent(new SendToConsoleEvent(code, true));
      };

      dependencyManager_.withTinyTeX(
            constants_.installTinytexLowercase(),
            constants_.installTinyTeX(),
            onInstall);
   }

   private void jumpToPreviousFunction()
   {
      Scope jumpTo = scopeHelper_.getPreviousFunction(
            docDisplay_.getCursorPosition());

      if (jumpTo != null)
         docDisplay_.navigateToPosition(toSourcePosition(jumpTo), true);
   }

   private void jumpToNextFunction()
   {
      Scope jumpTo = scopeHelper_.getNextFunction(
            docDisplay_.getCursorPosition());

      if (jumpTo != null)
         docDisplay_.navigateToPosition(toSourcePosition(jumpTo), true);
   }

   public void initialize(SourceColumn column,
                          final SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          EditingTargetNameProvider defaultNameProvider)
   {
      id_ = document.getId();
      fileContext_ = fileContext;
      fileType_ = (TextFileType) type;
      codeExecution_ = new EditingTargetCodeExecution(this, docDisplay_, getId(),
            this);
      extendedType_ = document.getExtendedType();
      extendedType_ = rmarkdownHelper_.detectExtendedType(document.getContents(),
                                                          extendedType_,
                                                          fileType_);

      themeHelper_ = new TextEditingTargetThemeHelper(this, events_, releaseOnDismiss_);

      docUpdateSentinel_ = new DocUpdateSentinel(
            server_,
            docDisplay_,
            document,
            globalDisplay_.getProgressIndicator(constants_.saveFile()),
            dirtyState_,
            events_,
            prefs_,
            () ->
            {
               // Implement chunk definition provider
               if (visualMode_.isVisualEditorActive())
               {
                  return visualMode_.getChunkDefs();
               }
               else
               {
                  return docDisplay_.getChunkDefs();
               }
            });

      view_ = new TextEditingTargetWidget(this,
                                          docUpdateSentinel_,
                                          commands_,
                                          prefs_,
                                          state_,
                                          fileTypeRegistry_,
                                          docDisplay_,
                                          fileType_,
                                          extendedType_,
                                          events_,
                                          session_,
                                          column);
      

      events_.addHandler(
            this,
            FileChangeEvent.TYPE,
            new FileChangeEvent.Handler()
      {
         @Override
         public void onFileChange(FileChangeEvent event)
         {
            // screen out adds and events that aren't for our path
            FileChange fileChange = event.getFileChange();
            if (fileChange.getType() == FileChange.ADD)
               return;
            else if (!fileChange.getFile().getPath().equals(getPath()))
               return;

            // always check for changes if this is the active editor
            if (isActiveDocument())
               checkForExternalEdit();

            // also check for changes on modifications if we are not dirty
            // note that we don't check for changes on removed files because
            // this will show a confirmation dialog
            else if (event.getFileChange().getType() == FileChange.MODIFIED &&
                     dirtyState().getValue() == false)
            {
               checkForExternalEdit();
            }
         }
      });
      
      events_.addHandler(
            this,
            ShinyApplicationStatusEvent.TYPE,
            new ShinyApplicationStatusEvent.Handler()
            {
               @Override
               public void onShinyApplicationStatus(
                     ShinyApplicationStatusEvent event)
               {
                  // If the document appears to be inside the directory
                  // associated with the event, update the view to match the
                  // new state.
                  if (getPath() != null &&
                      getPath().startsWith(event.getParams().getPath()))
                  {
                     String state = event.getParams().getState();
                     if (event.getParams().getViewerType() !=
                            UserPrefs.SHINY_VIEWER_TYPE_PANE &&
                         event.getParams().getViewerType() !=
                            UserPrefs.SHINY_VIEWER_TYPE_WINDOW)
                     {
                        // we can't control the state when it's not in an
                        // RStudio-owned window, so treat the app as stopped
                        state = ShinyApplicationParams.STATE_STOPPED;
                     }
                     view_.onShinyApplicationStateChanged(state);
                  }
               }
            });

      events_.addHandler(
            this,
            PlumberAPIStatusEvent.TYPE,
            new PlumberAPIStatusEvent.Handler()
            {
               @Override
               public void onPlumberAPIStatus(PlumberAPIStatusEvent event)
               {
                  // If the document appears to be inside the directory
                  // associated with the event, update the view to match the
                  // new state.
                  if (getPath() != null &&
                      getPath().startsWith(event.getParams().getPath()))
                  {
                     String state = event.getParams().getState();
                     if (event.getParams().getViewerType() !=
                            UserPrefs.PLUMBER_VIEWER_TYPE_PANE &&
                         event.getParams().getViewerType() !=
                            UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW)
                     {
                        // we can't control the state when it's not in an
                        // RStudio-owned window, so treat the app as stopped
                        state = PlumberAPIParams.STATE_STOPPED;
                     }
                     view_.onPlumberAPIStateChanged(state);
                  }
               }
            });

      events_.addHandler(
            this,
            BreakpointsSavedEvent.TYPE,
            new BreakpointsSavedEvent.Handler()
      {
         @Override
         public void onBreakpointsSaved(BreakpointsSavedEvent event)
         {
            // if this document isn't ready for breakpoints, stop now
            if (docUpdateSentinel_ == null)
            {
               return;
            }
            for (Breakpoint breakpoint: event.breakpoints())
            {
               // discard the breakpoint if it's not related to the file this
               // editor instance is concerned with
               if (!breakpoint.isInFile(getPath()))
               {
                  continue;
               }

               // if the breakpoint was saved successfully, enable it on the
               // editor surface; otherwise, just remove it.
               if (event.successful())
               {
                  docDisplay_.addOrUpdateBreakpoint(breakpoint);
               }
               else
               {
                  // Show a warning for breakpoints that didn't get set (unless
                  // the reason the breakpoint wasn't set was that it's being
                  // removed)
                  if (breakpoint.getState() != Breakpoint.STATE_REMOVING)
                  {
                     view_.showWarningBar(constants_.onBreakpointsSavedWarningBar());
                  }
                  docDisplay_.removeBreakpoint(breakpoint);
               }
            }
            updateBreakpointWarningBar();
         }
      });

      events_.addHandler(
            this,
            ConvertToShinyDocEvent.TYPE,
            new ConvertToShinyDocEvent.Handler()
      {
         @Override
         public void onConvertToShinyDoc(ConvertToShinyDocEvent event)
         {
            if (getPath() != null &&
                getPath().equals(event.getPath()))
            {
               String yaml = getRmdFrontMatter();
               if (yaml == null)
                  return;
               String newYaml = rmarkdownHelper_.convertYamlToShinyDoc(yaml);
               applyRmdFrontMatter(newYaml);
               renderRmd();
            }
         }
      });

      events_.addHandler(
            this,
            RSConnectDeployInitiatedEvent.TYPE,
            new RSConnectDeployInitiatedEvent.Handler()
            {
               @Override
               public void onRSConnectDeployInitiated(
                     RSConnectDeployInitiatedEvent event)
               {
                  // no need to process this event if this target doesn't have a
                  // path, or if the event's contents don't include additional
                  // files.
                  if (getPath() == null)
                     return;

                  // see if the event corresponds to a deployment of this file
                  if (!getPath().equals(event.getSource().getSourceFile()))
                     return;

                  RSConnectPublishSettings settings = event.getSettings();
                  if (settings == null)
                     return;

                  // ignore deployments of static content generated from this
                  // file
                  if (settings.getAsStatic())
                     return;

                  if (settings.getAdditionalFiles() != null &&
                      settings.getAdditionalFiles().size() > 0)
                  {
                     addAdditionalResourceFiles(settings.getAdditionalFiles());
                  }
               }
            });

      events_.addHandler(
            this,
            SetEditorCommandBindingsEvent.TYPE,
            new SetEditorCommandBindingsEvent.Handler()
            {
               @Override
               public void onSetEditorCommandBindings(SetEditorCommandBindingsEvent event)
               {
                  getDocDisplay().setEditorCommandBinding(
                        event.getId(),
                        event.getKeySequences());
               }
            });

      events_.addHandler(
            this,
            ResetEditorCommandsEvent.TYPE,
            new ResetEditorCommandsEvent.Handler()
            {
               @Override
               public void onResetEditorCommands(ResetEditorCommandsEvent event)
               {
                  getDocDisplay().resetCommands();
               }
            });

      events_.addHandler(
            this,
            DocTabDragStateChangedEvent.TYPE,
            new DocTabDragStateChangedEvent.Handler()
            {

               @Override
               public void onDocTabDragStateChanged(
                     DocTabDragStateChangedEvent e)
               {
                  // enable text drag/drop only while we're not dragging tabs
                  boolean enabled = e.getState() ==
                        DocTabDragStateChangedEvent.STATE_NONE;

                  // make editor read only while we're dragging and dropping
                  // tabs; otherwise the editor surface will accept a tab drop
                  // as text
                  docDisplay_.setReadOnly(!enabled);
               }
            });

      events_.addHandler(
            this,
            AceAfterCommandExecutedEvent.TYPE,
            new AceAfterCommandExecutedEvent.Handler()
            {
               @Override
               public void onAceAfterCommandExecuted(AceAfterCommandExecutedEvent event)
               {
                  JavaScriptObject data = event.getCommandData();
                  if (isIncrementalSearchCommand(data))
                  {
                     String message = getIncrementalSearchMessage();
                     if (StringUtil.isNullOrEmpty(message))

                     {
                        view_.getStatusBar().hideMessage();
                     }
                     else
                     {
                        view_.getStatusBar().showMessage(
                              getIncrementalSearchMessage(),
                              2000);
                     }
                  }
               }
            });
      
      events_.addHandler(
            this,
            CopilotEvent.TYPE,
            new CopilotEvent.Handler()
            {
               @Override
               public void onCopilot(CopilotEvent event)
               {
                  switch (event.getType())
                  {
                  
                  case COPILOT_DISABLED:
                     view_.getStatusBar().hideStatus();
                     break;
                     
                  case COMPLETION_REQUESTED:
                     view_.getStatusBar().showStatus(
                           StatusBarIconType.TYPE_INFO,
                           constants_.copilotWaiting());
                     break;
                     
                  case COMPLETION_CANCELLED:
                     view_.getStatusBar().showStatus(
                           StatusBarIconType.TYPE_INFO,
                           constants_.copilotNoCompletions());
                     break;
                     
                  case COMPLETION_RECEIVED_SOME:
                     view_.getStatusBar().showStatus(
                           StatusBarIconType.TYPE_OK,
                           constants_.copilotResponseReceived());
                     break;
                     
                  case COMPLETION_RECEIVED_NONE:
                     view_.getStatusBar().showStatus(
                           StatusBarIconType.TYPE_INFO,
                           constants_.copilotNoCompletions());
                     break;
                     
                  case COMPLETION_ERROR:
                     String message = (String) event.getData();
                     view_.getStatusBar().showStatus(
                           StatusBarIconType.TYPE_ERROR,
                           constants_.copilotResponseErrorMessage(message));
                     break;
                     
                  case COMPLETIONS_ENABLED:
                     view_.getStatusBar().showStatus(
                           StatusBarIconType.TYPE_INFO,
                           constants_.copilotEnabled());
                     break;
                     
                  case COMPLETIONS_DISABLED:
                     view_.getStatusBar().showStatus(
                           StatusBarIconType.TYPE_INFO,
                           constants_.copilotDisabled());
                     break;
                     
                  }
               }
            });
      
      packageDependencyHelper_ = new TextEditingTargetPackageDependencyHelper(this, docUpdateSentinel_, docDisplay_);

      // create notebook and forward resize events
      chunks_ = new TextEditingTargetChunks(this);
      notebook_ = new TextEditingTargetNotebook(this, chunks_, view_,
            docDisplay_, dirtyState_, docUpdateSentinel_, document,
            releaseOnDismiss_, dependencyManager_);
      view_.addResizeHandler(notebook_);

      // apply project properties
      projConfig_ = document.getProjectConfig();
      if (projConfig_ != null)
      {
         docDisplay_.setUseSoftTabs(projConfig_.useSoftTabs());
         docDisplay_.setTabSize(projConfig_.getTabSize());
      }

      // ensure that Makefile and Makevars always use tabs
      name_.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            view_.setAccessibleName(name_.getValue());
            FileSystemItem item = FileSystemItem.createFile(event.getValue());
            if (shouldEnforceHardTabs(item))
               docDisplay_.setUseSoftTabs(false);
         }
      });

      String name = getNameFromDocument(document, defaultNameProvider);
      name_.setValue(name, true);
      String contents = document.getContents();

      // disable change detection when setting code (since we're just doing
      // this to ensure the document's state reflects the server state and so
      // these aren't changes that diverge the document's client state from
      // the server state)
      docUpdateSentinel_.withChangeDetectionSuspended(() ->
      {
         docDisplay_.setCode(contents, false);
      });

      // Discover dependencies on file first open.
      packageDependencyHelper_.discoverPackageDependencies();

      // Load and apply folds.
      final ArrayList<Fold> folds = Fold.decode(document.getFoldSpec());
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            // disable change detection when adding folds (since we're just doing
            // this to ensure the document's state reflects the server state and so
            // these aren't changes that diverge the document's client state from
            // the server state)
            docUpdateSentinel_.withChangeDetectionSuspended(() ->
            {
               for (Fold fold : folds)
                  docDisplay_.addFold(fold.getRange());
            });
         }
      });

      // Load and apply Vim marks (if they exist).
      if (document.getProperties().hasKey("marks"))
      {
         final String marksSpec = document.getProperties().getString("marks");
         final JsMap<Position> marks = VimMarks.decode(marksSpec);

         // Time out the marks setting just to avoid conflict with other
         // mutations of the editor.
         new Timer()
         {
            @Override
            public void run()
            {
                docDisplay_.setMarks(marks);
            }
         }.schedule(100);
      }

      TextEditingTargetPrefsHelper.registerPrefs(
            releaseOnDismiss_, prefs_, projConfig_, docDisplay_, document);

      // Initialize sourceOnSave, and keep it in sync. Don't source on save
      // (regardless of preference) in auto save mode, which is mutually
      // exclusive with the manual source-and-save workflow.
      boolean sourceOnSave = document.sourceOnSave();
      if (prefs_.autoSaveEnabled())
         sourceOnSave = false;
      view_.getSourceOnSave().setValue(sourceOnSave, false);
      view_.getSourceOnSave().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            docUpdateSentinel_.setSourceOnSave(
                  event.getValue(),
                  globalDisplay_.getProgressIndicator(constants_.errorSavingSetting()));
         }
      });

      if (document.isDirty())
         dirtyState_.markDirty(false);
      else
         dirtyState_.markClean();
      docDisplay_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         public void onValueChange(ValueChangeEvent<Void> event)
         {
            dirtyState_.markDirty(true);
            docDisplay_.clearSelectionHistory();

            // Nudge autosave timer (so it doesn't fire while the document is
            // actively mutating)
            nudgeAutosave();
         }
      });

      docDisplay_.addFocusHandler(new FocusHandler()
      {
         public void onFocus(FocusEvent event)
         {
            // let anyone listening know this doc just got focus
            events_.fireEvent(new DocFocusedEvent(getPath(), getId()));

            if (queuedCollabParams_ != null)
            {
               // join an in-progress collab session if we aren't already part
               // of one
               if (docDisplay_ != null && !docDisplay_.hasActiveCollabSession())
               {
                  beginQueuedCollabSession();
               }
            }

            // check to see if the file's been saved externally--we do this even
            // in a collaborative editing session so we can get delete
            // notifications
            checkForExternalEdit(500);

            // relint on blur
            lintManager_.relintAfterDelay(100);
         }
      });


      if (fileType_.isR())
      {
         docDisplay_.addBreakpointSetHandler(new BreakpointSetEvent.Handler()
         {
            @Override
            public void onBreakpointSet(BreakpointSetEvent event)
            {
               if (event.isSet())
               {
                  Breakpoint breakpoint = null;

                  // don't set breakpoints in Plumber documents
                  if (SourceDocument.isPlumberFile(extendedType_))
                  {
                     view_.showWarningBar(constants_.onBreakpointSetPlumberfileWarning());
                     return;
                  }

                  // don't try to set breakpoints in unsaved code
                  if (isNewDoc())
                  {
                     view_.showWarningBar(constants_.onBreakpointSetNewDocWarning());
                     return;
                  }

                  Position breakpointPosition =
                        Position.create(event.getLineNumber() - 1, 1);

                  // if we're not in function scope, or this is a Shiny file,
                  // set a top-level (aka. Shiny-deferred) breakpoint
                  ScopeFunction innerFunction = null;
                  if (extendedType_ == null ||
                      !extendedType_.startsWith(SourceDocument.XT_SHINY_PREFIX))
                     innerFunction = docDisplay_.getFunctionAtPosition(
                           breakpointPosition, false);
                  if (innerFunction == null || !innerFunction.isFunction() ||
                      StringUtil.isNullOrEmpty(innerFunction.getFunctionName()))
                  {
                     breakpoint = breakpointManager_.setTopLevelBreakpoint(
                           getPath(),
                           event.getLineNumber());
                  }

                  // the scope tree will find nested functions, but in R these
                  // are addressable only as substeps of the parent function.
                  // keep walking up the scope tree until we've reached the top
                  // level function.
                  else
                  {
                     while (innerFunction.getParentScope() != null &&
                            innerFunction.getParentScope().isFunction())
                     {
                        innerFunction = (ScopeFunction) innerFunction.getParentScope();
                     }

                     String functionName = innerFunction.getFunctionName();

                     breakpoint = breakpointManager_.setBreakpoint(
                           getPath(),
                           functionName,
                           event.getLineNumber(),
                           dirtyState().getValue() == false);
                  }

                  docDisplay_.addOrUpdateBreakpoint(breakpoint);
               }
               else
               {
                  breakpointManager_.removeBreakpoint(event.getBreakpointId());
               }
               updateBreakpointWarningBar();
            }
         });

         docDisplay_.addBreakpointMoveHandler(new BreakpointMoveEvent.Handler()
         {
            @Override
            public void onBreakpointMove(BreakpointMoveEvent event)
            {
               breakpointManager_.moveBreakpoint(event.getBreakpointId());
            }
         });
      }

      // validate required components (e.g. Tex, knitr, C++ etc.)
      checkCompilePdfDependencies();
      rmarkdownHelper_.verifyPrerequisites(view_, fileType_);

      syncFontSize(releaseOnDismiss_, events_, view_, fontSizeManager_);


      releaseOnDismiss_.add(prefs_.softWrapRFiles().addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               public void onValueChange(ValueChangeEvent<Boolean> evt)
               {
                  view_.adaptToFileType(fileType_);
               }
            }
      ));

      spelling_ = new TextEditingTargetSpelling(docDisplay_, docUpdateSentinel_, lintManager_, prefs_);

      // find all of the debug breakpoints set in this document and replay them
      // onto the edit surface
      ArrayList<Breakpoint> breakpoints =
            breakpointManager_.getBreakpointsInFile(getPath());
      for (Breakpoint breakpoint: breakpoints)
      {
         docDisplay_.addOrUpdateBreakpoint(breakpoint);
      }

      view_.addRmdFormatChangedHandler(new RmdOutputFormatChangedEvent.Handler()
      {
         @Override
         public void onRmdOutputFormatChanged(RmdOutputFormatChangedEvent event)
         {
            if (event.isQuarto())
            {
               setQuartoFormat(event.getFormat());
            }
            else
            {
               setRmdFormat(event.getFormat());
            }
         }
      });

      docDisplay_.addCursorChangedHandler(new CursorChangedEvent.Handler()
      {
         final Timer timer_ = new Timer()
         {
            @Override
            public void run()
            {
               HashMap<String, String> properties = new HashMap<>();

               properties.put(
                     PROPERTY_CURSOR_POSITION,
                     Position.serialize(docDisplay_.getCursorPosition()));

               properties.put(
                     PROPERTY_SCROLL_LINE,
                     String.valueOf(docDisplay_.getFirstFullyVisibleRow()));

               docUpdateSentinel_.modifyProperties(properties);
            }
         };

         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            if (prefs_.restoreSourceDocumentCursorPosition().getValue())
               timer_.schedule(1000);
         }
      });


      // initialize visual mode
      visualMode_ = new VisualMode(
         TextEditingTarget.this,
         view_,
         rmarkdownHelper_,
         docDisplay_,
         dirtyState_,
         docUpdateSentinel_,
         events_,
         fileTypeRegistry_,
         releaseOnDismiss_
      );

      // populate the popup menu with a list of available formats
      if (extendedType_.startsWith(SourceDocument.XT_RMARKDOWN_PREFIX) ||
          extendedType_.equals(SourceDocument.XT_QUARTO_DOCUMENT))
      {
         updateRmdFormat();
         setRMarkdownBehaviorEnabled(true);
      }
     

      // provide find replace button to view
      view_.addVisualModeFindReplaceButton(visualMode_.getFindReplaceButton());

      // update status bar when visual mode status changes
      releaseOnDismiss_.add(
         docUpdateSentinel_.addPropertyValueChangeHandler(RMD_VISUAL_MODE, (value) -> {
            updateStatusBarLanguage();
         })
      );

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            if (!prefs_.restoreSourceDocumentCursorPosition().getValue())
               return;

            String cursorPosition = docUpdateSentinel_.getProperty(
                  PROPERTY_CURSOR_POSITION,
                  "");

            if (StringUtil.isNullOrEmpty(cursorPosition))
               return;


            int scrollLine = StringUtil.parseInt(
                  docUpdateSentinel_.getProperty(PROPERTY_SCROLL_LINE, "0"),
                  0);

            Position position = Position.deserialize(cursorPosition);
            docDisplay_.setCursorPosition(position);
            docDisplay_.scrollToLine(scrollLine, false);
            docDisplay_.setScrollLeft(0);
         }
      });

      syncPublishPath(document.getPath());
      initStatusBar();
      lintManager_.relintAfterDelay(prefs_.documentLoadLintDelay().getValue());
   }

   private void updateBreakpointWarningBar()
   {
      // check to see if there are any inactive breakpoints in this file
      boolean hasInactiveBreakpoints = false;
      boolean hasDebugPendingBreakpoints = false;
      boolean hasPackagePendingBreakpoints = false;
      String pendingPackageName = "";
      ArrayList<Breakpoint> breakpoints =
            breakpointManager_.getBreakpointsInFile(getPath());
      for (Breakpoint breakpoint: breakpoints)
      {
         if (breakpoint.getState() == Breakpoint.STATE_INACTIVE)
         {
            if (breakpoint.isPendingDebugCompletion())
            {
               hasDebugPendingBreakpoints = true;
            }
            else if (breakpoint.isPackageBreakpoint())
            {
               hasPackagePendingBreakpoints = true;
               pendingPackageName = breakpoint.getPackageName();
            }
            else
            {
               hasInactiveBreakpoints = true;
            }
            break;
         }
      }
      boolean showWarning = hasDebugPendingBreakpoints ||
                            hasInactiveBreakpoints ||
                            hasPackagePendingBreakpoints;

      if (showWarning && !isBreakpointWarningVisible_)
      {
         String message = "";
         if (hasDebugPendingBreakpoints)
         {
            message = constants_.updateBreakpointWarningBarFunctionMessage();
         }
         else if (isPackageFile())
         {
            message = constants_.updateBreakpointWarningBarPackageLoadMessage();
         }
         else if (hasPackagePendingBreakpoints)
         {
            message = constants_.updateBreakpointWarningBarPackageMessage(pendingPackageName);
         }
         else
         {
            message = constants_.updateBreakpointWarningBarSourcedMessage();
         }
         view_.showWarningBar(message);
         isBreakpointWarningVisible_ = true;
      }
      else if (!showWarning && isBreakpointWarningVisible_)
      {
         hideBreakpointWarningBar();
      }
   }

   private void hideBreakpointWarningBar()
   {
      if (isBreakpointWarningVisible_)
      {
         view_.hideWarningBar();
         isDebugWarningVisible_ = false;
         isBreakpointWarningVisible_ = false;
      }
   }

   private boolean isPackageFile()
   {
      // not a package file if we're not in package development mode
      String type = session_.getSessionInfo().getBuildToolsType();
      if (!type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         return false;
      }

      // get the directory associated with the project and see if the file is
      // inside that directory
      FileSystemItem projectDir = session_.getSessionInfo()
            .getActiveProjectDir();
      return getPath().startsWith(projectDir.getPath() + "/R");
   }

   private boolean isPackageDocumentationFile()
   {
      if (getPath() == null)
      {
         return false;
      }

      String type = session_.getSessionInfo().getBuildToolsType();
      if (!type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         return false;
      }

      FileSystemItem srcFile = FileSystemItem.createFile(getPath());
      FileSystemItem projectDir = session_.getSessionInfo()
            .getActiveProjectDir();
      if (srcFile.getPath().startsWith(projectDir.getPath() + "/vignettes"))
         return true;
      else if (srcFile.getParentPathString().equals(projectDir.getPath()) &&
               srcFile.getExtension().toLowerCase().equals(".md"))
         return true;
      else
         return false;
   }

   private void checkCompilePdfDependencies()
   {
      compilePdfHelper_.checkCompilers(view_, fileType_);
   }

   private void initStatusBar()
   {
      statusBar_ = view_.getStatusBar();
      docDisplay_.addCursorChangedHandler(new CursorChangedEvent.Handler()
      {
         public void onCursorChanged(CursorChangedEvent event)
         {
            updateStatusBarPosition();
            if (docDisplay_.isScopeTreeReady(event.getPosition().getRow()))
               updateCurrentScope();

         }
      });
      updateStatusBarPosition();
      updateStatusBarLanguage();

      // build file type menu dynamically (so it can change according
      // to whether e.g. knitr is installed)
      statusBar_.getLanguage().addMouseDownHandler(new MouseDownHandler() {

         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            // build menu with all file types - also track whether we need
            // to add the current type (may be the case for types which we
            // support but don't want to expose on the menu -- e.g. Rmd
            // files when knitr isn't installed)
            boolean addCurrentType = true;
            final StatusBarPopupMenu menu = new StatusBarPopupMenu();
            List<TextFileType> fileTypes = fileTypeCommands_.statusBarFileTypes();
            for (TextFileType type : fileTypes)
            {
               menu.addItem(createMenuItemForType(type));
               if (addCurrentType && type.equals(fileType_))
                  addCurrentType = false;
            }

            // add the current type if isn't on the menu
            if (addCurrentType)
               menu.addItem(createMenuItemForType(fileType_));

            // show the menu
            menu.showRelativeToUpward((UIObject) statusBar_.getLanguage(),
                  true);
         }
      });

      statusBar_.getScope().addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            // Unlike the other status bar elements, the function outliner
            // needs its menu built on demand
            if (fileType_.isRpres())
            {
               String path = docUpdateSentinel_.getPath();
               if (path != null)
               {
                  presentationHelper_.buildSlideMenu(
                     docUpdateSentinel_.getPath(),
                     dirtyState_.getValue(),
                     TextEditingTarget.this,
                     new CommandWithArg<StatusBarPopupRequest>() {

                        @Override
                        public void execute(StatusBarPopupRequest request)
                        {
                           showStatusBarPopupMenu(request);
                        }
                     });
               }
            }
            else if (isVisualEditorActive())
            {
               showStatusBarPopupMenu(visualMode_.getStatusBarPopup());
            }
            else
            {
               final StatusBarPopupMenu menu = new StatusBarPopupMenu();
               JsArray<Scope> tree = docDisplay_.getScopeTree();
               MenuItem defaultItem = addFunctionsToMenu(
                  menu, tree, "", docDisplay_.getCurrentScope(), true);

               showStatusBarPopupMenu(new StatusBarPopupRequest(menu,
                                                                defaultItem));
            }
         }
      });
   }

   private void showStatusBarPopupMenu(StatusBarPopupRequest popupRequest)
   {
      final StatusBarPopupMenu menu = popupRequest.getMenu();
      MenuItem defaultItem = popupRequest.getDefaultMenuItem();
      if (defaultItem != null)
      {
         menu.selectItem(defaultItem);
         Scheduler.get().scheduleFinally(new RepeatingCommand()
         {
            public boolean execute()
            {
               menu.ensureSelectedIsVisible();
               return false;
            }
         });
      }
      menu.showRelativeToUpward((UIObject) statusBar_.getScope(), false);
   }

   private MenuItem createMenuItemForType(final TextFileType type)
   {
      SafeHtmlBuilder labelBuilder = new SafeHtmlBuilder();
      labelBuilder.appendEscaped(type.getLabel());

      MenuItem menuItem = new MenuItem(
         labelBuilder.toSafeHtml(),
         new Command()
         {
            public void execute()
            {
               ensureTextEditorActive(() -> {
                  docUpdateSentinel_.changeFileType(
                        type.getTypeId(),
                        new SaveProgressIndicator(null, type, false,null));

                  Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                     @Override
                     public void execute()
                     {
                        focus();
                     }
                  });
               });
            }
         });

      return menuItem;
   }

   private void addScopeStyle(MenuItem item, Scope scope)
   {
      if (scope.isSection())
         item.getElement().getStyle().setFontWeight(FontWeight.BOLD);
   }

   private MenuItem addFunctionsToMenu(StatusBarPopupMenu menu,
                                       final JsArray<Scope> funcs,
                                       String indent,
                                       Scope defaultFunction,
                                       boolean includeNoFunctionsMessage)
   {
      MenuItem defaultMenuItem = null;

      if (funcs.length() == 0 && includeNoFunctionsMessage)
      {
         String type = fileType_.canExecuteChunks() ? constants_.chunks() : constants_.functions();
         MenuItem noFunctions = new MenuItem(constants_.addFunctionsToMenuText(type),
                                             false,
                                             (Command) null);
         noFunctions.setEnabled(false);
         noFunctions.getElement().addClassName("disabled");
         menu.addItem(noFunctions);
      }

      for (int i = 0; i < funcs.length(); i++)
      {
         final Scope func = funcs.get(i);

         String childIndent = indent;
         if (!StringUtil.isNullOrEmpty(func.getLabel()))
         {
            SafeHtmlBuilder labelBuilder = new SafeHtmlBuilder();
            labelBuilder.appendHtmlConstant(indent);
            labelBuilder.appendEscaped(func.getLabel());

            final MenuItem menuItem = new MenuItem(
                  labelBuilder.toSafeHtml(),
                  new Command()
                  {
                     public void execute()
                     {
                        docDisplay_.navigateToPosition(toSourcePosition(func),
                                                       true);
                     }
                  });
            addScopeStyle(menuItem, func);
            menu.addItem(menuItem);

            childIndent = indent + "&nbsp;&nbsp;";

            if (defaultFunction != null && defaultMenuItem == null &&
                func.getLabel() == defaultFunction.getLabel() &&
                func.getPreamble().getRow() == defaultFunction.getPreamble().getRow() &&
                func.getPreamble().getColumn() == defaultFunction.getPreamble().getColumn())
            {
               defaultMenuItem = menuItem;
            }
         }

         MenuItem childDefaultMenuItem = addFunctionsToMenu(
               menu,
               func.getChildren(),
               childIndent,
               defaultMenuItem == null ? defaultFunction : null,
               false);
         if (childDefaultMenuItem != null)
            defaultMenuItem = childDefaultMenuItem;
      }

      return defaultMenuItem;
   }

   private void updateStatusBarLanguage()
   {
      statusBar_.getLanguage().setValue(fileType_.getLabel());
      boolean canShowScope = fileType_.canShowScopeTree();
      statusBar_.setScopeVisible(canShowScope);
   }

   private void updateStatusBarPosition()
   {
      Position pos = docDisplay_.getCursorPosition();
      statusBar_.getPosition().setValue((pos.getRow() + 1) + ":" +
                                        (pos.getColumn() + 1));
   }

   public void updateStatusBarLocation(String title, int type)
   {
      statusBar_.setScopeType(type);
      statusBar_.getScope().setValue(title);
   }

   private void updateCurrentScope()
   {
      // don't sync scope if we can't show a scope tree or in visual mode (which
      // is responsible for updating the scope visualization itself)
      if (fileType_ == null || !fileType_.canShowScopeTree() || isVisualModeActivated())
         return;

      // special handing for presentations since we extract
      // the slide structure in a different manner than
      // the editor scope trees
      if (fileType_.isRpres())
      {
         statusBar_.getScope().setValue(
               presentationHelper_.getCurrentSlide());
         statusBar_.setScopeType(StatusBar.SCOPE_SLIDE);

      }
      else
      {
         Scope scope = docDisplay_.getCurrentScope();
         String label = scope != null
               ? scope.getLabel()
                     : null;
               statusBar_.getScope().setValue(label);

               if (scope != null)
               {
                  boolean useChunk =
                        scope.isChunk() ||
                        (fileType_.isRnw() && scope.isTopLevel());
                  if (useChunk)
                     statusBar_.setScopeType(StatusBar.SCOPE_CHUNK);
                  else if (scope.isNamespace())
                     statusBar_.setScopeType(StatusBar.SCOPE_NAMESPACE);
                  else if (scope.isClass())
                     statusBar_.setScopeType(StatusBar.SCOPE_CLASS);
                  else if (scope.isSection())
                     statusBar_.setScopeType(StatusBar.SCOPE_SECTION);
                  else if (scope.isTopLevel())
                     statusBar_.setScopeType(StatusBar.SCOPE_TOP_LEVEL);
                  else if (scope.isTest())
                     statusBar_.setScopeType(StatusBar.SCOPE_TEST);
                  else if (scope.isFunction())
                     statusBar_.setScopeType(StatusBar.SCOPE_FUNCTION);
                  else if (scope.isLambda())
                     statusBar_.setScopeType(StatusBar.SCOPE_LAMBDA);
                  else if (scope.isAnon())
                     statusBar_.setScopeType(StatusBar.SCOPE_ANON);
               }
      }
   }

   private String getNameFromDocument(SourceDocument document,
                                      EditingTargetNameProvider defaultNameProvider)
   {
      if (document.getPath() != null)
         return FileSystemItem.getNameFromPath(document.getPath());

      String name = document.getProperties().getString("tempName");
      if (!StringUtil.isNullOrEmpty(name))
         return name;

      String defaultName = defaultNameProvider.defaultNamePrefix(this);
      docUpdateSentinel_.setProperty("tempName", defaultName, null);
      return defaultName;
   }

   public long getFileSizeLimit()
   {
      return 5 * 1024 * 1024;
   }

   public long getLargeFileSize()
   {
      return 2 * 1024 * 1024;
   }

   public int getPixelWidth()
   {
      if (isVisualEditorActive())
      {
         return visualMode_.getPixelWidth();
      }
      else
      {
         return docDisplay_.getPixelWidth();
      }
   }

   public void insertCode(String source, boolean blockMode)
   {
      docDisplay_.insertCode(source, blockMode);
   }

   public HashSet<AppCommand> getSupportedCommands()
   {
      // start with the set of commands supported by the file type
      HashSet<AppCommand> commands = fileType_.getSupportedCommands(commands_);

      // if the file has a path, it can also be renamed
      if (getPath() != null)
      {
         commands.add(commands_.renameSourceDoc());
      }
      
      return commands;
   }

   @Override
   public void manageCommands()
   {
      if (fileType_.isRmd())
         notebook_.manageCommands();

      if (fileType_.isMarkdown())
      {
         visualMode_.manageCommands();
         quartoHelper_.manageCommands();
      }
      
   }
   
   @Override
   public CommandPaletteEntryProvider getPaletteEntryProvider()
   {
      if (visualMode_.isActivated())
      {
         return visualMode_.getPaletteEntryProvider();
      }
      else
         return null;
   }

   @Override
   public boolean canCompilePdf()
   {
      return fileType_.canCompilePDF();
   }

   public boolean canExecuteChunks()
   {
      return fileType_.canExecuteChunks();
   }


   @Override
   public void verifyCppPrerequisites()
   {
      // NOTE: will be a no-op for non-c/c++ file types
      cppHelper_.checkBuildCppDependencies(this, view_, fileType_);
   }

   @Override
   public void verifyPythonPrerequisites()
   {
      // TODO: ensure 'reticulate' installed
   }

   @Override
   public void verifyD3Prerequisites()
   {
      verifyD3Prequisites(null);
   }

   private void verifyD3Prequisites(final Command command)
   {
      dependencyManager_.withR2D3("Previewing D3 scripts", new Command() {
         @Override
         public void execute() {
            if (command != null)
               command.execute();
         }
      });
   }

   @Override
   public void verifyNewSqlPrerequisites()
   {
      verifyNewSqlPrerequisites(null);
   }

   private void verifyNewSqlPrerequisites(final Command command)
   {
      dependencyManager_.withRSQLite("Previewing SQL scripts", new Command() {
         @Override
         public void execute() {
            if (command != null)
               command.execute();
         }
      });
   }

   private void verifySqlPrerequisites(final Command command)
   {
      dependencyManager_.withDBI("Previewing SQL scripts", new Command() {
         @Override
         public void execute() {
            if (command != null)
               command.execute();
         }
      });
   }

   public void focus()
   {
      if (isVisualModeActivated())
      {
         visualMode_.focus(() ->
         {
            // Initialize notebook after activation if present (and notebook is
            // uninitialized)
            if (notebook_ != null &&
                notebook_.getState() == TextEditingTargetNotebook.STATE_NONE)
            {
               notebook_.onRenderFinished(null);
            }
         });
      }
      else
      {
         view_.editorContainer().focus();
      }
   }

   public void replaceSelection(String value, Command callback)
   {
      if (isVisualModeActivated())
      {
         ensureVisualModeActive(() ->
         {
            visualMode_.replaceSelection(value);
            callback.execute();
         });
      }
      else
      {
         ensureTextEditorActive(() ->
         {
            if (docDisplay_.hasSelection())
            {
               docDisplay_.replaceSelection(value);
            }
            else
            {
               docDisplay_.insertCode(value);
            }

            callback.execute();
         });
      }
   }

   public String getSelectedText()
   {
      if (docDisplay_.hasSelection())
         return docDisplay_.getSelectionValue();
      else
         return "";
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return view_.addEnsureVisibleHandler(handler);
   }

   public HandlerRegistration addEnsureHeightHandler(EnsureHeightEvent.Handler handler)
   {
      return view_.addEnsureHeightHandler(handler);
   }

   public HandlerRegistration addCloseHandler(CloseHandler<java.lang.Void> handler)
   {
      return handlers_.addHandler(CloseEvent.getType(), handler);
   }

   public HandlerRegistration addEditorThemeStyleChangedHandler(
                        EditorThemeStyleChangedEvent.Handler handler)
   {
      return themeHelper_.addEditorThemeStyleChangedHandler(handler);
   }

   public HandlerRegistration addInterruptChunkHandler(InterruptChunkEvent.Handler handler)
   {
      return handlers_.addHandler(InterruptChunkEvent.TYPE, handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public boolean isActivated()
   {
      return commandHandlerReg_ != null;
   }

   public void onActivate()
   {
      // IMPORTANT NOTE: most of this logic is duplicated in
      // CodeBrowserEditingTarget (no straightforward way to create a
      // re-usable implementation) so changes here need to be synced

      // If we're already hooked up for some reason, unhook.
      // This shouldn't happen though.
      if (commandHandlerReg_ != null)
      {
         Debug.log("Warning: onActivate called twice without intervening onDeactivate");
         commandHandlerReg_.removeHandler();
         commandHandlerReg_ = null;
      }
      commandHandlerReg_ = commandBinder.bind(commands_, this);

      // show outline if not yet rendered (deferred so that widget itself can
      // be sized first)
      if (!docDisplay_.isRendered())
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               view_.initWidgetSize();
            }
         });
      }

      Scheduler.get().scheduleFinally(new ScheduledCommand()
      {
         public void execute()
         {
            // This has to be executed in a scheduleFinally because
            // Source.manageCommands gets called after this.onActivate,
            // and if we're going from a non-editor (like data view) to
            // an editor, setEnabled(true) will be called on the command
            // in manageCommands.
            commands_.reopenSourceDocWithEncoding().setEnabled(
                  docUpdateSentinel_.getPath() != null);
         }
      });

      // notify notebook of activation if necessary
      if (notebook_ != null)
         notebook_.onActivate();

      view_.onActivate();
   }

   public void onDeactivate()
   {
      // IMPORTANT NOTE: most of this logic is duplicated in
      // CodeBrowserEditingTarget (no straightforward way to create a
      // re-usable implementation) so changes here need to be synced

      externalEditCheckInvalidation_.invalidate();

      commandHandlerReg_.removeHandler();
      commandHandlerReg_ = null;

      // switching tabs is a navigation action
      try
      {
         recordCurrentNavigationPosition();
      }
      catch(Exception e)
      {
         Debug.log("Exception recording nav position: " + e.toString());
      }

      visualMode_.unmanageCommands();
   }

   @Override
   public void onInitiallyLoaded()
   {
      checkForExternalEdit();
   }

   public boolean onBeforeDismiss()
   {
      final Command closeCommand = new Command()
      {
         public void execute()
         {
            // notify visual mode
            visualMode_.onClosing();

            // fire close event
            CloseEvent.fire(TextEditingTarget.this, null);
         }
      };


      final Command promptCommand = new Command()
      {
         public void execute()
         {
            if (dirtyState_.getValue())
               saveWithPrompt(closeCommand, null);
            else
               closeCommand.execute();
         }
      };

      if (docDisplay_.hasFollowingCollabSession())
      {
         globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_WARNING,
                         constants_.onBeforeDismissCaption(getName().getValue()),
                         constants_.onBeforeDismissMessage(getName().getValue()),
                         false,
                         new Operation()
                         {
                            public void execute()
                            {
                               promptCommand.execute();
                            }
                         },
                         null,
                         null,
                         constants_.closeAnyway(),
                         constants_.cancel(),
                         false);
      }
      else
      {
         promptCommand.execute();
      }

      return false;
   }

   public void save()
   {
      if (isSaving_)
         return;

      save(() -> {});
   }

   private void autoSave(Command onCompleted, Command onSilentFailure)
   {
      saveThenExecute(null, false, CommandUtil.join(postSaveCommand(false), onCompleted), onSilentFailure);
   }

   public void save(Command onCompleted)
   {
      saveThenExecute(null, true, CommandUtil.join(postSaveCommand(true), onCompleted));
   }

   public void saveWithPrompt(final Command command, final Command onCancelled)
   {
      view_.ensureVisible();

      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_WARNING,
                      constants_.saveWithPromptCaption(getName().getValue()),
                      constants_.saveWithPromptMessage(getName().getValue()),
                      true,
                      new Operation() {
                         public void execute() { saveThenExecute(null, true, command); }
                      },
                      new Operation() {
                         public void execute() { command.execute(); }
                      },
                      new Operation() {
                         public void execute() {
                            if (onCancelled != null)
                              onCancelled.execute();
                         }
                      },
                      constants_.save(),
                      constants_.dontSave(),
                      true);
   }

   public void revertChanges(Command onCompleted)
   {
      docUpdateSentinel_.revert(onCompleted, ignoreDeletes_);
   }

   public void saveThenExecute(String encodingOverride, boolean retryWrite, final Command command)
   {
      saveThenExecute(encodingOverride, retryWrite, command, null);
   }

   public void saveThenExecute(String encodingOverride, boolean retryWrite, final Command command, final Command onSilentFailure)
   {
      isSaving_ = true;

      checkCompilePdfDependencies();

      final String path = docUpdateSentinel_.getPath();
      if (path == null)
      {
         saveNewFile(null, encodingOverride, command);
         return;
      }

      withEncodingRequiredUnlessAscii(
            encodingOverride,
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  fixupCodeBeforeSaving(() -> {
                     docUpdateSentinel_.save(path,
                           null,
                           encoding,
                           retryWrite,
                           new SaveProgressIndicator(
                                 FileSystemItem.createFile(path),
                                 null,
                                 !retryWrite,
                                 command,
                                 onSilentFailure
                           ));
                  });
               }
            });
   }

   private void saveNewFile(final String suggestedPath,
                            String encodingOverride,
                            final Command executeOnSuccess)
   {
      isSaving_ = true;
      withEncodingRequiredUnlessAscii(
            encodingOverride,
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  saveNewFileWithEncoding(suggestedPath,
                                          encoding,
                                          executeOnSuccess);
               }
            });
   }

   private void withEncodingRequiredUnlessAscii(
         final String encodingOverride,
         final CommandWithArg<String> command)
   {
      String preferredDocumentEncoding = null;
      if (docDisplay_.getFileType().isRmd())
         preferredDocumentEncoding = "UTF-8";

      final String encoding = StringUtil.firstNotNullOrEmpty(new String[] {
            encodingOverride,
            docUpdateSentinel_.getEncoding(),
            preferredDocumentEncoding,
            prefs_.defaultEncoding().getValue()
      });

      if (StringUtil.isNullOrEmpty(encoding))
      {
         if (docUpdateSentinel_.isAscii())
         {
            // Don't bother asking when it's just ASCII
            command.execute(null);
         }
         else
         {
            withChooseEncoding(session_.getSessionInfo().getSystemEncoding(),
                               new CommandWithArg<String>()
            {
               public void execute(String newEncoding)
               {
                  command.execute(newEncoding);
               }
            });
         }
      }
      else
      {
         command.execute(encoding);
      }
   }

   private void withChooseEncoding(final String defaultEncoding,
                                   final CommandWithArg<String> command)
   {
      view_.ensureVisible();

      server_.iconvlist(new SimpleRequestCallback<IconvListResult>()
      {
         @Override
         public void onResponseReceived(IconvListResult response)
         {
            // Stupid compiler. Use this Value shim to make the dialog available
            // in its own handler.
            final HasValue<ChooseEncodingDialog> d = new Value<>(null);
            d.setValue(new ChooseEncodingDialog(
                  response.getCommon(),
                  response.getAll(),
                  defaultEncoding,
                  false,
                  true,
                  new OperationWithInput<String>()
                  {
                     public void execute(String newEncoding)
                     {
                        if (newEncoding == null)
                           return;

                        if (d.getValue().isSaveAsDefault())
                        {
                           prefs_.defaultEncoding().setGlobalValue(newEncoding);
                           prefs_.writeUserPrefs();
                        }

                        command.execute(newEncoding);
                     }
                  },
                  new Operation()
                  {
                     public void execute()
                     {
                        isSaving_ = false;
                     }
                  }));
            d.getValue().showModal();
         }
      });
   }

   private void saveNewFileWithEncoding(String suggestedPath,
                                        final String encoding,
                                        final Command executeOnSuccess)
   {
      isSaving_ = true;
      view_.ensureVisible();

      FileSystemItem fsi;
      if (suggestedPath != null)
         fsi = FileSystemItem.createFile(suggestedPath);
      else
         fsi = getSaveFileDefaultDir();

      fileDialogs_.saveFile(
            constants_.saveNewFileWithEncodingSaveFileCaption(getName().getValue()),
            fileContext_,
            fsi,
            fileType_.getDefaultExtension(),
            false,
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(final FileSystemItem saveItem,
                                   ProgressIndicator indicator)
               {
                  // null here implies the user cancelled the save
                  if (saveItem == null)
                  {
                     isSaving_ = false;
                     return;
                  }

                  try
                  {
                     workbenchContext_.setDefaultFileDialogDir(
                           saveItem.getParentPath());

                     final TextFileType fileType =
                           fileTypeRegistry_.getTextTypeForFile(saveItem);

                     final Command saveCommand = new Command() {
                        @Override
                        public void execute()
                        {
                           if (getPath() != null &&
                               !getPath().equals(saveItem.getPath()))
                           {
                              // breakpoints are file-specific, so when saving
                              // as a different file, clear the display of
                              // breakpoints from the old file name
                              docDisplay_.removeAllBreakpoints();

                              // update publish settings
                              syncPublishPath(saveItem.getPath());
                           }

                           fixupCodeBeforeSaving(() -> {
                              docUpdateSentinel_.save(
                                    saveItem.getPath(),
                                    fileType.getTypeId(),
                                    encoding,
                                    true,
                                    new SaveProgressIndicator(saveItem,
                                                              fileType,
                                                              false,
                                                              executeOnSuccess));

                              events_.fireEvent(
                                    new SourceFileSavedEvent(getId(),
                                          saveItem.getPath()));
                           });


                        }

                     };

                     // if we are switching from an R file type
                     // to a non-R file type then confirm
                     if (fileType_.isR() && !fileType.isR())
                     {
                        globalDisplay_.showYesNoMessage(
                              MessageDialog.WARNING,
                              constants_.saveNewFileWithEncodingWarningCaption(),
                              constants_.saveNewFileWithEncodingWarningMessage(),
                              new Operation() {

                                 @Override
                                 public void execute()
                                 {
                                    saveCommand.execute();
                                 }
                              },
                              false);
                     }
                     else
                     {
                        saveCommand.execute();
                     }
                  }
                  catch (Exception e)
                  {
                     indicator.onError(e.toString());
                     return;
                  }

                  indicator.onCompleted();
               }
            });
   }


   private void fixupCodeBeforeSaving(Command ready)
   {
      int lineCount = docDisplay_.getRowCount();
      if (lineCount < 1)
      {
         ready.execute();
         return;
      }

      if (docDisplay_.hasActiveCollabSession())
      {
         // mutating the code (especially as below where the entire document
         // contents are changed) during a save operation inside a collaborative
         // editing session would require some nuanced orchestration so for now
         // these preferences don't apply to shared editing sessions
         // note that visual editing is currently disabled for collab sessions
         // so none of the visual editing code below would apply
         ready.execute();
         return;
      }


      // apply visual mode fixups then continue w/ standard fixups
      applyVisualModeFixups(() -> {

         boolean stripTrailingWhitespace = (projConfig_ == null)
               ? prefs_.stripTrailingWhitespace().getValue()
               : projConfig_.stripTrailingWhitespace();

         // override preference for certain files
         boolean dontStripWhitespace =
               fileType_.isMarkdown() ||
               fileType_.isPython() ||
               name_.getValue().equals("DESCRIPTION");

         if (dontStripWhitespace)
         {
            stripTrailingWhitespace = false;
         }

         if (stripTrailingWhitespace)
         {
            String code = docDisplay_.getCode();
            String strippedCode = "";
            
            // If this is being performed as part of an autosave, avoid
            // mutating lines containing the cursor.
            if (isAutoSaving())
            {
               Pattern pattern = Pattern.create("[ \t]+$", "");
               int startRow = docDisplay_.getSelectionStart().getRow();
               int endRow = docDisplay_.getSelectionEnd().getRow();
               
               JsArrayString lines = docDisplay_.getLines();
               for (int i = 0, n = lines.length(); i < n; i++)
               {
                  if (i < startRow || i > endRow)
                  {
                     String line = lines.get(i);
                     lines.set(i, pattern.replaceAll(line, ""));
                  }
               }
               strippedCode = lines.join("\n");
            }
            else
            {
               Pattern pattern = Pattern.create("[ \t]+$", "gm");
               strippedCode = pattern.replaceAll(code, "");
            }
            
            if (!strippedCode.equals(code))
            {
               // Calling 'setCode' can remove folds in the document; cache the folds
               // and reapply them after document mutation.
               JsArray<AceFold> folds = docDisplay_.getFolds();
               docDisplay_.setCode(strippedCode, true);
               for (AceFold fold : JsUtil.asIterable(folds))
                  docDisplay_.addFold(fold.getRange());
            }
         }

         boolean autoAppendNewline = (projConfig_ == null)
               ? prefs_.autoAppendNewline().getValue()
               : projConfig_.ensureTrailingNewline();

         // auto-append newlines for commonly-used R startup files
         String path = StringUtil.notNull(docUpdateSentinel_.getPath());
         boolean isStartupFile =
               path.endsWith("/.Rprofile") ||
               path.endsWith("/.Rprofile.site") ||
               path.endsWith("/.Renviron") ||
               path.endsWith("/.Renviron.site");

         if (autoAppendNewline || isStartupFile || fileType_.isPython())
         {
            String lastLine = docDisplay_.getLine(lineCount - 1);
            if (lastLine.length() != 0)
            {
               docDisplay_.insertCode(docDisplay_.getEnd().getEnd(), "\n");
            }
         }

         // callback
         ready.execute();
      });
   }

   private void applyVisualModeFixups(Command onComplete)
   {
      // only do this for markdown files
      if (fileType_.isMarkdown())
      {
         boolean canonical = false;
         
         // start with quarto project level pref if its in play
         boolean foundQuartoCanonical = false;
         QuartoConfig quarto = session_.getSessionInfo().getQuartoConfig();
         boolean isQuartoDoc = QuartoHelper.isWithinQuartoProjectDir(docUpdateSentinel_.getPath(), quarto);
         if (isQuartoDoc)
         {
            if (quarto.project_editor != null && quarto.project_editor.markdown != null && quarto.project_editor.markdown.canonical != null)
            {
               canonical = Boolean.parseBoolean(quarto.project_editor.markdown.canonical);
               foundQuartoCanonical = true;
            }
         }
         
         if (!foundQuartoCanonical)
         {
            // check canonical pref
            canonical = prefs_.visualMarkdownEditingCanonical().getValue();
            
            // if we are cannonical but the global value isn't canonical then make sure this
            // file is in the current project
            if (canonical && !prefs_.visualMarkdownEditingCanonical().getGlobalValue())
            {
               canonical = VisualModeUtil.isDocInProject(workbenchContext_, docUpdateSentinel_);
            }
         }
        
         
         // check for a file based canonical setting
         String yaml = YamlFrontMatter.getFrontMatter(docDisplay_);
         String yamlCanonical = RmdEditorOptions.getMarkdownOption(yaml,  "canonical");
         if (!yamlCanonical.isEmpty())
            canonical = YamlTree.isTrue(yamlCanonical);

         // if visual mode is active then we need to grab its edits before proceeding
         if (visualMode_.isActivated())
         {
            visualMode_.syncToEditor(SyncType.SyncTypeNormal, onComplete);
         }

         // if visual mode is not active and we are doing canonical saves
         // then we need to apply any changes implied by canonical transformation
         // of our source
         else if (canonical && visualMode_.canWriteCanonical())
         {
            String code = docDisplay_.getCode();
            visualMode_.getCanonicalChanges(code, (changes) -> {
               // null changes means an error occurred (user has already been shown an alert)
               if (changes != null)
               {
                  if (changes.changes != null)
                     docDisplay_.applyChanges(changes.changes, true);
                  else if (changes.code != null)
                     docDisplay_.setCode(changes.code, true);
               }
               // need to continue in order to not permanetly break save
               // (user has seen an error message so will still likely report)
               onComplete.execute();
            });
         }

         // otherwise nothing to do
         else
         {
            onComplete.execute();
         }
      }
      // not a markdown file
      else
      {
         onComplete.execute();
      }
   }

   // When the editor loses focus, perform an autosave if enabled, the
   // buffer is dirty, and we have a file to save to
   private void maybeAutoSaveOnBlur()
   {
      boolean canAutosave =
            !isSaving_ &&
            ModalDialogTracker.numModalsShowing() == 0 &&
            prefs_.autoSaveOnBlur().getValue() &&
            getPath() != null &&
            !docDisplay_.hasActiveCollabSession();
            
      if (!canAutosave)
         return;
      
      try
      {
         save();
      }
      catch(Exception e)
      {
         // Autosave exceptions are logged rather than displayed
         Debug.logException(e);
      }
   }


   private FileSystemItem getSaveFileDefaultDir()
   {
      FileSystemItem fsi = null;
      SessionInfo si = session_.getSessionInfo();

      if (si.getBuildToolsType() == SessionInfo.BUILD_TOOLS_PACKAGE)
      {
         FileSystemItem pkg = FileSystemItem.createDir(si.getBuildTargetDir());

         if (fileType_.isR())
         {
            fsi = FileSystemItem.createDir(pkg.completePath("R"));
         }
         else if (fileType_.isC() && si.getHasPackageSrcDir())
         {
            fsi = FileSystemItem.createDir(pkg.completePath("src"));
         }
         else if (fileType_.isRd())
         {
            fsi = FileSystemItem.createDir(pkg.completePath("man"));
         }
         else if ((fileType_.isRnw() || fileType_.isRmd()) &&
                   si.getHasPackageVignetteDir())
         {
            fsi = FileSystemItem.createDir(pkg.completePath("vignettes"));
         }
      }

      if (fsi == null)
         fsi = workbenchContext_.getDefaultFileDialogDir();

      return fsi;
   }

   @Override
   public void onDismiss(int dismissType)
   {
      isClosing_ = true;

      docUpdateSentinel_.stop();

      if (spelling_ != null)
         spelling_.onDismiss();

      if (visualMode_ != null)
         visualMode_.onDismiss();

      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();

      docDisplay_.endCollabSession();

      codeExecution_.detachLastExecuted();

      if (notebook_ != null)
         notebook_.onDismiss();

      if (inlinePreviewer_ != null)
         inlinePreviewer_.onDismiss();
      
      if (autoSaveTimer_ != null)
         autoSaveTimer_.cancel();
      
      if (bgIdleMonitor_ != null)
         bgIdleMonitor_.endMonitoring();
   }

   public ReadOnlyValue<Boolean> dirtyState()
   {
      return dirtyState_;
   }

   @Override
   public boolean isSaveCommandActive()
   {
      return
         // force active?
         forceSaveCommandActive_ ||

         // standard check of dirty state
         (dirtyState().getValue() == true) ||

         // empty untitled document (allow for immediate save)
         ((getPath() == null) && docDisplay_.getCode().isEmpty()) ||

         // source on save is active
         (isSourceOnSaveEnabled() && docUpdateSentinel_.sourceOnSave());
   }


   @Override
   public void forceSaveCommandActive()
   {
      forceSaveCommandActive_ = true;
   }

   public Widget asWidget()
   {
      return (Widget) view_;
   }

   public String getId()
   {
      return id_;
   }

   @Override
   public void adaptToExtendedFileType(String extendedType)
   {
      // extended type can affect publish options; we need to sync here even if the type
      // hasn't changed as the path may have changed
      syncPublishPath(docUpdateSentinel_.getPath());

      // if autosaves are enabled and the extended type hasn't changed, then
      // don't do any further work as adapting to the extended type can cause
      // disruptive side effects during autosave (e.g., knocking down
      // autocomplete dialogs, resetting vim mode)
      if (StringUtil.equals(extendedType, extendedType_) &&
          prefs_.autoSaveEnabled())
      {
         return;
      }

      view_.adaptToExtendedFileType(extendedType);

      // save new extended type (updateRmdFormat below reads it)
      extendedType_ = extendedType;

      if (extendedType.startsWith(SourceDocument.XT_RMARKDOWN_PREFIX) ||
          extendedType.equals(SourceDocument.XT_QUARTO_DOCUMENT))
      {
         updateRmdFormat();
      }

      quartoHelper_.manageCommands();
   }

   @Override
   public String getExtendedFileType()
   {
      return extendedType_;
   }
   
   @Override
   public boolean isShinyPrerenderedDoc()
   {
      try
      {
         String yaml = getRmdFrontMatter();
         if (yaml == null)
            return false;
         return rmarkdownHelper_.isRuntimeShinyPrerendered(yaml);
      }
      catch(Exception e)
      {
         Debug.log(e.getMessage());
         return false;
      }
   }
   public HasValue<String> getName()
   {
      return name_;
   }

   public String getTitle()
   {
      return getName().getValue();
   }

   public String getPath()
   {
      if (docUpdateSentinel_ == null)
         return null;
      return docUpdateSentinel_.getPath();
   }

   public String getContext()
   {
      return null;
   }

   public FileIcon getIcon()
   {
      return fileType_.getDefaultFileIcon();
   }

   public String getTabTooltip()
   {
      return getPath();
   }

   @Override
   public FileType getFileType()
   {
      return fileType_;
   }

   @Override
   public TextFileType getTextFileType()
   {
      return fileType_;
   }

   @Handler
   void onToggleDocumentOutline()
   {
     view_.toggleDocumentOutline();
   }

   @Handler
   void onToggleRmdVisualMode()
   {
      recordCurrentNavigationPosition();
      view_.toggleRmdVisualMode();
   }

   @Handler
   void onToggleSoftWrapMode()
   {
      view_.toggleSoftWrapMode();
   }

   @Handler
   void onToggleRainbowParens()
   {
      view_.toggleRainbowParens();
   }

   @Handler
   void onToggleRainbowFencedDivs()
   {
      view_.toggleRainbowFencedDivs();
   }

   @Handler
   void onEnableProsemirrorDevTools()
   {
      visualMode_.activateDevTools();
   }
   
   private void withReformatDependencies(Command command)
   {
      String formatter = prefs_.codeFormatter().getValue();
      if (StringUtil.equals(formatter, UserPrefsAccessor.CODE_FORMATTER_STYLER))
      {
         dependencyManager_.withStyler(command);
      }
      else
      {
         command.execute();
      }
   }
   
   @Handler
   void onReformatDocument()
   {
      withActiveEditor((editor) ->
      {
         String formatType = prefs_.codeFormatter().getValue();
         if (StringUtil.equals(formatType, UserPrefsAccessor.CODE_FORMATTER_NONE))
         {
            Range currentRange = editor.getSelectionRange();
            editor.setSelectionRange(Range.fromPoints(
                  Position.create(0, 0),
                  Position.create(editor.getCurrentLineCount() + 1, 0)));
            new TextEditingTargetReformatHelper(editor).insertPrettyNewlines();
            editor.setSelectionRange(currentRange);
         }
         else
         {
            withReformatDependencies(() ->
            {
               withSavedDoc(() ->
               {
                  server_.formatDocument(
                        docUpdateSentinel_.getId(),
                        docUpdateSentinel_.getPath(),
                        new ServerRequestCallback<SourceDocument>()
                        {
                           @Override
                           public void onResponseReceived(SourceDocument document)
                           {
                              revertEdits();
                           }

                           @Override
                           public void onError(ServerError error)
                           {
                              Debug.logError(error);
                           }
                        });
               });
            });
         }
      });
   }

   @Handler
   void onReformatCode()
   {
      withActiveEditor((editor) ->
      {
         // Only allow if entire selection in R mode for now
         if (!DocumentMode.isSelectionInRMode(editor))
         {
            showRModeWarning(commands_.reformatCode().getLabel());
            return;
         }

         String formatType = prefs_.codeFormatter().getValue();
         if (StringUtil.equals(formatType, UserPrefsAccessor.CODE_FORMATTER_NONE))
         {
            new TextEditingTargetReformatHelper(editor).insertPrettyNewlines();
         }
         else
         {
            withReformatDependencies(() ->
            {
               Range range = editor.getSelectionRange();
               if (range.getStart().getRow() != range.getEnd().getRow())
               {
                  range.getStart().setColumn(0);
                  range.getEnd().setColumn(Integer.MAX_VALUE);
               }
               
               String selection = editor.getTextForRange(range);
               server_.formatCode(selection, new ServerRequestCallback<String>()
               {
                  @Override
                  public void onResponseReceived(String response)
                  {
                     editor.replaceRange(range, response);
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                  }
               });
            });
         }
      });
   }

   @Handler
   void onRenameInScope()
   {
      withActiveEditor((disp) ->
      {
         renameInScope(disp);
      });
   }

   void renameInScope(DocDisplay display)
   {
      display.focus();

      // Save folds (we need to remove them temporarily for the rename helper)
      final JsArray<AceFold> folds = display.getFolds();
      display.unfoldAll();

      int matches = (new TextEditingTargetRenameHelper(display)).renameInScope();
      if (matches <= 0)
      {
         if (!display.getSelectionValue().isEmpty())
         {
            String message = constants_.renameInScopeNoMatchesMessage(display.getSelectionValue());
            view_.getStatusBar().showMessage(message, 1000);
         }

         for (AceFold fold : JsUtil.asIterable(folds))
            display.addFold(fold.getRange());
         return;
      }

      String message = constants_.renameInScopeFoundMatchesMessage(matches);
      if (matches == 1)
         message += constants_.renameInScopeMatch(); // add space here
      else
         message += constants_.renameInScopeMatchesPlural();

      String selectedItem = display.getSelectionValue();
      message += constants_.renameInScopeSelectedItemMessage(selectedItem);

      display.disableSearchHighlight();
      view_.getStatusBar().showMessage(message, new HideMessageHandler()
      {
         private boolean onRenameFinished(boolean value)
         {
            for (AceFold fold : JsUtil.asIterable(folds))
               display.addFold(fold.getRange());
            return value;
         }

         @Override
         public boolean onNativePreviewEvent(NativePreviewEvent preview)
         {
            int type = preview.getTypeInt();
            if (display.isPopupVisible())
               return false;

            // End if the user clicks somewhere
            if (type == Event.ONCLICK)
            {
               display.exitMultiSelectMode();
               display.clearSelection();
               display.enableSearchHighlight();
               return onRenameFinished(true);
            }

            // Otherwise, handle key events
            else if (type == Event.ONKEYDOWN)
            {
               switch (preview.getNativeEvent().getKeyCode())
               {
               case KeyCodes.KEY_ENTER:
                  preview.cancel();
               case KeyCodes.KEY_UP:
               case KeyCodes.KEY_DOWN:
               case KeyCodes.KEY_ESCAPE:
                  display.exitMultiSelectMode();
                  display.clearSelection();
                  display.enableSearchHighlight();
                  return onRenameFinished(true);
               }
            }

            return false;
         }
      });
   }

   @Handler
   void onInsertRoxygenSkeleton()
   {
      withActiveEditor((disp) ->
      {
         new RoxygenHelper(disp, view_).insertRoxygenSkeleton();
      });
   }

   @Handler
   void onExpandSelection()
   {
      withActiveEditor((disp) ->
      {
         disp.expandSelection();
      });
   }

   @Handler
   void onShrinkSelection()
   {
      withActiveEditor((disp) ->
      {
         disp.shrinkSelection();
      });
   }

   @Handler
   void onExpandRaggedSelection()
   {
      withActiveEditor((disp) ->
      {
         disp.expandRaggedSelection();
      });
   }

   @Handler
   void onShowDiagnosticsActiveDocument()
   {
      lintManager_.lint(true, true, false);
   }

   public void withSavedDoc(Command onsaved)
   {
      docUpdateSentinel_.withSavedDoc(onsaved);
   }

   public void withSavedDocNoRetry(Command onsaved)
   {
      docUpdateSentinel_.withSavedDocNoRetry(onsaved);
   }

   @Handler
   void onWordCount()
   {
      prepareForVisualExecution(() ->
      {
         int totalWords = 0;
         int selectionWords = 0;

         Range selectionRange = null;

         // A selection in visual mode may span multiple editors and blocks of
         // prose, which we can't count here.
         if (!isVisualEditorActive())
         {
            selectionRange = docDisplay_.getSelectionRange();
         }

         TextFileType fileType = docDisplay_.getFileType();
         Iterator<Range> wordIter = docDisplay_.getWords(
            fileType.getTokenPredicate(),
            docDisplay_.getFileType().getCharPredicate(),
            Position.create(0, 0),
            null).iterator();

         while (wordIter.hasNext())
         {
            Range r = wordIter.next();
            totalWords++;
            if (selectionRange != null && selectionRange.intersects(r))
               selectionWords++;
         }

         String selectedWordsText = selectionWords == 0 ? "" : constants_.selectedWords(selectionWords);
         globalDisplay_.showMessage(MessageDisplay.MSG_INFO,
            constants_.wordCount(),
            constants_.onWordCountMessage(totalWords, selectedWordsText));
      });
   }

   @Handler
   void onCheckSpelling()
   {
      if (visualMode_.isActivated())
      {
         ensureVisualModeActive(() -> {
            visualMode_.checkSpelling();
         });
      }
      else
      {
         ensureTextEditorActive(() -> {
            spelling_.checkSpelling(docDisplay_.getSpellingDoc());
         });
      }


   }

   @Handler
   void onDebugDumpContents()
   {
      view_.debug_dumpContents();
   }

   @Handler
   void onDebugImportDump()
   {
      view_.debug_importDump();
   }

   @Handler
   void onReopenSourceDocWithEncoding()
   {
      final Command action = () -> {
         withChooseEncoding(
               docUpdateSentinel_.getEncoding(),
               (String encoding) -> docUpdateSentinel_.reopenWithEncoding(encoding));
      };

      // NOTE: we previously attempted to save any existing document diffs
      // and then re-opened the document with the requested encoding, but
      // this is a perilous action to take as if the user has opened a document
      // without specifying the correct encoding, the representation of the document
      // in the front-end might be corrupt / incorrect and so attempting to save
      // a document diff could further corrupt the document!
      //
      // Since the most common user workflow here should be:
      //
      //    1. Open a document,
      //    2. Discover the document was not opened with the correct encoding,
      //    3. Attempt to re-open with a separate encoding
      //
      // it's most likely that they do not want to persist any changes made to the
      // "incorrect" version of the document and instead want to discard any
      // changes and re-open the document as it exists on disk.
      if (dirtyState_.getValue())
      {
         String caption = constants_.onReopenSourceDocWithEncodingCaption();

         String message = constants_.onReopenSourceDocWithEncodingMessage();

         globalDisplay_.showYesNoMessage(
               GlobalDisplay.MSG_WARNING,
               caption,
               message,
               true,
               () -> action.execute(),
               () -> {},
               () -> {},
               constants_.reopenDocument(),
               constants_.cancel(),
               true);
      }
      else
      {
         action.execute();
      }
   }

   @Handler
   void onSaveSourceDoc()
   {
      if (isSaving_)
         return;

      saveThenExecute(null, true, postSaveCommand(true));
   }

   @Handler
   void onSaveSourceDocAs()
   {
      saveNewFile(docUpdateSentinel_.getPath(),
                  null,
                  postSaveCommand(true));
   }

   @Handler
   void onRenameSourceDoc()
   {
      events_.fireEvent(new RenameSourceFileEvent(docUpdateSentinel_.getPath()));
   }

   @Handler
   void onCopySourceDocPath()
   {
      events_.fireEvent(new CopySourcePathEvent(docUpdateSentinel_.getPath()));
   }

   @Handler
   void onSaveSourceDocWithEncoding()
   {
      withChooseEncoding(
            StringUtil.firstNotNullOrEmpty(new String[] {
                  docUpdateSentinel_.getEncoding(),
                  prefs_.defaultEncoding().getValue(),
                  session_.getSessionInfo().getSystemEncoding()
            }),
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  saveThenExecute(encoding, true, postSaveCommand(true));
               }
            });
   }

   @Handler
   void onPrintSourceDoc()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            docDisplay_.print();
         }
      });
   }

   @Handler
   void onVcsFileDiff()
   {
      Command showDiffCommand = new Command() {
         @Override
         public void execute()
         {
            events_.fireEvent(new ShowVcsDiffEvent(
                  FileSystemItem.createFile(docUpdateSentinel_.getPath())));
         }
      };

      if (dirtyState_.getValue())
         saveWithPrompt(showDiffCommand, null);
      else
         showDiffCommand.execute();
   }

   @Handler
   void onVcsFileLog()
   {
      events_.fireEvent(new ShowVcsHistoryEvent(
               FileSystemItem.createFile(docUpdateSentinel_.getPath())));
   }

   @Handler
   void onVcsFileRevert()
   {
      events_.fireEvent(new VcsRevertFileEvent(
            FileSystemItem.createFile(docUpdateSentinel_.getPath())));
   }

   @Handler
   void onVcsViewOnGitHub()
   {
      fireVcsViewOnGithubEvent(GitHubViewRequest.VCS_VIEW);
   }

   @Handler
   void onVcsBlameOnGitHub()
   {
      fireVcsViewOnGithubEvent(GitHubViewRequest.VCS_BLAME);
   }

   private void fireVcsViewOnGithubEvent(int type)
   {
      FileSystemItem file =
                  FileSystemItem.createFile(docUpdateSentinel_.getPath());

      if (docDisplay_.getSelectionValue().length() > 0)
      {
         int start = docDisplay_.getSelectionStart().getRow() + 1;
         int end = docDisplay_.getSelectionEnd().getRow() + 1;
         events_.fireEvent(new VcsViewOnGitHubEvent(
                         new GitHubViewRequest(file, type, start, end)));
      }
      else
      {
         events_.fireEvent(new VcsViewOnGitHubEvent(
                         new GitHubViewRequest(file, type)));
      }
   }

   @Handler
   void onExtractLocalVariable()
   {
      withActiveEditor((disp) ->
      {
         extractLocalVariable(disp);
      });
   }

   void extractLocalVariable(DocDisplay display)
   {
      if (!isCursorInRMode(display))
      {
         showRModeWarning("Extract Variable");
         return;
      }

      display.focus();

      String initialSelection = display.getSelectionValue();
      final String refactoringName = constants_.extractLocalVariableRefactoringName();
      final String pleaseSelectCodeMessage = constants_.pleaseSelectCodeMessage();
      if (checkSelectionAndAlert(refactoringName,
                                 pleaseSelectCodeMessage,
                                 initialSelection)) return;

      display.fitSelectionToLines(false);

      final String code = display.getSelectionValue();
      if (checkSelectionAndAlert(refactoringName,
                                 pleaseSelectCodeMessage,
                                 code))
         return;

      // get the first line of the selection and calculate it's indentation
      String firstLine = display.getLine(
                        display.getSelectionStart().getRow());
      final String indentation = extractIndentation(firstLine);

      // used to parse the code
      server_.detectFreeVars(code,
           new RefactorServerRequestCallback(refactoringName)
           {
              @Override
              void doExtract(JsArrayString response)
              {
                 globalDisplay_.promptForText(
                         refactoringName,
                         constants_.variableName(),
                         "",
                         new OperationWithInput<String>()
                         {
                            public void execute(String input)
                            {
                               final String extractedCode = indentation
                                                            + input.trim()
                                                            + " <- "
                                                            + code
                                                            + "\n";
                               InputEditorPosition insertPosition = display
                                       .getSelection()
                                       .extendToLineStart()
                                       .getStart();
                               display.replaceSelection(
                                       input.trim());
                               display.insertCode(
                                       insertPosition,
                                       extractedCode);
                            }
                         }
                 );
              }
           }
      );
   }

   private void showRModeWarning(String command)
   {
      globalDisplay_.showMessage(MessageDisplay.MSG_WARNING,
                                 constants_.showRModeWarningCaption(),
                                 constants_.showRModeWarningMessage(command));
   }


   @Handler
   void onExtractFunction()
   {
      withActiveEditor((disp) ->
      {
         extractActiveFunction(disp);
      });
   }

   void extractActiveFunction(DocDisplay display)
   {
      if (!isCursorInRMode(display))
      {
         showRModeWarning("Extract Function");
         return;
      }

      display.focus();

      String initialSelection = display.getSelectionValue();
      final String refactoringName = constants_.extractActiveFunctionRefactoringName();
      final String pleaseSelectCodeMessage = constants_.pleaseSelectCodeMessage();
      if (checkSelectionAndAlert(refactoringName,
                                 pleaseSelectCodeMessage,
                                 initialSelection)) return;

      display.fitSelectionToLines(false);

      final String code = display.getSelectionValue();
      if (checkSelectionAndAlert(refactoringName,
                                 pleaseSelectCodeMessage,
                                 code)) return;

      final String indentation = extractIndentation(code);
      server_.detectFreeVars(code,
           new RefactorServerRequestCallback(refactoringName)
           {
              @Override
              void doExtract(final JsArrayString response)
              {
                 globalDisplay_.promptForText(
                   refactoringName,
                   constants_.functionNameLabel(),
                   "",
                   new OperationWithInput<String>()
                   {
                      public void execute(String input)
                      {
                         String prefix;
                         if (display.getSelectionOffset(true) == 0)
                            prefix = "";
                         else prefix = "\n";
                         String args = response != null ? response.join(", ")
                                                        : "";
                         display.replaceSelection(
                                 prefix
                                 + indentation
                                 + input.trim()
                                 + " <- "
                                 + "function(" + args + ") {\n"
                                 + StringUtil.indent(code, "  ")
                                 + "\n"
                                 + indentation
                                 + "}");
                      }
                   }
                 );
              }
            }
      );
   }
   
   
   private boolean isSourceOnSaveEnabled()
   {
      return fileType_.canSourceOnSave() || StringUtil.equals(extendedType_, SourceDocument.XT_QUARTO_DOCUMENT);
   }

   private boolean checkSelectionAndAlert(String refactoringName,
                                          String pleaseSelectCodeMessage,
                                          String selection)
   {
      if (isSelectionValueEmpty(selection))
      {
         globalDisplay_.showErrorMessage(refactoringName,
                                         pleaseSelectCodeMessage);
         return true;
      }
      return false;
   }

   private String extractIndentation(String code)
   {
      Pattern leadingWhitespace = Pattern.create("^(\\s*)");
      Match match = leadingWhitespace.match(code, 0);
      return match == null ? "" : match.getGroup(1);
   }

   private boolean isSelectionValueEmpty(String selection)
   {
      return selection == null || selection.trim().length() == 0;
   }

   @Handler
   void onCommentUncomment()
   {
      withActiveEditor((disp) ->
      {
         commentUncomment(disp);
      });
   }

   void commentUncomment(DocDisplay display)
   {
      if (isCursorInTexMode(display))
         doCommentUncomment(display, "%", null);
      else if (isCursorInRMode(display) || isCursorInYamlMode(display))
         doCommentUncomment(display, "#", null);
      else if (fileType_.isCpp() || fileType_.isStan() || fileType_.isC())
         doCommentUncomment(display, "//", null);
      else if (fileType_.isPlainMarkdown())
         doCommentUncomment(display, "<!--", "-->");
      else if (DocumentMode.isSelectionInMarkdownMode(display))
         doCommentUncomment(display, "<!--", "-->");
      else if (DocumentMode.isSelectionInPythonMode(display))
         doCommentUncomment(display, "#", null);
   }

   /**
    * Push the current contents and state of the text editor into the local
    * copy of the source database
    */
   public void syncLocalSourceDb()
   {
      SourceWindowManager manager =
            RStudioGinjector.INSTANCE.getSourceWindowManager();
      JsArray<SourceDocument> docs = manager.getSourceDocs();
      for (int i = 0; i < docs.length(); i++)
      {
         if (docs.get(i).getId() == getId())
         {
            docs.get(i).getNotebookDoc().setChunkDefs(
                  docDisplay_.getChunkDefs());
            docs.get(i).setContents(docDisplay_.getCode());
            docs.get(i).setDirty(dirtyState_.getValue());
            break;
         }
      }
   }

   @Handler
   void onPopoutDoc()
   {
      if (docUpdateSentinel_ != null)
      {
         // ensure doc is synchronized with source database before popping it
         // out
         docUpdateSentinel_.withSavedDoc(new Command()
         {
            @Override
            public void execute()
            {
               // push the new doc state into the local source database
               syncLocalSourceDb();

               // fire popout event (this triggers a close in the current window
               // and the creation of a new window with the doc)
               events_.fireEvent(new PopoutDocEvent(getId(),
                     currentPosition(), null));
            }
         });
      }
   }


   @Handler
   void onReturnDocToMain()
   {
      // ensure doc is synchronized with source database before returning it
      if (!SourceWindowManager.isMainSourceWindow() &&
          docUpdateSentinel_ != null)
      {
         docUpdateSentinel_.withSavedDoc(new Command()
         {
            @Override
            public void execute()
            {
               events_.fireEventToMainWindow(new DocWindowChangedEvent(
                  getId(), SourceWindowManager.getSourceWindowId(), "",
                  DocTabDragParams.create(getId(), currentPosition()),
                  docUpdateSentinel_.getDoc().getCollabParams(), 0, -1));
            }
         });
      }
   }

   @Handler
   public void onNotebookCollapseAllOutput()
   {
      if (notebook_ != null)
         notebook_.onNotebookCollapseAllOutput();
   }

   @Handler
   public void onNotebookExpandAllOutput()
   {
      if (notebook_ != null)
         notebook_.onNotebookExpandAllOutput();
   }

   @Handler
   public void onNotebookClearOutput()
   {
      if (notebook_ != null)
         notebook_.onNotebookClearOutput();
   }

   @Handler
   public void onNotebookClearAllOutput()
   {
      if (notebook_ != null)
         notebook_.onNotebookClearAllOutput();
   }

   @Handler
   public void onNotebookToggleExpansion()
   {
      if (notebook_ != null)
         notebook_.onNotebookToggleExpansion();
   }

   @Handler
   public void onRestartRRunAllChunks()
   {
      if (notebook_ != null)
         notebook_.onRestartRRunAllChunks();
   }

   @Handler
   public void onRestartRClearOutput()
   {
      if (notebook_ != null)
         notebook_.onRestartRClearOutput();
   }

   @SuppressWarnings("deprecation") // GWT emulation only provides isSpace
   private void doCommentUncomment(DocDisplay display,
                                   String commentStart,
                                   String commentEnd)
   {
      Range initialRange = display.getSelectionRange();

      int rowStart = initialRange.getStart().getRow();
      int rowEnd = initialRange.getEnd().getRow();

      boolean isSingleLineAction = rowStart == rowEnd;
      boolean commentWhitespace = commentEnd == null;

      // Also figure out if we're commenting an Roxygen block.
      boolean looksLikeRoxygen = false;

      // Skip commenting the last line if the selection is
      // multiline and ends on the first column of the end row.
      boolean dontCommentLastLine = false;
      if (rowStart != rowEnd && initialRange.getEnd().getColumn() == 0)
         dontCommentLastLine = true;

      Range expanded = Range.create(
            rowStart,
            0,
            rowEnd,
            dontCommentLastLine ? 0 : display.getLine(rowEnd).length());
      display.setSelectionRange(expanded);

      String[] lines = JsUtil.toStringArray(
            display.getLines(rowStart, rowEnd - (dontCommentLastLine ? 1 : 0)));

      String commonPrefix = StringUtil.getCommonPrefix(
            lines,
            true,
            true);

      String commonIndent = StringUtil.getIndent(commonPrefix);

      // First, figure out whether we're commenting or uncommenting.
      // If we discover any line that doesn't start with the comment sequence,
      // then we'll comment the whole selection.

      // ignore empty lines at start, end of selection when detecting comments
      // https://github.com/rstudio/rstudio/issues/4163

      int start = 0;
      for (int i = 0; i < lines.length; i++)
      {
         if (lines[i].trim().isEmpty())
            continue;

         start = i;
         break;
      }

      int end = lines.length;
      for (int i = lines.length; i > 0; i--)
      {
         if (lines[i - 1].trim().isEmpty())
            continue;

         end = i;
         break;
      }

      boolean isCommentAction = false;
      for (int i = start; i < end; i++)
      {
         String line = lines[i];
         String trimmed = line.trim();

         // Ignore lines that are just whitespace.
         if (!commentWhitespace && trimmed.isEmpty())
            continue;

         if (!isCommentAction)
         {
            if (!trimmed.startsWith(commentStart))
               isCommentAction = true;
         }

         if (display.getFileType().isR())
         {
            if (!looksLikeRoxygen)
            {
               if (trimmed.startsWith("@"))
                  looksLikeRoxygen = true;
               else if (trimmed.startsWith("#'"))
                  looksLikeRoxygen = true;
            }
         }
      }

      if (looksLikeRoxygen)
         commentStart += "'";

      // Now, construct a new, commented selection to replace with.
      StringBuilder builder = new StringBuilder();
      if (isCommentAction)
      {
         for (String line : lines)
         {
            String trimmed = line.trim();

            if (!commentWhitespace && trimmed.isEmpty())
            {
               builder.append("\n");
               continue;
            }

            builder.append(commonIndent);
            builder.append(commentStart);
            builder.append(" ");
            builder.append(StringUtil.substring(line, commonIndent.length()));
            if (commentEnd != null)
            {
               builder.append(" ");
               builder.append(commentEnd);
            }

            builder.append("\n");
         }
      }
      else
      {
         for (String line : lines)
         {
            String trimmed = line.trim();

            if (trimmed.isEmpty())
            {
               builder.append("\n");
               continue;
            }

            boolean isCommentedLine = true;
            int commentStartIdx = line.indexOf(commentStart);
            if (commentStartIdx == -1)
               isCommentedLine = false;

            int commentEndIdx = line.length();
            if (commentEnd != null)
            {
               commentEndIdx = line.lastIndexOf(commentEnd);
               if (commentEndIdx == -1)
                  isCommentedLine = false;
            }

            if (!isCommentedLine)
            {
               builder.append(line);
               continue;
            }

            // We want to strip out the leading comment prefix,
            // but preserve the indent.
            int startIdx = commentStartIdx + commentStart.length();
            if (Character.isSpace(StringUtil.charAt(line, startIdx)))
               startIdx++;

            int endIdx = commentEndIdx;
            String afterComment = StringUtil.substring(line, startIdx, endIdx);
            builder.append(StringUtil.trimRight(commonIndent + afterComment));

            builder.append("\n");
         }
      }

      String newSelection = dontCommentLastLine ?
            builder.toString() :
            StringUtil.substring(builder, 0, builder.length() - 1);

      display.replaceSelection(newSelection);

      // Nudge the selection to match the commented action.
      if (isSingleLineAction)
      {
         int diff = newSelection.length() - lines[0].length();
         if (commentEnd != null)
            diff = diff < 0 ?
                  diff + commentEnd.length() + 1 :
                  diff - commentEnd.length() - 1;

         int colStart = initialRange.getStart().getColumn();
         int colEnd = initialRange.getEnd().getColumn();
         Range newRange = Range.create(
               rowStart,
               colStart + diff,
               rowStart,
               colEnd + diff);
         display.setSelectionRange(newRange);
      }
   }

   @Handler
   void onReflowComment()
   {
      withActiveEditor((disp) ->
      {
         reflowComment(disp);
      });
   }

   void reflowComment(DocDisplay display)
   {
      if (DocumentMode.isSelectionInRMode(display) ||
          DocumentMode.isSelectionInPythonMode(display))
      {
         doReflowComment(display, "(#)");
      }
      else if (DocumentMode.isSelectionInCppMode(display))
      {
         String currentLine = display.getLine(
                                    display.getCursorPosition().getRow());
         if (currentLine.startsWith(" *"))
            doReflowComment(display, "( \\*[^/])", false);
         else
            doReflowComment(display, "(//)");
      }
      else if (DocumentMode.isSelectionInTexMode(display))
         doReflowComment(display, "(%)");
      else if (DocumentMode.isSelectionInMarkdownMode(display))
         doReflowComment(display, "()");
      else if (display.getFileType().isText())
         doReflowComment(display, "()");
   }

   public void reflowText()
   {
      if (docDisplay_.getSelectionValue().isEmpty())
         docDisplay_.setSelectionRange(
               Range.fromPoints(
                     Position.create(docDisplay_.getCursorPosition().getRow(), 0),
                     Position.create(docDisplay_.getCursorPosition().getRow(),
                           docDisplay_.getCurrentLine().length())));

      onReflowComment();
      docDisplay_.setCursorPosition(
            Position.create(
                  docDisplay_.getSelectionEnd().getRow(),
                  0));
   }

   public void showHelpAtCursor()
   {
      docDisplay_.goToHelp();
   }

   @Handler
   void onDebugBreakpoint()
   {
      docDisplay_.toggleBreakpointAtCursor();
   }

   @Handler
   void onRsconnectDeploy()
   {
      view_.invokePublish();
   }

   @Handler
   void onRsconnectConfigure()
   {
      events_.fireEvent(RSConnectActionEvent.ConfigureAppEvent(
            docUpdateSentinel_.getPath()));
   }

   @Handler
   void onEditRmdFormatOptions()
   {
      rmarkdownHelper_.withRMarkdownPackage(
          "Editing R Markdown options",
          false,
          new CommandWithArg<RMarkdownContext>() {

            @Override
            public void execute(RMarkdownContext arg)
            {
               showFrontMatterEditor();
            }
          });
   }

   private void showFrontMatterEditor()
   {
      final String yaml = getRmdFrontMatter();
      if (yaml == null)
      {
         globalDisplay_.showErrorMessage(constants_.showFrontMatterEditorErrCaption(),
               constants_.showFrontMatterEditorMessage());
         return;
      }
      rmarkdownHelper_.convertFromYaml(yaml, new CommandWithArg<RmdYamlData>()
      {
         @Override
         public void execute(RmdYamlData arg)
         {
            String errCaption = constants_.showFrontMatterEditorErrCaption();
            String errMsg = constants_.showFrontMatterEditorErrMsg();

            if (arg == null)
            {
               globalDisplay_.showErrorMessage(errCaption, errMsg);
            }
            else if (!arg.parseSucceeded())
            {
               // try to find where the YAML segment begins in the document
               // so we can show an adjusted line number for the error
               int numLines = docDisplay_.getRowCount();
               int offsetLine = 0;
               String separator = RmdFrontMatter.FRONTMATTER_SEPARATOR.trim();
               for (int i = 0; i < numLines; i++)
               {
                  if (docDisplay_.getLine(i).equals(separator))
                  {
                     offsetLine = i + 1;
                     break;
                  }
               }
               globalDisplay_.showErrorMessage(errCaption,
                   errMsg + "\n\n" + arg.getOffsetParseError(offsetLine));
            }
            else
            {
               showFrontMatterEditorDialog(yaml, arg);
            }
         }
      });
   }

   private void showFrontMatterEditorDialog(String yaml, RmdYamlData data)
   {
      RmdSelectedTemplate selTemplate =
            rmarkdownHelper_.getTemplateFormat(yaml);
      if (selTemplate == null)
      {
         // we don't expect this to happen since we disable the dialog
         // entry point when we can't find an associated template
         globalDisplay_.showErrorMessage(constants_.showFrontMatterEditorDialogCaption(),
               constants_.showFrontMatterEditorDialogMessage());
         return;
      }
      RmdTemplateOptionsDialog dialog =
         new RmdTemplateOptionsDialog(selTemplate.template,
            selTemplate.format,
            data.getFrontMatter(),
            getPath() == null ? null : FileSystemItem.createFile(getPath()),
            selTemplate.isShiny,
            new OperationWithInput<RmdTemplateOptionsDialog.Result>()
            {
               @Override
               public void execute(RmdTemplateOptionsDialog.Result in)
               {
                  // when the dialog is completed successfully, apply the new
                  // front matter
                  applyRmdFormatOptions(in.format, in.outputOptions);
               }
            },
            new Operation()
            {
               @Override
               public void execute()
               {
                  // when the dialog is cancelled, update the view's format list
                  // (to cancel in-place changes)
                  updateRmdFormat();
               }
            });
      dialog.showModal();
   }

   private void applyRmdFormatOptions(String format,
         RmdFrontMatterOutputOptions options)
   {
      rmarkdownHelper_.replaceOutputFormatOptions(
            getRmdFrontMatter(), format, options,
            new OperationWithInput<String>()
            {
               @Override
               public void execute(String input)
               {
                  applyRmdFrontMatter(input);
               }
            });
   }

   private String getRmdFrontMatter()
   {
      if (isVisualEditorActive())
      {
         return visualMode_.getYamlFrontMatter();
      }
      else
      {
         return YamlFrontMatter.getFrontMatter(docDisplay_);
      }
   }

   private void applyRmdFrontMatter(String yaml)
   {
      boolean applied = false;
      if (isVisualEditorActive())
      {
         applied = visualMode_.applyYamlFrontMatter(yaml);
      }
      else
      {
         applied = YamlFrontMatter.applyFrontMatter(docDisplay_, yaml);
      }
      if (applied)
         updateRmdFormat();
   }

   private RmdSelectedTemplate getSelectedTemplate()
   {
      // try to extract the front matter and ascertain the template to which
      // it refers
      String yaml = getRmdFrontMatter();
      if (yaml == null)
         return null;
      return rmarkdownHelper_.getTemplateFormat(yaml);
   }

   private List<String> getOutputFormats()
   {
      String yaml = getRmdFrontMatter();
      if (yaml == null)
         return new ArrayList<>();
      List<String> formats = TextEditingTargetRMarkdownHelper.getOutputFormats(yaml);
      if (formats == null)
         formats = new ArrayList<>();
      return formats;
   }
   
   
   private List<String> getDocumentQuartoOutputFormats()
   {
      String yaml = getRmdFrontMatter();
      if (yaml == null)
         return new ArrayList<>();
      List<String> formats = TextEditingTargetRMarkdownHelper.getQuartoOutputFormats(yaml);
      if (formats == null)
         formats = new ArrayList<>();
      return formats;
   }
   
   private List<String> getProjectQuartoOutputFormats()
   {
      QuartoConfig quartoConfig = session_.getSessionInfo().getQuartoConfig();
      if (quartoConfig.is_project && quartoConfig.project_formats != null)
      {
         return Arrays.asList(quartoConfig.project_formats);
      }
      else
      {
         return new ArrayList<>();
      }
   }
   
   private List<String> getActiveQuartoOutputFormats()
   {
      List<String> docFormats = getDocumentQuartoOutputFormats();
      List<String> projFormats = getProjectQuartoOutputFormats();
      
      if (docFormats.size() > 0)
      {
         return docFormats;
      }
      else if (projFormats.size() > 0)
      {
         return projFormats;
      }
      else
      {
         List<String> formats = new ArrayList<>();
         return formats;
      }
   }

   private void updateRmdFormat()
   {
      String formatUiName = "";
      List<String> formatList = new ArrayList<>();
      List<String> valueList = new ArrayList<>();
      List<String> extensionList = new ArrayList<>();

      RmdSelectedTemplate selTemplate = getSelectedTemplate();
      
      // skip all of the format stuff for quarto docs
      if (extendedType_.equals(SourceDocument.XT_QUARTO_DOCUMENT))
      {
         if (isShinyPrerenderedDoc()) 
         {
            view_.setIsShinyFormat(false, false, true);  
         }
         else
         {
            view_.setIsNotShinyFormat();
            view_.setQuartoFormatOptions(fileType_, 
                                         getCustomKnit().length() == 0,
                                         this.getActiveQuartoOutputFormats());
         }
        
      }
      else if (selTemplate != null && selTemplate.isShiny)
      {
         view_.setIsShinyFormat(selTemplate.format != null,
                                selTemplate.format != null &&
                                selTemplate.format.endsWith(
                                      RmdOutputFormat.OUTPUT_PRESENTATION_SUFFIX),
                                isShinyPrerenderedDoc());
      }
      // could be runtime: shiny with a custom format
      else if (isShinyDoc())
      {
         view_.setIsShinyFormat(false,  // no output options b/c no template
                                false,  // not a presentation (unknown format)
                                isShinyPrerenderedDoc());
      }
      else
      {
         view_.setIsNotShinyFormat();
         if (selTemplate != null)
         {
            JsArray<RmdTemplateFormat> formats = selTemplate.template.getFormats();
            for (int i = 0; i < formats.length(); i++)
            {
               // skip notebook format (will enable it later if discovered)
               if (formats.get(i).getName() ==
                     RmdOutputFormat.OUTPUT_HTML_NOTEBOOK)
               {
                  continue;
               }

               // hide powerpoint if not available
               if (!session_.getSessionInfo().getPptAvailable() &&
                    StringUtil.equals(formats.get(i).getName(),
                                      RmdOutputFormat.OUTPUT_PPT_PRESENTATION))
               {
                  continue;
               }

               String uiName = formats.get(i).getUiName();
               formatList.add(uiName);
               valueList.add(formats.get(i).getName());
               extensionList.add(formats.get(i).getExtension());
               if (formats.get(i).getName() == selTemplate.format)
               {
                  formatUiName = uiName;
               }
            }
         }

         // add formats not in the selected template
         boolean isNotebook = false;
         List<String> outputFormats = getOutputFormats();
         for (int i = 0; i < outputFormats.size(); i++)
         {
            String format = outputFormats.get(i);
            if (format == RmdOutputFormat.OUTPUT_HTML_NOTEBOOK)
            {
               if (i == 0)
                  isNotebook = true;
               formatList.add(0, "Notebook");
               valueList.add(0, format);
               extensionList.add(0, ".nb.html");
               continue;
            }
            if (!valueList.contains(format))
            {
               String uiName = format;
               int nsLoc = uiName.indexOf("::");
               if (nsLoc != -1)
                  uiName = StringUtil.substring(uiName, nsLoc + 2);
               formatList.add(uiName);
               valueList.add(format);
               extensionList.add(null);
            }
         }

         view_.setFormatOptions(fileType_,
                                // can choose output formats
                                getCustomKnit().length() == 0,
                                // can edit format options
                                selTemplate != null,
                                formatList,
                                valueList,
                                extensionList,
                                formatUiName);

         // update notebook-specific options
         if (isNotebook)
         {
            // if the user manually set the output to console in a notebook,
            // respect that (even though it's weird)
            String outputType = RmdEditorOptions.getString(
                  YamlFrontMatter.getFrontMatter(docDisplay_),
                  TextEditingTargetNotebook.CHUNK_OUTPUT_TYPE, null);
            if (outputType != TextEditingTargetNotebook.CHUNK_OUTPUT_CONSOLE)
            {
               // chunk output should always be inline in notebooks
               outputType = docUpdateSentinel_.getProperty(
                     TextEditingTargetNotebook.CHUNK_OUTPUT_TYPE);
               if (outputType != TextEditingTargetNotebook.CHUNK_OUTPUT_INLINE)
               {
                  docUpdateSentinel_.setProperty(
                        TextEditingTargetNotebook.CHUNK_OUTPUT_TYPE,
                        TextEditingTargetNotebook.CHUNK_OUTPUT_INLINE);
               }
            }
            view_.setIsNotebookFormat();
         }
      }

      if (isShinyDoc())
      {
         // turn off inline output in Shiny documents (if it's not already)
         if (docDisplay_.showChunkOutputInline())
            docDisplay_.setShowChunkOutputInline(false);
      }
   }
   
   private void setQuartoFormat(String formatName)
   {
      HashMap<String, String> props = new HashMap<>();
      props.put(TextEditingTarget.QUARTO_PREVIEW_FORMAT, formatName);
      docUpdateSentinel_.modifyProperties(props, new NullProgressIndicator() {
         @Override
         public void onCompleted()
         {
            renderRmd();
         }
      });
   }
   

   private void setRmdFormat(String formatName)
   {
      // If the target format name already matches the first format then just
      // render and return
      List<String> outputFormats = getOutputFormats();
      if (outputFormats.size() > 0 && outputFormats.get(0).equals(formatName))
      {
         renderRmd();
         return;
      }

      rmarkdownHelper_.setOutputFormat(getRmdFrontMatter(), formatName,
            new CommandWithArg<String>()
      {
         @Override
         public void execute(String yaml)
         {
            if (yaml != null)
               applyRmdFrontMatter(yaml);

            // re-knit the document
            renderRmd();
         }
      });
   }

   void doReflowComment(DocDisplay display, String commentPrefix)
   {
      doReflowComment(display, commentPrefix, true);
   }

   void doReflowComment(DocDisplay display, String commentPrefix, boolean multiParagraphIndent)
   {
      display.focus();

      InputEditorSelection originalSelection = display.getSelection();
      InputEditorSelection selection = originalSelection;

      if (selection.isEmpty())
      {
         selection = selection.growToIncludeLines("^\\s*" + commentPrefix + ".*$");
      }
      else
      {
         selection = selection.shrinkToNonEmptyLines();
         selection = selection.extendToLineStart();
         selection = selection.extendToLineEnd();
      }
      if (selection.isEmpty())
         return;

      reflowComments(display,
                     commentPrefix,
                     multiParagraphIndent,
                     selection,
                     originalSelection.isEmpty() ?
                     originalSelection.getStart() :
                     null);
   }

   private void reflowComments(DocDisplay display,
                               String commentPrefix,
                               final boolean multiParagraphIndent,
                               InputEditorSelection selection,
                               final InputEditorPosition cursorPos)
   {
      String code = display.getCode(selection);
      String[] lines = code.split("\n");
      String prefix = StringUtil.getCommonPrefix(lines, true, false);
      Pattern pattern = Pattern.create("^\\s*" + commentPrefix + "+('?)\\s*");
      Match match = pattern.match(prefix, 0);
      // Selection includes non-comments? Abort.
      if (match == null)
         return;
      prefix = match.getValue();
      final boolean roxygen = match.hasGroup(1);

      int cursorRowIndex = 0;
      int cursorColIndex = 0;
      if (cursorPos != null)
      {
         cursorRowIndex = display.selectionToPosition(cursorPos).getRow() -
                          display.selectionToPosition(selection.getStart()).getRow();
         cursorColIndex =
               Math.max(0, cursorPos.getPosition() - prefix.length());
      }
      final WordWrapCursorTracker wwct = new WordWrapCursorTracker(
                                                cursorRowIndex, cursorColIndex);

      int maxLineLength = prefs_.marginColumn().getValue() - prefix.length();

      WordWrap wordWrap = new WordWrap(maxLineLength, false)
      {
         @Override
         protected boolean forceWrapBefore(String line)
         {
            String trimmed = line.trim();
            if (roxygen && trimmed.startsWith("@") && !trimmed.startsWith("@@"))
            {
               // Roxygen tags always need to be at the start of a line. If
               // there is content immediately following the roxygen tag, then
               // content should be wrapped until the next roxygen tag is
               // encountered.

               indent_ = "";
               if (TAG_WITH_CONTENTS.match(line, 0) != null)
               {
                  indentRestOfLines_ = true;
               }
               return true;
            }
            // empty line disables indentation
            else if (!multiParagraphIndent && (line.trim().length() == 0))
            {
               indent_ = "";
               indentRestOfLines_ = false;
            }

            return super.forceWrapBefore(line);
         }

         @Override
         protected void onChunkWritten(String chunk,
                                       int insertionRow,
                                       int insertionCol,
                                       int indexInOriginalString)
         {
            if (indentRestOfLines_)
            {
               indentRestOfLines_ = false;
               indent_ = "  "; // TODO: Use real indent from settings
            }

            wwct.onChunkWritten(chunk, insertionRow, insertionCol,
                                indexInOriginalString);
         }

         private boolean indentRestOfLines_ = false;
         private final Pattern TAG_WITH_CONTENTS = Pattern.create("@\\w+\\s+[^\\s]");
      };

      int macroDepth = 0;
      boolean outsideMarkdown = true;
      boolean bullet = false;
      boolean inExamples = false;
      
      for (String line : lines)
      {
         wwct.onBeginInputRow();
         
         boolean isWrappingEnabled = wordWrap.getWrappingEnabled();
         bullet = false;

         String content = StringUtil.substring(line, Math.min(line.length(),
                                                  prefix.length()));

         if (content.matches("^\\s*\\@examples\\b.*$"))
         {
            wordWrap.setWrappingEnabled(false);
            inExamples = true;
            wordWrap.appendLine(content);
         }
         else if (content.trim().startsWith("@"))
         {
            wordWrap.setWrappingEnabled(true);
            inExamples = false;
            wordWrap.appendLine(content);
         }
         else if (inExamples)
         {
            // still in @examples, keep being disabled
            wordWrap.appendLine(content);
         }
         else if (content.matches("^\\s*```.*")) 
         {
            wordWrap.setWrappingEnabled(false);
            wordWrap.appendLine(content);
            
            outsideMarkdown = !outsideMarkdown;
            wordWrap.setWrappingEnabled(outsideMarkdown);
         }
         else if (outsideMarkdown)
         {
            // the line is not in a markdown chunk
            bullet = content.matches("^\\s*[-*].*");
            if (bullet)
            {
               // this is a bullet line, temporarily disable
               wordWrap.setWrappingEnabled(false);
               wordWrap.appendLine(content);
               wordWrap.setWrappingEnabled(isWrappingEnabled);
            }
            else 
            {
               int previousMacroDepth = macroDepth;
               Pattern macro = Pattern.create("(\\{|\\})");
               Match macroMatch = macro.match(content, 0);
               while (macroMatch != null) 
               {
                  String value = macroMatch.getValue();
                  if (value.contains("}")) 
                     macroDepth--;
                  else 
                     macroDepth++;
                  
                  macroMatch = macroMatch.nextMatch();
               }
               if (macroDepth < 0)
               {
                  // should not happen, reset
                  macroDepth = 0;
               }

               wordWrap.setWrappingEnabled(macroDepth == 0 && previousMacroDepth == 0);
               wordWrap.appendLine(content);
               
            }
         }
         else
         {
            // the line is in a markdown chunk, disable for good measure
            // but not necessary really, because was disabled when seeing the ```
            wordWrap.setWrappingEnabled(false);
            wordWrap.appendLine(content);
         }

      }

      String wrappedString = wordWrap.getOutput();

      StringBuilder finalOutput = new StringBuilder();
      for (String line : StringUtil.getLineIterator(wrappedString))
         finalOutput.append(prefix).append(line).append("\n");
      // Remove final \n
      if (finalOutput.length() > 0)
         finalOutput.deleteCharAt(finalOutput.length()-1);

      String reflowed = finalOutput.toString();

      // Remove trailing whitespace that might have leaked in earlier
      reflowed = reflowed.replaceAll("\\s+\\n", "\n");

      display.setSelection(selection);
      if (!reflowed.equals(code))
      {
         display.replaceSelection(reflowed);
      }

      if (cursorPos != null)
      {
         if (wwct.getResult() != null)
         {
            int row = wwct.getResult().getY();
            int col = wwct.getResult().getX();
            row += display.selectionToPosition(selection.getStart()).getRow();
            col += prefix.length();
            Position pos = Position.create(row, col);
            display.setSelection(docDisplay_.createSelection(pos, pos));
         }
         else
         {
            display.collapseSelection(false);
         }
      }
   }

   @Handler
   void onExecuteCodeWithoutFocus()
   {
      withVisualModeSelection(() ->
      {
         codeExecution_.executeSelection(false);
      });
   }

   @Handler
   void onProfileCodeWithoutFocus()
   {
      dependencyManager_.withProfvis("The profiler", new Command()
      {
         @Override
         public void execute()
         {
            withVisualModeSelection(() ->
            {
               codeExecution_.executeSelection(false, false, "profvis::profvis", true);
            });
         }
      });
   }

   @Handler
   void onExecuteCodeWithoutMovingCursor()
   {
      if (docDisplay_.isFocused() || visualMode_.isVisualEditorActive())
      {
         withVisualModeSelection(() ->
         {
            codeExecution_.executeSelection(true, false);
         });
      }
      else if (view_.isAttached())
      {
         view_.findSelectAll();
      }
   }

   @Handler
   void onExecuteCode()
   {
      if (fileType_.isScript())
      {
         codeExecution_.sendSelectionToTerminal(true);
      }
      else
      {
         withVisualModeSelection(() ->
         {
            codeExecution_.executeSelection(true);
         });
      }
   }

   /**
    * Performs a command after synchronizing the document and selection state
    * from visual mode (useful for executing code). The command is not executed if
    * there is no active code editor in visual mode (e.g., the cursor is outside
    * a code chunk)
    *
    * @param command The command to perform
    */
   private void withVisualModeSelection(Command command)
   {
      if (isVisualEditorActive())
      {
         visualMode_.performWithSelection((pos) ->
         {
            // A null position indicates that the cursor is outside a code chunk.
            if (pos != null)
            {
               command.execute();
            }
         });
      }
      else
      {
         command.execute();
      }
   }

   /**
    * Performs a command after synchronizing the document and selection state
    * from visual mode. The command will be passed the current position of the
    * cursor after synchronizing, or null if the cursor in visual mode has no
    * corresponding location in source mode.
    *
    * @param command The command to perform.
    */
   private void withVisualModeSelection(CommandWithArg<Position> command)
   {
      if (isVisualEditorActive())
      {
         visualMode_.performWithSelection(command);
      }
      else
      {
         command.execute(docDisplay_.getCursorPosition());
      }
   }

   @Handler
   void onRunSelectionAsBackgroundJob()
   {
      withVisualModeSelection(() ->
      {
         codeExecution_.runSelectionAsJob(false /*isWorkbenchJob*/);
      });
   }

   @Handler
   void onRunSelectionAsWorkbenchJob()
   {
      withVisualModeSelection(() ->
      {
         codeExecution_.runSelectionAsJob(true /*isWorkbenchJob*/);
      });
   }

   @Handler
   void onExecuteCurrentLine()
   {
      withVisualModeSelection(() ->
      {
         codeExecution_.executeBehavior(UserPrefs.EXECUTION_BEHAVIOR_LINE);
      });
   }

   @Handler
   void onExecuteCurrentStatement()
   {
      withVisualModeSelection(() ->
      {
         codeExecution_.executeBehavior(UserPrefs.EXECUTION_BEHAVIOR_STATEMENT);
      });
   }

   @Handler
   void onExecuteCurrentParagraph()
   {
      withVisualModeSelection(() ->
      {
         codeExecution_.executeBehavior(UserPrefs.EXECUTION_BEHAVIOR_PARAGRAPH);
      });
   }

   @Handler
   void onSendToTerminal()
   {
      withVisualModeSelection(() ->
      {
         codeExecution_.sendSelectionToTerminal(false);
      });
   }

   @Handler
   void onOpenNewTerminalAtEditorLocation()
   {
      codeExecution_.openNewTerminalHere();
   }

   @Handler
   void onSendFilenameToTerminal()
   {
      codeExecution_.sendFilenameToTerminal();
   }

   @Override
   public String extractCode(DocDisplay docDisplay, Range range)
   {
      Scope sweaveChunk = null;

      if (fileType_.canExecuteChunks())
         sweaveChunk = scopeHelper_.getCurrentSweaveChunk(range.getStart());

      String code = sweaveChunk != null
                    ? scopeHelper_.getSweaveChunkText(sweaveChunk, range)
                    : docDisplay_.getCode(range.getStart(), range.getEnd());

      return code;
   }



   @Handler
   void onExecuteAllCode()
   {
      boolean executeChunks = fileType_.canCompilePDF() ||
                              fileType_.canKnitToHTML() ||
                              fileType_.isRpres();

      if (executeChunks)
      {
         prepareForVisualExecution(() ->
         {
            executeChunks(Position.create(
                  docDisplay_.getDocumentEnd().getRow() + 1,
                  0),
                  TextEditingTargetScopeHelper.PREVIOUS_CHUNKS);
         });
      }
      else
      {
         sourceActiveDocument(true);
      }
   }

   @Handler
   void onExecuteToCurrentLine()
   {
      withVisualModeSelection(() ->
      {
         if (!isVisualEditorActive())
         {
            docDisplay_.focus();
         }

         int row = docDisplay_.getSelectionEnd().getRow();
         int col = docDisplay_.getLength(row);

         codeExecution_.executeRange(Range.fromPoints(Position.create(0, 0),
                                     Position.create(row, col)));
      });
   }

   @Handler
   void onExecuteFromCurrentLine()
   {
      withVisualModeSelection(() ->
      {
         if (!isVisualEditorActive())
         {
            docDisplay_.focus();
         }

         int startRow = docDisplay_.getSelectionStart().getRow();
         int startColumn = 0;
         Position start = Position.create(startRow, startColumn);

         codeExecution_.executeRange(Range.fromPoints(start, endPosition()));
      });
   }

   @Handler
   void onExecuteCurrentFunction()
   {
      withVisualModeSelection(() ->
      {
         if (!isVisualEditorActive())
         {
            docDisplay_.focus();

            // HACK: This is just to force the entire function tree to be built.
            // It's the easiest way to make sure getCurrentScope() returns
            // a Scope with an end.
            //
            // We don't need to do this in visual mode since we force a scope
            // tree rebuild in the process of synchronizing the selection.
            docDisplay_.getScopeTree();
         }

         Scope currentFunction = docDisplay_.getCurrentFunction(false);

         // Check if we're at the top level (i.e. not in a function), or in
         // an unclosed function
         if (currentFunction == null || currentFunction.getEnd() == null)
            return;

         Position start = currentFunction.getPreamble();
         Position end = currentFunction.getEnd();

         codeExecution_.executeRange(Range.fromPoints(start, end));
      });
   }

   @Handler
   void onExecuteCurrentSection()
   {
      withVisualModeSelection(() ->
      {
         if (!isVisualEditorActive())
         {
            docDisplay_.focus();
            docDisplay_.getScopeTree();
         }

         // Determine the current section.
         Scope currentSection = docDisplay_.getCurrentSection();
         if (currentSection == null)
            return;

         // Determine the start and end of the section
         Position start = currentSection.getBodyStart();
         if (start == null)
            start = Position.create(0, 0);
         Position end = currentSection.getEnd();
         if (end == null)
            end = endPosition();

         codeExecution_.executeRange(Range.fromPoints(start, end));
      });
   }

   private Position endPosition()
   {
      int endRow = Math.max(0, docDisplay_.getRowCount() - 1);
      int endColumn = docDisplay_.getLength(endRow);
      return Position.create(endRow, endColumn);
   }

   // splits a chunk into two chunks
   // chunk 1: first chunk line to linePos (not including linePos)
   // chunk 2: linePos line to end
   private void splitChunk(Scope chunk, int linePos)
   {
      Position chunkStart = chunk.getBodyStart();
      Position chunkEnd = chunk.getEnd();
      if (chunkEnd == null)
         chunkEnd = docDisplay_.getDocumentEnd();
      Position preamble = chunk.getPreamble();
      String preambleLine = docDisplay_.getLine(preamble.getRow()) + "\n";

      // if the cursor line is in the preamble of the chunk
      // reset it to the body of the chunk to get the same semantics
      // (empty chunk followed by the entire existing chunk)
      if (linePos < chunkStart.getRow())
         linePos = chunkStart.getRow();

      // get chunk contents from chunk start up to the specified line
      Range firstChunkRange = Range.create(chunkStart.getRow(), chunkStart.getColumn(), linePos, 0);
      String firstChunkContents = docDisplay_.getTextForRange(firstChunkRange);
      firstChunkContents = firstChunkContents.trim();

      // add preamble line and ending line for new first chunk
      firstChunkContents = preambleLine + firstChunkContents;
      if (!firstChunkContents.endsWith("\n"))
         firstChunkContents += "\n";
      firstChunkContents += getChunkEnd() + "\n\n";

      // get second chunk contents from what's left
      Range secondChunkRange = Range.create(linePos, 0, chunkEnd.getRow(), chunkEnd.getColumn());
      String secondChunkContents = docDisplay_.getTextForRange(secondChunkRange);
      secondChunkContents = secondChunkContents.trim();

      // add the preamble line of the original chunk to the second chunk (so we have the correct language)
      secondChunkContents = preambleLine + secondChunkContents;

      // modify contents of original chunk with second chunk contents
      Range chunkRange = Range.create(chunkStart.getRow() - 1, chunkStart.getColumn(), chunkEnd.getRow(), chunkEnd.getColumn());
      docDisplay_.replaceRange(chunkRange, secondChunkContents);

      // insert new chunk with first chunk contents
      docDisplay_.setCursorPosition(Position.create(chunkStart.getRow() - 1, chunkStart.getColumn()));
      docDisplay_.insertCode(firstChunkContents, false);
   }

   // splits a chunk into three chunks
   // chunk 1: first chunk line to start pos
   // chunk 2: start pos to end pos
   // chunk 3: end pos to end of chunk
   private void splitChunk(Scope chunk, Position startPos, Position endPos)
   {
      Position chunkStart = chunk.getBodyStart();
      Position chunkEnd = chunk.getEnd();
      if (chunkEnd == null)
         chunkEnd = docDisplay_.getDocumentEnd();
      Position preamble = chunk.getPreamble();
      String preambleLine = docDisplay_.getLine(preamble.getRow()) + "\n";

      // if the selected position is only within the preamble, do nothing as it makes no sense to do any splitting
      if (startPos.getRow() == preamble.getRow() && endPos.getRow() == startPos.getRow())
         return;

      // if the selected position starts within the preamble, reset the start position to not include the preamble
      // as it does not make any sense to do any splitting within the preamble
      if (startPos.getRow() == preamble.getRow())
      {
         startPos.setRow(preamble.getRow() + 1);
         startPos.setColumn(0);
      }

      // if the selected position ends within the footer (```), reset the end position to not include it
      if (endPos.getRow() == chunkEnd.getRow() && endPos.getColumn() > 0)
      {
         endPos.setColumn(0);
      }

      // get chunk contents from chunk start up to the specified start pos
      Range firstChunkRange = Range.create(chunkStart.getRow(), chunkStart.getColumn(), startPos.getRow(), startPos.getColumn());
      String firstChunkContents = docDisplay_.getTextForRange(firstChunkRange);
      firstChunkContents = firstChunkContents.trim();

      // add preamble line and ending line for new first chunk
      firstChunkContents = preambleLine + firstChunkContents;
      if (!firstChunkContents.endsWith("\n"))
         firstChunkContents += "\n";
      firstChunkContents += getChunkEnd() + "\n\n";

      // get middle chunk contents from selected positions
      Range middleChunkRange = Range.create(startPos.getRow(), startPos.getColumn(), endPos.getRow(), endPos.getColumn());
      String middleChunkContents = docDisplay_.getTextForRange(middleChunkRange);
      middleChunkContents = middleChunkContents.trim();

      // // add preamble line and ending line for middle chunk
      middleChunkContents = preambleLine + middleChunkContents;
      if (!middleChunkContents.endsWith("\n"))
         middleChunkContents += "\n";
      middleChunkContents += getChunkEnd() + "\n\n";

      // get final chunk contents from ending selection position to ending chunk position
      Range finalChunkRange = Range.create(endPos.getRow(), endPos.getColumn(), chunkEnd.getRow(), chunkEnd.getColumn());
      String finalChunkContents = docDisplay_.getTextForRange(finalChunkRange);
      finalChunkContents = finalChunkContents.trim();

      // add preamble to final chunk
      finalChunkContents = preambleLine + finalChunkContents;

      // modify contents of original chunk with final chunk contents
      Range chunkRange = Range.create(chunkStart.getRow() - 1, chunkStart.getColumn(), chunkEnd.getRow(), chunkEnd.getColumn());
      docDisplay_.replaceRange(chunkRange, finalChunkContents);

      // insert first and middle chunk contents as new chunks
      docDisplay_.setCursorPosition(Position.create(chunkStart.getRow() - 1, chunkStart.getColumn()));
      docDisplay_.insertCode(firstChunkContents, false);
      Position middleChunkPos = docDisplay_.getCursorPosition();
      docDisplay_.insertCode(middleChunkContents, false);

      // reset cursor position to middle chunk (selected text)
      docDisplay_.setCursorPosition(middleChunkPos);
   }

   private void onInsertChunk(String chunkPlaceholder, int rowOffset, int colOffset)
   {
      // allow visual mode to handle
      if (visualMode_.isActivated())
      {
         // strip off the leading backticks (if rowOffset is 0 then adjust colOffset)
         chunkPlaceholder = chunkPlaceholder.replaceFirst("```", "");
         if (rowOffset == 0)
            colOffset -= 3;

         // strip off the trailing backticks
         chunkPlaceholder = chunkPlaceholder.replaceAll("\\n```\\n$", "");

         // do the insert
         visualMode_.insertChunk(chunkPlaceholder, rowOffset, colOffset);

         // all done!
         return;
      }

      String sel = "";
      Range selRange = null;

      // if currently in a chunk
      // with no selection, split this chunk into two chunks at the current line
      // with selection, split this chunk into three chunks (prior to selection, selected, post selection)
      Scope currentChunk = docDisplay_.getCurrentChunk();
      if (currentChunk != null)
      {
         // record current selection before manipulating text
         sel = docDisplay_.getSelectionValue();
         selRange = docDisplay_.getSelectionRange();

         if (selRange.isEmpty())
         {
            splitChunk(currentChunk, docDisplay_.getCursorPosition().getRow());
            return;
         }
         else
         {
            splitChunk(currentChunk, selRange.getStart() ,selRange.getEnd());
            return;
         }
      }

      Position pos = moveCursorToNextInsertLocation();
      InsertChunkInfo insertChunkInfo = docDisplay_.getInsertChunkInfo();
      if (insertChunkInfo != null)
      {
         // inject the chunk skeleton
         docDisplay_.insertCode(chunkPlaceholder, false);

         // if we had text selected, inject it into the chunk
         if (!StringUtil.isNullOrEmpty(sel))
         {
            Position contentPosition = insertChunkInfo.getContentPosition();
            Position docContentPos = Position.create(
                  pos.getRow() + contentPosition.getRow(),
                  contentPosition.getColumn());
            Position endPos = Position.create(docContentPos.getRow(),
                  docContentPos.getColumn());

            // move over newline if selected
            if (sel.endsWith("\n"))
               endPos.setRow(endPos.getRow() + 1);
            docDisplay_.replaceRange(
                  Range.fromPoints(docContentPos, endPos), sel);
            docDisplay_.replaceRange(selRange, "");
         }

         Position cursorPosition = insertChunkInfo.getCursorPosition();
         docDisplay_.setCursorPosition(Position.create(
               pos.getRow() + cursorPosition.getRow() + rowOffset,
               colOffset));
         docDisplay_.focus();
      }
      else
      {
         assert false : "Mode did not have insertChunkInfo available";
      }
   }

   String getChunkEnd()
   {
      InsertChunkInfo info = docDisplay_.getInsertChunkInfo();
      if (info == null)
         return "```"; // default to Rmd

      // chunks are delimited by 2 new lines
      // if not, we will fallback on an empty chunk end just for safety
      String[] chunkParts = info.getValue().split("\n\n");
      if (chunkParts.length == 2)
         return chunkParts[1];
      else
         return "";
   }

   @Handler
   void onInsertChunk()
   {
      if (fileType_.isQuartoMarkdown())
      {
         onQuartoInsertChunk();
         return;
      }
      
      InsertChunkInfo info = docDisplay_.getInsertChunkInfo();
      if (info == null)
         return;

      onInsertChunk(info.getValue(), 1, 0);
   }

   @Handler
   void onInsertChunkR()
   {
      onInsertChunk("```{r}\n\n```\n", 1, 0);
   }

   @Handler
   void onInsertChunkBash()
   {
      onInsertChunk("```{bash}\n\n```\n", 1, 0);
   }
   
   @Handler
   void onInsertChunkGraphViz()
   {
      onInsertChunk("```{dot}\n\n```\n", 1, 0);
   }
   
   @Handler
   void onInsertChunkJulia()
   {
      onInsertChunk("```{julia}\n\n```\n", 1, 0);
   }
   
   @Handler
   void onInsertChunkMermaid()
   {
      onInsertChunk("```{mermaid}\n\n```\n", 1, 0);
   }

   @Handler
   void onInsertChunkPython()
   {
      onInsertChunk("```{python}\n\n```\n", 1, 0);
   }

   @Handler
   void onInsertChunkRCPP()
   {
      onInsertChunk("```{Rcpp}\n\n```\n", 1, 0);
   }

   @Handler
   void onInsertChunkStan()
   {
      onInsertChunk("```{stan output.var=}\n\n```\n", 0, 20);
   }

   @Handler
   void onInsertChunkSQL()
   {
      server_.defaultSqlConnectionName(new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String name)
         {
            if (name != null)
            {
               onInsertChunk("```{sql connection=" + name + "}\n\n```\n", 1, 0);
            }
            else
            {
               onInsertChunk("```{sql connection=}\n\n```\n", 0, 19);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            onInsertChunk("```{sql connection=}\n\n```\n", 0, 19);
         }
      });
   }

   @Handler
   void onInsertChunkD3()
   {
      if (notebook_ != null) {
         Scope setupScope = notebook_.getSetupChunkScope();

         if (setupScope == null && !visualMode_.isActivated())
         {
            onInsertChunk("```{r setup}\nlibrary(r2d3)\n```\n\n```{d3 data=}\n\n```\n", 4, 12);
         }
         else {
            onInsertChunk("```{d3 data=}\n\n```\n", 0, 12);
         }
      }
   }
   
   // for qmd files, we default to python unless there is already an
   // r or ojs chunk in the file
   void onQuartoInsertChunk()
   {
      JsArrayString lines = docDisplay_.getLines();
      for (int i=0; i<lines.length(); i++)
      {
         Match match = RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN.match(lines.get(i), 0);
         if (match != null)
         {
            String engine = match.getGroup(1);
            Match matchName = RegexUtil.RE_RMARKDOWN_ENGINE_NAME.match(engine, 0);
            if (matchName != null)
            {
               onInsertChunk("```{" + matchName.getValue() + "}\n\n```\n", 1, 0);
               return;
            }
            
         }
      }
      
      // no other qualifying previous chunks, use r
      onInsertChunkR();  
   }

   @Handler
   void onInsertSection()
   {
      globalDisplay_.promptForText(
         constants_.onInsertSectionTitle(),
         constants_.onInsertSectionLabel(),
         MessageDisplay.INPUT_OPTIONAL_TEXT,
         new OperationWithInput<String>() {
            @Override
            public void execute(String label)
            {
               // move cursor to next insert location
               Position pos = moveCursorToNextInsertLocation();

               // truncate length to print margin - 5
               int printMarginColumn = prefs_.marginColumn().getValue();
               int length = printMarginColumn - 5;

               // truncate label to maxLength - 10 (but always allow at
               // least 20 chars for the label)
               int maxLabelLength = length - 10;
               maxLabelLength = Math.max(maxLabelLength, 20);
               if (label.length() > maxLabelLength)
                  label = StringUtil.substring(label, 0, maxLabelLength-1);

               // prefix
               String prefix = "# ";
               if (!label.isEmpty())
                  prefix = prefix + label + " ";

               // fill to maxLength (bit ensure at least 4 fill characters
               // so the section parser is sure to pick it up)
               StringBuffer sectionLabel = new StringBuffer();
               sectionLabel.append("\n");
               sectionLabel.append(prefix);
               int fillChars = length - prefix.length();
               fillChars = Math.max(fillChars, 4);
               for (int i=0; i<fillChars; i++)
                  sectionLabel.append("-");
               sectionLabel.append("\n\n");

               // insert code and move cursor
               docDisplay_.insertCode(sectionLabel.toString(), false);
               docDisplay_.setCursorPosition(Position.create(pos.getRow() + 3,
                                                             0));
               docDisplay_.focus();

            }
         });
   }

   private Position moveCursorToNextInsertLocation()
   {
      docDisplay_.collapseSelection(true);
      if (!docDisplay_.moveSelectionToBlankLine())
      {
         int lastRow = docDisplay_.getRowCount();
         int lastCol = docDisplay_.getLength(lastRow);
         Position endPos = Position.create(lastRow, lastCol);
         docDisplay_.setCursorPosition(endPos);
         docDisplay_.insertCode("\n", false);
      }
      return docDisplay_.getCursorPosition();

   }

   public void executeChunk(Position position)
   {
      docDisplay_.getScopeTree();
      executeSweaveChunk(scopeHelper_.getCurrentSweaveChunk(position),
            NotebookQueueUnit.EXEC_MODE_SINGLE, false);
   }

   public void dequeueChunk(int row)
   {
      notebook_.dequeueChunk(row);
   }

   @Handler
   void onExecuteCurrentChunk()
   {
      withVisualModeSelection(() ->
      {
         // HACK: This is just to force the entire function tree to be built.
         // It's the easiest way to make sure getCurrentScope() returns
         // a Scope with an end.
         docDisplay_.getScopeTree();

         executeSweaveChunk(scopeHelper_.getCurrentSweaveChunk(),
              NotebookQueueUnit.EXEC_MODE_SINGLE, false);
      });
   }

   @Handler
   void onExecuteNextChunk()
   {
      withVisualModeSelection((pos) ->
      {
         Scope nextChunk = null;
         if (pos == null)
         {
            // We are outside a chunk in visual mode, so get the nearest chunk below
            nextChunk = visualMode_.getNearestChunkScope(TextEditingTargetScopeHelper.FOLLOWING_CHUNKS);
            if (nextChunk == null)
            {
               // No next chunk to execute
               return;
            }
         }
         else
         {
            // Force scope tree rebuild and get chunk from source mode
            docDisplay_.getScopeTree();
            nextChunk = scopeHelper_.getNextSweaveChunk();
         }

         executeSweaveChunk(nextChunk, NotebookQueueUnit.EXEC_MODE_SINGLE,
               true);
         docDisplay_.setCursorPosition(nextChunk.getBodyStart());
         docDisplay_.ensureCursorVisible();
      });
   }

   @Handler
   void onExecutePreviousChunks()
   {
      executeScopedChunks(TextEditingTargetScopeHelper.PREVIOUS_CHUNKS);
   }

   @Handler
   void onExecuteSubsequentChunks()
   {
      executeScopedChunks(TextEditingTargetScopeHelper.FOLLOWING_CHUNKS);
   }

   /**
    * Executes all chunks in the given direction (previous or following)
    *
    * @param dir The direction in which to execute
    */
   private void executeScopedChunks(int dir)
   {
      withVisualModeSelection((pos) ->
      {
         if (pos == null)
         {
            // No active chunk position; look for the nearest chunk in the given direction
            Scope scope = visualMode_.getNearestChunkScope(dir);
            if (scope == null)
            {
               // No suitable chunks found; do nothing (expected if there just aren't
               // any previous/next chunks to run)
               return;
            }
            if (dir == TextEditingTargetScopeHelper.FOLLOWING_CHUNKS)
            {
               // Going down: start at beginning of next chunk
               pos = scope.getBodyStart();
            }
            else
            {
               // Going up: start *just beneath* chunk if we can (so chunk itself is included)

               // Clone position so we can update it without affecting the chunk scope
               pos = Position.create(scope.getEnd().getRow(), scope.getEnd().getColumn());
               if (pos.getRow() < docDisplay_.getRowCount())
               {
                  pos.setRow(pos.getRow() + 1);
                  pos.setColumn(1);
               }
            }
         }
         executeChunks(pos, dir);
      });
   }

   public void executeChunks(Position position, int which)
   {
      // null implies we should use current cursor position
      if (position == null)
         position = docDisplay_.getSelectionStart();

      if (docDisplay_.showChunkOutputInline())
      {
         executeChunksNotebookMode(position, which);
         return;
      }

      // HACK: This is just to force the entire function tree to be built.
      // It's the easiest way to make sure getCurrentScope() returns
      // a Scope with an end.
      docDisplay_.getScopeTree();

      // execute the chunks
      Scope[] previousScopes = scopeHelper_.getSweaveChunks(position,
            which);

      StringBuilder builder = new StringBuilder();
      for (Scope scope : previousScopes)
      {
         if (isRChunk(scope) && isExecutableChunk(scope))
         {
            builder.append("# " + scope.getLabel() + "\n");
            builder.append(scopeHelper_.getSweaveChunkText(scope));
            builder.append("\n\n");
         }
      }

      final String code = builder.toString().trim();
      if (fileType_.isRmd())
      {
         final Position positionFinal = position;
         docUpdateSentinel_.withSavedDoc(new Command()
         {
            @Override
            public void execute()
            {
               rmarkdownHelper_.prepareForRmdChunkExecution(
                     docUpdateSentinel_.getId(),
                     docUpdateSentinel_.getContents(),
                     new Command()
                     {
                        @Override
                        public void execute()
                        {
                           // compute the language for this chunk
                           String language = (DocumentMode.isPositionInPythonMode(docDisplay_, positionFinal))
                                 ? ConsoleLanguageTracker.LANGUAGE_PYTHON
                                 : ConsoleLanguageTracker.LANGUAGE_R;

                           events_.fireEvent(new SendToConsoleEvent(code, language, true));
                        }
                     });
            }
         });
      }
      else
      {
         events_.fireEvent(new SendToConsoleEvent(code, true));
      }
   }

   public void executeChunksNotebookMode(Position position, int which)
   {
      // HACK: This is just to force the entire function tree to be built.
      // It's the easiest way to make sure getCurrentScope() returns
      // a Scope with an end.
      docDisplay_.getScopeTree();

      // execute the previous chunks
      Scope[] previousScopes = scopeHelper_.getSweaveChunks(position, which);

      // create job description
      String jobDesc = "";
      if (previousScopes.length > 0)
      {
         if (position != null &&
             position.getRow() > docDisplay_.getDocumentEnd().getRow())
            jobDesc = constants_.runAll();
         else if (which == TextEditingTargetScopeHelper.PREVIOUS_CHUNKS)
            jobDesc = constants_.runPrevious();
         else if (which == TextEditingTargetScopeHelper.FOLLOWING_CHUNKS)
            jobDesc = constants_.runAfter();
      }

      List<ChunkExecUnit> chunks = new ArrayList<>();
      for (Scope scope : previousScopes)
      {
         if (isExecutableChunk(scope))
            chunks.add(
                  new ChunkExecUnit(scope, NotebookQueueUnit.EXEC_MODE_BATCH));
      }

      if (!chunks.isEmpty())
         notebook_.executeChunks(jobDesc, chunks);
   }

   @Handler
   public void onExecuteSetupChunk()
   {
      prepareForVisualExecution(() -> executeSetupChunk());
   }

   private void executeSetupChunk()
   {
      // attempt to find the setup scope by name
      Scope setupScope = null;
      if (notebook_ != null)
         setupScope = notebook_.getSetupChunkScope();

      // if we didn't find it by name, flatten the scope list and find the
      // first chunk
      if (setupScope == null)
      {
         ScopeList scopes = new ScopeList(docDisplay_);
         for (Scope scope: scopes)
         {
            if (scope.isChunk())
            {
               setupScope = scope;
               break;
            }
         }
      }

      // if we found a candidate, run it
      if (setupScope != null)
      {
         executeSweaveChunk(setupScope, NotebookQueueUnit.EXEC_MODE_BATCH,
               false);
      }
   }

   public void renderLatex()
   {
      if (mathjax_ != null)
         mathjax_.renderLatex();
   }

   public void renderLatex(Range range, boolean background)
   {
      if (mathjax_ != null)
         mathjax_.renderLatex(range, background);
   }

   public String getDefaultNamePrefix()
   {
      return null;
   }

   @Override
   public String getCurrentStatus()
   {
      Position pos = docDisplay_.getCursorPosition();
      String scope = statusBar_.getScope().getValue();
      if (StringUtil.isNullOrEmpty(scope))
         scope = constants_.none();
      String name = getName().getValue();
      if (StringUtil.isNullOrEmpty(name))
         name = constants_.noName();

      StringBuilder status = new StringBuilder();
      status.append(constants_.getCurrentStatusRow()).append(pos.getRow() + 1)
              .append(constants_.getCurrentStatusColumn("")).append(pos.getColumn() + 1);
      status.append(constants_.getCurrentStatusScope("")).append(scope);
      status.append(constants_.getCurrentStatusFileType("")).append(fileType_.getLabel());
      status.append(constants_.getCurrentStatusFileName("")).append(name);
      return status.toString();
   }
   
   public String getEngineForRow(int row)
   {
      String line = getDocDisplay().getLine(row);
      Map<String, String> options = RChunkHeaderParser.parse(line);
      String engine = StringUtil.stringValue(options.get("engine"));
      return engine;
   }
   
   private boolean hasREngineChunks()
   {
      JsArray<Scope> tree = docDisplay_.getScopeTree();
      for (int i=0; i<tree.length(); i++) 
      {
         Scope scope = tree.get(i);
         if (scope.isChunk())
         {  
            int row = scope.getPreamble().getRow();
            if (getEngineForRow(row).toLowerCase().equals("r"))
               return true;
         }
      }
      return false;
   }

   private boolean isRChunk(Scope scope)
   {
      String labelText = docDisplay_.getLine(scope.getPreamble().getRow());
      Map<String, String> chunkOptions = RChunkHeaderParser.parse(labelText);
      if (!chunkOptions.containsKey("engine"))
         return true;
      
      // NOTE: We might want to include 'Rscript' but such chunks are typically
      // intended to be run in their own process so it might not make sense to
      // collect those here.
      String engine = chunkOptions.get("engine").toLowerCase();
      return engine == "\"r\"";
   }
   

   private boolean isExecutableChunk(final Scope chunk)
   {
      if (!chunk.isChunk())
         return false;

      String headerText = docDisplay_.getLine(chunk.getPreamble().getRow());
      Pattern reEvalFalse = Pattern.create("eval\\s*=\\s*F(?:ALSE)?");
      if (reEvalFalse.test(headerText))
         return false;

      // Also check for YAML style chunk option with eval false
      Pattern reYamlOpt = Pattern.create("^#\\| .*");
      Pattern reYamlEvalFalse = Pattern.create("eval\\s*:\\s*false");
      int start = chunk.getBodyStart().getRow();
      int end = chunk.getEnd().getRow();
      ArrayList<String> chunkBody = JsArrayUtil.fromJsArrayString(docDisplay_.getLines(start, end));
      for (String line : chunkBody)
      {
         if (reYamlOpt.test(line))
         {
            // both #| eval: false and #| eval = FALSE style comments are permitted
            if (reYamlEvalFalse.test(line) || reEvalFalse.test(line))
               return false;
         }
         else
         {
            // all yaml chunk options should be at the beginning of the chunk, so we can stop early once we get to a
            // line that does not start with #|
            break;
         }
      }

      return true;
   }

   private void executeSweaveChunk(final Scope chunk,
                                   final int mode,
                                   final boolean scrollNearTop)
   {
      if (chunk == null)
         return;

      // command used to execute chunk (we may need to defer it if this
      // is an Rmd document as populating params might be necessary)
      final Command executeChunk = new Command() {
         @Override
         public void execute()
         {
            Range range = scopeHelper_.getSweaveChunkInnerRange(chunk);
            if (scrollNearTop)
            {
               docDisplay_.navigateToPosition(
                     SourcePosition.create(range.getStart().getRow(),
                                           range.getStart().getColumn()),
                     true);
            }
            if (!range.isEmpty())
            {
               codeExecution_.setLastExecuted(range.getStart(), range.getEnd());
            }
            if (fileType_.isRmd() &&
                docDisplay_.showChunkOutputInline())
            {
               // in notebook mode, an empty chunk can refer to external code,
               // so always execute it
               notebook_.executeChunk(chunk);
            }
            else if (!range.isEmpty())
            {
               String code = scopeHelper_.getSweaveChunkText(chunk);

               // compute the language for this chunk
               String language = "R";
               if (DocumentMode.isPositionInPythonMode(docDisplay_, chunk.getBodyStart()))
                  language = "Python";

               events_.fireEvent(new SendToConsoleEvent(code, language, true));
            }
            docDisplay_.collapseSelection(true);
         }
      };

      // Rmd allows server-side prep for chunk execution
      if (fileType_.isRmd() && !docDisplay_.showChunkOutputInline())
      {
         // ensure source is synced with server
         docUpdateSentinel_.withSavedDoc(new Command() {
            @Override
            public void execute()
            {
               // allow server to prepare for chunk execution
               // (e.g. by populating 'params' in the global environment)
               rmarkdownHelper_.prepareForRmdChunkExecution(
                     docUpdateSentinel_.getId(),
                     docUpdateSentinel_.getContents(),
                     executeChunk);
            }
         });
      }
      else
      {
         executeChunk.execute();
      }

   }

   @Handler
   void onJumpTo()
   {
      statusBar_.getScope().click();
   }

   @Handler
   void onGoToLine()
   {
      globalDisplay_.promptForInteger(
            constants_.onGoToLineTitle(),
            constants_.onGoToLineLabel(),
            null,
            new ProgressOperationWithInput<Integer>()
            {
               @Override
               public void execute(Integer line, ProgressIndicator indicator)
               {
                  indicator.onCompleted();

                  line = Math.max(1, line);
                  line = Math.min(docDisplay_.getRowCount(), line);

                  docDisplay_.navigateToPosition(
                        SourcePosition.create(line-1, 0),
                        true);
               }
            },
            null);
   }

   @Handler
   void onCodeCompletion()
   {
      withActiveEditor((disp) ->
      {
         disp.codeCompletion();
      });
   }
   
   @Handler
   void onGoToHelp()
   {
      withActiveEditor((disp) ->
      {
         disp.goToHelp();
      });
   }

   @Handler
   void onGoToDefinition()
   {
      withActiveEditor((disp) ->
      {
         disp.goToDefinition();
      });
   }

   @Handler
   void onFindAll()
   {
      docDisplay_.selectAll(docDisplay_.getSelectionValue());
   }

   @Handler
   void onFindUsages()
   {
      cppHelper_.findUsages();
   }

   @Handler
   public void onSetWorkingDirToActiveDoc()
   {
      // get path
      String activeDocPath = docUpdateSentinel_.getPath();
      if (activeDocPath != null)
      {
         FileSystemItem wdPath =
            FileSystemItem.createFile(activeDocPath).getParentPath();
         consoleDispatcher_.executeSetWd(wdPath, true);
      }
      else
      {
         globalDisplay_.showMessage(
               MessageDialog.WARNING,
               constants_.onSetWorkingDirToActiveDocCaption(),
               constants_.onSetWorkingDirToActiveDocMessage());
      }
   }

   private String stangle(String sweaveStr)
   {
      ScopeList chunks = new ScopeList(docDisplay_);
      chunks.selectAll(ScopeList.CHUNK);

      StringBuilder code = new StringBuilder();
      for (Scope chunk : chunks)
      {
         String text = scopeHelper_.getSweaveChunkText(chunk);
         code.append(text);
         if (text.length() > 0 && StringUtil.charAt(text, text.length()-1) != '\n')
            code.append('\n');
      }
      return code.toString();
   }

   @Handler
   void onPreviewJS()
   {
      previewJS();
   }

   @Handler
   void onPreviewSql()
   {
      previewSql();
   }

   @Handler
   void onSourceActiveDocument()
   {
      sourceActiveDocument(false);
   }

   @Handler
   void onSourceActiveDocumentWithEcho()
   {
      sourceActiveDocument(true);
   }

   @Handler
   void onSourceAsJob()
   {
      saveThenExecute(null, true, () ->
      {
         events_.fireEvent(new JobRunScriptEvent(getPath()));
      });
   }

   @Handler
   public void onSourceAsWorkbenchJob()
   {
      saveThenExecute(null, true, () ->
      {
         events_.fireEvent(new LauncherJobRunScriptEvent(getPath()));
      });
   }

   @Handler
   void onProfileCode()
   {
      dependencyManager_.withProfvis("The profiler", new Command()
      {
         @Override
         public void execute()
         {
            withVisualModeSelection(() ->
            {
               codeExecution_.executeSelection(true, true, "profvis::profvis", true);
            });
         }
      });
   }

   private void sourceActiveDocument(final boolean echo)
   {
      if (!isVisualEditorActive())
      {
         docDisplay_.focus();
      }

      // If this is a Python file, use reticulate.
      if (fileType_.isPython())
      {
         if (extendedType_.startsWith(SourceDocument.XT_PY_SHINY_PREFIX))
         {
            runPyShinyApp();
            return;
         }
         sourcePython();
         return;
      }

      if (fileType_.isR())
      {
         if (extendedType_.startsWith(SourceDocument.XT_SHINY_PREFIX))
         {
            // If the document being sourced is a Shiny file, run the app instead.
            runShinyApp();
            return;
         }
         else if (extendedType_ == SourceDocument.XT_PLUMBER_API)
         {
            // If the document being sourced in a Plumber file, run the API instead.
            runPlumberAPI();
            return;
         }
      }

      // if the document is an R Markdown notebook, run all its chunks instead
      if (fileType_.isRmd() && isRmdNotebook())
      {
         onExecuteAllCode();
         return;
      }

      // If the document being sourced is a script then use that codepath
      if (fileType_.isScript())
      {
         runScript();
         return;
      }

      // If the document is previewable
      if (fileType_.canPreviewFromR())
      {
         previewFromR();
         return;
      }

      String code = docDisplay_.getCode();
      if (code != null && code.trim().length() > 0)
      {
         boolean sweave =
            fileType_.canCompilePDF() ||
            fileType_.canKnitToHTML() ||
            fileType_.isRpres();

         RnwWeave rnwWeave = compilePdfHelper_.getActiveRnwWeave();
         final boolean forceEcho = sweave && (rnwWeave != null) ? rnwWeave.forceEchoOnExec() : false;

         // NOTE: we always set echo to true for knitr because knitr doesn't
         // require print statements so if you don't echo to the console
         // then you don't see any of the output

         boolean saveWhenSourcing = fileType_.isCpp() ||
               docDisplay_.hasBreakpoints() || (prefs_.saveBeforeSourcing().getValue() && (getPath() != null) && !sweave);

         if ((dirtyState_.getValue() || sweave) && !saveWhenSourcing)
         {
            server_.saveActiveDocument(code,
                                       sweave,
                                       compilePdfHelper_.getActiveRnwWeaveName(),
                                       new SimpleRequestCallback<Void>() {
               @Override
               public void onResponseReceived(Void response)
               {
                  consoleDispatcher_.executeSourceCommand(
                        "~/.active-rstudio-document",
                        fileType_,
                        "UTF-8",
                        activeCodeIsAscii(),
                        forceEcho ? true : echo,
                        prefs_.focusConsoleAfterExec().getValue(),
                        docDisplay_.hasBreakpoints());
               }
            });
         }
         else
         {
            Command sourceCommand = new Command() {
                  @Override
                  public void execute()
                  {
                     executeRSourceCommand(forceEcho ? true : echo,
                        prefs_.focusConsoleAfterExec().getValue());
                  }
               };

            if (saveWhenSourcing && (dirtyState_.getValue() || (getPath() == null)))
               saveThenExecute(null, true, sourceCommand);
            else
               sourceCommand.execute();
         }
      }

      // update pref if necessary
      if (prefs_.sourceWithEcho().getValue() != echo)
      {
         prefs_.sourceWithEcho().setGlobalValue(echo, true);
         prefs_.writeUserPrefs();
      }
   }

   private void runShinyApp()
   {
      source_.withSaveFilesBeforeCommand(() ->
      {
         events_.fireEvent(new LaunchShinyApplicationEvent(getPath(),
               prefs_.shinyBackgroundJobs().getValue() ?
                  ShinyApplication.BACKGROUND_APP :
                  ShinyApplication.FOREGROUND_APP, getExtendedFileType()));
      }, () -> {}, "Run Shiny Application");
   }

   private void runPlumberAPI()
   {
      source_.withSaveFilesBeforeCommand(new Command() {
         @Override
         public void execute()
         {
            events_.fireEvent(new LaunchPlumberAPIEvent(getPath()));
         }
      }, () -> {}, "Run Plumber API");
   }

   private void runPyShinyApp()
   {
      source_.withSaveFilesBeforeCommand(() ->
      {
         String path = getPath();
         FileSystemItem filePath = FileSystemItem.createFile(path);
         // resolve aliased path so that it can be understood in Windows terminal event

         path = server_.resolveAliasedPath(filePath);
         if (BrowseCap.isWindows())
         {
           // on Windows, double quote path to avoid issues with spaces
           path = '"' + path + '"';
         } else 
         {
            // escapeBashPath is POSIX only
            path = StringUtil.escapeBashPath(path, false);
         }
         events_.fireEvent(new SendToTerminalEvent("shiny run --reload --launch-browser --port=0 " + path + "\n", true));
      }, () -> {}, "Run Shiny Application");
   }

   private void sourcePython()
   {
      saveThenExecute(null, true, () -> {
         dependencyManager_.withReticulate(
               constants_.sourcePythonProgressCaption(),
               constants_.sourcePythonUserPrompt(),
               () -> {
                  String command = "reticulate::source_python('" + getPath() + "')";
                  events_.fireEvent(new SendToConsoleEvent(command, true));
               });
      });
   }

   private void runScript()
   {
      saveThenExecute(null, true, new Command() {
         @Override
         public void execute()
         {
            String interpreter = fileType_.getScriptInterpreter();
            server_.getScriptRunCommand(
               interpreter,
               getPath(),
               new SimpleRequestCallback<String>() {
                  @Override
                  public void onResponseReceived(String cmd)
                  {
                     events_.fireEvent(new SendToConsoleEvent(cmd, true));
                  }
               });
         }
      });
   }

   private void previewFromR()
   {
      saveThenExecute(null, true, new Command() {
         @Override
         public void execute()
         {
            server_.getMinimalSourcePath(
               getPath(),
               new SimpleRequestCallback<String>() {
                  @Override
                  public void onResponseReceived(String path)
                  {
                     String cmd = fileType_.createPreviewCommand(path);
                     if (cmd != null)
                        events_.fireEvent(new SendToConsoleEvent(cmd, true));
                  }
               });
         }
      });
   }

   private boolean activeCodeIsAscii()
   {
      String code = docDisplay_.getCode();
      for (int i=0; i< code.length(); i++)
      {
         if (code.charAt(i) > 127)
            return false;
      }

      return true;
   }

   @Handler
   void onExecuteLastCode()
   {
      withVisualModeSelection(() ->
      {
         if (!isVisualEditorActive())
         {
            docDisplay_.focus();
         }

         codeExecution_.executeLastCode();
      });
   }

   @Handler
   void onKnitDocument()
   {
      onPreviewHTML();
   }
   
   @Handler
   void onQuartoRenderDocument()
   {
      renderRmd();
   }
   
   @Handler
   void onRunDocumentFromServerDotR()
   {
      SourceColumn column = view_.getSourceColumn();
      EditingTarget runTarget = column.shinyRunDocumentEditor(docUpdateSentinel_.getPath());
      if (runTarget != null)
      {
         Command renderCommand = new Command()
         {
            @Override
            public void execute()
            { 
               rmarkdownHelper_.renderRMarkdown(
                     runTarget.getPath(),
                     1,
                     null,
                     "UTF-8",
                     null,
                     false,
                     RmdOutput.TYPE_SHINY,
                     false,
                     null,
                     null);
            }
         };
         
         final Command saveCommand = new Command()
         {
            @Override
            public void execute()
            {
               saveThenExecute(null, true, renderCommand);
            }
         };

         // save before rendering if the document is dirty or has never been saved;
         // otherwise render directly
         Command command =
               docUpdateSentinel_.getPath() == null || dirtyState_.getValue() ?
                     saveCommand : renderCommand;
         command.execute();
         
      }
   }

   @Handler
   void onPreviewHTML()
   {
      // last ditch extended type detection
      String extendedType = extendedType_;
      extendedType = rmarkdownHelper_.detectExtendedType(docDisplay_.getCode(),
                                                         extendedType,
                                                         fileType_);

      if (extendedType.startsWith(SourceDocument.XT_RMARKDOWN_PREFIX))
      {
         renderRmd();
      }
      else if (fileType_.isRd())
         previewRd();
      else if (fileType_.isRpres())
         previewRpresentation();
      else if (fileType_.isR())
         onCompileNotebook();
      else
         previewHTML();
   }

   void previewRpresentation()
   {
      SessionInfo sessionInfo = session_.getSessionInfo();
      if (!fileTypeCommands_.getHTMLCapabiliites().isRMarkdownSupported())
      {
         globalDisplay_.showMessage(
               MessageDisplay.MSG_WARNING,
               constants_.previewRpresentationCaption(),
               constants_.previewRpresentationMessage());
         return;
      }

      PresentationState state = sessionInfo.getPresentationState();

      // if this presentation is already showing then just activate
      if (state.isActive() &&
          state.getFilePath().equals(docUpdateSentinel_.getPath()))
      {
         commands_.activatePresentation().execute();
         save();
      }
      // otherwise reload
      else
      {
         saveThenExecute(null, true, new Command() {
               @Override
               public void execute()
               {
                  server_.showPresentationPane(docUpdateSentinel_.getPath(),
                                               new VoidServerRequestCallback());
               }

            });
         }
   }


   void previewRd()
   {
      saveThenExecute(null, true, new Command() {
         @Override
         public void execute()
         {
            String previewURL = "help/preview?file=";
            previewURL += URL.encodeQueryString(docUpdateSentinel_.getPath());
            events_.fireEvent(new ShowHelpEvent(previewURL));
         }
      });
   }

   void previewJS()
   {
      verifyD3Prequisites(new Command() {
         @Override
         public void execute()
         {
            saveThenExecute(null, true, new Command() {
               @Override
               public void execute()
               {
                  jsHelper_.previewJS(TextEditingTarget.this);
               }
            });
         }
      });
   }

   void previewSql()
   {
      verifySqlPrerequisites(new Command() {
         @Override
         public void execute()
         {
            saveThenExecute(null, true, new Command() {
               @Override
               public void execute()
               {
                  sqlHelper_.previewSql(TextEditingTarget.this);
               }
            });
         }
      });
   }

   boolean customSource()
   {
      return rHelper_.customSource(TextEditingTarget.this);
   }

   void renderRmd()
   {
      renderRmd(null);
   }
   
   void renderRmd(final String paramsFile)
   {
      renderRmd(null, paramsFile);
   }

   void renderRmd(final String format, final String paramsFile)
   {
      if (extendedType_ != SourceDocument.XT_QUARTO_DOCUMENT)
         events_.fireEvent(new RmdRenderPendingEvent(docUpdateSentinel_.getId()));

      final int type = isShinyDoc() ? RmdOutput.TYPE_SHINY:
                                      isRmdNotebook() ? RmdOutput.TYPE_NOTEBOOK:
                                                        RmdOutput.TYPE_STATIC;
      final Command renderCommand = new Command()
      {
         @Override
         public void execute()
         {
            boolean asTempfile = isPackageDocumentationFile();
            String viewerType = RmdEditorOptions.getString(
                  getRmdFrontMatter(), RmdEditorOptions.PREVIEW_IN, null);

            // if visual mode is active, move the cursor in source mode to
            // match its position in visual mode, so that we pass the correct
            // line number hint to render below
            if (isVisualEditorActive())
            {
               visualMode_.syncSourceOutlineLocation();
            }
              
            
            // Command we can use to do an R Markdown render
            Command renderCmd = new Command() {
               @Override
               public void execute()
               {
                  rmarkdownHelper_.renderRMarkdown(
                     docUpdateSentinel_.getPath(),
                     docDisplay_.getCursorPosition().getRow() + 1,
                     format != null ? format : quartoFormat(),
                     docUpdateSentinel_.getEncoding(),
                     paramsFile,
                     asTempfile,
                     type,
                     false,
                     rmarkdownHelper_.getKnitWorkingDir(docUpdateSentinel_),
                     viewerType);
                  
               }
               
            };
            
                         
            // see if we should be using quarto preview
            if (useQuartoPreview())
            {    
               // command to execute quarto preview
               Command quartoPreviewCmd = new Command() {
                  @Override
                  public void execute()
                  {
                     // quarto preview can reject the preview (e.g. if it turns
                     // out this file is part of a website or book project)
                     String format = quartoFormat();
                     server_.quartoPreview(
                        docUpdateSentinel_.getPath(), 
                        format, 
                        isQuartoRevealJs(format) ? presentationEditorLocation() : null,
                        new SimpleRequestCallback<Boolean>() {
                           @Override
                           public void onResponseReceived(Boolean previewed)
                           {
                              if (!previewed) 
                              {
                                 renderCmd.execute();
                              }
                           }
                        });
                     
                  }
               };
               
               // require rmarkdown if this document has R chunks
               if (hasREngineChunks())
               {
                  rmarkdownHelper_.withRMarkdownPackage("Rendering Quarto Knitr Documents", 
                                                        quartoPreviewCmd);
               }
               else
               {
                  quartoPreviewCmd.execute();
               }
            }
            else
            {
               renderCmd.execute();
            }
         }
      };

      final Command saveCommand = new Command()
      {
         @Override
         public void execute()
         {
            saveThenExecute(null, true, renderCommand);
         }
      };

      // save before rendering if the document is dirty or has never been saved;
      // otherwise render directly
      Command command =
            docUpdateSentinel_.getPath() == null || dirtyState_.getValue() ?
                  saveCommand : renderCommand;

      if (isRmdNotebook())
         dependencyManager_.withRMarkdown("Creating R Notebooks", command);
      else
         command.execute();
   }


   public boolean isRmdNotebook()
   {
      List<String> outputFormats = getOutputFormats();
      return outputFormats.size() > 0 &&
             outputFormats.get(0) == RmdOutputFormat.OUTPUT_HTML_NOTEBOOK;
   }

   public boolean hasRmdNotebook()
   {
      List<String> outputFormats = getOutputFormats();
      for (String format: outputFormats)
      {
         if (format == RmdOutputFormat.OUTPUT_HTML_NOTEBOOK)
            return true;
      }
      return false;
   }
   
   
   private PresentationEditorLocation presentationEditorLocation()
   {
      PresentationEditorLocation location;
      if (isVisualEditorActive())
      {
         location = visualMode_.getPresentationEditorLocation();
      }
      else
      {
         location = presentation2Helper_.getPresentationEditorLocation();
      } 
      return location;
   }

   private boolean isShinyDoc()
   {
      try
      {
         String yaml = getRmdFrontMatter();
         if (yaml == null)
            return false;
         return rmarkdownHelper_.isRuntimeShiny(yaml);
      }
      catch(Exception e)
      {
         Debug.log(e.getMessage());
         return false;
      }
   }

   private String getCustomKnit()
   {
      try
      {
         String yaml = getRmdFrontMatter();
         if (yaml == null)
            return "";
         return rmarkdownHelper_.getCustomKnit(yaml);
      }
      catch(Exception e)
      {
         Debug.log(e.getMessage());
         return "";
      }
   }
   
   private String quartoFormat()
   {
      if (session_.getSessionInfo().getQuartoConfig().enabled &&
         (extendedType_ == SourceDocument.XT_QUARTO_DOCUMENT))
      {
         List<String> formats = getActiveQuartoOutputFormats();
         String previewFormat = docUpdateSentinel_.getProperty(TextEditingTarget.QUARTO_PREVIEW_FORMAT);          
         if (previewFormat != null && formats.contains(previewFormat))
         {
            return previewFormat;
         }
         else
         {
            return formats.size() > 0 ? formats.get(0) : null;
         }
      }
      else
      {
         return null;
      }
   }
   
   
   private boolean useQuartoPreview()
   {
      return (session_.getSessionInfo().getQuartoConfig().enabled &&
            (extendedType_ == SourceDocument.XT_QUARTO_DOCUMENT) &&
            !isShinyDoc() && !isRmdNotebook());
   }
    
   
   private boolean isQuartoRevealJs(String format)
   {
      return (format != null) && (format.startsWith("revealjs") || format.endsWith("revealjs"));
   }
  

   void previewHTML()
   {
      // validate pre-reqs
      if (!rmarkdownHelper_.verifyPrerequisites(view_, fileType_))
         return;

      doHtmlPreview(new Provider<HTMLPreviewParams>()
      {
         @Override
         public HTMLPreviewParams get()
         {
            return HTMLPreviewParams.create(docUpdateSentinel_.getPath(),
                                            docUpdateSentinel_.getEncoding(),
                                            fileType_.isMarkdown(),
                                            fileType_.requiresKnit(),
                                            false);
         }
      });
   }

   private void doHtmlPreview(final Provider<HTMLPreviewParams> pParams)
   {
      // command to show the preview window
      final Command showPreviewWindowCommand = new Command() {
         @Override
         public void execute()
         {
            HTMLPreviewParams params = pParams.get();
            events_.fireEvent(new ShowHTMLPreviewEvent(params));
         }
      };

      // command to run the preview
      final Command runPreviewCommand = new Command() {
         @Override
         public void execute()
         {
            final HTMLPreviewParams params = pParams.get();
            server_.previewHTML(params, new SimpleRequestCallback<>());
         }
      };

      if (pParams.get().isNotebook())
      {
         saveThenExecute(null, true, new Command()
         {
            @Override
            public void execute()
            {
               generateNotebook(new Command()
               {
                  @Override
                  public void execute()
                  {
                     showPreviewWindowCommand.execute();
                     runPreviewCommand.execute();
                  }
               });
            }
         });
      }
      // if the document is new and unsaved, then resolve that and then
      // show the preview window -- it won't activate in web mode
      // due to popup activation rules but at least it will show up
      else if (isNewDoc())
      {
         saveThenExecute(null, true, CommandUtil.join(showPreviewWindowCommand,
                                                runPreviewCommand));
      }
      // otherwise if it's dirty then show the preview window first (to
      // beat the popup blockers) then save & run
      else if (dirtyState().getValue())
      {
         showPreviewWindowCommand.execute();
         saveThenExecute(null, true, runPreviewCommand);
      }
      // otherwise show the preview window then run the preview
      else
      {
         showPreviewWindowCommand.execute();
         runPreviewCommand.execute();
      }
   }

   private void generateNotebook(final Command executeOnSuccess)
   {
      // default title
      String defaultTitle = docUpdateSentinel_.getProperty(NOTEBOOK_TITLE);
      if (StringUtil.isNullOrEmpty(defaultTitle))
         defaultTitle = FileSystemItem.getNameFromPath(docUpdateSentinel_.getPath());

      // default author
      String defaultAuthor = docUpdateSentinel_.getProperty(NOTEBOOK_AUTHOR);
      if (StringUtil.isNullOrEmpty(defaultAuthor))
      {
         defaultAuthor = state_.compileRNotebookPrefs().getValue().getAuthor();
         if (StringUtil.isNullOrEmpty(defaultAuthor))
            defaultAuthor = session_.getSessionInfo().getUserIdentity();
      }

      // default type
      String defaultType = docUpdateSentinel_.getProperty(NOTEBOOK_TYPE);
      if (StringUtil.isNullOrEmpty(defaultType))
      {
         defaultType = state_.compileRNotebookPrefs().getValue().getType();
         if (StringUtil.isNullOrEmpty(defaultType))
            defaultType = CompileNotebookOptions.TYPE_DEFAULT;
      }

      CompileNotebookOptionsDialog dialog = new CompileNotebookOptionsDialog(
            getId(),
            defaultTitle,
            defaultAuthor,
            defaultType,
            new OperationWithInput<CompileNotebookOptions>()
      {
         @Override
         public void execute(CompileNotebookOptions input)
         {
            server_.createNotebook(
                          input,
                          new SimpleRequestCallback<CompileNotebookResult>()
            {
               @Override
               public void onResponseReceived(CompileNotebookResult response)
               {
                  if (response.getSucceeded())
                  {
                     executeOnSuccess.execute();
                  }
                  else
                  {
                     globalDisplay_.showErrorMessage(
                                       constants_.generateNotebookCaption(),
                                       response.getFailureMessage());
                  }
               }
            });

            // save options for this document
            HashMap<String, String> changedProperties = new HashMap<>();
            changedProperties.put(NOTEBOOK_TITLE, input.getNotebookTitle());
            changedProperties.put(NOTEBOOK_AUTHOR, input.getNotebookAuthor());
            changedProperties.put(NOTEBOOK_TYPE, input.getNotebookType());
            docUpdateSentinel_.modifyProperties(changedProperties, null);

            // save global prefs
            CompileNotebookPrefs prefs = CompileNotebookPrefs.create(
                                          input.getNotebookAuthor(),
                                          input.getNotebookType());
            if (!CompileNotebookPrefs.areEqual(
                                  prefs,
                                  state_.compileRNotebookPrefs().getValue().cast()))
            {
               state_.compileRNotebookPrefs().setGlobalValue(prefs.cast());
               state_.writeState();
            }
         }
      }
      );
      dialog.showModal();
   }

   @Handler
   void onCompileNotebook()
   {
      if (session_.getSessionInfo().getRMarkdownPackageAvailable())
      {
         saveThenExecute(null, true, new Command()
         {
            @Override
            public void execute()
            {
               rmarkdownHelper_.renderNotebookv2(docUpdateSentinel_, null);
            }
         });
      }
      else
      {
         if (!rmarkdownHelper_.verifyPrerequisites("Compile Report",
               view_,
               FileTypeRegistry.RMARKDOWN))
         {
            return;
         }

         doHtmlPreview(new Provider<HTMLPreviewParams>()
         {
            @Override
            public HTMLPreviewParams get()
            {
               return HTMLPreviewParams.create(docUpdateSentinel_.getPath(),
                                               docUpdateSentinel_.getEncoding(),
                                               true,
                                               true,
                                               true);
            }
         });
      }
   }

   @Handler
   void onCompilePDF()
   {
      String pdfPreview = prefs_.pdfPreviewer().getValue();
      boolean showPdf = !pdfPreview.equals(UserPrefs.PDF_PREVIEWER_NONE);
      boolean useInternalPreview =
            pdfPreview.equals(UserPrefs.PDF_PREVIEWER_RSTUDIO);
      boolean useDesktopSynctexPreview =
            pdfPreview.equals(UserPrefs.PDF_PREVIEWER_DESKTOP_SYNCTEX) &&
            Desktop.isDesktop();

      String action = "";
      if (showPdf && !useInternalPreview && !useDesktopSynctexPreview)
         action = "view_external";

      handlePdfCommand(action, useInternalPreview, null);
   }


   @Handler
   void onKnitWithParameters()
   {
      saveThenExecute(null, true, new Command() {
         @Override
         public void execute()
         {
            rmarkdownHelper_.getRMarkdownParamsFile(
               docUpdateSentinel_.getPath(),
               docUpdateSentinel_.getEncoding(),
               activeCodeIsAscii(),
               new CommandWithArg<String>() {
                  @Override
                  public void execute(String paramsFile)
                  {
                     // null return means user cancelled
                     if (paramsFile != null)
                     {
                        // special "none" value means no parameters
                        if (paramsFile.equals("none"))
                        {
                           new RMarkdownNoParamsDialog().showModal();
                        }
                        else
                        {
                           renderRmd(paramsFile);
                        }
                     }
                  }
             });
         }
      });
   }

   @Handler
   void onClearKnitrCache()
   {
      withSavedDoc(new Command() {
         @Override
         public void execute()
         {
            // determine the cache path (use relative path if possible)
            String path = docUpdateSentinel_.getPath();
            FileSystemItem fsi = FileSystemItem.createFile(path);
            path = fsi.getParentPath().completePath(fsi.getStem() + "_cache");
            String relativePath = FileSystemItem.createFile(path).getPathRelativeTo(
                workbenchContext_.getCurrentWorkingDir());
            if (relativePath != null)
               path = relativePath;
            final String docPath = path;

            globalDisplay_.showYesNoMessage(
               MessageDialog.QUESTION,
               constants_.onClearKnitrCacheCaption(),
               constants_.onClearKnitrCacheMessage(docPath),
               false,
               new Operation() {
                  @Override
                  public void execute()
                  {
                     String code = "unlink(" +
                                   ConsoleDispatcher.escapedPath(docPath) +
                                   ", recursive = TRUE)";
                     events_.fireEvent(new SendToConsoleEvent(code, true));
                  }
               },
               null,
               true);

         }

      });


   }


   @Handler
   void onClearPrerenderedOutput()
   {
      withSavedDoc(new Command() {
         @Override
         public void execute()
         {
            // determine the output path (use relative path if possible)
            String path = docUpdateSentinel_.getPath();
            String relativePath = FileSystemItem.createFile(path).getPathRelativeTo(
                workbenchContext_.getCurrentWorkingDir());
            if (relativePath != null)
               path = relativePath;
            final String docPath = path;

            globalDisplay_.showYesNoMessage(
               MessageDialog.QUESTION,
               constants_.onClearPrerenderedOutputCaption(),
               constants_.onClearPrerenderedOutputMessage(docPath),
               false,
               new Operation() {
                  @Override
                  public void execute()
                  {
                     String code = "rmarkdown::shiny_prerendered_clean(" +
                                   ConsoleDispatcher.escapedPath(docPath) +
                                   ")";
                     events_.fireEvent(new SendToConsoleEvent(code, true));
                  }
               },
               null,
               true);
         }
      });
   }


   @Handler
   void onSynctexSearch()
   {
      doSynctexSearch(true);
   }

   private void doSynctexSearch(boolean fromClick)
   {
      SourceLocation sourceLocation = getSelectionAsSourceLocation(fromClick);
      if (sourceLocation == null)
         return;

      // compute the target pdf
      FileSystemItem editorFile = FileSystemItem.createFile(
                                              docUpdateSentinel_.getPath());
      FileSystemItem targetFile = compilePdfHelper_.getTargetFile(editorFile);
      String pdfFile =
         targetFile.getParentPath().completePath(targetFile.getStem() + ".pdf");

      synctex_.forwardSearch(pdfFile, sourceLocation);
   }


   private SourceLocation getSelectionAsSourceLocation(boolean fromClick)
   {
      // get doc path (bail if the document is unsaved)
      String file = docUpdateSentinel_.getPath();
      if (file == null)
         return null;

      Position selPos = docDisplay_.getSelectionStart();
      int line = selPos.getRow() + 1;
      int column = selPos.getColumn() + 1;
      return SourceLocation.create(file, line, column, fromClick);
   }

   @Handler
   void onQuickAddNext()
   {
      docDisplay_.quickAddNext();
   }

   private HasFindReplace getFindReplace()
   {
      if (visualMode_.isActivated())
         return visualMode_.getFindReplace();
      else
         return view_;
   }

   @Handler
   void onFindReplace()
   {
      getFindReplace().showFindReplace(true);
   }

   @Handler
   void onFindNext()
   {
      getFindReplace().findNext();
   }

   @Handler
   void onFindPrevious()
   {
      getFindReplace().findPrevious();
   }

   @Handler
   void onFindSelectAll()
   {
      view_.findSelectAll();
   }

   @Handler
   void onFindFromSelection()
   {
      if (visualMode_.isActivated()) {
         ensureVisualModeActive(() -> {
            visualMode_.getFindReplace().findFromSelection(visualMode_.getSelectedText());
         });
      } else {
         withActiveEditor((disp) ->
         {
            view_.findFromSelection(disp.getSelectionValue());
            disp.focus();
         });
      }

   }

   @Handler
   void onReplaceAndFind()
   {
      getFindReplace().replaceAndFind();
   }

   @Override
   public Position search(String regex)
   {
      return search(Position.create(0, 0), regex);
   }

   @Override
   public Position search(Position startPos, String regex)
   {
      InputEditorSelection sel = docDisplay_.search(regex,
                                                    false,
                                                    false,
                                                    false,
                                                    false,
                                                    startPos,
                                                    null,
                                                    true);
      if (sel != null)
         return docDisplay_.selectionToPosition(sel.getStart());
      else
         return null;
   }


   @Handler
   void onFold()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.fold();
      }
      else if (useScopeTreeFolding())
      {
         Range range = Range.fromPoints(docDisplay_.getSelectionStart(),
                                        docDisplay_.getSelectionEnd());
         if (range.isEmpty())
         {
            // If no selection, fold the innermost non-anonymous scope
            Scope scope = docDisplay_.getCurrentScope();
            while (scope != null && scope.isAnon())
               scope = scope.getParentScope();

            if (scope == null || scope.isTopLevel())
               return;

            docDisplay_.addFoldFromRow(scope.getFoldStart().getRow());
         }
         else
         {
            // If selection, fold the selection
            docDisplay_.addFold(range);
         }
      }
      else
      {
         int row = docDisplay_.getSelectionStart().getRow();
         docDisplay_.addFoldFromRow(row);
      }
   }

   @Handler
   void onUnfold()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.unfold();
      }
      else if (useScopeTreeFolding())
      {
         Range range = Range.fromPoints(docDisplay_.getSelectionStart(),
                                        docDisplay_.getSelectionEnd());
         if (range.isEmpty())
         {
            // If no selection, either:
            //
            // 1) Unfold a fold containing the cursor, or
            // 2) Unfold the closest fold on the current row.
            Position pos = docDisplay_.getCursorPosition();

            AceFold containingCandidate = null;
            AceFold startCandidate = null;
            AceFold endCandidate = null;

            for (AceFold f : JsUtil.asIterable(docDisplay_.getFolds()))
            {
               // Check to see whether this fold contains the cursor position.
               if (f.getRange().contains(pos))
               {
                  if (containingCandidate == null ||
                      containingCandidate.getRange().contains(f.getRange()))
                  {
                     containingCandidate = f;
                  }
               }

               if (startCandidate == null
                   && f.getStart().getRow() == pos.getRow()
                   && f.getStart().getColumn() >= pos.getColumn())
               {
                  startCandidate = f;
               }

               if (startCandidate == null &&
                   f.getEnd().getRow() == pos.getRow() &&
                   f.getEnd().getColumn() <= pos.getColumn())
               {
                  endCandidate = f;
               }
            }

            if (containingCandidate != null)
            {
               docDisplay_.unfold(containingCandidate);
            }
            else if (startCandidate == null ^ endCandidate == null)
            {
               docDisplay_.unfold(startCandidate != null ? startCandidate
                                                          : endCandidate);
            }
            else if (startCandidate != null && endCandidate != null)
            {
               // Both are candidates; see which is closer
               int startDelta = startCandidate.getStart().getColumn() - pos.getColumn();
               int endDelta = pos.getColumn() - endCandidate.getEnd().getColumn();
               docDisplay_.unfold(startDelta <= endDelta? startCandidate
                                                        : endCandidate);
            }
         }
         else
         {
            // If selection, unfold the selection
            docDisplay_.unfold(range);
         }
      }
      else
      {
         int row = docDisplay_.getSelectionStart().getRow();
         docDisplay_.unfold(row);
      }
   }

   @Handler
   void onFoldAll()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.foldAll();
      }
      else  if (useScopeTreeFolding())
      {
         // Fold all except anonymous braces
         HashSet<Integer> rowsFolded = new HashSet<>();
         for (AceFold f : JsUtil.asIterable(docDisplay_.getFolds()))
            rowsFolded.add(f.getStart().getRow());

         ScopeList scopeList = new ScopeList(docDisplay_);
         scopeList.removeAll(ScopeList.ANON_BRACE);
         for (Scope scope : scopeList)
         {
            int row = scope.getFoldStart().getRow();
            if (!rowsFolded.contains(row))
               docDisplay_.addFoldFromRow(row);
         }
      }
      else
      {
         docDisplay_.foldAll();
      }
       
   }

   @Handler
   void onUnfoldAll()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.unfoldAll();
      }
      else  if (useScopeTreeFolding())
      {
         for (AceFold f : JsUtil.asIterable(docDisplay_.getFolds()))
            docDisplay_.unfold(f);
      }
      else
      {
         docDisplay_.unfoldAll();
      }
   }

   @Handler
   void onToggleEditorTokenInfo()
   {
      if (visualMode_.isActivated())
      {
         visualMode_.toggleEditorTokenInfo();
      }
      else
      {
         docDisplay_.toggleTokenInfo();
      }
   }

   boolean useScopeTreeFolding()
   {
      return docDisplay_.hasCodeModelScopeTree();
   }

   void handlePdfCommand(final String completedAction,
                         final boolean useInternalPreview,
                         final Command onBeforeCompile)
   {
      if (fileType_.isRnw() && prefs_.alwaysEnableRnwConcordance().getValue())
         compilePdfHelper_.ensureRnwConcordance();

      // if the document has been previously saved then we should execute
      // the onBeforeCompile command immediately
      final boolean isNewDoc = isNewDoc();
      if (!isNewDoc && (onBeforeCompile != null))
         onBeforeCompile.execute();

      saveThenExecute(null, true, new Command()
      {
         public void execute()
         {
            // if this was a new doc then we still need to execute the
            // onBeforeCompile command
            if (isNewDoc && (onBeforeCompile != null))
               onBeforeCompile.execute();

            String path = docUpdateSentinel_.getPath();
            if (path != null)
            {
               String encoding = StringUtil.notNull(
                                          docUpdateSentinel_.getEncoding());
               fireCompilePdfEvent(path,
                                   encoding,
                                   completedAction,
                                   useInternalPreview);
            }
         }
      });
   }

   private void fireCompilePdfEvent(String path,
                                    String encoding,
                                    String completedAction,
                                    boolean useInternalPreview)
   {
      // first validate the path to make sure it doesn't contain spaces
      FileSystemItem file = FileSystemItem.createFile(path);
      if (file.getName().indexOf(' ') != -1)
      {
         globalDisplay_.showErrorMessage(
               constants_.fireCompilePdfEventErrorCaption(),
               constants_.fireCompilePdfEventErrorMessage(file.getName()));

         return;
      }

      CompilePdfEvent event = new CompilePdfEvent(
                                       compilePdfHelper_.getTargetFile(file),
                                       encoding,
                                       getSelectionAsSourceLocation(false),
                                       completedAction,
                                       useInternalPreview);
      events_.fireEvent(event);
   }

   private Command postSaveCommand(boolean formatOnSave)
   {
      return new Command()
      {
         public void execute()
         {
            Command onFinished = () ->
            {
               isSaving_ = false;
            };
            
            // fire source document saved event
            FileSystemItem file = FileSystemItem.createFile(
                                             docUpdateSentinel_.getPath());
            events_.fireEvent(new SourceFileSaveCompletedEvent(
                                             file,
                                             docUpdateSentinel_.getContents(),
                                             docDisplay_.getCursorPosition()));

            // check for source on save
            if (isSourceOnSaveEnabled() && docUpdateSentinel_.sourceOnSave())
            {
               if (fileType_.isRd())
               {
                  previewRd();
               }
               else if (fileType_.isJS())
               {
                  if (extendedType_ == SourceDocument.XT_JS_PREVIEWABLE)
                     previewJS();
               }
               else if (fileType_.isSql())
               {
                  if (extendedType_ == SourceDocument.XT_SQL_PREVIEWABLE)
                     previewSql();
               }
               else if (fileType_.canPreviewFromR())
               {
                  previewFromR();
               }
               else if (extendedType_ == SourceDocument.XT_RMARKDOWN_DOCUMENT ||
                        extendedType_ == SourceDocument.XT_QUARTO_DOCUMENT)
               {
                  renderRmd();
               }
               else
               {
                  executeRSourceCommand(false, false);
               }
            }
            
            // check for format on save
            if (formatOnSave && formatOnSaveEnabled())
            {
               server_.formatDocument(
                     docUpdateSentinel_.getId(),
                     docUpdateSentinel_.getPath(),
                     new ServerRequestCallback<SourceDocument>()
                     {
                        @Override
                        public void onResponseReceived(SourceDocument response)
                        {
                           revertEdits();
                           onFinished.execute();
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           onFinished.execute();
                        }
                     });
            }
            else
            {
               onFinished.execute();
            }
         }
      };
   }
   
   private boolean formatOnSaveEnabled()
   {
      // TODO: What should we do if a user tries to enable 'Reformat on Save' for a document
      // without actually setting the code formatter? Should we just opt them into using
      // the 'styler' formatter?
      if (docUpdateSentinel_.hasProperty(TextEditingTarget.REFORMAT_ON_SAVE))
         return docUpdateSentinel_.getBoolProperty(TextEditingTarget.REFORMAT_ON_SAVE, false);
      
      String codeFormatter = prefs_.codeFormatter().getValue();
      if (codeFormatter == UserPrefsAccessor.CODE_FORMATTER_NONE)
         return false;
      
      return prefs_.reformatOnSave().getValue();
   }

   private void executeRSourceCommand(boolean forceEcho, boolean focusAfterExec)
   {
      // Hide breakpoint warning bar if visible (since we will re-evaluate
      // breakpoints after source)
      if (docDisplay_.hasBreakpoints())
      {
         hideBreakpointWarningBar();
      }

      if (fileType_.isR() && extendedType_ == SourceDocument.XT_R_CUSTOM_SOURCE)
      {
         // If this R script looks like it has a custom source
         // command, try to execute it; if successful, we're done.
         if (customSource())
            return;
      }

      // Execute the R source() command
      consoleDispatcher_.executeSourceCommand(
                                 docUpdateSentinel_.getPath(),
                                 fileType_,
                                 docUpdateSentinel_.getEncoding(),
                                 activeCodeIsAscii(),
                                 forceEcho,
                                 focusAfterExec,
                                 docDisplay_.hasBreakpoints());
   }

   public void checkForExternalEdit()
   {
      if (!externalEditCheckInterval_.hasElapsed())
         return;
      
      externalEditCheckInterval_.reset();
      externalEditCheckInvalidation_.invalidate();

      // If the doc has never been saved, don't even bother checking
      if (getPath() == null)
         return;

      // If we're already waiting for the user to respond to an edit event, bail
      if (isWaitingForUserResponseToExternalEdit_)
         return;
      
      if (isClosing_)
         return;

      final Invalidation.Token token = externalEditCheckInvalidation_.getInvalidationToken();

      server_.checkForExternalEdit(
            id_,
            new ServerRequestCallback<CheckForExternalEditResult>()
            {
               @Override
               public void onResponseReceived(CheckForExternalEditResult response)
               {
                  if (isClosing_)
                     return;
                  
                  if (token.isInvalid())
                     return;

                  if (response.isDeleted())
                  {
                     if (ignoreDeletes_)
                        return;

                     isWaitingForUserResponseToExternalEdit_ = true;
                     globalDisplay_.showYesNoMessage(
                           GlobalDisplay.MSG_WARNING,
                           constants_.checkForExternalEditFileDeletedCaption(),
                           constants_.checkForExternalEditFileDeletedMessage(
                                   StringUtil.notNull(docUpdateSentinel_.getPath())),
                           false,
                           new Operation()
                           {
                              public void execute()
                              {
                                 isWaitingForUserResponseToExternalEdit_ = false;
                                 CloseEvent.fire(TextEditingTarget.this, null);
                              }
                           },
                           new Operation()
                           {
                              public void execute()
                              {
                                 isWaitingForUserResponseToExternalEdit_ = false;
                                 externalEditCheckInterval_.reset();
                                 ignoreDeletes_ = true;
                                 // Make sure it stays dirty
                                 dirtyState_.markDirty(false);
                              }
                           },
                           true
                     );
                  }
                  else if (response.isModified())
                  {
                     // If we're in a collaborative session, we need to let it
                     // reconcile the modification
                     if (docDisplay_ != null &&
                         docDisplay_.hasActiveCollabSession() &&
                         response.getItem() != null)
                     {
                        events_.fireEvent(new CollabExternalEditEvent(
                              getId(), getPath(),
                              response.getItem().getLastModifiedNative()));
                        return;
                     }

                     ignoreDeletes_ = false; // Now we know it exists

                     // Use StringUtil.formatDate(response.getLastModified())?

                     if (!dirtyState_.getValue())
                     {
                        revertEdits();
                     }
                     else
                     {
                        externalEditCheckInterval_.reset();
                        isWaitingForUserResponseToExternalEdit_ = true;
                        globalDisplay_.showYesNoMessage(
                              GlobalDisplay.MSG_WARNING,
                              constants_.checkForExternalEditFileChangedCaption(),
                              constants_.checkForExternalEditFileChangedMessage(name_.getValue()),
                              false,
                              new Operation()
                              {
                                 public void execute()
                                 {
                                    isWaitingForUserResponseToExternalEdit_ = false;
                                    revertEdits();
                                 }
                              },
                              new Operation()
                              {
                                 public void execute()
                                 {
                                    isWaitingForUserResponseToExternalEdit_ = false;
                                    externalEditCheckInterval_.reset();
                                    docUpdateSentinel_.ignoreExternalEdit();
                                    // Make sure it stays dirty
                                    dirtyState_.markDirty(false);
                                 }
                              },
                              true
                        );
                     }
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }

   public void checkForExternalEdit(int delayMs)
   {
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         public boolean execute()
         {
            if (view_.isAttached())
               checkForExternalEdit();
            return false;
         }
      }, delayMs);
   }

   private void revertEdits()
   {
      docUpdateSentinel_.revert(() -> {
         visualMode_.syncFromEditorIfActivated();
      }, false);
   }

   private SourcePosition toSourcePosition(Scope func)
   {
      Position pos = func.getPreamble();
      return SourcePosition.create(pos.getRow(), pos.getColumn());
   }

   private boolean isCursorInTexMode(DocDisplay display)
   {
      if (fileType_ instanceof TexFileType)
         return true;
      
      if (fileType_.canCompilePDF())
      {
         if (fileType_.isRnw())
         {
            return SweaveFileType.TEX_LANG_MODE.equals(
               display.getLanguageMode(display.getCursorPosition()));
         }
         else
         {
            return true;
         }
      }
      else
      {
         return false;
      }
   }

   private boolean isCursorInRMode(DocDisplay display)
   {
      TextFileType type = display.getFileType();
      if (type != null && type instanceof TexFileType)
         return false;
      
      String mode = display.getLanguageMode(display.getCursorPosition());
      if (mode == null)
         return true;
      
      if (mode.equals(TextFileType.R_LANG_MODE))
         return true;
      
      return false;
   }

   private boolean isCursorInYamlMode(DocDisplay display)
   {
      String mode = display.getLanguageMode(display.getCursorPosition());
      if (mode == null)
         return false;

      if (mode.equals("YAML"))
         return true;

      return false;
   }

   private boolean isNewDoc()
   {
      return docUpdateSentinel_.getPath() == null;
   }

   public static boolean shouldEnforceHardTabs(FileSystemItem item)
   {
      if (item == null)
         return false;

      String[] requiresHardTabs = new String[] {
            "Makefile", "Makefile.in", "Makefile.win",
            "Makevars", "Makevars.in", "Makevars.win"
      };

      for (String file : requiresHardTabs)
         if (file.equals(item.getName()))
            return true;

      if (".tsv".equals(item.getExtension()))
         return true;

      return false;
   }

   private final CppCompletionContext cppCompletionContext_ = new CppCompletionContext() {
      @Override
      public boolean isCompletionEnabled()
      {
         return session_.getSessionInfo().getClangAvailable() &&
                (docUpdateSentinel_.getPath() != null) &&
                fileType_.isC();
      }

      @Override
      public void withUpdatedDoc(final CommandWith2Args<String, String> onUpdated)
      {
         docUpdateSentinel_.withSavedDoc(new Command() {
            @Override
            public void execute()
            {
               onUpdated.execute(docUpdateSentinel_.getPath(),
                                 docUpdateSentinel_.getId());
            }
         });

      }

      @Override
      public void cppCompletionOperation(final CppCompletionOperation operation)
      {
         if (isCompletionEnabled())
         {
            withUpdatedDoc(new CommandWith2Args<String, String>() {
               @Override
               public void execute(String docPath, String docId)
               {
                  Position pos = docDisplay_.getSelectionStart();

                  operation.execute(docPath,
                                    pos.getRow() + 1,
                                    pos.getColumn() + 1);
               }
            });
         }

      }

      @Override
      public String getDocPath()
      {
         if (docUpdateSentinel_ == null)
            return "";

         return docUpdateSentinel_.getPath();
      }
   };

   private final CompletionContext rContext_ = new CompletionContext()
   {
      @Override
      public String getId()
      {
         if (docUpdateSentinel_ == null)
            return null;
         else
            return docUpdateSentinel_.getId();
      }
      
      @Override
      public String getPath()
      {
         if (docUpdateSentinel_ == null)
            return null;
         else
            return docUpdateSentinel_.getPath();
      }

      @Override
      public String getExtendedFileType()
      {
         return extendedType_;
      }

      @Override
      public String[] getQuartoFormats()
      {
         if (SourceDocument.XT_QUARTO_DOCUMENT.equals(extendedType_))
         {
            return getDocumentQuartoOutputFormats().toArray(new String[] {});
         }
         else
         {
            return new String[] {};
         }
      }
      

      @Override
      public String[] getQuartoProjectFormats()
      {
         return getProjectQuartoOutputFormats().toArray(new String[] {});
      }   
      
      @Override 
      public String getQuartoEngine()
      {
         if (SourceDocument.XT_QUARTO_DOCUMENT.equals(extendedType_))
         {
            String yaml = getRmdFrontMatter();
            if (yaml != null)
            {
               // see if we can use a cached parse
               YamlTree tree = null;   
               if (lastYaml_ != null && yaml.equals(lastYaml_))
               {
                  tree = lastTree_;
               }
               else
               {
                  try
                  {
                     tree = new YamlTree(yaml);
                     lastYaml_ = yaml;
                     lastTree_ = tree;
                  }
                  catch(Exception ex)
                  {
                     Debug.log("Warning: Exception thrown while parsing YAML:\n" + yaml);
                  }
               }
               
               // determine engine
               String engine = tree.getKeyValue(RmdFrontMatter.ENGINE_KEY);
               if (engine.length() > 0)
                  return engine;
               if (tree.getKeyValue(RmdFrontMatter.JUPYTER_KEY).length() > 0)
                  return "jupyter";

               // scan chunk engines
               HashSet<String> chunkEngines = new HashSet<String>();
               ScopeList scopeList = new ScopeList(docDisplay_);
               for (int i=0; i<scopeList.size(); i++) 
               {
                  Scope scope = scopeList.get(i);
                  if (scope.isChunk())
                  {  
                     int row = scope.getPreamble().getRow();
                     chunkEngines.add(getEngineForRow(row).toLowerCase());
                  }
               }
               
               // work out engine
               if (chunkEngines.contains("r"))
               {
                  return "knitr";
               }
               else if (chunkEngines.size() == 1 && chunkEngines.contains("ojs"))
               {
                  return "markdown";
               }
               else if (chunkEngines.size() > 0)
               {
                  return "jupyter";
               }
               else
               {
                  return "markdown";
               }               
            }
         }
         
         return null;   
      }
      
      @Override
      public void withSavedDocument(Command command)
      {
         if (docUpdateSentinel_ != null)
         {
            docUpdateSentinel_.withSavedDoc(command);
         }
         else
         {
            command.execute();
         }
      }
      
      // cache front matter parse
      String lastYaml_ = null;
      YamlTree lastTree_ = null;
   };

   public CompletionContext getRCompletionContext()
   {
      return rContext_;
   }

   public CppCompletionContext getCppCompletionContext()
   {
      return cppCompletionContext_;
   }

   public RnwCompletionContext getRnwCompletionContext()
   {
      return compilePdfHelper_;
   }

   public static void syncFontSize(
                              ArrayList<HandlerRegistration> releaseOnDismiss,
                              EventBus events,
                              final TextDisplay view,
                              FontSizeManager fontSizeManager)
   {
      releaseOnDismiss.add(events.addHandler(ChangeFontSizeEvent.TYPE, changeFontSizeEvent ->
      {
         view.setFontSize(changeFontSizeEvent.getFontSize());
      }));
      view.setFontSize(fontSizeManager.getFontSize());

   }

   public static void onPrintSourceDoc(final DocDisplay docDisplay)
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            docDisplay.print();
         }
      });
   }

   public static void addRecordNavigationPositionHandler(
                  ArrayList<HandlerRegistration> releaseOnDismiss,
                  final DocDisplay docDisplay,
                  final EventBus events,
                  final EditingTarget target)
   {
      releaseOnDismiss.add(docDisplay.addRecordNavigationPositionHandler(
            new RecordNavigationPositionEvent.Handler() {
              @Override
              public void onRecordNavigationPosition(
                                         RecordNavigationPositionEvent event)
              {
                 SourcePosition pos = SourcePosition.create(
                                        target.getContext(),
                                        event.getPosition().getRow(),
                                        event.getPosition().getColumn(),
                                        docDisplay.getScrollTop());
                 events.fireEvent(new SourceNavigationEvent(
                                               SourceNavigation.create(
                                                   target.getId(),
                                                   target.getPath(),
                                                   pos)));
              }
           }));
   }

   public Position screenCoordinatesToDocumentPosition(int pageX, int pageY)
   {
      return docDisplay_.screenCoordinatesToDocumentPosition(pageX, pageY);
   }

   public DocDisplay getDocDisplay()
   {
      return docDisplay_;
   }

   private void addAdditionalResourceFiles(List<String> additionalFiles)
   {
      // it does--get the YAML front matter and modify it to include
      // the additional files named in the deployment
      String yaml = getRmdFrontMatter();
      if (yaml == null)
         return;
      
      rmarkdownHelper_.addAdditionalResourceFiles(
            yaml,
            additionalFiles,
            new CommandWithArg<String>()
            {
               @Override
               public void execute(String yamlOut)
               {
                  if (yamlOut != null)
                  {
                     applyRmdFrontMatter(yamlOut);
                  }
               }
            });
   }

   private void syncPublishPath(String path)
   {
      // if we have a view, a type, and a path, sync the view's content publish
      // path to the new content path--note that we need to do this even if the
      // document isn't currently of a publishable type, since it may become
      // publishable once saved.
      if (view_ != null && path != null)
      {
         view_.setPublishPath(extendedType_, path);
      }
   }

   public void setPreferredOutlineWidgetSize(double size)
   {
      state_.documentOutlineWidth().setGlobalValue((int) size);
      state_.writeState();
      docUpdateSentinel_.setProperty(DOC_OUTLINE_SIZE, size + "");
   }

   public double getPreferredOutlineWidgetSize()
   {
      String property = docUpdateSentinel_.getProperty(DOC_OUTLINE_SIZE);
      if (StringUtil.isNullOrEmpty(property))
         return state_.documentOutlineWidth().getGlobalValue();

      try {
         double value = Double.parseDouble(property);

         // Don't allow too-small widget sizes. This helps to protect against
         // a user who might drag the outline width to just a few pixels, and
         // then toggle its visibility by clicking on the 'toggle outline'
         // button. It's unlikely that, realistically, any user would desire an
         // outline width less than ~30 pixels; at minimum we just need to
         // ensure they will be able to see + drag the widget to a larger
         // size if desired.
         if (value < 30)
            return 30;

         return value;
      } catch (Exception e) {
         return state_.documentOutlineWidth().getGlobalValue();
      }
   }

   public void setPreferredOutlineWidgetVisibility(boolean visible)
   {
      docUpdateSentinel_.setProperty(DOC_OUTLINE_VISIBLE, visible ? "1" : "0");
   }

   public boolean getPreferredOutlineWidgetVisibility()
   {
      return getPreferredOutlineWidgetVisibility(prefs_.showDocOutlineRmd().getGlobalValue());
   }

   public boolean getPreferredOutlineWidgetVisibility(boolean defaultValue)
   {
      String property = docUpdateSentinel_.getProperty(DOC_OUTLINE_VISIBLE);
      return StringUtil.isNullOrEmpty(property)
            ? (getTextFileType().isRmd() && defaultValue)
            : Integer.parseInt(property) > 0;
   }

   // similar to get but will write the default value if it's used
   public boolean establishPreferredOutlineWidgetVisibility(boolean defaultValue)
   {
      String property = docUpdateSentinel_.getProperty(DOC_OUTLINE_VISIBLE);
      if (!StringUtil.isNullOrEmpty(property))
      {
         return Integer.parseInt(property) > 0;
      }
      else
      {
         boolean visible = getTextFileType().isRmd() && defaultValue;
         setPreferredOutlineWidgetVisibility(visible);
         return visible;
      }
   }

   public boolean isActiveDocument()
   {
      return commandHandlerReg_ != null;
   }

   public StatusBar getStatusBar()
   {
      return statusBar_;
   }

   public TextEditingTargetNotebook getNotebook()
   {
      return notebook_;
   }

   public VisualMode getVisualMode()
   {
      return visualMode_;
   }

   public EditingTargetCodeExecution getCodeExecutor()
   {
      return codeExecution_;
   }

   /**
    * Updates the path of the file loaded in the editor, as though the user
    * had just saved the file at the new path.
    *
    * @param path New path for the editor
    */
   public void setPath(FileSystemItem path)
   {
      // Find the new type
      TextFileType type = fileTypeRegistry_.getTextTypeForFile(path);

      // Simulate a completed save of the new path
      new SaveProgressIndicator(path, type, false, null).onCompleted();
   }

   private void setRMarkdownBehaviorEnabled(boolean enabled)
   {
      // register idle monitor; automatically creates/refreshes previews
      // of images and LaTeX equations during idle
      if (bgIdleMonitor_ == null && enabled)
      {
         bgIdleMonitor_ = new TextEditingTargetIdleMonitor(this, docUpdateSentinel_);
      }
      else if (bgIdleMonitor_ != null)
      {
         if (enabled)
            bgIdleMonitor_.beginMonitoring();
         else
            bgIdleMonitor_.endMonitoring();
      }

      // set up mathjax
      if (mathjax_ == null && enabled)
         mathjax_ = new MathJax(docDisplay_, docUpdateSentinel_, prefs_);

      if (enabled)
      {
         // auto preview images and equations
         if (inlinePreviewer_ == null)
            inlinePreviewer_ = new InlinePreviewer(
                  this, docUpdateSentinel_, prefs_);
         inlinePreviewer_.preview();

         // sync the notebook's output mode (enable/disable inline output)
         if (notebook_ != null)
            notebook_.syncOutputMode();
      }
      else
      {
         // clean up previewers
         if (inlinePreviewer_ != null)
            inlinePreviewer_.onDismiss();

         // clean up line widgets
         if (notebook_ != null)
            notebook_.onNotebookClearAllOutput();
         docDisplay_.removeAllLineWidgets();
      }
   }

   public void setIntendedAsReadOnly(List<String> alternatives)
   {
      view_.showReadOnlyWarning(alternatives);
   }

   void installShinyTestDependencies(final Command success) {
      server_.installShinyTestDependencies(new ServerRequestCallback<ConsoleProcess>() {
         @Override
         public void onResponseReceived(ConsoleProcess process)
         {
            final ConsoleProgressDialog dialog = new ConsoleProgressDialog(process, server_);
            dialog.showModal();

            process.addProcessExitHandler(new ProcessExitEvent.Handler()
            {
               @Override
               public void onProcessExit(ProcessExitEvent event)
               {
                  if (event.getExitCode() == 0)
                  {
                     success.execute();
                     dialog.closeDialog();
                  }
               }
            });
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            globalDisplay_.showErrorMessage(constants_.installShinyTestDependenciesError(), error.getUserMessage());
         }
      });
   }

   void checkTestPackageDependencies(final Command success, boolean isTestThat) {
      dependencyManager_.withTestPackage(
         new Command()
         {
            @Override
            public void execute()
            {
               if (isTestThat)
                  success.execute();
               else {
                  server_.hasShinyTestDependenciesInstalled(new ServerRequestCallback<Boolean>() {
                     @Override
                     public void onResponseReceived(Boolean hasPackageDependencies)
                     {
                        if (hasPackageDependencies)
                           success.execute();
                        else {
                           globalDisplay_.showYesNoMessage(
                              GlobalDisplay.MSG_WARNING,
                              constants_.checkTestPackageDependenciesCaption(),
                              constants_.checkTestPackageDependenciesMessage(),
                              new Operation()
                              {
                                 public void execute()
                                 {
                                    installShinyTestDependencies(success);
                                 }
                              },
                              false);
                        }
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                        globalDisplay_.showErrorMessage(constants_.checkTestPackageDependenciesError()
                                , error.getMessage());
                     }
                  });
               }
            }
         },
         isTestThat
      );
   }

   @Handler
   void onTestTestthatFile()
   {
      final String buildCommand = "test-file";

      checkTestPackageDependencies(
         new Command()
         {
            @Override
            public void execute()
            {
               save(new Command()
               {
                  @Override
                  public void execute()
                  {
                     server_.startBuild(buildCommand, docUpdateSentinel_.getPath(),
                        new SimpleRequestCallback<Boolean>() {
                        @Override
                        public void onResponseReceived(Boolean response)
                        {

                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           super.onError(error);
                        }
                     });
                  }
               });
            }
         },
         true
      );
   }

   @Handler
   void onTestShinytestFile()
   {
      final String buildCommand = "test-shiny-file";

      checkTestPackageDependencies(
         new Command()
         {
            @Override
            public void execute()
            {
               save(new Command()
               {
                  @Override
                  public void execute()
                  {
                     server_.startBuild(buildCommand, docUpdateSentinel_.getPath(),
                        new SimpleRequestCallback<Boolean>() {
                        @Override
                        public void onResponseReceived(Boolean response)
                        {

                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           super.onError(error);
                        }
                     });
                  }
               });
            }
         },
         false
      );
   }

   @Handler
   void onShinyRecordTest()
   {
      checkTestPackageDependencies(
         new Command()
         {
            @Override
            public void execute()
            {
               String shinyAppPath = FilePathUtils.dirFromFile(docUpdateSentinel_.getPath());

               if (fileType_.canKnitToHTML())
               {
                  shinyAppPath = docUpdateSentinel_.getPath();
               }

               String code = "shinytest::recordTest(\"" + shinyAppPath.replace("\"", "\\\"") + "\")";
               events_.fireEvent(new SendToConsoleEvent(code, true));
            }
         },
         false
      );
   }

   @Handler
   void onShinyRunAllTests()
   {
      checkTestPackageDependencies(
         new Command()
         {
            @Override
            public void execute()
            {
               server_.startBuild("test-shiny", FilePathUtils.dirFromFile(docUpdateSentinel_.getPath()),
                  new SimpleRequestCallback<Boolean>() {
                  @Override
                  public void onResponseReceived(Boolean response)
                  {

                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     super.onError(error);
                  }
               });
            }
         },
         false
      );
   }

   @Handler
   void onShinyCompareTest()
   {
      final String testFile = docUpdateSentinel_.getPath();
      server_.hasShinyTestResults(testFile, new ServerRequestCallback<ShinyTestResults>() {
         @Override
         public void onResponseReceived(ShinyTestResults results)
         {
            if (!results.testDirExists)
            {
               globalDisplay_.showMessage(
                  GlobalDisplay.MSG_INFO,
                       constants_.onShinyCompareTestResponseCaption(),
                       constants_.onShinyCompareTestResponseMessage()
               );
            }
            else
            {
               checkTestPackageDependencies(() ->
               {
                  String testName = FilePathUtils.fileNameSansExtension(testFile);
                  String code = "shinytest::viewTestDiff(\"" +
                        results.appDir + "\", \"" + testName + "\")";
                  events_.fireEvent(new SendToConsoleEvent(code, true));
               }, false);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            globalDisplay_.showErrorMessage(constants_.onShinyCompareTestError(), error.getUserMessage());
         }
      });
   }

   public TextEditingTargetSpelling getSpellingTarget() { return this.spelling_; }

   private void nudgeAutosave()
   {
      // Cancel any existing autosave timer
      autoSaveTimer_.cancel();

      // Bail if not enabled
      if (prefs_.autoSaveOnIdle().getValue() != UserPrefs.AUTO_SAVE_ON_IDLE_COMMIT)
         return;

      // OK, schedule autosave
      autoSaveTimer_.schedule(prefs_.autoSaveMs());
   }

   // logical state (may not be physically activated yet due to async loading)
   public boolean isVisualModeActivated()
   {
      return docUpdateSentinel_.getBoolProperty(RMD_VISUAL_MODE, false);
   }

   // physical state (guaranteed to be loaded and addressable)
   public boolean isVisualEditorActive()
   {
      return visualMode_ != null && visualMode_.isVisualEditorActive();
   }

   /**
    * Prepares to execute code when visual mode is active; ensures that the
    * underlying editor has a complete copy of the code and scope tree.
    *
    * @param onComplete Command to run when sync is complete.
    */
   public void prepareForVisualExecution(Command onComplete)
   {
      if (isVisualEditorActive())
      {
         visualMode_.syncToEditor(SyncType.SyncTypeExecution, onComplete);
      }
      else
      {
         onComplete.execute();
      }
   }

   /**
    * Executes a command with the active Ace instance. If there is no active
    * instance (e.g. in visual mode when focus is not in an editor), then the
    * command is not executed.
    *
    * @param cmd The command to execute.
    */
   public void withActiveEditor(CommandWithArg<DocDisplay> cmd)
   {
      if (isVisualEditorActive())
      {
         DocDisplay activeEditor = visualMode_.getActiveEditor();
         if (activeEditor != null)
         {
            cmd.execute(activeEditor);
         }
      }
      else
      {
         cmd.execute(docDisplay_);
      }
   }

   // user is switching to visual mode
   void onUserSwitchingToVisualMode()
   {
      visualMode_.onUserSwitchingToVisualMode();
   }

   public void getEditorContext()
   {
      if (visualMode_.isActivated())
      {
         ensureVisualModeActive(() ->
         {
            AceEditor activeEditor = AceEditor.getLastFocusedEditor();
            if (activeEditor == null)
            {
               GetEditorContextEvent.SelectionData data =
                     GetEditorContextEvent.SelectionData.create(
                           StringUtil.notNull(getId()),
                           StringUtil.notNull(getPath()),
                           "",
                           JavaScriptObject.createArray().cast());

               server_.getEditorContextCompleted(data, new VoidServerRequestCallback());
               return;
            }

            SourceColumnManager.getEditorContext(
                  getId(),
                  getPath(),
                  activeEditor,
                  server_);
         });
      }
      else
      {
         ensureTextEditorActive(() ->
         {
            SourceColumnManager.getEditorContext(
                  getId(),
                  getPath(),
                  getDocDisplay(),
                  server_);
         });
      }
   }

   public void withEditorSelection(final CommandWithArg<String> callback)
   {
      if (visualMode_.isActivated())
      {
         ensureVisualModeActive(new Command()
         {
            @Override
            public void execute()
            {
               callback.execute(visualMode_.getSelectedText());
            }
         });
      }
      else
      {
         ensureTextEditorActive(new Command()
         {
            @Override
            public void execute()
            {
               callback.execute(docDisplay_.getSelectionValue());
            }
         });
      }
   }
   
   @Override
   public HandlerRegistration addAttachHandler(AttachEvent.Handler event)
   {
      return view_.addAttachHandler(event);
   }

   @Override
   public boolean isAttached()
   {
      return view_.isAttached();
   }

   private StatusBar statusBar_;
   private final DocDisplay docDisplay_;
   private final UserPrefs prefs_;
   private final UserState state_;
   private Display view_;
   private final Commands commands_;
   private final SourceServerOperations server_;
   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final FileDialogs fileDialogs_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final FileTypeCommands fileTypeCommands_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final WorkbenchContext workbenchContext_;
   private final Session session_;
   private final Synctex synctex_;
   private final FontSizeManager fontSizeManager_;
   private final Source source_;
   private final DependencyManager dependencyManager_;
   private DocUpdateSentinel docUpdateSentinel_;
   private final Value<String> name_ = new Value<>(null);
   private TextFileType fileType_;
   private String id_;
   private HandlerRegistration commandHandlerReg_;
   private final ArrayList<HandlerRegistration> releaseOnDismiss_ = new ArrayList<>();
   private final DirtyState dirtyState_;
   private final HandlerManager handlers_ = new HandlerManager(this);
   private FileSystemContext fileContext_;
   private final TextEditingTargetCompilePdfHelper compilePdfHelper_;
   private final TextEditingTargetRMarkdownHelper rmarkdownHelper_;
   private final TextEditingTargetCppHelper cppHelper_;
   private final TextEditingTargetJSHelper jsHelper_;
   private final TextEditingTargetSqlHelper sqlHelper_;
   private final TextEditingTargetPresentationHelper presentationHelper_;
   private final TextEditingTargetPresentation2Helper presentation2Helper_;
   private final TextEditingTargetRHelper rHelper_;
   private VisualMode visualMode_;
   private final TextEditingTargetQuartoHelper quartoHelper_;
   private TextEditingTargetIdleMonitor bgIdleMonitor_;
   private TextEditingTargetThemeHelper themeHelper_;
   private boolean ignoreDeletes_;
   private boolean forceSaveCommandActive_ = false;
   private final TextEditingTargetScopeHelper scopeHelper_;
   private final TextEditingTargetCopilotHelper copilotHelper_;
   private TextEditingTargetPackageDependencyHelper packageDependencyHelper_;
   private TextEditingTargetSpelling spelling_;
   private TextEditingTargetNotebook notebook_;
   private TextEditingTargetChunks chunks_;
   private final BreakpointManager breakpointManager_;
   private final LintManager lintManager_;
   private CollabEditStartParams queuedCollabParams_;
   private MathJax mathjax_;
   private InlinePreviewer inlinePreviewer_;
   private ProjectConfig projConfig_;

   // Allows external edit checks to supercede one another
   private final Invalidation externalEditCheckInvalidation_ =
         new Invalidation();
   // Prevents external edit checks from happening too soon after each other
   private final IntervalTracker externalEditCheckInterval_ =
         new IntervalTracker(1000, true);
   private boolean isWaitingForUserResponseToExternalEdit_ = false;
   private EditingTargetCodeExecution codeExecution_;

   // Timer for autosave
   private final Timer autoSaveTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         // It's unlikely, but if we attempt to autosave while running a
         // previous autosave, just nudge the timer so we try again.
         if (autoSaveInitiatedTime_ != 0)
         {
            // If we've been trying to save for more than 5 seconds, we won't
            // nudge (just fall through and we'll attempt again below)
            if (System.currentTimeMillis() - autoSaveInitiatedTime_ < 5000)
            {
               nudgeAutosave();
               return;
            }
         }
         
         // If a save is already in progress via some other mechanism, skip this.
         if (isSaving_)
            return;

         // If this is an untitled document, then there's nothing to save.
         if (getPath() == null)
            return;

         // Everyone's autosave gets turned off during a collab session --
         // otherwise the autosaves all fire at once and fight
         if (docDisplay_.hasActiveCollabSession())
            return;

         // Save (and keep track of when we initiated it)
         autoSaveInitiatedTime_ = System.currentTimeMillis();
         try
         {
            autoSave(this::onCompleted, this::onSilentFailure);
         }
         catch (Exception e)
         {
            // Autosave exceptions are logged rather than displayed
            autoSaveInitiatedTime_ = 0;
            Debug.logException(e);
         }
      }
      
      private void onCompleted()
      {
         autoSaveInitiatedTime_ = 0;
      }
      
      private void onSilentFailure()
      {
         // if this autosave operation silently fails, we want to automatically restart it
         autoSaveInitiatedTime_ = 0;
         nudgeAutosave();
      }
   };
   
   private boolean isAutoSaving()
   {
      return autoSaveInitiatedTime_ != 0;
   }

   private HandlerRegistration documentDirtyHandler_ = null;
   private SourcePosition debugStartPos_ = null;
   private SourcePosition debugEndPos_ = null;
   private boolean isDebugWarningVisible_ = false;
   private boolean isBreakpointWarningVisible_ = false;
   private String extendedType_;

   // prevent multiple manual saves from queuing up
   private boolean documentChangedDuringDebugSession_ = false;
   private boolean isSaving_ = false;
   private long autoSaveInitiatedTime_ = 0;
   
   // track whether we're now closing the document
   private boolean isClosing_ = false;

   private abstract class RefactorServerRequestCallback
           extends ServerRequestCallback<JsArrayString>
   {
      private final String refactoringName_;

      public RefactorServerRequestCallback(String refactoringName)
      {
         refactoringName_ = refactoringName;
      }

      @Override
      public void onResponseReceived(final JsArrayString response)
      {
         doExtract(response);
      }

      @Override
      public void onError(ServerError error)
      {
         globalDisplay_.showYesNoMessage(
                 GlobalDisplay.MSG_WARNING,
                 refactoringName_,
                 constants_.refactorServerRequestCallbackError(),
                 new Operation()
                 {
                    public void execute()
                    {
                       doExtract(null);
                    }
                 },
                 false);
      }

      abstract void doExtract(final JsArrayString response);
   }

   private static final String PROPERTY_CURSOR_POSITION = "cursorPosition";
   private static final String PROPERTY_SCROLL_LINE = "scrollLine";
   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);
}
