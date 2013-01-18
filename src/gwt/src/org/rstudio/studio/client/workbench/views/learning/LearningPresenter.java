/*
 * LearningPresenter.java
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
package org.rstudio.studio.client.workbench.views.learning;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ReloadEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;

import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;

import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.learning.events.ShowLearningPaneEvent;
import org.rstudio.studio.client.workbench.views.learning.model.LearningServerOperations;
import org.rstudio.studio.client.workbench.views.learning.model.LearningState;

public class LearningPresenter extends BasePresenter 
{
   public interface Binder extends CommandBinder<Commands, LearningPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void load(String url, LearningState state);
      void refresh(boolean resetAnchor);
   }
   
   @Inject
   public LearningPresenter(Display display, 
                            LearningServerOperations server,
                            GlobalDisplay globalDisplay,
                            EventBus eventBus,
                            Session session,
                            Binder binder,
                            Commands commands)
   {
      super(display);
      view_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      session_ = session;
     
      binder.bind(commands, this);
   }
   
   public void initialize(LearningState learningState)
   {
      load(learningState);
   }
   
   public void onShowLearningPane(ShowLearningPaneEvent event)
   {
      // if the learning pane wasn't previously shown in this 
      // session then reload
      if (!session_.getSessionInfo().getLearningState().isActive())
         eventBus_.fireEvent(new ReloadEvent());
      else
         load(event.getLearningState());
   }
   
   @Handler
   void onRefreshLearning()
   {
      boolean resetAnchor = Event.getCurrentEvent().getShiftKey();
      view_.refresh(resetAnchor);
   }
   
   public void confirmClose(Command onConfirmed)
   {
      ProgressIndicator progress = new GlobalProgressDelayer(
            globalDisplay_,
            200,
            "Closing Learning Tab...").getIndicator();
      
      server_.closeLearningPane(new VoidServerRequestCallback(progress) {
         @Override
         public void onSuccess()
         {
            eventBus_.fireEvent(new ReloadEvent());
         }
      });
   }
   
   private void load(LearningState state)
   {
      view_.load(server_.getApplicationURL("learning/"), state);
   }

   private final Display view_ ; 
   private final LearningServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Session session_;
}
