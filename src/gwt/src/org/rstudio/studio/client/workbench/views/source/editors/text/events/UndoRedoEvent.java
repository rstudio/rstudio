/*
 * UndoRedoEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.event.shared.GwtEvent;

public class UndoRedoEvent extends GwtEvent<UndoRedoHandler>
{
   public UndoRedoEvent(boolean redo)
   {
      redo_ = redo;
   }

   public boolean isRedo()
   {
      return redo_;
   }

   @Override
   public Type<UndoRedoHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(UndoRedoHandler handler)
   {
      handler.onUndoRedo(this);
   }

   public static Type<UndoRedoHandler> TYPE = new Type<UndoRedoHandler>();
   private final boolean redo_;
}
