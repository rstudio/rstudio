/*
 * ConsoleWritePromptEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleText;

public class ConsoleWritePromptEvent extends GwtEvent<ConsoleWritePromptEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onConsoleWritePrompt(ConsoleWritePromptEvent event);
   }

   public ConsoleWritePromptEvent(ConsoleText text)
   {
      text_ = text;
   }

   public String getPrompt()
   {
      return text_.text;
   }

   public boolean isAgent()
   {
      return text_.agent;
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
      handler.onConsoleWritePrompt(this);
   }
}
