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

import org.rstudio.studio.client.workbench.views.terminal.events.TerminalTitleEvent.Handler;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Send when xterm sees a standard title escape sequence. The string value
 * of the command is sent.
 * 
 * ESC]0;stringBEL -- Set window title to string, example:
 * 
 * echo -ne "\033]0;${USER}@${HOSTNAME}: ${PWD}\007"
 */
public class TerminalTitleEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onTerminalTitle(TerminalTitleEvent event);
   }
   
   public interface HasHandlers extends com.google.gwt.event.shared.HasHandlers
   {
      HandlerRegistration addTerminalTitleHandler(Handler handler);
   }
   
   public TerminalTitleEvent(String title)
   {
      title_ = title;
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
   
   public String getTitle()
   {
      return title_;
   }
  
   private String title_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}
