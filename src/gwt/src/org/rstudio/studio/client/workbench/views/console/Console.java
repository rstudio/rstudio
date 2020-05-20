/*
 * Console.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import org.rstudio.studio.client.workbench.events.ZoomPaneEvent;
import org.rstudio.studio.client.workbench.views.console.ConsolePane.ConsoleMode;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleActivateEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobProgressEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.LocalJobProgress;
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
      void enterMode(ConsolePane.ConsoleMode mode);
      void leaveMode(ConsolePane.ConsoleMode mode);
      ConsolePane.ConsoleMode mode();
      void showProgress(LocalJobProgress progress);
   }

   @Inject
   public Console(final Display view, EventBus events, Commands commands)
   {
      view_ = view;
      events_ = events;

      events.addHandler(SendToConsoleEvent.TYPE, event ->
      {
         if (event.shouldRaise())
            view.bringToFront();
      });

      ((Binder) GWT.create(Binder.class)).bind(commands, this);

      interruptFadeInHelper_ = new DelayFadeInHelper(
            view_.getConsoleInterruptButton().asWidget());
      events.addHandler(BusyEvent.TYPE, event ->
      {
         if (event.isBusy())
         {
            interruptFadeInHelper_.beginShow();
         }
      });

      profilerFadeInHelper_ = new DelayFadeInHelper(
            view_.getProfilerInterruptButton().asWidget());
      events.addHandler(RprofEvent.TYPE, event ->
      {
         switch (event.getEventType())
         {
            case START:
               view.enterMode(ConsoleMode.Profiler);
               profilerFadeInHelper_.beginShow();
               break;
            case STOP:
               view.leaveMode(ConsoleMode.Profiler);
               profilerFadeInHelper_.hide();
               break;
            default:
               break;
         }
      });

      events.addHandler(ConsolePromptEvent.TYPE, event -> interruptFadeInHelper_.hide());

      events.addHandler(DebugModeChangedEvent.TYPE, event ->
      {
         if (event.debugging())
            view.enterMode(ConsoleMode.Debug);
         else
            view.leaveMode(ConsoleMode.Debug);
      });

      events.addHandler(ConsoleActivateEvent.TYPE, event -> activateConsole(event.getFocusWindow()));

      events.addHandler(JobProgressEvent.TYPE, (JobProgressEvent event) ->
      {
         if (event.hasProgress())
         {
            view.enterMode(ConsoleMode.Job);
            view.showProgress(event.progress());
         }
         else
            view.leaveMode(ConsoleMode.Job);
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
      return view_;
   }

   private final DelayFadeInHelper interruptFadeInHelper_;
   private final DelayFadeInHelper profilerFadeInHelper_;
   private final EventBus events_;
   private final Display view_;
}
