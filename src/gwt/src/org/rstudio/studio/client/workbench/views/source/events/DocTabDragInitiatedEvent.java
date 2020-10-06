/*
 * DocTabDragInitiatedEvent.java
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

import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class DocTabDragInitiatedEvent 
             extends GwtEvent<DocTabDragInitiatedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onDocTabDragInitiated(DocTabDragInitiatedEvent event);
   }

   public static final GwtEvent.Type<DocTabDragInitiatedEvent.Handler> TYPE =
      new GwtEvent.Type<DocTabDragInitiatedEvent.Handler>();
   
   public DocTabDragInitiatedEvent(String docId, int width, int cursorOffset)
   {
      params_ = DocTabDragParams.create(docId, width, cursorOffset);
   }
   
   public DocTabDragParams getDragParams()
   {
      return params_;
   }
   
   @Override
   protected void dispatch(DocTabDragInitiatedEvent.Handler handler)
   {
      handler.onDocTabDragInitiated(this);
   }

   @Override
   public GwtEvent.Type<DocTabDragInitiatedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final DocTabDragParams params_;
}
