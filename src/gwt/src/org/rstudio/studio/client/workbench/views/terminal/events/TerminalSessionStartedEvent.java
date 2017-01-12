/*
 * TerminalSessionStartedEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.views.terminal.TerminalSession;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent.Handler;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class TerminalSessionStartedEvent extends CrossWindowEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      /**
       * Sent when a terminal session has connected to a server process
       * @param event contains terminal session that has connected
       */
      void onTerminalSessionStarted(TerminalSessionStartedEvent event);
   }
   
   public TerminalSessionStartedEvent()
   {
   }
   
   public TerminalSessionStartedEvent(TerminalSession terminalWidget)
   {
      terminalWidget_ = terminalWidget;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalSessionStarted(this);
   }
   
   public TerminalSession getTerminalWidget()
   {
      return terminalWidget_;
   }
  
   private TerminalSession terminalWidget_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}
