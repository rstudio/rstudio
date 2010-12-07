/*
 * Console.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleHandler;

public class Console
{
   interface Binder extends CommandBinder<Commands, Console> {}

   public interface Display
   {
      void bringToFront();
      void focus();
   }
   
   @Inject
   public Console(final Display view, EventBus events, Commands commands)
   {    
      view_ = view;

      events.addHandler(SendToConsoleEvent.TYPE, new SendToConsoleHandler()
      {
         public void onSendToConsole(SendToConsoleEvent event)
         {
            view.bringToFront();
         }
      });

      ((Binder) GWT.create(Binder.class)).bind(commands, this);
   }

   @Handler
   void onActivateConsole()
   {
      view_.bringToFront();
      view_.focus();
   }
   
   public Display getDisplay()
   {
      return view_ ;
   }
   
   private final Display view_;
}
