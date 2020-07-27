/*
 * RmdRenderCompletedEvent.java
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

package org.rstudio.studio.client.rmarkdown.events;

import org.rstudio.studio.client.rmarkdown.model.RmdRenderResult;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RmdRenderCompletedEvent extends GwtEvent<RmdRenderCompletedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onRmdRenderCompleted(RmdRenderCompletedEvent event);
   }

   public RmdRenderCompletedEvent(RmdRenderResult result)
   {
      result_ = result;
   }

   public RmdRenderResult getResult()
   {
      return result_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRmdRenderCompleted(this);
   }

   private final RmdRenderResult result_;

   public static final Type<Handler> TYPE = new Type<>();
}
