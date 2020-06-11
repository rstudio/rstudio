/*
 * TextEditingTargetVisualMode.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.Rendezvous;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.patch.TextChange;
import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.core.client.widget.ProgressPanel;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.panmirror.PanmirrorChanges;
import org.rstudio.studio.client.panmirror.PanmirrorCode;
import org.rstudio.studio.client.panmirror.PanmirrorContext;
import org.rstudio.studio.client.panmirror.PanmirrorKeybindings;
import org.rstudio.studio.client.panmirror.PanmirrorOptions;
import org.rstudio.studio.client.panmirror.PanmirrorRmdChunk;
import org.rstudio.studio.client.panmirror.PanmirrorSetMarkdownResult;
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.panmirror.PanmirrorWidget.FormatSource;
import org.rstudio.studio.client.panmirror.PanmirrorWriterOptions;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommands;
import org.rstudio.studio.client.panmirror.events.PanmirrorFocusEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorStateChangeEvent;
import org.rstudio.studio.client.panmirror.events.PanmirrorUpdatedEvent;
import org.rstudio.studio.client.panmirror.format.PanmirrorExtendedDocType;
import org.rstudio.studio.client.panmirror.format.PanmirrorFormat;
import org.rstudio.studio.client.panmirror.format.PanmirrorHugoExtensions;
import org.rstudio.studio.client.panmirror.format.PanmirrorRmdExtensions;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingLocation;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingOutlineLocation;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingOutlineLocationItem;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineItemType;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocFormat;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIContext;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIDisplay;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIExecute;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorPandocFormatConfig;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsSource;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.BlogdownConfig;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import elemental2.core.JsObject;
import jsinterop.base.Js;


public class TextEditingTargetVisualMode implements CommandPaletteEntrySource
{
   public TextEditingTargetVisualMode(TextEditingTarget target,
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
      rmarkdownHelper_ = rmarkdownHelper;
      docDisplay_ = docDisplay;
      dirtyState_ = dirtyState;
      docUpdateSentinel_ = docUpdateSentinel;
      progress_ = new ProgressPanel(ProgressImages.createSmall(), 200);
      
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
   
   @Inject
   public void initialize(Commands commands, 
                          UserPrefs prefs, 
                          EventBus events,
                          SourceServerOperations source, 
                          WorkbenchContext context,
                          Session session)
   {
      commands_ = commands;
      prefs_ = prefs;
      events_ = events;
      source_ = source;
      context_ = context;
      sessionInfo_ = session.getSessionInfo();
   }
   
   public boolean isActivated()
   {
      return docUpdateSentinel_.getBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
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
   
   public void syncToEditor(boolean activatingEditor)
   {
      syncToEditor(activatingEditor, null);
   }

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
            
            WriterOptionsContext writerOptions = writerOptionsFromVisual();
            
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
   
   
   private void syncFromEditor(CommandWithArg<Boolean> done, boolean focus)
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
         
         WriterOptionsContext writerOptions = writerOptionsFromCode(editorCode);
         
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
                     panmirror_.setEditingLocation(getSourceOutlneLocation(), savedEditingLocation()); 
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
   
   public void getCanonicalChanges(String code, CommandWithArg<PanmirrorChanges> completed)
   {   
      withPanmirror(() -> {
         WriterOptionsContext writerOptions = writerOptionsFromCode(code);
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
         PanmirrorContext context = new PanmirrorContext(uiContext(), uiDisplay(), uiExecute());
         PanmirrorOptions options = panmirrorOptions();   
         PanmirrorWidget.Options widgetOptions = new PanmirrorWidget.Options();
         PanmirrorWidget.create(context, panmirrorFormat(), options, widgetOptions, kCreationProgressDelayMs, 
                                (panmirror) -> {
         
            // save reference to panmirror
            panmirror_ = panmirror;
            
            // track format comment (used to detect when we need to reload for a new format)
            panmirrorFormatConfig_ = new FormatConfig(new PanmirrorUITools().format);
            
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
                  PanmirrorEditingLocation location = panmirror_.getEditingLocation();
                  String locationProp = + location.pos + ":" + location.scrollTop; 
                  docUpdateSentinel_.setProperty(RMD_VISUAL_MODE_LOCATION, locationProp);
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
   
   private void markDirty()
   {
      dirtyState_.markDirty(true);
      source_.setSourceDocumentDirty(
            docUpdateSentinel_.getId(), true, 
            new VoidServerRequestCallback());
   }
   
 
   // bizzarly, removing this method triggers a gwt compiler issue that
   // results in the Panmirror interop breaking! we need to investigate
   // this, but in the meantime the method remains. note that this method
   // *should not* be called as it doesn't handle re-entrancy correctly
   // (the restoring of the getActiveWidget can result in forever progress
   // when 2 calls to withProgress are in the promise chain)
   @SuppressWarnings("unused")
   private void withProgress(int delayMs, CommandWithArg<Command> command)
   {
     
      TextEditorContainer editorContainer = view_.editorContainer();
      /*
      IsHideableWidget prevWidget = editorContainer.getActiveWidget();
      progress_.beginProgressOperation(delayMs);
      editorContainer.activateWidget(progress_);
      command.execute(() -> {
         progress_.endProgressOperation();
         editorContainer.activateWidget(prevWidget);
      });
      */
   }
   
   
   private class WriterOptionsContext
   {
      public WriterOptionsContext(PanmirrorWriterOptions options, boolean wrapColumnChanged)
      {
         this.options = options;
         this.wrapColumnChanged = wrapColumnChanged;
      }
      
      public final PanmirrorWriterOptions options;
      public final boolean wrapColumnChanged;
   }
   private PanmirrorWriterOptions lastUsedWriterOptions_ = null;
   
   private WriterOptionsContext writerOptionsFromCode(String code)
   {
      PanmirrorUIToolsFormat format = new PanmirrorUITools().format;
      PanmirrorPandocFormatConfig formatConfig = format.parseFormatConfig(code, true);
      return writerOptions(formatConfig); 
   }
   
   private WriterOptionsContext writerOptionsFromVisual()
   {
      PanmirrorPandocFormatConfig formatConfig = panmirror_.getPandocFormatConfig(true);
      return writerOptions(formatConfig);
   }
   
   private WriterOptionsContext writerOptions(PanmirrorPandocFormatConfig formatConfig)
   {
      // options defaults from preferences
      PanmirrorWriterOptions options = new PanmirrorWriterOptions();
      
      // always write atx headers (e.g. ##)
      options.atxHeaders = true;
      
      // use user pref for wrapColumn
      if (prefs_.visualMarkdownEditingWrapAuto().getValue())
         options.wrapColumn = prefs_.visualMarkdownEditingWrapColumn().getValue();
      else
         options.wrapColumn = 0;
      
      // use user pref for references location
      options.references = prefs_.visualMarkdownEditingReferencesLocation().getValue();
      
      // layer in format config
      if (formatConfig.wrapColumn > 0)
         options.wrapColumn = formatConfig.wrapColumn;
      if (formatConfig.references != null)
         options.references = formatConfig.references;
      
      // check if this represents a line wrapping change
      boolean wrapColumnChanged = lastUsedWriterOptions_ != null &&
                                  lastUsedWriterOptions_.wrapColumn != options.wrapColumn;
      
      // set last used
      lastUsedWriterOptions_ = options;
      
      // return context
      return new WriterOptionsContext(options, wrapColumnChanged);
   }
   
   
   private String getEditorCode()
   {
      TextEditorContainer editorContainer = view_.editorContainer();
      TextEditorContainer.Editor editor = editorContainer.getEditor();
      return editor.getCode();
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
   
   
   private PanmirrorEditingLocation savedEditingLocation()
   {
      String location = docUpdateSentinel_.getProperty(RMD_VISUAL_MODE_LOCATION, null);
      if (StringUtil.isNullOrEmpty(location))
         return null;
      
      String[] parts = location.split(":");
      if (parts.length != 2)
         return null;
      
      try
      {
         PanmirrorEditingLocation editingLocation = new PanmirrorEditingLocation();
         editingLocation.pos = Integer.parseInt(parts[0]);
         editingLocation.scrollTop = Integer.parseInt(parts[1]);
         return editingLocation;
      }
      catch(Exception ex)
      {
         Debug.logException(ex);
         return null;
      }
      
   }
   
   private PanmirrorEditingOutlineLocation getSourceOutlneLocation()
   {
      // if we are at the very top of the file then this is a not a good 'hint'
      // for where to navigate to, in that case return null
      Position cursorPosition = docDisplay_.getCursorPosition();
      if (cursorPosition.getRow() == 0 && cursorPosition.getColumn() == 0)
         return null;
      
      // if we don't have an outline then return null
      if (docDisplay_.getScopeTree().length() == 0)
         return null;
      
      // build the outline
      ArrayList<Pair<PanmirrorEditingOutlineLocationItem, Scope>> outlineItems = 
         new ArrayList<Pair<PanmirrorEditingOutlineLocationItem, Scope>>();
      buildOutlineLocation(docDisplay_.getScopeTree(), outlineItems);
      
      // return the location, set the active item by scanning backwards until
      // we find an item with a position before the cursor
      boolean foundActive = false;
      Position cursorPos = docDisplay_.getCursorPosition();
      ArrayList<PanmirrorEditingOutlineLocationItem> items = new ArrayList<PanmirrorEditingOutlineLocationItem>();
      for (int i = outlineItems.size() - 1; i >= 0; i--) 
      {
         Pair<PanmirrorEditingOutlineLocationItem, Scope> outlineItem = outlineItems.get(i);
         PanmirrorEditingOutlineLocationItem item = outlineItem.first;
         Scope scope = outlineItem.second;
         if (!foundActive && scope.getPreamble().isBefore(cursorPos))
         {
            item.active = true;
            foundActive = true;
         }
         items.add(0, item);
      }
   
      PanmirrorEditingOutlineLocation location = new PanmirrorEditingOutlineLocation();
      location.items = items.toArray(new PanmirrorEditingOutlineLocationItem[] {});
      return location;
   }
   
   private void buildOutlineLocation(JsArray<Scope> scopes, 
                                     ArrayList<Pair<PanmirrorEditingOutlineLocationItem, Scope>> outlineItems)
   {
      for (int i = 0; i<scopes.length(); i++)
      {
         // get scope
         Scope scope = scopes.get(i);
         
         // create item + default values
         PanmirrorEditingOutlineLocationItem item = new PanmirrorEditingOutlineLocationItem();
         item.level = scope.getDepth();
         item.title = scope.getLabel();
         item.active = false;
         
         // process yaml, headers, and chunks
         if (scope.isYaml())
         {
            item.type = PanmirrorOutlineItemType.YamlMetadata;
            outlineItems.add(new Pair<PanmirrorEditingOutlineLocationItem, Scope>(item, scope));
         }
         else if (scope.isMarkdownHeader())
         {
            item.type = PanmirrorOutlineItemType.Heading;
            outlineItems.add(new Pair<PanmirrorEditingOutlineLocationItem, Scope>(item, scope));
            buildOutlineLocation(scope.getChildren(), outlineItems);
         }
         else if (scope.isChunk())
         {
            item.type = PanmirrorOutlineItemType.RmdChunk;
            item.title = scope.getChunkLabel();
            outlineItems.add(new Pair<PanmirrorEditingOutlineLocationItem, Scope>(item, scope));
         }
      }
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
   
   private void executeRmdChunk(PanmirrorRmdChunk chunk)
   { 
      // ignore null chunk
      if (chunk == null)
         return;
      
      // see if this is for a supported language (bail if not)
      String chunkLang = null;
      for (String lang : kRmdChunkExecutionLangs) 
      {
         if (chunk.lang.equalsIgnoreCase(lang)) 
         {
            chunkLang = lang;
            break;
         }
      }
      if (chunkLang == null)
         return;
      
      // execute the chunk
      final String finalChunkLang = chunkLang;
      this.syncToEditor(false, () -> {
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
                  () -> {
                     events_.fireEvent(new SendToConsoleEvent(chunk.code, 
                                                              finalChunkLang, 
                                                              true));
                  }
               );
            }
         });  
      }); 
   }
   
   private HandlerRegistration onDocPropChanged(String prop, ValueChangeHandler<String> handler)
   {
      return docUpdateSentinel_.addPropertyValueChangeHandler(prop, handler);
   }
   
   private PanmirrorUIContext uiContext()
   {
      PanmirrorUIContext uiContext = new PanmirrorUIContext();
      uiContext.getDefaultResourceDir = () -> {  
         if (docUpdateSentinel_.getPath() != null)
            return FileSystemItem.createDir(docUpdateSentinel_.getPath()).getParentPathString();
         else
            return context_.getCurrentWorkingDir().getPath();
      };
      
      uiContext.mapPathToResource = path -> {
         FileSystemItem resourceDir = FileSystemItem.createDir(uiContext.getDefaultResourceDir.get());
         FileSystemItem file = FileSystemItem.createFile(path);
         String resourcePath = file.getPathRelativeTo(resourceDir);
         if (resourcePath != null)
         {
            return resourcePath;
         }
         else
         {
            // try for hugo asset
            return pathToHugoAsset(path);
         }
      };
      uiContext.mapResourceToURL = path -> {
         
         // see if this a hugo asset
         String hugoPath = hugoAssetPath(path);
         if (hugoPath != null)
            path = hugoPath;
         
         FileSystemItem resourceDir = FileSystemItem.createDir(uiContext.getDefaultResourceDir.get());
         return ImagePreviewer.imgSrcPathFromHref(resourceDir.getPath(), path);
      };
      uiContext.translateText = text -> {
         return text;
      };
      return uiContext;
   }
   
   private PanmirrorUIDisplay uiDisplay()
   {
      PanmirrorUIDisplay uiDisplay = new PanmirrorUIDisplay();
      
      uiDisplay.showContextMenu = (commands, clientX, clientY) -> {
         return panmirror_.showContextMenu(commands, clientX, clientY);
      };
       
      return uiDisplay;
   }
   
   private PanmirrorUIExecute uiExecute()
   {
      PanmirrorUIExecute uiExecute = new PanmirrorUIExecute();
      uiExecute.executeRmdChunk = (chunk) -> {
         executeRmdChunk(chunk);
      };
      return uiExecute;
   }
   
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
      options.rmdChunkExecution = kRmdChunkExecutionLangs;
      
      // add focus-visible class to prevent interaction with focus-visible.js
      // (it ends up attempting to apply the "focus-visible" class b/c ProseMirror
      // is contentEditable, and that triggers a dom mutation event for ProseMirror,
      // which in turn causes us to lose table selections)
      options.className = "focus-visible";
      
      return options;
   }
   
   private FormatSource panmirrorFormat()
   {
      return new PanmirrorWidget.FormatSource()
      {
         @Override
         public PanmirrorFormat getFormat(PanmirrorUIToolsFormat formatTools)
         {
            // create format
            PanmirrorFormat format = new PanmirrorFormat();
            
            // see if we have a format comment
            PanmirrorPandocFormatConfig formatComment = formatTools.parseFormatConfig(getEditorCode(), true);
              
            // doctypes
            if (formatComment.doctypes == null || formatComment.doctypes.length == 0)
            {
               List<String> configDocTypes = new ArrayList<String>();
               if (isBookdownProjectDocument())
                  configDocTypes.add(PanmirrorExtendedDocType.bookdown);
               if (isHugoProjectDocument() || isHugodownDocument())
                  configDocTypes.add(PanmirrorExtendedDocType.hugo);
               format.docTypes = configDocTypes.toArray(new String[] {});
            }
            else if (formatComment.doctypes != null)
            {
               format.docTypes = formatComment.doctypes;
            }
            else
            {
               format.docTypes = new String[] {};
            }
                  
            // mode and extensions         
            // non-standard mode and extension either come from a format comment,
            // a detection of an alternate engine (likely due to blogdown/hugo)
            
            Pair<String,String> alternateEngine = alternateMarkdownEngine();
            if (formatComment.mode != null)
            {
               format.pandocMode = formatComment.mode;
               format.pandocExtensions = StringUtil.notNull(formatComment.extensions);
            }
            else if (alternateEngine != null)
            {
               format.pandocMode = alternateEngine.first;
               format.pandocExtensions = alternateEngine.second;
               if (formatComment.extensions != null)
                  format.pandocExtensions += formatComment.extensions;
            }
            else
            {
               format.pandocMode = "markdown";
               format.pandocExtensions = "+autolink_bare_uris+tex_math_single_backslash";
               if (formatComment.extensions != null)
                  format.pandocExtensions += formatComment.extensions;
            }
              
            // rmdExtensions
            
            // get any rmd extensions declared by the user in the format comment
            PanmirrorRmdExtensions rmdExtensions = rmdExtensionsFromFormatConfig(formatComment);
          
            // create extensions
            format.rmdExtensions = new PanmirrorRmdExtensions();
            
            // chunk execution enabled if the target can execute
            format.rmdExtensions.codeChunks = target_.canExecuteChunks();
            
            // support for bookdown part headers is always enabled b/c typing 
            // (PART\*) in the visual editor would result in an escaped \, which
            // wouldn't parse as a port. the odds of (PART\*) occurring naturally
            // in an H1 are also vanishingly small
            format.rmdExtensions.bookdownPart = true;
            
            // support for bookdown cross-references is always enabled b/c they would not 
            // serialize correctly in markdown modes that don't escape @ if not enabled,
            // and the odds that someone wants to literally write @ref(foo) w/o the leading
            // \ are vanishingly small)
            format.rmdExtensions.bookdownXRef = true;
            format.rmdExtensions.bookdownXRefUI = 
               hasBookdownCrossReferences() || rmdExtensions.bookdownXRefUI;
            
            // enable blogdown math in code (e.g. `$math$`) however don't enable
            // it if the user has expressely added +tex_math_dollars to the format (as this
            // means that they've arranged for an alternate means of rendering math e.g. a 
            // goldmark extension)
            boolean texMathDollarsEnabled = format.pandocExtensions.contains("+tex_math_dollars");
            if (!texMathDollarsEnabled)
            {
               format.rmdExtensions.blogdownMathInCode = 
                  hasBlogdownMathInCode() || rmdExtensions.blogdownMathInCode;
            }
            
            // hugoExtensions
            format.hugoExtensions = new PanmirrorHugoExtensions();
            
            // always enable hugo shortcodes (w/o this we can end up destroying
            // shortcodes during round-tripping, and we don't want to require that 
            // blogdown files be opened within projects). this idiom is obscure 
            // enough that it's vanishingly unlikely to affect non-blogdown docs
            format.hugoExtensions.shortcodes = true;
                 
            // return format
            return format;
         }
      };
   }
   
   private String validateActivation()
   {
      if (this.docDisplay_.hasActiveCollabSession())
      {
         return "You cannot enter visual mode while using realtime collaboration.";
      }
      else if (isXaringanDocument())
      {
         return "Xaringan presentations cannot be edited in visual mode.";
      }
      else
      {
         return null;
      }
   }

   
   private boolean isBookdownProjectDocument() 
   {
      return sessionInfo_.getBuildToolsBookdownWebsite() && isDocInProject();
   }
   
   private boolean isBlogdownProjectDocument() 
   {
      return getBlogdownConfig().is_blogdown_project && isDocInProject() && !isHugodownDocument();
   }
   
   private boolean isHugoProjectDocument()
   {
      return (getBlogdownConfig().is_hugo_project && isDocInProject());
   }
   
   private boolean isHugodownDocument()
   {
      return getOutputFormats().contains("hugodown::hugo_document");
   }
   
   private boolean isDistillDocument()
   {
      return (sessionInfo_.getIsDistillProject() && isDocInProject()) ||
             getOutputFormats().contains("distill::distill_article");
   }
   
   private boolean isXaringanDocument()
   {
      List<String> formats = getOutputFormats();
      for (String format : formats)
      {
         if (format.startsWith("xaringan"))
            return true;
      }
      return false;
   }
   
   private boolean hasBookdownCrossReferences()
   {
      return isBookdownProjectDocument() || isBlogdownProjectDocument() || isDistillDocument();
   }
   
   private boolean hasBlogdownMathInCode()
   {
      boolean blogdownWithNonPandocMarkdown = isBlogdownProjectDocument() && (alternateMarkdownEngine() != null);
      return blogdownWithNonPandocMarkdown || isHugodownDocument();
   }
   
   
   private String pathToHugoAsset(String path)
   {
      if (isHugoProjectDocument())
      {
         FileSystemItem file = FileSystemItem.createFile(path);
         for (FileSystemItem dir : hugoStaticDirs())
         {
            String assetPath = file.getPathRelativeTo(dir);
            if (assetPath != null)
               return "/" + assetPath;
         }
         
         return null;
      }
      else
      {
         return null;
      }
   }
   
   // TODO: currently can only serve image preview out of main static dir
   // (to resolve we'd need to create a server-side handler that presents
   // a union view of the various static dirs, much as hugo does internally)
   private String hugoAssetPath(String asset)
   {
      if (isHugoProjectDocument() && asset.startsWith("/"))
      {
         return hugoStaticDirs().get(0).completePath(asset.substring(1));
      }
      else
      {
         return null;
      }
   }
   
   private List<FileSystemItem> hugoStaticDirs()
   {
      FileSystemItem siteDir = getBlogdownConfig().site_dir;
      List<FileSystemItem> staticDirs = new ArrayList<FileSystemItem>();
      for (String dir : getBlogdownConfig().static_dirs)
         staticDirs.add(FileSystemItem.createDir(siteDir.completePath(dir)));
      return staticDirs;
    
   }
   
   private BlogdownConfig getBlogdownConfig()
   {
      return sessionInfo_.getBlogdownConfig();
   }
   
   private PanmirrorRmdExtensions rmdExtensionsFromFormatConfig(PanmirrorPandocFormatConfig config)
   {
      PanmirrorRmdExtensions rmdExtensions = new PanmirrorRmdExtensions();
      if (config.rmdExtensions != null)
      {
         rmdExtensions.bookdownXRefUI = config.rmdExtensions.contains("+bookdown_cross_references");
         rmdExtensions.blogdownMathInCode = config.rmdExtensions.contains("+tex_math_dollars_in_code");
      }
      return rmdExtensions;
   }
   
   // see if there's an alternate markdown engine in play
   private Pair<String,String> alternateMarkdownEngine()
   {
      // if we have a doc
      String docPath = docUpdateSentinel_.getPath();
      if (docPath != null)
      {   
         // collect any alternate mode we may have
         BlogdownConfig config = getBlogdownConfig();
         Pair<String,String> alternateMode = new Pair<String,String>(
            config.markdown_engine,
            config.markdown_extensions
         );
         
         // if it's a blogdown document
         if (isBlogdownProjectDocument())
         {
            // if it has an extension indicating hugo will render markdown
            String extension = FileSystemItem.getExtensionFromPath(docPath);
            if (extension.compareToIgnoreCase(".md") == 0 ||
                extension.compareToIgnoreCase(".Rmarkdown") == 0)
            {
               return alternateMode;
            }
         }
         // if it's a hugo document (that is not a blogdown document)
         else if (isHugoProjectDocument())
         {
            return alternateMode;
         }
         // hugodown document that lives outside of a project
         else if (isHugodownDocument())
         {
            return new Pair<String,String>("goldmark", "");
         }
         
      }
   
      return null;   
   }
 
   
   private boolean isDocInProject()
   {  
      // if we are in a project
      if (context_.isProjectActive())
      {
         // if the doc path is  null let's assume it's going to be saved
         // within the current project
         String docPath = docUpdateSentinel_.getPath();
         if (docPath != null)
         {
            // if the doc is in the project directory
            FileSystemItem docFile = FileSystemItem.createFile(docPath);
            FileSystemItem projectDir = context_.getActiveProjectDir();
            return docFile.getPathRelativeTo(projectDir) != null;
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
   
  
   private List<String> getOutputFormats()
   {
      String yaml = YamlFrontMatter.getFrontMatter(docDisplay_);
      if (yaml == null)
      {
         return new ArrayList<String>();
      }
      else
      {
         List<String> formats = TextEditingTargetRMarkdownHelper.getOutputFormats(yaml);
         if (formats == null)
            return new ArrayList<String>();
         else
            return formats;   
      }
   }
   
   private class FormatConfig
   {
      public FormatConfig(PanmirrorUIToolsFormat formatTools)
      {
         formatTools_ = formatTools;
         config_ = formatTools_.parseFormatConfig(getEditorCode(), true);
      }
      
      @SuppressWarnings("unused")
      public boolean hasChanged()
      {
         PanmirrorPandocFormatConfig config = formatTools_.parseFormatConfig(getEditorCode(), true);
         return !PanmirrorPandocFormatConfig.areEqual(config,  config_);   
      }
      
      public boolean requiresReload()
      {
         PanmirrorPandocFormatConfig config = formatTools_.parseFormatConfig(getEditorCode(), true);
         return !PanmirrorPandocFormatConfig.editorBehaviorConfigEqual(config,  config_);  
      }
      
      
      private final PanmirrorUIToolsFormat formatTools_;
      private final PanmirrorPandocFormatConfig config_;
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
   
   private Commands commands_;
   private EventBus events_;
   private UserPrefs prefs_;
   private WorkbenchContext context_;
   private SessionInfo sessionInfo_;
   private SourceServerOperations source_;
   
   private final TextEditingTarget target_;
   private final TextEditingTarget.Display view_;
   private final TextEditingTargetRMarkdownHelper rmarkdownHelper_;
   private final DocDisplay docDisplay_;
   private final DirtyState dirtyState_;
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private DebouncedCommand syncOnIdle_; 
   private DebouncedCommand saveLocationOnIdle_;
   
   private boolean isDirty_ = false;
   private boolean loadingFromSource_ = false;
   
   private PanmirrorWidget panmirror_;
   private FormatConfig panmirrorFormatConfig_;
   private ToolbarButton findReplaceButton_;
   
   private ArrayList<AppCommand> disabledForVisualMode_ = new ArrayList<AppCommand>();
   
   private final ProgressPanel progress_;
   
   private SerializedCommandQueue syncToEditorQueue_ = new SerializedCommandQueue();
   
   private static final int kCreationProgressDelayMs = 0;
   private static final int kSerializationProgressDelayMs = 5000;
  
   
   private static final String RMD_VISUAL_MODE_LOCATION = "rmdVisualModeLocation";   
   private final static String[] kRmdChunkExecutionLangs = new String[] { "R", "Python" };
}



