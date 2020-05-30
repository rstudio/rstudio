/*
 * TerminalReceivedConsoleProcessInfo.java
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
package org.rstudio.studio.client.workbench.views.terminal.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;

/**
 * Fired by TerminalSession connect sequence when ConsoleProcessInfo is received
 */
public class TerminalReceivedConsoleProcessInfoEvent extends GwtEvent<TerminalReceivedConsoleProcessInfoEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      /**
       * Sent when TerminalSession connect sequence receives ConsoleProcessInfo
       */
      void onTerminalReceivedConsoleProcessInfo(TerminalReceivedConsoleProcessInfoEvent event);
   }

   public TerminalReceivedConsoleProcessInfoEvent(ConsoleProcessInfo data)
   {
      data_ = data;
   }

   public ConsoleProcessInfo getData()
   {
      return data_;
   }

   @Override
   public Type<TerminalReceivedConsoleProcessInfoEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalReceivedConsoleProcessInfo(this);
   }

   private ConsoleProcessInfo data_;
   public static final Type<TerminalReceivedConsoleProcessInfoEvent.Handler> TYPE =
         new Type<TerminalReceivedConsoleProcessInfoEvent.Handler>();
}
