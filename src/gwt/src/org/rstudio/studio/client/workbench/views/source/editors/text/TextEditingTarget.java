/*
 * TextEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
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
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.EnsureHeightHandler;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureHeightHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.ChangeFontSizeHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.*;
import org.rstudio.studio.client.common.debugging.BreakpointManager;
import org.rstudio.studio.client.common.debugging.events.BreakpointsSavedEvent;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.SweaveFileType;
import org.rstudio.studio.client.common.filetypes.TextFileType;
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
import org.rstudio.studio.client.rmarkdown.events.ConvertToShinyDocEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdOutputFormatChangedEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatterOutputOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdYamlData;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.rmarkdown.ui.RmdTemplateOptionsDialog;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
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
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetCodeExecution;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList.ContainsFoldPredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper.RmdSelectedTemplate;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionOperation;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupMenu;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupRequest;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
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

   private static final MyCommandBinder commandBinder =
         GWT.create(MyCommandBinder.class);

   public interface Display extends TextDisplay, 
                                    WarningBarDisplay,
                                    HasEnsureVisibleHandlers,
                                    HasEnsureHeightHandlers
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
      
      void setIsShinyFormat(boolean isPresentation);
      void setFormatOptions(TextFileType fileType,
                            List<String> options, 
                            List<String> values, 
                            List<String> extensions, 
                            String selected);
      void setFormatOptionsVisible(boolean visible);
      HandlerRegistration addRmdFormatChangedHandler(
            RmdOutputFormatChangedEvent.Handler handler);
      
      void setPublishPath(int contentType, String publishPath);
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
                            SourceBuildHelper sourceBuildHelper)
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

      docDisplay_ = docDisplay;
      dirtyState_ = new DirtyState(docDisplay_, false);
      lintManager_ = new LintManager(this);
      prefs_ = prefs;
      codeExecution_ = new EditingTargetCodeExecution(docDisplay_, this);
      compilePdfHelper_ = new TextEditingTargetCompilePdfHelper(docDisplay_);
      rmarkdownHelper_ = new TextEditingTargetRMarkdownHelper();
      cppHelper_ = new TextEditingTargetCppHelper(cppCompletionContext_, 
                                                  docDisplay_);
      presentationHelper_ = new TextEditingTargetPresentationHelper(
                                                                  docDisplay_);
      reformatHelper_ = new TextEditingTargetReformatHelper(docDisplay_);
      snippets_ = new SnippetHelper((AceEditor) docDisplay_);
      
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
         @SuppressWarnings("deprecation")
         public void onKeyDown(KeyDownEvent event)
         {
            NativeEvent ne = event.getNativeEvent();
            int mod = KeyboardShortcut.getModifierValue(ne);
            
            if ((mod == KeyboardShortcut.META || (mod == KeyboardShortcut.CTRL && !BrowseCap.hasMetaKey()))
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
            else if (mod == KeyboardShortcut.ALT &&
                     KeyboardHelper.isHyphenKeycode(ne.getKeyCode()))
            {
               event.preventDefault();
               event.stopPropagation();
               
               if (Character.isSpace(docDisplay_.getCharacterBeforeCursor()) ||
                   (!docDisplay_.hasSelection() &&
                         docDisplay_.getCursorPosition().getColumn() == 0))
                  docDisplay_.insertCode("<- ", false);
               else
                  docDisplay_.insertCode(" <- ", false);
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
               
               if (commands_.interruptR().isEnabled())
                  commands_.interruptR().execute();
            }
            else if (ne.getKeyCode() == KeyCodes.KEY_M && (
                  (BrowseCap.hasMetaKey() &&
                   mod == (KeyboardShortcut.META + KeyboardShortcut.SHIFT)) ||
                  (!BrowseCap.hasMetaKey() &&
                   mod == (KeyboardShortcut.CTRL + KeyboardShortcut.SHIFT))))
            {
               event.preventDefault();
               event.stopPropagation();
               
               if (Character.isSpace(docDisplay_.getCharacterBeforeCursor()) ||
                   (!docDisplay_.hasSelection() &&
                         docDisplay_.getCursorPosition().getColumn() == 0))
                  docDisplay_.insertCode("%>% ", false);
               else
                  docDisplay_.insertCode(" %>% ", false);
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
                     view_.onShinyApplicationStateChanged(
                           event.getParams().getState());
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

   public void initialize(SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
   {
      id_ = document.getId();
      fileContext_ = fileContext;
      fileType_ = (TextFileType) type;
      
      extendedType_ = document.getExtendedType();
      extendedType_ = rmarkdownHelper_.detectExtendedType(document.getContents(),
                                                          extendedType_, 
                                                          fileType_);
      
      view_ = new TextEditingTargetWidget(commands_,
                                          prefs_,
                                          fileTypeRegistry_,
                                          docDisplay_,
                                          fileType_,
                                          extendedType_,
                                          events_,
                                          session_);
      docUpdateSentinel_ = new DocUpdateSentinel(
            server_,
            docDisplay_,
            document,
            globalDisplay_.getProgressIndicator("Save File"),
            dirtyState_,
            events_);

      roxygenHelper_ = new RoxygenHelper(docDisplay_, view_);
      
      // ensure that Makefile and Makevars always use tabs
      name_.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if ("Makefile".equals(event.getValue()) ||
                "Makevars".equals(event.getValue()) ||
                "Makevars.win".equals(event.getValue()))
            {
               docDisplay_.setUseSoftTabs(false);
            }
         }
      });
      
      name_.setValue(getNameFromDocument(document, defaultNameProvider), true);
      docDisplay_.setCode(document.getContents(), false);

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
         }
      });

      docDisplay_.addFocusHandler(new FocusHandler()
      {
         public void onFocus(FocusEvent event)
         {
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
                  if (!extendedType_.equals("shiny")) 
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
      
      // for R Markdown docs, populate the popup menu with a list of available
      // formats
      if (extendedType_.equals("rmarkdown"))
         updateRmdFormatList();
      
      view_.addRmdFormatChangedHandler(new RmdOutputFormatChangedEvent.Handler()
      {
         @Override
         public void onRmdOutputFormatChanged(RmdOutputFormatChangedEvent event)
         {
            setRmdFormat(event.getFormat());
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
            menu.showRelativeToUpward((UIObject) statusBar_.getLanguage());  
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
      menu.showRelativeToUpward((UIObject) statusBar_.getScope());
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
      if (canShowScope)
         updateCurrentScope();
   }

   private void updateStatusBarPosition()
   {
      Position pos = docDisplay_.getCursorPosition();
      statusBar_.getPosition().setValue((pos.getRow() + 1) + ":" +
                                        (pos.getColumn() + 1));
      
      if (fileType_.canShowScopeTree())
         updateCurrentScope();
   }
  
   private void updateCurrentScope()
   {
      Scheduler.get().scheduleDeferred(
            new ScheduledCommand()
            {
               public void execute()
               {
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
            });
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
      Command closeCommand = new Command() {
         public void execute()
         {
            CloseEvent.fire(TextEditingTarget.this, null);
         }
      };
       
      if (dirtyState_.getValue())
         saveWithPrompt(closeCommand, null);
      else
         closeCommand.execute();

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

   private void saveThenExecute(String encodingOverride, final Command command)
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
                                 new SourceFileSavedEvent(saveItem.getPath()));
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
      
      if (prefs_.stripTrailingWhitespace().getValue() &&
          !fileType_.isMarkdown())
      {
         String code = docDisplay_.getCode();
         Pattern pattern = Pattern.create("[ \t]+$");
         String strippedCode = pattern.replaceAll(code, "");
         if (!strippedCode.equals(code))
            docDisplay_.setCode(strippedCode, true);
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



   public void onDismiss()
   {
      docUpdateSentinel_.stop();
      
      if (spelling_ != null)
         spelling_.onDismiss();
      
      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();

      codeExecution_.detachLastExecuted();
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
      if (extendedType.equals("rmarkdown"))
         updateRmdFormatList();
      extendedType_ = extendedType;
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
   void onInsertRoxygenSkeleton()
   {
      roxygenHelper_.insertRoxygenSkeleton();
   }
   
   @Handler
   void onInsertSnippet()
   {
      // NOTE: Bound to Shift + Tab so we delegate back there
      // if this isn't dispatched
      if (!snippets_.onInsertSnippet())
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
      fireVcsViewOnGithubEvent(GitHubViewRequest.ViewType.View);
   }

   @Handler
   void onVcsBlameOnGitHub()
   {
      fireVcsViewOnGithubEvent(GitHubViewRequest.ViewType.Blame);
   }
   
   private void fireVcsViewOnGithubEvent(GitHubViewRequest.ViewType type)
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
                                 + "function (" + args + ") {\n"
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
   void onJumpToMatching()
   {
      docDisplay_.jumpToMatching();
      docDisplay_.ensureCursorVisible();
   }
   
   @Handler
   void onSelectToMatching()
   {
      docDisplay_.selectToMatching();
      docDisplay_.ensureCursorVisible();
   }
   
   @Handler
   void onExpandToMatching()
   {
      docDisplay_.expandToMatching();
      docDisplay_.ensureCursorVisible();
   }

   @Handler
   void onCommentUncomment()
   {
      if (isCursorInTexMode())
         doCommentUncomment("%");
      else if (isCursorInRMode())
         doCommentUncomment("#");
      else if (fileType_.isCpp() || fileType_.isStan())
         doCommentUncomment("//"); 
   }
   
   private void doCommentUncomment(String c)
   {
      InputEditorSelection initialSelection = docDisplay_.getSelection();
      String indent = "";
      boolean singleLineAction = initialSelection.isEmpty() ||
            initialSelection.getStart().getLine().equals(
                  initialSelection.getEnd().getLine());
            
      if (singleLineAction)
      {
         String currentLine = docDisplay_.getCurrentLine();
         Match firstCharMatch = Pattern.create("([^\\s])").match(currentLine, 0);
         if (firstCharMatch != null)
         {
            indent = currentLine.substring(0, firstCharMatch.getIndex());
         }
         else
         {
            indent = currentLine;
         }
      }
      
      boolean selectionCollapsed = docDisplay_.isSelectionCollapsed();
      docDisplay_.fitSelectionToLines(true);
      String selection = docDisplay_.getSelectionValue();

      // If any line's first non-whitespace character is not #, then the whole
      // selection should be commented. Exception: If the whole selection is
      // whitespace, then we comment out the whitespace.
      Match match = Pattern.create("^\\s*[^" + c + "\\s]").match(selection, 0);
      boolean uncomment = match == null && selection.trim().length() != 0;
      if (uncomment)
      {
         String prefix = c + "'?";
         selection = selection.replaceAll("((^|\\n)\\s*)" + prefix + " ?", "$1");
      }
      else
      {
         // Check to see if we're commenting something that looks like Roxygen
         Pattern pattern = Pattern.create("(^\\s*@)|(\\n\\s*@)");
         boolean isRoxygen = pattern.match(selection, 0) != null;
         
         if (isRoxygen)
            c = c + "'";
         
         if (singleLineAction)
            selection = indent + c + " " + selection.replaceAll("^\\s*", "");
         else
         {
            selection = c + " " + selection.replaceAll("\\n", "\n" + c + " ");

            // If the selection ends at the very start of a line, we don't want
            // to comment out that line. This enables Shift+DownArrow to select
            // one line at a time.
            if (selection.endsWith("\n" + c + " "))
               selection = selection.substring(0, selection.length() - 1 - c.length());
         }
      }

      docDisplay_.replaceSelection(selection);
      
      if (selectionCollapsed)
         docDisplay_.collapseSelection(true);
      
      if (singleLineAction)
      {
         int offset = c.length() + 1;
         String line = docDisplay_.getCurrentLine();
         Match matchPos = Pattern.create("([^\\s])").match(line, 0);
         
         InputEditorSelection newSelection;
         if (uncomment)
         {
            if (initialSelection.isEmpty())
            {
               newSelection = new InputEditorSelection(
                     initialSelection.getStart().movePosition(-offset, true),
                     initialSelection.getStart().movePosition(-offset, true));
            }
            else
            {
               newSelection = new InputEditorSelection(
                     initialSelection.getStart().movePosition(matchPos.getIndex(), false),
                     initialSelection.getEnd().movePosition(-offset, true));
            }
         }
         else
         {
            if (initialSelection.isEmpty())
            {
               newSelection = new InputEditorSelection(
                     initialSelection.getStart().movePosition(offset, true),
                     initialSelection.getStart().movePosition(offset, true));
            }
            else
            {
               newSelection = new InputEditorSelection(
                     initialSelection.getStart().movePosition(matchPos.getIndex() + offset, false),
                     initialSelection.getEnd().movePosition(offset, true));
            }
         }
         docDisplay_.setSelection(newSelection);
      }
      
      docDisplay_.focus();
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
      if (docUpdateSentinel_ == null)
         return;

      // only Shiny files get the deploy command, so we can be confident we're
      // deploying an app here
      events_.fireEvent(RSConnectActionEvent.DeployAppEvent(
            docUpdateSentinel_.getPath(), null));
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
      return YamlFrontMatter.getFrontMatter(docDisplay_.getCode());
   }
   
   private void applyRmdFrontMatter(String yaml)
   {
      String code = docDisplay_.getCode();
      String newCode = YamlFrontMatter.applyFrontMatter(code, yaml);
      if (!code.equals(newCode)) 
      {
         docDisplay_.setCode(newCode, true);
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
   
   private void updateRmdFormatList()
   {
      RmdSelectedTemplate selTemplate = getSelectedTemplate();
      if (selTemplate == null)
      {
         view_.setFormatOptionsVisible(false);
         return;
      }
      
      else if (selTemplate.isShiny)
      {
         view_.setIsShinyFormat(selTemplate.format != null &&
                                selTemplate.format.endsWith(
                                      RmdOutputFormat.OUTPUT_PRESENTATION_SUFFIX));
         return;
      }
      
      // we know which template this doc is using--populate the format list
      // with the formats available in the template
      String formatUiName = "";
      JsArray<RmdTemplateFormat> formats = selTemplate.template.getFormats();
      List<String> formatList = new ArrayList<String>();
      List<String> valueList = new ArrayList<String>();
      List<String> extensionList = new ArrayList<String>();
      for (int i = 0; i < formats.length(); i++)
      {
         String uiName = formats.get(i).getUiName();
         formatList.add(uiName);
         valueList.add(formats.get(i).getName());
         extensionList.add(formats.get(i).getExtension());
         if (formats.get(i).getName().equals(selTemplate.format))
         {
            formatUiName = uiName;
         }
      }
      view_.setFormatOptions(fileType_, formatList, valueList, extensionList,
                             formatUiName);
   }
   
   private void setRmdFormat(String formatName)
   {
      RmdSelectedTemplate selTemplate = getSelectedTemplate();
      if (selTemplate == null)
         return;
      
      // if this is the current format, we don't need to change the front matter
      if (selTemplate.format.equals(formatName))
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
      String prefix = StringUtil.getCommonPrefix(lines, true);
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
      sourceActiveDocument(true);
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
   
   @Handler
   void onInsertChunk()
   {
      Position pos = moveCursorToNextInsertLocation();
      InsertChunkInfo insertChunkInfo = docDisplay_.getInsertChunkInfo();
      if (insertChunkInfo != null)
      {
         docDisplay_.insertCode(insertChunkInfo.getValue(), false);
         Position cursorPosition = insertChunkInfo.getCursorPosition();
         docDisplay_.setCursorPosition(Position.create(
               pos.getRow() + cursorPosition.getRow(),
               cursorPosition.getColumn()));
         docDisplay_.focus();
      }
      else
      {
         assert false : "Mode did not have insertChunkInfo available";
      }
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
   
   @Handler
   void onExecuteCurrentChunk()
   {
      // HACK: This is just to force the entire function tree to be built.
      // It's the easiest way to make sure getCurrentScope() returns
      // a Scope with an end.
      docDisplay_.getScopeTree();
      
      executeSweaveChunk(scopeHelper_.getCurrentSweaveChunk(), false);
   }
   
   @Handler
   void onExecuteNextChunk()
   {
      // HACK: This is just to force the entire function tree to be built.
      // It's the easiest way to make sure getCurrentScope() returns
      // a Scope with an end.
      docDisplay_.getScopeTree();
      
      executeSweaveChunk(scopeHelper_.getNextSweaveChunk(), true);
   }
   
   @Handler
   void onExecutePreviousChunks()
   {
      withPreservedSelection(new Command() {

         @Override
         public void execute()
         {
            // HACK: This is just to force the entire function tree to be built.
            // It's the easiest way to make sure getCurrentScope() returns
            // a Scope with an end.
            docDisplay_.getScopeTree();
            
            // see if there is a region of code in the current chunk to execute
            Range currentChunkRange = null;
            Scope currentScope = scopeHelper_.getCurrentSweaveChunk();
            if (currentScope != null)
            {
               // get end position (always execute the current line unless
               // the cursor is at the beginning of it)
               Position endPos = docDisplay_.getCursorPosition();
               if (endPos.getColumn() >0)
                  endPos = Position.create(endPos.getRow()+1, 0);
               
               currentChunkRange = Range.fromPoints(currentScope.getBodyStart(),
                                                    endPos);
            }
            
            // execute the previous chunks
            Scope[] previousScopes = scopeHelper_.getPreviousSweaveChunks();
            for (Scope scope : previousScopes)
               executeSweaveChunk(scope, false);
            
            // execute code from the current chunk if we have it
            if (currentChunkRange != null)
              codeExecution_.executeRange(currentChunkRange);
         }
      });    
   }
   
   private void withPreservedSelection(Command command)
   { 
      // save the selection and scroll position for restoration
      int scrollPosition = docDisplay_.getScrollTop();
      Position start = docDisplay_.getSelectionStart();
      Position end = docDisplay_.getSelectionEnd();
      AnchoredSelection anchoredSelection = 
                           docDisplay_.createAnchoredSelection(start,end);
      
      // execute the command
      command.execute();
      
      // restore the selection and scroll position
      anchoredSelection.apply();
      docDisplay_.scrollToY(scrollPosition);
   }

   private void executeSweaveChunk(final Scope chunk, 
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
            docDisplay_.setSelection(
                docDisplay_.createSelection(range.getStart(), range.getEnd()));
            if (!range.isEmpty())
            {
               codeExecution_.setLastExecuted(range.getStart(), range.getEnd());
               String code = scopeHelper_.getSweaveChunkText(chunk);
               events_.fireEvent(new SendToConsoleEvent(code, true));

               docDisplay_.collapseSelection(true);
            }
         }
      };
      
      // Rmd allows server-side prep for chunk execution
      if (fileType_.isRmd())
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

   @SuppressWarnings("unused")
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
   
  
   private void sourceActiveDocument(final boolean echo)
   {
      docDisplay_.focus();

      // If the document being sourced is a Shiny file, run the app instead.
      if (fileType_.isR() && 
          extendedType_.equals("shiny")) 
      {
         runShinyApp();
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
                        true,
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
                           true,
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
            RStudioGinjector.INSTANCE.getShinyApplication()
                                     .launchShinyApplication(getPath());
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
   void onMarkdownHelp()
   {
      events_.fireEvent(new ShowHelpEvent("help/doc/markdown_help.html")) ;
   }
   
   @Handler
   void onUsingRMarkdownHelp()
   {
      if (extendedType_.equals("rmarkdown"))
         globalDisplay_.openRStudioLink("using_rmarkdown");
      else
         globalDisplay_.openRStudioLink("using_markdown");
   }
   
   @Handler
   void onAuthoringRPresentationsHelp()
   {
      globalDisplay_.openRStudioLink("authoring_presentations");
   }
   
   @Handler
   void onRcppHelp()
   {
      globalDisplay_.openRStudioLink("rcpp_help");
   }
   
   @Handler
   void onDebugHelp()
   {
      globalDisplay_.openRStudioLink("visual_debugger");
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
      
      if (extendedType == "rmarkdown")
         renderRmd();
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
      saveThenExecute(null, new Command() {
         @Override
         public void execute()
         {
            boolean asTempfile = isPackageDocumentationFile();
            
            rmarkdownHelper_.renderRMarkdown(
               docUpdateSentinel_.getPath(),
               docDisplay_.getCursorPosition().getRow() + 1,
               null,
               docUpdateSentinel_.getEncoding(),
               asTempfile,
               isShinyDoc(),
               false);
         }
      });  
   }
   
   private boolean isShinyDoc()
   {
      try
      {
         RmdSelectedTemplate template = getSelectedTemplate();
         return (template != null) && template.isShiny;
      }
      catch(Exception e)
      {
         Debug.log(e.getMessage());
         return false;
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
                                       "Unable to Compile Notebook", 
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
               rmarkdownHelper_.renderNotebookv2(docUpdateSentinel_);
            }
         });
      }
      else
      {
         if (!rmarkdownHelper_.verifyPrerequisites("Compile Notebook",
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
   
            ScopeList scopeList = new ScopeList(docDisplay_);
            scopeList.removeAll(ScopeList.ANON_BRACE);
            Scope scope = scopeList.findLast(new ContainsFoldPredicate(
                  Range.fromPoints(docDisplay_.getSelectionStart(),
                                   docDisplay_.getSelectionEnd())));
   
            if (scope == null)
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
            // If no selection, unfold the closest fold on the current row
   
            Position pos = range.getStart();
   
            AceFold startCandidate = null;
            AceFold endCandidate = null;
   
            for (AceFold f : JsUtil.asIterable(docDisplay_.getFolds()))
            {
               if (startCandidate == null
                   && f.getStart().getRow() == pos.getRow()
                   && f.getStart().getColumn() >= pos.getColumn())
               {
                  startCandidate = f;
               }
   
               if (f.getEnd().getRow() == pos.getRow()
                   && f.getEnd().getColumn() <= pos.getColumn())
               {
                  endCandidate = f;
               }
            }
   
            if (startCandidate == null ^ endCandidate == null)
            {
               docDisplay_.unfold(startCandidate != null ? startCandidate
                                                          : endCandidate);
            }
            else if (startCandidate != null)
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
   
   boolean useScopeTreeFolding()
   {
      return docDisplay_.hasScopeTree() && !fileType_.isRmd();
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
      public void withUpdatedDoc(final CommandWithArg<String> onUpdated)
      {
         docUpdateSentinel_.withSavedDoc(new Command() {
            @Override
            public void execute()
            {
               onUpdated.execute(docUpdateSentinel_.getPath());
            }
         });

      }

      @Override
      public void cppCompletionOperation(final CppCompletionOperation operation)
      {
         if (isCompletionEnabled())
         {
            withUpdatedDoc(new CommandWithArg<String>() {
               @Override
               public void execute(String docPath)
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
      releaseOnDismiss.add(prefs.useVimMode().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setUseVimMode(arg);
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
      // path to the new content path
      if (view_ != null && extendedType_ != null && path != null &&
          (extendedType_.equals("shiny") || extendedType_.equals("rmarkdown")))
      {
         view_.setPublishPath(extendedType_.equals("shiny") ?
               RSConnect.CONTENT_TYPE_APP : 
               RSConnect.CONTENT_TYPE_DOCUMENT, path);
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
   private RoxygenHelper roxygenHelper_;
   private boolean ignoreDeletes_;
   private boolean forceSaveCommandActive_ = false;
   private final TextEditingTargetScopeHelper scopeHelper_;
   private TextEditingTargetSpelling spelling_;
   private BreakpointManager breakpointManager_;
   private final LintManager lintManager_;
   private final SnippetHelper snippets_;

   // Allows external edit checks to supercede one another
   private final Invalidation externalEditCheckInvalidation_ =
         new Invalidation();
   // Prevents external edit checks from happening too soon after each other
   private final IntervalTracker externalEditCheckInterval_ =
         new IntervalTracker(1000, true);
   private final EditingTargetCodeExecution codeExecution_;
   
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
}
