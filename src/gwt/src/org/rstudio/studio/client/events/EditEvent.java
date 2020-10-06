/*
 * EditEvent.java
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
package org.rstudio.studio.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EditEvent extends GwtEvent<EditEvent.Handler>
{
   public EditEvent(boolean before, int type)
   {
      before_ = before;
      type_ = type;
   }

   public boolean isBeforeEdit()
   {
      return before_;
   }

   public int getType()
   {
      return type_;
   }

   private final boolean before_;
   private final int type_;

   public static final int TYPE_NONE              = 0;
   public static final int TYPE_CUT               = 1;
   public static final int TYPE_COPY              = 2;
   public static final int TYPE_PASTE             = 4;
   public static final int TYPE_PASTE_WITH_INDENT = 8;

   // Boilerplate ----
   public interface Handler extends EventHandler
   {
      void onEdit(EditEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onEdit(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
