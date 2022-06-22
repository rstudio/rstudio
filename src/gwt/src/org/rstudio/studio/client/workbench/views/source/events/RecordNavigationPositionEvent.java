/*
 * RecordNavigationPositionEvent.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.event.shared.GwtEvent;

public class RecordNavigationPositionEvent extends GwtEvent<RecordNavigationPositionEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onRecordNavigationPosition(RecordNavigationPositionEvent event);
   }

   public RecordNavigationPositionEvent(SourcePosition position)
   {
      position_ = position;
   }

   public SourcePosition getPosition()
   {
      return position_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRecordNavigationPosition(this);
   }

   private final SourcePosition position_;
}
