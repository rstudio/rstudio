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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
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
      void openTutorial(ShinyApplicationParams params);
   }
   
   @Inject
   protected TutorialPresenter(Display display,
                               EventBus events,
                               Commands commands,
                               Binder binder)
   {
      super(display);
      
      binder.bind(commands, this);
      
      display_ = display;
      events_ = events;
      commands_ = commands;
      
      events_.addHandler(ShinyApplicationStatusEvent.TYPE, this);
      events_.addHandler(TutorialCommandEvent.TYPE, this);
      events_.addHandler(InterruptStatusEvent.TYPE, this);
   }
   
   @Override
   public void onTutorialCommand(TutorialCommandEvent event)
   {
      String type = event.getType();
      if (StringUtil.equals(type, "stop"))
         handleTutorialStop();
      else
         assert false : "Unhandled tutorial event '" + type + "'";
   }
   
   @Override
   public void onShinyApplicationStatus(ShinyApplicationStatusEvent event)
   {
      if (event.getParams().getViewerType() == UserPrefs.SHINY_VIEWER_TYPE_TUTORIAL &&
          event.getParams().getState() == ShinyApplicationParams.STATE_STARTED)
      {
         display_.bringToFront();
         if (Desktop.hasDesktopFrame())
         {
            String url = event.getParams().getUrl();
            Desktop.getFrame().setTutorialUrl(url);
         }
         
         params_ = event.getParams();
         display_.openTutorial(params_);
      }
   }
   
   @Override
   public void onInterruptStatus(InterruptStatusEvent event)
   {
      // TODO Auto-generated method stub
   }
   
   private void handleTutorialStop()
   {
      display_.clear();
   }
   
   @Handler
   void onTutorialStop()
   {
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
   
   private final Display display_;
   private final EventBus events_;
   private final Commands commands_;
   
   private ShinyApplicationParams params_;
   
}
