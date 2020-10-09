/*
 * ConsoleWriteInputEvent.java
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
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleText;

import com.google.gwt.event.shared.GwtEvent;

public class ConsoleWriteInputEvent extends GwtEvent<ConsoleWriteInputEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();
   
   public interface Handler extends EventHandler
   {
      void onConsoleWriteInput(ConsoleWriteInputEvent event);
   }

   public ConsoleWriteInputEvent(ConsoleText text)
   {
      text_ = text;
   }

   public String getInput()
   {
      return text_.text;
   }
   
   public String getConsole()
   {
      return text_.console;
   }

   private final ConsoleText text_;

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onConsoleWriteInput(this);
   }
}
