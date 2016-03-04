/*
 * ConsoleWriteInputEvent.java
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
package org.rstudio.studio.client.workbench.views.console.events;

import org.rstudio.studio.client.workbench.views.console.model.ConsoleText;

import com.google.gwt.event.shared.GwtEvent;

public class ConsoleWriteInputEvent extends GwtEvent<ConsoleWriteInputHandler>
{
   public static final Type<ConsoleWriteInputHandler> TYPE = new Type<ConsoleWriteInputHandler>();

   public ConsoleWriteInputEvent(ConsoleText text)
   {
      text_ = text;
   }

   public String getInput()
   {
      return text_.getText();
   }
   
   public String getConsole()
   {
      return text_.getConsole();
   }

   private final ConsoleText text_;

   @Override
   public Type<ConsoleWriteInputHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(ConsoleWriteInputHandler handler)
   {
      handler.onConsoleWriteInput(this);
   }
}
