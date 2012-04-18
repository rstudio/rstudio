/*
 * TabClosingEvent.java
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

public class TabClosingEvent extends GwtEvent<TabClosingHandler>
{
   public static final GwtEvent.Type<TabClosingHandler> TYPE =
      new GwtEvent.Type<TabClosingHandler>();

   public TabClosingEvent(int tabIndex)
   {
      tabIndex_ = tabIndex;
   }

   public int getTabIndex()
   {
      return tabIndex_;
   }

   public boolean isCancelled()
   {
      return cancelled_;
   }

   public void cancel()
   {
      cancelled_ = true;
   }

   @Override
   protected void dispatch(TabClosingHandler handler)
   {
      handler.onTabClosing(this);
   }

   @Override
   public GwtEvent.Type<TabClosingHandler> getAssociatedType()
   {
      return TYPE;
   }

   private final int tabIndex_;
   private boolean cancelled_;
}
