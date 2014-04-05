/*
 * ConsoleActivateEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
import com.google.gwt.event.shared.GwtEvent;

public class ConsoleActivateEvent 
   extends GwtEvent<ConsoleActivateEvent.Handler>
{
   public static final GwtEvent.Type<ConsoleActivateEvent.Handler> TYPE =
      new GwtEvent.Type<ConsoleActivateEvent.Handler>();
  
   public interface Handler extends EventHandler
   {
      void onConsoleActivate(ConsoleActivateEvent event);
   }
   
   public ConsoleActivateEvent(boolean focusWindow)
   {
      focusWindow_ = focusWindow;
   }
   
   public boolean getFocusWindow()
   {
      return focusWindow_;
   }
         
   @Override
   public Type<ConsoleActivateEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onConsoleActivate(this);
   }
   
   private final boolean focusWindow_;
}
