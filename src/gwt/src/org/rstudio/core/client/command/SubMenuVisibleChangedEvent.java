/*
 * SubMenuVisibleChangedEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.command;

import com.google.gwt.event.shared.GwtEvent;

public class SubMenuVisibleChangedEvent extends GwtEvent<SubMenuVisibleChangedHandler>
{
   public static final Type<SubMenuVisibleChangedHandler> TYPE = new Type<>();
   @Override
   public Type<SubMenuVisibleChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   public SubMenuVisibleChangedEvent(boolean visible)
   {
      visible_ = visible;
   }

   public boolean isVisible()
   {
      return visible_;
   }

   @Override
   protected void dispatch(SubMenuVisibleChangedHandler handler)
   {
      handler.onSubMenuVisibleChanged(this);
   }

   private final boolean visible_;
}
