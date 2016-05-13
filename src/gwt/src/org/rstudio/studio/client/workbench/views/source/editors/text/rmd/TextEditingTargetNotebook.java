/*
 * TextEditingTargetNotebook.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.SendToChunkConsoleEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutput;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutputUnit;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputHandler;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList.ScopePredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetChunks;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkContextChangeEvent;
import org.rstudio.studio.client.workbench.views.source.events.NotebookRenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileHandler;
import org.rstudio.studio.client.workbench.views.source.events.SourceDocAddedEvent;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class TextEditingTargetNotebook 
               implements EditorThemeStyleChangedEvent.Handler,
                          RmdChunkOutputEvent.Handler,
                          RmdChunkOutputFinishedEvent.Handler,
                          ChunkPlotRefreshedEvent.Handler,
                          ChunkPlotRefreshFinishedEvent.Handler,
                          SendToChunkConsoleEvent.Handler, 
                          ChunkChangeEvent.Handler,
                          ChunkContextChangeEvent.Handler,
                          ConsoleWriteInputHandler,
                          ResizeHandler,
                          InterruptStatusEvent.Handler,
                          RestartStatusEvent.Handler,
                          ScopeTreeReadyEvent.Handler,
                          PinnedLineWidget.Host,
                          SourceDocAddedEvent.Handler,
                          RenderFinishedEvent.Handler
{
   private class ChunkExecQueueUnit
   {
      public ChunkExecQueueUnit(String chunkIdIn, String labelIn, int modeIn, 
            String codeIn, String optionsIn, int rowIn, String setupCrc32In)
      {
         chunkId = chunkIdIn;
         label = labelIn;
         mode = modeIn;
         options = optionsIn;
         code = codeIn;
         row = rowIn;
         setupCrc32 = setupCrc32In;
         pos = 0;
         linesExecuted = 0;
         executingRowStart = 0;
         executingRowEnd = 0;
      }
      public String chunkId;
      public String label;
      public String options;
      public String code;
      public String setupCrc32;
      public int pos;
      public int row;
      public int mode;
      public int linesExecuted;
      public int executingRowStart;
      public int executingRowEnd;
   };

   public TextEditingTargetNotebook(final TextEditingTarget editingTarget,
                                    TextEditingTargetChunks chunks,
                                    TextEditingTarget.Display editingDisplay,
                                    DocDisplay docDisplay,
                                    DirtyState dirtyState,
                                    DocUpdateSentinel docUpdateSentinel,
                                    SourceDocument document,
                                    ArrayList<HandlerRegistration> releaseOnDismiss)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;  
      dirtyState_ = dirtyState;
      releaseOnDismiss_ = releaseOnDismiss;
      initialChunkDefs_ = JsArrayUtil.deepCopy(document.getChunkDefs());
      outputs_ = new HashMap<String, ChunkOutputUi>();
      chunkExecQueue_ = new LinkedList<ChunkExecQueueUnit>();
      setupCrc32_ = docUpdateSentinel_.getProperty(LAST_SETUP_CRC32);
      editingTarget_ = editingTarget;
      chunks_ = chunks;
      editingDisplay_ = editingDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // initialize the display's default output mode based on 
      // global and per-document preferences
      syncOutputMode();
      
      releaseOnDismiss.add(docDisplay_.addEditorFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent arg0)
         {
            if (queuedResize_ != null)
            {
               onResize(queuedResize_);
               queuedResize_ = null;
            }
         }
      }));
      
      // listen for future changes to the preference and sync accordingly
      releaseOnDismiss.add(
         docUpdateSentinel_.addPropertyValueChangeHandler(CHUNK_OUTPUT_TYPE, 
            new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            changeOutputMode(event.getValue());
         }
      }));
      
      // when the width of the outline changes, treat it as a resize
      ValueChangeHandler<String> outlineWidthHandler = 
         new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            onResize(null);
         }
      };
      
      releaseOnDismiss.add(
         docUpdateSentinel_.addPropertyValueChangeHandler(
               TextEditingTarget.DOC_OUTLINE_SIZE, 
               outlineWidthHandler));
      releaseOnDismiss.add(
         docUpdateSentinel_.addPropertyValueChangeHandler(
               TextEditingTarget.DOC_OUTLINE_VISIBLE, 
               outlineWidthHandler));
      
      releaseOnDismiss.add(docDisplay_.addValueChangeHandler(
            new ValueChangeHandler<Void>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Void> arg0)
         {
            // check the setup chunk's CRC32 next time we run a chunk
            validateSetupChunk_ = true;
            
            // if the change happened in one of our scopes, clean up gutter
            // indicators for that scope (debounce this so it doesn't fire on
            // every keystroke)
            cleanErrorGutter_.schedule(250);
         }
      }));
      
      // rendering of chunk output line widgets (we wait until after the first
      // render to ensure that ace places the line widgets correctly)
      releaseOnDismiss.add(docDisplay_.addRenderFinishedHandler(this));
      
      releaseOnDismiss.add(docDisplay_.addSaveCompletedHandler(new SaveFileHandler()
      {
         @Override
         public void onSaveFile(SaveFileEvent event)
         {
            // bail if this was an autosave
            if (event.isAutosave())
               return;
            
            // bail if we don't render chunks inline (for safety--notebooks
            // are always in this mode)
            if (!docDisplay_.showChunkOutputInline())
               return;
            
            // bail if not notebook output format
            if (!editingTarget_.isRmdNotebook())
               return;
            
            String rmdPath = docUpdateSentinel_.getPath();
            
            // bail if unsaved doc (no point in generating notebooks for those)
            if (StringUtil.isNullOrEmpty(rmdPath))
               return;
            
            String outputPath = FilePathUtils.filePathSansExtension(rmdPath) + 
                                RmdOutput.NOTEBOOK_EXT;
            
            createNotebookFromCache(rmdPath, outputPath);
         }
      }));
   }
   
   public void createNotebookFromCache(final String rmdPath, final String outputPath)
   {
      Command createNotebookCmd = new Command()
      {
         @Override
         public void execute()
         {
            server_.createNotebookFromCache(
                  rmdPath,
                  outputPath,
                  new ServerRequestCallback<Void>()
                  {
                     @Override
                     public void onResponseReceived(Void v)
                     {
                        events_.fireEvent(new NotebookRenderFinishedEvent(
                              docUpdateSentinel_.getId(), 
                              docUpdateSentinel_.getPath()));
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                     }
                  });
         }
      };
      
      dependencyManager_.withRMarkdown("R Notebook", "Creating R Notebooks", createNotebookCmd);
   }
   
   @Inject
   public void initialize(EventBus events, 
         RMarkdownServerOperations server,
         ConsoleServerOperations console,
         SourceServerOperations source,
         Session session,
         UIPrefs prefs,
         Commands commands,
         Provider<SourceWindowManager> pSourceWindowManager,
         DependencyManager dependencyManager)
   {
      events_ = events;
      server_ = server;
      console_ = console;
      source_ = source;
      session_ = session;
      prefs_ = prefs;
      commands_ = commands;
      pSourceWindowManager_ = pSourceWindowManager;
      dependencyManager_ = dependencyManager;
      
      releaseOnDismiss_.add(
            events_.addHandler(RmdChunkOutputEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(RmdChunkOutputFinishedEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(ChunkPlotRefreshedEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(ChunkPlotRefreshFinishedEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(SendToChunkConsoleEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(ChunkChangeEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(ChunkContextChangeEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(ConsoleWriteInputEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(InterruptStatusEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(RestartStatusEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(SourceDocAddedEvent.TYPE, this));
      
      // subscribe to global rmd output inline preference and sync
      // again when it changes
      releaseOnDismiss_.add(
         prefs_.showRmdChunkOutputInline().addValueChangeHandler(
            new ValueChangeHandler<Boolean>() {
               @Override
               public void onValueChange(ValueChangeEvent<Boolean> event)
               {
                  syncOutputMode();
               }
            }));
   }
   
   public void onActivate()
   {
      // remember that we haven't maximized the pane in this session
      maximizedPane_ = false;
   }
   
   public void executeChunk(Scope chunk, String code, String options,
         int mode)
   {
      // get the row that ends the chunk
      int row = chunk.getEnd().getRow();
      
      // maximize the source pane if we haven't yet this session
      if (!maximizedPane_ && 
          prefs_.hideConsoleOnChunkExecute().getValue())
      {
         pSourceWindowManager_.get().maximizeSourcePaneIfNecessary();
         maximizedPane_ = true;
      }

      String chunkId = "";
      String setupCrc32 = "";
      if (isSetupChunkScope(chunk))
      {
         setupCrc32 = getChunkCrc32(chunk);
         chunkId = SETUP_CHUNK_ID;
      }
      else
      {
         ensureSetupChunkExecuted();
      }

      // find or create a matching chunk definition 
      ChunkDefinition chunkDef = getChunkDefAtRow(row, chunkId);
      if (chunkDef == null)
         return;
      chunkId = chunkDef.getChunkId();

      // check to see if this chunk is already in the execution queue--if so
      // just update the code and leave it queued
      for (ChunkExecQueueUnit unit: chunkExecQueue_)
      {
         if (unit.chunkId == chunkId)
         {
            unit.code = code;
            unit.options = options;
            unit.setupCrc32 = setupCrc32;
            unit.row = row;
            return;
         }
      }

      // if this is the currently executing chunk, don't queue it again
      if (executingChunk_ != null && executingChunk_.chunkId == chunkId)
         return;

      // decorate the gutter to show the chunk is queued
      docDisplay_.setChunkLineExecState(chunk.getBodyStart().getRow() + 1,
            chunk.getEnd().getRow(), ChunkRowExecState.LINE_QUEUED);
      chunks_.setChunkState(chunk.getPreamble().getRow(), 
            ChunkContextToolbar.STATE_QUEUED);
      
      // find the appropriate place in the queue for this chunk
      int idx = chunkExecQueue_.size();
      for (int i = 0; i < chunkExecQueue_.size(); i++)
      {
         if (chunkExecQueue_.get(i).row > row)
         {
            idx = i;
            break;
         }
      }

      // put it in the queue 
      chunkExecQueue_.add(idx, new ChunkExecQueueUnit(chunkId, 
            StringUtil.isNullOrEmpty(chunk.getChunkLabel()) ? 
                  chunk.getLabel() : chunk.getChunkLabel(),
            mode, code, options, row, setupCrc32));
      
      // record maximum queue size (for scaling progress when we start popping
      // chunks from the list)
      if (chunkExecQueue_.size() == 1)
      {
         // first item in the queue resets the max recorded size
         execQueueMaxSize_ = 1;
      }
      else
      {
         // otherwise, maintain the high water mark until the queue is empty
         execQueueMaxSize_ = Math.max(execQueueMaxSize_, 
                                      chunkExecQueue_.size());
      }
      
      // draw progress meter for the new queue in batch mode
      if (mode == MODE_BATCH)
         updateProgress();

      // if there are other chunks in the queue, let them run
      if (chunkExecQueue_.size() > 1)
         return;
      
      // if this is the first chunk in the queue, make sure the doc is saved
      // and check for param eval before we continue. since we suppress the
      // execution of additional chunks while waiting for param evaluation, we
      // need to guarantee that this state gets cleaned up correctly on error
      evaluatingParams_ = true;
      final Command completeEval = new Command()
      {
         @Override
         public void execute()
         {
            evaluatingParams_ = false;
            processChunkExecQueue();
         }
      };

      docUpdateSentinel_.withSavedDoc(new Command()
      {
         @Override
         public void execute()
         {
            server_.prepareForRmdChunkExecution(docUpdateSentinel_.getId(), 
               new ServerRequestCallback<Void>()
               {
                  @Override
                  public void onResponseReceived(Void v)
                  {
                     completeEval.execute();
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     // if we couldn't evaluate the parameters, process the queue
                     // anyway
                     Debug.logError(error);
                     completeEval.execute();
                  }
               });
         }
      }, 
      new CommandWithArg<String>()
      {
         @Override
         public void execute(String arg)
         {
            Debug.logWarning(arg);
            completeEval.execute();
         }
      });
   }
   
   public void manageCommands()
   {
      boolean inlineOutput = docDisplay_.showChunkOutputInline();   
      commands_.restartRClearOutput().setEnabled(inlineOutput);
      commands_.restartRClearOutput().setVisible(inlineOutput);
      commands_.restartRRunAllChunks().setEnabled(inlineOutput);
      commands_.restartRRunAllChunks().setVisible(inlineOutput);
      commands_.notebookCollapseAllOutput().setEnabled(inlineOutput);
      commands_.notebookCollapseAllOutput().setVisible(inlineOutput);
      commands_.notebookExpandAllOutput().setEnabled(inlineOutput);
      commands_.notebookExpandAllOutput().setVisible(inlineOutput); 
      commands_.notebookClearAllOutput().setEnabled(inlineOutput); 
      commands_.notebookClearAllOutput().setVisible(inlineOutput); 
      editingDisplay_.setNotebookUIVisible(inlineOutput);
   }
   
   public void dequeueChunk(int preambleRow)
   {
      // clean up the toolbar on the chunk
      chunks_.setChunkState(preambleRow, ChunkContextToolbar.STATE_RESTING);
      
      // find the chunk's ID
      String chunkId = getRowChunkId(preambleRow);
      if (StringUtil.isNullOrEmpty(chunkId))
         return;
      
      // clear from the execution queue and update display
      for (ChunkExecQueueUnit unit: chunkExecQueue_)
      {
         if (unit.chunkId == chunkId)
         {
            chunkExecQueue_.remove(unit);
            cleanChunkExecState(unit.chunkId);
            break;
         }
      }
   }
   
   // Command handlers --------------------------------------------------------
   //
   // Notebook-specific commands are received on the parent text editing target
   // and then dispatched here (the logic to bind commands to the active editor
   // exists only in the text editing target)
   
   public void onNotebookCollapseAllOutput()
   {
      setAllExpansionStates(ChunkOutputWidget.COLLAPSED);
   }

   public void onNotebookExpandAllOutput()
   {
      setAllExpansionStates(ChunkOutputWidget.EXPANDED);
   }
   
   public void onNotebookClearAllOutput()
   {
      if (executingChunk_ == null && chunkExecQueue_.isEmpty())
      {
         // no chunks running, just clean everything up
         removeAllChunks();
         return;
      }
      else
      {
         RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
               GlobalDisplay.MSG_INFO,
               "Chunks Currently Running",
               "Output can't be cleared because there are still chunks " +
               "running. Do you want to interrupt them?",
               false,
               new Operation()
               {
                  @Override
                  public void execute()
                  {
                     if (commands_.interruptR().isEnabled())
                         commands_.interruptR().execute();
                     
                     clearChunkExecQueue();
                     removeAllChunks();
                  }
               },
               null,
               null,
               "Interrupt and Clear Output",
               "Cancel",
               false);
      }
   }
   
   public void onRestartRRunAllChunks()
   {
      restartThenExecute(commands_.executeAllCode());
   }

   public void onRestartRClearOutput()
   {
      restartThenExecute(commands_.notebookClearAllOutput());
   }
   
   // Event handlers ----------------------------------------------------------
   
   @Override
   public void onEditorThemeStyleChanged(EditorThemeStyleChangedEvent event)
   {
      // update cached style 
      editorStyle_ = event.getStyle();
      ChunkOutputWidget.cacheEditorStyle(event.getEditorContent(),
            editorStyle_);
      
      for (ChunkOutputUi output: outputs_.values())
      {
         output.getOutputWidget().applyCachedEditorStyle();
      }
   }
   
   @Override
   public void onSendToChunkConsole(final SendToChunkConsoleEvent event)
   {
      // not for our doc
      if (event.getDocId() != docUpdateSentinel_.getId())
         return;
      
      // create or update the chunk at the given row
      final ChunkDefinition chunkDef = getChunkDefAtRow(event.getRow(), null);
      String options = TextEditingTargetRMarkdownHelper.getRmdChunkOptionText(
            event.getScope(), docDisplay_);
      
      // have the server start recording output from this chunk
      syncWidth();
      server_.setChunkConsole(docUpdateSentinel_.getId(), 
            chunkDef.getChunkId(), MODE_SINGLE, SCOPE_PARTIAL, options, 
            getPlotWidth(), charWidth_,
            new ServerRequestCallback<RmdChunkOptions>()
      {
         @Override
         public void onResponseReceived(RmdChunkOptions options)
         {
            // execute the input
            console_.consoleInput(event.getCode(), chunkDef.getChunkId(), 
                  new VoidServerRequestCallback());
         }
         @Override
         public void onError(ServerError error)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Chunk Execution Error", error.getMessage());
            
         }
      });
      
      if (outputs_.containsKey(chunkDef.getChunkId()))
         outputs_.get(chunkDef.getChunkId()).getOutputWidget()
                                         .setCodeExecuting(false, MODE_SINGLE);
   }
   
   @Override
   public void onRmdChunkOutput(RmdChunkOutputEvent event)
   {
      // ignore if not targeted at this document
      if (event.getOutput().getDocId() != docUpdateSentinel_.getId())
         return;
      
      // if nothing at all was returned, this means the chunk doesn't exist on
      // the server, so clean it up here.
      if (event.getOutput().isEmpty())
      {
         events_.fireEvent(new ChunkChangeEvent(
               docUpdateSentinel_.getId(), event.getOutput().getChunkId(), 0, 
               ChunkChangeEvent.CHANGE_REMOVE));
         return;
      }

      String chunkId = event.getOutput().getChunkId();
      
      // if this is the currently executing chunk and it has an error...
      if (executingChunk_ != null &&
          executingChunk_.chunkId == event.getOutput().getChunkId() &&
          event.getOutput().getType() == RmdChunkOutput.TYPE_SINGLE_UNIT &&
          event.getOutput().getUnit().getType() == 
             RmdChunkOutputUnit.TYPE_ERROR)
      {
         // draw the error 
         docDisplay_.setChunkLineExecState(
               executingChunk_.executingRowStart,
               executingChunk_.executingRowEnd,
               ChunkRowExecState.LINE_ERROR);
         
         // don't execute any more chunks if this chunk's options includes
         // error = FALSE
         if (!outputs_.containsKey(chunkId) ||
             !outputs_.get(chunkId).getOptions().error())
         {
            clearChunkExecQueue();
         }
      }

      // show output in matching chunk
      if (outputs_.containsKey(chunkId))
      {
         // by default, ensure chunks are visible if we aren't replaying them
         // from the cache
         boolean ensureVisible = !event.getOutput().isReplay();
         int mode = MODE_SINGLE;
         if (executingChunk_ != null &&
             executingChunk_.chunkId == event.getOutput().getChunkId())
         {
            mode = executingChunk_.mode;
         }
         
         // no need to make chunks visible in batch mode
         if (ensureVisible && mode == MODE_BATCH)
            ensureVisible = false;
         
         outputs_.get(chunkId).getOutputWidget()
                              .showChunkOutput(event.getOutput(), mode,
                                  TextEditingTargetNotebook.SCOPE_PARTIAL,
                                  ensureVisible);
      }
   }

   @Override
   public void onRmdChunkOutputFinished(RmdChunkOutputFinishedEvent event)
   {
      // ignore if not targeted at this document
      if (event.getData().getDocId() != docUpdateSentinel_.getId())
         return;
      boolean ensureVisible = true;
      
      // mark chunk execution as finished
      if (executingChunk_ != null &&
          event.getData().getChunkId() == executingChunk_.chunkId)
      {
         cleanChunkExecState(executingChunk_.chunkId);
         ensureVisible = executingChunk_.mode == MODE_SINGLE;

         // if this was the setup chunk, and no errors were encountered while
         // executing it, mark it clean
         if (!StringUtil.isNullOrEmpty(executingChunk_.setupCrc32) &&
              outputs_.containsKey(executingChunk_.chunkId))
         {
            if (!outputs_.get(executingChunk_.chunkId).hasErrors())
            {
               writeSetupCrc32(executingChunk_.setupCrc32);
            }
            else
            {
               ensureVisible = true;
               validateSetupChunk_ = true;
            }
         }
      
         executingChunk_ = null;
      }

      RmdChunkOutputFinishedEvent.Data data = event.getData();
      if (data.getType() == RmdChunkOutputFinishedEvent.TYPE_REPLAY &&
          data.getRequestId() == Integer.toHexString(requestId_)) 
      {
         state_ = STATE_INITIALIZED;
      }
      else if (data.getType() == RmdChunkOutputFinishedEvent.TYPE_INTERACTIVE &&
               data.getDocId() == docUpdateSentinel_.getId())
      {
         if (outputs_.containsKey(data.getChunkId()))
         {
            outputs_.get(data.getChunkId()).getOutputWidget()
                           .onOutputFinished(ensureVisible, data.getScope());

            // mark the document dirty (if it isn't already) since it now
            // contains notebook cache changes that haven't been committed 
            if (!dirtyState_.getValue())
            {
               dirtyState_.markDirty(false);
               source_.setSourceDocumentDirty(
                     docUpdateSentinel_.getId(), true, 
                     new VoidServerRequestCallback());
            }
         }

         // process next chunk in execution queue
         processChunkExecQueue();
      }
   }


   @Override
   public void onChunkPlotRefreshFinished(ChunkPlotRefreshFinishedEvent event)
   {
      // ignore if targeted at another document
      if (event.getData().getDocId() != docUpdateSentinel_.getId())
         return;

      lastPlotWidth_ = event.getData().getWidth();
      docUpdateSentinel_.setProperty(CHUNK_RENDERED_WIDTH, 
            "" + lastPlotWidth_);

      // clean up flag
      resizingPlotsRemote_ = false;

      // mark any plots as no longer queued for resize
      for (ChunkOutputUi output: outputs_.values())
      {
         output.getOutputWidget().setPlotPending(false);
      }
   }

   @Override
   public void onChunkPlotRefreshed(ChunkPlotRefreshedEvent event)
   {
      // ignore if targeted at another document
      if (event.getData().getDocId() != docUpdateSentinel_.getId())
         return;
      
      // find chunk containing plot and push the new plot in
      String chunkId = event.getData().getChunkId();
      if (outputs_.containsKey(chunkId))
         outputs_.get(chunkId).getOutputWidget().updatePlot(
               event.getData().getPlotUrl());
   }

   @Override
   public void onChunkChange(ChunkChangeEvent event)
   {
      if (event.getDocId() != docUpdateSentinel_.getId())
         return;
      
      switch(event.getChangeType())
      {
         case ChunkChangeEvent.CHANGE_CREATE:
            createChunkOutput(ChunkDefinition.create(event.getRow(), 
                  1, true, ChunkOutputWidget.EXPANDED, RmdChunkOptions.create(),
                  event.getChunkId(), 
                  getKnitrChunkLabel(event.getRow(), docDisplay_, 
                        new ScopeList(docDisplay_))));
            break;
         case ChunkChangeEvent.CHANGE_REMOVE:
            removeChunk(event.getChunkId());
            break;
      }
   }

   @Override
   public void onChunkContextChange(ChunkContextChangeEvent event)
   {
      contextId_ = event.getContextId();
      if (docDisplay_.isRendered())
      {
         // if the doc is already up, clean it out and replace the contents
         removeAllChunks();
         populateChunkDefs(event.getChunkDefs());
      }
      else
      {
         // otherwise, just queue up for when we do render
         initialChunkDefs_ = event.getChunkDefs();
      }
   }
   
   @Override
   public void onConsoleWriteInput(ConsoleWriteInputEvent event)
   {
      if (executingChunk_ == null)
         return;
      Scope chunk = getChunkScope(executingChunk_.chunkId);
      if (chunk == null)
         return;

      String code = docDisplay_.getCode(chunk.getBodyStart(), chunk.getEnd());
      
      // find the line just emitted (start looking from the current position)
      int idx = code.indexOf(event.getInput(), executingChunk_.pos);
      if (idx < 0)
         return;
      
      // if we found it, count the number of lines moved over
      int lines = 0;
      int end = idx + event.getInput().length();
      for (int i = executingChunk_.pos; i < end; i++)
      {
         if (code.charAt(i) == '\n')
            lines++;
      }
      
      // update display 
      int start = chunk.getBodyStart().getRow() + 
                  executingChunk_.linesExecuted;
      executingChunk_.executingRowStart = start + 1;
      executingChunk_.executingRowEnd = start + lines;
      docDisplay_.setChunkLineExecState(
            executingChunk_.executingRowStart, 
            executingChunk_.executingRowEnd,
            ChunkRowExecState.LINE_EXECUTED);
      
      // advance internal input indicator past the text just emitted
      executingChunk_.pos = idx + event.getInput().length();
      executingChunk_.linesExecuted += lines;
   }

   @Override
   public void onResize(ResizeEvent event)
   {
      // queue resize rather than processing it right away if we're not the
      // active document--in addition to being wasteful we're likely to compute
      // incorrect sizes
      if (!editingTarget_.isActiveDocument())
      {
         queuedResize_ = event;
         return;
      }
      
      // lightly debounce local resizes since they're somewhat expensive
      resizePlotsLocal_.schedule(50);
      
      // heavily debounce remote resizes since they're very expensive
      // (this actually spins up a separate R process to re-render all the
      // plots at the new resolution)
      resizePlotsRemote_.schedule(500);
   }

   @Override
   public void onRenderFinished(RenderFinishedEvent event)
   {
      // single shot rendering of output line widgets (we wait until after the
      // first render to ensure that ace places the line widgets correctly)
      if (initialChunkDefs_ != null)
      {
         for (int i = 0; i < initialChunkDefs_.length(); i++)
         {
            createChunkOutput(initialChunkDefs_.get(i));
         }
         // if we got chunk content, load initial chunk output from server
         if (initialChunkDefs_.length() > 0)
            loadInitialChunkOutput();

         initialChunkDefs_ = null;
         
         // sync to editor style changes
         editingTarget_.addEditorThemeStyleChangedHandler(
                                       TextEditingTargetNotebook.this);
         
         // read and/or set initial render width
         String renderedWidth = docUpdateSentinel_.getProperty(
               CHUNK_RENDERED_WIDTH);
         if (!StringUtil.isNullOrEmpty(renderedWidth))
            lastPlotWidth_ = StringUtil.parseInt(renderedWidth, 0);
         if (lastPlotWidth_ == 0)
         {
            lastPlotWidth_ = getPlotWidth();
            docUpdateSentinel_.setProperty(CHUNK_RENDERED_WIDTH, 
                  "" + lastPlotWidth_);
         }
      }
      else
      {
         // on ordinary render, we need to sync any chunk line widgets that have
         // just been laid out; debounce this
         syncHeightTimer_.schedule(250);
      }
   }

   @Override
   public void onInterruptStatus(InterruptStatusEvent event)
   {
      if (event.getStatus() != InterruptStatusEvent.INTERRUPT_INITIATED)
         return;
      
      // when the user interrupts R, clear any pending chunk executions
      clearChunkExecQueue();
      
      // clear currently executing chunk
      cleanCurrentExecChunk();
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      if (event.getStatus() == RestartStatusEvent.RESTART_COMPLETED)
      {
         // if we had recorded a run of the setup chunk prior to restart, clear
         // it
         if (!StringUtil.isNullOrEmpty(setupCrc32_))
            writeSetupCrc32("");
         
         // clean execution state
         clearChunkExecQueue();
         cleanCurrentExecChunk();
         
         // run any queued command
         if (postRestartCommand_ != null)
         {
            if (postRestartCommand_.isEnabled())
               postRestartCommand_.execute();
            postRestartCommand_ = null;
            return;
         }
      }
   }

   @Override
   public void onLineWidgetRemoved(LineWidget widget)
   {
      for (ChunkOutputUi output: outputs_.values())
      {
         // ignore moving widgets -- ACE doesn't have a way to move a line 
         // widget from one row to another, but we occasionally need to do this
         // to keep the output pinned to the end of the chunk
         if (output.moving())
            continue;
         if (output.getLineWidget() == widget && !output.moving())
         {
            output.remove();
            outputs_.remove(output.getChunkId());
            break;
         }
      }
   }

   @Override
   public void onSourceDocAdded(SourceDocAddedEvent e)
   {
      if (e.getDoc().getId() != docUpdateSentinel_.getId())
         return;
      
      // when interactively adding a new notebook, we maximize the source pane
      if (e.getMode() == Source.OPEN_INTERACTIVE &&
          editingTarget_.isActiveDocument() &&
          editingTarget_.isRmdNotebook())
      {
         pSourceWindowManager_.get().maximizeSourcePaneIfNecessary();
         maximizedPane_ = true;
      }
   }

   @Override
   public void onScopeTreeReady(ScopeTreeReadyEvent event)
   {
      Scope thisScope = event.getCurrentScope();
      
      // initialization 
      if (lastStart_ == null && lastEnd_ == null)
      {
         if (thisScope != null)
         {
            lastStart_ = Position.create(thisScope.getBodyStart());
            lastEnd_ = Position.create(thisScope.getEnd());
         }
         return;
      }
      
      // if the cursor is in the same scope as it was before and that 
      // scope hasn't changed, there's no need to revalidate all the chunk 
      // scopes (this is purely an optimization to avoid revalidating 
      // constantly, since scope tree ready events fire as the user types
      // and we don't want to do proportional work on the UI thread if we can
      // avoid it)
      if (thisScope != null)
      {
         Position thisStart = thisScope.getBodyStart();
         Position thisEnd = thisScope.getEnd();
         if (((lastStart_ == null && thisStart == null) ||
              (lastStart_ != null && lastStart_.compareTo(thisStart) == 0)) &&
             ((lastEnd_ == null && thisEnd == null) ||
              (lastEnd_ != null && lastEnd_.compareTo(thisEnd) == 0))) 
         {
            return;
         }

         lastStart_ = Position.create(thisScope.getBodyStart());
         lastEnd_ = Position.create(thisScope.getEnd());
      }
      else
      {
         lastStart_ = null;
         lastEnd_ = null;
      }
      
      for (ChunkOutputUi output: outputs_.values())
      {
         Scope scope = output.getScope();
         // if the scope associated with this output no longer looks like a 
         // valid chunk scope, or is considerably out of sync with the widget,
         // remove the widget
         if (scope == null || !scope.isChunk() ||
             scope.getBodyStart() == null || scope.getEnd() == null ||
             scope.getEnd().getRow() - output.getCurrentRow() > 1)
         {
            events_.fireEvent(new ChunkChangeEvent(
                  docUpdateSentinel_.getId(), output.getChunkId(), 0, 
                  ChunkChangeEvent.CHANGE_REMOVE));
         }
      }
   }

   public static String getKnitrChunkLabel(int row, DocDisplay display, 
         ScopeList scopes)
   {
      // find the chunk at this row
      Scope chunk = display.getCurrentChunk(Position.create(row, 0));
      if (chunk == null)
         return "";
      
      // if it has a name, just return it
      String label = chunk.getChunkLabel();
      if (!StringUtil.isNullOrEmpty(label))
         return label;
      
      // label the first unlabeled chunk as unlabeled-chunk-1, the next as
      // unlabeled-chunk-2, etc.
      int pos = 1;
      for (Scope curScope: scopes)
      {
         if (!curScope.isChunk())
            continue;
         if (curScope.getPreamble().getRow() == 
             chunk.getPreamble().getRow())
            break;
         if (StringUtil.isNullOrEmpty(curScope.getChunkLabel()))
            pos++;
      }
      
      return "unnamed-chunk-" + pos;
   }

   // Private methods --------------------------------------------------------
   
   private void restartThenExecute(AppCommand command)
   {
      if (commands_.restartR().isEnabled())
      {
         postRestartCommand_ = command;
         commands_.restartR().execute();
      }
   }
   
   private void cleanCurrentExecChunk()
   {
      if (executingChunk_ == null)
         return;
      
      if (outputs_.containsKey(executingChunk_.chunkId))
      {
         outputs_.get(executingChunk_.chunkId)
                 .getOutputWidget().onOutputFinished(false, 
                       TextEditingTargetNotebook.SCOPE_PARTIAL);
      }
      cleanChunkExecState(executingChunk_.chunkId);
      executingChunk_ = null;
   }
   
   private void processChunkExecQueue()
   {
      // do nothing if we're currently executing a chunk 
      if (executingChunk_ != null)
         return;

      // if the queue is empty, just make sure we've hidden the progress UI
      if (chunkExecQueue_.isEmpty())
      {
         editingTarget_.getStatusBar().hideNotebookProgress(false);
         return;
      }
      
      // do nothing if we're waiting for params to evaluate
      if (evaluatingParams_)
         return;
      
      // begin chunk execution
      final ChunkExecQueueUnit unit = chunkExecQueue_.remove();
      executingChunk_ = unit;
      
      if (unit.mode == MODE_BATCH)
         updateProgress();
      
      // let the chunk widget know it's started executing
      if (outputs_.containsKey(unit.chunkId))
      {
         ChunkOutputUi output = outputs_.get(unit.chunkId);

         // expand the chunk if it's in a fold
         Scope scope = output.getScope();
         if (scope != null)
         {
            docDisplay_.unfold(Range.fromPoints(scope.getPreamble(),
                                                scope.getEnd()));
         }

         output.getOutputWidget().setCodeExecuting(true, executingChunk_.mode);
         syncWidth();
         
         // scroll the widget into view if it's a single-shot exec
         if (executingChunk_.mode == MODE_SINGLE)
            output.ensureVisible();
      }
      
      // draw UI on chunk
      Scope chunk = getChunkScope(executingChunk_.chunkId);
      if (chunk != null)
      {
         chunks_.setChunkState(chunk.getPreamble().getRow(), 
               ChunkContextToolbar.STATE_EXECUTING);
      }
      
      server_.setChunkConsole(docUpdateSentinel_.getId(),
            unit.chunkId,
            unit.mode,
            SCOPE_CHUNK,
            unit.options,
            getPlotWidth(),
            charWidth_,
            new ServerRequestCallback<RmdChunkOptions>()
            {
               @Override
               public void onResponseReceived(RmdChunkOptions options)
               {
                  if (!options.eval() && unit.mode == MODE_BATCH)
                  {
                     // if this chunk doesn't want to be evaluated and we're in
                     // batch mode, clean its execution state and move on
                     cleanCurrentExecChunk();
                     processChunkExecQueue();
                  }
                  else 
                  {
                     if (outputs_.containsKey(unit.chunkId))
                     {
                        outputs_.get(unit.chunkId).setOptions(options);
                     }
                     console_.consoleInput(unit.code, unit.chunkId, 
                           new VoidServerRequestCallback());
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  // don't leave the chunk hung in execution state
                  cleanCurrentExecChunk();

                  // if the queue is empty, show an error; if it's not, prompt
                  // for continuing
                  if (chunkExecQueue_.isEmpty())
                     RStudioGinjector.INSTANCE.getGlobalDisplay()
                       .showErrorMessage("Chunk Execution Failed", 
                              error.getUserMessage());
                  else
                     RStudioGinjector.INSTANCE.getGlobalDisplay()
                       .showYesNoMessage(
                        GlobalDisplay.MSG_QUESTION, 
                        "Continue Execution?", 
                        "The following error was encountered during chunk " +
                        "execution: \n\n" + error.getUserMessage() + "\n\n" +
                        "Do you want to continue executing notebook chunks?", 
                        false, 
                        new Operation()
                        {
                           @Override
                           public void execute()
                           {
                              processChunkExecQueue();
                           }
                        }, 
                        null, null, "Continue", "Abort", true);
               }
            });
   }
   
   private void loadInitialChunkOutput()
   {
      if (state_ != STATE_NONE)
         return;
      
      state_ = STATE_INITIALIZING;
      requestId_ = nextRequestId_++;
      server_.refreshChunkOutput(
            docUpdateSentinel_.getPath(),
            docUpdateSentinel_.getId(), 
            contextId_,
            Integer.toHexString(requestId_), 
            new VoidServerRequestCallback());
   }
   
   private ChunkDefinition getChunkDefAtRow(int row, String newId)
   {
      ChunkDefinition chunkDef;
      
      // if there is an existing widget just modify it in place
      LineWidget widget = docDisplay_.getLineWidgetForRow(row);
      if (widget != null && 
          widget.getType().equals(ChunkDefinition.LINE_WIDGET_TYPE))
      {
         chunkDef = widget.getData();
      }
      // otherwise create a new one
      else
      {
         if (StringUtil.isNullOrEmpty(newId))
            newId = "c" + StringUtil.makeRandomId(12);
         chunkDef = ChunkDefinition.create(row, 1, true, 
               ChunkOutputWidget.EXPANDED, RmdChunkOptions.create(), newId,
               getKnitrChunkLabel(row, docDisplay_, 
                                  new ScopeList(docDisplay_)));
         
         if (newId == SETUP_CHUNK_ID)
            chunkDef.getOptions().setInclude(false);
         
         events_.fireEvent(new ChunkChangeEvent(
               docUpdateSentinel_.getId(), chunkDef.getChunkId(), row, 
               ChunkChangeEvent.CHANGE_CREATE));
      }
      return chunkDef;
   }
   
   // NOTE: this implements chunk removal locally; prefer firing a
   // ChunkChangeEvent if you're removing a chunk so appropriate hooks are
   // invoked elsewhere
   private void removeChunk(final String chunkId)
   {
      final ChunkOutputUi output = outputs_.get(chunkId);
      if (output == null)
         return;
      
      // remove any errors in the gutter associated with this chunk
      cleanScopeErrorState(output.getScope());

      ArrayList<Widget> widgets = new ArrayList<Widget>();
      widgets.add(output.getOutputWidget());
      FadeOutAnimation anim = new FadeOutAnimation(widgets, new Command()
      {
         @Override
         public void execute()
         {
            // physically remove chunk output
            output.remove();
            outputs_.remove(chunkId);

            // mark doc dirty (this is not undoable)
            dirtyState_.markDirty(false);
         }
      });
      anim.run(400);
   }
   
   private void removeAllChunks()
   {
      for (String chunkId: outputs_.keySet())
      {
         outputs_.get(chunkId).remove();
      }
      outputs_.clear();
   }
   
   private void changeOutputMode(String mode)
   {
      docDisplay_.setShowChunkOutputInline(mode == CHUNK_OUTPUT_INLINE);

      // manage commands
      manageCommands();
         
      // if we don't have any inline output, we're done
      if (outputs_.size() == 0 || mode != CHUNK_OUTPUT_CONSOLE)
         return;
      
      // if we do have inline output, offer to clean it up
      RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
            GlobalDisplay.MSG_QUESTION, 
            "Remove Inline Chunk Output", 
            "Do you want to clear all the existing chunk output from your " + 
            "notebook?", false, 
            new Operation()
            {
               @Override
               public void execute()
               {
                  removeAllChunks();
               }
            }, 
            new Operation()
            {
               @Override
               public void execute()
               {
                  // no action necessary
               }
            }, 
            null, 
            "Remove Output", 
            "Keep Output", 
            false);
   }
   
   // set the output mode based on the global pref (or our local 
   // override of it, if any)
   private void syncOutputMode()
   {
      String outputType = docUpdateSentinel_.getProperty(CHUNK_OUTPUT_TYPE);
      if (!StringUtil.isNullOrEmpty(outputType) && outputType != "undefined")
      {
         // if the document property is set, apply it directly
         docDisplay_.setShowChunkOutputInline(
               outputType == CHUNK_OUTPUT_INLINE);
      }
      else
      {
         // otherwise, use the global preference to set the value
         docDisplay_.setShowChunkOutputInline(
            docDisplay_.getModeId() == "mode/rmarkdown" &&
            RStudioGinjector.INSTANCE.getUIPrefs()
                                     .showRmdChunkOutputInline().getValue());
      }

      // watch for scope tree changes if showing output inline
      if (docDisplay_.showChunkOutputInline() && scopeTreeReg_ == null)
      {
         scopeTreeReg_ = docDisplay_.addScopeTreeReadyHandler(this);
      }
      else if (!docDisplay_.showChunkOutputInline() && scopeTreeReg_ != null)
      {
         scopeTreeReg_.removeHandler();
         scopeTreeReg_ = null;
      }
   }
   
   private void populateChunkDefs(JsArray<ChunkDefinition> defs)
   {
      for (int i = 0; i < defs.length(); i++)
      {
         createChunkOutput(defs.get(i));
      }
   }
   
   private void createChunkOutput(ChunkDefinition def)
   {
      outputs_.put(def.getChunkId(), 
             new ChunkOutputUi(docUpdateSentinel_.getId(), docDisplay_,
                               def, this));
   }
   
   private void ensureSetupChunkExecuted()
   {
      // ignore if disabled
      if (!prefs_.autoRunSetupChunk().getValue())
         return;

      // ignore if setup chunk currently running or already in the execution
      // queue
      if (executingChunk_ != null &&
          executingChunk_.chunkId == SETUP_CHUNK_ID)
      {
         return;
      }
      for (ChunkExecQueueUnit unit: chunkExecQueue_)
      {
         if (unit.chunkId == SETUP_CHUNK_ID)
         {
            return;
         }
      }
      
      // no reason to do work if we don't need to re-validate the setup chunk
      if (!validateSetupChunk_ && !StringUtil.isNullOrEmpty(setupCrc32_))
         return;
      validateSetupChunk_ = false;
      
      // find the setup chunk
      Scope setupScope = getSetupChunkScope();
      if (setupScope != null)
      {
         String crc32 = getChunkCrc32(setupScope);

         // compare with previously known hash; if it differs, re-run the
         // setup chunk
         if (crc32 != setupCrc32_)
         {
            // extract current options from setup chunk
            String options = 
               TextEditingTargetRMarkdownHelper.getRmdChunkOptionText(
                  setupScope, docDisplay_);
            
            // if there are no chunks in the queue yet, show progress 
            if (chunkExecQueue_.isEmpty())
               editingTarget_.getStatusBar().showNotebookProgress("Run Chunks");
            
            // push it into the execution queue
            executeChunk(setupScope, getChunkCode(setupScope), options, 
                         MODE_BATCH);
         }
      }
   }
   
   private String getChunkCode(Scope chunk)
   {
      return docDisplay_.getCode(
            chunk.getBodyStart(),
            Position.create(chunk.getEnd().getRow(), 0));
   }
   
   private String getChunkCrc32(Scope chunk)
   {
      // extract the body of the chunk
      String code = getChunkCode(chunk);
      
      // hash the body and prefix with the virtual session ID (so all
      // hashes are automatically invalidated when the session changes)
      return session_.getSessionInfo().getSessionId() +
            StringUtil.crc32(code);
   }
   
   private static boolean isSetupChunkScope(Scope scope)
   {
      if (!scope.isChunk())
         return false;
      if (scope.getChunkLabel() == null)
         return false;
      return scope.getChunkLabel().toLowerCase() == "setup";
   }
   
   private void writeSetupCrc32(String crc32)
   {
      setupCrc32_ = crc32;
      docUpdateSentinel_.setProperty(LAST_SETUP_CRC32, crc32);
   }
   
   private void cleanChunkExecState(String chunkId)
   {
      Scope chunk = getChunkScope(chunkId);
      if (chunk != null)
      {
         docDisplay_.setChunkLineExecState(
               chunk.getBodyStart().getRow(), 
               chunk.getEnd().getRow(), 
               ChunkRowExecState.LINE_RESTING);

         chunks_.setChunkState(chunk.getPreamble().getRow(), 
               ChunkContextToolbar.STATE_RESTING);
      }
   }
   
   private void syncWidth()
   {
      // check the width and see if it's already synced
      int width = docDisplay_.getPixelWidth();
      if (pixelWidth_ == width)
         return;
      
      // it's not synced, so compute the new width
      pixelWidth_ = width;
      charWidth_ = DomUtils.getCharacterWidth(pixelWidth_, pixelWidth_,
            ConsoleResources.INSTANCE.consoleStyles().console());
   }
   
   private Scope getChunkScope(String chunkId)
   {
      if (chunkId == SETUP_CHUNK_ID)
      {
         return getSetupChunkScope();
      }
      else if (outputs_.containsKey(chunkId))
      {
         return outputs_.get(chunkId).getScope();
      }
      return null;
   }
   
   private String getRowChunkId(int preambleRow)
   {
      // find the chunk corresponding to the row
      for (ChunkOutputUi output: outputs_.values())
      {
         if (output.getScope().getPreamble().getRow() == preambleRow)
            return output.getChunkId();
      }
      
      // no row mapped -- how about the setup chunk?
      Scope setupScope = getSetupChunkScope();
      if (setupScope != null &&
          setupScope.getPreamble().getRow() == preambleRow)
      {
         return SETUP_CHUNK_ID;
      }
      
      return null;
   }
   
   
   private Scope getSetupChunkScope()
   {
      ScopeList scopes = new ScopeList(docDisplay_);
      return scopes.findFirst(new ScopePredicate()
      {
         @Override
         public boolean test(Scope scope)
         {
            return isSetupChunkScope(scope);
         }
      });
   }
   
   private void setAllExpansionStates(int state)
   {
      for (ChunkOutputUi output: outputs_.values())
      {
         output.getOutputWidget().setExpansionState(state);
      }
   }
   
   private void clearChunkExecQueue()
   {
      for (ChunkExecQueueUnit unit: chunkExecQueue_)
      {
         cleanChunkExecState(unit.chunkId);
      }
      chunkExecQueue_.clear();

      editingTarget_.getStatusBar().hideNotebookProgress(true);
   }
   
   private Timer cleanErrorGutter_ = new Timer()
   {
      @Override
      public void run()
      {
         // get the doc's current scope and see if it matches any of our 
         // known chunk outputs
         Scope current = docDisplay_.getCurrentChunk();
         if (current == null)
            return;
         
         for (ChunkOutputUi output: outputs_.values())
         {
            if (output.getScope().getPreamble() == current.getPreamble())
            {
               cleanScopeErrorState(current);
               return;
            }
         }
      }
   };
   
   private int getPlotWidth()
   {
      // subtract some space to account for padding; ensure the plot doesn't
      // grow arbitrarily large. note that this value must total the amount of
      // space outside the element (here, 2 * (10px margin + 1px border)); since
      // we stretch the plot to fit the space it will scale in unpredictable
      // ways if it doesn't fit exactly
      return Math.min(docDisplay_.getPixelWidth() - 22,
                      ChunkOutputUi.MAX_PLOT_WIDTH);
   }
   
   private Timer resizePlotsLocal_ = new Timer()
   {
      @Override
      public void run()
      {
         for (ChunkOutputUi output: outputs_.values())
         {
            output.getOutputWidget().syncHeight(false, false);
         }
      }
   };
   
   private Timer resizePlotsRemote_ = new Timer()
   {
      @Override
      public void run()
      {
         // avoid reentrancy
         if (resizingPlotsRemote_)
            return;
         
         // avoid unnecessary work
         final int plotWidth = getPlotWidth();
         if (plotWidth == lastPlotWidth_)
            return;
         
         // we want to resize the visible plots first, so provide the server
         // with the id of the first visible chunk as a cue
         int row = docDisplay_.getFirstVisibleRow();
         Integer min = null;
         String chunkId = "";
         for (ChunkOutputUi output: outputs_.values())
         {
            int delta = Math.abs(output.getCurrentRow() - row);
            if (min == null || delta < min)
            {
               min = delta;
               chunkId = output.getChunkId();
            }
         }
         
         // make the request
         server_.replayNotebookPlots(docUpdateSentinel_.getId(), 
               chunkId,
               plotWidth,
               new ServerRequestCallback<Boolean>()
               {
                  @Override
                  public void onResponseReceived(Boolean started)
                  {
                     // server returns false in the case wherein there's already
                     // a resize RPC in process (could be from e.g. another 
                     // notebook in this session)
                     if (!started)
                        return;
                     
                     // don't replay a request for this width again
                     lastPlotWidth_ = plotWidth;
                     
                     // mark all plots as queued for resize
                     for (ChunkOutputUi output: outputs_.values())
                        output.getOutputWidget().setPlotPending(true);
                     resizingPlotsRemote_ = true;
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                  }
               });
      }
   };
   
   private void cleanScopeErrorState(Scope scope)
   {
      // this can be called on a timer, so ensure the scope is still valid
      if (scope == null ||
          scope.getBodyStart() == null ||
          scope.getEnd() == null)
         return;

      docDisplay_.setChunkLineExecState(
            scope.getBodyStart().getRow(), 
            scope.getEnd().getRow(), 
            ChunkRowExecState.LINE_NONE);
   }
   
   private void updateProgress()
   {
      // update progress meter on status bar
      editingTarget_.getStatusBar().updateNotebookProgress(
           executingChunk_ == null ? "" : executingChunk_.label,
           (int)Math.round(100 * ((double)(execQueueMaxSize_ - 
                                           chunkExecQueue_.size()) / 
                                  (double) execQueueMaxSize_)));
      
      // register click callback if necessary
      if (progressClickReg_ == null)
      {
         progressClickReg_ = editingTarget_.getStatusBar()
               .addProgressClickHandler(new ClickHandler()
               {
                  
                  @Override
                  public void onClick(ClickEvent arg0)
                  {
                     if (executingChunk_ != null &&
                         outputs_.containsKey(executingChunk_.chunkId))
                     {
                        outputs_.get(executingChunk_.chunkId).ensureVisible();
                     }
                  }
               });
         releaseOnDismiss_.add(progressClickReg_);
      }
      
      if (progressCancelReg_ == null)
      {
         progressCancelReg_ = editingTarget_.getStatusBar()
               .addProgressCancelHandler(new Command()
                {
                  @Override
                  public void execute()
                  {
                     // interrupt R if it's busy
                     if (commands_.interruptR().isEnabled())
                         commands_.interruptR().execute();
                     
                     // don't execute any more chunks
                     clearChunkExecQueue();
                  }
                });
         releaseOnDismiss_.add(progressCancelReg_);
      }
   }
   
   private final Timer syncHeightTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         // compute top/bottom of the doc (we'll sync widgets that lie in this
         // range)
         int top = docDisplay_.getFirstVisibleRow();
         int bot = docDisplay_.getLastVisibleRow();

         // sync any widgets that need it
         for (ChunkOutputUi output: outputs_.values())
         {
            ChunkOutputWidget widget = output.getOutputWidget();
            if (output.getCurrentRow() >= top &&
                output.getCurrentRow() <= bot && 
                widget.needsHeightSync())
            {
               output.getOutputWidget().syncHeight(false, false);
            }
         }
      }
   };
   
   private JsArray<ChunkDefinition> initialChunkDefs_;
   private HashMap<String, ChunkOutputUi> outputs_;
   private LinkedList<ChunkExecQueueUnit> chunkExecQueue_;
   private ChunkExecQueueUnit executingChunk_;
   private HandlerRegistration progressClickReg_;
   private HandlerRegistration scopeTreeReg_;
   private HandlerRegistration progressCancelReg_;
   private Position lastStart_;
   private Position lastEnd_;
   private AppCommand postRestartCommand_;
   
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel docUpdateSentinel_;
   private final TextEditingTarget editingTarget_;
   private final TextEditingTarget.Display editingDisplay_;
   private final TextEditingTargetChunks chunks_;
   private final DirtyState dirtyState_;
   
   ArrayList<HandlerRegistration> releaseOnDismiss_;
   private Session session_;
   private Provider<SourceWindowManager> pSourceWindowManager_;
   private DependencyManager dependencyManager_;
   private UIPrefs prefs_;
   private Commands commands_;

   private RMarkdownServerOperations server_;
   private ConsoleServerOperations console_;
   private SourceServerOperations source_;
   private EventBus events_;
   
   private Style editorStyle_;

   private static int nextRequestId_ = 0;
   private int requestId_ = 0;
   private String contextId_ = "";
   private ResizeEvent queuedResize_ = null;
   private boolean validateSetupChunk_ = false;
   private String setupCrc32_ = "";
   private int charWidth_ = 65;
   private int pixelWidth_ = 0;
   private int lastPlotWidth_ = 0;
   private boolean evaluatingParams_ = false;
   private int execQueueMaxSize_ = 0;
   private boolean resizingPlotsRemote_ = false;
   private boolean maximizedPane_ = false;
   
   private int state_ = STATE_NONE;

   // no chunk state
   private final static int STATE_NONE = 0;
   
   // synchronizing chunk state from server
   private final static int STATE_INITIALIZING = 0;
   
   // chunk state synchronized
   private final static int STATE_INITIALIZED = 1;
   
   private final static String LAST_SETUP_CRC32 = "last_setup_crc32";
   private final static String SETUP_CHUNK_ID = "csetup_chunk";
   
   // stored document properties/values
   public final static String CHUNK_OUTPUT_TYPE    = "chunk_output_type";
   public final static String CHUNK_OUTPUT_INLINE  = "inline";
   public final static String CHUNK_OUTPUT_CONSOLE = "console";
   public final static String CHUNK_RENDERED_WIDTH = "chunk_rendered_width";
   
   public final static int MODE_SINGLE = 0;
   public final static int MODE_BATCH  = 1;
   
   public final static int SCOPE_CHUNK   = 0;
   public final static int SCOPE_PARTIAL = 1;
}
