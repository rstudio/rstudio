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
import org.rstudio.studio.client.common.shiny.model.ShinyAppsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShinyApps implements SessionInitHandler
{
   public interface Binder
           extends CommandBinder<Commands, ShinyApps> {}

   @Inject
   public ShinyApps(EventBus events, 
                    Commands commands, 
                    Session session,
                    GlobalDisplay display, 
                    Binder binder, 
                    ShinyAppsServerOperations server)
   {
      commands_ = commands;
      display_ = display;
      session_ = session;
      server_ = server;

      binder.bind(commands, this);

      events.addHandler(SessionInitEvent.TYPE, this);
   }
   
   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      // Deployment-related ShinyApps commands are invisible by default; they
      // will be set to visible by the source pane if a Shiny file is open and
      // the ShinyApps package is installed.
      commands_.shinyAppsConfigure().setVisible(false);
      commands_.shinyAppsDeploy().setVisible(false);
      commands_.shinyAppsTerminate().setVisible(false);
      
      // "Manage accounts" can be invoked any time the package is available
      commands_.shinyAppsManageAccounts().setVisible(
            session_.getSessionInfo().getShinyappsInstalled());
   }
   
   @Handler
   public void onShinyAppsDeploy()
   {
      display_.showMessage(GlobalDisplay.MSG_INFO, "NYI", "Not yet implemented");
   }
   
   @Handler
   public void onShinyAppsManageAccounts()
   {
      server_.getShinyAppsAccountList(new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(JsArrayString accounts)
         {
            display_.showMessage(GlobalDisplay.MSG_INFO, "Accounts", accounts.get(0));
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error retrieving ShinyApps accounts", 
                                      error.getMessage());
         }
      });
   }

   private final Commands commands_;
   private final GlobalDisplay display_;
   private final Session session_;
   private final ShinyAppsServerOperations server_;
}
