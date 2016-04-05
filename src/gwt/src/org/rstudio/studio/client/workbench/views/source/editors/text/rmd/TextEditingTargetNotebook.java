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
import java.util.Queue;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.SendToChunkConsoleEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputHandler;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetChunks;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkContextChangeEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class TextEditingTargetNotebook 
               implements EditorThemeStyleChangedEvent.Handler,
                          RmdChunkOutputEvent.Handler,
                          RmdChunkOutputFinishedEvent.Handler,
                          SendToChunkConsoleEvent.Handler, 
                          ChunkChangeEvent.Handler,
                          ChunkContextChangeEvent.Handler,
                          ConsolePromptHandler,
                          ConsoleWriteInputHandler,
                          ResizeHandler,
                          InterruptStatusEvent.Handler,
                          RestartStatusEvent.Handler,
                          PinnedLineWidget.Host
{
   public interface Binder
   extends CommandBinder<Commands, TextEditingTargetNotebook> {}

   private class ChunkExecQueueUnit
   {
      public ChunkExecQueueUnit(String chunkIdIn, String codeIn, 
            String optionsIn, String setupCrc32In)
      {
         chunkId = chunkIdIn;
         options = optionsIn;
         code = codeIn;
         setupCrc32 = setupCrc32In;
         pos = 0;
         linesExecuted = 0;
      }
      public String chunkId;
      public String options;
      public String code;
      public String setupCrc32;
      public int pos;
      public int linesExecuted;
   };

   public TextEditingTargetNotebook(final TextEditingTarget editingTarget,
                                    TextEditingTargetChunks chunks,
                                    TextEditingTarget.Display editingDisplay,
                                    DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel,
                                    SourceDocument document,
                                    ArrayList<HandlerRegistration> releaseOnDismiss)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;  
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
      
      releaseOnDismiss.add(docDisplay_.addValueChangeHandler(
            new ValueChangeHandler<Void>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Void> arg0)
         {
            validateSetupChunk_ = true;
         }
      }));
      
      // single shot rendering of chunk output line widgets
      // (we wait until after the first render to ensure that
      // ace places the line widgets correctly)
      releaseOnDismiss.add(docDisplay_.addRenderFinishedHandler(
            new RenderFinishedEvent.Handler()
      { 
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            if (initialChunkDefs_ != null)
            {
               for (int i = 0; i<initialChunkDefs_.length(); i++)
               {
                  createChunkOutput(initialChunkDefs_.get(i));
               }
               // if we got chunk content, load initial chunk output from server
               if (initialChunkDefs_.length() > 0)
                  loadInitialChunkOutput();

               initialChunkDefs_ = null;
               
               // sync to editor style changes
               editingTarget.addEditorThemeStyleChangedHandler(
                                             TextEditingTargetNotebook.this);
            }
         }
      }));
   }
   
   @Inject
   public void initialize(EventBus events, 
         RMarkdownServerOperations server,
         ConsoleServerOperations console,
         Session session,
         UIPrefs prefs,
         Commands commands,
         Binder binder,
         Provider<SourceWindowManager> pSourceWindowManager)
   {
      events_ = events;
      server_ = server;
      console_ = console;
      session_ = session;
      prefs_ = prefs;
      commands_ = commands;
      binder.bind(commands, this);
      pSourceWindowManager_ = pSourceWindowManager;
      
      events_.addHandler(RmdChunkOutputEvent.TYPE, this);
      events_.addHandler(RmdChunkOutputFinishedEvent.TYPE, this);
      events_.addHandler(SendToChunkConsoleEvent.TYPE, this);
      events_.addHandler(ChunkChangeEvent.TYPE, this);
      events_.addHandler(ChunkContextChangeEvent.TYPE, this);
      events_.addHandler(ConsolePromptEvent.TYPE, this);
      events_.addHandler(ConsoleWriteInputEvent.TYPE, this);
      events_.addHandler(InterruptStatusEvent.TYPE, this);
      events_.addHandler(RestartStatusEvent.TYPE, this);
      
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
      executedSinceActivate_ = false;
   }
   
   public void executeChunk(Scope chunk, String code, String options)
   {
      // maximize the source window if it's paired with the console and this
      // is the first chunk we've executed since we were activated
      if (!executedSinceActivate_)
      {
         pSourceWindowManager_.get().maximizeSourcePaneIfNecessary();
         executedSinceActivate_ = true;
      }
      
      // get the row that ends the chunk
      int row = chunk.getEnd().getRow();

      String chunkId = "";
      String setupCrc32 = "";
      if (isSetupChunkScope(chunk))
      {
         setupCrc32 = setupCrc32_;
         chunkId = SETUP_CHUNK_ID;
      }
      else
      {
         // find or create a matching chunk definition 
         ChunkDefinition chunkDef = getChunkDefAtRow(row);
         if (chunkDef == null)
            return;
         chunkId = chunkDef.getChunkId();
         ensureSetupChunkExecuted();
      }
      
      // decorate the gutter to show the chunk is queued
      docDisplay_.setChunkLineExecState(chunk.getBodyStart().getRow() + 1,
            chunk.getEnd().getRow(), ChunkRowExecState.LINE_QUEUED);
      chunks_.setChunkState(chunk.getPreamble().getRow(), 
            ChunkContextToolbar.STATE_QUEUED);

      // check to see if this chunk is already in the execution queue--if so
      // just update the code and leave it queued
      for (ChunkExecQueueUnit unit: chunkExecQueue_)
      {
         if (unit.chunkId == chunkId)
         {
            unit.code = code;
            unit.options = options;
            unit.setupCrc32 = setupCrc32;
            return;
         }
      }

      // put it in the queue 
      chunkExecQueue_.add(new ChunkExecQueueUnit(chunkId, code,
            options, setupCrc32));
      
      // initiate queue processing
      processChunkExecQueue();
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
   
   @Handler
   public void onNotebookCollapseAllOutput()
   {
      setAllExpansionStates(ChunkOutputWidget.COLLAPSED);
   }

   @Handler
   public void onNotebookExpandAllOutput()
   {
      setAllExpansionStates(ChunkOutputWidget.EXPANDED);
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
      final ChunkDefinition chunkDef = getChunkDefAtRow(event.getRow());
      String options = TextEditingTargetRMarkdownHelper.getRmdChunkOptionText(
            event.getScope(), docDisplay_);
      
      // have the server start recording output from this chunk
      syncWidth(chunkDef.getChunkId());
      server_.setChunkConsole(docUpdateSentinel_.getId(), 
            chunkDef.getChunkId(), options, pixelWidth_, charWidth_, false, 
            new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
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
                                            .setCodeExecuting(false);
   }
   
   @Override
   public void onRmdChunkOutput(RmdChunkOutputEvent event)
   {
      // ignore if not targeted at this document
      if (event.getOutput().getDocId() != docUpdateSentinel_.getId())
         return;
      
      // mark chunk execution as finished
      if (executingChunk_ != null &&
          event.getOutput().getChunkId() == executingChunk_.chunkId)
      {
         cleanChunkExecState(executingChunk_.chunkId);
         executingChunk_ = null;
      }
      
      // if nothing at all was returned, this means the chunk doesn't exist on
      // the server, so clean it up here.
      if (event.getOutput().isEmpty())
      {
         events_.fireEvent(new ChunkChangeEvent(
               docUpdateSentinel_.getId(), event.getOutput().getChunkId(), 0, 
               ChunkChangeEvent.CHANGE_REMOVE));
         return;
      }

      // show output in matching chunk
      String chunkId = event.getOutput().getChunkId();
      if (outputs_.containsKey(chunkId))
      {
         outputs_.get(chunkId).getOutputWidget()
                              .showChunkOutput(event.getOutput());
      }
      
      // process next chunk in execution queue
      processChunkExecQueue();
   }

   @Override
   public void onRmdChunkOutputFinished(RmdChunkOutputFinishedEvent event)
   {
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
                                           .onOutputFinished();
         }
      }
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
                  1, true, ChunkOutputWidget.EXPANDED, event.getChunkId()));
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
   public void onConsolePrompt(ConsolePromptEvent event)
   {
      // mark chunk execution as finished
      if (executingChunk_ != null)
      {
         cleanChunkExecState(executingChunk_.chunkId);
         executingChunk_ = null;
      }
      
      processChunkExecQueue();
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
      docDisplay_.setChunkLineExecState(start + 1, start + lines,
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
      
      for (ChunkOutputUi output: outputs_.values())
      {
         output.getOutputWidget().syncHeight(false);
      }
   }

   @Override
   public void onInterruptStatus(InterruptStatusEvent event)
   {
      if (event.getStatus() != InterruptStatusEvent.INTERRUPT_INITIATED)
         return;
      
      // when the user interrupts R, clear any pending chunk executions
      for (ChunkExecQueueUnit unit: chunkExecQueue_)
      {
         cleanChunkExecState(unit.chunkId);
      }
      chunkExecQueue_.clear();
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // if we had recorded a run of the setup chunk prior to restart, clear it
      if (event.getStatus() == RestartStatusEvent.RESTART_COMPLETED &&
          !StringUtil.isNullOrEmpty(setupCrc32_))
      {
         writeSetupCrc32("");
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

   // Private methods --------------------------------------------------------
   
   private void processChunkExecQueue()
   {
      if (chunkExecQueue_.isEmpty() || executingChunk_ != null)
         return;
      
      // begin chunk execution
      final ChunkExecQueueUnit unit = chunkExecQueue_.remove();
      executingChunk_ = unit;
      
      // let the chunk widget know it's started executing
      if (outputs_.containsKey(unit.chunkId))
      {
         ChunkOutputUi output = outputs_.get(unit.chunkId);
         boolean visible = output.getOutputWidget().isVisible();

         output.getOutputWidget().setCodeExecuting(true);
         syncWidth(unit.chunkId);
         
         // we want to be sure the user can see the row beneath the output 
         // (this is just a convenient way to determine whether the entire 
         // output is visible)
         int targetRow = output.getCurrentRow() + 1;
         
         // if the output is not visible, we have to guess at how big it will
         // become -- guess ~10 rows of text
         if (!visible)
         {
            targetRow = Math.min(docDisplay_.getRowCount(), targetRow + 10);
         }
      
         if (docDisplay_.getLastVisibleRow() < targetRow)
         {
            Scope chunk = output.getScope();
            Rectangle bounds = docDisplay_.getPositionBounds(
                  chunk.getPreamble());
            docDisplay_.scrollToY(docDisplay_.getScrollTop() + 
                  (bounds.getTop() - (docDisplay_.getBounds().getTop() + 60)));
         }
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
            unit.options,
            pixelWidth_,
            charWidth_,
            true,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void v)
               {
                  console_.consoleInput(unit.code, unit.chunkId, 
                        new VoidServerRequestCallback());

                  // if this was the setup chunk, mark it
                  if (!StringUtil.isNullOrEmpty(unit.setupCrc32))
                     writeSetupCrc32(unit.setupCrc32);
               }

               @Override
               public void onError(ServerError error)
               {
                  // don't leave the chunk hung in execution state
                  if (executingChunk_ != null)
                  {
                     if (outputs_.containsKey(executingChunk_.chunkId))
                     {
                        outputs_.get(executingChunk_.chunkId)
                                .getOutputWidget().onOutputFinished();
                     }
                     cleanChunkExecState(executingChunk_.chunkId);
                  }

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
   
   private ChunkDefinition getChunkDefAtRow(int row)
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
         chunkDef = ChunkDefinition.create(row, 1, true, 
               ChunkOutputWidget.EXPANDED,
               "c" + StringUtil.makeRandomId(12));
         
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
      
      ArrayList<Widget> widgets = new ArrayList<Widget>();
      widgets.add(output.getOutputWidget());
      FadeOutAnimation anim = new FadeOutAnimation(widgets, new Command()
      {
         @Override
         public void execute()
         {
            output.remove();
            outputs_.remove(chunkId);
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
            RStudioGinjector.INSTANCE.getUIPrefs()
                                     .showRmdChunkOutputInline().getValue());
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

      // no reason to do work if we don't need to re-validate the setup chunk
      if (!validateSetupChunk_ && !StringUtil.isNullOrEmpty(setupCrc32_))
         return;
      validateSetupChunk_ = false;

      // find the setup chunk
      JsArray<Scope> scopes = docDisplay_.getScopeTree();
      for (int i = 0; i < scopes.length(); i++)
      {
         if (isSetupChunkScope(scopes.get(i)))
         {
            // extract the body of the chunk
            String setupCode = docDisplay_.getCode(
                  scopes.get(i).getBodyStart(),
                  Position.create(scopes.get(i).getEnd().getRow(), 0));
            
            // hash the body and prefix with the virtual session ID (so all
            // hashes are automatically invalidated when the session changes)
            String crc32 = session_.getSessionInfo().getSessionId() +
                  StringUtil.crc32(setupCode);
            
            // compare with previously known hash; if it differs, re-run the
            // setup chunk
            if (crc32 != setupCrc32_)
            {
               setupCrc32_ = crc32;
               executeChunk(scopes.get(i), setupCode, "");
            }
         }
      }
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
   
   private void syncWidth(String chunkId)
   {
      if (!outputs_.containsKey(chunkId))
         return;
      
      // check the width and see if it's already synced
      Element ele = outputs_.get(chunkId).getOutputWidget().getElement();
      int width = ele.getOffsetWidth();
      if (pixelWidth_ == width)
         return;
      
      // it's not synced, so compute the new width
      pixelWidth_ = width;
      charWidth_ = DomUtils.getCharacterWidth(ele, 
            ConsoleResources.INSTANCE.consoleStyles().console());
   }
   
   private Scope getChunkScope(String chunkId)
   {
      if (chunkId == SETUP_CHUNK_ID)
      {
         JsArray<Scope> scopes = docDisplay_.getScopeTree();
         for (int i = 0; i < scopes.length(); i++)
         {
            if (isSetupChunkScope(scopes.get(i)))
               return scopes.get(i);
         }
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
      JsArray<Scope> scopes = docDisplay_.getScopeTree();
      for (int i = 0; i < scopes.length(); i++)
      {
         if (isSetupChunkScope(scopes.get(i)) && 
             scopes.get(i).getPreamble().getRow() == preambleRow)
            return SETUP_CHUNK_ID;
      }
      
      return null;
   }
   
   private void setAllExpansionStates(int state)
   {
      for (ChunkOutputUi output: outputs_.values())
      {
         output.getOutputWidget().setExpansionState(state);
      }
   }
   
   private JsArray<ChunkDefinition> initialChunkDefs_;
   private HashMap<String, ChunkOutputUi> outputs_;
   private Queue<ChunkExecQueueUnit> chunkExecQueue_;
   private ChunkExecQueueUnit executingChunk_;
   
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel docUpdateSentinel_;
   ArrayList<HandlerRegistration> releaseOnDismiss_;
   private final TextEditingTarget editingTarget_;
   private final TextEditingTarget.Display editingDisplay_;
   private final TextEditingTargetChunks chunks_;
   private Session session_;
   private Provider<SourceWindowManager> pSourceWindowManager_;
   private UIPrefs prefs_;
   private Commands commands_;

   private RMarkdownServerOperations server_;
   private ConsoleServerOperations console_;
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
   private boolean executedSinceActivate_ = false;
   
   private int state_ = STATE_NONE;

   // no chunk state
   private final static int STATE_NONE = 0;
   
   // synchronizing chunk state from server
   private final static int STATE_INITIALIZING = 0;
   
   // chunk state synchronized
   private final static int STATE_INITIALIZED = 1;
   
   public final static String CHUNK_OUTPUT_TYPE    = "chunk_output_type";
   public final static String CHUNK_OUTPUT_INLINE  = "inline";
   public final static String CHUNK_OUTPUT_CONSOLE = "console";
   
   private final static String LAST_SETUP_CRC32 = "last_setup_crc32";
   private final static String SETUP_CHUNK_ID = "csetup_chunk";
}
