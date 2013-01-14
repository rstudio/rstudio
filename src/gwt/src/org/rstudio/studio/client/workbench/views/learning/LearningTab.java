/*
 * LearningTab.java
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


import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.learning.model.LearningState;


public class LearningTab extends DelayLoadWorkbenchTab<LearningPresenter>
{
   public interface Binder extends CommandBinder<Commands, Shim> {}
   
   public abstract static class Shim extends DelayLoadTabShim<LearningPresenter, LearningTab> 
   {
      abstract void initialize(LearningState learningState);
   }

   @Inject
   public LearningTab(final Shim shim,
                      Binder binder, 
                      final Commands commands,
                      EventBus eventBus,
                      Session session)
   {
      super("Learning", shim);
      binder.bind(commands, shim);
      session_ = session;
     
      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler() {
        
         public void onSessionInit(SessionInitEvent sie)
         {
            
            
            // initialize from learning state if necessary
            LearningState learningState = 
                              session_.getSessionInfo().getLearningState();
            if (learningState != null)
               shim.initialize(learningState);           
         }
      });
   }
   
   @Override
   public boolean isSuppressed()
   {
      return !session_.getSessionInfo().getLearningState().isActive();
   }

   private Session session_;
}
