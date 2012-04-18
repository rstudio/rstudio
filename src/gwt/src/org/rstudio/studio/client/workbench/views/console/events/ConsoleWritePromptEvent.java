/*
 * ConsoleWritePromptEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.event.shared.GwtEvent;

public class ConsoleWritePromptEvent extends GwtEvent<ConsoleWritePromptHandler>
{
   public static final Type<ConsoleWritePromptHandler> TYPE = new Type<ConsoleWritePromptHandler>();

   public ConsoleWritePromptEvent(String prompt)
   {
      prompt_ = prompt;
   }

   public String getPrompt()
   {
      return prompt_;
   }

   private String prompt_;

   @Override
   public Type<ConsoleWritePromptHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(ConsoleWritePromptHandler handler)
   {
      handler.onConsoleWritePrompt(this);
   }
}
