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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.SendToChunkConsoleEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutput;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;


public class EditingTargetInlineChunkExecution
      implements RmdChunkOutputEvent.Handler,
                 RmdChunkOutputFinishedEvent.Handler
{
   @Inject
   private void initialize(EventBus events)
   {
      events_ = events;
   }
   
   public EditingTargetInlineChunkExecution(String docId)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      docId_ = docId;
      handlers_ = new ArrayList<HandlerRegistration>();
   }
   
   public void execute(Range range)
   {
      clearHandlers();
      
      handlers_.add(events_.addHandler(RmdChunkOutputEvent.TYPE, this));
      handlers_.add(events_.addHandler(RmdChunkOutputFinishedEvent.TYPE, this));
      
      // create dummy scope for execution
      Scope scope = Scope.createRScopeNode(
            StringUtil.makeRandomId(8),
            range.getStart(),
            range.getEnd(),
            Scope.SCOPE_TYPE_CHUNK);
      
      SendToChunkConsoleEvent event =
            new SendToChunkConsoleEvent(docId_, scope, range, NotebookQueueUnit.EXEC_SCOPE_INLINE);
      events_.fireEvent(event);
   }
   
   private void clearHandlers()
   {
      for (HandlerRegistration handler : handlers_)
         handler.removeHandler();
      handlers_.clear();
   }
   
   // Handlers ----
   
   @Override
   public void onRmdChunkOutputFinished(RmdChunkOutputFinishedEvent event)
   {
      Debug.logObject(event.getData());
      Debug.logToRConsole("RmdChunkOutputFinishedEvent");
   }

   @Override
   public void onRmdChunkOutput(RmdChunkOutputEvent event)
   {
      Debug.logToRConsole("RmdChunkOutputEvent");
      RmdChunkOutput output = event.getOutput();
   }
   
   // Private Members ----

   private final String docId_;
   private final List<HandlerRegistration> handlers_;
   
   // Injected ----
   private EventBus events_;

}
