/*
 * ConsoleWriteOutputEvent.java
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

public class ConsoleWriteOutputEvent extends GwtEvent<ConsoleWriteOutputHandler>
{
   public static final GwtEvent.Type<ConsoleWriteOutputHandler> TYPE =
      new GwtEvent.Type<ConsoleWriteOutputHandler>();
    
   public ConsoleWriteOutputEvent(String output)
   {
      output_ = output;
   }
   
   public String getOutput()
   {
      return output_;
   }
   
   @Override
   protected void dispatch(ConsoleWriteOutputHandler handler)
   {
      handler.onConsoleWriteOutput(this);
   }

   @Override
   public GwtEvent.Type<ConsoleWriteOutputHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String output_;
}

