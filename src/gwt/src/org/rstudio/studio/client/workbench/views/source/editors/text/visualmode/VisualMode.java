/*
 * VisualMode.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.Rendezvous;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.patch.TextChange;
import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.core.client.widget.ProgressPanel;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.panmirror.PanmirrorChanges;
import org.rstudio.studio.client.panmirror.PanmirrorCode;
import org.rstudio.studio.client.panmirror.PanmirrorContext;
import org.rstudio.studio.client.panmirror.PanmirrorKeybindings;
import org.rstudio.studio.client.panmirror.PanmirrorOptions;
import org.rstudio.studio.client.panmirror.PanmirrorSetMarkdownResult;
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommands;
import org.rstudio.studio.client.panmirror.events.PanmirrorFocusEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorNavigationEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorStateChangeEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorUpdatedEvent;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocFormat;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIDisplay;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsSource;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditorContainer;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import elemental2.core.JsObject;
import jsinterop.base.Js;


public class VisualMode implements VisualModeEditorSync,
                                   CommandPaletteEntrySource
{
   public VisualMode(TextEditingTarget target,
                     TextEditingTarget.Display view,
                     TextEditingTargetRMarkdownHelper rmarkdownHelper,
                     DocDisplay docDisplay,
                     DirtyState dirtyState,
                     DocUpdateSentinel docUpdateSentinel,
                     EventBus eventBus,
                     final ArrayList<HandlerRegistration> releaseOnDismiss)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      target_ = target;
      view_ = view;
      docDisplay_ = docDisplay;
      dirtyState_ = dirtyState;
      docUpdateSentinel_ = docUpdateSentinel;
      progress_ = new ProgressPanel(ProgressImages.createSmall(), 200);
      
      // create peer helpers
      visualModeFormat_ = new VisualModePanmirrorFormat(docUpdateSentinel_, docDisplay_, target_, view_);
      visualModeExec_ = new VisualModeChunkExec(docUpdateSentinel_, rmarkdownHelper, this);
      visualModeContext_ = new VisualModePanmirrorContext(docUpdateSentinel_, target_, visualModeExec_, visualModeFormat_);
      visualModeLocation_ = new VisualModeEditingLocation(docUpdateSentinel_, docDisplay_);
      visualModeWriterOptions_ = new VisualModeMarkdownWriter();
      visualModeNavigation_ = new VisualModeNavigation(navigationContext_);
      
      // create widgets that the rest of startup (e.g. manageUI) may rely on
      initWidgets();
             
      // manage UI (then track changes over time)
      manageUI(isActivated(), false);
      releaseOnDismiss.add(onDocPropChanged(TextEditingTarget.RMD_VISUAL_MODE, (value) -> {
         manageUI(isActivated(), true);
      }));
      
      // sync to outline visible prop
      releaseOnDismiss.add(onDocPropChanged(TextEditingTarget.DOC_OUTLINE_VISIBLE, (value) -> {
         withPanmirror(() -> {
            panmirror_.showOutline(getOutlineVisible(), getOutlineWidth(), true);
         });
      }));
      
      // sync to user pref changed
      releaseOnDismiss.add(prefs_.enableVisualMarkdownEditingMode().addValueChangeHandler((value) -> {
         view_.manageCommandUI();
      }));
   } 
   
   
   @Inject
   public void initialize(GlobalDisplay globalDisplay,
                          Commands commands, 
                          UserPrefs prefs, 
                          SourceServerOperations source,
                          Session session)
   {
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      prefs_ = prefs;
      source_ = source;
      sessionInfo_ = session.getSessionInfo();
   }
   
   private void initWidgets()
   {
      findReplaceButton_ = new ToolbarButton(
         ToolbarButton.NoText,
         "Find/Replace",
         FindReplaceBar.getFindIcon(),
         (event) -> {
            HasFindReplace findReplace = getFindReplace();
            findReplace.showFindReplace(!findReplace.isFindReplaceShowing());
         }
      );
   }
   
   public boolean isActivated()
   {
      return docUpdateSentinel_.getBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
   }
   
   public void activate(ScheduledCommand completed)
   {
      if (!isActivated())
      {
         docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, true);
         manageUI(true, true, completed);
      }
      else if (isLoading_)
      {
         onReadyHandlers_.add(completed);
      }
      else
      {
         completed.execute();
      }
   }
  
   public void deactivate(ScheduledCommand completed)
   {
      if (isActivated())
      {
         docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
         manageUI(false, true, completed);
      }
      else
      {
         completed.execute();
      }
   }
   
   @Override
   public void syncToEditor(boolean activatingEditor)
   {
      syncToEditor(activatingEditor, null);
   }

   @Override
   public void syncToEditor(boolean activatingEditor, Command ready)
   {
      // This is an asynchronous task, that we want to behave in a mostly FIFO
      // way when overlapping calls to syncToEditor are made.

      // Each syncToEditor operation can be thought of as taking place in three
      // phases:
      //
      // 1 - Synchronously gathering state from panmirror, and kicking off the
      //     async pandoc operation
      // 2 - The pandoc operation itself--this happens completely off the UI
      //     thread (in a different process in fact)
      // 3 - With the result from pandoc, do some synchronous processing, sync
      //     the source editor, and invoke the `ready` parameter
      //
      // Part 2 is a "pure" operation so it doesn't matter when it runs. What
      // matters is that phase 1 gathers state at the moment it's called, and
      // if there are multiple operations in progress simultaneously, that the
      // order in which different phase 3's are invoked reflect the order the
      // operations were started. For example, if syncToEditor was called once
      // (A) and then again (B), any of these sequences are fine:
      //   A1->A2->A3->B1->B2->B3
      //   A1->B1->A2->B2->A3->B3
      // or even
      //   A1->B1->B2->A2->A3->B3
      // but NOT
      //   A1->A2->B1->B2->B3->A3
      //
      // because if A1 comes before B1, then A3 must come before B3.

      // Our plan of execution is:
      // 1. Start the async operation
      // 2a. Wait for the async operation to finish
      // 2b. Wait for all preceding async operations to finish
      // 3. Run our phase 3 logic and ready.execute()
      // 4. Signal to the next succeeding async operation (if any) that we're
      //    done

      // We use syncToEditorQueue_ to enforce the FIFO ordering. Because we
      // don't know whether the syncToEditorQueue_ or the pandoc operation will
      // finish first, we use a Rendezvous object to make sure both conditions
      // are satisfied before we proceed.
      Rendezvous rv = new Rendezvous(2);

      syncToEditorQueue_.addCommand(new SerializedCommand() {
         @Override
         public void onExecute(Command continuation)
         {
            // We pass false to arrive() because it's important to not invoke
            // the continuation before our phase 3 work has completed; the whole
            // point is to enforce ordering of phase 3.
            rv.arrive(() -> {
               continuation.execute();
            }, false);
         }
      });

      if (isPanmirrorActive() && (activatingEditor || isDirty_)) {
         // set flags
         isDirty_ = false;
         
         withPanmirror(() -> {
            
            VisualModeMarkdownWriter.Options writerOptions = visualModeWriterOptions_.optionsFromConfig(panmirror_.getPandocFormatConfig(true));
            
            panmirror_.getMarkdown(writerOptions.options, kSerializationProgressDelayMs, 
                                   new CommandWithArg<JsObject>() {
               @Override
               public void execute(JsObject obj)
               {
                  PanmirrorCode markdown = Js.uncheckedCast(obj);
                  rv.arrive(() -> {
                     if (markdown == null) {
                        // note that ready.execute() is never called in the error case
                        return;
                     }
                     
                     // apply diffs unless the wrap column changed (too expensive)
                     if (!writerOptions.wrapColumnChanged) 
                     {
                        TextEditorContainer.Changes changes = toEditorChanges(markdown);
                        getSourceEditor().applyChanges(changes, activatingEditor); 
                     }
                     else
                     {
                        getSourceEditor().setCode(markdown.code);
                     }
                    
                     // if the format comment has changed then show the reload prompt
                     if (panmirrorFormatConfig_.requiresReload()) {
                        view_.showPanmirrorFormatChanged(() -> {
                           // dismiss the warning bar
                           view_.hideWarningBar();
                           // this will trigger the refresh b/c the format changed
                           syncFromEditorIfActivated();
                          
                        });
                     }
                     
                     // callback
                     if (ready != null) {
                        ready.execute();
                     }
                  }, true);  
               } 
            });
         });
      } else {
         // Even if ready is null, it's important to arrive() so the
         // syncToEditorQueue knows it can continue
         rv.arrive(() -> {
            if (ready != null) {
               ready.execute();
            }
         }, true);
      }
   }


   @Override
   public void syncFromEditorIfActivated()
   {
      if (isActivated()) 
      {
         // get reference to the editing container 
         TextEditorContainer editorContainer = view_.editorContainer();
         
         // show progress
         progress_.beginProgressOperation(400);
         editorContainer.activateWidget(progress_);
         
         syncFromEditor((success) -> {
            // clear progress
            progress_.endProgressOperation();
            
            // re-activate panmirror widget
            editorContainer.activateWidget(panmirror_, false);
            
         }, false);
      }
   }
   
   
   @Override
   public void syncFromEditor(CommandWithArg<Boolean> done, boolean focus)
   {      
      // flag to prevent the document being set to dirty when loading
      // from source mode
      loadingFromSource_ = true;
      
      // if there is a previous format comment and it's changed then
      // we need to tear down the editor instance and create a new one
      if (panmirrorFormatConfig_ != null && panmirrorFormatConfig_.requiresReload()) 
      {
         panmirrorFormatConfig_ = null;
         view_.editorContainer().removeWidget(panmirror_);
         panmirror_ = null;
      }
      
      withPanmirror(() -> {
         
         String editorCode = getEditorCode();
         
         VisualModeMarkdownWriter.Options writerOptions = visualModeWriterOptions_.optionsFromCode(editorCode);
         
         panmirror_.setMarkdown(editorCode, writerOptions.options, true, kCreationProgressDelayMs, 
                                new CommandWithArg<JsObject>() {
            @Override
            public void execute(JsObject obj)
            {
               // get result
               PanmirrorSetMarkdownResult result = Js.uncheckedCast(obj);
               
               // bail on error
               if (result == null)
               {
                  if (done != null)
                     done.execute(false);
                  return;
               }
               
               // update flags
               isDirty_ = false;
               loadingFromSource_ = false;
               
               // if pandoc's view of the document doesn't match the editor's we 
               // need to reset the editor's code (for both dirty state and 
               // so that diffs are efficient)
               if (result.canonical != editorCode)
               {
                  getSourceEditor().setCode(result.canonical);
                  markDirty();
               }
               
               // completed
               if (done != null)
                  done.execute(true);
               
               Scheduler.get().scheduleDeferred(() -> {
                      
                  // if we are being focused it means we are switching from source mode, in that
                  // case sync our editing location to what it is in source 
                  if (focus)
                  { 
                     panmirror_.focus();
                     panmirror_.setEditingLocation(
                        visualModeLocation_.getSourceOutlneLocation(), 
                        visualModeLocation_.savedEditingLocation()
                     ); 
                  }
                  
                  // show any warnings
                  PanmirrorPandocFormat format = panmirror_.getPandocFormat();
                  if (result.unrecognized.length > 0) 
                  {
                     view_.showWarningBar("Unrecognized Pandoc token(s); " + String.join(", ", result.unrecognized));
                  } 
                  else if (format.warnings.invalidFormat.length() > 0)
                  {
                     view_.showWarningBar("Invalid Pandoc format: " + format.warnings.invalidFormat);
                  }
                  else if (format.warnings.invalidOptions.length > 0)
                  {
                     view_.showWarningBar("Unsupported extensions for markdown mode: " + String.join(", ", format.warnings.invalidOptions));;
                  }
                  else if (visualModeFormat_.isBookdownProjectDocument() && 
                           !sessionInfo_.getBookdownHasRenumberFootnotes() &&
                           !bookdownVersionWarningShown)
                  {
                     view_.showWarningBar(
                       "Bookdown package update required for compatibility with visual mode.",
                       "Learn more", () -> {
                          globalDisplay_.openRStudioLink("visual_markdown_editing-bookdown-upgrade", false);                   
                       });
                     bookdownVersionWarningShown = true;
                  }
                  
               });          
            }
         });
      });
   }

   public void getCanonicalChanges(String code, CommandWithArg<PanmirrorChanges> completed)
   {   
      withPanmirror(() -> {
         VisualModeMarkdownWriter.Options writerOptions = visualModeWriterOptions_.optionsFromCode(code);
         panmirror_.getCanonical(code, writerOptions.options, kSerializationProgressDelayMs, 
                                 (markdown) -> {
            if  (markdown != null) 
            {
               if (!writerOptions.wrapColumnChanged)
               {
                  PanmirrorUIToolsSource sourceTools = new PanmirrorUITools().source;
                  TextChange[] changes = sourceTools.diffChars(code, markdown, 1);
                  completed.execute(new PanmirrorChanges(null, changes));
               }
               else
               {
                  completed.execute(new PanmirrorChanges(markdown, null));
               }
            }
            else
            {
               completed.execute(null);
            }
         });
      });
   }
   
   
   public void manageCommands()
   {
      // hookup devtools
      syncDevTools();
      
      // disable commands
      disableForVisualMode(
        commands_.jumpTo(),
        commands_.jumpToMatching(),
        commands_.showDiagnosticsActiveDocument(),
        commands_.goToHelp(),
        commands_.goToDefinition(),
        commands_.extractFunction(),
        commands_.extractLocalVariable(),
        commands_.renameInScope(),
        commands_.reflowComment(),
        commands_.commentUncomment(),
        commands_.insertRoxygenSkeleton(),
        commands_.reindent(),
        commands_.reformatCode(),
        commands_.findSelectAll(),
        commands_.findFromSelection(),
        commands_.executeSetupChunk(),
        commands_.executeAllCode(),
        commands_.executeCode(),
        commands_.executeCodeWithoutFocus(),
        commands_.executeCodeWithoutMovingCursor(),
        commands_.executeCurrentFunction(),
        commands_.executeCurrentLine(),
        commands_.executeCurrentParagraph(),
        commands_.executeCurrentSection(),
        commands_.executeCurrentStatement(),
        commands_.executeFromCurrentLine(),
        commands_.executeLastCode(),
        commands_.executeNextChunk(),
        commands_.executeSubsequentChunks(),
        commands_.executeToCurrentLine(),
        commands_.sendToTerminal(),
        commands_.runSelectionAsJob(),
        commands_.runSelectionAsLauncherJob(),
        commands_.sourceActiveDocument(),
        commands_.sourceActiveDocumentWithEcho(),
        commands_.pasteWithIndentDummy(),
        commands_.fold(),
        commands_.foldAll(),
        commands_.unfold(),
        commands_.unfoldAll(),
        commands_.yankAfterCursor(),
        commands_.notebookExpandAllOutput(),
        commands_.notebookCollapseAllOutput(),
        commands_.notebookClearAllOutput(),
        commands_.notebookClearOutput(),
        commands_.goToLine(),
        commands_.wordCount(),
        commands_.restartRClearOutput(),
        commands_.restartRRunAllChunks(),
        commands_.profileCode()
      );
   }
   
   
   public void unmanageCommands()
   {
      restoreDisabledForVisualMode();
   }
   
   public void insertChunk(String chunkPlaceholder, int rowOffset, int colOffset)
   {
      panmirror_.insertChunk(chunkPlaceholder, rowOffset, colOffset);
   }
   
   public void executeChunk()
   {
      panmirror_.execCommand(PanmirrorCommands.ExecuteCurrentRmdChunk);
   }
   
   public void executePreviousChunks()
   {
      panmirror_.execCommand(PanmirrorCommands.ExecutePreviousRmdChunks);
   }
   
   public HasFindReplace getFindReplace()
   {
      if (panmirror_ != null) {
         return panmirror_.getFindReplace();
      } else {
         return new HasFindReplace() {
            public boolean isFindReplaceShowing() { return false; }
            public void showFindReplace(boolean defaultForward) {}
            public void hideFindReplace() {}
            public void findNext() {}
            public void findPrevious() {}
            public void replaceAndFind() {}
            
         };
      }  
   }
   
   public ToolbarButton getFindReplaceButton()
   {
      return findReplaceButton_;
   }

   public boolean isVisualModePosition(SourcePosition position)
   {
      return visualModeNavigation_.isVisualModePosition(position);
   }
   
   public void navigate(SourcePosition position, boolean recordCurrentPosition)
   {
      visualModeNavigation_.navigate(position, recordCurrentPosition);
   }
   
   public void navigateToXRef(String xref, boolean recordCurrentPosition)
   {
      visualModeNavigation_.navigateToXRef(xref, recordCurrentPosition);
   }
   
   public void recordCurrentNavigationPosition()
   {
      visualModeNavigation_.recordCurrentNavigationPosition();
   }
   
   public SourcePosition getSourcePosition()
   {
      return visualModeNavigation_.getSourcePosition();
   }
   
   public boolean isAtRow(SourcePosition position)
   {
      if (visualModeNavigation_.isVisualModePosition(position))
      {
         return position.getRow() == getSourcePosition().getRow();
      }
      else
      {
         return false;
      }
            
   }
   
   public void activateDevTools()
   {
      withPanmirror(() -> {
         panmirror_.activateDevTools();
      });
   }
   
   public void onClosing()
   {
      if (syncOnIdle_ != null)
         syncOnIdle_.suspend();
      if (saveLocationOnIdle_ != null)
         saveLocationOnIdle_.suspend();
   }
  

   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      return panmirror_.getCommandPaletteItems();
   }

   
   private void manageUI(boolean activate, boolean focus)
   {
      manageUI(activate, focus, () -> {});
   }
   
   private void manageUI(boolean activate, boolean focus, ScheduledCommand completed)
   {
      // validate the activation
      if (activate)
      {
         String invalid = validateActivation();
         if (invalid != null) 
         {
            docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
            view_.showWarningBar(invalid);
            return;
         }
      }
      
      // manage commands
      manageCommands();
      
      // manage toolbar buttons / menus in display
      view_.manageCommandUI();
      
      // get references to the editing container and it's source editor
      TextEditorContainer editorContainer = view_.editorContainer();
        
      // visual mode enabled (panmirror editor)
      if (activate)
      {
         // set flag indicating that we are loading
         isLoading_ = true;
         
         // show progress (as this may well require either loading the 
         // panmirror library for the first time or a reload of visual mode,
         // which is normally instant but for very, very large documents
         // can take a couple of seconds)
         progress_.beginProgressOperation(400);
         editorContainer.activateWidget(progress_);
         
         CommandWithArg<Boolean> done = (success) -> {
            
            // clear progress
            progress_.endProgressOperation();
            
            if (success)
            {
               // sync to editor outline prefs
               panmirror_.showOutline(establishOutlineVisible(), getOutlineWidth());
               
               // show find replace button
               findReplaceButton_.setVisible(true);
               
               // activate widget
               editorContainer.activateWidget(panmirror_, focus);
               
               // begin save-on-idle behavior
               syncOnIdle_.resume();
               saveLocationOnIdle_.resume();
               
               // execute completed hook
               Scheduler.get().scheduleDeferred(completed);  
               
               // clear loading flag and execute any onReady handlers
               isLoading_ = false;
               onReadyHandlers_.forEach(handler -> { Scheduler.get().scheduleDeferred(handler); });
               onReadyHandlers_.clear();
            }
            else
            {
               editorContainer.activateEditor(focus);
               docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
            }
         };
         
         withPanmirror(() -> {
            // if we aren't currently active then set our markdown based
            // on what's currently in the source ditor
            if (!isPanmirrorActive()) 
            {
               syncFromEditor(done, focus);
            }
            else
            {
               done.execute(true);
            }  
         });
      }
      
      // visual mode not enabled (source editor)
      else 
      {
         // sync any pending edits, then activate the editor
         syncToEditor(true, () -> {
            
            unmanageCommands();
            
            // hide find replace button
            findReplaceButton_.setVisible(false);
            
            editorContainer.activateEditor(focus); 
            
            if (syncOnIdle_ != null)
               syncOnIdle_.suspend();
            
            if (saveLocationOnIdle_ != null)
               saveLocationOnIdle_.suspend();
            
            // execute completed hook
            Scheduler.get().scheduleDeferred(completed);
         });  
      }
   }

   private void markDirty()
   {
      dirtyState_.markDirty(true);
      source_.setSourceDocumentDirty(
            docUpdateSentinel_.getId(), true, 
            new VoidServerRequestCallback());
   }
   
   private TextEditorContainer.Changes toEditorChanges(PanmirrorCode panmirrorCode)
   {
      // code to diff
      String fromCode = getEditorCode();
      String toCode = panmirrorCode.code;
         
      // do the diff (timeout after 1 second). note that we only do this 
      // once the user has stopped typing for 1 second so it's not something
      // that will run continuously during editing (in which case a much
      // lower timeout would be warranted). note also that timeouts are for
      // the diff planning phase so we will still get a valid diff back
      // even if the timeout occurs.
      PanmirrorUIToolsSource sourceTools = new PanmirrorUITools().source;
      TextChange[] changes = sourceTools.diffChars(fromCode, toCode, 1);
     
      // return changes w/ cursor
      return new TextEditorContainer.Changes(
         changes, 
         panmirrorCode.cursor != null 
            ? new TextEditorContainer.Cursor(
                  panmirrorCode.cursor.row, panmirrorCode.cursor.column
              )
            : null
      );
   }
   
   
   private void syncDevTools()
   {
      if (panmirror_ != null && panmirror_.devToolsLoaded()) 
         panmirror_.activateDevTools();
   }
   
   
   private void withPanmirror(Command ready)
   {
      if (panmirror_ == null)
      {
         // create panmirror (no progress b/c we alread have pane progress)
         PanmirrorContext context = createPanmirrorContext(); 
         PanmirrorOptions options = panmirrorOptions();   
         PanmirrorWidget.Options widgetOptions = new PanmirrorWidget.Options();
         PanmirrorWidget.create(context, visualModeFormat_.formatSource(), 
                                options, widgetOptions, kCreationProgressDelayMs, (panmirror) -> {
         
            // save reference to panmirror
            panmirror_ = panmirror;
            
            // track format comment (used to detect when we need to reload for a new format)
            panmirrorFormatConfig_ = new VisualModeReloadChecker(view_);
            
            // remove some keybindings that conflict with the ide
            // (currently no known conflicts)
            disableKeys();
           
            // periodically sync edits back to main editor
            syncOnIdle_ = new DebouncedCommand(1000)
            {
               @Override
               protected void execute()
               {
                  if (isDirty_ && !panmirror_.isInitialDoc())
                     syncToEditor(false);
               }
            };
            
            // periodically save selection
            saveLocationOnIdle_ = new DebouncedCommand(1000)
            {
               @Override
               protected void execute()
               {
                  visualModeLocation_.saveEditingLocation(panmirror_.getEditingLocation());
               }
            };
            
            // set dirty flag + nudge idle sync on change
            panmirror_.addPanmirrorUpdatedHandler(new PanmirrorUpdatedEvent.Handler()
            {
               @Override
               public void onPanmirrorUpdated(PanmirrorUpdatedEvent event)
               {
                  // set flag and nudge sync on idle
                  isDirty_ = true;
                  syncOnIdle_.nudge();
                  
                  // update editor dirty state if necessary
                  if (!loadingFromSource_ && !dirtyState_.getValue())
                     markDirty();
               }  
            });
            
            // save selection
            panmirror_.addPanmirrorStateChangeHandler(new PanmirrorStateChangeEvent.Handler()
            {
               @Override
               public void onPanmirrorStateChange(PanmirrorStateChangeEvent event)
               {
                  saveLocationOnIdle_.nudge();
               }
            });
            
            // forward navigation event
            panmirror_.addPanmirrorNavigationHandler(new PanmirrorNavigationEvent.Handler()
            {
               @Override
               public void onPanmirrorNavigation(PanmirrorNavigationEvent event)
               {
                  visualModeNavigation_.onNavigated(event.getNavigation());
               }
            });
            
            // check for external edit on focus
            panmirror_.addPanmirrorFocusHandler(new PanmirrorFocusEvent.Handler()
            {  
               @Override
               public void onPanmirrorFocus(PanmirrorFocusEvent event)
               {
                  target_.checkForExternalEdit(100);
               }
            });
             
            // track changes in outline sidebar and save as prefs
            panmirror_.addPanmirrorOutlineVisibleHandler((event) -> {
               setOutlineVisible(event.getVisible());
            });
            panmirror_.addPanmirrorOutlineWidthHandler((event) -> {
               setOutlineWidth(event.getWidth());
            });
            
            // manage latch state of findreplace button
            panmirror_.addPanmirrorFindReplaceVisibleHandler((event) -> {
               findReplaceButton_.setLeftImage(event.getVisible() 
                     ? FindReplaceBar.getFindLatchedIcon()
                     : FindReplaceBar.getFindIcon());
            });
            
            // good to go!
            ready.execute();
         });
      }
      else
      {
         // panmirror already created
         ready.execute();
      }
   } 
   
   private PanmirrorContext createPanmirrorContext()
   {
      PanmirrorUIDisplay.ShowContextMenu showContextMenu = (commands, clientX, clientY) -> {
         return panmirror_.showContextMenu(commands, clientX, clientY);
      };
      
      return visualModeContext_.createContext(showContextMenu);
   }
   
   
   private String getEditorCode()
   {
      return VisualModeUtil.getEditorCode(view_);
   }   
   
   // is our widget active in the editor container
   private boolean isPanmirrorActive()
   {
      return view_.editorContainer().isWidgetActive(panmirror_);
   }
   
   private TextEditorContainer.Editor getSourceEditor()
   {
      return view_.editorContainer().getEditor();
   }
  
   private boolean establishOutlineVisible()
   {
      return target_.establishPreferredOutlineWidgetVisibility(
         prefs_.visualMarkdownEditingShowDocOutline().getValue()
      );  
   }
   
   private boolean getOutlineVisible()
   {
      return target_.getPreferredOutlineWidgetVisibility(
         prefs_.visualMarkdownEditingShowDocOutline().getValue()
      );
   }
   
   private void setOutlineVisible(boolean visible)
   {
      target_.setPreferredOutlineWidgetVisibility(visible);
   }
   
   private double getOutlineWidth()
   {
      return target_.getPreferredOutlineWidgetSize();
   }
   
   private void setOutlineWidth(double width)
   {
      target_.setPreferredOutlineWidgetSize(width);
   }
     
   
   private void disableKeys(String... commands)
   {
      PanmirrorKeybindings keybindings = disabledKeybindings(commands);
      panmirror_.setKeybindings(keybindings);
   }
   
   private PanmirrorKeybindings disabledKeybindings(String... commands)
   {
      PanmirrorKeybindings keybindings = new PanmirrorKeybindings();
      for (String command : commands)
         keybindings.add(command,  new String[0]);
      
      return keybindings;
   }
   
   private void disableForVisualMode(AppCommand... commands)
   {
      if (isActivated())
      {
         for (AppCommand command : commands)
         {
            if (command.isVisible() && command.isEnabled())
            {
               command.setEnabled(false);
               if (!disabledForVisualMode_.contains(command))
                  disabledForVisualMode_.add(command);
            }
         }
      }
   }
   
   private void restoreDisabledForVisualMode()
   {
      disabledForVisualMode_.forEach((command) -> {
         command.setEnabled(true);
      });
      disabledForVisualMode_.clear();
   } 
   
   private HandlerRegistration onDocPropChanged(String prop, ValueChangeHandler<String> handler)
   {
      return docUpdateSentinel_.addPropertyValueChangeHandler(prop, handler);
   }
   
   private VisualModeNavigation.Context navigationContext_ = new  VisualModeNavigation.Context() {

      @Override
      public String getId()
      {
         return docUpdateSentinel_.getId();
      }

      @Override
      public String getPath()
      {
         return docUpdateSentinel_.getPath();
      }

      @Override
      public PanmirrorWidget panmirror()
      {
         return panmirror_;
      }
      
   };
   
   
   private PanmirrorOptions panmirrorOptions()
   {
      // create options
      PanmirrorOptions options = new PanmirrorOptions();
      
      // use embedded codemirror for code blocks
      options.codemirror = true;
      
      // enable rmdImagePreview if we are an executable rmd
      options.rmdImagePreview = target_.canExecuteChunks();
      
      // highlight rmd example chunks
      options.rmdExampleHighlight = true;
      
      // enable chunk execution for R and Python
      options.rmdChunkExecution = VisualModeChunkExec.kRmdChunkExecutionLangs;
      
      // add focus-visible class to prevent interaction with focus-visible.js
      // (it ends up attempting to apply the "focus-visible" class b/c ProseMirror
      // is contentEditable, and that triggers a dom mutation event for ProseMirror,
      // which in turn causes us to lose table selections)
      options.className = "focus-visible";
      
      return options;
   }
   
   
   private String validateActivation()
   {
      if (this.docDisplay_.hasActiveCollabSession())
      {
         return "You cannot enter visual mode while using realtime collaboration.";
      }
      else if (visualModeFormat_.isXaringanDocument())
      {
         return "Xaringan presentations cannot be edited in visual mode.";
      }
      else
      {
         return null;
      }
   }
   
   private Commands commands_;
   private UserPrefs prefs_;
   private SourceServerOperations source_;
   private SessionInfo sessionInfo_;
   private GlobalDisplay globalDisplay_;
   
   private final TextEditingTarget target_;
   private final TextEditingTarget.Display view_;
   private final DocDisplay docDisplay_;
   private final DirtyState dirtyState_;
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private final VisualModePanmirrorFormat visualModeFormat_;
   private final VisualModeChunkExec visualModeExec_;
   private final VisualModePanmirrorContext visualModeContext_;
   private final VisualModeEditingLocation visualModeLocation_;
   private final VisualModeMarkdownWriter visualModeWriterOptions_;
   private final VisualModeNavigation visualModeNavigation_;
   
   private VisualModeReloadChecker panmirrorFormatConfig_;
   
   private DebouncedCommand syncOnIdle_; 
   private DebouncedCommand saveLocationOnIdle_;
   
   private boolean isDirty_ = false;
   private boolean loadingFromSource_ = false;
   
   private PanmirrorWidget panmirror_;
  
   private ToolbarButton findReplaceButton_;
   
   private ArrayList<AppCommand> disabledForVisualMode_ = new ArrayList<AppCommand>();
   
   private final ProgressPanel progress_;
   
   private SerializedCommandQueue syncToEditorQueue_ = new SerializedCommandQueue();
   
   private boolean isLoading_ = false;
   private List<ScheduledCommand> onReadyHandlers_ = new ArrayList<ScheduledCommand>(); 
   
   
   private static final int kCreationProgressDelayMs = 0;
   private static final int kSerializationProgressDelayMs = 5000;
   
   private static boolean bookdownVersionWarningShown = false;
  
}



