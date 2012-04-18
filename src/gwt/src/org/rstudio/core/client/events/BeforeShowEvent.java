/*
 * BeforeShowEvent.java
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

public class BeforeShowEvent extends GwtEvent<BeforeShowHandler>
{
   public BeforeShowEvent()
   {
   }

   @Override
   public Type<BeforeShowHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(BeforeShowHandler handler)
   {
      handler.onBeforeShow(this);
   }

   public static final Type<BeforeShowHandler> TYPE = new Type<BeforeShowHandler>();
}
