/*
 * ViewerClearedEvent.java
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
package org.rstudio.studio.client.workbench.views.viewer.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ViewerClearedEvent extends GwtEvent<ViewerClearedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onViewerCleared(ViewerClearedEvent event);
   }

   public ViewerClearedEvent(boolean forStop)
   {
      forStop_ = forStop;
   }

   public boolean isForStop()
   {
      return forStop_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onViewerCleared(this);
   }

   private final boolean forStop_;

   public static final Type<Handler> TYPE = new Type<>();
}
