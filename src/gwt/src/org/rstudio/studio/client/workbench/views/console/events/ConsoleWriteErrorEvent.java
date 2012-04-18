/*
 * ConsoleWriteErrorEvent.java
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

public class ConsoleWriteErrorEvent extends GwtEvent<ConsoleWriteErrorHandler>
{
   public static final GwtEvent.Type<ConsoleWriteErrorHandler> TYPE =
      new GwtEvent.Type<ConsoleWriteErrorHandler>();
    
   public ConsoleWriteErrorEvent(String error)
   {
      error_ = error;
   }
   
   public String getError()
   {
      return error_;
   }
   
   @Override
   protected void dispatch(ConsoleWriteErrorHandler handler)
   {
      handler.onConsoleWriteError(this);
   }

   @Override
   public GwtEvent.Type<ConsoleWriteErrorHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String error_;
}
