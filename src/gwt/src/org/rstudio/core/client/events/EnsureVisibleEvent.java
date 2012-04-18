/*
 * EnsureVisibleEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class EnsureVisibleEvent extends GwtEvent<EnsureVisibleHandler>
{
   public static final Type<EnsureVisibleHandler> TYPE
         = new Type<EnsureVisibleHandler>();

   public EnsureVisibleEvent()
   {
      this(true);
   }

   public EnsureVisibleEvent(boolean activate)
   {
      activate_ = activate;
   }

   public boolean getActivate()
   {
      return activate_;
   }

   @Override
   public Type<EnsureVisibleHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(EnsureVisibleHandler handler)
   {
      handler.onEnsureVisible(this);
   }

   private final boolean activate_;
}
