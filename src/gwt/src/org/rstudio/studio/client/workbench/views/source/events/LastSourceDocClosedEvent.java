/*
 * LastSourceDocClosedEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.GwtEvent;

public class LastSourceDocClosedEvent extends GwtEvent<LastSourceDocClosedHandler>
{
   public static final Type<LastSourceDocClosedHandler> TYPE =
      new Type<LastSourceDocClosedHandler>();

   @Override
   protected void dispatch(LastSourceDocClosedHandler handler)
   {
      handler.onLastSourceDocClosed(this);
   }

   @Override
   public Type<LastSourceDocClosedHandler> getAssociatedType()
   {
      return TYPE;
   }
}