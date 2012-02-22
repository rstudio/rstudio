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
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.layout.DelayFadeInHelper;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptHandler;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleHandler;

public class Console
{
   interface Binder extends CommandBinder<Commands, Console> {}

   public interface Display
   {
      void bringToFront();
      void focus();
      IsWidget getConsoleInterruptButton();
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

      fadeInHelper_ = new DelayFadeInHelper(
            view_.getConsoleInterruptButton().asWidget());
      events.addHandler(BusyEvent.TYPE, new BusyHandler()
      {
         @Override
         public void onBusy(BusyEvent event)
         {
            if (event.isBusy())
               fadeInHelper_.beginShow();
            else
               fadeInHelper_.hide();
         }
      });

      events.addHandler(ConsolePromptEvent.TYPE, new ConsolePromptHandler()
      {
         @Override
         public void onConsolePrompt(ConsolePromptEvent event)
         {
            fadeInHelper_.hide();
         }
      });
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

   private final DelayFadeInHelper fadeInHelper_;
   private final Display view_;
}
