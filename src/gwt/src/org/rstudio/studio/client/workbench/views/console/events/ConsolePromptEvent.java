/*
 * ConsolePromptEvent.java
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

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.console.model.ConsolePrompt;

public class ConsolePromptEvent extends GwtEvent<ConsolePromptHandler>
{
   public static final GwtEvent.Type<ConsolePromptHandler> TYPE =
      new GwtEvent.Type<ConsolePromptHandler>();
    
   public ConsolePromptEvent(ConsolePrompt prompt)
   {
      prompt_ = prompt;
   }
   
   public ConsolePrompt getPrompt() 
   {
      return prompt_;
   }
   
   @Override
   protected void dispatch(ConsolePromptHandler handler)
   {
      handler.onConsolePrompt(this);
   }

   @Override
   public GwtEvent.Type<ConsolePromptHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final ConsolePrompt prompt_;
}

