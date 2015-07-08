/*
 * SourceDocAddedEvent.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SourceDocAddedEvent extends GwtEvent<SourceDocAddedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSourceDocAdded(SourceDocAddedEvent e);
   }
   
   public SourceDocAddedEvent(SourceDocument doc)
   {
      doc_ = doc;
   }

   public SourceDocument getDoc()
   {
      return doc_;
   }
  
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSourceDocAdded(this);
   }

   private final SourceDocument doc_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}