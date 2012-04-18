/*
 * TabClosedEvent.java
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

public class TabClosedEvent extends GwtEvent<TabClosedHandler>
{
   public static final Type<TabClosedHandler> TYPE = new Type<TabClosedHandler>();

   public TabClosedEvent(int tabIndex)
   {
      tabIndex_ = tabIndex;
   }

   public int getTabIndex()
   {
      return tabIndex_;
   }

   @Override
   public Type<TabClosedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(TabClosedHandler handler)
   {
      handler.onTabClosed(this);
   }

   private int tabIndex_;
}
