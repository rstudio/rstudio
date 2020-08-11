/*
 * ConsolePromptEvent.java
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.common.console.ConsolePromptEvent.Handler;

public class ConsolePromptEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onConsolePrompt(ConsolePromptEvent event);
   }

   public interface HasHandlers extends com.google.gwt.event.shared.HasHandlers
   {
      HandlerRegistration addConsolePromptHandler(Handler handler);
   }

   public ConsolePromptEvent(String prompt)
   {
      prompt_ = prompt;
   }

   public String getPrompt()
   {
      return prompt_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onConsolePrompt(this);
   }

   private final String prompt_;

   public static final Type<Handler> TYPE = new Type<>();
}
