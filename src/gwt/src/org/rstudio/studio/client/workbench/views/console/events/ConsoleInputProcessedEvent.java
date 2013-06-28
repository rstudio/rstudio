/*
 * ConsoleInputProcessedEvent.java
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

public class ConsoleInputProcessedEvent 
   extends GwtEvent<ConsoleInputProcessedHandler>
{
   public static final GwtEvent.Type<ConsoleInputProcessedHandler> TYPE =
      new GwtEvent.Type<ConsoleInputProcessedHandler>();
    
   public ConsoleInputProcessedEvent(String input)
   {
      input_ = input;
   }
   
   public String getInput()
   {
      return input_;
   }
   
   @Override
   protected void dispatch(ConsoleInputProcessedHandler handler)
   {
      handler.onConsoleInputProcessed(this);
   }

   @Override
   public GwtEvent.Type<ConsoleInputProcessedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String input_;
}
