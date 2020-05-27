/*
 * InsertSourceEvent.java
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

import com.google.gwt.event.shared.GwtEvent;

public class InsertSourceEvent extends GwtEvent<InsertSourceHandler>
{
   public static final Type<InsertSourceHandler> TYPE =
         new Type<InsertSourceHandler>();

   public InsertSourceEvent(String source, boolean block)
   {

      source_ = source;
      block_ = block;
   }

   public String getCode()
   {
      return source_;
   }

   public boolean isBlock()
   {
      return block_;
   }

   @Override
   public Type<InsertSourceHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(InsertSourceHandler handler)
   {
      handler.onInsertSource(this);
   }

   private final String source_;
   private final boolean block_;
}
