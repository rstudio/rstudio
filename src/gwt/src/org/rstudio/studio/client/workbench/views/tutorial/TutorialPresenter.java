/*
 * TutorialPresenter.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.tutorial;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialCommandEvent;

import com.google.inject.Inject;

public class TutorialPresenter
      extends
         BasePresenter
      implements
         TutorialCommandEvent.Handler,
         ShinyApplicationStatusEvent.Handler,
         InterruptStatusEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, TutorialPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void back();
      void forward();
      void clear();
      void popout();
      void refresh();
      
      void openTutorial(ShinyApplicationParams params);
      void showLandingPage();
   }
   
   @Inject
   protected TutorialPresenter(Display display,
                               EventBus events,
                               Commands commands,
                               Binder binder,
                               TutorialServerOperations server)
   {
      super(display);
      
      binder.bind(commands, this);
      
      display_ = display;
      events_ = events;
      commands_ = commands;
      server_ = server;
      
      events_.addHandler(ShinyApplicationStatusEvent.TYPE, this);
      events_.addHandler(TutorialCommandEvent.TYPE, this);
      events_.addHandler(InterruptStatusEvent.TYPE, this);
      
      manageCommands(false);
   }
   
   @Override
   public void onTutorialCommand(TutorialCommandEvent event)
   {
      String type = event.getType();
      assert false : "Unhandled tutorial event '" + type + "'";
   }
   
   @Override
   public void onShinyApplicationStatus(ShinyApplicationStatusEvent event)
   {
      // discard non-tutorial events
      String type = event.getParams().getViewerType();
      if (!type.startsWith(VIEWER_TYPE_TUTORIAL))
         return;
      
      String state = event.getParams().getState();
      if (StringUtil.equals(state, ShinyApplicationParams.STATE_STARTED))
      {
         display_.bringToFront();
         if (Desktop.hasDesktopFrame())
         {
            String url = event.getParams().getUrl();
            Desktop.getFrame().setTutorialUrl(url);
         }
         
         params_ = event.getParams();
         manageCommands(true);
         display_.openTutorial(params_);
      }
      else if (StringUtil.equals(state, ShinyApplicationParams.STATE_STOPPING))
      {
         Debug.logToRConsole("Tutorial: stopping");
      }
      else if (StringUtil.equals(state, ShinyApplicationParams.STATE_STOPPED))
      {
         Debug.logToRConsole("Tutorial: stopped");
      }
   }
   
   @Override
   public void onInterruptStatus(InterruptStatusEvent event)
   {
      // TODO Auto-generated method stub
   }
   
   private void onTutorialStopped()
   {
      manageCommands(false);
      display_.showLandingPage();
   }
   
   @Handler
   void onTutorialStop()
   {
      server_.tutorialStop(new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void response)
         {
            onTutorialStopped();
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   @Handler
   void onTutorialBack()
   {
      display_.back();
   }
   
   @Handler
   void onTutorialForward()
   {
      display_.forward();
   }
   
   @Handler
   void onTutorialPopout()
   {
      display_.popout();
   }
   
   @Handler
   void onTutorialRefresh()
   {
      display_.refresh();
   }
   
   private void manageCommands(boolean enabled)
   {
      commands_.tutorialBack().setEnabled(enabled);
      commands_.tutorialForward().setEnabled(enabled);
      commands_.tutorialZoom().setEnabled(enabled);
      
      commands_.tutorialPopout().setEnabled(enabled);
    
      commands_.tutorialRefresh().setEnabled(enabled);;
      
      commands_.tutorialStop().setEnabled(enabled);
      commands_.tutorialStop().setVisible(enabled);
   }
   
   
   private final Display display_;
   private final EventBus events_;
   private final Commands commands_;
   private final TutorialServerOperations server_;
   
   private ShinyApplicationParams params_;
   
   public static final String VIEWER_TYPE_TUTORIAL = "tutorial";
   
}
