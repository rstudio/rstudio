/*
 * Console.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.layout.DelayFadeInHelper;
import org.rstudio.core.client.widget.FocusContext;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.events.ZoomPaneEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleActivateEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptHandler;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleHandler;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.RprofEvent;

public class Console
{
   interface Binder extends CommandBinder<Commands, Console> {}

   public interface Display
   {
      void bringToFront();
      void focus();
      void ensureCursorVisible();
      IsWidget getConsoleInterruptButton();
      IsWidget getConsoleClearButton();
      IsWidget getProfilerInterruptButton();
      void setDebugMode(boolean debugMode);
      void setProfilerMode(boolean profilerMode);
   }
   
   @Inject
   public Console(final Display view, EventBus events, Commands commands)
   {    
      view_ = view;
      events_ = events;

      events.addHandler(SendToConsoleEvent.TYPE, new SendToConsoleHandler()
      {
         public void onSendToConsole(SendToConsoleEvent event)
         {
            if (event.shouldRaise())
               view.bringToFront();
         }
      });

      ((Binder) GWT.create(Binder.class)).bind(commands, this);

      interruptFadeInHelper_ = new DelayFadeInHelper(
            view_.getConsoleInterruptButton().asWidget());
      events.addHandler(BusyEvent.TYPE, new BusyHandler()
      {
         @Override
         public void onBusy(BusyEvent event)
         {
            if (event.isBusy())
            {
               interruptFadeInHelper_.beginShow();
            }
         }
      });
      
      profilerFadeInHelper_ = new DelayFadeInHelper(
            view_.getProfilerInterruptButton().asWidget());
      events.addHandler(RprofEvent.TYPE, new RprofEvent.Handler()
      {
         @Override
         public void onRprofEvent(RprofEvent event)
         {
            switch (event.getEventType())
            {
               case START:
                  view.setProfilerMode(true);
                  profilerFadeInHelper_.beginShow();
                  break;
               case STOP:
                  view.setProfilerMode(false);
                  profilerFadeInHelper_.hide();
                  break;
               default:
                  break;
            }
         }
      });

      events.addHandler(ConsolePromptEvent.TYPE, new ConsolePromptHandler()
      {
         @Override
         public void onConsolePrompt(ConsolePromptEvent event)
         {
            interruptFadeInHelper_.hide();
         }
      });
      
      events.addHandler(DebugModeChangedEvent.TYPE, 
            new DebugModeChangedEvent.Handler()
      { 
         @Override
         public void onDebugModeChanged(DebugModeChangedEvent event)
         {
            view.setDebugMode(event.debugging());
         }
      });
      
      events.addHandler(ConsoleActivateEvent.TYPE, 
                        new ConsoleActivateEvent.Handler()
      {
         @Override
         public void onConsoleActivate(ConsoleActivateEvent event)
         {
            activateConsole(event.getFocusWindow());
         }
      });
   }
   
   @Handler
   void onActivateConsole()
   {
      activateConsole(true);
   }

   private void activateConsole(boolean focusWindow)
   {
      // ensure we don't leave focus in the console
      final FocusContext focusContext = new FocusContext();
      if (!focusWindow)
         focusContext.record();
      
      if (focusWindow)
         WindowEx.get().focus();
      
      view_.bringToFront();
      view_.focus();
      view_.ensureCursorVisible();
      
      // the above code seems to always leave focus in the console
      // (haven't been able to sort out why). this ensure it's restored
      // if that's what the caller requested.
      if (!focusWindow) 
      {
         new Timer() {
   
            @Override
            public void run()
            {
               focusContext.restore(); 
            }
         }.schedule(100);
      }    
   }
   
   @Handler
   public void onLayoutZoomConsole()
   {
      onActivateConsole();
      events_.fireEvent(new ZoomPaneEvent("Console"));
   }
   
   public Display getDisplay()
   {
      return view_ ;
   }

   private final DelayFadeInHelper interruptFadeInHelper_;
   private final DelayFadeInHelper profilerFadeInHelper_;
   private final EventBus events_;
   private final Display view_;
}
