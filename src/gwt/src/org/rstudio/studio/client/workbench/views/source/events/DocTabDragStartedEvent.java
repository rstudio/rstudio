/*
 * DocTabDragStartedEvent.java
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

package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class DocTabDragStartedEvent 
             extends CrossWindowEvent<DocTabDragStartedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onDocTabDragStarted(DocTabDragStartedEvent event);
   }

   public static final GwtEvent.Type<DocTabDragStartedEvent.Handler> TYPE =
      new GwtEvent.Type<DocTabDragStartedEvent.Handler>();
   
   public DocTabDragStartedEvent()
   {
   }
   
   // this event is broadcast to all satellites when a document tab drag 
   // begins; since the data being dragged is not available until the drop 
   // occurs, this side channel allows the satellites to know e.g. how wide
   // to draw the shadow tab when the tab is dragged over them
   public DocTabDragStartedEvent(DocTabDragParams params)
   {
      dragParams_ = params;
   }
   
   public DocTabDragParams getDragParams()
   {
      return dragParams_;
   }
   
   @Override
   public boolean forward() 
   {
      return false;
   }
   
   @Override
   protected void dispatch(DocTabDragStartedEvent.Handler handler)
   {
      handler.onDocTabDragStarted(this);
   }

   @Override
   public GwtEvent.Type<DocTabDragStartedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private DocTabDragParams dragParams_;
}
