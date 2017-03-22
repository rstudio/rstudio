/*
 * ConsoleOutputEvent.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent.Handler;

public class ConsoleOutputEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onConsoleOutput(ConsoleOutputEvent event);
   }

   public interface HasHandlers extends com.google.gwt.event.shared.HasHandlers
   {
      HandlerRegistration addConsoleOutputHandler(Handler handler);
   }

   public ConsoleOutputEvent(String output)
   {
      output_ = output;
   }

   public String getOutput()
   {
      return output_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onConsoleOutput(this);
   }

   private final String output_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
