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

import com.google.inject.Inject;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;

import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;

import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.learning.model.LearningState;

public class LearningPresenter extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
   }
   
   @Inject
   public LearningPresenter(Display display, 
                            GlobalDisplay globalDisplay,
                            final Commands commands,
                            EventBus eventBus)
   {
      super(display);
      view_ = display;
      
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      commands_ = commands;
     
        
    
   }
   
   public void initialize(LearningState learningState)
   {
    
      
   }
   
 
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private final Display view_ ; 
   @SuppressWarnings("unused")
   private final EventBus eventBus_;
   @SuppressWarnings("unused")
   private final Commands commands_;
}
