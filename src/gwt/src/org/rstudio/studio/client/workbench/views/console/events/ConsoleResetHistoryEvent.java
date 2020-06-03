/*
 * ConsoleResetHistoryEvent.java
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
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleResetHistory;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.GwtEvent;

public class ConsoleResetHistoryEvent extends GwtEvent<ConsoleResetHistoryEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onConsoleResetHistory(ConsoleResetHistoryEvent event);
   }

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
   protected void dispatch(Handler handler)
   {
      handler.onConsoleResetHistory(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final ConsoleResetHistory reset_;
}
