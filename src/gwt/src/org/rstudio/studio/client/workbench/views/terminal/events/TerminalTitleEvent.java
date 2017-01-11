/*
 * TerminalTitleEvent.java
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

import org.rstudio.studio.client.workbench.views.terminal.TerminalSession;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalTitleEvent.Handler;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Send by an XTermSession that has processed an xterm.js title-change event 
 * and updated its title property.
 */
public class TerminalTitleEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      /**
       * Sent by a terminal session that has changed its caption
       * @param event terminal session that has a changed caption
       */
      void onTerminalTitle(TerminalTitleEvent event);
   }
   
   public TerminalTitleEvent(TerminalSession terminalSession)
   {
      terminalSession_ = terminalSession;
   }

   @Override
   public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalTitle(this);
   }
   
   public TerminalSession getTerminalSession()
   {
      return terminalSession_;
   }
  
   private TerminalSession terminalSession_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}