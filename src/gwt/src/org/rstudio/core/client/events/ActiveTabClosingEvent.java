/*
 * ActiveTabClosingEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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

public class ActiveTabClosingEvent 
                           extends GwtEvent<ActiveTabClosingHandler>
{ 
   public static final GwtEvent.Type<ActiveTabClosingHandler> TYPE =
      new GwtEvent.Type<ActiveTabClosingHandler>();

   public ActiveTabClosingEvent(int tabIndex)
   {
      tabIndex_ = tabIndex;
   }

   public int getTabIndex()
   {
      return tabIndex_;
   }

   public int getNextTabIndex()
   {
      return nextTabIndex_;
   }

   public void setNextTabIndex(int nextTabIndex)
   {
      nextTabIndex_ = nextTabIndex;
   }

   @Override
   protected void dispatch(ActiveTabClosingHandler handler)
   {
      handler.onActiveTabClosing(this);
   }

   @Override
   public GwtEvent.Type<ActiveTabClosingHandler> getAssociatedType()
   {
      return TYPE;
   }

   private final int tabIndex_;
   private int nextTabIndex_ = -1;
}
