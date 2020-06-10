/*
 * EditingTargetInlineChunkExecution.java
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
package org.rstudio.studio.client.workbench.views.source.editors;


import java.util.HashMap;

import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rmarkdown.events.ChunkExecStateChangedEvent;
import org.rstudio.studio.client.rmarkdown.events.SendToChunkConsoleEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookDocQueue;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkInlineOutput;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;


public class EditingTargetInlineChunkExecution
      implements ConsoleWriteOutputEvent.Handler,
                 ConsoleWriteErrorEvent.Handler,
                 ChunkExecStateChangedEvent.Handler
{
   @Inject
   private void initialize(EventBus events)
   {
      events_ = events;
      events_.addHandler(ConsoleWriteOutputEvent.TYPE, this);
      events_.addHandler(ConsoleWriteErrorEvent.TYPE, this);
      events_.addHandler(ChunkExecStateChangedEvent.TYPE, this);
   }
   
   public EditingTargetInlineChunkExecution(DocDisplay display, String docId)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      display_ = display;
      docId_ = docId;
      outputs_ = new HashMap<String, ChunkInlineOutput>();
   }
   
   public void execute(Range range)
   {
      // synthesize an identifier for this chunk execution
      final String chunkId = "i" + StringUtil.makeRandomId(12);
      
      // check to see if we're already showing a panel for this range; if we
      // are, remove it to make way for the new one
      for (ChunkInlineOutput output: outputs_.values())
      {
         if (output.range().isEqualTo(range))
         {
            if (output.state() == ChunkInlineOutput.State.Finished)
            {
               // remove old, completed output for this input
               output.hide();
               outputs_.remove(output.chunkId());
            }
            else
            {
               // we already have an output panel for this input, and it's
               // either waiting to execute or currently executing. ignore this
               // request to re-execute the range, as it's likely to be an
               // unintended duplicate.
               return;
            }
         }
      }
      
      // create dummy scope for execution
      Scope scope = Scope.createRScopeNode(
            chunkId,
            range.getStart(),
            range.getEnd(),
            Scope.SCOPE_TYPE_CHUNK);
      
      // create popup panel to host output
      final ChunkInlineOutput output = new ChunkInlineOutput(chunkId, 
            display_.createAnchoredSelection(range.getStart(), range.getEnd()));
      
      // auto dismiss the panel when the cursor leaves the inline chunk
      final Mutable<HandlerRegistration> cursorHandler =
            new Mutable<HandlerRegistration>();
      cursorHandler.set(display_.addCursorChangedHandler(
            new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            Position position = event.getPosition();
            if (!output.range().contains(position))
            {
               output.hide();
            }
         }
      }));

      // when the popup is dismissed, clean up local state
      output.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            outputs_.remove(chunkId);
            cursorHandler.get().removeHandler();
         }
      });

      // render offscreen until complete
      output.setPopupPosition(-100000, -100000);
      output.show();
      outputs_.put(chunkId, output);

      SendToChunkConsoleEvent event =
            new SendToChunkConsoleEvent(docId_, scope, range, 
                  NotebookQueueUnit.EXEC_SCOPE_INLINE);
      events_.fireEvent(event);
   }
   
   // Handlers ----
   
   @Override
   public void onConsoleWriteError(ConsoleWriteErrorEvent event)
   {
      if (outputs_.containsKey(event.getConsole()))
        outputs_.get(event.getConsole()).onConsoleWriteError(event);
   }

   @Override
   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      if (outputs_.containsKey(event.getConsole()))
        outputs_.get(event.getConsole()).onConsoleWriteOutput(event);
   }

   @Override
   public void onChunkExecStateChanged(ChunkExecStateChangedEvent event)
   {
      // ignore if not targeted at one of our chunks
      if (event.getDocId() != docId_ || 
          !outputs_.containsKey(event.getChunkId()))
         return;
      
      ChunkInlineOutput output = outputs_.get(event.getChunkId());
      
      if (event.getExecState() == NotebookDocQueue.CHUNK_EXEC_STARTED)
      {
         output.setState(ChunkInlineOutput.State.Started);
      }
      else if (event.getExecState() == NotebookDocQueue.CHUNK_EXEC_FINISHED)
      {
         output.setState(ChunkInlineOutput.State.Finished);
         output.positionNearRange(display_, output.range());
         output.show();
      }
   }

   // Private Members ----

   private final String docId_;
   private final DocDisplay display_;
   private final HashMap<String, ChunkInlineOutput> outputs_;
   
   // Injected ----
   private EventBus events_;
}
