/*
 * ConsoleProcessCreatedEvent.java
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
package org.rstudio.studio.client.common.console;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.common.console.ConsoleProcessCreatedEvent.Handler;

public class ConsoleProcessCreatedEvent extends GwtEvent<Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final ConsoleProcessInfo getProcessInfo() /*-{
         return this.process_info;
         }-*/;

      public native final String getTargetWindow() /*-{
         return this.target_window;
      }-*/;

   }

   public interface Handler extends EventHandler
   {
      void onConsoleProcessCreated(ConsoleProcessCreatedEvent event);
   }

   public ConsoleProcessCreatedEvent(Data data)
   {
      processInfo_ = data.getProcessInfo();
      targetWindow_ = data.getTargetWindow();
   }

   public ConsoleProcessInfo getProcessInfo()
   {
      return processInfo_;
   }

   public String getTargetWindow()
   {
      return targetWindow_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onConsoleProcessCreated(this);
   }

   private final ConsoleProcessInfo processInfo_;
   private final String targetWindow_;

   public static final Type<Handler> TYPE = new Type<>();
}
