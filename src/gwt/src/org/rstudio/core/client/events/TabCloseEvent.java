/*
 * TabCloseEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Indicates that the specified tab is about to close. Happens after
 * TabClosingEvent but before TabClosedEvent.
 */
public class TabCloseEvent extends GwtEvent<TabCloseEvent.Handler>
{
   public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

   public TabCloseEvent(int tabIndex)
   {
      tabIndex_ = tabIndex;
   }

   public int getTabIndex()
   {
      return tabIndex_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTabClose(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   public interface Handler extends EventHandler
   {
      void onTabClose(TabCloseEvent event);
   }

   private final int tabIndex_;
}
