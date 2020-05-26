/*
 * CollabEditStartedEvent.java
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CollabEditStartedEvent extends GwtEvent<CollabEditStartedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onCollabEditStarted(CollabEditStartedEvent event);
   }

   public static final GwtEvent.Type<CollabEditStartedEvent.Handler> TYPE =
      new GwtEvent.Type<CollabEditStartedEvent.Handler>();
   
   public CollabEditStartedEvent(CollabEditStartParams params)
   {
      params_ = params;
   }
   
   public CollabEditStartParams getStartParams()
   {
      return params_;
   }
   
   @Override
   protected void dispatch(CollabEditStartedEvent.Handler handler)
   {
      handler.onCollabEditStarted(this);
   }

   @Override
   public GwtEvent.Type<CollabEditStartedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final CollabEditStartParams params_;
}
