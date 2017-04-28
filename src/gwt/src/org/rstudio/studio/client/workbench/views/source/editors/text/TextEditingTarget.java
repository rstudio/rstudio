/*
 * TextEditingTarget.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

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
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.resources.client.ImageResource;
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

import org.rstudio.core.client.*;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureHeightHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsMap;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.ChangeFontSizeHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ResetEditorCommandsEvent;
import org.rstudio.studio.client.application.events.SetEditorCommandBindingsEvent;
import org.rstudio.studio.client.common.*;
import org.rstudio.studio.client.common.debugging.BreakpointManager;
import org.rstudio.studio.client.common.debugging.events.BreakpointsSavedEvent;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.SweaveFileType;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.mathjax.MathJax;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.SynctexUtils;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.htmlpreview.events.ShowHTMLPreviewEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.notebook.CompileNotebookOptions;
import org.rstudio.studio.client.notebook.CompileNotebookOptionsDialog;
import org.rstudio.studio.client.notebook.CompileNotebookPrefs;
import org.rstudio.studio.client.notebook.CompileNotebookResult;
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
import org.rstudio.studio.client.rmarkdown.ui.RmdTemplateOptionsDialog;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.events.LaunchShinyApplicationEvent;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.model.ShinyViewerType;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.output.compilepdf.events.CompilePdfEvent;
import org.rstudio.studio.client.workbench.views.output.lint.LintManager;
import org.rstudio.studio.client.workbench.views.presentation.events.SourceFileSaveCompletedEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;
import org.rstudio.studio.client.workbench.views.source.SourceBuildHelper;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetCodeExecution;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper.RmdSelectedTemplate;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceAfterCommandExecutedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.VimMarks;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionOperation;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkExecUnit;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.events.InterruptChunkEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar.HideMessageHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupMenu;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupRequest;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.RMarkdownNoParamsDialog;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.CollabExternalEditEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocFocusedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragStateChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocWindowChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionEvent;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionHandler;
import org.rstudio.studio.client.workbench.views.source.events.SourceFileSavedEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.*;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsDiffEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsHistoryEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRevertFileEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsViewOnGitHubEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.model.GitHubViewRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TextEditingTarget implements 
                                  EditingTarget,
                                  EditingTargetCodeExecution.CodeExtractor
{
   interface MyCommandBinder
         extends CommandBinder<Commands, TextEditingTarget>
   {
   }

   private static final String NOTEBOOK_TITLE = "notebook_title";
   private static final String NOTEBOOK_AUTHOR = "notebook_author";
   private static final String NOTEBOOK_TYPE = "notebook_type";
   
   public final static String DOC_OUTLINE_SIZE    = "docOutlineSize";
   public final static String DOC_OUTLINE_VISIBLE = "docOutlineVisible";

   private static final MyCommandBinder commandBinder =
         GWT.create(MyCommandBinder.class);

   public interface Display extends TextDisplay, 
                                    WarningBarDisplay,
                                    HasEnsureVisibleHandlers,
                                    HasEnsureHeightHandlers,
                                    HasResizeHandlers
   {
      HasValue<Boolean> getSourceOnSave();
      void ensureVisible();
      void showFindReplace(boolean defaultForward);
      void findNext();
      void findPrevious();
      void findSelectAll();
      void findFromSelection();
      void replaceAndFind();
      
      StatusBar getStatusBar();

      boolean isAttached();
      
      void adaptToExtendedFileType(String extendedType);
      void onShinyApplicationStateChanged(String state);

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
      HandlerRegistration addRmdFormatChangedHandler(
            RmdOutputFormatChangedEvent.Handler handler);
      
      void setPublishPath(String type, String publishPath);
      void invokePublish();
      
      void initWidgetSize();
      
      void toggleDocumentOutline();
      
      void setNotebookUIVisible(boolean visible);
   }

   private class SaveProgressIndicator implements ProgressIndicator
   {

      public SaveProgressIndicator(FileSystemItem file,
                                   TextFileType fileType,
                                   Command executeOnSuccess)
      {
         file_ = file;
         newFileType_ = fileType;
         executeOnSuccess_ = executeOnSuccess;
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
         // don't need to check again soon because we just saved
         // (without this and when file monitoring is active we'd
         // end up immediately checking for external edits)
         externalEditCheckInterval_.reset(250);

         if (newFileType_ != null)
            fileType_ = newFileType_;

         if (file_ != null)
         {
            ignoreDeletes_ = false;
            forceSaveCommandActive_ = false;
            commands_.reopenSourceDocWithEncoding().setEnabled(true);
            name_.setValue(file_.getName(), true);
            // Make sure tooltip gets updated, even if name hasn't changed
            name_.fireChangeEvent();

            // If we were dirty prior to saving, clean up the debug state so
            // we don't continue highlighting after saving. (There are cases
            // in which we want to restore highlighting after the dirty state
            // is marked clean--i.e. when unwinding the undo stack.)
            if (dirtyState_.getValue())
               endDebugHighlighting();

            dirtyState_.markClean();
         }

         if (newFileType_ != null)
         {
            // Make sure the icon gets updated, even if name hasn't changed
            name_.fireChangeEvent();
            updateStatusBarLanguage();
            view_.adaptToFileType(newFileType_);
            
            // turn R Markdown behavior (inline execution, previews, etc.)
            // based on whether we just became an R Markdown type
            setRMarkdownBehaviorEnabled(newFileType_.isRmd());

            events_.fireEvent(new FileTypeChangedEvent());
            if (!fileType_.canSourceOnSave() && docUpdateSentinel_.sourceOnSave())
            {
               view_.getSourceOnSave().setValue(false, true);
            }
         }

         if (executeOnSuccess_ != null)
            executeOnSuccess_.execute();
      }

      public void onError(final String message)
      {
         // in case the error occured saving a document that wasn't 
         // in the foreground
         view_.ensureVisible();
         
         // command to show the error
         final Command showErrorCommand = new Command() {
            @Override
            public void execute()
            {
               globalDisplay_.showErrorMessage("Error Saving File",
                                               message);
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
                     String message = "This source file is read-only " +
                                      "so changes cannot be saved";
                     view_.showWarningBar(message);
                     
                     String saveAsPath = file_.getParentPath().completePath(
                           file_.getStem() + "-copy" + file_.getExtension());
                     saveNewFile(
                           saveAsPath, 
                           null, 
                           CommandUtil.join(postSaveCommand(), new Command() {

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
      private final Command executeOnSuccess_;
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
                            UIPrefs prefs, 
                            BreakpointManager breakpointManager,
                            SourceBuildHelper sourceBuildHelper,
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
      sourceBuildHelper_ = sourceBuildHelper;
      dependencyManager_ = dependencyManager;

      docDisplay_ = docDisplay;
      dirtyState_ = new DirtyState(docDisplay_, false);
      lintManager_ = new LintManager(this, cppCompletionContext_);
      prefs_ = prefs;
      compilePdfHelper_ = new TextEditingTargetCompilePdfHelper(docDisplay_);
      rmarkdownHelper_ = new TextEditingTargetRMarkdownHelper();
      cppHelper_ = new TextEditingTargetCppHelper(cppCompletionContext_, 
                                                  docDisplay_);
      presentationHelper_ = new TextEditingTargetPresentationHelper(
                                                                  docDisplay_);
      reformatHelper_ = new TextEditingTargetReformatHelper(docDisplay_);
      renameHelper_ = new TextEditingTargetRenameHelper(docDisplay_);
      
      docDisplay_.setRnwCompletionContext(compilePdfHelper_);
      docDisplay_.setCppCompletionContext(cppCompletionContext_);
      docDisplay_.setRCompletionContext(rContext_);
      scopeHelper_ = new TextEditingTargetScopeHelper(docDisplay_);
      
      addRecordNavigationPositionHandler(releaseOnDismiss_, 
                                         docDisplay_, 
                                         events_, 
                                         this);
       
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
                     !prefs_.useVimMode().getValue())
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
            else if (
                  prefs_.continueCommentsOnNewline().getValue() && 
                  !docDisplay_.isPopupVisible() &&
                  ne.getKeyCode() == KeyCodes.KEY_ENTER && mod == 0 &&
                    (fileType_.isC() || isCursorInRMode() || isCursorInTexMode()))
            {
               String line = docDisplay_.getCurrentLineUpToCursor();
               Pattern pattern = null;
               
               if (isCursorInRMode())
                  pattern = Pattern.create("^(\\s*#+'?\\s*)");
               else if (isCursorInTexMode())
                  pattern = Pattern.create("^(\\s*%+'?\\s*)");
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
               docDisplay_.goToFunctionDefinition();
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
      
      events_.addHandler(
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
                            ShinyViewerType.SHINY_VIEWER_PANE &&
                         event.getParams().getViewerType() != 
                            ShinyViewerType.SHINY_VIEWER_WINDOW)
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
                     view_.showWarningBar("Breakpoints can only be set inside "+
                                          "the body of a function. ");
                  }
                  docDisplay_.removeBreakpoint(breakpoint);
               }
            }
            updateBreakpointWarningBar();
         }
      });
      
      events_.addHandler(ConvertToShinyDocEvent.TYPE, 
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
      
      events_.addHandler(RSConnectDeployInitiatedEvent.TYPE, 
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
            ResetEditorCommandsEvent.TYPE,
            new ResetEditorCommandsEvent.Handler()
            {
               @Override
               public void onResetEditorCommands(ResetEditorCommandsEvent event)
               {
                  getDocDisplay().resetCommands();
               }
            });
      
      events_.addHandler(DocTabDragStateChangedEvent.TYPE, 
            new DocTabDragStateChangedEvent.Handler()
            {
               
               @Override
               public void onDocTabDragStateChanged(
                     DocTabDragStateChangedEvent e)
               {
                  docDisplay_.setDragEnabled(e.getState() == 
                        DocTabDragStateChangedEvent.STATE_NONE);
               }
            });
      
      events_.addHandler(
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
   
   @Handler
   void onGoToPrevSection()
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
   
   @Handler
   void onGoToNextChunk()
   {
      moveCursorToNextSectionOrChunk(false);
   }
   
   @Handler
   void onGoToPrevChunk()
   {
      moveCursorToPreviousSectionOrChunk(false);
   }
   
   @Override
   public void recordCurrentNavigationPosition()
   {
      docDisplay_.recordCurrentNavigationPosition();
   }
   
   @Override
   public void navigateToPosition(SourcePosition position, 
                                  boolean recordCurrent)
   {
      docDisplay_.navigateToPosition(position, recordCurrent);
   }
   
   @Override
   public void navigateToPosition(SourcePosition position, 
                                  boolean recordCurrent,
                                  boolean highlightLine)
   {
      docDisplay_.navigateToPosition(position, recordCurrent, highlightLine);
   }

   @Override
   public void restorePosition(SourcePosition position)
   {
      docDisplay_.restorePosition(position);
   }
   
   @Override
   public SourcePosition currentPosition()
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
   
   @Override
   public boolean isAtSourceRow(SourcePosition position)
   {
      return docDisplay_.isAtSourceRow(position);
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
   public void highlightDebugLocation(
         SourcePosition startPos,
         SourcePosition endPos,
         boolean executing)
   {
      debugStartPos_ = startPos;
      debugEndPos_ = endPos;
      docDisplay_.highlightDebugLocation(startPos, endPos, executing);
      updateDebugWarningBar();
   }

   @Override
   public void endDebugHighlighting()
   {
      docDisplay_.endDebugHighlighting();      
      debugStartPos_ = null;
      debugEndPos_ = null;
      updateDebugWarningBar();
   }
   
   @Override
   public void beginCollabSession(CollabEditStartParams params)
   {
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
               "Join Edit Session", 
               "You have unsaved changes to " + filename + ", but another " +
               "user is editing the file. Do you want to discard your " + 
               "changes and join their edit session, or make your own copy " +
               "of the file to work on?",
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
               "Discard and Join", 
               "Work on a Copy", 
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
   
   private void updateDebugWarningBar()
   {
      // show the warning bar if we're debugging and the document is dirty
      if (debugStartPos_ != null && 
          dirtyState().getValue() && 
          !isDebugWarningVisible_)
      {
         view_.showWarningBar("Debug lines may not match because the file contains unsaved changes.");
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
   
   public void showWarningMessage(String message)
   {
      view_.showWarningBar(message);
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

   public void initialize(final SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
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
      
      themeHelper_ = new TextEditingTargetThemeHelper(this, events_);
      
      docUpdateSentinel_ = new DocUpdateSentinel(
            server_,
            docDisplay_,
            document,
            globalDisplay_.getProgressIndicator("Save File"),
            dirtyState_,
            events_);
      
      view_ = new TextEditingTargetWidget(this,
                                          docUpdateSentinel_,
                                          commands_,
                                          prefs_,
                                          fileTypeRegistry_,
                                          docDisplay_,
                                          fileType_,
                                          extendedType_,
                                          events_,
                                          session_);

      roxygenHelper_ = new RoxygenHelper(docDisplay_, view_);
      
      // create notebook and forward resize events
      chunks_ = new TextEditingTargetChunks(this);
      notebook_ = new TextEditingTargetNotebook(this, chunks_, view_, 
            docDisplay_, dirtyState_, docUpdateSentinel_, document, 
            releaseOnDismiss_, dependencyManager_);
      view_.addResizeHandler(notebook_);
      
      // ensure that Makefile and Makevars always use tabs
      name_.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if ("Makefile".equals(event.getValue()) ||
                "Makefile.in".equals(event.getValue()) ||
                "Makefile.win".equals(event.getValue()) ||
                "Makevars".equals(event.getValue()) ||
                "Makevars.in".equals(event.getValue()) ||
                "Makevars.win".equals(event.getValue()))
            {
               docDisplay_.setUseSoftTabs(false);
            }
         }
      });
      
      name_.setValue(getNameFromDocument(document, defaultNameProvider), true);
      String contents = document.getContents();
      docDisplay_.setCode(contents, false);
      
      // Load and apply folds.
      final ArrayList<Fold> folds = Fold.decode(document.getFoldSpec());
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            for (Fold fold : folds)
               docDisplay_.addFold(fold.getRange());
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

      registerPrefs(releaseOnDismiss_, prefs_, docDisplay_, document);
      
      // Initialize sourceOnSave, and keep it in sync
      view_.getSourceOnSave().setValue(document.sourceOnSave(), false);
      view_.getSourceOnSave().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            docUpdateSentinel_.setSourceOnSave(
                  event.getValue(),
                  globalDisplay_.getProgressIndicator("Error Saving Setting"));
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
            Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
            {
               public boolean execute()
               {
                  if (view_.isAttached())
                     checkForExternalEdit();
                  return false;
               }
            }, 500);
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
                  
                  // don't try to set breakpoints in unsaved code
                  if (isNewDoc())
                  {
                     view_.showWarningBar("Breakpoints cannot be set until " +
                                          "the file is saved.");
                     return;
                  }
                  
                  // don't try to set breakpoints if the R version is too old
                  if (!session_.getSessionInfo().getHaveSrcrefAttribute())
                  {
                     view_.showWarningBar("Editor breakpoints require R 2.14 " +
                                          "or newer.");
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
     

      final String rTypeId = FileTypeRegistry.R.getTypeId();
      releaseOnDismiss_.add(prefs_.softWrapRFiles().addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               public void onValueChange(ValueChangeEvent<Boolean> evt)
               {
                  if (fileType_.getTypeId().equals(rTypeId))
                     view_.adaptToFileType(fileType_);
               }
            }
      ));

      releaseOnDismiss_.add(events_.addHandler(FileChangeEvent.TYPE,
                                               new FileChangeHandler() {
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
            if (commandHandlerReg_ != null)
            {
               checkForExternalEdit();
            }

            // also check for changes on modifications if we are not dirty
            // note that we don't check for changes on removed files because
            // this will show a confirmation dialog
            else if (event.getFileChange().getType() == FileChange.MODIFIED &&
                     dirtyState().getValue() == false)
            {
               checkForExternalEdit();
            }
         }
      }));
      
      spelling_ = new TextEditingTargetSpelling(docDisplay_, 
                                                docUpdateSentinel_);
      

      // show/hide the debug toolbar when the dirty state changes. (note:
      // this doesn't yet handle the case where the user saves the document,
      // in which case we should still show some sort of warning.)
      dirtyState().addValueChangeHandler(new ValueChangeHandler<Boolean>()
            {
               public void onValueChange(ValueChangeEvent<Boolean> evt)
               {
                  updateDebugWarningBar();
               }
            }
      );
      
      // find all of the debug breakpoints set in this document and replay them
      // onto the edit surface
      ArrayList<Breakpoint> breakpoints = 
            breakpointManager_.getBreakpointsInFile(getPath());
      for (Breakpoint breakpoint: breakpoints)
      {
         docDisplay_.addOrUpdateBreakpoint(breakpoint);
      }
      
      if (extendedType_.equals(SourceDocument.XT_RMARKDOWN))
      {
         // populate the popup menu with a list of available formats
         updateRmdFormatList();
         setRMarkdownBehaviorEnabled(true);
      }
      
      view_.addRmdFormatChangedHandler(new RmdOutputFormatChangedEvent.Handler()
      {
         @Override
         public void onRmdOutputFormatChanged(RmdOutputFormatChangedEvent event)
         {
            setRmdFormat(event.getFormat());
         }
      });
      
      docDisplay_.addCursorChangedHandler(new CursorChangedHandler()
      {
         Timer timer_ = new Timer()
         {
            @Override
            public void run()
            {
               HashMap<String, String> properties = new HashMap<String, String>();
               
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
            timer_.schedule(1000);
         }
      });
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
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
            message = "Breakpoints will be activated when the file or " +
                      "function is finished executing.";
         }
         else if (isPackageFile())
         {
            message = "Breakpoints will be activated when the package is " +
                      "built and reloaded.";
         }
         else if (hasPackagePendingBreakpoints)
         {
            message = "Breakpoints will be activated when an updated version " +
                      "of the " + pendingPackageName + " package is loaded";
         }
         else
         {
            message = "Breakpoints will be activated when this file is " + 
                      "sourced.";
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
      docDisplay_.addCursorChangedHandler(new CursorChangedHandler()
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
            TextFileType[] fileTypes = fileTypeCommands_.statusBarFileTypes();
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
            JsArray<Scope> tree = docDisplay_.getScopeTree();
            final StatusBarPopupMenu menu = new StatusBarPopupMenu();
            MenuItem defaultItem = null;
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
            else
            {
               defaultItem = addFunctionsToMenu(
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
               docUpdateSentinel_.changeFileType(
                     type.getTypeId(),
                     new SaveProgressIndicator(null, type, null));  
               
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                  @Override
                  public void execute()
                  {
                     focus(); 
                  } 
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
         String type = fileType_.canExecuteChunks() ? "chunks" : "functions";
         MenuItem noFunctions = new MenuItem("(No " + type + " defined)",
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
                func.getLabel().equals(defaultFunction.getLabel()) &&
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
  
   private void updateCurrentScope()
   {
      if (fileType_ == null || !fileType_.canShowScopeTree())
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
                                      Provider<String> defaultNameProvider)
   {
      if (document.getPath() != null)
         return FileSystemItem.getNameFromPath(document.getPath());

      String name = document.getProperties().getString("tempName");
      if (!StringUtil.isNullOrEmpty(name))
         return name;

      String defaultName = defaultNameProvider.get();
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

   public void insertCode(String source, boolean blockMode)
   {
      docDisplay_.insertCode(source, blockMode);
   }

   public HashSet<AppCommand> getSupportedCommands()
   {
      return fileType_.getSupportedCommands(commands_);
   }
   
   @Override
   public void manageCommands()
   {
      if (fileType_.isRmd())
         notebook_.manageCommands();
   }
   
   @Override
   public boolean canCompilePdf()
   {
      return fileType_.canCompilePDF();
   }
   
   
   @Override
   public void verifyCppPrerequisites()
   {
      // NOTE: will be a no-op for non-c/c++ file types
      cppHelper_.checkBuildCppDependencies(this, view_, fileType_);
   }
   

   public void focus()
   {
      docDisplay_.focus();
   }
   
   public String getSelectedText()
   {
      if (docDisplay_.hasSelection())
         return docDisplay_.getSelectionValue();
      else
         return "";
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return view_.addEnsureVisibleHandler(handler);
   }
   
   public HandlerRegistration addEnsureHeightHandler(EnsureHeightHandler handler)
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
         docDisplay_.recordCurrentNavigationPosition();
      }
      catch(Exception e)
      {
         Debug.log("Exception recording nav position: " + e.toString());
      }
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
                         getName().getValue() + " - Active Following Session",
                         "You're actively following another user's cursor " +
                         "in '" + getName().getValue() + "'.\n\n" +
                         "If you close this file, you won't see their " + 
                         "cursor until they edit another file.",
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
                         "Close Anyway",
                         "Cancel",
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
      save(new Command() {
         @Override
         public void execute()
         {   
         }});
   }
   
   public void save(Command onCompleted)
   {
      saveThenExecute(null, CommandUtil.join(postSaveCommand(), 
                                             onCompleted));
   }
   
   public void saveWithPrompt(final Command command, final Command onCancelled)
   {
      view_.ensureVisible();
      
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_WARNING,
                      getName().getValue() + " - Unsaved Changes",
                      "The document '" + getName().getValue() + 
                      "' has unsaved changes.\n\n" +
                      "Do you want to save these changes?",
                      true,
                      new Operation() {
                         public void execute() { saveThenExecute(null, command); }
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
                      "Save",
                      "Don't Save",
                      true);   
   }
   
   public void revertChanges(Command onCompleted)
   {
      docUpdateSentinel_.revert(onCompleted);
   }

   public void saveThenExecute(String encodingOverride, final Command command)
   {
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
                  fixupCodeBeforeSaving();
                  
                  docUpdateSentinel_.save(path,
                                          null,
                                          encoding,
                                          new SaveProgressIndicator(
                                                FileSystemItem.createFile(path),
                                                null,
                                                command
                                          ));
               }
            });
   }

   private void saveNewFile(final String suggestedPath,
                            String encodingOverride,
                            final Command executeOnSuccess)
   {
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
      final String encoding = StringUtil.firstNotNullOrEmpty(new String[] {
            encodingOverride,
            docUpdateSentinel_.getEncoding(),
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
      view_.ensureVisible();;
      
      server_.iconvlist(new SimpleRequestCallback<IconvListResult>()
      {
         @Override
         public void onResponseReceived(IconvListResult response)
         {
            // Stupid compiler. Use this Value shim to make the dialog available
            // in its own handler.
            final HasValue<ChooseEncodingDialog> d = new Value<ChooseEncodingDialog>(null);
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
                           prefs_.writeUIPrefs();
                        }

                        command.execute(newEncoding);
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
      view_.ensureVisible();
      
      FileSystemItem fsi;
      if (suggestedPath != null)
         fsi = FileSystemItem.createFile(suggestedPath);
      else
         fsi = getSaveFileDefaultDir();
 
      fileDialogs_.saveFile(
            "Save File - " + getName().getValue(),
            fileContext_,
            fsi,
            fileType_.getDefaultExtension(),
            false,
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(final FileSystemItem saveItem,
                                   ProgressIndicator indicator)
               {
                  if (saveItem == null)
                     return;

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
                           if (!getPath().equals(saveItem.getPath()))
                           {
                              // breakpoints are file-specific, so when saving
                              // as a different file, clear the display of
                              // breakpoints from the old file name
                              docDisplay_.removeAllBreakpoints();
                              
                              // update publish settings 
                              syncPublishPath(saveItem.getPath());
                           }
                           
                           fixupCodeBeforeSaving();
                                 
                           docUpdateSentinel_.save(
                                 saveItem.getPath(),
                                 fileType.getTypeId(),
                                 encoding,
                                 new SaveProgressIndicator(saveItem,
                                                           fileType,
                                                           executeOnSuccess));

                           events_.fireEvent(
                                 new SourceFileSavedEvent(getId(),
                                       saveItem.getPath()));
                        }
 
                     };
                     
                     // if we are switching from an R file type 
                     // to a non-R file type then confirm
                     if (fileType_.isR() && !fileType.isR())
                     {
                        globalDisplay_.showYesNoMessage(
                              MessageDialog.WARNING, 
                              "Confirm Change File Type", 
                              "This file was created as an R script however " +
                              "the file extension you specified will change " +
                              "it into another file type that will no longer " +
                              "open as an R script.\n\n" +
                              "Are you sure you want to change the type of " +
                              "the file so that it is no longer an R script?",
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
   
   
   private void fixupCodeBeforeSaving()
   { 
      int lineCount = docDisplay_.getRowCount();
      if (lineCount < 1)
         return;
      
      if (docDisplay_.hasActiveCollabSession())
      {
         // mutating the code (especially as below where the entire document 
         // contents are changed) during a save operation inside a collaborative
         // editing session would require some nuanced orchestration so for now
         // these preferences don't apply to shared editing sessions
         return;
      }
      
      
      if (prefs_.stripTrailingWhitespace().getValue() &&
          !fileType_.isMarkdown() &&
          !name_.getValue().equals("DESCRIPTION"))
      {
         String code = docDisplay_.getCode();
         Pattern pattern = Pattern.create("[ \t]+$");
         String strippedCode = pattern.replaceAll(code, "");
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
      
      if (prefs_.autoAppendNewline().getValue() || fileType_.isPython())
      {
         String lastLine = docDisplay_.getLine(lineCount - 1);
         if (lastLine.length() != 0)
            docDisplay_.insertCode(docDisplay_.getEnd().getEnd(), "\n");
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

   public void onDismiss(int dismissType)
   {
      docUpdateSentinel_.stop();
      
      if (spelling_ != null)
         spelling_.onDismiss();
      
      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();
      
      docDisplay_.endCollabSession();

      codeExecution_.detachLastExecuted();

      if (notebook_ != null)
         notebook_.onDismiss();
      
      if (inlinePreviewer_ != null)
         inlinePreviewer_.onDismiss();
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
         (fileType_.canSourceOnSave() && docUpdateSentinel_.sourceOnSave());
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
      view_.adaptToExtendedFileType(extendedType);
      if (extendedType.equals(SourceDocument.XT_RMARKDOWN))
         updateRmdFormatList();
      extendedType_ = extendedType;

      // extended type can affect publish options
      syncPublishPath(docUpdateSentinel_.getPath());
   }

   @Override
   public String getExtendedFileType()
   {
      return extendedType_;
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

   public ImageResource getIcon()
   {
      return fileType_.getDefaultIcon();
   }

   public String getTabTooltip()
   {
      return getPath();
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
   void onReformatCode()
   {
      // Only allow if entire selection in R mode for now
      if (!DocumentMode.isSelectionInRMode(docDisplay_))
      {
         showRModeWarning("Reformat Code");
         return;
      }
      
      reformatHelper_.insertPrettyNewlines();
   }
   
   @Handler
   void onRenameInScope()
   {
      docDisplay_.focus();
      
      // Save folds (we need to remove them temporarily for the rename helper)
      final JsArray<AceFold> folds = docDisplay_.getFolds();
      docDisplay_.unfoldAll();
      
      int matches = renameHelper_.renameInScope();
      if (matches <= 0)
      {
         if (!docDisplay_.getSelectionValue().isEmpty())
         {
            String message = "No matches for '" + docDisplay_.getSelectionValue() + "'";
            view_.getStatusBar().showMessage(message, 1000);
         }
         
         for (AceFold fold : JsUtil.asIterable(folds))
            docDisplay_.addFold(fold.getRange());
         return;
      }
      
      String message = "Found " + matches;
      if (matches == 1)
         message += " match";
      else
         message += " matches";

      String selectedItem = docDisplay_.getSelectionValue();
      message += " for " + selectedItem + ".";

      docDisplay_.disableSearchHighlight();
      view_.getStatusBar().showMessage(message, new HideMessageHandler()
      {
         private boolean onRenameFinished(boolean value)
         {
            for (AceFold fold : JsUtil.asIterable(folds))
               docDisplay_.addFold(fold.getRange());
            return value;
         }
         
         @Override
         public boolean onNativePreviewEvent(NativePreviewEvent preview)
         {
            int type = preview.getTypeInt();
            if (docDisplay_.isPopupVisible())
               return false;
            
            // End if the user clicks somewhere
            if (type == Event.ONCLICK)
            {
               docDisplay_.exitMultiSelectMode();
               docDisplay_.clearSelection();
               docDisplay_.enableSearchHighlight();
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
                  docDisplay_.exitMultiSelectMode();
                  docDisplay_.clearSelection();
                  docDisplay_.enableSearchHighlight();
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
      roxygenHelper_.insertRoxygenSkeleton();
   }
   
   @Handler
   void onExpandSelection()
   {
      docDisplay_.expandSelection();
   }
   
   @Handler
   void onShrinkSelection()
   {
      docDisplay_.shrinkSelection();
   }
   
   @Handler
   void onExpandRaggedSelection()
   {
      docDisplay_.expandRaggedSelection();
   }
   
   @Handler
   void onInsertSnippet()
   {
      // NOTE: Bound to Shift + Tab so we delegate back there
      // if this isn't dispatched
      if (!docDisplay_.onInsertSnippet())
         docDisplay_.blockOutdent();
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
   
   @Handler
   void onCheckSpelling()
   {
      spelling_.checkSpelling();
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
      withChooseEncoding(
            docUpdateSentinel_.getEncoding(),
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  docUpdateSentinel_.reopenWithEncoding(encoding);
               }
            });
   }

   @Handler
   void onSaveSourceDoc()
   {
      saveThenExecute(null, postSaveCommand());
   }

   @Handler
   void onSaveSourceDocAs()
   {
      saveNewFile(docUpdateSentinel_.getPath(),
                  null,
                  postSaveCommand());
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
                  saveThenExecute(encoding, postSaveCommand());
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
      if (!isCursorInRMode())
      {
         showRModeWarning("Extract Variable");
         return;
      }
      
      docDisplay_.focus();

      String initialSelection = docDisplay_.getSelectionValue();
      final String refactoringName = "Extract local variable";
      final String pleaseSelectCodeMessage = "Please select the code to " +
                                             "extract into a variable.";
      if (checkSelectionAndAlert(refactoringName,
                                 pleaseSelectCodeMessage,
                                 initialSelection)) return;

      docDisplay_.fitSelectionToLines(false);

      final String code = docDisplay_.getSelectionValue();
      if (checkSelectionAndAlert(refactoringName,
                                 pleaseSelectCodeMessage,
                                 code))
         return;

      // get the first line of the selection and calculate it's indentation
      String firstLine = docDisplay_.getLine(
                        docDisplay_.getSelectionStart().getRow());
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
                         "Variable Name",
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
                               InputEditorPosition insertPosition = docDisplay_
                                       .getSelection()
                                       .extendToLineStart()
                                       .getStart();
                               docDisplay_.replaceSelection(
                                       input.trim());
                               docDisplay_.insertCode(
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
                                 "Command Not Available", 
                                 "The "+ command + " command is " +
                                 "only valid for R code chunks.");
      return;
   }
   
   
   @Handler
   void onExtractFunction()
   {
      if (!isCursorInRMode())
      {
         showRModeWarning("Extract Function");
         return;
      }
      
      docDisplay_.focus();

      String initialSelection = docDisplay_.getSelectionValue();
      final String refactoringName = "Extract Function";
      final String pleaseSelectCodeMessage = "Please select the code to " +
                                             "extract into a function.";
      if (checkSelectionAndAlert(refactoringName,
                                 pleaseSelectCodeMessage,
                                 initialSelection)) return;

      docDisplay_.fitSelectionToLines(false);

      final String code = docDisplay_.getSelectionValue();
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
                   "Function Name",
                   "",
                   new OperationWithInput<String>()
                   {
                      public void execute(String input)
                      {
                         String prefix;
                         if (docDisplay_.getSelectionOffset(true) == 0)
                            prefix = "";
                         else prefix = "\n";
                         String args = response != null ? response.join(", ")
                                                        : "";
                         docDisplay_.replaceSelection(
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
   void onSplitIntoLines()
   {
      docDisplay_.splitIntoLines();
   }

   @Handler
   void onCommentUncomment()
   {
      if (isCursorInTexMode())
         doCommentUncomment("%", null);
      else if (isCursorInRMode() || isCursorInYamlMode())
         doCommentUncomment("#", null);
      else if (fileType_.isCpp() || fileType_.isStan() || fileType_.isC())
         doCommentUncomment("//", null); 
      else if (fileType_.isPlainMarkdown())
         doCommentUncomment("<!--", "-->");
      else if (DocumentMode.isSelectionInMarkdownMode(docDisplay_))
         doCommentUncomment("<!--", "-->");
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
                     currentPosition()));
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
                     docUpdateSentinel_.getDoc().getCollabParams(), 0));
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
   private void doCommentUncomment(String commentStart,
                                   String commentEnd)
   {
      Range initialRange = docDisplay_.getSelectionRange();
      
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
            dontCommentLastLine ? 0 : docDisplay_.getLine(rowEnd).length());
      docDisplay_.setSelectionRange(expanded);
      
      String[] lines = JsUtil.toStringArray(
            docDisplay_.getLines(rowStart, rowEnd - (dontCommentLastLine ? 1 : 0)));
      
      String commonPrefix = StringUtil.getCommonPrefix(
            lines,
            true,
            true);
      
      String commonIndent = StringUtil.getIndent(commonPrefix);
      
      // First, figure out whether we're commenting or uncommenting.
      // If we discover any line that doesn't start with the comment sequence,
      // then we'll comment the whole selection.
      boolean isCommentAction = false;
      for (String line : lines)
      {
         String trimmed = line.trim();
         
         // Ignore lines that are just whitespace.
         if (!commentWhitespace && trimmed.isEmpty())
            continue;
         
         if (!isCommentAction)
         {
            if (!trimmed.startsWith(commentStart))
               isCommentAction = true;
         }
         
         if (docDisplay_.getFileType().isR())
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
            builder.append(line.substring(commonIndent.length()));
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
            if (Character.isSpace(line.charAt(startIdx)))
               startIdx++;
            
            int endIdx = commentEndIdx;
            String afterComment = line.substring(startIdx, endIdx);
            builder.append(StringUtil.trimRight(commonIndent + afterComment));
            
            builder.append("\n");
         }
      }
      
      String newSelection = dontCommentLastLine ?
            builder.toString() :
            builder.substring(0, builder.length() - 1);
            
      docDisplay_.replaceSelection(newSelection);
      
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
         docDisplay_.setSelectionRange(newRange);
      }
   }

   @Handler
   void onReindent()
   {
      docDisplay_.reindent();
      docDisplay_.focus();
   }

   @Handler
   void onReflowComment()
   {
      if (DocumentMode.isSelectionInRMode(docDisplay_))
         doReflowComment("(#)");
      else if (DocumentMode.isSelectionInCppMode(docDisplay_))
      {
         String currentLine = docDisplay_.getLine(
                                    docDisplay_.getCursorPosition().getRow());
         if (currentLine.startsWith(" *"))
            doReflowComment("( \\*[^/])", false);
         else
            doReflowComment("(//)");
      }
      else if (DocumentMode.isSelectionInTexMode(docDisplay_))
         doReflowComment("(%)");
      else if (DocumentMode.isSelectionInMarkdownMode(docDisplay_))
         doReflowComment("()");
      else if (docDisplay_.getFileType().isText())
         doReflowComment("()");
         
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
         globalDisplay_.showErrorMessage("Edit Format Failed",  
               "Can't find the YAML front matter for this document. Make " +
               "sure the front matter is enclosed by lines containing only " +
               "three dashes: ---.");
         return;
      }
      rmarkdownHelper_.convertFromYaml(yaml, new CommandWithArg<RmdYamlData>() 
      {
         @Override
         public void execute(RmdYamlData arg)
         {
            String errCaption = "Edit Format Failed";
            String errMsg = 
               "The YAML front matter in this document could not be " +
               "successfully parsed. This parse error needs to be " + 
               "resolved before format options can be edited.";
            
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
         globalDisplay_.showErrorMessage("Edit Format Failed", 
               "Couldn't determine the format options from the YAML front " +
               "matter. Make sure the YAML defines a supported output " +
               "format in its 'output' field.");
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
                  updateRmdFormatList();
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
      return YamlFrontMatter.getFrontMatter(docDisplay_);
   }
   
   private void applyRmdFrontMatter(String yaml)
   {
      if (YamlFrontMatter.applyFrontMatter(docDisplay_, yaml))
      {
         updateRmdFormatList();
      }
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
         return new ArrayList<String>();
      List<String> formats = rmarkdownHelper_.getOutputFormats(yaml);
      if (formats == null)
         formats = new ArrayList<String>();
      return formats;  
   }
   
   private void updateRmdFormatList()
   {
      String formatUiName = "";
      List<String> formatList = new ArrayList<String>();
      List<String> valueList = new ArrayList<String>();
      List<String> extensionList = new ArrayList<String>();
      
      RmdSelectedTemplate selTemplate = getSelectedTemplate();
      if (selTemplate != null && selTemplate.isShiny)
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

               String uiName = formats.get(i).getUiName();
               formatList.add(uiName);
               valueList.add(formats.get(i).getName());
               extensionList.add(formats.get(i).getExtension());
               if (formats.get(i).getName().equals(selTemplate.format))
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
                  uiName = uiName.substring(nsLoc + 2);
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
   
   void doReflowComment(String commentPrefix)
   {
      doReflowComment(commentPrefix, true);
   }
   
   void doReflowComment(String commentPrefix, boolean multiParagraphIndent)
   {
      docDisplay_.focus();

      InputEditorSelection originalSelection = docDisplay_.getSelection();
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

      reflowComments(commentPrefix,
                     multiParagraphIndent,
                     selection, 
                     originalSelection.isEmpty() ?
                     originalSelection.getStart() :
                     null);
   }

   private Position selectionToPosition(InputEditorPosition pos)
   {
      return docDisplay_.selectionToPosition(pos);
   }

   private void reflowComments(String commentPrefix,
                               final boolean multiParagraphIndent,
                               InputEditorSelection selection,
                               final InputEditorPosition cursorPos)
   {
      String code = docDisplay_.getCode(selection);
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
         cursorRowIndex = selectionToPosition(cursorPos).getRow() -
                          selectionToPosition(selection.getStart()).getRow();
         cursorColIndex =
               Math.max(0, cursorPos.getPosition() - prefix.length());
      }
      final WordWrapCursorTracker wwct = new WordWrapCursorTracker(
                                                cursorRowIndex, cursorColIndex);

      int maxLineLength =
                        prefs_.printMarginColumn().getValue() - prefix.length();

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
         private Pattern TAG_WITH_CONTENTS = Pattern.create("@\\w+\\s+[^\\s]");
      };

      for (String line : lines)
      {
         String content = line.substring(Math.min(line.length(),
                                                  prefix.length()));

         if (content.matches("^\\s*\\@examples\\b.*$"))
            wordWrap.setWrappingEnabled(false);
         else if (content.trim().startsWith("@"))
            wordWrap.setWrappingEnabled(true);

         wwct.onBeginInputRow();
         wordWrap.appendLine(content);
      }

      String wrappedString = wordWrap.getOutput();

      StringBuilder finalOutput = new StringBuilder();
      for (String line : StringUtil.getLineIterator(wrappedString))
         finalOutput.append(prefix).append(line).append("\n");
      // Remove final \n
      if (finalOutput.length() > 0)
         finalOutput.deleteCharAt(finalOutput.length()-1);

      String reflowed = finalOutput.toString();

      docDisplay_.setSelection(selection);
      if (!reflowed.equals(code))
      {
         docDisplay_.replaceSelection(reflowed);
      }

      if (cursorPos != null)
      {
         if (wwct.getResult() != null)
         {
            int row = wwct.getResult().getY();
            int col = wwct.getResult().getX();
            row += selectionToPosition(selection.getStart()).getRow();
            col += prefix.length();
            Position pos = Position.create(row, col);
            docDisplay_.setSelection(docDisplay_.createSelection(pos, pos));
         }
         else
         {
            docDisplay_.collapseSelection(false);
         }
      }
   }
   
   @Handler
   void onExecuteCodeWithoutFocus()
   {
      codeExecution_.executeSelection(false);
   }

   @Handler
   void onProfileCodeWithoutFocus()
   {
      dependencyManager_.withProfvis("Preparing profiler", new Command()
      {
         @Override
         public void execute()
         {
            codeExecution_.executeSelection(false, false, "profvis::profvis", true);
         }
      });
   }
   
   @Handler
   void onExecuteCodeWithoutMovingCursor()
   {
      if (docDisplay_.isFocused())
         codeExecution_.executeSelection(true, false);
      else if (view_.isAttached())
         view_.findSelectAll();
   }
   
   @Handler
   void onExecuteCode()
   {
      codeExecution_.executeSelection(true);
   }
   
   @Handler
   void onExecuteCurrentLine()
   {
      codeExecution_.executeBehavior(UIPrefsAccessor.EXECUTE_LINE);
   }

   @Handler
   void onExecuteCurrentStatement()
   {
      codeExecution_.executeBehavior(UIPrefsAccessor.EXECUTE_STATEMENT);
   }

   @Handler
   void onExecuteCurrentParagraph()
   {
      codeExecution_.executeBehavior(UIPrefsAccessor.EXECUTE_PARAGRAPH);
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
         executeChunks(Position.create(
               docDisplay_.getDocumentEnd().getRow() + 1,
               0),
               TextEditingTargetScopeHelper.PREVIOUS_CHUNKS);
      }
      else
      {
         sourceActiveDocument(true);
      }
   }

   @Handler
   void onExecuteToCurrentLine()
   {
      docDisplay_.focus();


      int row = docDisplay_.getSelectionEnd().getRow();
      int col = docDisplay_.getLength(row);

      codeExecution_.executeRange(Range.fromPoints(Position.create(0, 0),
                                  Position.create(row, col)));
   }
   
   @Handler
   void onExecuteFromCurrentLine()
   {
      docDisplay_.focus();

      int startRow = docDisplay_.getSelectionStart().getRow();
      int startColumn = 0;
      Position start = Position.create(startRow, startColumn);
      
      codeExecution_.executeRange(Range.fromPoints(start, endPosition()));
   }

   @Handler
   void onExecuteCurrentFunction()
   {
      docDisplay_.focus();

      // HACK: This is just to force the entire function tree to be built.
      // It's the easiest way to make sure getCurrentScope() returns
      // a Scope with an end.
      docDisplay_.getScopeTree();
      Scope currentFunction = docDisplay_.getCurrentFunction(false);

      // Check if we're at the top level (i.e. not in a function), or in
      // an unclosed function
      if (currentFunction == null || currentFunction.getEnd() == null)
         return;
      
      Position start = currentFunction.getPreamble();
      Position end = currentFunction.getEnd();

      codeExecution_.executeRange(Range.fromPoints(start, end));
   }

   @Handler   
   void onExecuteCurrentSection()
   {
      docDisplay_.focus();

      // Determine the current section.
      docDisplay_.getScopeTree();
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
   }
    
   private Position endPosition()
   {
      int endRow = Math.max(0, docDisplay_.getRowCount() - 1);
      int endColumn = docDisplay_.getLength(endRow);
      return Position.create(endRow, endColumn);
   }
   
   private void onInsertChunk(String chunkPlaceholder, int rowOffset, int colOffset)
   {
      String sel = null;
      Range selRange = null;
      
      // if currently in a chunk, add a blank line (for padding) and insert 
      // beneath it
      Scope currentChunk = docDisplay_.getCurrentChunk();
      if (currentChunk != null)
      {
         // record current selection before manipulating text
         sel = docDisplay_.getSelectionValue();
         selRange = docDisplay_.getSelectionRange();
         
         docDisplay_.setCursorPosition(currentChunk.getEnd());
         docDisplay_.insertCode("\n");
         docDisplay_.moveCursorForward(1);
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

   @Handler
   void onInsertChunk()
   {
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
   void onInsertChunkPython()
   {
      onInsertChunk("```{python}\n\n```\n", 1, 0);
   }

   @Handler
   void onInsertChunkRCPP()
   {
      onInsertChunk("```{rcpp}\n\n```\n", 1, 0);
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
   void onInsertSection()
   {
      globalDisplay_.promptForText(
         "Insert Section", 
         "Section label:", 
         "", 
         new OperationWithInput<String>() {
            @Override
            public void execute(String label)
            {
               // move cursor to next insert location
               Position pos = moveCursorToNextInsertLocation();
               
               // truncate length to print margin - 5
               int printMarginColumn = prefs_.printMarginColumn().getValue();
               int length = printMarginColumn - 5;
               
               // truncate label to maxLength - 10 (but always allow at 
               // least 20 chars for the label)
               int maxLabelLength = length - 10;
               maxLabelLength = Math.max(maxLabelLength, 20);
               if (label.length() > maxLabelLength)
                  label = label.substring(0, maxLabelLength-1);
               
               // prefix 
               String prefix = "# " + label + " ";
               
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
      // HACK: This is just to force the entire function tree to be built.
      // It's the easiest way to make sure getCurrentScope() returns
      // a Scope with an end.
      docDisplay_.getScopeTree();
      
      executeSweaveChunk(scopeHelper_.getCurrentSweaveChunk(), 
           NotebookQueueUnit.EXEC_MODE_SINGLE, false);
   }
   
   @Handler
   void onExecuteNextChunk()
   {
      // HACK: This is just to force the entire function tree to be built.
      // It's the easiest way to make sure getCurrentScope() returns
      // a Scope with an end.
      docDisplay_.getScopeTree();
      
      Scope nextChunk = scopeHelper_.getNextSweaveChunk();
      executeSweaveChunk(nextChunk, NotebookQueueUnit.EXEC_MODE_SINGLE, 
            true);
      docDisplay_.setCursorPosition(nextChunk.getBodyStart());
      docDisplay_.ensureCursorVisible();
   }
   
   @Handler
   void onExecutePreviousChunks()
   {
      executeChunks(null, TextEditingTargetScopeHelper.PREVIOUS_CHUNKS);
   }
   
   @Handler
   void onExecuteSubsequentChunks()
   {
      executeChunks(null, TextEditingTargetScopeHelper.FOLLOWING_CHUNKS);
   }
   
   public void executeChunks(final Position position, int which)
   {
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
                           events_.fireEvent(new SendToConsoleEvent(code, true));
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
            jobDesc = "Run All";
         else if (which == TextEditingTargetScopeHelper.PREVIOUS_CHUNKS)
            jobDesc = "Run Previous";
         else if (which == TextEditingTargetScopeHelper.FOLLOWING_CHUNKS)
            jobDesc = "Run After";
      }

      List<ChunkExecUnit> chunks = new ArrayList<ChunkExecUnit>();
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
   
   private boolean isRChunk(Scope scope)
   {
      String labelText = docDisplay_.getLine(scope.getPreamble().getRow());
      Pattern reEngine = Pattern.create(".*engine\\s*=\\s*['\"]([^'\"]*)['\"]");
      Match match = reEngine.match(labelText, 0);
      if (match == null)
         return true;
      
      String engine = match.getGroup(1).toLowerCase();
      
      // NOTE: We might want to include 'Rscript' but such chunks are typically
      // intended to be run in their own process so it might not make sense to
      // collect those here.
      return engine.equals("r");
   }
   
   private boolean isExecutableChunk(final Scope chunk)
   {
      if (!chunk.isChunk())
         return false;
      
      String headerText = docDisplay_.getLine(chunk.getPreamble().getRow());
      Pattern reEvalFalse = Pattern.create("eval\\s*=\\s*F(?:ALSE)?");
      if (reEvalFalse.test(headerText))
         return false;
      
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
               events_.fireEvent(new SendToConsoleEvent(code, true));
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
            "Go to Line",
            "Enter line number:",
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
      docDisplay_.codeCompletion();
   }
   
   @Handler
   void onGoToHelp()
   {
      docDisplay_.goToHelp();
   } 

   @Handler
   void onGoToFunctionDefinition()
   {
      docDisplay_.goToFunctionDefinition();
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
               "Source File Not Saved",   
               "The currently active source file is not saved so doesn't " +
               "have a directory to change into.");
         return;
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
         if (text.length() > 0 && text.charAt(text.length()-1) != '\n')
            code.append('\n');
      }
      return code.toString();
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
   void onProfileCode()
   {
      dependencyManager_.withProfvis("Preparing profiler", new Command()
      {
         @Override
         public void execute()
         {
            codeExecution_.executeSelection(true, true, "profvis::profvis", true);
         }
      });
   }
  
   private void sourceActiveDocument(final boolean echo)
   {
      docDisplay_.focus();

      // If the document being sourced is a Shiny file, run the app instead.
      if (fileType_.isR() && 
          extendedType_.startsWith(SourceDocument.XT_SHINY_PREFIX))
      {
         runShinyApp();
         return;
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
         // R 2.14 prints a warning when sourcing a file with no trailing \n
         if (!code.endsWith("\n"))
            code = code + "\n";

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
                     if (docDisplay_.hasBreakpoints())
                     {
                        hideBreakpointWarningBar();
                     }
                     consoleDispatcher_.executeSourceCommand(
                           getPath(),
                           fileType_,
                           docUpdateSentinel_.getEncoding(),
                           activeCodeIsAscii(),
                           forceEcho ? true : echo,
                           prefs_.focusConsoleAfterExec().getValue(),
                           docDisplay_.hasBreakpoints());   
                  }
               };
            
            if (saveWhenSourcing && (dirtyState_.getValue() || (getPath() == null)))
               saveThenExecute(null, sourceCommand);
            else
               sourceCommand.execute(); 
         }
      }
      
      // update pref if necessary
      if (prefs_.sourceWithEcho().getValue() != echo)
      {
         prefs_.sourceWithEcho().setGlobalValue(echo, true);
         prefs_.writeUIPrefs();
      }
   }
   
   private void runShinyApp()
   {
      sourceBuildHelper_.withSaveFilesBeforeCommand(new Command() {
         @Override
         public void execute()
         {
            events_.fireEvent(new LaunchShinyApplicationEvent(getPath(),
                  getExtendedFileType()));
         }
      }, "Run Shiny Application");
   }
   
   private void runScript()
   {
      saveThenExecute(null, new Command() {
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
      saveThenExecute(null, new Command() {
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
      docDisplay_.focus();

      codeExecution_.executeLastCode();
   }
   
   @Handler
   void onKnitDocument()
   {
      onPreviewHTML();
   }
   
   @Handler
   void onPreviewHTML()
   {
      // last ditch extended type detection
      String extendedType = extendedType_;
      extendedType = rmarkdownHelper_.detectExtendedType(docDisplay_.getCode(),
                                                         extendedType, 
                                                         fileType_);
      
      if (extendedType == SourceDocument.XT_RMARKDOWN)
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
               "Unable to Preview",
               "R Presentations require the knitr package " +
               "(version 1.2 or higher)");
         return;
      } 
      
      PresentationState state = sessionInfo.getPresentationState();
      
      // if we are showing a tutorial then don't allow preview
      if (state.isTutorial())
      {
         globalDisplay_.showMessage(
               MessageDisplay.MSG_WARNING,
               "Unable to Preview",
               "R Presentations cannot be previewed when a Tutorial " +
               "is active");
         return;
      }
      
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
         saveThenExecute(null, new Command() {
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
      saveThenExecute(null, new Command() {
         @Override
         public void execute()
         {
            String previewURL = "help/preview?file=";
            previewURL += URL.encodeQueryString(docUpdateSentinel_.getPath());   
            events_.fireEvent(new ShowHelpEvent(previewURL)) ; 
         }
      });
   }
   
   void renderRmd()
   {
      renderRmd(null);
   }
   
   void renderRmd(final String paramsFile)
   { 
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

            rmarkdownHelper_.renderRMarkdown(
                  docUpdateSentinel_.getPath(),
                  docDisplay_.getCursorPosition().getRow() + 1,
                  null,
                  docUpdateSentinel_.getEncoding(),
                  paramsFile,
                  asTempfile,
                  type,
                  false,
                  rmarkdownHelper_.getKnitWorkingDir(docUpdateSentinel_),
                  viewerType);
         }
      };  

      final Command saveCommand = new Command()
      {
         @Override
         public void execute()
         {
            saveThenExecute(null, renderCommand);
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
   
   private boolean isShinyPrerenderedDoc()
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
   
   private String getCustomKnit()
   {
      try
      {
         String yaml = getRmdFrontMatter();
         if (yaml == null)
            return new String();
         return rmarkdownHelper_.getCustomKnit(yaml);  
      }
      catch(Exception e)
      {
         Debug.log(e.getMessage());
         return new String();
      }
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
            server_.previewHTML(params, new SimpleRequestCallback<Boolean>());
         }
      };

      if (pParams.get().isNotebook())
      {
         saveThenExecute(null, new Command()
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
         saveThenExecute(null, CommandUtil.join(showPreviewWindowCommand,
                                                runPreviewCommand));
      }
      // otherwise if it's dirty then show the preview window first (to
      // beat the popup blockers) then save & run
      else if (dirtyState().getValue())
      {
         showPreviewWindowCommand.execute();
         saveThenExecute(null, runPreviewCommand);
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
         defaultAuthor = prefs_.compileNotebookOptions().getValue().getAuthor();
         if (StringUtil.isNullOrEmpty(defaultAuthor))
            defaultAuthor = session_.getSessionInfo().getUserIdentity();
      }
      
      // default type
      String defaultType = docUpdateSentinel_.getProperty(NOTEBOOK_TYPE);
      if (StringUtil.isNullOrEmpty(defaultType))
      {
         defaultType = prefs_.compileNotebookOptions().getValue().getType();
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
                                       "Unable to Compile Report", 
                                       response.getFailureMessage());
                  }
               }
            });
            
            // save options for this document
            HashMap<String, String> changedProperties = new HashMap<String, String>();
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
                                  prefs_.compileNotebookOptions().getValue()))
            {
               prefs_.compileNotebookOptions().setGlobalValue(prefs);
               prefs_.writeUIPrefs();
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
         saveThenExecute(null, new Command()
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
      String pdfPreview = prefs_.pdfPreview().getValue();
      boolean showPdf = !pdfPreview.equals(UIPrefsAccessor.PDF_PREVIEW_NONE);
      boolean useInternalPreview = 
            pdfPreview.equals(UIPrefsAccessor.PDF_PREVIEW_RSTUDIO);
      boolean useDesktopSynctexPreview = 
            pdfPreview.equals(UIPrefsAccessor.PDF_PREVIEW_DESKTOP_SYNCTEX) &&
            Desktop.isDesktop();
      
      String action = new String();
      if (showPdf && !useInternalPreview && !useDesktopSynctexPreview)
         action = "view_external";
      
      handlePdfCommand(action, useInternalPreview, null);
   }
   
   
   @Handler
   void onKnitWithParameters()
   {
      saveThenExecute(null, new Command() {
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
               "Clear Knitr Cache", 
               "Clearing the Knitr cache will delete the cache " +
               "directory for " + docPath + ". " +
               "\n\nAre you sure you want to clear the cache now?",
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
               "Clear Prerendered Output", 
               "This will remove all previously generated output " +
               "for " + docPath + " (html, prerendered data, knitr cache, etc.)." +
               "\n\nAre you sure you want to clear the output now?",
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

   @Handler
   void onFindReplace()
   {
      view_.showFindReplace(true);
   }
   
   @Handler
   void onFindNext()
   {
      view_.findNext();
   }
   
   @Handler
   void onFindPrevious()
   {
      view_.findPrevious();
   }
   
   @Handler
   void onFindSelectAll()
   {
      view_.findSelectAll();
   }
   
   @Handler
   void onFindFromSelection()
   {
      view_.findFromSelection();
      docDisplay_.focus();
   }
   
   @Handler
   void onReplaceAndFind()
   {
      view_.replaceAndFind();
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
      if (useScopeTreeFolding())
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
      if (useScopeTreeFolding())
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
      if (useScopeTreeFolding())
      {
         // Fold all except anonymous braces
         HashSet<Integer> rowsFolded = new HashSet<Integer>();
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
      if (useScopeTreeFolding())
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
      docDisplay_.toggleTokenInfo();
   }
   
   boolean useScopeTreeFolding()
   {
      return docDisplay_.hasScopeTree();
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
      
      saveThenExecute(null, new Command()
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
               "Invalid Filename",
               "The file '" + file.getName() + "' cannot be compiled to " +
               "a PDF because TeX does not understand paths with spaces. " +
               "If you rename the file to remove spaces then " +
               "PDF compilation will work correctly.");
        
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
   
   private Command postSaveCommand()
   {
      return new Command()
      {
         public void execute()
         {
            // fire source document saved event
            FileSystemItem file = FileSystemItem.createFile(
                                             docUpdateSentinel_.getPath());
            events_.fireEvent(new SourceFileSaveCompletedEvent(
                                             file,
                                             docUpdateSentinel_.getContents(),
                                             docDisplay_.getCursorPosition()));
            
            // check for source on save
            if (fileType_.canSourceOnSave() && docUpdateSentinel_.sourceOnSave())
            {
               if (fileType_.isRd())
               {
                  previewRd();
               }
               else if (fileType_.canPreviewFromR())
               {
                  previewFromR();
               }
               else
               {
                  if (docDisplay_.hasBreakpoints())
                  {
                     hideBreakpointWarningBar();
                  }
                  consoleDispatcher_.executeSourceCommand(
                                             docUpdateSentinel_.getPath(), 
                                             fileType_,
                                             docUpdateSentinel_.getEncoding(), 
                                             activeCodeIsAscii(),
                                             false,
                                             false,
                                             docDisplay_.hasBreakpoints());
               }
            }
         }
      };
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
      
      final Invalidation.Token token = externalEditCheckInvalidation_.getInvalidationToken();

      server_.checkForExternalEdit(
            id_,
            new ServerRequestCallback<CheckForExternalEditResult>()
            {
               @Override
               public void onResponseReceived(CheckForExternalEditResult response)
               {
                  if (token.isInvalid())
                     return;

                  if (response.isDeleted())
                  {
                     if (ignoreDeletes_)
                        return;

                     globalDisplay_.showYesNoMessage(
                           GlobalDisplay.MSG_WARNING,
                           "File Deleted",
                           "The file " + 
                           StringUtil.notNull(docUpdateSentinel_.getPath()) + 
                           " has been deleted or moved. " +
                           "Do you want to close this file now?",
                           false,
                           new Operation()
                           {
                              public void execute()
                              {
                                 CloseEvent.fire(TextEditingTarget.this, null);
                              }
                           },
                           new Operation()
                           {
                              public void execute()
                              {
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
                        docUpdateSentinel_.revert();
                     }
                     else
                     {
                        externalEditCheckInterval_.reset();
                        globalDisplay_.showYesNoMessage(
                              GlobalDisplay.MSG_WARNING,
                              "File Changed",
                              "The file " + name_.getValue() + " has changed " +
                              "on disk. Do you want to reload the file from " +
                              "disk and discard your unsaved changes?",
                              false,
                              new Operation()
                              {
                                 public void execute()
                                 {
                                    docUpdateSentinel_.revert();
                                 }
                              },
                              new Operation()
                              {
                                 public void execute()
                                 {
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
   
   private SourcePosition toSourcePosition(Scope func)
   {
      Position pos = func.getPreamble();
      return SourcePosition.create(pos.getRow(), pos.getColumn());
   }
   
   private boolean isCursorInTexMode()
   {
      if (fileType_.canCompilePDF())
      {
         if (fileType_.isRnw())
         {
            return SweaveFileType.TEX_LANG_MODE.equals(
               docDisplay_.getLanguageMode(docDisplay_.getCursorPosition()));
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
   
   private boolean isCursorInRMode()
   {
      String mode = docDisplay_.getLanguageMode(docDisplay_.getCursorPosition());
      if (mode == null)
         return true;
      if (mode.equals(TextFileType.R_LANG_MODE))
         return true;
      return false;
   }
   
   private boolean isCursorInYamlMode()
   {
      String mode = docDisplay_.getLanguageMode(docDisplay_.getCursorPosition());
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
   
   private CppCompletionContext cppCompletionContext_ = 
                                          new CppCompletionContext() {
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
   
   private RCompletionContext rContext_ = new RCompletionContext() {

      @Override
      public String getPath()
      {
         if (docUpdateSentinel_ == null)
            return null;
         else
            return docUpdateSentinel_.getPath();
      }
      
      @Override
      public String getId()
      {
         if (docUpdateSentinel_ == null)
            return null;
         else
            return docUpdateSentinel_.getId();
      }
   };
   
   // these methods are public static so that other editing targets which
   // display source code (but don't inherit from TextEditingTarget) can share
   // their implementation
   
   public static interface PrefsContext
   {
      FileSystemItem getActiveFile();
   }
   
   public static void registerPrefs(
                     ArrayList<HandlerRegistration> releaseOnDismiss,
                     UIPrefs prefs,
                     DocDisplay docDisplay,
                     final SourceDocument sourceDoc)
   {
      registerPrefs(releaseOnDismiss,
                    prefs,
                    docDisplay,
                    new PrefsContext() {
                        @Override
                        public FileSystemItem getActiveFile()
                        {
                           String path = sourceDoc.getPath();
                           if (path != null)
                              return FileSystemItem.createFile(path);
                           else
                              return null;
                        }
                    });
   }
   
   public static void registerPrefs(
                     ArrayList<HandlerRegistration> releaseOnDismiss,
                     UIPrefs prefs,
                     final DocDisplay docDisplay,
                     final PrefsContext context)
   {
      releaseOnDismiss.add(prefs.highlightSelectedLine().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setHighlightSelectedLine(arg);
               }}));
      releaseOnDismiss.add(prefs.highlightSelectedWord().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setHighlightSelectedWord(arg);
               }}));
      releaseOnDismiss.add(prefs.showLineNumbers().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setShowLineNumbers(arg);
               }}));
      releaseOnDismiss.add(prefs.useSpacesForTab().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  // Makefile always uses tabs
                  FileSystemItem file = context.getActiveFile();
                  if ((file != null) && 
                     ("Makefile".equals(file.getName()) ||
                      "Makevars".equals(file.getName()) ||
                      "Makevars.win".equals(file.getName())))
                  {
                     docDisplay.setUseSoftTabs(false);
                  }
                  else
                  {
                     docDisplay.setUseSoftTabs(arg);
                  }
               }}));
      releaseOnDismiss.add(prefs.numSpacesForTab().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay.setTabSize(arg);
               }}));
      releaseOnDismiss.add(prefs.showMargin().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setShowPrintMargin(arg);
               }}));
      releaseOnDismiss.add(prefs.blinkingCursor().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setBlinkingCursor(arg);
               }}));
      releaseOnDismiss.add(prefs.printMarginColumn().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay.setPrintMarginColumn(arg);
               }}));
      releaseOnDismiss.add(prefs.showInvisibles().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setShowInvisibles(arg);
               }}));
      releaseOnDismiss.add(prefs.showIndentGuides().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setShowIndentGuides(arg);
               }}));
      releaseOnDismiss.add(prefs.scrollPastEndOfDocument().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setScrollPastEndOfDocument(arg);
               }}));
      releaseOnDismiss.add(prefs.highlightRFunctionCalls().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setHighlightRFunctionCalls(arg);
               }}));
      releaseOnDismiss.add(prefs.useVimMode().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setUseVimMode(arg);
               }}));
      releaseOnDismiss.add(prefs.enableEmacsKeybindings().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setUseEmacsKeybindings(arg);
               }}));
      releaseOnDismiss.add(prefs.codeCompleteOther().bind(
            new CommandWithArg<String>() {
               public void execute(String arg) {
                  docDisplay.syncCompletionPrefs();
               }}));
      releaseOnDismiss.add(prefs.alwaysCompleteCharacters().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay.syncCompletionPrefs();
               }}));
      releaseOnDismiss.add(prefs.alwaysCompleteDelayMs().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay.syncCompletionPrefs();
               }}));
      releaseOnDismiss.add(prefs.enableSnippets().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.syncCompletionPrefs();
               }}));
      releaseOnDismiss.add(prefs.showDiagnosticsOther().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.syncDiagnosticsPrefs();
               }}));
      releaseOnDismiss.add(prefs.diagnosticsOnSave().bind(
            new CommandWithArg<Boolean>() {
               @Override
               public void execute(Boolean arg)
               {
                  docDisplay.syncDiagnosticsPrefs();
               }}));
      releaseOnDismiss.add(prefs.backgroundDiagnosticsDelayMs().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay.syncDiagnosticsPrefs();
               }}));
      releaseOnDismiss.add(prefs.showInlineToolbarForRCodeChunks().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.forceImmediateRender();
               }}));
      releaseOnDismiss.add(prefs.foldStyle().bind(
            new CommandWithArg<String>() {
               public void execute(String style)
               {
                  docDisplay.setFoldStyle(style);
               }}));
      releaseOnDismiss.add(prefs.surroundSelection().bind(
            new CommandWithArg<String>() {
               public void execute(String string)
               {
                  docDisplay.setSurroundSelectionPref(string);
               }}));
      
   }
   
   public static void syncFontSize(
                              ArrayList<HandlerRegistration> releaseOnDismiss,
                              EventBus events,
                              final TextDisplay view,
                              FontSizeManager fontSizeManager)
   {
      releaseOnDismiss.add(events.addHandler(
            ChangeFontSizeEvent.TYPE,
            new ChangeFontSizeHandler()
            {
               public void onChangeFontSize(ChangeFontSizeEvent event)
               {
                  view.setFontSize(event.getFontSize());
               }
            }));
      view.setFontSize(fontSizeManager.getSize());

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
            new RecordNavigationPositionHandler() {
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
   
   private void addAdditionalResourceFiles(ArrayList<String> additionalFiles)
   {
      // it does--get the YAML front matter and modify it to include
      // the additional files named in the deployment
      String yaml = getRmdFrontMatter();
      if (yaml == null)
         return;
      rmarkdownHelper_.addAdditionalResourceFiles(yaml,
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
      prefs_.preferredDocumentOutlineWidth().setGlobalValue((int) size);
      prefs_.writeUIPrefs();
      docUpdateSentinel_.setProperty(DOC_OUTLINE_SIZE, size + "");
   }
   
   public double getPreferredOutlineWidgetSize()
   {
      String property = docUpdateSentinel_.getProperty(DOC_OUTLINE_SIZE);
      if (StringUtil.isNullOrEmpty(property))
         return prefs_.preferredDocumentOutlineWidth().getGlobalValue();
      
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
         return prefs_.preferredDocumentOutlineWidth().getGlobalValue();
      }
   }
   
   public void setPreferredOutlineWidgetVisibility(boolean visible)
   {
      docUpdateSentinel_.setProperty(DOC_OUTLINE_VISIBLE, visible ? "1" : "0");
   }
   
   public boolean getPreferredOutlineWidgetVisibility()
   {
      String property = docUpdateSentinel_.getProperty(DOC_OUTLINE_VISIBLE);
      return StringUtil.isNullOrEmpty(property)
            ? (getTextFileType().isRmd() && prefs_.showDocumentOutlineRmd().getGlobalValue())
            : Integer.parseInt(property) > 0;
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
   
   /**
    * Updates the path of the file loaded in the editor, as though the user
    * had just saved the file at the new paht.
    * 
    * @param path New path for the editor
    */
   public void setPath(FileSystemItem path)
   {
      // Find the new type
      TextFileType type = fileTypeRegistry_.getTextTypeForFile(path);
      
      // Simulate a completed save of the new path
      new SaveProgressIndicator(path, type, null).onCompleted();
   }
   
   private void setRMarkdownBehaviorEnabled(boolean enabled)
   {
      // register idle monitor; automatically creates/refreshes previews
      // of images and LaTeX equations during idle
      if (bgIdleMonitor_ == null && enabled)
         bgIdleMonitor_ = new TextEditingTargetIdleMonitor(this, 
               docUpdateSentinel_);
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
   
   private StatusBar statusBar_;
   private final DocDisplay docDisplay_;
   private final UIPrefs prefs_;
   private Display view_;
   private final Commands commands_;
   private SourceServerOperations server_;
   private EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final FileDialogs fileDialogs_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final FileTypeCommands fileTypeCommands_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final WorkbenchContext workbenchContext_;
   private final Session session_;
   private final Synctex synctex_;
   private final FontSizeManager fontSizeManager_;
   private final SourceBuildHelper sourceBuildHelper_;
   private final DependencyManager dependencyManager_;
   private DocUpdateSentinel docUpdateSentinel_;
   private Value<String> name_ = new Value<String>(null);
   private TextFileType fileType_;
   private String id_;
   private HandlerRegistration commandHandlerReg_;
   private ArrayList<HandlerRegistration> releaseOnDismiss_ =
         new ArrayList<HandlerRegistration>();
   private final DirtyState dirtyState_;
   private HandlerManager handlers_ = new HandlerManager(this);
   private FileSystemContext fileContext_;
   private final TextEditingTargetCompilePdfHelper compilePdfHelper_;
   private final TextEditingTargetRMarkdownHelper rmarkdownHelper_;
   private final TextEditingTargetCppHelper cppHelper_;
   private final TextEditingTargetPresentationHelper presentationHelper_;
   private final TextEditingTargetReformatHelper reformatHelper_;
   private TextEditingTargetIdleMonitor bgIdleMonitor_;
   private TextEditingTargetThemeHelper themeHelper_;
   private RoxygenHelper roxygenHelper_;
   private boolean ignoreDeletes_;
   private boolean forceSaveCommandActive_ = false;
   private final TextEditingTargetScopeHelper scopeHelper_;
   private TextEditingTargetSpelling spelling_;
   private TextEditingTargetNotebook notebook_;
   private TextEditingTargetChunks chunks_;
   private BreakpointManager breakpointManager_;
   private final LintManager lintManager_;
   private final TextEditingTargetRenameHelper renameHelper_;
   private CollabEditStartParams queuedCollabParams_;
   private MathJax mathjax_;
   private InlinePreviewer inlinePreviewer_;
   
   // Allows external edit checks to supercede one another
   private final Invalidation externalEditCheckInvalidation_ =
         new Invalidation();
   // Prevents external edit checks from happening too soon after each other
   private final IntervalTracker externalEditCheckInterval_ =
         new IntervalTracker(1000, true);
   private EditingTargetCodeExecution codeExecution_;
   
   private SourcePosition debugStartPos_ = null;
   private SourcePosition debugEndPos_ = null;
   private boolean isDebugWarningVisible_ = false;
   private boolean isBreakpointWarningVisible_ = false;
   private String extendedType_;

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
                 "The selected code could not be " +
                 "parsed.\n\n" +
                 "Are you sure you want to continue?",
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
}
