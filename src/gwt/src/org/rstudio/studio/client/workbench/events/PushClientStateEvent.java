/*
 * PushClientStateEvent.java
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
package org.rstudio.studio.client.workbench.events;

import com.google.gwt.event.shared.GwtEvent;

public class PushClientStateEvent extends GwtEvent<PushClientStateHandler>
{
   public static final GwtEvent.Type<PushClientStateHandler> TYPE =
      new GwtEvent.Type<PushClientStateHandler>();

   public PushClientStateEvent()
   {
   }

   @Override
   protected void dispatch(PushClientStateHandler handler)
   {
      handler.onPushClientState(this);
   }

   @Override
   public GwtEvent.Type<PushClientStateHandler> getAssociatedType()
   {
      return TYPE;
   }
}
