/*
 * TerminalDataEvent.java
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

import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent.Handler;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

public class TerminalDataInputEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      /**
       * Sent when user has typed in the terminal
       * @param event contains string data typed by the user
       */
      void onTerminalDataInput(TerminalDataInputEvent event);
   }
   
   public interface HasHandlers extends com.google.gwt.event.shared.HasHandlers
   {
      HandlerRegistration addTerminalDataInputHandler(Handler handler);
   }
   
   public TerminalDataInputEvent(String data)
   {
      data_ = data;
   }

   @Override
   public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalDataInput(this);
   }
   
   public String getData()
   {
      return data_;
   }
  
   private final String data_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}