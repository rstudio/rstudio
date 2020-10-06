/*
 * LastSourceDocClosedEvent.java
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

public class LastSourceDocClosedEvent extends GwtEvent<LastSourceDocClosedHandler>
{
   public static final Type<LastSourceDocClosedHandler> TYPE =
      new Type<LastSourceDocClosedHandler>();

   public LastSourceDocClosedEvent() {}

   public LastSourceDocClosedEvent(String name)
   {
      name_ = name;
   }

   public String getName()
   {
      return name_;
   }

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

   String name_;
}
