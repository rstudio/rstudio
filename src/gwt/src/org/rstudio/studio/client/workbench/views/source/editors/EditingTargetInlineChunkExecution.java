/*
 * EditingTargetInlineChunkExecution.java
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
package org.rstudio.studio.client.workbench.views.source.editors;


import java.util.HashMap;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rmarkdown.events.ChunkExecStateChangedEvent;
import org.rstudio.studio.client.rmarkdown.events.SendToChunkConsoleEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkInlineOutput;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;

import com.google.inject.Inject;


public class EditingTargetInlineChunkExecution
      implements ConsoleWriteOutputHandler,
                 ConsoleWriteErrorHandler,
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
      final String chunkId = "i" + StringUtil.makeRandomId(12);
      
      // create dummy scope for execution
      Scope scope = Scope.createRScopeNode(
            chunkId,
            range.getStart(),
            range.getEnd(),
            Scope.SCOPE_TYPE_CHUNK);
      
      final ChunkInlineOutput output = new ChunkInlineOutput(chunkId);
      
      final ScreenCoordinates pos = computePopupPosition(range);
      output.setPopupPosition(pos.getPageX(), pos.getPageY());
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
   }

   private ScreenCoordinates computePopupPosition(Range range)
   {
      Rectangle bounds = display_.getRangeBounds(range);
      Point center = bounds.center();
      
      int pageX = center.getX() - (100 / 2);
      int pageY = bounds.getBottom() + 10;
      
      return ScreenCoordinates.create(pageX, pageY);
   }

   // Private Members ----

   private final String docId_;
   private final DocDisplay display_;
   private final HashMap<String, ChunkInlineOutput> outputs_;
   
   // Injected ----
   private EventBus events_;
}
