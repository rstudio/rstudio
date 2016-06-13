/*
 * NotebookQueueState.java
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
import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rmarkdown.events.ChunkExecStateChangedEvent;
import org.rstudio.studio.client.rmarkdown.events.NotebookRangeExecutedEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookDocQueue;
import org.rstudio.studio.client.rmarkdown.model.NotebookExecRange;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetScopeHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.core.client.JsArray;

public class NotebookQueueState implements NotebookRangeExecutedEvent.Handler,
                                           ChunkExecStateChangedEvent.Handler
{
   public NotebookQueueState(DocDisplay display, TextEditingTarget editingTarget,
         DocUpdateSentinel sentinel, RMarkdownServerOperations server, 
         EventBus events, TextEditingTargetNotebook notebook)
   {
      docDisplay_ = display;
      sentinel_ = sentinel;
      server_ = server;
      events_ = events;
      notebook_ = notebook;
      editingTarget_ = editingTarget;
      scopeHelper_ = new TextEditingTargetScopeHelper(display);
      
      events_.addHandler(NotebookRangeExecutedEvent.TYPE, this);
      events_.addHandler(ChunkExecStateChangedEvent.TYPE, this);
      
      syncWidth();
   }
   
   public boolean isExecuting()
   {
      return queue_ != null && !queue_.complete();
   }
   
   public String getExecutingChunkId()
   {
      if (executingUnit_ == null)
         return null;
      
      return executingUnit_.getChunkId();
   }
   
   public void setQueue(NotebookDocQueue queue)
   {
      queue_ = queue;
      renderQueueState();
   }
   
   public int getChunkExecMode(String chunkId)
   {
      if (queue_ != null)
      {
         // check queued units
         NotebookQueueUnit unit = getUnit(chunkId);
         
         // check completed units
         if (unit == null)
         {
            for (int i = 0; i < queue_.getCompletedUnits().length(); i++)
            {
               NotebookQueueUnit completedUnit = 
                     queue_.getCompletedUnits().get(i);
               if (completedUnit.getChunkId() == chunkId)
                  return completedUnit.getExecMode();
            }
         }
         else
         {
            return unit.getExecMode();
         }
      }
      return TextEditingTargetNotebook.MODE_BATCH;
   }
   
   public void clear()
   {
      if (queue_ != null)
      {
         for (int i = 0; i < queue_.getUnits().length(); i++) 
         {
            notebook_.cleanChunkExecState(queue_.getUnits().get(i).getChunkId());
         }
         
         queue_.removeAllUnits();
      }
      editingTarget_.getStatusBar().hideNotebookProgress(true);
   }
   
   public void executeRange(Scope chunk, Range range)
   {
      if (isExecuting())
      {
         String chunkId = notebook_.getRowChunkId(chunk.getPreamble().getRow());
         if (chunkId == null)
            return;

         NotebookQueueUnit unit = getUnit(chunkId);
         if (unit == null)
         {
            // unit is not in the queue; add it
            queueChunkRange(chunk, range, TextEditingTargetNotebook.MODE_BATCH,
                  TextEditingTargetNotebook.SCOPE_PARTIAL);
         }
         else
         {
            // unit is in the queue, modify it
            unit.addPendingRange(getNotebookExecRange(chunk, range));

            // redraw the pending lines
            renderLineState(chunk.getBodyStart().getRow(), 
                  unit.getPendingLines(), ChunkRowExecState.LINE_QUEUED);

            server_.updateNotebookExecQueue(unit, 
                  NotebookDocQueue.QUEUE_OP_UPDATE, "", 
                  new VoidServerRequestCallback());
         }
      }
      else
      {
         // no queue, create one
         createQueue("Run Chunk");
         NotebookQueueUnit unit = unitFromScope(chunk, range, 
               TextEditingTargetNotebook.MODE_SINGLE,
               TextEditingTargetNotebook.SCOPE_PARTIAL);
         queue_.addUnit(unit);
         executeQueue();
      }
   }
   
   public void executeChunk(Scope chunk)
   {
      if (isExecuting())
      {
         queueChunkRange(chunk, scopeHelper_.getSweaveChunkInnerRange(chunk),
               TextEditingTargetNotebook.MODE_SINGLE, 
               TextEditingTargetNotebook.SCOPE_CHUNK);
      }
      else
      {
         List<Scope> scopes = new ArrayList<Scope>();
         scopes.add(chunk);
         executeChunks("Run Chunk", scopes);
      }
   }
   
   public void executeChunks(String jobDesc, List<Scope> scopes)
   {
      // TODO: handle case where there's already a queue
      
      createQueue(jobDesc);

      // create queue units from scopes
      for (Scope scope: scopes)
      {
         NotebookQueueUnit unit = unitFromScope(scope,
               scopeHelper_.getSweaveChunkInnerRange(scope),
               TextEditingTargetNotebook.MODE_BATCH,
               TextEditingTargetNotebook.SCOPE_CHUNK);
         queue_.addUnit(unit);
      }
      
      executeQueue();
   }
   
   public boolean isChunkExecuting(String chunkId)
   {
      if (executingUnit_ == null)
         return false;

      return executingUnit_.getChunkId() == chunkId;
   }
   
   public boolean isChunkQueued(String chunkId)
   {
      if (queue_ == null)
         return false;

      return getUnit(chunkId) != null;
   }

   public void dequeueChunk(int preambleRow)
   {
      // find the chunk's ID
      String chunkId = notebook_.getRowChunkId(preambleRow);
      if (StringUtil.isNullOrEmpty(chunkId))
         return;

      notebook_.cleanChunkExecState(chunkId);
      
      // clear from the execution queue and update display
      for (int i = 0; i < queue_.getUnits().length(); i++)
      {
         if (queue_.getUnits().get(i).getChunkId() == chunkId)
         {
            NotebookQueueUnit unit = queue_.getUnits().get(i);
            queue_.removeUnit(unit);
            server_.updateNotebookExecQueue(unit, 
                  NotebookDocQueue.QUEUE_OP_DELETE, "",
                  new VoidServerRequestCallback());
            break;
         }
      }
   }
   
   // Event handlers ----------------------------------------------------------
   
   @Override
   public void onNotebookRangeExecuted(NotebookRangeExecutedEvent event)
   {
      if (queue_ == null || event.getDocId() != queue_.getDocId())
         return;
      
      Scope scope = notebook_.getChunkScope(event.getChunkId());
      if (scope == null)
         return;
      
      if (isChunkExecuting(event.getChunkId()))
      {
         executingUnit_.setExecutingRange(event.getExecRange());
      }
      
      // find the queue unit and convert to lines
      for (int i = 0; i < queue_.getUnits().length(); i++)
      {
         NotebookQueueUnit unit = queue_.getUnits().get(i);
         if (unit.getChunkId() == event.getChunkId())
         {
            List<Integer> lines = unit.linesFromRange(event.getExecRange());
            renderLineState(scope.getBodyStart().getRow(), 
                 lines, ChunkRowExecState.LINE_EXECUTED);
            break;
         }
      }
   }

   @Override
   public void onChunkExecStateChanged(ChunkExecStateChangedEvent event)
   {
      if (queue_ == null || event.getDocId() != queue_.getDocId())
         return;

      switch (event.getExecState())
      {
      case NotebookDocQueue.CHUNK_EXEC_STARTED:
         // find the unit
         executingUnit_ = getUnit(event.getChunkId());

         // unfold the scope
         Scope scope = notebook_.getChunkScope(event.getChunkId());
         if (scope != null)
         {
            docDisplay_.unfold(Range.fromPoints(scope.getPreamble(),
                                                scope.getEnd()));
         }
         
         // apply options
         notebook_.setOutputOptions(event.getChunkId(), 
               event.getOptions());

         // TODO: scroll the widget into view if it's a single-shot exec
      
         notebook_.setChunkExecuting(event.getChunkId(), 
               executingUnit_.getExecMode());

         break;

      case NotebookDocQueue.CHUNK_EXEC_FINISHED:

         if (executingUnit_ != null && 
             executingUnit_.getChunkId() == event.getChunkId())
         {
            queue_.removeUnit(executingUnit_);
            queue_.addCompletedUnit(executingUnit_);
            executingUnit_ = null;
            
            // if there are no more units, clean up the queue so we get a clean
            // slate on the next execution
            if (queue_.complete())
            {
               editingTarget_.getStatusBar().hideNotebookProgress(false);
            }
            else
            {
               updateNotebookProgress();
            }
         }
         break;

      case NotebookDocQueue.CHUNK_EXEC_CANCELLED:

         queue_.removeUnit(event.getChunkId());
         notebook_.cleanChunkExecState(event.getChunkId());
         break;
      }
   }
   
   public NotebookQueueUnit executingUnit()
   {
      return executingUnit_;
   }

   public void renderQueueState()
   {
      if (queue_ == null)
         return;
      
      JsArray<NotebookQueueUnit> units = queue_.getUnits();
      for (int i = 0; i < units.length(); i++)
      {
         NotebookQueueUnit unit = units.get(i);

         // get the offset into the doc 
         Scope scope = notebook_.getChunkScope(unit.getChunkId());
         if (scope == null)
            continue;
         
         // draw the completed lines (queue them first so they render properly
         // in the gutter)
         List<Integer> completed = unit.getCompletedLines();
         int row = scope.getBodyStart().getRow();
         renderLineState(row, completed, ChunkRowExecState.LINE_QUEUED);
         renderLineState(row, completed, ChunkRowExecState.LINE_EXECUTED);

         // draw the pending lines
         renderLineState(row, unit.getPendingLines(), 
               ChunkRowExecState.LINE_QUEUED);
         
         notebook_.setChunkState(scope, ChunkContextToolbar.STATE_QUEUED);
      }

      // update the status bar
      if (queue_.getMaxUnits() > 1)
      {
         editingTarget_.getStatusBar().showNotebookProgress(
               queue_.getJobDesc());
         updateNotebookProgress();
      }
   }
   
   private void renderLineState(int offset, List<Integer> lines, int state)
   {
      for (Integer line: lines)
      {
         docDisplay_.setChunkLineExecState(line + offset, line + offset, state);
      }
   }
   
   private NotebookQueueUnit unitFromScope(Scope scope, Range range, 
         int execMode, int execScope)
   {
      // find associated chunk definition
      String id = null;
      if (TextEditingTargetNotebook.isSetupChunkScope(scope))
         id = TextEditingTargetNotebook.SETUP_CHUNK_ID;
      ChunkDefinition def = getChunkDefAtRow(scope.getEnd().getRow(), id);

      String code = docDisplay_.getCode(
         scope.getPreamble(),
         scope.getEnd());
      NotebookQueueUnit unit = NotebookQueueUnit.create(sentinel_.getId(), 
            def.getChunkId(), execMode, execScope, code);
      
      unit.addPendingRange(getNotebookExecRange(scope, range));
      
      return unit;
   }
   
   private NotebookExecRange getNotebookExecRange(Scope scope, Range range)
   {
      // convert range into character offsets
      Position startPos = range.getStart();
      Position endPos = range.getEnd();
      int start = 0;
      int end = 0;
      int pos = 0;
      
      for (int row = scope.getPreamble().getRow();
           row < scope.getEnd().getRow();
           row++)
      {
         String line = docDisplay_.getLine(row);
         for (int col = 0; col <= line.length(); col++)
         {
            if (startPos.getRow() == row && startPos.getColumn() == col)
            {
               start = pos;
            }
            else if (endPos.getRow() == row && endPos.getColumn() == col)
            {
               end = pos;
               break;
            }
            pos++;
         }
      }
      
      // if we never found the end, just use the last character (less one for 
      // the newline)
      if (end == 0)
         end = pos - 1;
      
      return NotebookExecRange.create(start, end);
   }
   
   private NotebookQueueUnit getUnit(String chunkId)
   {
      JsArray<NotebookQueueUnit> units = queue_.getUnits();
      for (int i = 0; i < units.length(); i++)
      {
         if (units.get(i).getChunkId() == chunkId)
         {
            return units.get(i);
         }
      }
      return null;
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
               TextEditingTargetNotebook.getKnitrChunkLabel(row, docDisplay_, 
                                  new ScopeList(docDisplay_)));
         
         if (newId == TextEditingTargetNotebook.SETUP_CHUNK_ID)
            chunkDef.getOptions().setInclude(false);
         
         RStudioGinjector.INSTANCE.getEventBus().fireEvent(new ChunkChangeEvent(
               sentinel_.getId(), chunkDef.getChunkId(), row, 
               ChunkChangeEvent.CHANGE_CREATE));
      }
      return chunkDef;
   }
   
   private void queueChunkRange(Scope chunk, Range range, int execMode, 
         int execScope)
   {
      NotebookQueueUnit unit = unitFromScope(chunk, range, execMode, execScope);

      renderLineState(chunk.getBodyStart().getRow(), 
            unit.getPendingLines(), ChunkRowExecState.LINE_QUEUED);
      notebook_.setChunkState(chunk, ChunkContextToolbar.STATE_QUEUED);

      queue_.addUnit(unit);
      server_.updateNotebookExecQueue(unit, 
            NotebookDocQueue.QUEUE_OP_ADD, "", 
            new VoidServerRequestCallback());
   }
   
   private void createQueue(String jobDesc)
   {
      // ensure width is up to date
      syncWidth();
      
      // create new queue
      queue_ = NotebookDocQueue.create(sentinel_.getId(), jobDesc, 
            notebook_.getCommitMode(), pixelWidth_, charWidth_);
   }
   
   private void executeQueue()
   {
      server_.executeNotebookChunks(queue_, new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            renderQueueState();
         }

         @Override
         public void onError(ServerError error)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Can't execute " + queue_.getJobDesc(), error.getMessage());
         }
      });
   }
   
   public void updateNotebookProgress()
   {
      editingTarget_.getStatusBar().updateNotebookProgress(
           (int)Math.round(100 * ((double)(queue_.getMaxUnits() - 
                                           queue_.getUnits().length())) / 
                                  (double) queue_.getMaxUnits()));
   }
   
   private NotebookDocQueue queue_;
   
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel sentinel_;
   private final RMarkdownServerOperations server_;
   private final TextEditingTargetNotebook notebook_;
   private final EventBus events_;
   private final TextEditingTargetScopeHelper scopeHelper_;
   private final TextEditingTarget editingTarget_;
   
   private int pixelWidth_;
   private int charWidth_;
   public NotebookQueueUnit executingUnit_;
}
