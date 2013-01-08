/*
 * InlineEditor.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.workspace.table;

import com.google.gwt.event.shared.HandlerManager;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.workspace.events.HasValueChangeRequestHandlers;
import org.rstudio.studio.client.workbench.views.workspace.events.ValueChangeRequestEvent;
import org.rstudio.studio.client.workbench.views.workspace.events.ValueChangeRequestHandler;
import org.rstudio.studio.client.workbench.views.workspace.events.WorkspaceRefreshEvent;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceObjectInfo;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceServerOperations;

public class InlineEditor<T> implements ValueChangeRequestHandler<T>
{
   public interface Display<V> extends HasValueChangeRequestHandlers<V>
   {
   }
   
   public InlineEditor(WorkspaceObjectInfo object,
                      WorkspaceServerOperations server,
                      EventBus events,
                      Display<T> view,
                      GlobalDisplay globalDisplay)
   {
      object_ = object ;
      globalDisplay_ = globalDisplay ;
      handlerManager_ = new HandlerManager(this) ;
      view_ = view ;
      server_ = server ;
      events_ = events ;
      
      view.addValueChangeRequestHandler(this) ;
   }
   
   public void onValueChangeRequest(ValueChangeRequestEvent<T> event)
   {
      server_.setObjectValue(object_.getName(), event.getValue().toString(),
         new ServerRequestCallback<Void>() {
            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage("Unable to update value", 
                         error.getUserMessage(),
                         new Operation()
                         {
                            public void execute()
                            {
                               events_.fireEvent(new WorkspaceRefreshEvent());
                            }
                         }); ;
            }
         }) ;
   }

   private final WorkspaceObjectInfo object_ ;
   @SuppressWarnings("unused")
   private final HandlerManager handlerManager_ ;
   @SuppressWarnings("unused")
   private final Display<T> view_ ;
   private final WorkspaceServerOperations server_ ;
   private final EventBus events_ ;
   private final GlobalDisplay globalDisplay_ ;
}
