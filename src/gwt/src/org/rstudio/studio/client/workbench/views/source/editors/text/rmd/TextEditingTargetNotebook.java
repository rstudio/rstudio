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
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.SendToChunkConsoleEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookDoc;
import org.rstudio.studio.client.rmarkdown.model.NotebookDocQueue;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutput;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutputUnit;
import org.rstudio.studio.client.rmarkdown.model.RmdEditorOptions;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
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
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetScopeHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCacheEditorStyleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCloseAllWindowEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCodeExecutingEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteWindowOpenedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.events.InterruptChunkEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkContextChangeEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileHandler;
import org.rstudio.studio.client.workbench.views.source.events.SourceDocAddedEvent;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
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
                          ResizeHandler,
                          InterruptStatusEvent.Handler,
                          DeferredInitCompletedEvent.Handler,
                          ScopeTreeReadyEvent.Handler,
                          PinnedLineWidget.Host,
                          SourceDocAddedEvent.Handler,
                          RenderFinishedEvent.Handler,
                          ChunkSatelliteWindowOpenedEvent.Handler
{
   public TextEditingTargetNotebook(final TextEditingTarget editingTarget,
                                    TextEditingTargetChunks chunks,
                                    TextEditingTarget.Display editingDisplay,
                                    DocDisplay docDisplay,
                                    DirtyState dirtyState,
                                    DocUpdateSentinel docUpdateSentinel,
                                    SourceDocument document,
                                    ArrayList<HandlerRegistration> releaseOnDismiss,
                                    DependencyManager dependencyManager)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;  
      dirtyState_ = dirtyState;
      releaseOnDismiss_ = releaseOnDismiss;
      notebookDoc_ = document.getNotebookDoc();
      initialChunkDefs_ = JsArrayUtil.deepCopy(notebookDoc_.getChunkDefs());
      outputs_ = new HashMap<String, ChunkOutputUi>();
      satelliteChunkRequestIds_ = new ArrayList<String>();
      setupCrc32_ = docUpdateSentinel_.getProperty(LAST_SETUP_CRC32);
      editingTarget_ = editingTarget;
      chunks_ = chunks;
      editingDisplay_ = editingDisplay;
      scopeHelper_ = new TextEditingTargetScopeHelper(docDisplay_);
      dependencyManager_ = dependencyManager;
      RStudioGinjector.INSTANCE.injectMembers(this);
      
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
            // propagate to YAML
            String yaml = RmdEditorOptions.set(
                  YamlFrontMatter.getFrontMatter(docDisplay_), 
                  CHUNK_OUTPUT_TYPE, event.getValue());
            YamlFrontMatter.applyFrontMatter(docDisplay_, yaml);
            
            // change the output mode in the document
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
      renderReg_ = docDisplay_.addRenderFinishedHandler(this);
      
      releaseOnDismiss_.add(editingTarget_.addInterruptChunkHandler(new InterruptChunkEvent.Handler()
      {
         @Override
         public void onInterruptChunk(InterruptChunkEvent event)
         {
            int row = event.getRow();
            String chunkId = getRowChunkId(row);
            
            // just interrupt R if we have no chunk id for some reason (shouldn't happen)
            if (StringUtil.isNullOrEmpty(chunkId))
            {
               RStudioGinjector.INSTANCE.getApplicationInterrupt().interruptR(null);
               return;
            }
            
            // interrupt this chunk's execution
            server_.interruptChunk(
                  editingTarget_.getId(),
                  chunkId,
                  new ServerRequestCallback<Void>()
                  {
                     @Override
                     public void onResponseReceived(Void response)
                     {
                        RStudioGinjector.INSTANCE.getApplicationInterrupt().interruptR(null);
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                        RStudioGinjector.INSTANCE.getApplicationInterrupt().interruptR(null);
                     }
                  });
         }
      }));
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
      source_ = source;
      session_ = session;
      prefs_ = prefs;
      commands_ = commands;
      pSourceWindowManager_ = pSourceWindowManager;
      queue_ = new NotebookQueueState(docDisplay_, editingTarget_, 
            docUpdateSentinel_, server, events, this);
      
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
            events_.addHandler(InterruptStatusEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(DeferredInitCompletedEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(SourceDocAddedEvent.TYPE, this));
      releaseOnDismiss_.add(
            events_.addHandler(ChunkSatelliteWindowOpenedEvent.TYPE, this));
      
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

      // set up HTML rendering on save
      htmlRenderer_ = new NotebookHtmlRenderer(docDisplay_, 
            editingTarget_, editingDisplay_, docUpdateSentinel_, 
            server_, events_, dependencyManager);
      releaseOnDismiss_.add(docDisplay_.addSaveCompletedHandler(htmlRenderer_));
      
      // set up preference sync on save
      releaseOnDismiss_.add(docDisplay_.addSaveCompletedHandler(
            new SaveFileHandler()
      {
         @Override
         public void onSaveFile(SaveFileEvent event)
         {
            // propagate output preference from YAML into doc preference
            String frontMatter = YamlFrontMatter.getFrontMatter(docDisplay_);
            if (!StringUtil.isNullOrEmpty(frontMatter))
            {
               // if the YAML mode was manually changed to be different than
               // the doc mode, set the doc mode appropriately
               String yamlMode = RmdEditorOptions.getString(frontMatter,
                     CHUNK_OUTPUT_TYPE, null);
               if (!StringUtil.isNullOrEmpty(yamlMode))
               {
                  String docMode = docUpdateSentinel_.getProperty(
                        CHUNK_OUTPUT_TYPE, yamlMode);
                  if (yamlMode != docMode)
                     docUpdateSentinel_.setProperty(CHUNK_OUTPUT_TYPE, 
                           yamlMode);
               }
            }
         }
      }));
   }
   
   public void onActivate()
   {
      // remember that we haven't maximized the pane in this session
      maximizedPane_ = false;
      
      // listen for clicks on notebook progress UI
      registerProgressHandlers();

      // propagate output preference from YAML into doc preference
      String frontMatter = YamlFrontMatter.getFrontMatter(docDisplay_);
      if (!StringUtil.isNullOrEmpty(frontMatter))
      {
         String mode = RmdEditorOptions.getString(frontMatter,
               CHUNK_OUTPUT_TYPE, null);
         if (!StringUtil.isNullOrEmpty(mode))
         {
            docUpdateSentinel_.setProperty(CHUNK_OUTPUT_TYPE, mode);
         }
      }
   }
   
   public void executeChunks(final String jobDesc, 
                             final List<ChunkExecUnit> chunks)
   {
      if (queue_.isExecuting())
      {
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               jobDesc + ": Chunks Currently Executing", 
               "RStudio cannot execute '" + jobDesc + "' because this " +
               "notebook is already executing code. Interrupt R, or wait " +
               "for execution to complete.");
         return;
      }
      docUpdateSentinel_.withSavedDoc(new Command()
      {
         @Override
         public void execute()
         {
            queue_.executeChunks(jobDesc, chunks);
         }
      });
   }
   
   public void executeChunk(final Scope chunk)
   {
      // maximize the source pane if we haven't yet this session
      if (!maximizedPane_ && 
          prefs_.hideConsoleOnChunkExecute().getValue())
      {
         pSourceWindowManager_.get().maximizeSourcePaneIfNecessary();
         maximizedPane_ = true;
      }
      
      docUpdateSentinel_.withSavedDoc(new Command() 
      {
         @Override
         public void execute()
         {
            // if this isn't the setup chunk, ensure the setup chunk is executed
            // by creating a job that runs both chunks
            if (!isSetupChunkScope(chunk) &&
                needsSetupChunkExecuted())
            {
               List<ChunkExecUnit> chunks = new ArrayList<ChunkExecUnit>();
               chunks.add(new ChunkExecUnit(getSetupChunkScope(), 
                     NotebookQueueUnit.EXEC_MODE_BATCH));
               chunks.add(new ChunkExecUnit(chunk,
                     NotebookQueueUnit.EXEC_MODE_SINGLE));
               queue_.executeChunks("Run Chunks", chunks);
            }
            else
            {
               queue_.executeChunk(new ChunkExecUnit(chunk,
                     NotebookQueueUnit.EXEC_MODE_SINGLE));
            }
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
      commands_.notebookClearOutput().setEnabled(inlineOutput); 
      commands_.notebookClearOutput().setVisible(inlineOutput); 
      commands_.notebookClearAllOutput().setEnabled(inlineOutput); 
      commands_.notebookClearAllOutput().setVisible(inlineOutput); 
      commands_.notebookToggleExpansion().setEnabled(inlineOutput); 
      commands_.notebookToggleExpansion().setVisible(inlineOutput); 
      editingDisplay_.setNotebookUIVisible(inlineOutput);
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
   
   public void onNotebookClearOutput()
   {
      // find the current chunk 
      Scope chunk = docDisplay_.getCurrentChunk();
      clearChunkOutput(chunk);
   }
   
   public void onNotebookToggleExpansion()
   {
      String chunkId = getCurrentChunkId();
      if (chunkId == null || !outputs_.containsKey(chunkId))
         return;
     ChunkOutputWidget widget = outputs_.get(chunkId).getOutputWidget();
     widget.setExpansionState(
           widget.getExpansionState() == ChunkOutputWidget.COLLAPSED ? 
                 ChunkOutputWidget.EXPANDED : ChunkOutputWidget.COLLAPSED);
   }

   public void clearChunkOutput(Scope chunk)
   {
      if (chunk == null)
      {
         // no-op if cursor is not inside a chunk
         return;
      }
      
      String chunkId = getRowChunkId(chunk.getPreamble().getRow());
      if (chunkId == null)
      {
         // no-op if we don't have known output 
         return;
      }

      events_.fireEvent(new ChunkChangeEvent(docUpdateSentinel_.getId(), 
            chunkId, "", 0, ChunkChangeEvent.CHANGE_REMOVE));
   }
   
   public void onNotebookClearAllOutput()
   {
      if (!queue_.isExecuting())
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
   
   public String getChunkCode(Scope chunk)
   {
      return docDisplay_.getCode(
            chunk.getBodyStart(),
            Position.create(chunk.getEnd().getRow(), 0));
   }
   
   public int getCommitMode()
   {
      if (editingTarget_.isRmdNotebook())
      {
         // notebooks always play chunks as uncommitted
         return MODE_UNCOMMITTED;
      }
      else if (dirtyState_.getValue())
      {
         // uncommitted R Markdown files also play chunks as uncommitted
         return MODE_UNCOMMITTED;
      }
      // everything else plays directly to committed
      return MODE_COMMITTED;
   }
   
   // Event handlers ----------------------------------------------------------
   
   @Override
   public void onEditorThemeStyleChanged(EditorThemeStyleChangedEvent event)
   {
      // update cached style 
      editorStyle_ = event.getStyle();
      ChunkOutputWidget.cacheEditorStyle(
         editorStyle_.getColor(),
         editorStyle_.getBackgroundColor(),
         DomUtils.extractCssValue("ace_editor", "color")
      );
      
      for (ChunkOutputUi output: outputs_.values())
      {
         output.getOutputWidget().applyCachedEditorStyle();
      }

      events_.fireEvent(
         new ChunkSatelliteCacheEditorStyleEvent(
            docUpdateSentinel_.getId(),
            editorStyle_.getColor(),
            editorStyle_.getBackgroundColor(),
            DomUtils.extractCssValue("ace_editor", "color")
         )
      );
      
      // update if currently executing
      if (queue_.isExecuting())
      {
         NotebookQueueUnit unit = queue_.executingUnit();
         if (unit != null)
         {
            setChunkExecuting(unit.getChunkId(), unit.getExecMode(),
                  unit.getExecScope());
         }
      }
   }
   
   @Override
   public void onSendToChunkConsole(final SendToChunkConsoleEvent event)
   {
      // not for our doc
      if (event.getDocId() != docUpdateSentinel_.getId())
         return;
      
      // execute setup chunk first if necessary
      if (needsSetupChunkExecuted() && !isSetupChunkScope(event.getScope()))
      {
         List<ChunkExecUnit> chunks = new ArrayList<ChunkExecUnit>();
         chunks.add(new ChunkExecUnit(getSetupChunkScope(), 
               NotebookQueueUnit.EXEC_MODE_BATCH));
         chunks.add(new ChunkExecUnit(event.getScope(), event.getRange(), 
               NotebookQueueUnit.EXEC_MODE_SINGLE, event.getExecScope()));
         queue_.executeChunks("Run Chunks", chunks);
      }
      else
      {
         queue_.executeChunk(
               new ChunkExecUnit(event.getScope(), event.getRange(), 
                     NotebookQueueUnit.EXEC_MODE_SINGLE, event.getExecScope())); 
      }
   }
   
   @Override
   public void onRmdChunkOutput(RmdChunkOutputEvent event)
   {
      // ignore if not targeted at this document
      if (event.getOutput().getDocId() != docUpdateSentinel_.getId())
         return;
      
      // if nothing at all was returned, this means the chunk doesn't exist on
      // the server, so clean it up here.
      if (event.getOutput().isEmpty() && !queue_.isExecuting())
      {
         events_.fireEvent(new ChunkChangeEvent(
               docUpdateSentinel_.getId(), event.getOutput().getChunkId(), 
               event.getOutput().getRequestId(), 0, 
               ChunkChangeEvent.CHANGE_REMOVE));
         return;
      }

      String chunkId = event.getOutput().getChunkId();
      
      // ignore requests performed to initialize satellite chunks
      if (satelliteChunkRequestIds_.contains(event.getOutput().getRequestId()))
         return;
      
      // if this is the currently executing chunk and it has an error...
      NotebookQueueUnit unit = queue_.executingUnit();
      if (unit != null &&
          unit.getChunkId() == event.getOutput().getChunkId() &&
          event.getOutput().getType() == RmdChunkOutput.TYPE_SINGLE_UNIT &&
          event.getOutput().getUnit().getType() == 
             RmdChunkOutputUnit.TYPE_ERROR)
      {
         // draw the error 
         Scope scope = getChunkScope(unit.getChunkId());
         if (scope != null)
         {
            int offset = scope.getBodyStart().getRow();
            List<Integer> lines = unit.getExecutingLines();
            for (Integer line: lines)
               docDisplay_.setChunkLineExecState(line + offset, line + offset, 
                     ChunkRowExecState.LINE_ERROR);
         }

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
         int mode = queue_.getChunkExecMode(chunkId);
         
         // no need to make chunks visible in batch mode
         if (ensureVisible && mode == NotebookQueueUnit.EXEC_MODE_BATCH)
            ensureVisible = false;
         
         outputs_.get(chunkId).getOutputWidget()
                              .showChunkOutput(event.getOutput(), mode,
                                  NotebookQueueUnit.EXEC_SCOPE_PARTIAL,
                                  !queue_.isChunkExecuting(chunkId),
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
      
      RmdChunkOutputFinishedEvent.Data data = event.getData();
      
      // ignore requests performed to initialize satellite chunks
      if (satelliteChunkRequestIds_.contains(event.getData().getRequestId()))
         return;
      
      // clean up execution state
      cleanChunkExecState(event.getData().getChunkId());
      
      ensureVisible = queue_.getChunkExecMode(data.getChunkId()) == 
            NotebookQueueUnit.EXEC_MODE_SINGLE;

      if (outputs_.containsKey(data.getChunkId()))
      {
         ChunkOutputUi output = outputs_.get(data.getChunkId());
         if (isSetupChunkScope(output.getScope()))
         {
            writeSetupCrc32(getChunkCrc32(output.getScope()));
            if (output.hasErrors())
            {
               ensureVisible = true;
               validateSetupChunk_ = true;
            }
            else
            {
               writeSetupCrc32(getChunkCrc32(output.getScope()));
            }
         }
      }

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

            // set dirty state if necessary
            setDirtyState();
         }
      }
   }

   @Override
   public void onChunkPlotRefreshFinished(ChunkPlotRefreshFinishedEvent event)
   {
      // ignore replays that are not targeting this instance
      if (currentPlotsReplayId_ != event.getData().getReplayId())
         return;

      currentPlotsReplayId_ = null;

      // ignore if targeted at another document
      if (event.getData().getDocId() != docUpdateSentinel_.getId())
         return;

      lastPlotWidth_ = event.getData().getWidth();

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
      // ignore replays that are not targeting this instance
      if (currentPlotsReplayId_ != event.getData().getReplayId())
         return;

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
                  event.getDocId(), event.getChunkId(), 
                  getKnitrChunkLabel(event.getRow(), docDisplay_, 
                        new ScopeList(docDisplay_))));
            break;
         case ChunkChangeEvent.CHANGE_REMOVE:
            removeChunk(event.getChunkId(), event.getRequestId());
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

      for (ChunkOutputUi output: outputs_.values())
      {
         // throwing exceptions during resize breaks most of the UI and this
         // invokes Javascript from a package downstream, so tolerate 
         // (and log) exceptions
         try
         {
            output.getOutputWidget().onResize();
         }
         catch(Exception e)
         {
            Debug.logException(e);
         }
      }
   }

   @Override
   public void onChunkSatelliteWindowOpened(ChunkSatelliteWindowOpenedEvent event)
   {
      String docId = event.getDocId();
      String chunkId = event.getChunkId();

      if (docId != docUpdateSentinel_.getId())
         return;

      events_.fireEvent(
         new ChunkSatelliteCacheEditorStyleEvent(
            docId,
            editorStyle_.getColor(),
            editorStyle_.getBackgroundColor(),
            DomUtils.extractCssValue("ace_editor", "color")
         )
      );

      refreshSatelliteChunk(chunkId);
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
         // if we got chunk content, load initial chunk output from server --
         // note that some outputs need the rmarkdown package to render, so 
         // update that silently if needed
         if (initialChunkDefs_.length() > 0)
         {
            dependencyManager_.withRMarkdown("R Notebook",
               null, new CommandWithArg<Boolean>()
               {
                  @Override
                  public void execute(Boolean arg)
                  {
                     loadInitialChunkOutput();
                  }
               });
         }

         initialChunkDefs_ = null;
         
         // sync to editor style changes
         editingTarget_.addEditorThemeStyleChangedHandler(
                                       TextEditingTargetNotebook.this);
         
         // read and/or set initial render width
         lastPlotWidth_ = notebookDoc_.getChunkRenderedWidth();
         if (lastPlotWidth_ == 0)
         {
            lastPlotWidth_ = getPlotWidth();
         }
      }
      
      // remove render handler
      renderReg_.removeHandler();
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
   public void onDeferredInitCompleted(DeferredInitCompletedEvent event)
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
   
   @Override
   public void onLineWidgetAdded(LineWidget widget)
   {
      // no action necessary; this just lets us know that a chunk output has
      // been attached to the DOM
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
            // save scope and widget
            int terminalLine = widget.getRow() - 1;
            Scope scope = docDisplay_.getCurrentChunk(Position.create(
                  terminalLine, 1));
            ChunkOutputWidget outputWidget = output.getOutputWidget();

            // clean up old widget
            output.remove();
            outputs_.remove(output.getChunkId());

            // if the scope is still at the terminal line, then this widget was
            // deleted over-aggressively by Ace (this can happen when e.g. 
            // removing the final line of a chunk). create a new linewidget in
            // place of the old one (but re-use the output widget so the output
            // is identical)
            if (scope != null &&
                scope.getEnd().getRow() == terminalLine)
            {
               ChunkDefinition def = (ChunkDefinition)widget.getData();
               def.setRow(terminalLine);
               widget.setRow(terminalLine);
               ChunkOutputUi newOutput = new ChunkOutputUi(
                     docUpdateSentinel_.getId(), 
                     docDisplay_, def, this, outputWidget);
               outputs_.put(output.getChunkId(), newOutput);
               return;
            }

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
                  docUpdateSentinel_.getId(), output.getChunkId(), "", 0, 
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

   public String getRowChunkId(int preambleRow)
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
   
   public Scope getChunkScope(String chunkId)
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
   
   public Scope getSetupChunkScope()
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
   
   public void setChunkExecuting(String chunkId, int mode, int execScope)
   {
      // let the chunk widget know it's started executing
      if (outputs_.containsKey(chunkId))
      {
         ChunkOutputUi output = outputs_.get(chunkId);

         // expand the chunk if it's in a fold
         Scope scope = output.getScope();
         if (scope != null)
         {
            docDisplay_.unfold(Range.fromPoints(scope.getPreamble(),
                                                scope.getEnd()));
         }

         events_.fireEvent(
            new ChunkSatelliteCodeExecutingEvent(
                  docUpdateSentinel_.getId(),
                  chunkId,
                  mode,
                  NotebookQueueUnit.EXEC_SCOPE_PARTIAL
               )
         );

         output.getOutputWidget().setCodeExecuting(mode, execScope);
         
         // scroll the widget into view if it's a single-shot exec
         if (mode == NotebookQueueUnit.EXEC_MODE_SINGLE)
            output.ensureVisible();
      }

      // draw UI on chunk
      Scope chunk = getChunkScope(chunkId);
      if (chunk != null)
      {
         setChunkState(chunk, ChunkContextToolbar.STATE_EXECUTING);
      }
   }

   public void cleanChunkExecState(String chunkId)
   {
      Scope chunk = getChunkScope(chunkId);
      if (chunk != null)
      {
         docDisplay_.setChunkLineExecState(
               chunk.getBodyStart().getRow(), 
               chunk.getEnd().getRow(), 
               ChunkRowExecState.LINE_RESTING);

         setChunkState(chunk, ChunkContextToolbar.STATE_RESTING);
      }
   }
   
   public void setChunkState(Scope chunk, int state)
   {
      chunks_.setChunkState(chunk.getPreamble().getRow(), state);
   }
   
   public static boolean isSetupChunkScope(Scope scope)
   {
      if (!scope.isChunk())
         return false;
      if (scope.getChunkLabel() == null)
         return false;
      return scope.getChunkLabel().toLowerCase() == "setup";
   }
   
   public void dequeueChunk(int preambleRow)
   {
      queue_.dequeueChunk(preambleRow);
   }
   
   public void setOutputOptions(String chunkId, RmdChunkOptions options)
   {
      if (outputs_.containsKey(chunkId))
      {
         outputs_.get(chunkId).setOptions(options);
      }
   }
   
   public int getPlotWidth()
   {
      // subtract some space to account for padding; ensure the plot doesn't
      // grow arbitrarily large. note that this value must total the amount of
      // space outside the element (here, 2 * (10px margin + 1px border)); since
      // we stretch the plot to fit the space it will scale in unpredictable
      // ways if it doesn't fit exactly
      return Math.min(Math.max(docDisplay_.getPixelWidth() - 22, 
                               ChunkOutputUi.MIN_PLOT_WIDTH),
                      ChunkOutputUi.MAX_PLOT_WIDTH);
   }
   
   public void cleanScopeErrorState(Scope scope)
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

   public void onDismiss()
   {
      closeAllSatelliteChunks();
   }
   
   // set the output mode based on the global pref (or our local 
   // override of it, if any)
   public void syncOutputMode()
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
   
   // Private methods --------------------------------------------------------

   private void closeAllSatelliteChunks()
   {
      String docId = docUpdateSentinel_.getId();
      events_.fireEvent(new ChunkSatelliteCloseAllWindowEvent(docId));
   }
   
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
      String chunkId = queue_.getExecutingChunkId();
      if (chunkId == null)
         return;
      
      if (outputs_.containsKey(chunkId))
      {
         outputs_.get(chunkId)
                 .getOutputWidget().onOutputFinished(false, 
                       NotebookQueueUnit.EXEC_SCOPE_PARTIAL);
      }
      cleanChunkExecState(chunkId);
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
            "",
            new ServerRequestCallback<NotebookDocQueue>()
            {
               @Override
               public void onResponseReceived(NotebookDocQueue queue)
               {
                  if (queue != null)
                     queue_.setQueue(queue);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   // look for a line widget associated with the given chunk ID (used to find
   // orphans)
   private LineWidget getLineWidget(String chunkId)
   {
      JsArray<LineWidget> lineWidgets = docDisplay_.getLineWidgets();
      for (int i = 0; i < lineWidgets.length(); i++)
      {
         LineWidget w = lineWidgets.get(i);
         if (w.getType() == ChunkDefinition.LINE_WIDGET_TYPE)
         {
            ChunkDefinition def = w.getData();
            if (def.getChunkId() == chunkId)
               return w;
         }
      }
      return null;
   }
   
   // NOTE: this implements chunk removal locally; prefer firing a
   // ChunkChangeEvent if you're removing a chunk so appropriate hooks are
   // invoked elsewhere
   private void removeChunk(final String chunkId, final String requestId)
   {
      // ignore if this chunk is currently executing
      if (queue_.isChunkExecuting(chunkId))
         return;
      
      final ChunkOutputUi output = outputs_.get(chunkId);
      if (output == null)
      {
         // this case is unexpected; it means that a chunk we don't know about
         // was removed. look for an orphaned line widget matching the chunk ID
         // in case our output map is out of sync.
         LineWidget w = getLineWidget(chunkId);
         if (w != null)
         {
            docDisplay_.removeLineWidget(w);
            if (w.getElement() != null)
            {
               w.getElement().getStyle().setDisplay(Display.NONE);
               w.getElement().removeFromParent();
            }
         }
         return;
      }
      
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

            // mark doc dirty if interactive (this is not undoable)
            if (StringUtil.isNullOrEmpty(requestId))
               setDirtyState();
         }
      });
      anim.run(400);
   }
   
   private void removeAllChunks()
   {
      for (ChunkOutputUi output: outputs_.values())
      {
         // clean any error state still attached to the output's scope
         cleanScopeErrorState(output.getScope());
         output.remove();
      }

      closeAllSatelliteChunks();
      
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
   
   private boolean needsSetupChunkExecuted()
   {
      // ignore if disabled
      if (!prefs_.autoRunSetupChunk().getValue())
         return false;

      // ignore if setup chunk currently running or already in the execution
      // queue
      if (queue_.isChunkExecuting(SETUP_CHUNK_ID) ||
          queue_.isChunkQueued(SETUP_CHUNK_ID))
      {
         return false;
      }
      
      // no reason to do work if we don't need to re-validate the setup chunk
      if (!validateSetupChunk_ && !StringUtil.isNullOrEmpty(setupCrc32_))
         return false;
      validateSetupChunk_ = false;
      
      // find the setup chunk
      Scope setupScope = getSetupChunkScope();
      if (setupScope != null)
      {
         // make sure there's some code to execute
         Range range = scopeHelper_.getSweaveChunkInnerRange(setupScope);
         if (range.getStart().isEqualTo(range.getEnd()))
            return false;

         String crc32 = getChunkCrc32(setupScope);

         // compare with previously known hash; if it differs, re-run the
         // setup chunk
         if (crc32 != setupCrc32_)
         {
            // push it into the execution queue
            return true;
         }
      }
      
      return false;
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
   
   private void writeSetupCrc32(String crc32)
   {
      setupCrc32_ = crc32;
      docUpdateSentinel_.setProperty(LAST_SETUP_CRC32, crc32);
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
      if (queue_ != null)
         queue_.clear();
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
            if (output.getScope() == null)
               continue;
            
            if (output.getScope().getPreamble() == current.getPreamble())
            {
               cleanScopeErrorState(current);
               return;
            }
         }
      }
   };
   
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
         boolean hasPlots = false;
         for (ChunkOutputUi output: outputs_.values())
         {
            int delta = Math.abs(output.getCurrentRow() - row);
            if (min == null || delta < min)
            {
               min = delta;
               chunkId = output.getChunkId();
            }
            hasPlots = hasPlots || output.getOutputWidget().hasPlots();
         }
         
         // if no widgets have plots, don't bother with resize
         if (!hasPlots)
            return;
         
         // make the request
         server_.replayNotebookPlots(docUpdateSentinel_.getId(), 
               chunkId,
               plotWidth,
               0,
               new ServerRequestCallback<String>()
               {
                  @Override
                  public void onResponseReceived(String replayId)
                  {
                     // server returns empty in the case wherein there's already
                     // a resize RPC in process (could be from e.g. another 
                     // notebook in this session)
                     if (replayId == null || replayId.isEmpty())
                        return;

                     currentPlotsReplayId_ = replayId;
                     
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
                     currentPlotsReplayId_ = null;

                     Debug.logError(error);
                  }
               });
      }
   };
   
   private void registerProgressHandlers()
   {
      // register click callback if necessary
      if (progressClickReg_ == null)
      {
         progressClickReg_ = editingTarget_.getStatusBar()
               .addProgressClickHandler(new ClickHandler()
               {
                  
                  @Override
                  public void onClick(ClickEvent arg0)
                  {
                     String chunkId = queue_.getExecutingChunkId();
                     if (chunkId != null &&
                         outputs_.containsKey(chunkId))
                     {
                        outputs_.get(chunkId).ensureVisible();
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
   
   private void setDirtyState()
   {
      if (getCommitMode() == MODE_UNCOMMITTED && !dirtyState_.getValue())
      {
         // mark the document dirty (if it isn't already) since it now
         // contains notebook cache changes that haven't been committed 
         dirtyState_.markDirty(false);
         source_.setSourceDocumentDirty(
               docUpdateSentinel_.getId(), true, 
               new VoidServerRequestCallback());
      }
   }

   private void refreshSatelliteChunk(String chunkId)
   {
      requestId_ = nextRequestId_++;
      satelliteChunkRequestIds_.add(Integer.toHexString(requestId_));

      server_.refreshChunkOutput(
         docUpdateSentinel_.getPath(),
         docUpdateSentinel_.getId(), 
         contextId_,
         Integer.toHexString(requestId_), 
         chunkId,
         new ServerRequestCallback<NotebookDocQueue>()
         {
            @Override
            public void onResponseReceived(NotebookDocQueue queue)
            {
               if (queue != null)
                  queue_.setQueue(queue);
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
   }
   
   private String getCurrentChunkId()
   {
      Scope chunk = docDisplay_.getCurrentChunk();
      if (chunk == null)
         return null;
      return getRowChunkId(chunk.getPreamble().getRow());
   }
   
   private JsArray<ChunkDefinition> initialChunkDefs_;
   private HashMap<String, ChunkOutputUi> outputs_;
   private ArrayList<String> satelliteChunkRequestIds_;
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
   private final NotebookDoc notebookDoc_;
   private final TextEditingTargetScopeHelper scopeHelper_;
   private final DependencyManager dependencyManager_;
   private final HandlerRegistration renderReg_;
   
   ArrayList<HandlerRegistration> releaseOnDismiss_;
   private Session session_;
   private Provider<SourceWindowManager> pSourceWindowManager_;
   private UIPrefs prefs_;
   private Commands commands_;
   private NotebookHtmlRenderer htmlRenderer_;

   private RMarkdownServerOperations server_;
   private SourceServerOperations source_;
   private EventBus events_;
   private NotebookQueueState queue_;
   
   private Style editorStyle_;

   private static int nextRequestId_ = 0;
   private int requestId_ = 0;
   private String contextId_ = "";
   private ResizeEvent queuedResize_ = null;
   private boolean validateSetupChunk_ = false;
   private String setupCrc32_ = "";
   private int lastPlotWidth_ = 0;
   private boolean resizingPlotsRemote_ = false;
   private boolean maximizedPane_ = false;
   
   private int state_ = STATE_NONE;

   private String currentPlotsReplayId_ = null;
   
   // no chunk state
   private final static int STATE_NONE = 0;
   
   // synchronizing chunk state from server
   private final static int STATE_INITIALIZING = 0;
   
   // chunk state synchronized
   private final static int STATE_INITIALIZED = 1;
   
   private final static String LAST_SETUP_CRC32 = "last_setup_crc32";
   public final static String SETUP_CHUNK_ID = "csetup_chunk";
   
   // stored document properties/values
   public final static String CHUNK_OUTPUT_TYPE    = "chunk_output_type";
   public final static String CHUNK_OUTPUT_INLINE  = "inline";
   public final static String CHUNK_OUTPUT_CONSOLE = "console";

   public final static String CONTENT_PREVIEW_ENABLED  = 
         "content_preview_enabled";
   public final static String CONTENT_PREVIEW_INLINE   = 
         "content_preview_inline";
   
   public final static int MODE_COMMITTED   = 0;
   public final static int MODE_UNCOMMITTED = 1;
}
