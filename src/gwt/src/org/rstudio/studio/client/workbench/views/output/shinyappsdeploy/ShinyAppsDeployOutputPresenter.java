/*
 * ShinyAppsDeployOutputPresenter.java
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

package org.rstudio.studio.client.workbench.views.output.shinyappsdeploy;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.shiny.events.ShinyAppsDeploymentCompletedEvent;
import org.rstudio.studio.client.shiny.events.ShinyAppsDeploymentOutputEvent;
import org.rstudio.studio.client.shiny.events.ShinyAppsDeploymentStartedEvent;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleActivateEvent;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneDisplay;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneFactory;

public class ShinyAppsDeployOutputPresenter extends BasePresenter
   implements ShinyAppsDeploymentStartedEvent.Handler, 
              ShinyAppsDeploymentOutputEvent.Handler,
              ShinyAppsDeploymentCompletedEvent.Handler,
              RestartStatusEvent.Handler
{
   @Inject
   public ShinyAppsDeployOutputPresenter(CompileOutputPaneFactory outputFactory,
                                   GlobalDisplay globalDisplay,
                                   EventBus events)
   {
      super(outputFactory.create("Deploy Shiny", ""));
      view_ = (CompileOutputPaneDisplay) getView();
      view_.setHasLogs(false);
      events_ = events;
   }
   
   public void initialize()
   {
   }

   public void confirmClose(final Command onConfirmed)
   {
     onConfirmed.execute();
   }

   @Override
   public void onShinyAppsDeploymentStarted(ShinyAppsDeploymentStartedEvent event)
   {
      switchToConsoleAfterDeploy_ = !view_.isEffectivelyVisible();
      view_.ensureVisible(true);
      view_.compileStarted(event.getPath());
      deployRunning_ = true;
   }

   @Override
   public void onShinyAppsDeploymentOutput(ShinyAppsDeploymentOutputEvent event)
   {
      view_.showOutput(event.getOutput());
   }
   
   @Override
   public void onShinyAppsDeploymentCompleted(
         ShinyAppsDeploymentCompletedEvent event)
   {
      view_.compileCompleted();
      deployRunning_ = false;
      if (switchToConsoleAfterDeploy_)
      {
         events_.fireEvent(new ConsoleActivateEvent(false)); 
      }
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // when the restart finishes, clean up the view in case we didn't get a
      // RmdCompletedEvent
      if (event.getStatus() != RestartStatusEvent.RESTART_COMPLETED ||
          !deployRunning_)
         return;

      view_.compileCompleted();
      deployRunning_ = false;
      if (switchToConsoleAfterDeploy_)
      {
         events_.fireEvent(new ConsoleActivateEvent(false)); 
      }
   }
   
   private final CompileOutputPaneDisplay view_;
   private final EventBus events_;
   
   private boolean deployRunning_ = false;
   private boolean switchToConsoleAfterDeploy_ = false;
}