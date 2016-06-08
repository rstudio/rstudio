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
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.core.client.JsArray;

public class NotebookQueueState implements NotebookRangeExecutedEvent.Handler,
                                           ChunkExecStateChangedEvent.Handler
{
   public NotebookQueueState(DocDisplay display, DocUpdateSentinel sentinel,
         RMarkdownServerOperations server, EventBus events, 
         TextEditingTargetNotebook notebook)
   {
      docDisplay_ = display;
      sentinel_ = sentinel;
      server_ = server;
      events_ = events;
      notebook_ = notebook;
      
      events_.addHandler(NotebookRangeExecutedEvent.TYPE, this);
      events_.addHandler(ChunkExecStateChangedEvent.TYPE, this);
      
      syncWidth();
   }
   
   public void executeChunk(Scope chunk)
   {
      List<Scope> scopes = new ArrayList<Scope>();
      scopes.add(chunk);
      executeChunks("Run Chunk", scopes);
      
      // TODO: insert into existing queue if it exists
   }
   
   public void executeChunks(final String jobDesc, List<Scope> scopes)
   {
      // ensure width is up to date
      syncWidth();
      
      // create new queue
      queue_ = NotebookDocQueue.create(sentinel_.getId(), jobDesc, 
            pixelWidth_, charWidth_);

      // create queue units from scopes
      for (Scope scope: scopes)
      {
         NotebookQueueUnit unit = unitFromScope(scope);
         queue_.addUnit(unit);
      }
      
      // send it to the server!
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
                  "Can't execute " + jobDesc, error.getMessage());
         }
      });
   }
   
   public boolean isChunkExecuting(String chunkId)
   {
      if (executingUnit_ == null)
         return false;
      return executingUnit_.getChunkId() == chunkId;
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
         executingUnit_.setExecutingRange(event.getExecRange());
      
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
      
      if (event.getExecState() == NotebookDocQueue.CHUNK_EXEC_STARTED)
      {
         // find the unit
         JsArray<NotebookQueueUnit> units = queue_.getUnits();
         for (int i = 0; i < units.length(); i++)
         {
            if (units.get(i).getChunkId() == event.getChunkId())
            {
               executingUnit_ = units.get(i);
               break;
            }
         }

         // TODO: respect actual chunk execution mode
         notebook_.setChunkExecuting(event.getChunkId(), 
               TextEditingTargetNotebook.MODE_BATCH);
      }
      else if (event.getExecState() == NotebookDocQueue.CHUNK_EXEC_FINISHED)
      {
         if (executingUnit_ != null && 
             executingUnit_.getChunkId() == event.getChunkId())
         {
            executingUnit_ = null;
         }
      }
   }
   
   public NotebookQueueUnit executingUnit()
   {
      return executingUnit_;
   }

   public void renderQueueState()
   {
      JsArray<NotebookQueueUnit> units = queue_.getUnits();
      for (int i = 0; i < units.length(); i++)
      {
         NotebookQueueUnit unit = units.get(i);

         // get the offset into the doc 
         Scope scope = notebook_.getChunkScope(unit.getChunkId());
         if (scope == null)
            continue;
         
         // draw the completed lines
         renderLineState(scope.getBodyStart().getRow(), 
               unit.getCompletedLines(), ChunkRowExecState.LINE_EXECUTED);

         // draw the pending lines
         renderLineState(scope.getBodyStart().getRow(), 
               unit.getPendingLines(), ChunkRowExecState.LINE_QUEUED);
      }
   }
   
   private void renderLineState(int offset, List<Integer> lines, int state)
   {
      for (Integer line: lines)
      {
         docDisplay_.setChunkLineExecState(line + offset, line + offset, state);
      }
   }
   
   private NotebookQueueUnit unitFromScope(Scope scope)
   {
      ChunkDefinition def = getChunkDefAtRow(scope.getEnd().getRow(), 
            null);
      String code = docDisplay_.getCode(
         scope.getPreamble(),
         scope.getEnd());
      NotebookQueueUnit unit = NotebookQueueUnit.create(sentinel_.getId(), 
            def.getChunkId(), code);
      
      // add a single range which encompasses all of the actual code in the
      // chunk
      int start = code.indexOf("\n") + 1;
      int end = code.lastIndexOf("\n");
      
      NotebookExecRange range = NotebookExecRange.create(start, end);
      unit.addPendingRange(range);
      
      return unit;
   }
         
   // TODO: resolve with copy at TextEditingTargetNotebook
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
   
   // TODO: resolve with copy at TextEditingTargetNotebook
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
   
   private NotebookDocQueue queue_;
   
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel sentinel_;
   private final RMarkdownServerOperations server_;
   private final TextEditingTargetNotebook notebook_;
   private final EventBus events_;
   
   private int pixelWidth_;
   private int charWidth_;
   public NotebookQueueUnit executingUnit_;
}
