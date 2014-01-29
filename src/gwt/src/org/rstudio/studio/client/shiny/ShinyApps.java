/*
 * ShinyApps.java
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
package org.rstudio.studio.client.shiny;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShinyApps implements SessionInitHandler
{
   public interface Binder
           extends CommandBinder<Commands, ShinyApps> {}

   @Inject
   public ShinyApps(EventBus events, Session session, Commands commands, 
                    GlobalDisplay display, Binder binder)
   {
      session_ = session;
      commands_ = commands;
      display_ = display;

      binder.bind(commands, this);

      events.addHandler(SessionInitEvent.TYPE, this);
   }
   
   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      boolean isShinyAppsInstalled = 
            session_.getSessionInfo().getShinyappsInstalled();
      commands_.shinyAppsConfigure().setVisible(isShinyAppsInstalled);
      commands_.shinyAppsDeploy().setVisible(isShinyAppsInstalled);
      commands_.shinyAppsTerminate().setVisible(isShinyAppsInstalled);
      commands_.shinyAppsManageAccounts().setVisible(isShinyAppsInstalled);
   }
   
   @Handler
   public void onShinyAppsDeploy()
   {
      display_.showMessage(GlobalDisplay.MSG_INFO, "NYI", "Not yet implemented");
   }

   private final Session session_;
   private final Commands commands_;
   private final GlobalDisplay display_;
}
