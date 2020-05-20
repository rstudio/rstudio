/*
 * AddTerminalEvent.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.views.terminal.events.AddTerminalEvent.Handler;

public class AddTerminalEvent extends GwtEvent<Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final ConsoleProcessInfo getProcessInfo() /*-{ 
         return this.process_info; 
      }-*/;

      public native final boolean getShow() /*-{ 
         return this.show; 
      }-*/;
   }
   
   public interface Handler extends EventHandler
   {
      void onAddTerminal(AddTerminalEvent event);
   }

   public AddTerminalEvent(Data data)
   {
      processInfo_ = data.getProcessInfo();
      show_ = data.getShow();
   }

   public ConsoleProcessInfo getProcessInfo()
   {
      return processInfo_;
   }
   
   public boolean getShow()
   {
      return show_;
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAddTerminal(this);
   }

   private final ConsoleProcessInfo processInfo_;
   private final boolean show_;

   public static final Type<Handler> TYPE = new Type<>();
}
