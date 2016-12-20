/*
 * TerminalBusyEvent.java
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
package org.rstudio.studio.client.workbench.views.terminal.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Sent to indicate change in state of a terminal's child processes.
 */
public class TerminalBusyEvent extends GwtEvent<TerminalBusyEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      /**
       * @return Terminal handle
       */
      public final native String getHandle() /*-{
         return this.handle;
      }-*/;

      /**
       * @return true if terminal shell has a child process
       */
      public final native boolean getHasChildren() /*-{
         return this.children;
      }-*/;

      /**
       * @return true if a full screen (ncurses-style) process is running
       */
      public final native boolean getHasFullScreen() /*-{
         return this.fullscreen;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onTerminalBusy(TerminalBusyEvent event);
   }

   public TerminalBusyEvent(Data data)
   {
      data_ = data;
   }
   
   public String getHandle()
   {
      return data_.getHandle();
   }

   public boolean getHasChildren()
   {
      return data_.getHasChildren();
   }

   public boolean getFullScreen()
   {
      return getFullScreen();
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalBusy(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
