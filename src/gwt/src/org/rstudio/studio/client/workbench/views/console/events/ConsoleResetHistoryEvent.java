/*
 * ConsoleResetHistoryEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.console.events;

import org.rstudio.studio.client.workbench.views.console.model.ConsoleResetHistory;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.GwtEvent;

public class ConsoleResetHistoryEvent extends GwtEvent<ConsoleResetHistoryHandler>
{
   public static final GwtEvent.Type<ConsoleResetHistoryHandler> TYPE =
      new GwtEvent.Type<ConsoleResetHistoryHandler>();
    
   public ConsoleResetHistoryEvent(ConsoleResetHistory reset)
   {
      reset_ = reset;
   }
   
   public JsArrayString getHistory()
   {
      return reset_.getHistory();
   }
   
   public boolean getPreserveUIContext()
   {
      return reset_.getPreserveUIContext();
   }
   
   @Override
   protected void dispatch(ConsoleResetHistoryHandler handler)
   {
      handler.onConsoleResetHistory(this);
   }

   @Override
   public GwtEvent.Type<ConsoleResetHistoryHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final ConsoleResetHistory reset_;
}
