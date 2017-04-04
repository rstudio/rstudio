/*
 * ObjectExplorerPresenter.java
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
package org.rstudio.studio.client.workbench.views.source.editors.explorer;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.events.ObjectExplorerEvent;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.events.OpenObjectExplorerEvent;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ObjectExplorerPresenter
      implements ObjectExplorerEvent.Handler
{
   @Inject
   public ObjectExplorerPresenter(Commands commands,
                                  EventBus events,
                                  SourceServerOperations server)
   {
      commands_ = commands;
      events_ = events;
      server_ = server;
      
      events_.addHandler(ObjectExplorerEvent.TYPE, this);
   }
   
   // Handlers ----
   
   @Override
   public void onObjectExplorerEvent(ObjectExplorerEvent event)
   {
      Debug.logToRConsole("Responding to object explorer event");
      
      switch (event.getType())
      {
      case NEW:        onNew(event.getData());       break;
      case OPEN_NODE:  onOpenNode(event.getData());  break;
      case CLOSE_NODE: onCloseNode(event.getData()); break;
      case UNKNOWN:    break;
      }
   }
   
   // Private methods ----
   
   private void onNew(ObjectExplorerEvent.Data eventData)
   {
      Debug.logToRConsole("ObjectExplorer: onNew()");
      ObjectExplorerHandle handle = eventData.getHandle();
      OpenObjectExplorerEvent event = new OpenObjectExplorerEvent(handle);
      events_.fireEvent(event);
   }
   
   private void onOpenNode(ObjectExplorerEvent.Data data)
   {
      Debug.logToRConsole("ObjectExplorer: onOpenNode()");
      
   }
   
   private void onCloseNode(ObjectExplorerEvent.Data data)
   {
      Debug.logToRConsole("ObjectExplorer: onCloseNode()");
   }
   
   // Private members ----
   
   private final Commands commands_;
   private final EventBus events_;
   private final SourceServerOperations server_;
}
