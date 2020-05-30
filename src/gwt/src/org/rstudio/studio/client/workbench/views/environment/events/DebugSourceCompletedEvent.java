/*
 * DebugSourceCompletedEvent.java
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
package org.rstudio.studio.client.workbench.views.environment.events;

import org.rstudio.studio.client.workbench.views.environment.model.DebugSourceResult;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class DebugSourceCompletedEvent 
                        extends GwtEvent<DebugSourceCompletedEvent.Handler>
{

   public interface Handler extends EventHandler
   {
      void onDebugSourceCompleted(DebugSourceCompletedEvent event);
   }

   public static final GwtEvent.Type<DebugSourceCompletedEvent.Handler> TYPE =
      new GwtEvent.Type<DebugSourceCompletedEvent.Handler>();
   
   public DebugSourceCompletedEvent(DebugSourceResult result)
   {
      result_ = result;
   }
   
   public String getPath()
   {
      return result_.getPath();
   }
   
   public boolean getSucceeded()
   {
      return result_.getSucceeded();
   }
   
   @Override
   protected void dispatch(DebugSourceCompletedEvent.Handler handler)
   {
      handler.onDebugSourceCompleted(this);
   }

   @Override
   public GwtEvent.Type<DebugSourceCompletedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private DebugSourceResult result_;
}
