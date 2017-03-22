/*
 * XTermTitleEvent.java
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

import org.rstudio.studio.client.workbench.views.terminal.events.XTermTitleEvent.Handler;

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
public class XTermTitleEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      /**
       * Sent by xterm.js when it has detected escape sequence for setting 
       * terminal title.
       * @param event title string extracted from escape sequence
       */
      void onXTermTitle(XTermTitleEvent event);
   }
   
   public interface HasHandlers extends com.google.gwt.event.shared.HasHandlers
   {
      HandlerRegistration addXTermTitleHandler(Handler handler);
   }
   
   public XTermTitleEvent(String title)
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
      handler.onXTermTitle(this);
   }
 
   public String getTitle()
   {
      return title_;
   }
  
   private final String title_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}
