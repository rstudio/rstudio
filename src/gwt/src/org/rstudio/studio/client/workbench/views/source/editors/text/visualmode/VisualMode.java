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

import org.rstudio.core.client.BrowseCap;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.PreemptiveTaskQueue;
import org.rstudio.core.client.Rendezvous;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.patch.TextChange;
import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.core.client.widget.ProgressPanel;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.Value;
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
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingOutlineLocation;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingOutlineLocationItem;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineItemType;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocFormat;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIDisplay;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsSource;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditorContainer;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.events.VisualModeSpellingAddToDictionaryEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceDocAddedEvent;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import elemental2.core.JsObject;
import jsinterop.base.Js;


public class VisualMode implements VisualModeEditorSync,
                                   CommandPaletteEntrySource,
                                   SourceDocAddedEvent.Handler,
                                   VisualModeSpelling.Context,
                                   VisualModeConfirm.Context,
                                   VisualModeSpellingAddToDictionaryEvent.Handler
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
      visualModeChunks_ = new VisualModeChunks(docUpdateSentinel_, docDisplay_, target_, this);
      visualModeLocation_ = new VisualModeEditingLocation(docUpdateSentinel_, docDisplay_);
      visualModeWriterOptions_ = new VisualModeMarkdownWriter(docUpdateSentinel_, visualModeFormat_);
      visualModeNavigation_ = new VisualModeNavigation(navigationContext_);
      visualModeConfirm_ = new VisualModeConfirm(docUpdateSentinel_, docDisplay, this);
      visualModeSpelling_ = new VisualModeSpelling(docUpdateSentinel_, docDisplay, this);
      visualModeContext_ = new VisualModePanmirrorContext(
         docUpdateSentinel_, 
         target_, 
         visualModeChunks_, 
         visualModeFormat_,
         visualModeSpelling_
      );
      
      // create widgets that the rest of startup (e.g. manageUI) may rely on
      initWidgets();
      
      // subscribe to source doc added
      releaseOnDismiss.add(eventBus.addHandler(SourceDocAddedEvent.TYPE, this));
      
      // subscribe to spelling invalidation event
      releaseOnDismiss.add(eventBus.addHandler(VisualModeSpellingAddToDictionaryEvent.TYPE, this));
             
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
   } 
   
   /**
    * Classification of synchronization types from the visual editor to the code
    * editor.
    */
   public enum SyncType
   {
      // A normal synchronization (usually performed on idle)
      SyncTypeNormal,
      
      // A synchronization performed prior to executing code
      SyncTypeExecution,
      
      // A synchronization performed in order to activate the code editor
      SyncTypeActivate
   }
   
   @Inject
   public void initialize(Commands commands, 
                          UserPrefs prefs, 
                          SourceServerOperations source)
   {
      commands_ = commands;
      prefs_ = prefs;
      source_ = source;
   }
   
   public void onDismiss()
   {
      
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
   
   
   public boolean isVisualEditorActive()
   {
      return view_.editorContainer().isWidgetActive(panmirror_);
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
   public void syncToEditor(SyncType syncType)
   {
      syncToEditor(syncType, null);
   }

   @Override
   public void syncToEditor(SyncType syncType, Command ready)
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

      if (isVisualEditorActive() && (syncType == SyncType.SyncTypeActivate || isDirty_)) {
         // set flags
         isDirty_ = false;
         
         withPanmirror(() -> {
            
            VisualModeMarkdownWriter.Options writerOptions = 
                  visualModeWriterOptions_.optionsFromConfig(panmirror_.getPandocFormatConfig(true));
            
            panmirror_.getMarkdown(writerOptions.options, kSerializationProgressDelayMs, 
                                   new CommandWithArg<JsObject>() {
               @Override
               public void execute(JsObject obj)
               {
                  PanmirrorCode markdown = Js.uncheckedCast(obj);
                  rv.arrive(() ->
                  {
                     if (markdown == null)
                     {
                        // note that ready.execute() is never called in the error case
                        return;
                     }
                     
                     // we are about to mutate the document, so create a single
                     // shot handler that will adjust the known position of
                     // items in the outline (we do this opportunistically
                     // unless executing code)
                     if (markdown.location != null && syncType != SyncType.SyncTypeExecution)
                     {
                        final Value<HandlerRegistration> handler = new Value<HandlerRegistration>(null);
                        handler.setValue(docDisplay_.addScopeTreeReadyHandler((evt) ->
                        {
                           alignScopeOutline(markdown.location);
                           handler.getValue().removeHandler();
                        }));
                     }
                     
                     // apply diffs unless the wrap column changed (too expensive)
                     if (!writerOptions.wrapChanged) 
                     {
                        TextEditorContainer.Changes changes = toEditorChanges(markdown);
                        getSourceEditor().applyChanges(changes, syncType == SyncType.SyncTypeActivate); 
                     }
                     else
                     {
                        getSourceEditor().setCode(markdown.code);
                     }
                     
                     // if the format comment has changed then show the reload prompt
                     if (panmirrorFormatConfig_.requiresReload())
                     {
                        view_.showPanmirrorFormatChanged(() ->
                        {
                           // dismiss the warning bar
                           view_.hideWarningBar();
                           // this will trigger the refresh b/c the format changed
                           syncFromEditorIfActivated();
                          
                        });
                     }
                     
                     if (markdown.location != null && syncType == SyncType.SyncTypeExecution)
                     {
                        // if syncing for execution, force a rebuild of the scope tree 
                        alignScopeOutline(markdown.location);
                     }

                     // invoke ready callback if supplied
                     if (ready != null)
                     {
                        ready.execute();
                     }
                  }, true);  
               } 
            });
         });
      } else {
         // Even if ready is null, it's important to arrive() so the
         // syncToEditorQueue knows it can continue
         rv.arrive(() ->
         {
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
         // new editor content about to be sent to prosemirror, validate that we can edit it
         String invalid = validateActivation();
         if (invalid != null)
         {
            deactivateForInvalidSource(invalid);
            return;
         }
         
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
   public void syncFromEditor(final CommandWithArg<Boolean> done, boolean focus)
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
         
         final String editorCode = getEditorCode();
         
         final VisualModeMarkdownWriter.Options writerOptions = visualModeWriterOptions_.optionsFromCode(editorCode);
          
         // serialize these calls (they are expensive on both the server side for the call(s)
         // to pandoc, and on the client side for initialization of the editor (esp. ace editors)
         setMarkdownQueue_.addTask(new PreemptiveTaskQueue.Task()
         {
            @Override
            public String getLabel()
            {
               return target_.getTitle();
            }
            
            @Override
            public boolean shouldPreempt()
            {
               return target_.isActiveDocument();
            }
           
            @Override
            public void execute(final Command taskDone)
            {
               // join done commands
               final CommandWithArg<Boolean> allDone = (result) -> {
                  taskDone.execute();
                  if (done != null)
                     done.execute(result);
               };
               
               panmirror_.setMarkdown(editorCode, writerOptions.options, true, kCreationProgressDelayMs, 
                     new CommandWithArg<JsObject>() {
                  @Override
                  public void execute(JsObject obj)
                  {
                     // get result
                     PanmirrorSetMarkdownResult result = Js.uncheckedCast(obj);

                     // update flags
                     isDirty_ = false;
                     loadingFromSource_ = false;
                     
                     // bail on error
                     if (result == null)
                     {
                        allDone.execute(false);
                        return;
                     }

                     // show warning and terminate if there was unparsed metadata. note that the other 
                     // option here would be to have setMarkdown send the unparsed metadata back to the
                     // server to generate yaml, and then include the metadata as yaml at end the of the
                     // document. this could be done using the method outlined here: 
                     //   https://github.com/jgm/pandoc/issues/2019 
                     // specifically using this template:
                     /*
                         $if(titleblock)$
                         $titleblock$
                         $else$
                         --- {}
                         $endif$
                      */
                     /// ...with this command line: 
                     /*
                         pandoc -t markdown --template=yaml.template foo.md
                      */
                     if (JsObject.keys(result.unparsed_meta).length > 0)
                     {
                        view_.showWarningBar("Unable to activate visual mode (unsupported front matter format or non top-level YAML block)");
                        allDone.execute(false);
                        return;
                     }

                     // clear progress (for possible dialog overlays created by confirmation)
                     progress_.endProgressOperation();
                     
                     // confirm if necessary
                     visualModeConfirm_.withSwitchConfirmation(
                           
                        // allow inspection of result
                        result, 
                        
                        // onConfirmed
                        () -> {
                           // if pandoc's view of the document doesn't match the editor's we 
                           // need to reset the editor's code (for both dirty state and 
                           // so that diffs are efficient)
                           if (result.canonical != editorCode)
                           {
                              getSourceEditor().setCode(result.canonical);
                              markDirty();
                           }
                           
                           // completed
                           allDone.execute(true);
                           
                           // deferred actions
                           Scheduler.get().scheduleDeferred(() -> {
                              // if we are being focused it means we are switching from source mode, in that
                              // case sync our editing location to what it is in source 
                              if (focus)
                              { 
                                 panmirror_.focus();
                                 panmirror_.setEditingLocation(
                                       visualModeLocation_.getSourceOutlineLocation(), 
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
                           });         
                        }, 
                        
                        // onCancelled
                        () -> {
                           allDone.execute(false);
                        }
                     ); 
                  }
               });
               
            }
         });
         
         
      });
   }
   
   public boolean canWriteCanonical()
   {
      return validateActivation() == null;
   }
 
   public void getCanonicalChanges(String code, CommandWithArg<PanmirrorChanges> completed)
   {   
      withPanmirror(() -> {
         VisualModeMarkdownWriter.Options writerOptions = visualModeWriterOptions_.optionsFromCode(code);
         panmirror_.getCanonical(code, writerOptions.options, kSerializationProgressDelayMs, 
                                 (markdown) -> {
            if  (markdown != null) 
            {
               if (!writerOptions.wrapChanged)
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
   
   /**
    * Returns the width of the entire visual editor
    * 
    * @return The visual editor's width.
    */
   public int getPixelWidth()
   {
      return panmirror_.getOffsetWidth();
   }
   
   /**
    * Returns the width of the content inside the visual editor
    * 
    * @return Width of content.
    */
   public int getContentWidth()
   {
      Element[] elements = DomUtils.getElementsByClassName(panmirror_.getElement(), 
            "pm-content");
      if (elements.length < 1)
      {
         // if no root node, use the entire surface
         return getPixelWidth();
      }

      return elements[0].getOffsetWidth();
   }
   
   public void manageCommands()
   {
      // hookup devtools
      syncDevTools();
      
      // disable commands
      disableForVisualMode(
        // Disabled since it just opens the scope tree widget (which doens't
        // exist in visual mode)
        commands_.jumpTo(),

        // Disabled since diagnostics aren't active in visual mode
        commands_.showDiagnosticsActiveDocument(),

        // Disabled since we can't meaningfully select instances in several
        // embedded editors simultaneously
        commands_.findSelectAll(),

        // Disabled since code folding doesn't work in embedded editors (there's
        // no gutter in which to toggle folds)
        commands_.fold(),
        commands_.foldAll(),
        commands_.unfold(),
        commands_.unfoldAll(),

        // Disabled since we don't have line numbers in the visual editor
        commands_.goToLine()
      );
      
      // initially disable code commands (they will be re-enabled later when an
      // editor has focus)
      setCodeCommandsEnabled(false);
   }
   
   
   public void unmanageCommands()
   {
      restoreDisabledForVisualMode();
   }
   
   public void insertChunk(String chunkPlaceholder, int rowOffset, int colOffset)
   {
      panmirror_.insertChunk(chunkPlaceholder, rowOffset, colOffset);
   }

   /**
    * Perform a command after synchronizing the selection state of the visual
    * editor. Note that the command will not be performed unless focus is in a
    * code editor (as otherwise we can't map selection 1-1).
    * 
    * @param command
    */
   public void performWithSelection(Command command)
   {
      // Drive focus to the editing surface. This is necessary so we correctly
      // identify the active (focused) editor on which to perform the command.
      panmirror_.focus();
      
      // Perform the command in the active code editor, if any.
      visualModeChunks_.performWithSelection(command);
   }
   
   public DocDisplay getActiveEditor()
   {
      return activeEditor_;
   }
   
   /**
    * Sets the active (currently focused) code chunk editor.
    * 
    * @param editor The current code chunk editor, or null if no code chunk
    *   editor has focus.
    */
   public void setActiveEditor(DocDisplay editor)
   {
      activeEditor_ = editor;
      
      if (editor != null)
      {
         // A code chunk has focus; enable code commands
         setCodeCommandsEnabled(true);
      }
   }
   
   /**
    * Sets the enabled state for code commands -- i.e. those that require
    * selection to be inside a chunk of code. We disable these outside code
    * chunks.
    * 
    * @param enabled Whether to enable code commands
    */
   private void setCodeCommandsEnabled(boolean enabled)
   {
      AppCommand[] commands = {
         commands_.commentUncomment(),
         commands_.executeCode(),
         commands_.executeCodeWithoutFocus(),
         commands_.executeCodeWithoutMovingCursor(),
         commands_.executeCurrentFunction(),
         commands_.executeCurrentLine(),
         commands_.executeCurrentParagraph(),
         commands_.executeCurrentSection(),
         commands_.executeCurrentStatement(),
         commands_.executeFromCurrentLine(),
         commands_.executeToCurrentLine(),
         commands_.extractFunction(),
         commands_.extractLocalVariable(),
         commands_.goToDefinition(),
         commands_.insertRoxygenSkeleton(),
         commands_.profileCode(),
         commands_.profileCodeWithoutFocus(),
         commands_.reflowComment(),
         commands_.reformatCode(),
         commands_.reindent(),
         commands_.renameInScope(),
         commands_.runSelectionAsJob(),
         commands_.runSelectionAsLauncherJob(),
         commands_.sendToTerminal(),
      };

      for (AppCommand command : commands)
      {
         if (command.isVisible())
         {
            command.setEnabled(enabled);
         }
      }
      
   }

   public void goToNextSection()
   {
      panmirror_.execCommand(PanmirrorCommands.GoToNextSection);
   }
   
   public void goToPreviousSection()
   {
      panmirror_.execCommand(PanmirrorCommands.GoToPreviousSection);
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
   
   public void checkSpelling()
   {
      visualModeSpelling_.checkSpelling(panmirror_.getSpellingDoc());
   }

   @Override
   public void invalidateAllWords()
   {
      if (panmirror_ != null)
         panmirror_.spellingInvalidateAllWords();
   }
   
   @Override
   public void invalidateWord(String word)
   {
      if (panmirror_ != null)
         panmirror_.spellingInvalidateWord(word);
   }
   
   @Override
   public void onVisualModeSpellingAddToDictionary(VisualModeSpellingAddToDictionaryEvent event)
   {
      if (panmirror_ != null)
         panmirror_.spellingInvalidateWord(event.getWord());
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
   
   @Override
   public String getYamlFrontMatter()
   {
      return panmirror_.getYamlFrontMatter();
   }
   
   @Override
   public boolean applyYamlFrontMatter(String yaml)
   {
      panmirror_.applyYamlFrontMatter(yaml);
      return true;
   }
   
   public void activateDevTools()
   {
      withPanmirror(() -> {
         panmirror_.activateDevTools();
      });
   }
   
   @Override
   public void onSourceDocAdded(SourceDocAddedEvent e)
   {
      if (e.getDoc().getId() != docUpdateSentinel_.getId())
         return;
      
      // when interactively adding a visual mode doc, make sure we set the focus
      // (special handling required b/c initialization of visual mode docs is
      // async so can miss the normal setting of focus)
      if (e.getMode() == Source.OPEN_INTERACTIVE &&  isActivated() && target_.isActiveDocument())
      {
         if (panmirror_ != null) 
         {
            panmirror_.focus();
         }
         else if (isLoading_)
         {
            onReadyHandlers_.add(() -> panmirror_.focus());
         }
      }
   }
   
   public void onClosing()
   {
      if (syncOnIdle_ != null)
         syncOnIdle_.suspend();
      if (saveLocationOnIdle_ != null)
         saveLocationOnIdle_.suspend();
   }
   
   public VisualModeChunk getChunkAtRow(int row)
   {
      return visualModeChunks_.getChunkAtRow(row);
   }
   
   public JsArray<ChunkDefinition> getChunkDefs()
   {
      return visualModeChunks_.getChunkDefs();
   }
   
   public ChunkDefinition getChunkDefAtRow(int row)
   {
      VisualModeChunk chunk = getChunkAtRow(row);
      if (chunk == null)
         return null;
      return chunk.getDefinition();
   }

   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      return panmirror_.getCommandPaletteItems();
   }

   public void focus(Command onComplete)
   {
      activate(() ->
      {
         panmirror_.focus(); 
         if (onComplete != null)
         {
            onComplete.execute();
         }
      });
   }

   public void setChunkLineExecState(int start, int end, int state)
   {
      visualModeChunks_.setChunkLineExecState(start, end, state);
   }
   
   public void setChunkState(Scope chunk, int state)
   {
      visualModeChunks_.setChunkState(chunk, state);
   }
   
   public void onUserSwitchingToVisualMode()
   {
      visualModeConfirm_.onUserSwitchToVisualModePending();
   }
   
   public String getSelectedText()
   {
      return panmirror_.getSelectedText();
   }
   
   public void replaceSelection(String value)
   {
      panmirror_.replaceSelection(value);
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
            deactivateWithMessage(invalid);
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
               
               // (re)inject notebook output from the editor
               target_.getNotebook().migrateCodeModeOutput();
               
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
            if (!isVisualEditorActive()) 
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
         Command activateSourceEditor = () -> {
            
            unmanageCommands();
            
            // hide find replace button
            findReplaceButton_.setVisible(false);
            
            editorContainer.activateEditor(focus); 
            
            if (syncOnIdle_ != null)
               syncOnIdle_.suspend();
            
            if (saveLocationOnIdle_ != null)
               saveLocationOnIdle_.suspend();
            
            // move notebook outputs from visual mode
            target_.getNotebook().migrateVisualModeOutput();
            
            // execute completed hook
            Scheduler.get().scheduleDeferred(completed);
         };
         
         // if we are deactivating to allow the user to edit invalid source code then don't sync
         // back to the source editor (as this would have happened b/c we inspected the contents
         // of the source editor in syncFromEditorIfActivated() and decided we couldn't edit it)
         if (deactivatingForInvalidSource_)
         {
            deactivatingForInvalidSource_ = false;
            activateSourceEditor.execute();
         }
         else
         {
            syncToEditor(SyncType.SyncTypeActivate, activateSourceEditor);
         }
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
         panmirrorCode.selection_only 
            ? new TextEditorContainer.Navigator()
            {
               @Override
               public void onNavigate(DocDisplay docDisplay)
               {
                  visualModeLocation_.setSourceOutlineLocation(panmirrorCode.location); 
               }
            }
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
                     syncToEditor(SyncType.SyncTypeNormal);
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
                  
                  // Disable code-related commands, on the presumption that we
                  // are in a prose region of the document. These commands will
                  // be re-enabled shortly if focus is sent to a code chunk, and
                  // will remain disabled if we aren't.
                  //
                  // Note that the PanmirrorFocusEvent is fired when selection
                  // exits a code chunk as well as when the entire widget loses
                  // focus.
                  setCodeCommandsEnabled(false);
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
      options.codeEditor = prefs_.visualMarkdownCodeEditor().getValue();
         
      // highlight rmd example chunks
      options.rmdExampleHighlight = true;
      
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
      else if (BrowseCap.isInternetExplorer())
      {
         return "Visual mode is not supported in Internet Explorer.";
      }
      else 
      {
         return visualModeFormat_.validateSourceForVisualMode();
      }
   }
   
   private void deactivateForInvalidSource(String invalid)
   {
      deactivatingForInvalidSource_ = true;
      deactivateWithMessage(invalid);     
   }

   private void deactivateWithMessage(String message)
   {
      docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
      view_.showWarningBar(message);
   }
   
   /**
    * Align the document's scope tree with the code chunks in visual mode.
    * 
    * @param location Array of outline locations from visual mode
    */
   private void alignScopeOutline(PanmirrorEditingOutlineLocation location)
   {
      // Get all of the chunks from the document (code view)
      ArrayList<Scope> chunkScopes = new ArrayList<Scope>();
      ScopeList chunks = new ScopeList(docDisplay_);
      chunks.selectAll(ScopeList.CHUNK);
      for (Scope chunk : chunks)
      {
         chunkScopes.add(chunk);
      }
      
      // Get all of the chunks from the outline emitted by visual mode
      ArrayList<PanmirrorEditingOutlineLocationItem> chunkItems = 
            new ArrayList<PanmirrorEditingOutlineLocationItem>();
      for (int j = 0; j < location.items.length; j++)
      {
         if (location.items[j].type == PanmirrorOutlineItemType.RmdChunk)
         {
            chunkItems.add(location.items[j]);
         }
      }
      
      // Refuse to proceed if cardinality doesn't match (consider: does this
      // need to account for deeply nested chunks that might appear in one
      // outline but not the other?)
      if (chunkScopes.size() != chunkItems.size())
      {
         Debug.logWarning(chunkScopes.size() + " chunks in scope tree, but " + 
                  chunkItems.size() + " chunks in visual editor.");
         return;
      }

      for (int k = 0; k < chunkItems.size(); k++)
      {
         PanmirrorEditingOutlineLocationItem visualItem = 
               Js.uncheckedCast(chunkItems.get(k));
         VisualModeChunk chunk = visualModeChunks_.getChunkAtVisualPosition(
               visualItem.position);
         if (chunk == null)
         {
            // This is normal; it is possible that we haven't created a chunk
            // editor at this position yet.
            continue;
         }
         chunk.setScope(chunkScopes.get(k));
      }
   }
   
   private Commands commands_;
   private UserPrefs prefs_;
   private SourceServerOperations source_;
   private DocDisplay activeEditor_;  // the current embedded editor
   
   private final TextEditingTarget target_;
   private final TextEditingTarget.Display view_;
   private final DocDisplay docDisplay_;   // the parent editor
   private final DirtyState dirtyState_;
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private final VisualModePanmirrorFormat visualModeFormat_;
   private final VisualModeChunks visualModeChunks_;
   private final VisualModePanmirrorContext visualModeContext_;
   private final VisualModeEditingLocation visualModeLocation_;
   private final VisualModeMarkdownWriter visualModeWriterOptions_;
   private final VisualModeNavigation visualModeNavigation_;
   private final VisualModeConfirm visualModeConfirm_;
   private final VisualModeSpelling visualModeSpelling_;
   
   private VisualModeReloadChecker panmirrorFormatConfig_;
   
   private DebouncedCommand syncOnIdle_; 
   private DebouncedCommand saveLocationOnIdle_;
   
   private boolean isDirty_ = false;
   private boolean loadingFromSource_ = false;
   private boolean deactivatingForInvalidSource_ = false;
   
   private PanmirrorWidget panmirror_;
  
   private ToolbarButton findReplaceButton_;
   
   private ArrayList<AppCommand> disabledForVisualMode_ = new ArrayList<AppCommand>();
   
   private final ProgressPanel progress_;
   
   private SerializedCommandQueue syncToEditorQueue_ = new SerializedCommandQueue();
   
   private boolean isLoading_ = false;
   private List<ScheduledCommand> onReadyHandlers_ = new ArrayList<ScheduledCommand>(); 
   
   private static final int kCreationProgressDelayMs = 0;
   private static final int kSerializationProgressDelayMs = 5000;
   
   // priority task queue for expensive calls to panmirror_.setMarkdown
   // (currently active tab bumps itself up in priority)
   private static PreemptiveTaskQueue setMarkdownQueue_ = new PreemptiveTaskQueue(true, false);     
}



