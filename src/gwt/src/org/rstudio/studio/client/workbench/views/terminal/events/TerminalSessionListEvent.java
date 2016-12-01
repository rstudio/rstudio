/*
 * TerminalSessionListEvent.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import java.util.ArrayList;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionListEvent.Handler;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class TerminalSessionListEvent extends CrossWindowEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      /**
       * Sent when a list of terminal processes has been received from the
       * server.
       * @param event event containing a list of terminal processes
       */
      void onTerminalSessionList(TerminalSessionListEvent event);
   }
   
   public TerminalSessionListEvent()
   {
   }
   
   public TerminalSessionListEvent(ArrayList<ConsoleProcess> terminalList)
   {
      terminalList_ = terminalList;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalSessionList(this);
   }
   
   public ArrayList<ConsoleProcess> getTerminalList()
   {
      return terminalList_;
   }
  
   private ArrayList<ConsoleProcess> terminalList_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}
